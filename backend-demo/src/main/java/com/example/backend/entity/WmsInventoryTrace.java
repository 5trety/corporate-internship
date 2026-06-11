package com.example.backend.entity;

import java.time.LocalDateTime;

public class WmsInventoryTrace {
    private Integer id;
    private String traceNo;
    private String kanbanNo;
    private String orderNo;
    private String partCode;
    private String partName;
    private String supplierCode;
    private String supplierName;
    private Integer quantity;
    private String locationCode;
    private String actionType;
    private String actionTypeText;
    private LocalDateTime actionTime;
    private String operator;
    private String remark;

    public String getActionTypeText() {
        if ("INBOUND".equals(actionType)) return "入库";
        if ("OUTBOUND".equals(actionType)) return "出库";
        if ("MOVE".equals(actionType)) return "移库";
        return actionType;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTraceNo() { return traceNo; }
    public void setTraceNo(String traceNo) { this.traceNo = traceNo; }
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
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public LocalDateTime getActionTime() { return actionTime; }
    public void setActionTime(LocalDateTime actionTime) { this.actionTime = actionTime; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}