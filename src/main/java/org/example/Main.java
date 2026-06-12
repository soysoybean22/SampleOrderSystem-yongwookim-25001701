package org.example;

import org.example.controller.MonitoringController;
import org.example.controller.OrderController;
import org.example.controller.ProductionController;
import org.example.controller.SampleController;
import org.example.repository.OrderRepository;
import org.example.repository.ProductionJobRepository;
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
        SampleRepository sampleRepo = new SampleRepository();
        OrderRepository orderRepo = new OrderRepository();
        ProductionJobRepository jobRepo = new ProductionJobRepository();

        SampleController sampleController = new SampleController(sampleRepo);
        OrderController orderController = new OrderController(orderRepo, sampleRepo, jobRepo);
        ProductionController productionController = new ProductionController(jobRepo, sampleRepo, orderRepo);
        MonitoringController monitoringController = new MonitoringController(orderRepo, sampleRepo);

        new MainView(sampleController, orderController, productionController, monitoringController).run();
    }
}
