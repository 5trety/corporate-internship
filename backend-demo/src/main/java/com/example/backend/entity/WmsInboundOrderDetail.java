package com.example.backend.entity;

public class WmsInboundOrderDetail {
    private Integer id;
    private String orderNo;
    private String partCode;
    private String partName;
    private Integer expectedQuantity;
    private Integer receivedQuantity;
    private Integer packagingCapacity;
    private Integer expectedBoxes;
    private Integer receivedBoxes;
    private String unit;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getPartCode() { return partCode; }
    public void setPartCode(String partCode) { this.partCode = partCode; }
    public String getPartName() { return partName; }
    public void setPartName(String partName) { this.partName = partName; }
    public Integer getExpectedQuantity() { return expectedQuantity; }
    public void setExpectedQuantity(Integer expectedQuantity) { this.expectedQuantity = expectedQuantity; }
    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public Integer getPackagingCapacity() { return packagingCapacity; }
    public void setPackagingCapacity(Integer packagingCapacity) { this.packagingCapacity = packagingCapacity; }
    public Integer getExpectedBoxes() { return expectedBoxes; }
    public void setExpectedBoxes(Integer expectedBoxes) { this.expectedBoxes = expectedBoxes; }
    public Integer getReceivedBoxes() { return receivedBoxes; }
    public void setReceivedBoxes(Integer receivedBoxes) { this.receivedBoxes = receivedBoxes; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}