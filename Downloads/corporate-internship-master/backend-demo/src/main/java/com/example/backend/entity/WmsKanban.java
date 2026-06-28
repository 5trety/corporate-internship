package com.example.backend.entity;

import java.time.LocalDateTime;

public class WmsKanban {
    private Integer id;
    private String kanbanNo;
    private String orderNo;
    private String partCode;
    private String partName;
    private String supplierCode;
    private String supplierName;
    private Integer quantity;
    private Integer boxCount;
    private String qrCode;
    private String status;
    private String statusText;
    private String locationCode;
    private String locationName;
    private LocalDateTime scanTime;
    private String scanBy;
    private LocalDateTime createdAt;

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_SCANNED = "scanned";
    public static final String STATUS_STORED = "stored";

    public String getStatusText() {
        switch (status) {
            case STATUS_PENDING: return "待入库";
            case STATUS_SCANNED: return "已扫码";
            case STATUS_STORED: return "已上架";
            default: return status;
        }
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getKanbanNo() { return kanbanNo; }
    public void setKanbanNo(String kanbanNo) { this.kanbanNo = kanbanNo; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getPartCode() { return partCode; }
    public void setPartCode(String partCode) { this.partCode = partCode; }
    public String getPartName() { return partName; }
    public void setPartName(String partName) { this.partName = partName; }
    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getBoxCount() { return boxCount; }
    public void setBoxCount(Integer boxCount) { this.boxCount = boxCount; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public LocalDateTime getScanTime() { return scanTime; }
    public void setScanTime(LocalDateTime scanTime) { this.scanTime = scanTime; }
    public String getScanBy() { return scanBy; }
    public void setScanBy(String scanBy) { this.scanBy = scanBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}