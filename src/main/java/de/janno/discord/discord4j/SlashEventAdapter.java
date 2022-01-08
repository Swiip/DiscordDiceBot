package de.janno.discord.discord4j;

import de.janno.discord.command.ISlashEventAdaptor;
import de.janno.discord.dice.DiceResult;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class SlashEventAdapter extends DiscordAdapter implements ISlashEventAdaptor {

    private final ChatInputInteractionEvent event;

    public SlashEventAdapter(ChatInputInteractionEvent event) {
        this.event = event;
    }

    @Override
    public String checkPermissions() {
        PermissionSet permissions = Mono.zip(event.getInteraction().getChannel().ofType(TextChannel.class), event.getInteraction().getGuild().flatMap(Guild::getSelfMember))
                .flatMap(channelAndMember -> channelAndMember.getT1().getEffectivePermissions(channelAndMember.getT2()))
                .blockOptional()
                .orElse(PermissionSet.of());

        List<String> checks = new ArrayList<>();
        if (!permissions.contains(Permission.SEND_MESSAGES)) {
            checks.add("'SEND_MESSAGES'");
        }
        if (!permissions.contains(Permission.EMBED_LINKS)) {
            checks.add("'EMBED_LINKS'");
        }
        if (checks.isEmpty()) {
            return null;
        }
        String result = String.format("The bot is missing the permission: %s. It will not work correctly without it. Please check the guild and channel permissions for the bot", String.join(" and ", checks));
        log.info(result);
        return result;
    }

    @Override
    public Optional<ApplicationCommandInteractionOption> getOption(String optionName) {
        return event.getOption(optionName);
    }

    @Override
    public Mono<Void> reply(String message) {
        return event.reply(message)
                .onErrorResume(t -> {
                    log.error("Error on replay", t);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> replyEphemeral(EmbedCreateSpec embedCreateSpec) {
        return event.reply().withEphemeral(true).withEmbeds(embedCreateSpec)
                .onErrorResume(t -> {
                    log.error("Error on replay to slash help command", t);
                    return Mono.empty();
                });

    }

    @Override
    public Mono<Long> createButtonMessage(@NonNull String buttonMessage, @NonNull List<LayoutComponent> buttons) {
        return event.getInteraction().getChannel().ofType(TextChannel.class)
                .flatMap(channel -> createButtonMessage(channel, buttonMessage, buttons))
                .onErrorResume(t -> {
                    log.error("Error on creating button message", t);
                    return Mono.empty();
                })
                .map(m -> m.getId().asLong());
    }

    @Override
    public Mono<Void> createResultMessageWithEventReference(List<DiceResult> diceResults) {
        return event.getInteraction().getChannel().ofType(TextChannel.class)
                .flatMap(channel -> channel.createMessage(createEmbedMessageWithReference(diceResults, event.getInteraction().getMember().orElseThrow())))
                .onErrorResume(t -> {
                    log.error("Error on creating dice result message", t);
                    return Mono.empty();
                })
                .ofType(Void.class);
    }

    @Override
    public Long getChannelId() {
        return event.getInteraction().getChannelId().asLong();
    }

    @Override
    public String getCommandString() {
        String options = event.getOptions().stream()
                .map(a -> optionToString(a.getName(), a.getOptions(), a.getValue().orElse(null)))
                .collect(Collectors.joining(" "));
        return String.format("`/%s %s`", event.getCommandName(), options);


    }

    private String optionToString(@NonNull String name, @NonNull List<ApplicationCommandInteractionOption> options, @Nullable ApplicationCommandInteractionOptionValue value) {
        if (options.isEmpty() && value == null) {
            return name;
        }
        String out = name;
        if (value != null) {
            out = String.format("%s:%s", name, value.getRaw());
        }
        String optionsString = options.stream()
                .map(a -> optionToString(a.getName(), a.getOptions(), a.getValue().orElse(null)))
                .collect(Collectors.joining(" "));
        if (!optionsString.isEmpty()) {
            out = String.format("%s %s", out, optionsString);
        }

        return out;
    }


}
