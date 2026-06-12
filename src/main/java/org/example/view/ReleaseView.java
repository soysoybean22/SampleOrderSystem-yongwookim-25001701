package org.example.view;

import org.example.controller.ReleaseController;
import org.example.controller.SampleController;
import org.example.model.Order;
import org.example.model.Sample;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ReleaseView {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReleaseController releaseController;
    private final SampleController sampleController;

    public ReleaseView(ReleaseController releaseController, SampleController sampleController) {
        this.releaseController = releaseController;
        this.sampleController = sampleController;
    }

    public void run() {
        ConsoleHelper.println("");
        ConsoleHelper.printHeader("[6] 출고 처리");

        List<Order> confirmed = releaseController.findConfirmedOrders();
        if (confirmed.isEmpty()) {
            ConsoleHelper.println("  출고 가능한 주문이 없습니다.");
            return;
        }

        printConfirmedList(confirmed);

        String input = ConsoleHelper.readLine("출고할 번호 > ");
        if (input.equals("0")) return;

        int index;
        try {
            index = Integer.parseInt(input) - 1;
        } catch (NumberFormatException e) {
            ConsoleHelper.println("  잘못된 입력입니다.");
            return;
        }

        if (index < 0 || index >= confirmed.size()) {
            ConsoleHelper.println("  목록에 없는 번호입니다.");
            return;
        }

        processRelease(confirmed.get(index));
    }

    private void printConfirmedList(List<Order> orders) {
        ConsoleHelper.println("출고 가능 주문 (CONFIRMED)");
        ConsoleHelper.println("");
        System.out.printf("  %-4s %-22s %-22s %-18s %7s%n",
            "번호", "주문번호", "시료명", "고객명", "수량");
        ConsoleHelper.printThinLine();
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            String sampleName = resolveSampleName(o.getSampleId());
            System.out.printf("  [%d]  %-22s %-22s %-18s %5d ea%n",
                i + 1, o.getOrderId(), sampleName, o.getCustomerName(), o.getQuantity());
        }
        ConsoleHelper.println("  [0]  위로");
        ConsoleHelper.printThinLine();
    }

    private void processRelease(Order order) {
        String sampleName = resolveSampleName(order.getSampleId());

        ConsoleHelper.println("");
        ConsoleHelper.println("출고 처리하겠습니까?");
        ConsoleHelper.printThinLine();
        System.out.printf("  주문번호   %s%n", order.getOrderId());
        System.out.printf("  시료        %s (%s)%n", sampleName, order.getSampleId());
        System.out.printf("  고객        %s%n", order.getCustomerName());
        System.out.printf("  수량        %d ea%n", order.getQuantity());
        ConsoleHelper.println("");

        String confirm = ConsoleHelper.readLine("[Y] 확인    [N] 취소\n선택 > ");
        if (!confirm.equalsIgnoreCase("Y")) {
            ConsoleHelper.println("  취소되었습니다.");
            return;
        }

        Order released = releaseController.release(order.getOrderId());
        ConsoleHelper.println("");
        ConsoleHelper.println("출고 처리 완료.");
        System.out.printf("  주문번호    %s%n", released.getOrderId());
        ConsoleHelper.println("  상태 변경   CONFIRMED → RELEASE");
        System.out.printf("  처리 일시   %s%n", LocalDateTime.now().format(FMT));
    }

    private String resolveSampleName(String sampleId) {
        return sampleController.findAll().stream()
            .filter(s -> s.getSampleId().equals(sampleId))
            .map(Sample::getName)
            .findFirst().orElse(sampleId);
    }
}
