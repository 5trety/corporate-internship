package com.example.backend.entity;

import java.time.LocalDateTime;
import java.util.List;

public class WmsInboundOrder {
    private Integer id;
    private String orderNo;
    private String inboundType;
    private String supplierCode;
    private String supplierName;
    private String warehouseCode;
    private String warehouseName;
    private String status;
    private String statusText;
    private Integer totalQuantity;
    private Integer receivedQuantity;
    private Integer totalBoxes;
    private Integer receivedBoxes;
    private String remark;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WmsInboundOrderDetail> details;

    // 状态常量
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PARTIAL = "partial";
    public static final String STATUS_COMPLETED = "completed";

    public String getStatusText() {
        switch (status) {
            case STATUS_PENDING: return "待入库";
            case STATUS_PARTIAL: return "部分入库";
            case STATUS_COMPLETED: return "已完成";
            default: return status;
        }
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getInboundType() { return inboundType; }
    public void setInboundType(String inboundType) { this.inboundType = inboundType; }
    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public Integer getTotalBoxes() { return totalBoxes; }
    public void setTotalBoxes(Integer totalBoxes) { this.totalBoxes = totalBoxes; }
    public Integer getReceivedBoxes() { return receivedBoxes; }
    public void setReceivedBoxes(Integer receivedBoxes) { this.receivedBoxes = receivedBoxes; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<WmsInboundOrderDetail> getDetails() { return details; }
    public void setDetails(List<WmsInboundOrderDetail> details) { this.details = details; }
}