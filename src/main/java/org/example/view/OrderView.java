package org.example.view;

import org.example.controller.OrderController;
import org.example.controller.SampleController;
import org.example.model.Order;
import org.example.model.Sample;

import java.util.Optional;

public final class OrderView {

    private final OrderController orderController;
    private final SampleController sampleController;

    public OrderView(OrderController orderController, SampleController sampleController) {
        this.orderController = orderController;
        this.sampleController = sampleController;
    }

    public void run() {
        ConsoleHelper.clearScreen();
        ConsoleHelper.println("");
        ConsoleHelper.printHeader("[2] 시료 주문");
        placeOrder();
    }

    private void placeOrder() {
        ConsoleHelper.printThinLine();

        String sampleId = ConsoleHelper.readLine("시료 ID   > ");
        Optional<Sample> sampleOpt = sampleController.findAll().stream()
            .filter(s -> s.getSampleId().equals(sampleId))
            .findFirst();

        if (sampleOpt.isEmpty()) {
            ConsoleHelper.println("");
            ConsoleHelper.println("  [오류] 등록되지 않은 시료 ID입니다: " + sampleId);
            return;
        }

        String customerName = ConsoleHelper.readLine("고객명    > ");

        int quantity;
        while (true) {
            quantity = ConsoleHelper.readInt("주문 수량 > ");
            if (quantity > 0) break;
            ConsoleHelper.println("  [오류] 수량은 1 이상이어야 합니다.");
        }

        Sample sample = sampleOpt.get();
        ConsoleHelper.println("");
        ConsoleHelper.println("입력 내용 확인");
        ConsoleHelper.printThinLine();
        System.out.printf("  시료      %s (%s)%n", sample.getName(), sample.getSampleId());
        System.out.printf("  고객      %s%n", customerName);
        System.out.printf("  수량      %d ea%n", quantity);
        ConsoleHelper.println("");

        String confirm = ConsoleHelper.readLine("[Y] 예약 접수    [N] 취소\n선택 > ");
        if (!confirm.equalsIgnoreCase("Y")) {
            ConsoleHelper.println("");
            ConsoleHelper.println("  주문이 취소되었습니다.");
            return;
        }

        Order order = orderController.placeOrder(sampleId, customerName, quantity);
        ConsoleHelper.println("");
        ConsoleHelper.println("예약 접수 완료.");
        System.out.printf("  주문번호   %s%n", order.getOrderId());
        System.out.printf("  현재 상태  %s%n", order.getStatus());
        ConsoleHelper.println("");
        ConsoleHelper.println("  ※ 재고 확인 및 승인은 [3] 주문 승인/거절 메뉴에서 진행하세요.");
    }
}
