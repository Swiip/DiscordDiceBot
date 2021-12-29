package de.janno.discord.command;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import de.janno.discord.Metrics;
import de.janno.discord.dice.DiceResult;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.janno.discord.DiscordUtils.*;
import static de.janno.discord.dice.DiceUtils.MINUS;

@Slf4j
public abstract class AbstractCommand implements ISlashCommand, IComponentInteractEventHandler {

    protected static final String ACTION_START = "start";
    protected static final String ACTION_HELP = "help";
    protected static final String CONFIG_DELIMITER = ",";
    protected final ActiveButtonsCache activeButtonsCache;

    protected AbstractCommand(ActiveButtonsCache activeButtonsCache) {
        this.activeButtonsCache = activeButtonsCache;
    }

    protected static String createButtonCustomId(String system, String value, List<String> config) {
        Preconditions.checkArgument(!system.contains(CONFIG_DELIMITER));
        Preconditions.checkArgument(!value.contains(CONFIG_DELIMITER));
        Preconditions.checkArgument(config.stream().noneMatch(s -> s.contains(CONFIG_DELIMITER)));
        return Stream.concat(Stream.of(system, value), config.stream())
                .collect(Collectors.joining(CONFIG_DELIMITER));
    }

    protected List<ApplicationCommandOptionData> getStartOptions() {
        return ImmutableList.of();
    }

    protected abstract String getCommandDescription();

    @Override
    public ApplicationCommand getApplicationCommand() {
        return ApplicationCommand.builder()
                .name(getName())
                .description(getCommandDescription())
                .option(ApplicationCommandOptionData.builder()
                        .name(ACTION_START)
                        .description("Start")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .addAllOptions(getStartOptions())
                        .build())
                .option(ApplicationCommandOptionData.builder()
                        .name(ACTION_HELP)
                        .description("Help")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .build()
                )
                .build();
    }

    @Override
    public Mono<Void> handleComponentInteractEvent(@NonNull ComponentInteractionEvent event) {
        List<String> config = getConfigFromEvent(event);
        //adding the message of the event to the cache, in the case that the bot was restarted and has forgotten the button
        if (event.getInteraction().getMessageId().isPresent()) {
            activeButtonsCache.addChannelWithButton(event.getInteraction().getChannelId(), event.getInteraction().getMessageId().get(), config);
        }
        String buttonValue = getButtonValueFromEvent(event);
        List<Mono<Void>> actions = new ArrayList<>();
        actions.add(event
                .edit(editMessage(buttonValue, config))
                .onErrorResume(t -> {
                    log.warn("Error on acknowledge button event");
                    return Mono.empty();
                }));
        if (createAnswerMessage(buttonValue, config)) {
            Metrics.incrementButtonMetricCounter(getName(), config);
            actions.add(event.getInteraction().getChannel()
                    .ofType(TextChannel.class)
                    .flatMap(channel -> channel.createMessage(createButtonEventAnswer(event, config)))
                    .ofType(Void.class));
        }
        if (copyButtonMessageToTheEnd(buttonValue, config)) {
            actions.add(event.getInteraction().getChannel()
                    .ofType(TextChannel.class)
                    .flatMap(createButtonMessage(activeButtonsCache, getButtonMessage(buttonValue, config), getButtonLayout(buttonValue, config), config))
                    .flatMap(m -> deleteMessage(m.getChannel(), m.getChannelId(), activeButtonsCache, m.getId(), config)));
        }

        return Flux.mergeDelayError(1, actions.toArray(new Mono<?>[0]))
                .parallel()
                .then();
    }

    protected EmbedCreateSpec createButtonEventAnswer(@NonNull ComponentInteractionEvent event, @NonNull List<String> config) {
        List<DiceResult> result = getDiceResult(getButtonValueFromEvent(event), config);
        result.forEach(d -> log.info(String.format("%s:%s -> %s: %s", getName(), config, d.getResultTitle(), d.getResultDetails()
                .replace("▢", "0")
                .replace("＋", "+")
                .replace(MINUS, "-")
                .replace("*", ""))));
        return createEmbedMessageWithReference(result, event.getInteraction().getMember().orElseThrow());
    }

    protected String editMessage(String buttonId, List<String> config) {
        return "rolling...";
    }

    protected boolean createAnswerMessage(String buttonId, List<String> config) {
        return true;
    }

    protected boolean copyButtonMessageToTheEnd(String buttonId, List<String> config) {
        return true;
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull ChatInputInteractionEvent event) {

        if (event.getOption(ACTION_START).isPresent()) {
            Metrics.incrementSlashStartMetricCounter(getName(), event.getOption(ACTION_START).map(this::getConfigValuesFromStartOptions).orElse(ImmutableList.of()));
            ApplicationCommandInteractionOption options = event.getOption(ACTION_START).get();
            String validationMessage = getStartOptionsValidationMessage(options);
            if (validationMessage != null) {
                log.info("Validation message: {}", validationMessage);
                return event.reply(validationMessage);
            }
            List<String> config = getConfigValuesFromStartOptions(options);

            return event.reply("...")
                    .onErrorResume(t -> {
                        log.error("Error on replay to slash start command", t);
                        return Mono.empty();
                    })
                    .then(event.getInteraction().getChannel().ofType(TextChannel.class)
                            .flatMap(createButtonMessage(activeButtonsCache, getButtonMessage(null, config), getButtonLayout(null, config), config))
                            .ofType(Void.class)
                    );

        } else if (event.getOption(ACTION_HELP).isPresent()) {
            Metrics.incrementSlashHelpMetricCounter(getName());
            return event.reply().withEphemeral(true).withEmbeds(getHelpMessage())
                    .onErrorResume(t -> {
                        log.error("Error on replay to slash help command", t);
                        return Mono.empty();
                    });
        }
        return Mono.empty();
    }

    protected abstract EmbedCreateSpec getHelpMessage();

    protected abstract String getButtonMessage(@Nullable String buttonValue, List<String> config);

    protected abstract List<String> getConfigValuesFromStartOptions(ApplicationCommandInteractionOption options);

    protected abstract List<DiceResult> getDiceResult(String buttonValue, List<String> config);

    protected abstract List<LayoutComponent> getButtonLayout(@Nullable String buttonValue, List<String> config);

    protected String getStartOptionsValidationMessage(ApplicationCommandInteractionOption options) {
        return null;
    }

    protected List<String> getConfigFromEvent(ComponentInteractionEvent event) {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        String[] split = event.getCustomId().split(CONFIG_DELIMITER);
        for (int i = 2; i < split.length; i++) {
            builder.add(split[i]);
        }
        return builder.build();
    }

    protected String getButtonValueFromEvent(ComponentInteractionEvent event) {
        return event.getCustomId().split(CONFIG_DELIMITER)[1];
    }

}
