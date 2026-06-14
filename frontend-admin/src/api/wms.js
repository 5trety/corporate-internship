import http from './http'

// ==================== 供应商管理 ====================
export function getSupplierList(keyword) {
    return http.get('/wms/supplier/list', { params: { keyword } })
}

export function getSupplierById(id) {
    return http.get(`/wms/supplier/${id}`)
}

export function saveSupplier(data) {
    return http.post('/wms/supplier/save', data)
}

export function deleteSupplier(id) {
    return http.delete(`/wms/supplier/delete/${id}`)
}

// ==================== 零件管理 ====================
export function getPartList(supplierCode) {
    return http.get('/wms/part/list', { params: { supplierCode } })
}

export function savePart(data) {
    return http.post('/wms/part/save', data)
}

export function deletePart(id) {
    return http.delete(`/wms/part/delete/${id}`)
}

// ==================== 仓库库位 ====================
export function getWarehouseList() {
    return http.get('/wms/warehouse/list')
}

export function getLocationList(warehouseCode) {
    return http.get('/wms/location/list', { params: { warehouseCode } })
}

// ==================== 入库单管理 ====================
export function createInboundOrder(data) {
    return http.post('/wms/inbound-order/create', data)
}

export function getInboundOrderList(params) {
    return http.get('/wms/inbound-order/list', { params })
}

export function getInboundOrderDetail(orderNo) {
    return http.get(`/wms/inbound-order/detail/${orderNo}`)
}

export function updateInboundOrder(orderNo, data) {
    return http.put(`/wms/inbound-order/update/${orderNo}`, data)
}

export function deleteInboundOrder(orderNo) {
    return http.delete(`/wms/inbound-order/delete/${orderNo}`)
}

// ==================== 打印功能 ====================
export function printInboundOrder(orderNo) {
    return http.get(`/wms/print/inbound-order/${orderNo}`)
}

export function printOutboundOrder(orderNo) {
    return http.get(`/wms/print/outbound-order/${orderNo}`)
}

export function printKanban(data) {
    return http.post('/wms/print/kanban', data)
}

// ==================== 看板管理 ====================
export function getKanbanList(params) {
    return http.get('/wms/kanban/list', { params })
}

// ==================== 扫码入库 ====================
export function validateKanban(kanbanNo) {
    return http.get(`/wms/kanban/validate/${kanbanNo}`)
}

export function scanInbound(data) {
    return http.post('/wms/scan/inbound', data)
}

// ==================== 库存追溯 ====================
export function getTraceList(params) {
    return http.get('/wms/trace/list', { params })
}

export function getCurrentInventory(params) {
    return http.get('/wms/inventory/current', { params })
}

export function getTraceByKanban(kanbanNo) {
    return http.get(`/wms/trace/by-kanban/${kanbanNo}`)
}

// ==================== 出库单管理 ====================
export function createOutboundOrder(data) {
    return http.post('/wms/outbound-order/create', data)
}

export function getOutboundOrderList(params) {
    return http.get('/wms/outbound-order/list', { params })
}

export function getOutboundOrderDetail(orderNo) {
    return http.get(`/wms/outbound-order/detail/${orderNo}`)
}

export function updateOutboundOrder(orderNo, data) {
    return http.put(`/wms/outbound-order/update/${orderNo}`, data)
}

export function deleteOutboundOrder(orderNo) {
    return http.delete(`/wms/outbound-order/delete/${orderNo}`)
}

export function validateOutboundOrder(orderNo) {
    return http.get(`/wms/outbound-order/validate/${orderNo}`)
}

// ==================== 扫码出库 ====================
export function scanOutbound(data) {
    return http.post('/wms/scan/outbound', data)
}

// ==================== FIFO库存查询 ====================
export function getFifoInventoryList(partCode) {
    return http.get('/wms/inventory/fifo-list', { params: { partCode } })
}
