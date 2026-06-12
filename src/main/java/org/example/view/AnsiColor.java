package org.example.view;

import org.example.model.OrderStatus;
import org.example.model.StockStatus;

public final class AnsiColor {

    // ── 기본 색상 ──────────────────────────────────────────────
    public static final String RESET   = "\033[0m";
    public static final String BOLD    = "\033[1m";
    public static final String RED     = "\033[31m";
    public static final String GREEN   = "\033[32m";
    public static final String YELLOW  = "\033[33m";
    public static final String BLUE    = "\033[34m";
    public static final String MAGENTA = "\033[35m";
    public static final String CYAN    = "\033[36m";

    // ── 용도별 조합 상수 ───────────────────────────────────────
    public static final String SUCCESS  = GREEN + BOLD;    // ✓ 완료/성공
    public static final String ERROR    = RED + BOLD;      // ✗ 오류/실패
    public static final String WARN     = YELLOW;          // ※ 안내/경고
    public static final String MENU_NUM = CYAN + BOLD;     // [0]~[6] 메뉴 번호
    public static final String HEADER   = CYAN + BOLD;     // 헤더/제목
    public static final String NUMBER   = BOLD;            // 주요 숫자값

    private AnsiColor() {}

    // ── 핵심 메서드 ────────────────────────────────────────────

    public static String color(String text, String ansiCode) {
        return ansiCode + text + RESET;
    }

    public static String statusBadge(OrderStatus status) {
        return switch (status) {
            case RESERVED  -> color("[RESERVED]",  YELLOW);
            case CONFIRMED -> color("[CONFIRMED]", SUCCESS);
            case PRODUCING -> color("[PRODUCING]", MAGENTA + BOLD);
            case RELEASE   -> color("[RELEASE]",   CYAN + BOLD);
            case REJECTED  -> color("[REJECTED]",  ERROR);
        };
    }

    public static String stockStatusColored(StockStatus status) {
        return switch (status) {
            case 여유 -> color("여유", SUCCESS);
            case 부족 -> color("부족", YELLOW + BOLD);
            case 고갈 -> color("고갈", ERROR);
        };
    }

    public static String menuNum(String num) {
        return color("[" + num + "]", MENU_NUM);
    }

    public static String boldNum(String value) {
        return color(value, NUMBER);
    }
}
