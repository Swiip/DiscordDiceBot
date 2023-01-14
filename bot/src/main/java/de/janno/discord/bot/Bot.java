package de.janno.discord.bot;


import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.ClearCommand;
import de.janno.discord.bot.command.directRoll.DirectRollCommand;
import de.janno.discord.bot.command.HelpCommand;
import de.janno.discord.bot.command.WelcomeCommand;
import de.janno.discord.bot.command.countSuccesses.CountSuccessesCommand;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.directRoll.DirectRollConfigCommand;
import de.janno.discord.bot.command.fate.FateCommand;
import de.janno.discord.bot.command.holdReroll.HoldRerollCommand;
import de.janno.discord.bot.command.poolTarget.PoolTargetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumDiceSet.SumDiceSetCommand;
import de.janno.discord.bot.persistance.PersistanceManager;
import de.janno.discord.bot.persistance.PersistanceManagerImpl;
import de.janno.discord.connector.DiscordConnectorImpl;

import java.util.Set;

public class Bot {
    public static void main(final String[] args) throws Exception {
        final String token = args[0];
        final boolean disableCommandUpdate = Boolean.parseBoolean(args[1]);
        final String publishMetricsToUrl = args[2];
        BotMetrics.init(publishMetricsToUrl, 8080);

        final String h2Url;
        if (args.length >= 4) {
            h2Url = args[3];
        } else {
            h2Url = "jdbc:h2:file:./persistence/dice_config;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=10";
        }
        final String h2User;
        if (args.length >= 5) {
            h2User = args[4];
        } else {
            h2User = null;
        }
        final String h2Password;
        if (args.length >= 6) {
            h2Password = args[5];
        } else {
            h2Password = null;
        }

        PersistanceManager persistanceManager = new PersistanceManagerImpl(h2Url, h2User, h2Password);

        Set<Long> allGuildIdsInPersistence = persistanceManager.getAllGuildIds();

        DiscordConnectorImpl.createAndStart(token,
                disableCommandUpdate,
                ImmutableList.of(
                        new CountSuccessesCommand(persistanceManager),
                        new CustomDiceCommand(persistanceManager),
                        new FateCommand(persistanceManager),
                        new DirectRollCommand(persistanceManager),
                        new DirectRollConfigCommand(persistanceManager),
                        new SumDiceSetCommand(persistanceManager),
                        new SumCustomSetCommand(persistanceManager),
                        new HoldRerollCommand(persistanceManager),
                        new PoolTargetCommand(persistanceManager),
                        new CustomParameterCommand(persistanceManager),
                        new WelcomeCommand(persistanceManager),
                        new ClearCommand(persistanceManager),
                        new HelpCommand()
                ),
                new WelcomeCommand(persistanceManager).getWelcomeMessage(),
                allGuildIdsInPersistence);
    }

}
