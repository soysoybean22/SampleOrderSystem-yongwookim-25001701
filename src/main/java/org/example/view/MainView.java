package org.example.view;

import org.example.controller.OrderController;
import org.example.controller.SampleController;
import org.example.repository.ProductionJobRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class MainView {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SampleController sampleController;
    private final OrderController orderController;
    private final SampleView sampleView;
    private final OrderView orderView;

    public MainView(SampleController sampleController, OrderController orderController) {
        this.sampleController = sampleController;
        this.orderController = orderController;
        this.sampleView = new SampleView(sampleController);
        this.orderView = new OrderView(orderController, sampleController);
    }

    public void run() {
        while (true) {
            printSummary();
            printMenu();
            String input = ConsoleHelper.readLine("선택 > ");
            switch (input) {
                case "1" -> sampleView.run();
                case "2" -> orderView.run();
                case "3", "4", "5", "6" -> ConsoleHelper.println("  준비 중인 기능입니다.");
                case "0" -> {
                    ConsoleHelper.println("  시스템을 종료합니다.");
                    return;
                }
                default -> ConsoleHelper.println("  잘못된 입력입니다. 다시 선택해주세요.");
            }
        }
    }

    private void printSummary() {
        int sampleCount = sampleController.findAll().size();
        int totalStock = sampleController.findAll().stream().mapToInt(s -> s.getStock()).sum();
        int orderCount = orderController.findAllOrders().size();
        int productionCount = new ProductionJobRepository().findAll().size();

        ConsoleHelper.println("");
        ConsoleHelper.printSeparator();
        ConsoleHelper.println("        반도체 시료 생산주문관리 시스템");
        ConsoleHelper.printSeparator();
        ConsoleHelper.println("시스템 현황  " + LocalDateTime.now().format(FMT));
        ConsoleHelper.println("");
        System.out.printf("등록 시료 %3d종    총 재고 %6d ea%n", sampleCount, totalStock);
        System.out.printf("전체 주문 %3d건    생산라인 %3d건 대기%n", orderCount, productionCount);
    }

    private void printMenu() {
        ConsoleHelper.printThinLine();
        ConsoleHelper.println("  [1] 시료 관리          [2] 시료 주문");
        ConsoleHelper.println("  [3] 주문 승인/거절     [4] 모니터링");
        ConsoleHelper.println("  [5] 생산라인 조회      [6] 출고 처리");
        ConsoleHelper.println("  [0] 종료");
        ConsoleHelper.printThinLine();
    }
}
