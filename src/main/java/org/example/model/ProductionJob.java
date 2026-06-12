package org.example.model;

import java.time.LocalDateTime;

public final class ProductionJob {

    private final String orderId;
    private final String sampleId;
    private final int shortage;
    private final int actualProductionQty;
    private final double totalProductionTime;
    private final LocalDateTime enqueuedAt;

    public ProductionJob(String orderId, String sampleId, int shortage,
                         int actualProductionQty, double totalProductionTime,
                         LocalDateTime enqueuedAt) {
        this.orderId = orderId;
        this.sampleId = sampleId;
        this.shortage = shortage;
        this.actualProductionQty = actualProductionQty;
        this.totalProductionTime = totalProductionTime;
        this.enqueuedAt = enqueuedAt;
    }

    public String getOrderId() { return orderId; }
    public String getSampleId() { return sampleId; }
    public int getShortage() { return shortage; }
    public int getActualProductionQty() { return actualProductionQty; }
    public double getTotalProductionTime() { return totalProductionTime; }
    public LocalDateTime getEnqueuedAt() { return enqueuedAt; }
}
