package org.example.view;

import org.example.controller.ReleaseController;
import org.example.controller.SampleController;
import org.example.model.Order;
import org.example.model.OrderStatus;
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
        List<Order> confirmed = releaseController.findConfirmedOrders();
        if (confirmed.isEmpty()) {
            ConsoleHelper.clearScreen();
            ConsoleHelper.println("");
            ConsoleHelper.printHeader("[6] 출고 처리");
            ConsoleHelper.println("  출고 가능한 주문이 없습니다.");
            ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
            return;
        }

        Paginator<Order> paginator = new Paginator<>(confirmed);

        while (true) {
            ConsoleHelper.clearScreen();
            ConsoleHelper.println("");
            ConsoleHelper.printHeader("[6] 출고 처리");
            printConfirmedList(paginator.currentItems(), paginator);

            String prompt = paginator.needsPagination()
                ? "                                     번호 선택 또는 [P/N] 페이지 이동, [0] 위로 > "
                : "                                                     출고할 번호 > ";
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

            processRelease(current.get(index));
            return;
        }
    }

    private void printConfirmedList(List<Order> orders, Paginator<Order> paginator) {
        ConsoleHelper.println("출고 가능 주문 (CONFIRMED)");
        ConsoleHelper.println("");
        ConsoleHelper.printTableTop();
        ConsoleHelper.println("│  " + ConsoleHelper.padRight("번호", 5)
            + ConsoleHelper.padRight("주문번호", 22)
            + " " + ConsoleHelper.padRight("시료명", 36)
            + " " + ConsoleHelper.padRight("고객명", 36)
            + " " + ConsoleHelper.padRight("수량", 8) + "      │");
        ConsoleHelper.printTableDivider();
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            String sampleName = resolveSampleName(o.getSampleId());
            System.out.printf("│  [%d]  %-22s %s %s %5d ea      │%n",
                i + 1, o.getOrderId(),
                ConsoleHelper.padRight(sampleName, 36),
                ConsoleHelper.padRight(o.getCustomerName(), 36),
                o.getQuantity());
        }
        ConsoleHelper.printTableBottom();
        if (paginator.needsPagination()) {
            paginator.printNavBar();
        }
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
            ConsoleHelper.println(AnsiColor.color("  취소되었습니다.", AnsiColor.WARN));
            ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
            return;
        }

        Order released = releaseController.release(order.getOrderId());
        ConsoleHelper.println("");
        ConsoleHelper.println(AnsiColor.color("✓ 출고 처리 완료.", AnsiColor.SUCCESS));
        System.out.printf("  주문번호    %s%n", released.getOrderId());
        ConsoleHelper.println("  상태 변경  "
            + AnsiColor.statusBadge(OrderStatus.CONFIRMED) + " → " + AnsiColor.statusBadge(OrderStatus.RELEASE));
        System.out.printf("  처리 일시   %s%n", LocalDateTime.now().format(FMT));
        ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
    }

    private String resolveSampleName(String sampleId) {
        return sampleController.findAll().stream()
            .filter(s -> s.getSampleId().equals(sampleId))
            .map(Sample::getName)
            .findFirst().orElse(sampleId);
    }
}
