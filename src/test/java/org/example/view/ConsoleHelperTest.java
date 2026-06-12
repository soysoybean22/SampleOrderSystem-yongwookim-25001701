package org.example.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleHelperTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        outContent.reset();
    }

    @Test
    @DisplayName("clearScreen()은 ANSI 화면 클리어 코드를 출력한다")
    void clearScreen_ANSI_클리어코드를_출력한다() {
        ConsoleHelper.clearScreen();

        String output = outContent.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[H\033[2J"));
    }

    @Test
    @DisplayName("printBanner()는 S-Semi ASCII 아트를 출력한다")
    void printBanner_ASCII아트를_출력한다() {
        ConsoleHelper.printBanner();

        String output = outContent.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("____"));
    }

    @Test
    @DisplayName("printBanner()는 시스템 부제를 출력한다")
    void printBanner_시스템_부제를_출력한다() {
        ConsoleHelper.printBanner();

        String output = outContent.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("반도체 시료 생산주문관리 시스템"));
    }

    @Test
    @DisplayName("printBanner()는 CYAN 색상을 적용한다")
    void printBanner_CYAN_색상을_적용한다() {
        ConsoleHelper.printBanner();

        String output = outContent.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains(AnsiColor.CYAN));
    }

    @Test
    @DisplayName("printHeader()는 제목 텍스트에 CYAN 색상을 적용한다")
    void printHeader_제목에_CYAN_색상을_적용한다() {
        ConsoleHelper.printHeader("테스트 헤더");

        String output = outContent.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains(AnsiColor.CYAN));
    }

    @Test
    @DisplayName("printHeader()는 제목 텍스트에 BOLD를 적용한다")
    void printHeader_제목에_BOLD를_적용한다() {
        ConsoleHelper.printHeader("테스트 헤더");

        String output = outContent.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains(AnsiColor.BOLD));
    }

    @Test
    @DisplayName("printHeader()는 제목 텍스트를 포함한다")
    void printHeader_제목_텍스트를_포함한다() {
        ConsoleHelper.printHeader("테스트 헤더");

        String output = outContent.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("테스트 헤더"));
    }

    @Test
    @DisplayName("progressBar(0)은 전체 빈 막대를 RED로 출력한다")
    void progressBar_0_전체빈막대_RED() {
        String result = ConsoleHelper.progressBar(0);

        assertTrue(result.contains("░░░░░░░░░░"));
        assertTrue(result.contains(AnsiColor.RED));
    }

    @Test
    @DisplayName("progressBar(100)은 전체 채워진 막대를 GREEN으로 출력한다")
    void progressBar_100_전체채워짐_GREEN() {
        String result = ConsoleHelper.progressBar(100);

        assertTrue(result.contains("██████████"));
        assertTrue(result.contains(AnsiColor.GREEN));
    }

    @Test
    @DisplayName("progressBar(50)은 절반 채워진 막대를 YELLOW로 출력한다")
    void progressBar_50_절반채워짐_YELLOW() {
        String result = ConsoleHelper.progressBar(50);

        assertTrue(result.contains("█████░░░░░"));
        assertTrue(result.contains(AnsiColor.YELLOW));
    }

    @Test
    @DisplayName("progressBar(60)은 6칸 채워진 막대를 GREEN으로 출력한다")
    void progressBar_60_GREEN_경계값() {
        String result = ConsoleHelper.progressBar(60);

        assertTrue(result.contains("██████░░░░"));
        assertTrue(result.contains(AnsiColor.GREEN));
    }

    @Test
    @DisplayName("progressBar()는 대괄호로 막대를 감싼다")
    void progressBar_대괄호로_감싼다() {
        String result = ConsoleHelper.progressBar(50);

        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }
}
