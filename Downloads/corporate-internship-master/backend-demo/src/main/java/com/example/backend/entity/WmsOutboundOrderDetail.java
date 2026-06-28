package com.example.backend.entity;

public class WmsOutboundOrderDetail {
    private Integer id;
    private String orderNo;
    private String partCode;
    private String partName;
    private Integer expectedQuantity;
    private Integer shippedQuantity;
    private Integer packagingCapacity;
    private Integer expectedBoxes;
    private Integer shippedBoxes;
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
    public Integer getShippedQuantity() { return shippedQuantity; }
    public void setShippedQuantity(Integer shippedQuantity) { this.shippedQuantity = shippedQuantity; }
    public Integer getPackagingCapacity() { return packagingCapacity; }
    public void setPackagingCapacity(Integer packagingCapacity) { this.packagingCapacity = packagingCapacity; }
    public Integer getExpectedBoxes() { return expectedBoxes; }
    public void setExpectedBoxes(Integer expectedBoxes) { this.expectedBoxes = expectedBoxes; }
    public Integer getShippedBoxes() { return shippedBoxes; }
    public void setShippedBoxes(Integer shippedBoxes) { this.shippedBoxes = shippedBoxes; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
