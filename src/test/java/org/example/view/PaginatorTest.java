package org.example.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaginatorTest {

    private List<Integer> tenItems() {
        return List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    @DisplayName("currentItems()는 첫 페이지의 5건을 반환한다")
    void currentItems_첫페이지_5건() {
        Paginator<Integer> paginator = new Paginator<>(tenItems());

        List<Integer> result = paginator.currentItems();

        assertEquals(List.of(1, 2, 3, 4, 5), result);
    }

    @Test
    @DisplayName("nextPage() 후 currentItems()는 다음 5건을 반환한다")
    void nextPage_후_다음항목() {
        Paginator<Integer> paginator = new Paginator<>(tenItems());
        paginator.nextPage();

        List<Integer> result = paginator.currentItems();

        assertEquals(List.of(6, 7, 8, 9, 10), result);
    }

    @Test
    @DisplayName("hasNext()는 다음 페이지가 있으면 true를 반환한다")
    void hasNext_다음페이지_있으면_true() {
        Paginator<Integer> paginator = new Paginator<>(tenItems());

        assertTrue(paginator.hasNext());
    }

    @Test
    @DisplayName("hasNext()는 마지막 페이지에서 false를 반환한다")
    void hasNext_마지막페이지_false() {
        Paginator<Integer> paginator = new Paginator<>(tenItems());
        paginator.nextPage();

        assertFalse(paginator.hasNext());
    }

    @Test
    @DisplayName("hasPrev()는 첫 페이지에서 false를 반환한다")
    void hasPrev_첫페이지_false() {
        Paginator<Integer> paginator = new Paginator<>(tenItems());

        assertFalse(paginator.hasPrev());
    }

    @Test
    @DisplayName("hasPrev()는 두 번째 페이지에서 true를 반환한다")
    void hasPrev_두번째페이지_true() {
        Paginator<Integer> paginator = new Paginator<>(tenItems());
        paginator.nextPage();

        assertTrue(paginator.hasPrev());
    }

    @Test
    @DisplayName("totalPages()는 올바른 총 페이지 수를 반환한다")
    void totalPages_올바른_계산() {
        assertEquals(2, new Paginator<>(tenItems()).totalPages());
        assertEquals(1, new Paginator<>(List.of(1, 2, 3)).totalPages());
        assertEquals(2, new Paginator<>(List.of(1, 2, 3, 4, 5, 6)).totalPages());
    }

    @Test
    @DisplayName("needsPagination()은 5건 이하에서 false를 반환한다")
    void needsPagination_5건이하_false() {
        assertFalse(new Paginator<>(List.of(1, 2, 3, 4, 5)).needsPagination());
    }

    @Test
    @DisplayName("needsPagination()은 6건 이상에서 true를 반환한다")
    void needsPagination_6건이상_true() {
        assertTrue(new Paginator<>(List.of(1, 2, 3, 4, 5, 6)).needsPagination());
    }

    @Test
    @DisplayName("nextPage()는 마지막 페이지에서 페이지를 변경하지 않는다")
    void nextPage_마지막페이지_변경없음() {
        Paginator<Integer> paginator = new Paginator<>(tenItems());
        paginator.nextPage();
        paginator.nextPage(); // 마지막 페이지 초과 시도

        assertEquals(List.of(6, 7, 8, 9, 10), paginator.currentItems());
    }
}
