package de.janno.discord.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface ISlashCommand {

    String getName();

    ApplicationCommand getApplicationCommand();

    Mono<Void> handleSlashCommandEvent(@NonNull ChatInputInteractionEvent event);


}
