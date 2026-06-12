package org.example.view;

import java.util.List;

public final class Paginator<T> {

    public static final int PAGE_SIZE = 5;

    private final List<T> items;
    private int page = 0;

    public Paginator(List<T> items) {
        this.items = List.copyOf(items);
    }

    public List<T> currentItems() {
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, items.size());
        return items.subList(from, to);
    }

    public boolean hasNext()  { return (page + 1) * PAGE_SIZE < items.size(); }
    public boolean hasPrev()  { return page > 0; }
    public void nextPage()    { if (hasNext()) page++; }
    public void prevPage()    { if (hasPrev()) page--; }
    public int totalPages()   { return (int) Math.ceil((double) items.size() / PAGE_SIZE); }
    public int currentPage()  { return page + 1; }
    public boolean needsPagination() { return items.size() > PAGE_SIZE; }

    public String pageInfo() {
        return AnsiColor.color(
            String.format("페이지 %d / %d", currentPage(), totalPages()),
            AnsiColor.CYAN);
    }

    public void printNavBar() {
        String prev = hasPrev() ? AnsiColor.color("◀ 이전 [P]", AnsiColor.CYAN) : "          ";
        String next = hasNext() ? AnsiColor.color("다음 [N] ▶", AnsiColor.CYAN) : "          ";
        ConsoleHelper.println("");
        ConsoleHelper.println("  " + prev + "    " + pageInfo() + "    " + next);
    }
}
