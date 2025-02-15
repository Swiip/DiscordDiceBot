package de.janno.discord.bot.command.sumCustomSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.command.channelConfig.AliasHelper;
import de.janno.discord.bot.dice.*;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
public class SumCustomSetCommand extends AbstractCommand<SumCustomSetConfig, SumCustomSetStateDataV2> {
    static final String BUTTONS_COMMAND_OPTIONS_NAME = "buttons";
    static final String ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_NAME = "always_sum_result";
    static final String HIDE_EXPRESSION_IN_ANSWER = "hide_expression_in_answer";
    private static final String COMMAND_NAME = "sum_custom_set";
    private static final String ROLL_BUTTON_ID = "roll";
    private static final String NO_ACTION = "no action";
    private static final String CLEAR_BUTTON_ID = "clear";
    private static final String BACK_BUTTON_ID = "back";

    private static final String LABEL_DELIMITER = "@";
    private static final String CONFIG_TYPE_ID = "SumCustomSetConfig";
    private static final String STATE_DATA_TYPE_ID = "SumCustomSetStateDataV2";
    private static final String STATE_DATA_TYPE_LEGACY_ID = "SumCustomSetStateData";
    private final DiceSystemAdapter diceSystemAdapter;

    public SumCustomSetCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        this(persistenceManager, new DiceParser(), cachingDiceEvaluator);
    }

    @VisibleForTesting
    public SumCustomSetCommand(PersistenceManager persistenceManager, Dice dice, CachingDiceEvaluator cachingDiceEvaluator) {
        super(persistenceManager);
        this.diceSystemAdapter = new DiceSystemAdapter(cachingDiceEvaluator, dice);
    }

    @Override
    protected ConfigAndState<SumCustomSetConfig, SumCustomSetStateDataV2> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO messageConfigDTO,
                                                                                                                 @NonNull MessageDataDTO messageDataDTO,
                                                                                                                 @NonNull String buttonValue,
                                                                                                                 @NonNull String invokingUserName) {
        return deserializeAndUpdateState(messageConfigDTO, messageDataDTO, buttonValue, invokingUserName);
    }

    @Override
    protected void updateCurrentMessageStateData(UUID configUUID, long guildId, long channelId, long messageId, @NonNull SumCustomSetConfig config, @NonNull State<SumCustomSetStateDataV2> state) {
        if (ROLL_BUTTON_ID.equals(state.getButtonValue())) {
            persistenceManager.deleteStateForMessage(channelId, messageId);
            //message data so we knew the button message exists
            persistenceManager.saveMessageData(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), Mapper.NO_PERSISTED_STATE, null));
        } else if (state.getData() != null) {
            persistenceManager.deleteStateForMessage(channelId, messageId);
            persistenceManager.saveMessageData(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), STATE_DATA_TYPE_ID, Mapper.serializedObject(state.getData())));
        }
    }

    @VisibleForTesting
    ConfigAndState<SumCustomSetConfig, SumCustomSetStateDataV2> deserializeAndUpdateState(
            @NonNull MessageConfigDTO messageConfigDTO,
            @NonNull MessageDataDTO messageDataDTO,
            @NonNull String buttonValue,
            @NonNull String invokingUserName) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId()), "Unknown configClassId: %s", messageConfigDTO.getConfigClassId());
        Preconditions.checkArgument(Optional.of(messageDataDTO)
                .map(MessageDataDTO::getStateDataClassId)
                .map(c -> Set.of(STATE_DATA_TYPE_ID, STATE_DATA_TYPE_LEGACY_ID, Mapper.NO_PERSISTED_STATE).contains(c))
                .orElse(true), "Unknown stateDataClassId: %s", Optional.of(messageDataDTO)
                .map(MessageDataDTO::getStateDataClassId).orElse("null"));

        final SumCustomSetStateDataV2 loadedStateData;
        if (messageDataDTO.getStateDataClassId().equals(STATE_DATA_TYPE_ID)) {
            loadedStateData = Optional.of(messageDataDTO)
                    .map(MessageDataDTO::getStateData)
                    .map(sd -> Mapper.deserializeObject(sd, SumCustomSetStateDataV2.class))
                    .orElse(null);
        } else if (messageDataDTO.getStateDataClassId().equals(STATE_DATA_TYPE_LEGACY_ID)) {
            loadedStateData = Optional.of(messageDataDTO)
                    .map(MessageDataDTO::getStateData)
                    .map(sd -> Mapper.deserializeObject(sd, SumCustomSetStateData.class))
                    .map(l -> new SumCustomSetStateDataV2(l.getDiceExpressions().stream()
                            .map(e -> new ExpressionAndLabel(e, e))
                            .collect(Collectors.toList()),
                            l.getLockedForUserName()))
                    .orElse(null);
        } else {
            loadedStateData = null;
        }


        final SumCustomSetConfig loadedConfig = Mapper.deserializeObject(messageConfigDTO.getConfig(), SumCustomSetConfig.class);
        final State<SumCustomSetStateDataV2> updatedState = updateStateWithButtonValue(buttonValue,
                Optional.ofNullable(loadedStateData).map(SumCustomSetStateDataV2::getDiceExpressions).orElse(ImmutableList.of()),
                invokingUserName,
                Optional.ofNullable(loadedStateData).map(SumCustomSetStateDataV2::getLockedForUserName).orElse(""),
                loadedConfig.getLabelAndExpression());
        return new ConfigAndState<>(messageConfigDTO.getConfigUUID(), loadedConfig, updatedState);
    }

    @Override
    public Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID, long guildId, long channelId, @NonNull SumCustomSetConfig config) {
        return Optional.of(new MessageConfigDTO(configUUID, guildId, channelId, getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    protected @NonNull EmbedOrMessageDefinition getHelpMessage(Locale userLocale) {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent(I18n.getMessage("sum_custom_set.help.message", userLocale) + "\n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.example.field.name", userLocale), I18n.getMessage("sum_custom_set.help.example.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.documentation.field.name", userLocale), I18n.getMessage("help.documentation.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.discord.server.field.name", userLocale), I18n.getMessage("help.discord.server.field.value", userLocale), false))
                .build();
    }

    @Override
    public @NonNull String getCommandId() {
        return COMMAND_NAME;
    }

    @Override
    protected @NonNull List<CommandDefinitionOption> getStartOptions() {
        return List.of(CommandDefinitionOption.builder()
                        .name(BUTTONS_COMMAND_OPTIONS_NAME)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("sum_dice_set.option.buttons.name"))
                        .description(I18n.getMessage("sum_dice_set.option.buttons.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("sum_dice_set.option.buttons.description"))
                        .type(CommandDefinitionOption.Type.STRING)
                        .required(true)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_NAME)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("sum_dice_set.option.alwaysSum.name"))
                        .description(I18n.getMessage("sum_dice_set.option.alwaysSum.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("sum_dice_set.option.alwaysSum.description"))
                        .type(CommandDefinitionOption.Type.BOOLEAN)
                        .required(false)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(HIDE_EXPRESSION_IN_ANSWER)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("sum_dice_set.option.hideExpression.name"))
                        .description(I18n.getMessage("sum_dice_set.option.hiddeExpression.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("sum_dice_set.option.hiddeExpression.description"))
                        .type(CommandDefinitionOption.Type.BOOLEAN)
                        .required(false)
                        .build()
        );
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(SumCustomSetConfig config, State<SumCustomSetStateDataV2> state, long channelId, long userId) {
        if (!(ROLL_BUTTON_ID.equals(state.getButtonValue()) &&
                !Optional.ofNullable(state.getData())
                        .map(SumCustomSetStateDataV2::getDiceExpressions)
                        .map(List::isEmpty)
                        .orElse(true))) {
            return Optional.empty();
        }
        String label = combineLabel(state.getData().getDiceExpressions(), config);
        String newExpression = AliasHelper.getAndApplyAliaseToExpression(channelId, userId, persistenceManager, combineExpressions(state.getData().getDiceExpressions()));

        return Optional.of(diceSystemAdapter.answerRollWithGivenLabel(newExpression,
                label,
                config.isAlwaysSumResult(),
                config.getDiceParserSystem(),
                config.getAnswerFormatType(),
                config.getDiceStyleAndColor(),
                config.getConfigLocale()));
    }

    @Override
    public @NonNull EmbedOrMessageDefinition createNewButtonMessage(@NonNull UUID configUUID, @NonNull SumCustomSetConfig config) {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent(I18n.getMessage("sum_custom_set.buttonMessage.empty", config.getConfigLocale()))
                .type(EmbedOrMessageDefinition.Type.MESSAGE)
                .componentRowDefinitions(createButtonLayout(configUUID, config, true, config.getConfigLocale()))
                .build();
    }

    @Override
    protected @NonNull Optional<EmbedOrMessageDefinition> createNewButtonMessageWithState(@NonNull UUID customUuid, @NonNull SumCustomSetConfig config, @NonNull State<SumCustomSetStateDataV2> state, long guildId, long channelId) {
        if (ROLL_BUTTON_ID.equals(state.getButtonValue()) && !Optional.ofNullable(state.getData())
                .map(SumCustomSetStateDataV2::getDiceExpressions)
                .map(List::isEmpty)
                .orElse(false)) {
            return Optional.of(EmbedOrMessageDefinition.builder()
                    .descriptionOrContent(I18n.getMessage("sum_custom_set.buttonMessage.empty", config.getConfigLocale()))
                    .type(EmbedOrMessageDefinition.Type.MESSAGE)
                    .componentRowDefinitions(createButtonLayout(customUuid, config, true, config.getConfigLocale()))
                    .build());
        }
        return Optional.empty();
    }

    @Override
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(UUID customUuid, SumCustomSetConfig config, State<SumCustomSetStateDataV2> state, long channelId, long userId) {
        if (state.getData() == null) {
            return Optional.empty();
        }
        String expression = AliasHelper.getAndApplyAliaseToExpression(channelId, userId, persistenceManager, combineExpressions(state.getData().getDiceExpressions()));

        return Optional.of(createButtonLayout(customUuid, config, !diceSystemAdapter.isValidExpression(expression, config.getDiceParserSystem()), config.getConfigLocale()));
    }

    @Override
    public @NonNull Optional<String> getCurrentMessageContentChange(SumCustomSetConfig config, State<SumCustomSetStateDataV2> state) {
        if (ROLL_BUTTON_ID.equals(state.getButtonValue())) {
            return Optional.of(I18n.getMessage("sum_custom_set.buttonMessage.empty", config.getConfigLocale()));
        } else if (CLEAR_BUTTON_ID.equals(state.getButtonValue())) {
            return Optional.of(I18n.getMessage("sum_custom_set.buttonMessage.empty", config.getConfigLocale()));
        } else {
            if (Optional.ofNullable(state.getData())
                    .map(SumCustomSetStateDataV2::getDiceExpressions)
                    .map(List::isEmpty)
                    .orElse(false)) {
                return Optional.of(I18n.getMessage("sum_custom_set.buttonMessage.empty", config.getConfigLocale()));
            }
            if (Optional.ofNullable(state.getData()).map(SumCustomSetStateDataV2::getLockedForUserName).isEmpty()) {
                return Optional.ofNullable(state.getData()).map(SumCustomSetStateDataV2::getDiceExpressions).map(e -> combineLabel(e, config));
            } else {
                String cleanName = state.getData().getLockedForUserName();
                return Optional.of(String.format("%s: %s", cleanName, combineLabel(state.getData().getDiceExpressions(), config)));
            }
        }
    }


    private State<SumCustomSetStateDataV2> updateStateWithButtonValue(@NonNull final String buttonValue,
                                                                      @NonNull final List<ExpressionAndLabel> currentExpressions,
                                                                      @NonNull final String invokingUserName,
                                                                      @Nullable final String lockedToUser,
                                                                      @NonNull final List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions) {
        if (CLEAR_BUTTON_ID.equals(buttonValue)) {
            return new State<>(buttonValue, new SumCustomSetStateDataV2(ImmutableList.of(), null));
        }
        if (!Strings.isNullOrEmpty(lockedToUser) && !lockedToUser.equals(invokingUserName)) {
            return new State<>(NO_ACTION, new SumCustomSetStateDataV2(currentExpressions, lockedToUser));
        }
        if (BACK_BUTTON_ID.equals(buttonValue)) {
            final List<ExpressionAndLabel> newExpressionList;
            if (currentExpressions.isEmpty()) {
                newExpressionList = ImmutableList.of();
            } else {
                newExpressionList = ImmutableList.copyOf(currentExpressions.subList(0, currentExpressions.size() - 1));
            }
            return new State<>(buttonValue, new SumCustomSetStateDataV2(newExpressionList, newExpressionList.isEmpty() ? null : lockedToUser));
        }
        if (ROLL_BUTTON_ID.equals(buttonValue)) {
            return new State<>(buttonValue, new SumCustomSetStateDataV2(currentExpressions, lockedToUser));
        }
        final Optional<ExpressionAndLabel> addExpression = buttonIdLabelAndDiceExpressions.stream()
                .filter(bld -> bld.getButtonId().equals(buttonValue))
                .map(bld -> new ExpressionAndLabel(bld.getDiceExpression(), bld.getLabel()))
                .findFirst();
        if (addExpression.isEmpty()) {
            return new State<>(NO_ACTION, new SumCustomSetStateDataV2(ImmutableList.of(), null));
        }
        final List<ExpressionAndLabel> expressionWithNewValue = ImmutableList.<ExpressionAndLabel>builder()
                .addAll(currentExpressions)
                .add(addExpression.get())
                .build();

        return new State<>(buttonValue, new SumCustomSetStateDataV2(expressionWithNewValue, invokingUserName));
    }

    private String combineExpressions(List<ExpressionAndLabel> expressions) {
        return expressions.stream()
                .map(ExpressionAndLabel::getExpression)
                .collect(Collectors.joining(""));
    }

    private String combineLabel(List<ExpressionAndLabel> expressions, SumCustomSetConfig config) {
        if (config.isHideExpressionInStatusAndAnswer()) {
            return expressions.stream()
                    .map(ExpressionAndLabel::getLabel)
                    .collect(Collectors.joining(" "));
        }
        return combineExpressions(expressions);
    }

    @Override
    protected @NonNull SumCustomSetConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options, @NonNull Locale userLocale) {
        final List<ButtonIdAndExpression> buttons = getButtonsFromCommandInteractionOption(options);
        final boolean alwaysSumResults = options.getBooleanSubOptionWithName(ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_NAME).orElse(true);
        final DiceParserSystem diceParserSystem = DiceParserSystem.DICE_EVALUATOR;
        final Long answerTargetChannelId = BaseCommandOptions.getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null);
        final AnswerFormatType answerType = BaseCommandOptions.getAnswerTypeFromStartCommandOption(options).orElse(defaultAnswerFormat());
        final boolean hideExpressionInAnswer = options.getBooleanSubOptionWithName(HIDE_EXPRESSION_IN_ANSWER).orElse(true);

        return getConfigOptionStringList(buttons, answerTargetChannelId, diceParserSystem, alwaysSumResults, answerType,
                BaseCommandOptions.getDiceStyleOptionFromStartCommandOption(options).orElse(DiceImageStyle.polyhedral_3d),
                BaseCommandOptions.getDiceColorOptionFromStartCommandOption(options).orElse(DiceImageStyle.polyhedral_3d.getDefaultColor()),
                userLocale,
                hideExpressionInAnswer
        );
    }

    private List<ButtonIdAndExpression> getButtonsFromCommandInteractionOption(@NonNull CommandInteractionOption options) {
        ImmutableList.Builder<ButtonIdAndExpression> builder = ImmutableList.builder();
        String buttons = options.getStringSubOptionWithName(BUTTONS_COMMAND_OPTIONS_NAME).orElseThrow();
        int idCounter = 1;
        for (String button : buttons.split(";")) {
            builder.add(new ButtonIdAndExpression(idCounter++ + "_button", button.trim()));
        }
        return builder.build();
    }

    private List<ComponentRowDefinition> createButtonLayout(UUID customUUID, SumCustomSetConfig config, boolean rollDisabled, Locale configLocale) {
        List<ButtonDefinition> buttons = config.getLabelAndExpression().stream()
                .map(d -> ButtonDefinition.builder()
                        .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), d.getButtonId(), customUUID))
                        .label(d.getLabel())
                        .build())
                .collect(Collectors.toList());
        buttons.add(ButtonDefinition.builder()
                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ROLL_BUTTON_ID, customUUID))
                .label(I18n.getMessage("sum_custom_set.button.label.roll", configLocale))
                .disabled(rollDisabled)
                .style(rollDisabled ? ButtonDefinition.Style.PRIMARY : ButtonDefinition.Style.SUCCESS)
                .build());
        buttons.add(ButtonDefinition.builder()
                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), CLEAR_BUTTON_ID, customUUID))
                .label(I18n.getMessage("sum_custom_set.button.label.clear", configLocale))
                .style(ButtonDefinition.Style.DANGER)
                .build());
        buttons.add(ButtonDefinition.builder()
                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), BACK_BUTTON_ID, customUUID))
                .label(I18n.getMessage("sum_custom_set.button.label.back", configLocale))
                .style(ButtonDefinition.Style.SECONDARY)
                .build());
        return Lists.partition(buttons, 5).stream()
                .map(bl -> ComponentRowDefinition.builder().buttonDefinitions(bl).build())
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    SumCustomSetConfig getConfigOptionStringList(List<ButtonIdAndExpression> startOptions,
                                                 Long answerTargetChannelId,
                                                 DiceParserSystem diceParserSystem,
                                                 boolean alwaysSumResult,
                                                 AnswerFormatType answerFormatType,
                                                 DiceImageStyle diceImageStyle,
                                                 String defaultDiceColor,
                                                 Locale userLocale,
                                                 boolean useLabelForAnswer) {
        return new SumCustomSetConfig(answerTargetChannelId, startOptions.stream()
                .filter(be -> !be.getExpression().contains(BottomCustomIdUtils.CUSTOM_ID_DELIMITER))
                .filter(be -> !be.getExpression().contains(LABEL_DELIMITER) || be.getExpression().split(LABEL_DELIMITER).length == 2)
                .map(be -> {
                    String label = null;
                    String diceExpression;
                    if (be.getExpression().contains(LABEL_DELIMITER)) {
                        String[] split = be.getExpression().split(LABEL_DELIMITER);
                        label = split[1].trim();
                        diceExpression = split[0].trim();
                    } else {
                        diceExpression = be.getExpression().trim();
                    }
                    if (!diceExpression.startsWith("+") && !diceExpression.startsWith("-")
                            && diceParserSystem == DiceParserSystem.DICEROLL_PARSER) {
                        diceExpression = "+" + diceExpression;
                    }
                    if (label == null) {
                        label = diceExpression;
                    }
                    return new ButtonIdLabelAndDiceExpression(be.getButtonId(), label, diceExpression);
                })
                .filter(s -> !s.getDiceExpression().isEmpty())
                .filter(s -> !s.getLabel().isEmpty())
                .filter(lv -> {
                    if (DiceParserSystem.DICEROLL_PARSER == diceParserSystem) {
                        return diceSystemAdapter.isValidExpression(lv.getDiceExpression(), diceParserSystem);
                    }
                    return true;
                })
                .distinct()
                .limit(22)
                .collect(Collectors.toList()),
                diceParserSystem, alwaysSumResult, useLabelForAnswer, answerFormatType, null, new DiceStyleAndColor(diceImageStyle, defaultDiceColor),
                userLocale);
    }

    @Value
    private static class ButtonIdAndExpression {
        @NonNull
        String buttonId;
        @NonNull
        String expression;
    }

}
