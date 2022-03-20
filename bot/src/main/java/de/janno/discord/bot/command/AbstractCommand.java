package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.connector.api.*;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.discord.bot.cache.ButtonMessageCache;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
public abstract class AbstractCommand<C extends IConfig, S extends IState> implements ISlashCommand, IComponentInteractEventHandler {

    public static final String CONFIG_DELIMITER = ",";
    protected static final String ACTION_START = "start";
    protected static final String ACTION_HELP = "help";
    protected final ButtonMessageCache buttonMessageCache;

    protected AbstractCommand(ButtonMessageCache buttonMessageCache) {
        this.buttonMessageCache = buttonMessageCache;
    }

    @VisibleForTesting
    Map<Long, Set<ButtonMessageCache.ButtonWithConfigHash>> getButtonMessageCache() {
        return buttonMessageCache.getCacheContent();
    }

    @Override
    public CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getName())
                .description(getCommandDescription())
                .option(CommandDefinitionOption.builder()
                        .name(ACTION_START)
                        .description("Start")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .options(getStartOptions())
                        .build())
                .option(CommandDefinitionOption.builder()
                        .name(ACTION_HELP)
                        .description("Help")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .build()
                )
                .build();
    }

    @Override
    public Mono<Void> handleComponentInteractEvent(@NonNull IButtonEventAdaptor event) {
        C config = getConfigFromEvent(event);
        //adding the message of the event to the cache, in the case that the bot was restarted and has forgotten the button
        long messageId = event.getMessageId();
        long channelId = event.getChannelId();
        buttonMessageCache.addChannelWithButton(channelId, messageId, config.hashCode());

        S state = getStateFromEvent(event);

        //all the answer actions
        List<Mono<Void>> actions = new ArrayList<>();
        actions.add(event.acknowledge());
        //the delete action must be the last action
        Mono<Void> deleteAction = Mono.empty();
        boolean triggeringMessageIsPinned = event.isPinned();
        String editMessage;

        if (triggeringMessageIsPinned) {
            //if the old button is pined, the old message will be edited or reset to the slash default
            editMessage = getEditButtonMessage(state, config) != null ? getEditButtonMessage(state, config) : getButtonMessage(config);
        } else {
            //edit the current message if the command changes it or mark it as processing
            editMessage = getEditButtonMessage(state, config) != null ? getEditButtonMessage(state, config) : "processing ...";
        }
        actions.add(event.editMessage(editMessage));

        if (createAnswerMessage(state, config)) {
            BotMetrics.incrementButtonMetricCounter(getName(), config.toShortString());
            Answer answer = getAnswer(state, config);
            actions.add(event.createResultMessageWithEventReference(answer));
            actions.add(event.getRequester()
                    .doOnNext(requester -> log.info("'{}'.'{}' from '{}' button: '{}'={}{} -> {}",
                            requester.getGuildName(),
                            requester.getChannelName(),
                            requester.getUserName(),
                            event.getCustomId(),
                            config.toShortString(),
                            state.toShortString(),
                            answer.toShortString()
                    ))
                    .ofType(Void.class));
        }
        if (copyButtonMessageToTheEnd(state, config)) {
            Mono<Long> newMessageIdMono = event.createButtonMessage(getButtonMessageWithState(state, config), getButtonLayoutWithState(state, config))
                    .map(m -> {
                        buttonMessageCache.addChannelWithButton(channelId, m, config.hashCode());
                        return m;
                    });


            if (triggeringMessageIsPinned) {
                //removing from cache on pin event would be better?
                //if the message was not removed, we don't want that it is removed later
                buttonMessageCache.removeButtonFromChannel(channelId, messageId, config.hashCode());
            }

            deleteAction = newMessageIdMono
                    .flux()
                    .flatMap(id -> Flux.fromIterable(buttonMessageCache.getAllWithoutOneAndRemoveThem(channelId, id, config.hashCode())))
                    .flatMap(event::deleteMessage)
                    .then();
        }

        return Flux.merge(1, actions.toArray(new Mono<?>[0]))
                .parallel()
                .then()
                .then(deleteAction);
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull ISlashEventAdaptor event) {
        String checkPermissions = event.checkPermissions();
        if (checkPermissions != null) {
            return event.reply(checkPermissions);
        }

        String commandString = event.getCommandString();
        Optional<CommandInteractionOption> startOption = event.getOption(ACTION_START);
        if (startOption.isPresent()) {
            CommandInteractionOption options = startOption.get();
            String validationMessage = getStartOptionsValidationMessage(options);
            if (validationMessage != null) {
                log.info("Validation message: {} for {}", validationMessage, commandString);
                return event.reply(String.format("%s\n%s", commandString, validationMessage));
            }
            C config = getConfigFromStartOptions(options);
            BotMetrics.incrementSlashStartMetricCounter(getName(), config.toShortString());

            long channelId = event.getChannelId();

            return event.reply(commandString)
                    .then(event.createButtonMessage(getButtonMessage(config), getButtonLayout(config))
                            .map(m -> {
                                buttonMessageCache.addChannelWithButton(channelId, m, config.hashCode());
                                return m;
                            })
                            .flux()
                            .flatMap(id -> Flux.fromIterable(buttonMessageCache.getAllWithoutOneAndRemoveThem(channelId, id, config.hashCode())))
                            .flatMap(event::deleteMessage).then())
                    .then(event.getRequester()
                            .doOnNext(requester -> log.info("'{}'.'{}' from '{}' slash: '{}'",
                                    requester.getGuildName(),
                                    requester.getChannelName(),
                                    requester.getUserName(),
                                    commandString
                            ))
                            .ofType(Void.class));

        } else if (event.getOption(ACTION_HELP).isPresent()) {
            BotMetrics.incrementSlashHelpMetricCounter(getName());
            return event.replyEphemeral(getHelpMessage());
        }
        return Mono.empty();
    }

    protected List<CommandDefinitionOption> getStartOptions() {
        return ImmutableList.of();
    }

    protected abstract String getCommandDescription();

    protected abstract EmbedDefinition getHelpMessage();

    /**
     * if an answer message (without buttons) should be created
     */
    protected boolean createAnswerMessage(S state, C config) {
        return true;
    }

    /**
     * if the old button message should be moved
     */
    protected boolean copyButtonMessageToTheEnd(S state, C config) {
        return true;
    }

    /**
     * The text content for the old button message, after a button event. Returns null means no editing should be done.
     */
    protected String getEditButtonMessage(S state, C config) {
        return null;
    }

    /**
     * The text content for the new button message, after a button event
     */
    protected abstract String getButtonMessageWithState(S state, C config);

    /**
     * The text content for the new button message, after a slash event
     */
    protected abstract String getButtonMessage(C config);

    protected abstract Answer getAnswer(S state, C config);

    /**
     * The button layout for the new button message, after a button event
     */
    protected abstract List<ComponentRowDefinition> getButtonLayoutWithState(S state, C config);

    /**
     * The button layout for the new button message, after a slash event
     */
    protected abstract List<ComponentRowDefinition> getButtonLayout(C config);

    protected String getStartOptionsValidationMessage(CommandInteractionOption options) {
        //standard is no validation
        return null;
    }

    protected abstract C getConfigFromStartOptions(CommandInteractionOption options);

    protected abstract C getConfigFromEvent(IButtonEventAdaptor event);

    protected abstract S getStateFromEvent(IButtonEventAdaptor event);

}
