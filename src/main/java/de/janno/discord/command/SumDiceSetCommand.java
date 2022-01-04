package de.janno.discord.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.janno.discord.cache.ActiveButtonsCache;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * TODO:
 * - only the result is in the channel and the adding of dice is ephemeral.
 * This is currently not possible because ephemeral can't be deleted,
 * the alternative would be private threads (currently not supported by discord4j)
 * - d100
 * - configurable buttons
 */
@Slf4j
public class SumDiceSetCommand extends AbstractCommand<SumDiceSetCommand.Config, SumDiceSetCommand.State> {
    private static final String COMMAND_NAME = "sum_dice_set";
    private static final String DICE_SET_DELIMITER = " ";
    private static final String ROLL_BUTTON_ID = "roll";
    private static final String EMPTY_MESSAGE = "Click on the buttons to add dice to the set";
    private static final String CLEAR_BUTTON_ID = "clear";
    private static final String X2_BUTTON_ID = "x2";
    private static final String DICE_SYMBOL = "d";
    private static final String MODIFIER_KEY = "m";
    private final DiceUtils diceUtils;

    public SumDiceSetCommand() {
        this(new DiceUtils());
    }

    @VisibleForTesting
    public SumDiceSetCommand(DiceUtils diceUtils) {
        super(new ActiveButtonsCache(COMMAND_NAME));
        this.diceUtils = diceUtils;
    }

    private static String parseDiceMapToMessageString(Map<String, Integer> diceSet) {
        String message = diceSet.entrySet().stream()
                .sorted(Comparator.comparing(e -> {
                    if (e.getKey().contains(DICE_SYMBOL)) {
                        return Integer.parseInt(e.getKey().substring(1));
                    }
                    //modifiers should always be at the end
                    return Integer.MAX_VALUE;
                }))
                .map(e -> {
                    if (MODIFIER_KEY.equals(e.getKey())) {
                        return String.format("%s%d", e.getValue() > 0 ? "+" : "", e.getValue());
                    }
                    return String.format("%s%d%s", e.getValue() > 0 ? "+" : "", e.getValue(), e.getKey());
                })
                .collect(Collectors.joining(DICE_SET_DELIMITER));
        //remove leading +
        if (message.startsWith("+")) {
            message = message.substring(1);
        }
        return message;
    }

    @Override
    protected String getCommandDescription() {
        return "Configure a variable set of d4 to d20 dice";
    }

