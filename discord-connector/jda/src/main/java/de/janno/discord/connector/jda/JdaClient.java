package de.janno.discord.connector.jda;

import de.janno.discord.connector.api.*;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.utils.IOUtil;
import okhttp3.OkHttpClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.security.auth.login.LoginException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

@Slf4j
public class JdaClient {

    public static final Duration START_UP_BUFFER = Duration.of(5, ChronoUnit.MINUTES);

    public void start(String token,
                      boolean disableCommandUpdate,
                      List<SlashCommand> commands,
                      Function<DiscordConnector.WelcomeRequest, EmbedOrMessageDefinition> welcomeMessageDefinition,
                      Set<Long> allGuildIdsInPersistence) throws LoginException {
        LocalDateTime startTimePlusBuffer = LocalDateTime.now().plus(START_UP_BUFFER);
        Scheduler scheduler = Schedulers.boundedElastic();
        Set<Long> botInGuildIdSet = new ConcurrentSkipListSet<>();
        Duration timeout = Duration.of(30, ChronoUnit.SECONDS);
        OkHttpClient okHttpClient = IOUtil.newHttpClientBuilder()
                .eventListener(JdaMetrics.getOkHttpEventListener())
                .writeTimeout(timeout)
                .readTimeout(timeout)
                .connectTimeout(timeout)
                .build();

        JdaMetrics.registerHttpClient(okHttpClient);
        DefaultShardManagerBuilder shardManagerBuilder = DefaultShardManagerBuilder.createLight(token, Collections.emptyList())
                .setHttpClient(okHttpClient)
                .addEventListeners(
                        new ListenerAdapter() {
                            @Override
                            public void onGuildJoin(@NonNull GuildJoinEvent event) {
                                if (!botInGuildIdSet.contains(event.getGuild().getIdLong())) {
                                    log.info("Bot started in guild: name='{}', memberCount={}", event.getGuild().getName(),
                                            event.getGuild().getMemberCount());
                                    botInGuildIdSet.add(event.getGuild().getIdLong());
                                    if (LocalDateTime.now().isAfter(startTimePlusBuffer)) {
                                        Optional.ofNullable(event.getGuild().getSystemChannel())
                                                .filter(GuildMessageChannel::canTalk)
                                                .ifPresent(textChannel -> {
                                                    EmbedOrMessageDefinition welcomeMessage = welcomeMessageDefinition.apply(new DiscordConnector.WelcomeRequest(event.getGuild().getIdLong(),
                                                            textChannel.getIdLong(), LocaleConverter.toLocale(event.getGuild().getLocale())));
                                                    Mono.fromFuture(textChannel.sendMessage(
                                                                            MessageComponentConverter.messageComponent2MessageLayout(welcomeMessage.getDescriptionOrContent(),
                                                                                    welcomeMessage.getComponentRowDefinitions()))
                                                                    .submit())
                                                            .doOnSuccess(m -> {
                                                                JdaMetrics.sendWelcomeMessage();
                                                                log.info("Welcome message send in '{}'.'{}'",
                                                                        event.getGuild().getName(),
                                                                        textChannel.getName());
                                                            })
                                                            .subscribeOn(scheduler)
                                                            .subscribe();
                                                });
                                    }
                                }
                            }

                            @Override
                            public void onGuildLeave(@NonNull GuildLeaveEvent event) {
                                if (botInGuildIdSet.contains(event.getGuild().getIdLong())) {
                                    log.info("Bot removed in guild: name='{}', memberCount={}", event.getGuild().getName(),
                                            event.getGuild().getMemberCount());
                                    botInGuildIdSet.remove(event.getGuild().getIdLong());
                                }
                            }

                            @Override
                            public void onGuildReady(@NonNull GuildReadyEvent event) {
                                if (!botInGuildIdSet.contains(event.getGuild().getIdLong())) {
                                    log.info("Bot started with guild: name='{}', memberCount={}", event.getGuild().getName(),
                                            event.getGuild().getMemberCount());
                                    botInGuildIdSet.add(event.getGuild().getIdLong());
                                }
                            }

                            @Override
                            public void onReady(@NonNull ReadyEvent event) {
                                long inactiveGuildIdCountWithConfig = allGuildIdsInPersistence.stream()
                                        .filter(id -> !botInGuildIdSet.contains(id))
                                        .count();
                                log.info("Inactive guild count with config: {}", inactiveGuildIdCountWithConfig);
                            }

                            @Override
                            public void onCommandAutoCompleteInteraction(@NonNull CommandAutoCompleteInteractionEvent event) {
                                Flux.fromIterable(commands)
                                        .filter(command -> command.getCommandId().equals(event.getName()))
                                        .next()
                                        .map(command -> command.getAutoCompleteAnswer(fromEvent(event), LocaleConverter.toLocale(event.getUserLocale())))
                                        .flatMap(a -> Mono.fromFuture(event.replyChoices(a.stream()
                                                .map(c -> new Command.Choice(c.getName(), c.getValue()))
                                                .limit(25)
                                                .toList()).submit()))
                                        .subscribeOn(scheduler)
                                        .subscribe();
                            }

                            private AutoCompleteRequest fromEvent(CommandAutoCompleteInteractionEvent event) {
                                return new AutoCompleteRequest(event.getFocusedOption().getName(),
                                        event.getFocusedOption().getValue(),
                                        event.getOptions().stream()
                                                .map(s -> new OptionValue(s.getName(), s.getAsString()))
                                                .toList()
                                );
                            }

                            @Override
                            public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {
                                log.trace("ChatInputEvent: {} from {}", event.getInteraction().getCommandId(),
                                        event.getInteraction().getUser().getName());
                                Flux.fromIterable(commands)
                                        .filter(command -> command.getCommandId().equals(event.getName()))
                                        .next()
                                        .flatMap(command -> {
                                            Locale userLocale = LocaleConverter.toLocale(event.getInteraction().getUserLocale());
                                            JdaMetrics.userLocalInteraction(userLocale);
                                            return command.handleSlashCommandEvent(new SlashEventAdapterImpl(event,
                                                    new Requester(event.getInteraction().getUser().getName(),
                                                            event.getChannel().getName(),
                                                            Optional.ofNullable(event.getGuild()).map(Guild::getName).orElse(""),
                                                            event.getJDA().getShardInfo().getShardString(),
                                                            userLocale)
                                            ), UUID::randomUUID, LocaleConverter.toLocale(event.getUserLocale()));
                                        })
                                        .onErrorResume(e -> {
                                            log.error("SlashCommandEvent Exception: ", e);
                                            return Mono.empty();
                                        })
                                        .subscribeOn(scheduler)
                                        .subscribe();
                            }

                            @Override
                            public void onButtonInteraction(@NonNull ButtonInteractionEvent event) {
                                log.trace("ComponentEvent: {} from {}", event.getInteraction().getComponentId(), event.getInteraction().getUser().getName());
                                Flux.fromIterable(commands)
                                        .ofType(ComponentInteractEventHandler.class)
                                        .filter(command -> command.matchingComponentCustomId(event.getInteraction().getComponentId()))
                                        .next()
                                        .flatMap(command -> {
                                            Locale userLocale = LocaleConverter.toLocale(event.getInteraction().getUserLocale());
                                            JdaMetrics.userLocalInteraction(userLocale);
                                            return command.handleComponentInteractEvent(new ButtonEventAdapterImpl(event,
                                                    new Requester(event.getInteraction().getUser().getName(),
                                                            event.getChannel().getName(),
                                                            Optional.ofNullable(event.getInteraction().getGuild()).map(Guild::getName).orElse(""),
                                                            event.getJDA().getShardInfo().getShardString(),
                                                            userLocale
                                                    )));
                                        })
                                        .onErrorResume(e -> {
                                            log.error("ButtonInteractEvent Exception: ", e);
                                            return Mono.empty();
                                        })
                                        .subscribeOn(scheduler)
                                        .subscribe();
                            }
                        }
                )
                .setActivity(Activity.customStatus("Type /quickstart or /help"));

        ShardManager shardManager = shardManagerBuilder.build();
        JdaMetrics.startGuildCountGauge(botInGuildIdSet);

        shardManager.getShards().forEach(jda -> {
            try {
                jda.awaitReady();
                JdaMetrics.startGatewayResponseTimeGauge(jda);
                JdaMetrics.startUserCacheGauge(jda);
                JdaMetrics.startShardCountGauge(jda);
                JdaMetrics.startTextChannelCacheGauge(jda);
                JdaMetrics.startGuildCacheGauge(jda);
                JdaMetrics.startRestLatencyGauge(jda);

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    log.info("start jda %s shutdown".formatted(jda.getShardInfo().getShardString()));
                    shutdown(jda);
                    log.info("finished jda %s shutdown".formatted(jda.getShardInfo().getShardString()));
                }));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        SlashCommandRegistry.builder()
                .addSlashCommands(commands)
                .registerSlashCommands(shardManager.getShards().get(0), disableCommandUpdate);
    }


    private void shutdown(JDA jda) {
        // Initiating the shutdown, this closes the gateway connection and subsequently closes the requester queue
        jda.shutdown();
        try {
            // Allow at most 5 seconds for remaining requests to finish
            if (!jda.awaitShutdown(Duration.ofSeconds(10))) { // returns true if shutdown is graceful, false if timeout exceeded
                log.warn("shutdown took more then 10sec");
                jda.shutdownNow(); // Cancel all remaining requests, and stop thread-pools
                boolean finishWithoutTimeout = jda.awaitShutdown(Duration.ofSeconds(10)); // Wait until shutdown is complete (10 sec)
                if (!finishWithoutTimeout) {
                    log.warn("shutdown now took more then 10sec");
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
