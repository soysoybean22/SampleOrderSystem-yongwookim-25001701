package org.example.tool;

import org.example.model.Order;
import org.example.model.ProductionJob;
import org.example.model.Sample;
import org.example.repository.OrderRepository;
import org.example.repository.ProductionJobRepository;
import org.example.repository.SampleRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class DataMonitorTool {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String LINE = "=".repeat(72);
    private static final String DASH = "-".repeat(72);

    private DataMonitorTool() {}

    public static void run() {
        System.out.println(LINE);
        System.out.printf("  데이터 모니터링 Tool  —  %s%n", LocalDateTime.now().format(FMT));
        System.out.println(LINE);
        printSamples();
        printOrders();
        printProductionJobs();
        System.out.println(LINE);
    }

    private static void printSamples() {
        List<Sample> samples = new SampleRepository().findAll();
        System.out.printf("%n[시료 목록]  (총 %d종)%n", samples.size());
        System.out.printf("%-8s %-24s %10s  %5s  %7s%n", "ID", "이름", "생산시간", "수율", "재고");
        System.out.println(DASH);
        for (Sample s : samples) {
            System.out.printf("%-8s %-24s %7.1f min  %5.2f  %5d ea%n",
                s.getSampleId(), s.getName(),
                s.getAvgProductionTime(), s.getYield(), s.getStock());
        }
    }

    private static void printOrders() {
        List<Order> orders = new OrderRepository().findAll();
        System.out.printf("%n[주문 목록]  (총 %d건)%n", orders.size());
        System.out.printf("%-22s %-7s %-18s %7s  %-10s  %s%n",
            "주문번호", "시료ID", "고객명", "수량", "상태", "접수일시");
        System.out.println(DASH);
        for (Order o : orders) {
            System.out.printf("%-22s %-7s %-18s %5d ea  %-10s  %s%n",
                o.getOrderId(), o.getSampleId(), o.getCustomerName(),
                o.getQuantity(), o.getStatus(), o.getCreatedAt().format(FMT));
        }
    }

    private static void printProductionJobs() {
        List<ProductionJob> jobs = new ProductionJobRepository().findAll();
        System.out.printf("%n[생산 작업 큐]  (총 %d건, FIFO 순)%n", jobs.size());
        System.out.printf("%-4s %-22s %-7s  %7s  %7s  %10s  %s%n",
            "순서", "주문번호", "시료ID", "부족분", "실생산량", "총생산시간", "등록일시");
        System.out.println(DASH);
        for (int i = 0; i < jobs.size(); i++) {
            ProductionJob j = jobs.get(i);
            System.out.printf("%-4d %-22s %-7s  %5d ea  %5d ea  %7.1f min  %s%n",
                i + 1, j.getOrderId(), j.getSampleId(),
                j.getShortage(), j.getActualProductionQty(),
                j.getTotalProductionTime(), j.getEnqueuedAt().format(FMT));
        }
    }
}
