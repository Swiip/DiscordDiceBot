package de.janno.discord.bot.command.help;

import de.janno.discord.bot.command.AbstractCommand;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.dice.image.provider.D6Dotted;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


public class RpgSystemCommandPreset {

    private static final CustomParameterConfig FATE = new CustomParameterConfig(null, "replace(4d[＋,▢,−],'＋',1,'▢',0,'−',-1)+{Modifier:-4<=>10}=", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()));
    private static final CustomParameterConfig FATE_IMAGE = new CustomParameterConfig(null, "4d[-1,0,1]+{Modifier:-4<=>10}=", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.fate, DiceImageStyle.fate.getDefaultColor()));
    private static final CustomDiceConfig DND5 = new CustomDiceConfig(null, string2ButtonIdLabelAndDiceExpression("1d4;1d6;1d8;1d10;1d12;1d20;1d100;2d20k1@D20 Advantage;2d20L1@D20 Disadvantage;2d4=@2d4;2d6=@2d6;2d8=@2d8;2d10=@2d10;2d12=@2d12;2d20=@2d20"), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()));
    private static final CustomDiceConfig DND5_IMAGE = new CustomDiceConfig(null, string2ButtonIdLabelAndDiceExpression("1d4;1d6;1d8;1d10;1d12;1d20;1d100;2d20k1@D20 Advantage;2d20L1@D20 Disadvantage;2d4=@2d4;2d6=@2d6;2d8=@2d8;2d10=@2d10;2d12=@2d12;2d20=@2d20"),
            DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()));
    private static final CustomParameterConfig NWOD = new CustomParameterConfig(null, "{Number of Dice}d!10>=8c", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_knots, DiceImageStyle.polyhedral_knots.getDefaultColor()));
    private static final CustomParameterConfig OWOD = new CustomParameterConfig(null, "val('$diceNumber',{Number of Dice}) val('$target', {Target Number:2<=>10}) val('$reroll', {Reroll on 10:0@No/1@Yes}) val('$roll', if('$reroll'=?0, '$diceNumber'd10,'$diceNumber'd!10)) ('$roll'>='$target'c) - ('$roll'==1c)=", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_knots, DiceImageStyle.polyhedral_knots.getDefaultColor()));
    private static final CustomParameterConfig SHADOWRUN = new CustomParameterConfig(null, "val('$roll',{number of dice:1<=>20}d6) concat('$roll'>4c, if('$roll'==1c >? '$roll'c/2,' - Glitch!'))", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()));
    private static final CustomParameterConfig SHADOWRUN_IMAGE = new CustomParameterConfig(null, "val('$roll',{number of dice:1<=>20}d6) concat('$roll'>4c, if('$roll'==1c >? '$roll'c/2,' - Glitch!'))", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.d6_dots, D6Dotted.BLACK_AND_GOLD));
    private static final CustomDiceConfig COIN = new CustomDiceConfig(null, List.of(new ButtonIdLabelAndDiceExpression("1_button", "Coin Toss \uD83E\uDE99", "1d[Head \uD83D\uDE00/Tail \uD83E\uDD85]")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()));
    private static final SumCustomSetConfig DICE_CALCULATOR = new SumCustomSetConfig(null, string2ButtonIdLabelAndDiceExpression("7;8;9;+;-;4;5;6;d;k;1;2;3;0;l"),
            DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()));
    private static final CustomDiceConfig OSR = new CustomDiceConfig(null,
            string2ButtonIdLabelAndDiceExpression("1d20@D20;1d6@D6;2d6@2D6;1d4@D4;1d8@D8;6x3d6=@Stats;(3d6=)*10@Gold;1d100@D100;1d10@D10;1d12@D12"),
            DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()));
    private static final SumCustomSetConfig TRAVELLER = new SumCustomSetConfig(null,
            string2ButtonIdLabelAndDiceExpression("+2d6;+(3d6k2)@Boon;+(3d6l2)@Bane;+1d6;+1;+2;+3;+4;-1;-2;-3;-4"),
            DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()));
    private static final CustomParameterConfig SAVAGE_WORLDS = new CustomParameterConfig(null, "(d!!{Dice:4@D4/6@D6/8@D8/12@D12/20@D20} + {Type: 0@Regular/1d!!6@Wildcard})k1", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()));
    private static final CustomDiceConfig BLADES_IN_THE_DARK = new CustomDiceConfig(null,
            string2ButtonIdLabelAndDiceExpression("ifE(2d[0/0/0/1/1/3]l1,3,'Success',1,'Partial','Failure')@Zero;ifE(1d[0/0/0/1/1/3],3,'Success',1,'Partial','Failure')@1d6;ifG(2d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@2d6;ifG(3d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@3d6;ifG(4d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@4d6;ifG(5d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@5d6;ifG(6d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@6d6;ifG(7d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@7d6"),
            DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()));
    private static final CustomDiceConfig CALL_OF_CTHULHU_7ED = new CustomDiceConfig(null, string2ButtonIdLabelAndDiceExpression("1d100;2d100L1@1d100 Advantage; 2d100K1@1d100 Penalty; 1d3; 1d4; 1d6; 1d8; 1d10; 1d12; 1d20; 3d6"),
            DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()));
    private static final CustomParameterConfig EXALTED_3ED = new CustomParameterConfig(null, "val('$1', cancel(double({number of dice}d10,10),1,[7/8/9/10])), ifE(('$1'>=7)c,0,ifG(('$1'<=1)c,0,'Botch'))", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()));
    private static final CustomParameterConfig VAMPIRE_5ED = new CustomParameterConfig(null, "val('$r',{regular dice:1<=>16}d10 col 'blue') val('$h',{hunger dice:0<=>5}d10  col 'purple_dark') val('$s',('$r'+'$h')>=6c) val('$rt','$r'==10c) val('$ht','$h'==10c) val('$ho','$h'==1c) val('$2s',((('$rt'+'$ht'=))/2)*2) val('$ts',('$s'+'$2s'=)) concat('successes: ', '$ts', ifE('$ts',0,ifG('$ho',1,' bestial failure' , ''),''), ifE('$rt' mod 2, 1, ifE('$ht' mod 2, 1, ' messy critical', ''), ''))", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_knots, DiceImageStyle.polyhedral_knots.getDefaultColor()));
    private static final CustomParameterConfig HUNTER_5ED = new CustomParameterConfig(null, "val('$r',{Regular D10 Dice:1<=>16}d10 col 'blue') val('$h', {Desperation Dice:0<=>5}d10 col'purple_dark') val('$s',('$r'+'$h')>=6c) val('$rt','$r'==10c) val('$ht','$h'==10c) val('$ho','$h'==1c) val('$2s',((('$rt'+'$ht'=))/2)*2) val('$ts',('$s'+'$2s'=)) concat('successes: ', '$ts', ifE('$ts',0,ifG('$ho',1,' bestial failure' , ''),''), ifE('$rt' mod 2, 1, ifE('$ht' mod 2, 1, ' messy critical', ''), ''))", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_knots, DiceImageStyle.polyhedral_knots.getDefaultColor()));
    private static final CustomParameterConfig ONE_ROLL_ENGINE = new CustomParameterConfig(null, "groupc({Number of Dice:1<=>10}d10+({Number of Extra Die:0@0/10@1/2r10@2/3r10@3/4r10@4})>={Difficulty:1<=>10})", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()));
    private static final SumCustomSetConfig DUNGEON_CRAWL_CLASSICS = new SumCustomSetConfig(null, string2ButtonIdLabelAndDiceExpression("+1d4;+1d6;+1d7;+1d8;+1d10;+1d12;+1d14;+1d16;+1d20;+1d24;+1d16;+1d30;+1d100;+1;+2;+3;+4;-1;-2;-3;-4"),
            DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor()));
    private static final CustomDiceConfig TINY_D6 = new CustomDiceConfig(null, string2ButtonIdLabelAndDiceExpression("ifG(1d6>=5c,0,'Success','Failure')@Disadvantage; ifG(2d6>=5c,0,'Success','Failure')@Test;ifG(3d6>=5c,0,'Success','Failure')@Advantage"),
            DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()));
    private static final CustomParameterConfig CYBERPUNK_RED = new CustomParameterConfig(null, "val('$roll', 1d10) ifE('$roll', 1, '$roll'-1d10, 10, '$roll'+1d10, '$roll')+{ability:0<=>9}+{skill:0<=>9}=", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()));
    private static final CustomParameterConfig ASOIAF = new CustomParameterConfig(null, "{numberOfDice:1<=>15}d6k{keep:1<=>10}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()));
    private static final CustomParameterConfig CITY_OF_MIST = new CustomParameterConfig(null, "2d6+{modifier:-6<=>6}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()));
    private static final CustomParameterConfig RISUS = new CustomParameterConfig(null, "val('$r',{numberOfDice:1<=>10}d!6) ('$r'==2c) + ('$r'==4c) + ('$r'==6c)=", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()));
    private static final CustomParameterConfig KIDS_ON_BROOMS = new CustomParameterConfig(null, "(d!!{Dice:4@D4/6@D6/8@D8/12@D12/20@D20/4+d!!4@D4Spell/6+d!!4@D6Spell/8+d!!4@D8Spell/12+d!!4@D12Spell/20+d!!4@D20Spell} {plus or minus:+/-}{Modifier:0<=>15}=)-{Difficulty:1<=>30}=", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()));
    private static final SumCustomSetConfig DND5_CALC = new SumCustomSetConfig(null, string2ButtonIdLabelAndDiceExpression("d4;d6;d8;d10;d12;d20;d100;1;2;3;4;5;k@Keep Highest;L@Keep Lowest;(2d20k1)@D20 Advantage;(2d20L1)@D20 Disadvantage;-@Minus;+@Plus;4d6k3=@Stats;,@Split"), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()));
    private static final CustomDiceConfig REVE_DE_DRAGON = new CustomDiceConfig(null, string2ButtonIdLabelAndDiceExpression("1d4@D4;1d6@D6;2d6=@2D6;1d7@D7;1d8@D8;val('roll',1d!8 col 'special') val('diceCount','roll' c) 'roll'-'diceCount'+7=@DDR;2d10=@2D10;1d12@D12;val('$r',1d12 col 'special'),if('$r'=?1,'vaisseau','$r'=?2,'sirène','$r'=?3,'faucon','$r'=?4,'couronne','$r'=?5,'dragon','$r'=?6,'épées','$r'=?7,'lyre','$r'=?8,'serpent','$r'=?9,'poisson acrobate','$r'=?10,'araignée','$r'=?11,'roseaux','$r'=?12,'château dormant')@DAS;1d20@D20;1d100@D100"),
            DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()));
    private static final CustomDiceConfig PUBLIC_ACCESS = new CustomDiceConfig(null, string2ButtonIdLabelAndDiceExpression(" 2d6=@Roll;3d6k2=@Advantage;3d6l2=@Disadvantage"),
            DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, DiceImageStyle.polyhedral_3d.getDefaultColor()));
    private static final CustomDiceConfig PARANOIA = new CustomDiceConfig(null, string2ButtonIdLabelAndDiceExpression(" asc((0d6 > 4) col 'black' + replace(1d6 > 4, [6], 'Computer!')) @ 0d6 + Computer; asc((1d6 > 4) col 'black' + replace(1d6 > 4, [6], 'Computer!')) @ 1d6 + Computer; asc((2d6 > 4) col 'red' + replace(1d6 > 4, [6], 'Computer!')) @ 2d6 + Computer; asc((3d6 > 4) col 'orange' + replace(1d6 > 4, [6], 'Computer!')) @ 3d6 + Computer; asc((4d6 > 4) col 'yellow' + replace(1d6 > 4, [6], 'Computer!')) @ 4d6 + Computer; asc((5d6 > 4) col 'green' + replace(1d6 > 4, [6], 'Computer!')) @ 5d6 + Computer; asc((6d6 > 4) col 'cyan' + replace(1d6 > 4, [6], 'Computer!')) @ 6d6 + Computer; asc((7d6 > 4) col 'blue' + replace(1d6 > 4, [6], 'Computer!')) @ 7d6 + Computer; asc((8d6 > 4) col 'magenta' + replace(1d6 > 4, [6], 'Computer!')) @ 8d6 + Computer; asc((9d6 > 4) col 'white' + replace(1d6 > 4, [6], 'Computer!')) @ 9d6 + Computer"),
            DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_2d, "grey"));
    private final PersistenceManager persistenceManager;
    private final CustomParameterCommand customParameterCommand;
    private final CustomDiceCommand customDiceCommand;
    private final SumCustomSetCommand sumCustomSetCommand;

    public RpgSystemCommandPreset(PersistenceManager persistenceManager,
                                  CustomParameterCommand customParameterCommand,
                                  CustomDiceCommand customDiceCommand,
                                  SumCustomSetCommand sumCustomSetCommand) {
        this.persistenceManager = persistenceManager;
        this.customParameterCommand = customParameterCommand;
        this.customDiceCommand = customDiceCommand;
        this.sumCustomSetCommand = sumCustomSetCommand;
    }

    private static List<ButtonIdLabelAndDiceExpression> string2ButtonIdLabelAndDiceExpression(String buttons) {
        AtomicLong buttonIdCounter = new AtomicLong(1);
        return Arrays.stream(buttons.split(";"))
                .map(s -> {
                    if (s.contains("@")) {
                        String[] expressionLabel = s.split("@");
                        return new ButtonIdLabelAndDiceExpression(buttonIdCounter.getAndIncrement() + "_button", expressionLabel[1].trim(), expressionLabel[0].trim());
                    }
                    return new ButtonIdLabelAndDiceExpression(buttonIdCounter.getAndIncrement() + "_button", s.trim(), s.trim());

                })
                .collect(Collectors.toList());
    }

    public CommandAndMessageDefinition createMessage(PresetId presetId, UUID newConfigUUID, long guildId, long channelId) {
        return switch (presetId) {
            //custom_parameter start expression: 4d[-1,0,1]+{Modifier:-4<=>10}=
            case FATE -> startPreset(FATE, customParameterCommand, newConfigUUID, guildId, channelId);
            case FATE_IMAGE -> startPreset(FATE_IMAGE, customParameterCommand, newConfigUUID, guildId, channelId);
            case DND5 -> startPreset(DND5, customDiceCommand, newConfigUUID, guildId, channelId);
            case DND5_IMAGE -> startPreset(DND5_IMAGE, customDiceCommand, newConfigUUID, guildId, channelId);
            //custom_parameter start expression: {Number of Dice}d!10>=8c
            case NWOD -> startPreset(NWOD, customParameterCommand, newConfigUUID, guildId, channelId);
            //custom_parameter start expression:val('$diceNumber',{Number of Dice}) val('$target', {Target Number:2<=>10}) val('$reroll', {Reroll on 10:0@No/1@Yes}) val('$roll', if('$reroll'=?0, '$diceNumber'd10,'$diceNumber'd!10)) ('$roll'>='$target'c) - ('$roll'==1c)=
            case OWOD -> startPreset(OWOD, customParameterCommand, newConfigUUID, guildId, channelId);
            //val('$roll',{number of dice:1<=>20}d6) concat('$roll'>4c, if('$roll'==1c >? '$roll'c/2,' - Glitch!'))
            case SHADOWRUN -> startPreset(SHADOWRUN, customParameterCommand, newConfigUUID, guildId, channelId);
            //val('$roll',{number of dice:1<=>20}d6) concat('$roll'>4c, if('$roll'==1c >? '$roll'c/2,' - Glitch!'))
            case SHADOWRUN_IMAGE -> startPreset(SHADOWRUN_IMAGE, customParameterCommand, newConfigUUID, guildId, channelId);
            case COIN -> startPreset(COIN, customDiceCommand, newConfigUUID, guildId, channelId);
            //sum_custom_set start buttons: 7;8;9;+;-;4;5;6;d;k;1;2;3;0;l always_sum_result: true
            case DICE_CALCULATOR ->
                    startPreset(DICE_CALCULATOR, sumCustomSetCommand, newConfigUUID, guildId, channelId);
            //1d20@D20;1d6@D6;2d6@2D6;1d4@D4;1d8@D8;6x3d6=@Stats;(3d6=)*10@Gold;1d100@D100;1d10@D10;1d12@D12
            case OSR -> startPreset(OSR, customDiceCommand, newConfigUUID, guildId, channelId);
            //sum_custom_set start buttons:+2d6;+(3d6k2)@Boon;+(3d6l2)@Bane;+1d6;+1;+2;+3;+4;-1;-2;-3;-4
            case TRAVELLER -> startPreset(TRAVELLER, sumCustomSetCommand, newConfigUUID, guildId, channelId);
            //custom_parameter start expression: (d!!{Dice:4@D4/6@D6/8@D8/12@D12/20@D20} + {Type: 0@Regular/1d!!6@Wildcard})k1
            case SAVAGE_WORLDS -> startPreset(SAVAGE_WORLDS, customParameterCommand, newConfigUUID, guildId, channelId);
            // custom_dice start buttons: ifE(2d[0/0/0/1/1/3]l1,3,'Success',1,'Partial','Failure')@Zero;ifE(1d[0/0/0/1/1/3],3,'Success',1,'Partial','Failure')@1d6;ifG(2d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@2d6;ifG(3d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@3d6;ifG(4d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@4d6;ifG(5d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@5d6;ifG(6d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@6d6;ifG(7d[0/0/0/1/1/3]k2=,5,'Critical',2,'Success',0,'Partial','Failure')@7d6
            case BLADES_IN_THE_DARK ->
                    startPreset(BLADES_IN_THE_DARK, customDiceCommand, newConfigUUID, guildId, channelId);
            //7th Edition Call of Cthulhu: custom_dice start buttons:  1d100; 2d100L1@1d100 Advantage; 2d100K1@1d100 Penalty; 1d3; 1d4; 1d6; 1d8; 1d10; 1d12; 1d20; 3d6
            case CALL_OF_CTHULHU_7ED ->
                    startPreset(CALL_OF_CTHULHU_7ED, customDiceCommand, newConfigUUID, guildId, channelId);
            //Exalted 3rd /custom_parameter start expression: val('$1', cancel(double({number of dice}d10,10),1,[7/8/9/10])), ifE(('$1'>=7)c,0,ifG(('$1'<=1)c,0,'Botch'))
            case EXALTED_3ED -> startPreset(EXALTED_3ED, customParameterCommand, newConfigUUID, guildId, channelId);
            //Vampire 5ed /custom_parameter start expression: val('$r',{regular dice:1<=>16}d10 col 'blue') val('$h',{hunger dice:0<=>5}d10 col 'purple_dark') val('$s',('$r'+'$h')>=6c) val('$rt','$r'==10c) val('$ht','$h'==10c) val('$ho','$h'==1c) val('$2s',((('$rt'+'$ht'=))/2)*2) val('$ts',('$s'+'$2s'=)) concat('successes: ', '$ts', ifE('$ts',0,ifG('$ho',1,' bestial failure' , ''),''), ifE('$rt' mod 2, 1, ifE('$ht' mod 2, 1, ' messy critical', ''), '')) answer_format: without_expression
            case VAMPIRE_5ED -> startPreset(VAMPIRE_5ED, customParameterCommand, newConfigUUID, guildId, channelId);
            //Hunter 5ed /custom_parameter start expression: val('$r',{Regular D10 Dice:1<=>16}d10 col 'blue') val('$h', {Desperation Dice:0<=>5}d10 col'purple_dark') val('$s',('$r'+'$h')>=6c) val('$rt','$r'==10c) val('$ht','$h'==10c) val('$ho','$h'==1c) val('$2s',((('$rt'+'$ht'=))/2)*2) val('$ts',('$s'+'$2s'=)) concat('successes: ', '$ts', ifE('$ts',0,ifG('$ho',1,' bestial failure' , ''),''), ifE('$rt' mod 2, 1, ifE('$ht' mod 2, 1, ' messy critical', ''), '')) answer_format: without_expression dice_image_style: polyhedral_knots
            case HUNTER_5ED -> startPreset(HUNTER_5ED, customParameterCommand, newConfigUUID, guildId, channelId);
            //One-Roll Engine /custom_parameter start expression: groupc({Number of Dice:1<=>10}d10+({Number of Extra Die:0@0/10@1/2r10@2/3r10@3/4r10@4})>={Difficulty:1<=>10})
            case ONE_ROLL_ENGINE ->
                    startPreset(ONE_ROLL_ENGINE, customParameterCommand, newConfigUUID, guildId, channelId);
            //Dungeon Crawl Classics  /sum_custom_set start buttons: 1d4;1d6;1d7;1d8;1d10;1d12;1d14;1d16;1d20;1d24;1d16;1d30;1d100;+1;+2;+3;+4;+5;-1;-2;-3;-4;-5
            case DUNGEON_CRAWL_CLASSICS ->
                    startPreset(DUNGEON_CRAWL_CLASSICS, sumCustomSetCommand, newConfigUUID, guildId, channelId);
            // Tiny D6  /custom_dice start buttons: ifG(1d6>=5c,0,'Success','Failure')@Disadvantage; ifG(2d6>=5c,0,'Success','Failure')@Test;ifG(3d6>=5c,0,'Success','Failure')@Advantage answer_format: without_expression
            case TINY_D6 -> startPreset(TINY_D6, customDiceCommand, newConfigUUID, guildId, channelId);
            //Cyberpunk Red /custom_parameter start expression: <val('$roll', 1d10) ifE('$roll', 1, '$roll'-1d10, 10, '$roll'+1d10, '$roll')+{ability:0<=>9}+{skill:0<=>9}=>
            case CYBERPUNK_RED -> startPreset(CYBERPUNK_RED, customParameterCommand, newConfigUUID, guildId, channelId);
            //A Song of Ice and Fire /custom_parameter start expression: {numberOfDice:1<=>15}d6k{keep:1<=>10}
            case ASOIAF -> startPreset(ASOIAF, customParameterCommand, newConfigUUID, guildId, channelId);
            //City of mist /custom_parameter start expression: 2d6+{modifier:-6<=>6}
            case CITY_OF_MIST -> startPreset(CITY_OF_MIST, customParameterCommand, newConfigUUID, guildId, channelId);
            //Risus The Anything RPG "Evens Up" optional rule /custom_parameter start expression: val('$r',{numberOfDice:1<=>10}d!6) ('$r'==2c) + ('$r'==4c) + ('$r'==6c)=
            case RISUS -> startPreset(RISUS, customParameterCommand, newConfigUUID, guildId, channelId);
            //Kids on Brooms: /custom_parameter start expression: (d!!{Dice:4@D4/6@D6/8@D8/12@D12/20@D20/4+d!!4@D4Spell/6+d!!4@D6Spell/8+d!!4@D8Spell/12+d!!4@D12Spell/20+d!!4@D20Spell} {plus or minus:+/-}{Modifier:0<=>15}=)-{Difficulty:1<=>30}=
            case KIDS_ON_BROOMS ->
                    startPreset(KIDS_ON_BROOMS, customParameterCommand, newConfigUUID, guildId, channelId);
            //Alternate 5e Calculating setup: /sum_custom_set start buttons: d4;d6;d8;d10;d12;d20;d100;1;2;3;4;5;k@Keep Highest;L@Keep Lowest;(2d20k1)@D20 Advantage;(2d20L1)@D20 Disadvantage;-@Minus;+@Plus;4d6k3=@Stats;,@Split;[Muliple dice can be rolled using Number then die type. Plus Minus can be used to add modifiers If adding a modifier to an Advantage or disadvantage roll those buttons must be used. Keep Highest Lowest only work correctly with multiple dice of the same type. If you want to roll dice added together use Plus between each die type. This is a work around to give some guidance. Bot is not supposed to work this way.]@Help always_sum_result: true answer_format: full dice_image_style: polyhedral_alies_v2 dice_image_color: orange_and_silver
            case DND5_CALC -> startPreset(DND5_CALC, sumCustomSetCommand, newConfigUUID, guildId, channelId);
            // Rêve de Dragon /custom_dice start buttons: 1d4@D4;1d6@D6;2d6=@2D6;1d7@D7;1d8@D8;val('roll',1d!8 col 'special') val('diceCount','roll' c) 'roll'-'diceCount'+7=@DDR;2d10=@2D10;1d12@D12;val('$r',1d12 col 'special'),if('$r'=?1,'vaisseau','$r'=?2,'sirène','$r'=?3,'faucon','$r'=?4,'couronne','$r'=?5,'dragon','$r'=?6,'épées','$r'=?7,'lyre','$r'=?8,'serpent','$r'=?9,'poisson acrobate','$r'=?10,'araignée','$r'=?11,'roseaux','$r'=?12,'château dormant')@DAS;1d20@D20;1d100@D100 answer_format: without_expression dice_image_style: polyhedral_RdD
            case REVE_DE_DRAGON -> startPreset(REVE_DE_DRAGON, customDiceCommand, newConfigUUID, guildId, channelId);
            //Public Access (Carved from Brindlewood): /custom_dice start buttons: 2d6=@Roll;3d6k2=@Advantage;3d6l2=@Disadvantage answer_format: full dice_image_style: polyhedral_3d dice_image_color: red_and_white
            case PUBLIC_ACCESS -> startPreset(PUBLIC_ACCESS, customDiceCommand, newConfigUUID, guildId, channelId);
            //custom_dice start buttons:asc((0d6 > 4) col 'black' + replace(1d6 > 4, [6], 'Computer!')) @ 0d6 + Computer; asc((1d6 > 4) col 'black' + replace(1d6 > 4, [6], 'Computer!')) @ 1d6 + Computer; asc((2d6 > 4) col 'red' + replace(1d6 > 4, [6], 'Computer!')) @ 2d6 + Computer; asc((3d6 > 4) col 'orange' + replace(1d6 > 4, [6], 'Computer!')) @ 3d6 + Computer; asc((4d6 > 4) col 'yellow' + replace(1d6 > 4, [6], 'Computer!')) @ 4d6 + Computer; asc((5d6 > 4) col 'green' + replace(1d6 > 4, [6], 'Computer!')) @ 5d6 + Computer; asc((6d6 > 4) col 'cyan' + replace(1d6 > 4, [6], 'Computer!')) @ 6d6 + Computer; asc((7d6 > 4) col 'blue' + replace(1d6 > 4, [6], 'Computer!')) @ 7d6 + Computer; asc((8d6 > 4) col 'magenta' + replace(1d6 > 4, [6], 'Computer!')) @ 8d6 + Computer; asc((9d6 > 4) col 'white' + replace(1d6 > 4, [6], 'Computer!')) @ 9d6 + Computer dice_image_style:polyhedral_2d dice_image_color:gray
            case PARANOIA -> startPreset(PARANOIA, customDiceCommand, newConfigUUID, guildId, channelId);
        };
    }

    private <C extends Config> CommandAndMessageDefinition startPreset(C config, AbstractCommand<C, ?> command, UUID newConfigUUID, long guildId, long channelId) {
        String commandString = "/%s %s".formatted(customDiceCommand.getCommandId(), config.toCommandOptionsString());
        command.createMessageConfig(newConfigUUID, guildId, channelId, config).ifPresent(persistenceManager::saveMessageConfig);
        return new CommandAndMessageDefinition(commandString, command.createNewButtonMessage(newConfigUUID, config));
    }

    @Getter
    @AllArgsConstructor
    public enum PresetId {
        DND5_IMAGE("Dungeon & Dragons 5e with Dice Images", List.of("DnD", "D&D")),
        DND5("Dungeon & Dragons 5e", List.of("DnD", "D&D")),
        DND5_CALC("Dungeon & Dragons 5e Calculator", List.of("DnD", "D&D")),
        NWOD("nWod / Chronicles of Darkness", List.of("World of Darkness")),
        OWOD("oWod / Storyteller System", List.of("World of Darkness")),
        SHADOWRUN("Shadowrun", List.of()),
        SHADOWRUN_IMAGE("Shadowrun with Dice Images", List.of()),
        SAVAGE_WORLDS("Savage Worlds", List.of()),
        FATE_IMAGE("Fate with Dice Images", List.of("Fudge")),
        FATE("Fate", List.of("Fudge")),
        COIN("Coin Toss", List.of()),
        DICE_CALCULATOR("Dice Calculator", List.of()),
        OSR("OSR", List.of("Old School Renaissance")),
        TRAVELLER("Traveller", List.of()),
        BLADES_IN_THE_DARK("Blades in the Dark", List.of()),
        CALL_OF_CTHULHU_7ED("Call of Cthulhu 7th Edition", List.of()),
        EXALTED_3ED("Exalted 3ed", List.of()),
        VAMPIRE_5ED("Vampire 5ed", List.of()),
        HUNTER_5ED("Hunter 5ed", List.of()),
        ONE_ROLL_ENGINE("One-Roll Engine", List.of()),
        DUNGEON_CRAWL_CLASSICS("Dungeon Crawl Classics", List.of()),
        TINY_D6("Tiny D6", List.of()),
        CYBERPUNK_RED("Cyberpunk Red", List.of()),
        ASOIAF("A Song of Ice and Fire", List.of("ASOIAF")),
        CITY_OF_MIST("City of Mist", List.of()),
        RISUS("Risus The Anything RPG \"Evens Up\"", List.of()),
        KIDS_ON_BROOMS("Kids on Brooms", List.of()),
        REVE_DE_DRAGON("Rêve de Dragon", List.of("Reve de Dragon")),
        PARANOIA("Paranoia: Red Clearance Edition", List.of()),
        PUBLIC_ACCESS("Public Access", List.of("Carved from Brindlewood"));
        private final String displayName;
        private final List<String> synonymes;
    }

    @Value
    public static class CommandAndMessageDefinition {
        @NonNull
        String command;
        @NonNull
        EmbedOrMessageDefinition messageDefinition;
    }
}
