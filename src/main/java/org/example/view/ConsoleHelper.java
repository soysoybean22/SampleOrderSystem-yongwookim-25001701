package org.example.view;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public final class ConsoleHelper {

    private static final Scanner SCANNER = new Scanner(System.in, StandardCharsets.UTF_8);
    private static final String SEPARATOR = "=".repeat(64);
    private static final String THIN_LINE = "-".repeat(64);

    private ConsoleHelper() {}

    public static String readLine(String prompt) {
        System.out.print(prompt);
        return SCANNER.nextLine().trim();
    }

    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("  숫자를 입력해주세요.");
            }
        }
    }

    public static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("  숫자를 입력해주세요.");
            }
        }
    }

    public static void printSeparator() {
        System.out.println(SEPARATOR);
    }

    public static void printThinLine() {
        System.out.println(THIN_LINE);
    }

    public static void printHeader(String title) {
        System.out.println(SEPARATOR);
        System.out.println("  " + title);
        System.out.println(SEPARATOR);
    }

    public static void println(String message) {
        System.out.println(message);
    }

    public static void println() {
        System.out.println();
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void printBanner() {
        String art = """
              ____       ____                 _
             / ___|     / ___|  ___ _ __ ___ (_)
             \\___ \\ ____\\___ \\ / _ \\ '_ ` _ \\| |
              ___) |_____|__) |  __/ | | | | | |
             |____/     |____/ \\___|_| |_| |_|_|
            """;
        System.out.println(AnsiColor.color(art, AnsiColor.CYAN + AnsiColor.BOLD));
        System.out.println("        반도체 시료 생산주문관리 시스템");
        printSeparator();
    }
}
