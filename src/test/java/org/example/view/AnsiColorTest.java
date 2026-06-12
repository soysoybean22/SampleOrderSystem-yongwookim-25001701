package org.example.view;

import org.example.model.OrderStatus;
import org.example.model.StockStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnsiColorTest {

    @Test
    @DisplayName("color()는 ANSI 코드로 텍스트를 감싸고 RESET으로 닫는다")
    void color_텍스트를_ANSI코드로_감싼다() {
        String result = AnsiColor.color("hello", AnsiColor.GREEN);
        assertEquals("\033[32mhello\033[0m", result);
    }

    @Test
    @DisplayName("statusBadge()는 CONFIRMED에 초록 굵게를 적용한다")
    void statusBadge_CONFIRMED는_초록굵게() {
        String badge = AnsiColor.statusBadge(OrderStatus.CONFIRMED);
        assertTrue(badge.contains("[CONFIRMED]"));
        assertTrue(badge.contains(AnsiColor.GREEN));
    }

    @Test
    @DisplayName("statusBadge()는 REJECTED에 빨강을 적용한다")
    void statusBadge_REJECTED는_빨강() {
        String badge = AnsiColor.statusBadge(OrderStatus.REJECTED);
        assertTrue(badge.contains("[REJECTED]"));
        assertTrue(badge.contains(AnsiColor.RED));
    }

    @Test
    @DisplayName("stockStatusColored()는 고갈에 빨강을 적용한다")
    void stockStatusColored_고갈은_빨강() {
        String result = AnsiColor.stockStatusColored(StockStatus.고갈);
        assertTrue(result.contains("고갈"));
        assertTrue(result.contains(AnsiColor.RED));
    }

    @Test
    @DisplayName("menuNum()은 번호를 대괄호로 감싸고 청록 굵게를 적용한다")
    void menuNum_청록굵게() {
        String result = AnsiColor.menuNum("1");
        assertTrue(result.contains("[1]"));
        assertTrue(result.contains(AnsiColor.CYAN));
    }
}
