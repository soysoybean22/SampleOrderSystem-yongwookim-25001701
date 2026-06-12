package org.example.view;

import org.example.controller.SampleController;
import org.example.model.Sample;

import java.util.List;

public final class SampleView {

    private final SampleController controller;

    public SampleView(SampleController controller) {
        this.controller = controller;
    }

    public void run() {
        while (true) {
            ConsoleHelper.clearScreen();
            printMenu();
            String input = ConsoleHelper.readLine("선택 > ");
            switch (input) {
                case "1" -> registerSample();
                case "2" -> listSamples();
                case "3" -> searchSample();
                case "0" -> { return; }
                default  -> ConsoleHelper.println("  잘못된 입력입니다. 다시 선택해주세요.");
            }
        }
    }

    private void printMenu() {
        ConsoleHelper.println("");
        ConsoleHelper.printHeader("[1] 시료 관리");
        ConsoleHelper.println("  [1] 시료 등록    [2] 시료 목록    [3] 시료 검색    [0] 위로");
        ConsoleHelper.printThinLine();
    }

    private void registerSample() {
        ConsoleHelper.println("");
        ConsoleHelper.println("[1] 시료 관리 > 시료 등록");
        ConsoleHelper.printThinLine();

        String sampleId = ConsoleHelper.readLine("시료 ID          > ");
        String name = ConsoleHelper.readLine("시료 이름        > ");
        double avgProductionTime = ConsoleHelper.readDouble("평균 생산시간 (min/ea) > ");
        double yield = ConsoleHelper.readDouble("수율 (0~1)       > ");
        int stock = ConsoleHelper.readInt("초기 재고        > ");

        try {
            controller.register(sampleId, name, avgProductionTime, yield, stock);
            ConsoleHelper.println("");
            ConsoleHelper.println("  등록 완료: " + name + " (" + sampleId + ")");
        } catch (IllegalArgumentException e) {
            ConsoleHelper.println("");
            ConsoleHelper.println("  [오류] " + e.getMessage());
        }
    }

    private void listSamples() {
        List<Sample> samples = controller.findAll();
        ConsoleHelper.println("");
        ConsoleHelper.println("[1] 시료 관리 > 시료 목록  (총 " + samples.size() + "종)");
        ConsoleHelper.printThinLine();

        if (samples.isEmpty()) {
            ConsoleHelper.println("  등록된 시료가 없습니다.");
            return;
        }

        System.out.printf("  %-8s %-22s %10s  %5s  %7s%n",
            "ID", "이름", "생산시간", "수율", "재고");
        ConsoleHelper.printThinLine();
        for (Sample s : samples) {
            System.out.printf("  %-8s %-22s %7.1f min  %5.2f  %5d ea%n",
                s.getSampleId(), s.getName(),
                s.getAvgProductionTime(), s.getYield(), s.getStock());
        }
    }

    private void searchSample() {
        ConsoleHelper.println("");
        ConsoleHelper.println("[1] 시료 관리 > 시료 검색");
        ConsoleHelper.printThinLine();

        String keyword = ConsoleHelper.readLine("검색어 > ");
        List<Sample> result = controller.searchByName(keyword);

        ConsoleHelper.println("");
        if (result.isEmpty()) {
            ConsoleHelper.println("  검색 결과가 없습니다: \"" + keyword + "\"");
            return;
        }

        ConsoleHelper.println("  검색 결과 (" + result.size() + "건)");
        ConsoleHelper.printThinLine();
        System.out.printf("  %-8s %-22s %10s  %5s  %7s%n",
            "ID", "이름", "생산시간", "수율", "재고");
        ConsoleHelper.printThinLine();
        for (Sample s : result) {
            System.out.printf("  %-8s %-22s %7.1f min  %5.2f  %5d ea%n",
                s.getSampleId(), s.getName(),
                s.getAvgProductionTime(), s.getYield(), s.getStock());
        }
    }
}
