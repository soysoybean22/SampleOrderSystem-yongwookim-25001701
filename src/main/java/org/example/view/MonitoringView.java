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
        ConsoleHelper.printThinLine();
        ConsoleHelper.println("  상태별 주문 현황");
        ConsoleHelper.println("");

        for (Map.Entry<OrderStatus, Integer> entry : summary.entrySet()) {
            String suffix = entry.getKey() == OrderStatus.PRODUCING ? "  ← 생산라인 대기" : "";
            System.out.printf("  %-12s %4d건%s%n", entry.getKey(), entry.getValue(), suffix);
        }

        ConsoleHelper.println("  ─────────────────────");
        System.out.printf("  %-12s %4d건  (REJECTED 제외)%n", "합계", total);
    }

    private void showStockStatus() {
        List<SampleStockInfo> stockList = monitoringController.getStockStatus();

        ConsoleHelper.println("");
        ConsoleHelper.println("[4] 모니터링 > 재고량 확인");
        ConsoleHelper.printThinLine();
        ConsoleHelper.println("  시료별 재고 현황");
        ConsoleHelper.println("");
        System.out.printf("  %-24s %8s %10s  %-4s  %5s%n",
            "시료명", "재고", "미처리주문", "상태", "잔여율");
        ConsoleHelper.printThinLine();

        for (SampleStockInfo info : stockList) {
            int stock = info.getSample().getStock();
            int pending = info.getPendingQuantity();
            int total = stock + pending;
            int ratio = total == 0 ? 0 : (int) (stock * 100.0 / total);

            System.out.printf("  %-24s %6d ea %8d ea  %-4s  %4d%%%n",
                info.getSample().getName(),
                stock, pending,
                info.getStatus(),
                ratio);
        }

        ConsoleHelper.println("");
        ConsoleHelper.println("  * 미처리 주문 = CONFIRMED + PRODUCING 상태 주문 수량 합계");
        ConsoleHelper.println("  * 잔여율 = 재고 / (재고 + 미처리 주문) × 100");
    }
}
