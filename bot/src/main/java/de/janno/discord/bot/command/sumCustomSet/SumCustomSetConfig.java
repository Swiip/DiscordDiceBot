package de.janno.discord.bot.command.sumCustomSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString(callSuper = true)
public class SumCustomSetConfig extends Config {
    @NonNull
    private final List<ButtonIdLabelAndDiceExpression> labelAndExpression;
    @NonNull
    private final DiceParserSystem diceParserSystem;
    private final boolean alwaysSumResult;
    private final boolean hideExpressionInStatusAndAnswer;

    @JsonCreator
    public SumCustomSetConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                              @JsonProperty("labelAndExpression") @NonNull List<ButtonIdLabelAndDiceExpression> labelAndExpression,
                              @JsonProperty("diceParserSystem") DiceParserSystem diceParserSystem,
                              @JsonProperty("alwaysSumResult") Boolean alwaysSumResult,
                              @JsonProperty("hideExpressionInStatusAndAnswer") Boolean hideExpressionInStatusAndAnswer,
                              @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
                              @JsonProperty("resultImage") ResultImage resultImage,
                              @JsonProperty("diceImageStyle") DiceStyleAndColor diceStyleAndColor,
                              @JsonProperty("configLocale") Locale configLocale
    ) {
        super(answerTargetChannelId, answerFormatType, resultImage, diceStyleAndColor, configLocale);
        this.labelAndExpression = labelAndExpression;
        this.diceParserSystem = diceParserSystem == null ? DiceParserSystem.DICEROLL_PARSER : diceParserSystem;
        this.alwaysSumResult = alwaysSumResult == null || alwaysSumResult;
        this.hideExpressionInStatusAndAnswer = Optional.ofNullable(hideExpressionInStatusAndAnswer).orElse(false);
    }

    @Override
    public String toShortString() {
        String buttons = labelAndExpression.stream()
                .map(ButtonIdLabelAndDiceExpression::toShortString)
                .collect(Collectors.joining(", "));
        String statusAndAnswerType = hideExpressionInStatusAndAnswer ? "labelAnswer" : "expressionAnswer";
        return "[%s, %s, %s, %s, %s, %s, %s]".formatted(buttons, getTargetChannelShortString(), diceParserSystem, alwaysSumResult, getAnswerFormatType(), getDiceStyleAndColor(), statusAndAnswerType);
    }

    @Override
    public String toCommandOptionsString() {
        String buttons = labelAndExpression.stream()
                .map(b -> {
                    if (b.getDiceExpression().equals(b.getLabel())) {
                        return b.getDiceExpression();
                    }
                    return "%s@%s".formatted(b.getDiceExpression(), b.getLabel());
                })
                .collect(Collectors.joining(";"));
        return "%s: %s %s: %s %s: %s %s".formatted(SumCustomSetCommand.BUTTONS_COMMAND_OPTIONS_NAME, String.join(" ", buttons),
                SumCustomSetCommand.ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_NAME, alwaysSumResult,
                SumCustomSetCommand.HIDE_EXPRESSION_IN_ANSWER, hideExpressionInStatusAndAnswer,
                super.toCommandOptionsString());
    }
}
