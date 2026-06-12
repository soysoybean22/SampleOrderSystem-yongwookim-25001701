package org.example;

import org.example.tool.DataMonitorTool;
import org.example.tool.DummyDataGenerator;

public final class Main {

    private Main() {}

    public static void main(String[] args) {
        if (args.length > 0) {
            switch (args[0]) {
                case "monitor" -> DataMonitorTool.run();
                case "dummy"   -> DummyDataGenerator.run();
                default        -> System.out.println("알 수 없는 명령: " + args[0]);
            }
            return;
        }
        // Phase 2 이후: 메인 메뉴 루프 추가 예정
        System.out.println("사용법: monitor | dummy");
    }
}
