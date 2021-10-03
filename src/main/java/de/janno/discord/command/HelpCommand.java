package de.janno.discord.command;

import com.codahale.metrics.SharedMetricRegistries;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class HelpCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public ApplicationCommandRequest getApplicationCommand() {
        return ApplicationCommandRequest.builder()
                .name(getName())
                .description("Link to the documentation")
                .build();
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull ChatInputInteractionEvent event) {
        if (getName().equals(event.getCommandName())) {
            SharedMetricRegistries.getDefault().counter(getName()).inc();
            return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                    .ephemeral(true)
                    .content("Full documentation can be found under: https://github.com/twonirwana/DiscordDiceBot/blob/main/README.md")
                    .build());
        }
        return Mono.empty();
    }
}
