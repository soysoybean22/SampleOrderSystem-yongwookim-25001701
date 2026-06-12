package org.example.model;

public final class ApprovalResult {

    private final Order order;
    private final ProductionJob productionJob; // 재고 부족 시 생성, 충분 시 null

    public ApprovalResult(Order order, ProductionJob productionJob) {
        this.order = order;
        this.productionJob = productionJob;
    }

    public Order getOrder() { return order; }
    public ProductionJob getProductionJob() { return productionJob; }

    public boolean isConfirmed() {
        return order.getStatus() == OrderStatus.CONFIRMED;
    }
}
