package org.example.view;

import org.example.controller.OrderController;
import org.example.controller.SampleController;
import org.example.model.ApprovalResult;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.Sample;

import java.util.List;
import java.util.Optional;

public final class ApprovalView {

    private final OrderController orderController;
    private final SampleController sampleController;

    public ApprovalView(OrderController orderController, SampleController sampleController) {
        this.orderController = orderController;
        this.sampleController = sampleController;
    }

    public void run() {
        List<Order> reserved = orderController.findReservedOrders();
        if (reserved.isEmpty()) {
            ConsoleHelper.clearScreen();
            ConsoleHelper.println("");
            ConsoleHelper.printHeader("[3] 주문 승인/거절");
            ConsoleHelper.println("  승인 대기 중인 주문이 없습니다.");
            ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
            return;
        }

        Paginator<Order> paginator = new Paginator<>(reserved);

        while (true) {
            ConsoleHelper.clearScreen();
            ConsoleHelper.println("");
            ConsoleHelper.printHeader("[3] 주문 승인/거절");
            printReservedList(paginator.currentItems(), paginator);

            String prompt = paginator.needsPagination()
                ? "번호 선택 또는 [P/N] 페이지 이동, [0] 위로 > "
                : "승인할 번호 > ";
            String input = ConsoleHelper.readLine(prompt);

            if (input.equals("0")) return;
            if (input.equalsIgnoreCase("N")) { paginator.nextPage(); continue; }
            if (input.equalsIgnoreCase("P")) { paginator.prevPage(); continue; }

            int index;
            try {
                index = Integer.parseInt(input) - 1;
            } catch (NumberFormatException e) {
                ConsoleHelper.println("  잘못된 입력입니다.");
                continue;
            }

            List<Order> current = paginator.currentItems();
            if (index < 0 || index >= current.size()) {
                ConsoleHelper.println("  목록에 없는 번호입니다.");
                continue;
            }

            processApproval(current.get(index));
            return;
        }
    }

    private void printReservedList(List<Order> orders, Paginator<Order> paginator) {
        ConsoleHelper.println("승인 대기 중인 예약 목록 (RESERVED)");
        ConsoleHelper.println("");
        ConsoleHelper.printTableTop();
        System.out.printf("│  %-4s %-22s %-18s %7s      │%n", "번호", "주문번호", "고객명", "수량");
        ConsoleHelper.printTableDivider();
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            String sampleName = sampleController.findAll().stream()
                .filter(s -> s.getSampleId().equals(o.getSampleId()))
                .map(Sample::getName)
                .findFirst().orElse(o.getSampleId());
            System.out.printf("│  [%d]  %-22s %-18s %5d ea     │%n",
                i + 1, o.getOrderId(), o.getCustomerName(), o.getQuantity());
            System.out.printf("│        시료: %-50s│%n", sampleName);
        }
        System.out.printf("│  %-60s│%n", "[0]  위로");
        ConsoleHelper.printTableBottom();
        if (paginator.needsPagination()) {
            paginator.printNavBar();
        }
    }

    private void processApproval(Order order) {
        Optional<Sample> sampleOpt = sampleController.findAll().stream()
            .filter(s -> s.getSampleId().equals(order.getSampleId()))
            .findFirst();

        if (sampleOpt.isEmpty()) {
            ConsoleHelper.println("  [오류] 시료 정보를 찾을 수 없습니다.");
            ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
            return;
        }

        Sample sample = sampleOpt.get();
        int stock = sample.getStock();
        int quantity = order.getQuantity();

        ConsoleHelper.println("");
        ConsoleHelper.println("재고 확인 중...");
        ConsoleHelper.println("");
        System.out.printf("  시료      %s (%s)%n", sample.getName(), sample.getSampleId());
        System.out.printf("  주문 수량  %d ea%n", quantity);

        if (stock >= quantity) {
            ConsoleHelper.println("  현재 재고  " + stock + " ea    " + AnsiColor.color("← 충분", AnsiColor.SUCCESS));
            ConsoleHelper.println("");
            String confirm = ConsoleHelper.readLine("[Y] 승인 (즉시 출고 대기)    [N] 주문 거절\n선택 > ");
            handleApprovalConfirm(confirm, order, stock, quantity);
        } else {
            int shortage = quantity - stock;
            int actualQty = (int) Math.ceil(shortage / (sample.getYield() * 0.9));
            double totalTime = sample.getAvgProductionTime() * actualQty;

            ConsoleHelper.println("  현재 재고  " + stock + " ea    "
                + AnsiColor.color("← 부족 (" + shortage + " ea 모자람)", AnsiColor.WARN));
            ConsoleHelper.println("");
            ConsoleHelper.println(AnsiColor.color("  재고 부족. 생산이 필요합니다.", AnsiColor.WARN));
            System.out.printf("    부족분      %d ea%n", shortage);
            System.out.printf("    실 생산량   %d ea  (수율 %.2f / 오차 보정 0.9 적용)%n",
                actualQty, sample.getYield());
            System.out.printf("    총 생산시간 %.1f min%n", totalTime);
            ConsoleHelper.println("");
            String confirm = ConsoleHelper.readLine("[Y] 승인 (생산 후 출고)    [N] 주문 거절\n선택 > ");
            handleApprovalConfirm(confirm, order, stock, quantity);
        }
    }

    private void handleApprovalConfirm(String confirm, Order order, int stock, int quantity) {
        ConsoleHelper.println("");
        if (confirm.equalsIgnoreCase("Y")) {
            ApprovalResult result = orderController.approveOrder(order.getOrderId());
            if (result.isConfirmed()) {
                ConsoleHelper.println(AnsiColor.color("✓ 승인 완료.", AnsiColor.SUCCESS));
                ConsoleHelper.println("  상태 변경  "
                    + AnsiColor.statusBadge(OrderStatus.RESERVED) + " → " + AnsiColor.statusBadge(OrderStatus.CONFIRMED));
                System.out.printf("  주문번호   %s%n", result.getOrder().getOrderId());
                System.out.printf("  재고 차감  %d ea → %d ea%n", stock, stock - quantity);
            } else {
                ConsoleHelper.println(AnsiColor.color("✓ 승인 완료.", AnsiColor.SUCCESS));
                ConsoleHelper.println("  상태 변경  "
                    + AnsiColor.statusBadge(OrderStatus.RESERVED) + " → " + AnsiColor.statusBadge(OrderStatus.PRODUCING));
                System.out.printf("  주문번호   %s%n", result.getOrder().getOrderId());
                ConsoleHelper.println(AnsiColor.color("  생산 큐에 등록되었습니다.", AnsiColor.WARN));
            }
        } else {
            Order rejected = orderController.rejectOrder(order.getOrderId());
            ConsoleHelper.println(AnsiColor.color("✗ 거절 완료.", AnsiColor.ERROR));
            ConsoleHelper.println("  상태 변경  "
                + AnsiColor.statusBadge(OrderStatus.RESERVED) + " → " + AnsiColor.statusBadge(OrderStatus.REJECTED));
            System.out.printf("  주문번호   %s%n", rejected.getOrderId());
        }
        ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
    }
}
