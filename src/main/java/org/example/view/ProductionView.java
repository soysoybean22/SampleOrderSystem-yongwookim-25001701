package org.example.view;

import org.example.controller.ProductionController;
import org.example.controller.SampleController;
import org.example.model.OrderStatus;
import org.example.model.ProductionJob;
import org.example.model.Sample;

import java.util.List;
import java.util.Optional;

public final class ProductionView {

    private final ProductionController productionController;
    private final SampleController sampleController;

    public ProductionView(ProductionController productionController,
                          SampleController sampleController) {
        this.productionController = productionController;
        this.sampleController = sampleController;
    }

    public void run() {
        ConsoleHelper.clearScreen();
        ConsoleHelper.println("");
        ConsoleHelper.printHeader("[5] 생산라인 조회  FIFO 방식");

        List<ProductionJob> queue = productionController.getQueue();

        if (queue.isEmpty()) {
            ConsoleHelper.println("생산라인  단일 라인    현재 상태: " + AnsiColor.color("IDLE", AnsiColor.SUCCESS));
            ConsoleHelper.println("");
            ConsoleHelper.println(AnsiColor.color("  현재 생산 대기 중인 작업이 없습니다.", AnsiColor.WARN));
            ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
            return;
        }

        ConsoleHelper.println("생산라인  단일 라인    현재 상태: " + AnsiColor.color("RUNNING", AnsiColor.MAGENTA + AnsiColor.BOLD));
        printCurrentJob(queue.get(0));
        printWaitingQueue(queue);
        printFootnote();
        ConsoleHelper.printThinLine();

        String input = ConsoleHelper.readLine("  [1] 생산 완료 처리    [0] 위로\n선택 > ");
        if (input.equals("1")) completeProduction(queue.get(0));
    }

    private void printCurrentJob(ProductionJob job) {
        String sampleName = resolveSampleName(job.getSampleId());
        Optional<Sample> sampleOpt = sampleController.findAll().stream()
            .filter(s -> s.getSampleId().equals(job.getSampleId()))
            .findFirst();
        int stock = sampleOpt.map(Sample::getStock).orElse(0);
        int shortage = job.getShortage();

        ConsoleHelper.println("");
        ConsoleHelper.println(AnsiColor.color("[ 현재 처리 중 ]", AnsiColor.MAGENTA + AnsiColor.BOLD));
        System.out.printf("  주문번호    %s%n", job.getOrderId());
        System.out.printf("  시료        %s (%s)%n", sampleName, job.getSampleId());
        System.out.printf("  주문량      %d ea    현재 재고 %d ea → 부족 %d ea%n",
            job.getShortage() + stock, stock, shortage);
        System.out.printf("  실 생산량   %d ea    총 생산시간 %.1f min%n",
            job.getActualProductionQty(), job.getTotalProductionTime());
    }

    private void printWaitingQueue(List<ProductionJob> queue) {
        ConsoleHelper.println("");
        ConsoleHelper.println("[ 대기 중인 주문 (FIFO 순) ]");
        if (queue.size() <= 1) {
            ConsoleHelper.println("  대기 중인 작업이 없습니다.");
            return;
        }
        ConsoleHelper.printTableTop();
        ConsoleHelper.println("│  " + ConsoleHelper.padRight("순서", 4)
            + " " + ConsoleHelper.padRight("주문번호", 22)
            + " " + ConsoleHelper.padRight("시료명", 40)
            + " " + ConsoleHelper.padRight("부족분", 8)
            + " " + ConsoleHelper.padRight("실생산량", 9)
            + " " + ConsoleHelper.padRight("총생산시간", 11)
            + " ".repeat(17) + "│");
        ConsoleHelper.printTableDivider();
        for (int i = 1; i < queue.size(); i++) {
            ProductionJob j = queue.get(i);
            System.out.printf("│  %-4d %-22s %s %5d ea %6d ea %7.1f min" + " ".repeat(17) + "│%n",
                i, j.getOrderId(), ConsoleHelper.padRight(resolveSampleName(j.getSampleId()), 40),
                j.getShortage(), j.getActualProductionQty(), j.getTotalProductionTime());
        }
        ConsoleHelper.printTableBottom();
    }

    private void printFootnote() {
        ConsoleHelper.println("");
        ConsoleHelper.println(AnsiColor.color("  * 부족분 = 주문량 - 재고,  실생산량 = ceil(부족분 / (수율 × 0.9))", AnsiColor.WARN));
        ConsoleHelper.println(AnsiColor.color("  * FIFO 방식으로 처리됩니다.", AnsiColor.WARN));
    }

    private void completeProduction(ProductionJob current) {
        String sampleName = resolveSampleName(current.getSampleId());
        Optional<Sample> sampleOpt = sampleController.findAll().stream()
            .filter(s -> s.getSampleId().equals(current.getSampleId()))
            .findFirst();
        int beforeStock = sampleOpt.map(Sample::getStock).orElse(0);

        ConsoleHelper.println("");
        ConsoleHelper.println("생산 완료 처리하겠습니까?");
        System.out.printf("  주문번호    %s%n", current.getOrderId());
        System.out.printf("  시료        %s%n", sampleName);
        System.out.printf("  실 생산량   %d ea → 재고에 추가됩니다%n",
            current.getActualProductionQty());
        ConsoleHelper.println("");

        String confirm = ConsoleHelper.readLine("[Y] 확인    [N] 취소\n선택 > ");
        if (!confirm.equalsIgnoreCase("Y")) {
            ConsoleHelper.println(AnsiColor.color("  취소되었습니다.", AnsiColor.WARN));
            ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
            return;
        }

        ProductionJob completed = productionController.completeProduction();
        int afterStock = beforeStock + completed.getActualProductionQty();
        int remaining = productionController.getQueue().size();

        ConsoleHelper.println("");
        ConsoleHelper.println(AnsiColor.color("✓ 생산 완료.", AnsiColor.SUCCESS));
        System.out.printf("  주문번호    %s%n", completed.getOrderId());
        ConsoleHelper.println("  상태 변경  "
            + AnsiColor.statusBadge(OrderStatus.PRODUCING) + " → " + AnsiColor.statusBadge(OrderStatus.CONFIRMED));
        System.out.printf("  재고 추가   %d ea + %d ea = %d ea%n",
            beforeStock, completed.getActualProductionQty(), afterStock);
        System.out.printf("  잔여 큐     %d건 대기 중%n", remaining);
        ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
    }

    private String resolveSampleName(String sampleId) {
        return sampleController.findAll().stream()
            .filter(s -> s.getSampleId().equals(sampleId))
            .map(Sample::getName)
            .findFirst().orElse(sampleId);
    }
}
