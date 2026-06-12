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
        System.out.println("  시료  8종       → data/samples.json       (목록 2페이지)");
        System.out.println("  주문 18건       → data/orders.json");
        System.out.println("    RESERVED  7건  (승인 대기 목록 2페이지)");
        System.out.println("    CONFIRMED 6건  (출고 처리 목록 2페이지)");
        System.out.println("    PRODUCING 2건");
        System.out.println("    RELEASE   3건");
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
        repo.save(new Sample("S-006", "질화갈륨 기판-GaN",    1.2, 0.85, 150));
        repo.save(new Sample("S-007", "탄화규소 에피-4H",     0.9, 0.90,  75));
        repo.save(new Sample("S-008", "인화인듐 기판-InP",    0.7, 0.82, 320));
    }

    private static void generateOrders() {
        OrderRepository repo = new OrderRepository();
        LocalDateTime base = LocalDateTime.of(2026, 4, 16, 9, 0, 0);

        // RESERVED 7건 — 승인 대기 목록 2페이지
        repo.save(new Order("ORD-20260416-0001", "S-001", "LG이노텍",          300, OrderStatus.RESERVED,  base.plusMinutes(0)));
        repo.save(new Order("ORD-20260416-0002", "S-002", "SK하이닉스",        150, OrderStatus.RESERVED,  base.plusMinutes(5)));
        repo.save(new Order("ORD-20260416-0003", "S-003", "삼성전자 파운드리", 200, OrderStatus.RESERVED,  base.plusMinutes(10)));
        repo.save(new Order("ORD-20260416-0011", "S-006", "현대차",            120, OrderStatus.RESERVED,  base.plusMinutes(15)));
        repo.save(new Order("ORD-20260416-0012", "S-007", "기아",               80, OrderStatus.RESERVED,  base.plusMinutes(20)));
        repo.save(new Order("ORD-20260416-0013", "S-008", "포스코",            250, OrderStatus.RESERVED,  base.plusMinutes(25)));
        repo.save(new Order("ORD-20260416-0014", "S-001", "한화시스템",        100, OrderStatus.RESERVED,  base.plusMinutes(30)));

        // CONFIRMED 6건 — 출고 처리 목록 2페이지
        repo.save(new Order("ORD-20260416-0004", "S-004", "DB하이텍",          400, OrderStatus.CONFIRMED, base.plusMinutes(35)));
        repo.save(new Order("ORD-20260416-0005", "S-001", "인텔코리아",        100, OrderStatus.CONFIRMED, base.plusMinutes(40)));
        repo.save(new Order("ORD-20260416-0006", "S-002", "퀄컴코리아",         80, OrderStatus.CONFIRMED, base.plusMinutes(45)));
        repo.save(new Order("ORD-20260416-0015", "S-006", "엔비디아코리아",     60, OrderStatus.CONFIRMED, base.plusMinutes(50)));
        repo.save(new Order("ORD-20260416-0016", "S-008", "AMD코리아",         150, OrderStatus.CONFIRMED, base.plusMinutes(55)));
        repo.save(new Order("ORD-20260416-0017", "S-004", "마이크론",          200, OrderStatus.CONFIRMED, base.plusMinutes(60)));

        // PRODUCING 2건
        repo.save(new Order("ORD-20260416-0007", "S-003", "삼성전자 파운드리", 200, OrderStatus.PRODUCING, base.plusMinutes(65)));
        repo.save(new Order("ORD-20260416-0008", "S-005", "LG이노텍",          150, OrderStatus.PRODUCING, base.plusMinutes(70)));

        // RELEASE 3건
        repo.save(new Order("ORD-20260416-0009", "S-004", "SK하이닉스",        500, OrderStatus.RELEASE,   base.plusMinutes(75)));
        repo.save(new Order("ORD-20260416-0010", "S-001", "DB하이텍",          200, OrderStatus.RELEASE,   base.plusMinutes(80)));
        repo.save(new Order("ORD-20260416-0018", "S-002", "삼성전자",          100, OrderStatus.RELEASE,   base.plusMinutes(85)));
    }

    private static void generateProductionJobs() {
        ProductionJobRepository repo = new ProductionJobRepository();
        LocalDateTime base = LocalDateTime.of(2026, 4, 16, 9, 30, 0);
        repo.save(new ProductionJob("ORD-20260416-0007", "S-003", 170, 206, 164.8, base));
        repo.save(new ProductionJob("ORD-20260416-0008", "S-005", 150, 190, 114.0, base.plusMinutes(5)));
    }
}
