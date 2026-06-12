package org.example.model;

public final class SampleStockInfo {

    private final Sample sample;
    private final int pendingQuantity; // CONFIRMED + PRODUCING 주문 수량 합계
    private final StockStatus status;

    public SampleStockInfo(Sample sample, int pendingQuantity, StockStatus status) {
        this.sample = sample;
        this.pendingQuantity = pendingQuantity;
        this.status = status;
    }

    public Sample getSample() { return sample; }
    public int getPendingQuantity() { return pendingQuantity; }
    public StockStatus getStatus() { return status; }
}
