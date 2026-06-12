package org.example;

import org.example.controller.SampleController;
import org.example.repository.SampleRepository;
import org.example.tool.DataMonitorTool;
import org.example.tool.DummyDataGenerator;
import org.example.view.MainView;

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
        SampleController sampleController = new SampleController(new SampleRepository());
        new MainView(sampleController).run();
    }
}
