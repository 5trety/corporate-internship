package com.example.backend.entity;

import java.time.LocalDateTime;
import java.util.List;

public class WmsOutboundOrder {
    private Integer id;
    private String orderNo;
    private String outboundType;
    private String customerCode;
    private String customerName;
    private String warehouseCode;
    private String warehouseName;
    private String status;
    private String statusText;
    private Integer totalQuantity;
    private Integer shippedQuantity;
    private Integer totalBoxes;
    private Integer shippedBoxes;
    private String remark;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WmsOutboundOrderDetail> details;

    // 状态常量
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PARTIAL = "partial";
    public static final String STATUS_COMPLETED = "completed";

    public String getStatusText() {
        switch (status) {
            case STATUS_PENDING: return "待出库";
            case STATUS_PARTIAL: return "部分出库";
            case STATUS_COMPLETED: return "已完成";
            default: return status;
        }
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getOutboundType() { return outboundType; }
    public void setOutboundType(String outboundType) { this.outboundType = outboundType; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public Integer getShippedQuantity() { return shippedQuantity; }
    public void setShippedQuantity(Integer shippedQuantity) { this.shippedQuantity = shippedQuantity; }
    public Integer getTotalBoxes() { return totalBoxes; }
    public void setTotalBoxes(Integer totalBoxes) { this.totalBoxes = totalBoxes; }
    public Integer getShippedBoxes() { return shippedBoxes; }
    public void setShippedBoxes(Integer shippedBoxes) { this.shippedBoxes = shippedBoxes; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<WmsOutboundOrderDetail> getDetails() { return details; }
    public void setDetails(List<WmsOutboundOrderDetail> details) { this.details = details; }
}
