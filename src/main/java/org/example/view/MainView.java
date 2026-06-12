package org.example.view;

import org.example.controller.MonitoringController;
import org.example.controller.OrderController;
import org.example.controller.ProductionController;
import org.example.controller.ReleaseController;
import org.example.controller.SampleController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class MainView {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SampleController sampleController;
    private final OrderController orderController;
    private final ProductionController productionController;
    private final MonitoringController monitoringController;
    private final ReleaseController releaseController;
    private final SampleView sampleView;
    private final OrderView orderView;
    private final ApprovalView approvalView;
    private final ProductionView productionView;
    private final MonitoringView monitoringView;
    private final ReleaseView releaseView;

    public MainView(SampleController sampleController,
                    OrderController orderController,
                    ProductionController productionController,
                    MonitoringController monitoringController,
                    ReleaseController releaseController) {
        this.sampleController = sampleController;
        this.orderController = orderController;
        this.productionController = productionController;
        this.monitoringController = monitoringController;
        this.releaseController = releaseController;
        this.sampleView = new SampleView(sampleController);
        this.orderView = new OrderView(orderController, sampleController);
        this.approvalView = new ApprovalView(orderController, sampleController);
        this.productionView = new ProductionView(productionController, sampleController);
        this.monitoringView = new MonitoringView(monitoringController);
        this.releaseView = new ReleaseView(releaseController, sampleController);
    }

    public void run() {
        while (true) {
            productionController.autoCompleteIfReady();
            ConsoleHelper.clearScreen();
            ConsoleHelper.printBanner();
            printSummary();
            printMenu();
            String input = ConsoleHelper.readLine("선택 > ");
            switch (input) {
                case "1" -> sampleView.run();
                case "2" -> orderView.run();
                case "3" -> approvalView.run();
                case "4" -> monitoringView.run();
                case "6" -> releaseView.run();
                case "5" -> productionView.run();
                case "0" -> {
                    ConsoleHelper.println("  시스템을 종료합니다.");
                    return;
                }
                default -> ConsoleHelper.println("  잘못된 입력입니다. 다시 선택해주세요.");
            }
        }
    }

    private void printSummary() {
        int sampleCount     = sampleController.findAll().size();
        int totalStock      = sampleController.findAll().stream().mapToInt(s -> s.getStock()).sum();
        int orderCount      = orderController.findAllOrders().size();
        int productionCount = productionController.getQueue().size();
        int reservedCount   = orderController.findReservedOrders().size();

        ConsoleHelper.println("시스템 현황  " + LocalDateTime.now().format(FMT));
        ConsoleHelper.println("");

        String sc = AnsiColor.color(String.format("%3d", sampleCount),     AnsiColor.SUCCESS);
        String ts = AnsiColor.color(String.format("%6d", totalStock),      AnsiColor.SUCCESS);
        String oc = AnsiColor.boldNum(String.format("%3d", orderCount));
        String pc = AnsiColor.color(String.format("%3d", productionCount), AnsiColor.MAGENTA);
        String rc = AnsiColor.color(String.format("%3d", reservedCount),   AnsiColor.WARN);

        ConsoleHelper.println("등록 시료 " + sc + "종    총 재고 " + ts + " ea");
        ConsoleHelper.println("전체 주문 " + oc + "건    생산라인 " + pc + "건 대기    승인 대기 " + rc + "건");
    }

    private void printMenu() {
        String n0 = AnsiColor.color("[0]", AnsiColor.RED);
        ConsoleHelper.printThinLine();
        ConsoleHelper.println("  " + AnsiColor.menuNum("1") + " 시료 관리                         " + AnsiColor.menuNum("2") + " 시료 주문");
        ConsoleHelper.println("  " + AnsiColor.menuNum("3") + " 주문 승인/거절                    " + AnsiColor.menuNum("4") + " 모니터링");
        ConsoleHelper.println("  " + AnsiColor.menuNum("5") + " 생산라인 조회                     " + AnsiColor.menuNum("6") + " 출고 처리");
        ConsoleHelper.println("  " + n0 + " 종료");
        ConsoleHelper.printThinLine();
    }
}
