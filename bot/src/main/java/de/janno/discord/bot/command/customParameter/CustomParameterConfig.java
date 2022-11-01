package de.janno.discord.bot.command.customParameter;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.dice.DiceParserSystem;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Getter
@ToString
public class CustomParameterConfig extends Config {
    @NonNull
    private final String baseExpression;
    @NonNull
    private final DiceParserSystem diceParserSystem;

    public CustomParameterConfig(
            @JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
            @JsonProperty("baseExpression") @NonNull String baseExpression,
            @JsonProperty("diceParserSystem") DiceParserSystem diceParserSystem) {
        super(answerTargetChannelId);
        this.baseExpression = baseExpression;
        this.diceParserSystem = diceParserSystem == null ? DiceParserSystem.DICEROLL_PARSER : diceParserSystem;
    }

    @Override
    public String toShortString() {
        return "[%s, %s, %s]".formatted(baseExpression, getTargetChannelShortString(), diceParserSystem);
    }
}
