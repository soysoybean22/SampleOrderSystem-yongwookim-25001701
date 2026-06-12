package org.example.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

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
}
