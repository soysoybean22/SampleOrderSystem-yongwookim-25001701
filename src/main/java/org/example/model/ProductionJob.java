package org.example.model;

import java.time.Duration;
import java.time.LocalDateTime;

public final class ProductionJob {

    private final String orderId;
    private final String sampleId;
    private final int shortage;
    private final int actualProductionQty;
    private final double totalProductionTime;
    private final LocalDateTime enqueuedAt;
    private final LocalDateTime startedAt;

    public ProductionJob(String orderId, String sampleId, int shortage,
                         int actualProductionQty, double totalProductionTime,
                         LocalDateTime enqueuedAt, LocalDateTime startedAt) {
        this.orderId = orderId;
        this.sampleId = sampleId;
        this.shortage = shortage;
        this.actualProductionQty = actualProductionQty;
        this.totalProductionTime = totalProductionTime;
        this.enqueuedAt = enqueuedAt;
        this.startedAt = startedAt;
    }

    public String getOrderId() { return orderId; }
    public String getSampleId() { return sampleId; }
    public int getShortage() { return shortage; }
    public int getActualProductionQty() { return actualProductionQty; }
    public double getTotalProductionTime() { return totalProductionTime; }
    public LocalDateTime getEnqueuedAt() { return enqueuedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }

    public double elapsedMinutes(LocalDateTime now) {
        LocalDateTime start = (startedAt != null) ? startedAt : enqueuedAt;
        return Duration.between(start, now).toSeconds() / 60.0;
    }

    public int progressRatio(LocalDateTime now) {
        return (int) Math.min(100, elapsedMinutes(now) / totalProductionTime * 100);
    }
}