    @Override
    protected EmbedCreateSpec getHelpMessage() {
        return EmbedCreateSpec.builder()
                .description("Use '/sum_dice_set start' " +
                        "to get message, where the user can create a dice set and roll it.")
                .build();
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected String createButtonCustomId(String system, String value, Config config, State state) {
        Preconditions.checkArgument(!system.contains(CONFIG_DELIMITER));
        Preconditions.checkArgument(!value.contains(CONFIG_DELIMITER));

        return String.join(CONFIG_DELIMITER,
                system,
                value);
    }

    @Override
    protected List<ApplicationCommandOptionData> getStartOptions() {
        return ImmutableList.of();
    }

    @Override
    protected List<DiceResult> getDiceResult(State state, Config config) {
        List<Integer> diceResultValues = state.getDiceSetMap().entrySet().stream()
                .sorted(Comparator.comparing(e -> {
                    if (e.getKey().contains(DICE_SYMBOL)) {
                        return Integer.parseInt(e.getKey().substring(1));
                    }
                    //modifiers should always be at the end
                    return Integer.MAX_VALUE;
                }))
                .flatMap(e -> {
                    if (MODIFIER_KEY.equals(e.getKey())) {
                        return Stream.of(e.getValue());
                    }
                    int diceSides = Integer.parseInt(e.getKey().substring(1));
                    return diceUtils.rollDiceOfType(Math.abs(e.getValue()), diceSides).stream()
                            .map(dv -> {
                                //modify the result if the dice count is negative
                                if (e.getValue() < 0) {
                                    return dv * -1;
                                }
                                return dv;
                            });
                })
                .collect(Collectors.toList());
        long sumResult = diceResultValues.stream().mapToLong(Integer::longValue).sum();
        String title = parseDiceMapToMessageString(state.getDiceSetMap());
        DiceResult diceResult = new DiceResult(String.format("%s = %d", title, sumResult), diceResultValues.toString());
        return ImmutableList.of(diceResult);
    }

    private int limit(int input) {
        if (input > 100) {
            return 100;
        }
        if (input < -100) {
            return -100;
        }
        return input;
    }

    @Override
    protected String getButtonMessage(State state, Config config) {
        if(state == null){
            return EMPTY_MESSAGE;
        }
        if (ROLL_BUTTON_ID.equals(state.getButtonValue())) {
            return EMPTY_MESSAGE;
        } else if (CLEAR_BUTTON_ID.equals(state.getButtonValue())) {
            return EMPTY_MESSAGE;
        } else if (X2_BUTTON_ID.equals(state.getButtonValue())) {
            return parseDiceMapToMessageString(state.getDiceSetMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> limit(e.getValue() * 2))));
        } else {
            Map<String, Integer> currentDiceSet = new HashMap<>(state.getDiceSetMap());
            int diceModifier;
            String die;

            if (state.getButtonValue().contains(DICE_SYMBOL)) {
                diceModifier = "-".equals(state.getButtonValue().substring(0, 1)) ? -1 : +1;
                die = state.getButtonValue().substring(2);
            } else {
                diceModifier = Integer.parseInt(state.getButtonValue());
                die = MODIFIER_KEY;
            }
            int currentCount = currentDiceSet.getOrDefault(die, 0);
            int newCount = currentCount + diceModifier;
            newCount = limit(newCount);

            if (newCount == 0) {
                currentDiceSet.remove(die);
            } else {
                currentDiceSet.put(die, newCount);
            }

            if (currentDiceSet.isEmpty()) {
                return EMPTY_MESSAGE;
            }
            return parseDiceMapToMessageString(currentDiceSet);
        }
    }

    @Override
    protected boolean createAnswerMessage(State state, Config config) {
        return ROLL_BUTTON_ID.equals(state.getButtonValue()) && !state.getDiceSetMap().isEmpty();
    }

    @Override
    protected boolean copyButtonMessageToTheEnd(State state, Config config) {
        return ROLL_BUTTON_ID.equals(state.getButtonValue()) && !state.getDiceSetMap().isEmpty();
    }

    @Override
    protected Config getConfigFromEvent(IButtonEventAdaptor event) {
        return new Config();
    }

    @Override
    protected State getStateFromEvent(IButtonEventAdaptor event) {
        String buttonMessage = event.getMessageContent();
        String buttenValue = event.getCustomId().split(CONFIG_DELIMITER)[1];
        if (EMPTY_MESSAGE.equals(buttonMessage)) {
            return new State(buttenValue, ImmutableMap.of());
        }

        return new State(buttenValue, Arrays.stream(buttonMessage.split(Pattern.quote(DICE_SET_DELIMITER)))
                //for handling legacy buttons with '1d4 + 1d6)
                .filter(s -> !"+".equals(s))
                //adding the + for the first die type in the message
                .map(s -> {
                    if (!s.startsWith("-") && !s.startsWith("+")) {
                        return "+" + s;
                    }
                    return s;
                })
                .collect(Collectors.toMap(s -> {
                    if (s.contains(DICE_SYMBOL)) {
                        return s.substring(s.indexOf(DICE_SYMBOL));
                    } else {
                        return MODIFIER_KEY;
                    }
                }, s -> {
                    if (s.contains(DICE_SYMBOL)) {
                        return Integer.valueOf(s.substring(0, s.indexOf(DICE_SYMBOL)));
                    } else {
                        return Integer.valueOf(s);
                    }
                })));
    }

    @Override
    protected Config getConfigValuesFromStartOptions(ApplicationCommandInteractionOption options) {
        return new Config();
    }

    @Override
    public boolean matchingComponentCustomId(String buttonCustomId) {
        return buttonCustomId.startsWith(COMMAND_NAME + CONFIG_DELIMITER);
    }

    @Override
    protected List<LayoutComponent> getButtonLayout(State state, Config config) {
        return ImmutableList.of(
                ActionRow.of(
                        //              ID,  label
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+1d4", config, state), "+1d4"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-1d4", config, state), "-1d4"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+1d6", config, state), "+1d6"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-1d6", config, state), "-1d6"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, X2_BUTTON_ID, config, state), "x2")
                ),
                ActionRow.of(
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+1d8", config, state), "+1d8"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-1d8", config, state), "-1d8"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+1d10", config, state), "+1d10"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-1d10", config, state), "-1d10"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, CLEAR_BUTTON_ID, config, state), "Clear")
                ),
                ActionRow.of(
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+1d12", config, state), "+1d12"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-1d12", config, state), "-1d12"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+1d20", config, state), "+1d20"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-1d20", config, state), "-1d20"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, ROLL_BUTTON_ID, config, state), "Roll")
                ),
                ActionRow.of(
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+1", config, state), "+1"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-1", config, state), "-1"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+5", config, state), "+5"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-5", config, state), "-5"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+10", config, state), "+10")
                ));
    }

    @Value
    protected static class Config implements IConfig {


        @Override
        public String toMetricString() {
            return "";
        }

    }

    @Value
    static class State implements IState {
        @NonNull
        String buttonValue;
        @NonNull
        Map<String, Integer> diceSetMap;

    }
}
