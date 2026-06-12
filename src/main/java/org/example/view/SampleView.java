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
            ConsoleHelper.println(AnsiColor.color("  ✓ 등록 완료: " + name + " (" + sampleId + ")", AnsiColor.SUCCESS));
        } catch (IllegalArgumentException e) {
            ConsoleHelper.println("");
            ConsoleHelper.println(AnsiColor.color("  ✗ [오류] " + e.getMessage(), AnsiColor.ERROR));
        }
    }

    private void listSamples() {
        List<Sample> samples = controller.findAll();

        if (samples.isEmpty()) {
            ConsoleHelper.println("");
            ConsoleHelper.println("[1] 시료 관리 > 시료 목록  (총 0종)");
            ConsoleHelper.printThinLine();
            ConsoleHelper.println("  등록된 시료가 없습니다.");
            ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
            return;
        }

        Paginator<Sample> paginator = new Paginator<>(samples);

        while (true) {
            ConsoleHelper.clearScreen();
            ConsoleHelper.println("");
            ConsoleHelper.println("[1] 시료 관리 > 시료 목록  (총 " + samples.size() + "종)");
            ConsoleHelper.printTableTop();
            ConsoleHelper.println("│  " + ConsoleHelper.padRight("ID", 8)
                + " " + ConsoleHelper.padRight("이름", 22)
                + " " + ConsoleHelper.padRight("생산시간", 13)
                + ConsoleHelper.padRight("수율", 5)
                + "  " + ConsoleHelper.padRight("재고", 8) + "│");
            ConsoleHelper.printTableDivider();
            for (Sample s : paginator.currentItems()) {
                System.out.printf("│  %-8s %s %7.1f min  %5.2f  %5d ea│%n",
                    s.getSampleId(), ConsoleHelper.padRight(s.getName(), 22),
                    s.getAvgProductionTime(), s.getYield(), s.getStock());
            }
            ConsoleHelper.printTableBottom();

            if (!paginator.needsPagination()) {
                ConsoleHelper.readLine("\n  [0] 위로 > ");
                return;
            }

            paginator.printNavBar();
            String input = ConsoleHelper.readLine("  [P/N] 페이지 이동, [0] 위로 > ");
            if (input.equals("0"))                return;
            if (input.equalsIgnoreCase("N"))      paginator.nextPage();
            else if (input.equalsIgnoreCase("P")) paginator.prevPage();
        }
    }

    private void searchSample() {
        ConsoleHelper.println("");
        ConsoleHelper.println("[1] 시료 관리 > 시료 검색");
        ConsoleHelper.printThinLine();

        String keyword = ConsoleHelper.readLine("검색어 > ");
        List<Sample> result = controller.searchByName(keyword);

        if (result.isEmpty()) {
            ConsoleHelper.println("");
            ConsoleHelper.println("  검색 결과가 없습니다: \"" + keyword + "\"");
            ConsoleHelper.readLine("\n  [Enter] 메뉴로 돌아가기");
            return;
        }

        Paginator<Sample> paginator = new Paginator<>(result);

        while (true) {
            ConsoleHelper.clearScreen();
            ConsoleHelper.println("");
            ConsoleHelper.println("  검색 결과 (" + result.size() + "건)  키워드: \"" + keyword + "\"");
            ConsoleHelper.printTableTop();
            ConsoleHelper.println("│  " + ConsoleHelper.padRight("ID", 8)
                + " " + ConsoleHelper.padRight("이름", 22)
                + " " + ConsoleHelper.padRight("생산시간", 13)
                + ConsoleHelper.padRight("수율", 5)
                + "  " + ConsoleHelper.padRight("재고", 8) + "│");
            ConsoleHelper.printTableDivider();
            for (Sample s : paginator.currentItems()) {
                System.out.printf("│  %-8s %s %7.1f min  %5.2f  %5d ea│%n",
                    s.getSampleId(), ConsoleHelper.padRight(s.getName(), 22),
                    s.getAvgProductionTime(), s.getYield(), s.getStock());
            }
            ConsoleHelper.printTableBottom();

            if (!paginator.needsPagination()) {
                ConsoleHelper.readLine("\n  [0] 위로 > ");
                return;
            }

            paginator.printNavBar();
            String input = ConsoleHelper.readLine("  [P/N] 페이지 이동, [0] 위로 > ");
            if (input.equals("0"))                return;
            if (input.equalsIgnoreCase("N"))      paginator.nextPage();
            else if (input.equalsIgnoreCase("P")) paginator.prevPage();
        }
    }
}
