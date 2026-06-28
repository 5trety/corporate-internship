package com.example.backend.entity;

import java.time.LocalDateTime;

/**
 * 转包单实体类
 */
public class WmsTransferOrder {
    private Integer id;
    private String orderNo;              // 转包单号 TO-yyyyMMdd-0001
    private String fromKanbanNo;         // 源看板号
    private String toKanbanNos;          // 目标看板号列表（JSON）
    private String transferType;         // split-拆分 / merge-合并
    private Integer originalQuantity;    // 原始数量
    private String newQuantities;        // 新数量列表（JSON）
    private String status;               // pending / completed
    private String operator;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    
    public String getFromKanbanNo() { return fromKanbanNo; }
    public void setFromKanbanNo(String fromKanbanNo) { this.fromKanbanNo = fromKanbanNo; }
    
    public String getToKanbanNos() { return toKanbanNos; }
    public void setToKanbanNos(String toKanbanNos) { this.toKanbanNos = toKanbanNos; }
    
    public String getTransferType() { return transferType; }
    public void setTransferType(String transferType) { this.transferType = transferType; }
    
    public Integer getOriginalQuantity() { return originalQuantity; }
    public void setOriginalQuantity(Integer originalQuantity) { this.originalQuantity = originalQuantity; }
    
    public String getNewQuantities() { return newQuantities; }
    public void setNewQuantities(String newQuantities) { this.newQuantities = newQuantities; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
