package org.example.view;

import org.example.controller.MonitoringController;
import org.example.model.OrderStatus;
import org.example.model.SampleStockInfo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public final class MonitoringView {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final MonitoringController monitoringController;

    public MonitoringView(MonitoringController monitoringController) {
        this.monitoringController = monitoringController;
    }

    public void run() {
        while (true) {
            ConsoleHelper.clearScreen();
            ConsoleHelper.println("");
            ConsoleHelper.printHeader("[4] 모니터링  " + LocalDateTime.now().format(FMT));
            ConsoleHelper.println("  [1] 주문량 확인    [2] 재고량 확인    [0] 위로");
            ConsoleHelper.printThinLine();

            String input = ConsoleHelper.readLine("선택 > ");
            switch (input) {
                case "1" -> showOrderSummary();
                case "2" -> showStockStatus();
                case "0" -> { return; }
                default  -> ConsoleHelper.println("  잘못된 입력입니다.");
            }
        }
    }

    private void showOrderSummary() {
        Map<OrderStatus, Integer> summary = monitoringController.getOrderSummary();
        int total = summary.values().stream().mapToInt(Integer::intValue).sum();

        ConsoleHelper.println("");
        ConsoleHelper.println("[4] 모니터링 > 주문량 확인");
        ConsoleHelper.printTableTop();
        ConsoleHelper.println("│  " + ConsoleHelper.padRight("상태별 주문 현황", 116) + "│");
        ConsoleHelper.printTableDivider();

        for (Map.Entry<OrderStatus, Integer> entry : summary.entrySet()) {
            String suffix = entry.getKey() == OrderStatus.PRODUCING
                ? AnsiColor.color("  ← 생산라인 대기", AnsiColor.MAGENTA) : "";
            int badgeLen = entry.getKey().name().length() + 2;
            int suffixLen = entry.getKey() == OrderStatus.PRODUCING ? 16 : 0;
            int trailing = 118 - (2 + badgeLen + 1 + 6 + suffixLen);
            ConsoleHelper.println("│  " + AnsiColor.statusBadge(entry.getKey())
                + String.format(" %4d건", entry.getValue()) + suffix
                + " ".repeat(trailing) + "│");
        }

        ConsoleHelper.printTableDivider();
        ConsoleHelper.println("│  " + ConsoleHelper.padRight("합계", 5)
            + String.format(" %4d건  (REJECTED 제외)", total)
            + " ".repeat(88) + "│");
        ConsoleHelper.printTableBottom();
        ConsoleHelper.println("");
        ConsoleHelper.println(AnsiColor.color(
            String.format("  * 거절된 주문  %d건  (참고용)", monitoringController.getRejectedCount()),
            AnsiColor.WARN));
        ConsoleHelper.readLine("\n  [0] 위로 > ");
    }

    private void showStockStatus() {
        List<SampleStockInfo> stockList = monitoringController.getStockStatus();

        ConsoleHelper.println("");
        ConsoleHelper.println("[4] 모니터링 > 재고량 확인");
        ConsoleHelper.printTableTop();
        ConsoleHelper.println("│  " + ConsoleHelper.padRight("시료명", 38)
            + " " + ConsoleHelper.padRight("재고", 9)
            + " " + ConsoleHelper.padRight("미처리주문", 11)
            + "  " + ConsoleHelper.padRight("상태", 4)
            + "  " + ConsoleHelper.padRight("잔여율", 18)
            + " ".repeat(30) + "│");
        ConsoleHelper.printTableDivider();

        for (SampleStockInfo info : stockList) {
            int stock = info.getSample().getStock();
            int pending = info.getPendingQuantity();
            int total = stock + pending;
            int ratio = total == 0 ? 100 : (int) (stock * 100.0 / total);

            String dataLine = String.format("  %s %6d ea %8d ea  %-10s  %s  %3d%%",
                ConsoleHelper.padRight(info.getSample().getName(), 38),
                stock, pending,
                AnsiColor.stockStatusColored(info.getStatus()),
                ConsoleHelper.progressBar(ratio),
                ratio);
            ConsoleHelper.println("│" + dataLine + " ".repeat(30) + "│");
        }

        ConsoleHelper.printTableBottom();
        ConsoleHelper.println("");
        ConsoleHelper.println(AnsiColor.color("  * 미처리 주문 = CONFIRMED + PRODUCING 상태 주문 수량 합계", AnsiColor.WARN));
        ConsoleHelper.println(AnsiColor.color("  * 잔여율 = 재고 / (재고 + 미처리 주문) × 100", AnsiColor.WARN));
        ConsoleHelper.readLine("\n  [0] 위로 > ");
    }
}
