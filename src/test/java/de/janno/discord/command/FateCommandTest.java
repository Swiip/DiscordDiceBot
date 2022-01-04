package de.janno.discord.command;

import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FateCommandTest {

    FateCommand underTest;

    @BeforeEach
    void setup() {
        underTest = new FateCommand(new DiceUtils(1, 2, 3, 1, 2, 3, 1, 2));
    }

    @Test
    void getName() {
        assertThat(underTest.getName()).isEqualTo("fate");
    }

    @Test
    void getButtonMessage_modifier() {
        String res = underTest.getButtonMessage(null, new FateCommand.Config("with_modifier"));

        assertThat(res).isEqualTo("Click a button to roll four fate dice and add the value of the button");
    }

    @Test
    void getButtonMessage_simple() {
        String res = underTest.getButtonMessage(null, new FateCommand.Config("simple"));

        assertThat(res).isEqualTo("Click a button to roll four fate dice");
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("fate,1;2")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("fate")).isFalse();
    }

    @Test
    void getDiceResult_simple() {
        List<DiceResult> res = underTest.getDiceResult("roll", new FateCommand.Config("simple"));

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getResultTitle()).isEqualTo("4dF = -1");
        assertThat(res.get(0).getResultDetails()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getDiceResult_modifier_minus1() {
        List<DiceResult> res = underTest.getDiceResult("-1", new FateCommand.Config("with_modifier"));

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getResultTitle()).isEqualTo("4dF -1 = -2");
        assertThat(res.get(0).getResultDetails()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getDiceResult_modifier_plus1() {
        List<DiceResult> res = underTest.getDiceResult("+1", new FateCommand.Config("with_modifier"));

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getResultTitle()).isEqualTo("4dF +1 = 0");
        assertThat(res.get(0).getResultDetails()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getDiceResult_modifier_0() {
        List<DiceResult> res = underTest.getDiceResult("0", new FateCommand.Config("with_modifier"));

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getResultTitle()).isEqualTo("4dF = -1");
        assertThat(res.get(0).getResultDetails()).isEqualTo("[−,▢,＋,−]");
    }

    @Test
    void getStartOptions() {
        List<ApplicationCommandOptionData> res = underTest.getStartOptions();

        assertThat(res.stream().map(ApplicationCommandOptionData::name)).containsExactly("type");
    }
}