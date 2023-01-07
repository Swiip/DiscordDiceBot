package de.janno.discord.bot.dice;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.RollAnswer;
import de.janno.discord.bot.dice.image.ImageResultCreator;
import de.janno.evaluator.dice.DiceEvaluator;
import de.janno.evaluator.dice.ExpressionException;
import de.janno.evaluator.dice.Roll;
import de.janno.evaluator.dice.RollElement;
import de.janno.evaluator.dice.random.NumberSupplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Slf4j
public class DiceEvaluatorAdapter {

    private final static ImageResultCreator IMAGE_RESULT_CREATOR = new ImageResultCreator();
    private final DiceEvaluator diceEvaluator;

    public DiceEvaluatorAdapter(NumberSupplier numberSupplier, int maxNumberOfDice) {
        this.diceEvaluator = new DiceEvaluator(numberSupplier, maxNumberOfDice);
    }

    private static @NonNull String getExpressionFromExpressionWithOptionalLabel(String expressionWithOptionalLabel, String labelDelimiter) {
        if (expressionWithOptionalLabel.contains(labelDelimiter)) {
            int firstDelimiter = expressionWithOptionalLabel.indexOf(labelDelimiter);
            return expressionWithOptionalLabel.substring(0, firstDelimiter);
        }
        return expressionWithOptionalLabel;
    }

    private static Optional<String> getLabelFromExpressionWithOptionalLabel(String expressionWithOptionalLabel, String labelDelimiter) {
        if (expressionWithOptionalLabel.contains(labelDelimiter)) {
            int firstDelimiter = expressionWithOptionalLabel.indexOf(labelDelimiter);
            String label = expressionWithOptionalLabel.substring(firstDelimiter + labelDelimiter.length());
            if (label.length() > 0) {
                return Optional.of(label);
            }
        }
        return Optional.empty();
    }

    private static String getResult(Roll roll, boolean sumUp) {
        if (sumUp && allElementsAreIntegers(roll) && allElementsHaveNoColor(roll)) {
            return String.valueOf(roll.getElements().stream().flatMap(r -> r.asInteger().stream()).mapToInt(i -> i).sum());
        }
        return roll.getResultString();
    }

    private static boolean allElementsAreIntegers(Roll roll) {
        return roll.getElements().stream().allMatch(r -> r.asInteger().isPresent());
    }

    private static boolean allElementsHaveNoColor(Roll roll) {
        return roll.getElements().stream().allMatch(r -> RollElement.NO_COLOR.equals(r.getColor()));
    }

    public static String getHelp() {
        return "```\n" + DiceEvaluator.getHelpText() + "\n```";
    }

    public Optional<String> validateDiceExpression(String expression, String helpCommand) {
        try {
            log.debug("Validating expression: {}", expression);
            diceEvaluator.evaluate(expression);
            return Optional.empty();
        } catch (ExpressionException | ArithmeticException e) {
            return Optional.of(String.format("The following expression is invalid: '%s'. The error is: %s. Use %s to get more information on how to use the command.", expression, e.getMessage(), helpCommand));
        }
    }

    public RollAnswer answerRollWithOptionalLabelInExpression(String expression, String labelDelimiter, boolean sumUp, AnswerFormatType answerFormatType, ResultImage resultImage) {
        String diceExpression = getExpressionFromExpressionWithOptionalLabel(expression, labelDelimiter);
        String label = getLabelFromExpressionWithOptionalLabel(expression, labelDelimiter).orElse(null);
        return answerRollWithGivenLabel(diceExpression, label, sumUp, answerFormatType, resultImage);
    }

    public RollAnswer answerRollWithGivenLabel(String diceExpression, @Nullable String label, boolean sumUp, AnswerFormatType answerFormatType, ResultImage resultImage) {

        try {
            log.debug("Roll expression: {}", diceExpression);
            List<Roll> rolls = diceEvaluator.evaluate(diceExpression);
            File diceImage = null;
            if (resultImage.equals(ResultImage.polyhedral_black_and_gold)) {
                diceImage = IMAGE_RESULT_CREATOR.getImageForRoll(rolls);
            }
            if (rolls.size() == 1) {
                return RollAnswer.builder()
                        .answerFormatType(answerFormatType)
                        .expression(diceExpression)
                        .expressionLabel(label)
                        .file(diceImage)
                        .result(getResult(rolls.get(0), sumUp))
                        .rollDetails(rolls.get(0).getRandomElementsString())
                        .build();
            } else {
                List<RollAnswer.RollResults> multiRollResults = rolls.stream()
                        .map(r -> new RollAnswer.RollResults(r.getExpression(), getResult(r, sumUp), r.getRandomElementsString()))
                        .collect(ImmutableList.toImmutableList());
                return RollAnswer.builder()
                        .answerFormatType(answerFormatType)
                        .expression(diceExpression)
                        .expressionLabel(label)
                        .multiRollResults(multiRollResults)
                        .build();
            }
        } catch (ExpressionException e) {
            return RollAnswer.builder()
                    .answerFormatType(answerFormatType)
                    .expression(diceExpression)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }


    public boolean validExpression(String expression) {
        try {
            log.debug("check if valid: {}", expression);
            diceEvaluator.evaluate(expression);
            return true;
        } catch (ExpressionException | ArithmeticException e) {
            return false;
        }
    }
}
