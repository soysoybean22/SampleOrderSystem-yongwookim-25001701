package org.example.tool;

import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.ProductionJob;
import org.example.model.Sample;
import org.example.repository.OrderRepository;
import org.example.repository.ProductionJobRepository;
import org.example.repository.SampleRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public final class DummyDataGenerator {

    private DummyDataGenerator() {}

    public static void run() {
        resetFiles();
        generateSamples();
        generateOrders();
        generateProductionJobs();
        System.out.println("더미 데이터 생성 완료.");
        System.out.println("  시료 5종       → data/samples.json");
        System.out.println("  주문 10건      → data/orders.json");
        System.out.println("  생산 작업 2건  → data/production_jobs.json");
    }

    private static void resetFiles() {
        try {
            Files.deleteIfExists(Path.of("data", "samples.json"));
            Files.deleteIfExists(Path.of("data", "orders.json"));
            Files.deleteIfExists(Path.of("data", "production_jobs.json"));
        } catch (IOException e) {
            throw new RuntimeException("기존 파일 삭제 실패", e);
        }
    }

    private static void generateSamples() {
        SampleRepository repo = new SampleRepository();
        repo.save(new Sample("S-001", "실리콘 웨이퍼-8인치",  0.5, 0.92, 480));
        repo.save(new Sample("S-002", "GaN 에피택셜-4인치",   0.3, 0.78, 220));
        repo.save(new Sample("S-003", "SiC 파워기판-6인치",   0.8, 0.92,  30));
        repo.save(new Sample("S-004", "포토레지스트-PR7",      0.2, 0.95, 910));
        repo.save(new Sample("S-005", "산화막 웨이퍼-SiO2",   0.6, 0.88,   0));
    }

    private static void generateOrders() {
        OrderRepository repo = new OrderRepository();
        LocalDateTime base = LocalDateTime.of(2026, 4, 16, 9, 0, 0);
        repo.save(new Order("ORD-20260416-0001", "S-001", "LG이노텍",          300, OrderStatus.RESERVED,  base.plusMinutes(0)));
        repo.save(new Order("ORD-20260416-0002", "S-002", "SK하이닉스",        150, OrderStatus.RESERVED,  base.plusMinutes(5)));
        repo.save(new Order("ORD-20260416-0003", "S-003", "삼성전자 파운드리", 200, OrderStatus.RESERVED,  base.plusMinutes(10)));
        repo.save(new Order("ORD-20260416-0004", "S-004", "DB하이텍",          400, OrderStatus.CONFIRMED, base.plusMinutes(15)));
        repo.save(new Order("ORD-20260416-0005", "S-001", "인텔코리아",        100, OrderStatus.CONFIRMED, base.plusMinutes(20)));
        repo.save(new Order("ORD-20260416-0006", "S-002", "퀄컴코리아",         80, OrderStatus.CONFIRMED, base.plusMinutes(25)));
        repo.save(new Order("ORD-20260416-0007", "S-003", "삼성전자 파운드리", 200, OrderStatus.PRODUCING, base.plusMinutes(30)));
        repo.save(new Order("ORD-20260416-0008", "S-005", "LG이노텍",          150, OrderStatus.PRODUCING, base.plusMinutes(35)));
        repo.save(new Order("ORD-20260416-0009", "S-004", "SK하이닉스",        500, OrderStatus.RELEASE,   base.plusMinutes(40)));
        repo.save(new Order("ORD-20260416-0010", "S-001", "DB하이텍",          200, OrderStatus.RELEASE,   base.plusMinutes(45)));
    }

    private static void generateProductionJobs() {
        ProductionJobRepository repo = new ProductionJobRepository();
        LocalDateTime base = LocalDateTime.of(2026, 4, 16, 9, 30, 0);
        repo.save(new ProductionJob("ORD-20260416-0007", "S-003", 170, 206, 164.8, base));
        repo.save(new ProductionJob("ORD-20260416-0008", "S-005", 150, 190, 114.0, base.plusMinutes(5)));
    }
}
