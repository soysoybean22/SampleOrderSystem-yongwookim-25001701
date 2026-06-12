package org.example.model;

public final class Sample {

    private final String sampleId;
    private final String name;
    private final double avgProductionTime;
    private final double yield;
    private int stock;

    public Sample(String sampleId, String name, double avgProductionTime, double yield, int stock) {
        if (sampleId == null || sampleId.isBlank()) throw new IllegalArgumentException("sampleId는 비어있을 수 없습니다.");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name은 비어있을 수 없습니다.");
        if (avgProductionTime <= 0) throw new IllegalArgumentException("avgProductionTime은 0보다 커야 합니다.");
        if (yield <= 0 || yield > 1) throw new IllegalArgumentException("yield는 (0, 1] 범위여야 합니다.");
        if (stock < 0) throw new IllegalArgumentException("stock은 0 이상이어야 합니다.");
        this.sampleId = sampleId;
        this.name = name;
        this.avgProductionTime = avgProductionTime;
        this.yield = yield;
        this.stock = stock;
    }

    public String getSampleId() { return sampleId; }
    public String getName() { return name; }
    public double getAvgProductionTime() { return avgProductionTime; }
    public double getYield() { return yield; }
    public int getStock() { return stock; }

    public void addStock(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount는 0보다 커야 합니다.");
        this.stock += amount;
    }

    public void subtractStock(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount는 0보다 커야 합니다.");
        if (amount > this.stock) throw new IllegalArgumentException("차감량이 현재 재고를 초과합니다.");
        this.stock -= amount;
    }
}
