package com.example.backend.controller;

import com.example.backend.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/wms")
public class WmsController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ==================== 基础信息管理 ====================

    /**
     * 获取供应商列表
     */
    @GetMapping("/supplier/list")
    public Result<List<WmsSupplier>> getSupplierList(@RequestParam(required = false) String keyword) {
        try {
            String sql = "SELECT * FROM supplier WHERE status = 1";
            if (keyword != null && !keyword.isEmpty()) {
                sql += " AND (supplier_code LIKE '%" + keyword + "%' OR supplier_name LIKE '%" + keyword + "%')";
            }
            sql += " ORDER BY supplier_code";
            List<WmsSupplier> suppliers = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(WmsSupplier.class));
            return Result.success(suppliers);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取供应商列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取供应商详情
     */
    @GetMapping("/supplier/{id}")
    public Result<WmsSupplier> getSupplierById(@PathVariable Integer id) {
        try {
            String sql = "SELECT * FROM supplier WHERE id = ?";
            WmsSupplier supplier = jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(WmsSupplier.class), id);
            return Result.success(supplier);
        } catch (Exception e) {
            return Result.error("获取供应商详情失败: " + e.getMessage());
        }
    }

    /**
     * 保存供应商（新增或修改）
     */
    @PostMapping("/supplier/save")
    public Result<Map<String, Object>> saveSupplier(@RequestBody WmsSupplier supplier) {
        try {
            if (supplier.getId() == null) {
                // 新增
                List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                        "SELECT id, status FROM supplier WHERE supplier_code = ?",
                        supplier.getSupplierCode());
                if (!existing.isEmpty()) {
                    Map<String, Object> row = existing.get(0);
                    Integer id = ((Number) row.get("id")).intValue();
                    Integer status = ((Number) row.get("status")).intValue();
                    if (status != null && status == 1) {
                        return Result.error("供应商代码已存在");
                    }
                    jdbcTemplate.update("UPDATE supplier SET supplier_name=?, contact_person=?, phone=?, address=?, status=1 WHERE id=?",
                            supplier.getSupplierName(), supplier.getContactPerson(),
                            supplier.getPhone(), supplier.getAddress(), id);
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    return Result.success(result);
                }
                String sql = "INSERT INTO supplier (supplier_code, supplier_name, contact_person, phone, address) VALUES (?, ?, ?, ?, ?)";
                jdbcTemplate.update(sql, supplier.getSupplierCode(), supplier.getSupplierName(),
                        supplier.getContactPerson(), supplier.getPhone(), supplier.getAddress());
            } else {
                // 修改
                String sql = "UPDATE supplier SET supplier_name=?, contact_person=?, phone=?, address=? WHERE id=?";
                jdbcTemplate.update(sql, supplier.getSupplierName(), supplier.getContactPerson(),
                        supplier.getPhone(), supplier.getAddress(), supplier.getId());
            }
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("保存供应商失败: " + e.getMessage());
        }
    }

    /**
     * 删除供应商
     */
    @DeleteMapping("/supplier/delete/{id}")
    public Result<Map<String, Object>> deleteSupplier(@PathVariable Integer id) {
        try {
            jdbcTemplate.update("UPDATE supplier SET status = 0 WHERE id = ?", id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("删除供应商失败: " + e.getMessage());
        }
    }

    /**
     * 获取零件列表
     */
    @GetMapping("/part/list")
    public Result<List<WmsPart>> getPartList(@RequestParam(required = false) String supplierCode) {
        try {
            String sql = "SELECT p.*, s.supplier_name FROM part p LEFT JOIN supplier s ON p.supplier_code = s.supplier_code WHERE p.status = 1";
            if (supplierCode != null && !supplierCode.isEmpty()) {
                sql += " AND p.supplier_code = '" + supplierCode + "'";
            }
            sql += " ORDER BY p.part_code";
            List<WmsPart> parts = jdbcTemplate.query(sql, (rs, rowNum) -> {
                WmsPart part = new WmsPart();
                part.setId(rs.getInt("id"));
                part.setPartCode(rs.getString("part_code"));
                part.setPartName(rs.getString("part_name"));
                part.setSupplierCode(rs.getString("supplier_code"));
                part.setSupplierName(rs.getString("supplier_name"));
                part.setPackagingCapacity(rs.getInt("packaging_capacity"));
                part.setUnit(rs.getString("unit"));
                part.setPrice(rs.getBigDecimal("price"));
                part.setWeight(rs.getBigDecimal("weight"));
                part.setStatus(rs.getInt("status"));
                return part;
            });
            return Result.success(parts);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取零件列表失败: " + e.getMessage());
        }
    }

    /**
     * 保存零件
     */
    @PostMapping("/part/save")
    public Result<Map<String, Object>> savePart(@RequestBody WmsPart part) {
        try {
            if (part.getPackagingCapacity() == null || part.getPackagingCapacity() <= 0) {
                return Result.error("包装容量必须大于0");
            }
            if (part.getId() == null) {
                String sql = "INSERT INTO part (part_code, part_name, supplier_code, packaging_capacity, unit, price, weight) VALUES (?, ?, ?, ?, ?, ?, ?)";
                jdbcTemplate.update(sql, part.getPartCode(), part.getPartName(), part.getSupplierCode(),
                        part.getPackagingCapacity(), part.getUnit(), part.getPrice(), part.getWeight());
            } else {
                String sql = "UPDATE part SET part_name=?, packaging_capacity=?, unit=?, price=?, weight=? WHERE id=?";
                jdbcTemplate.update(sql, part.getPartName(), part.getPackagingCapacity(),
                        part.getUnit(), part.getPrice(), part.getWeight(), part.getId());
            }
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("保存零件失败: " + e.getMessage());
        }
    }

    /**
     * 删除零件
     */
    @DeleteMapping("/part/delete/{id}")
    public Result<Map<String, Object>> deletePart(@PathVariable Integer id) {
        try {
            jdbcTemplate.update("UPDATE part SET status = 0 WHERE id = ?", id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("删除零件失败: " + e.getMessage());
        }
    }

    /**
     * 获取仓库列表
     */
    @GetMapping("/warehouse/list")
    public Result<List<Map<String, Object>>> getWarehouseList() {
        try {
            List<Map<String, Object>> warehouses = jdbcTemplate.queryForList("SELECT * FROM warehouse WHERE status = 1");
            return Result.success(warehouses);
        } catch (Exception e) {
            return Result.error("获取仓库列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取库位列表（已废弃，保留接口以防前端报错）
     * @deprecated 系统已从“商品→库位→仓库”简化为“商品→仓库”
     */
    @Deprecated
    @GetMapping("/location/list")
    public Result<List<Map<String, Object>>> getLocationList(@RequestParam(required = false) String warehouseCode) {
        try {
            // 返回空列表，因为库位概念已废弃
            return Result.success(new java.util.ArrayList<>());
        } catch (Exception e) {
            return Result.error("获取库位列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询仓库零件可用库存（用于出库单创建）
     */
    @GetMapping("/warehouse/{warehouseCode}/part-stock")
    public Result<List<Map<String, Object>>> getWarehousePartStock(@PathVariable String warehouseCode) {
        try {
            String sql = "SELECT ci.part_code, p.part_name, p.packaging_capacity, " +
                    "SUM(ci.quantity) as available_stock " +
                    "FROM current_inventory ci " +
                    "LEFT JOIN part p ON ci.part_code = p.part_code " +
                    "WHERE ci.warehouse_code = ? AND ci.quantity > 0 " +
                    "GROUP BY ci.part_code, p.part_name, p.packaging_capacity";
            
            List<Map<String, Object>> stock = jdbcTemplate.queryForList(sql, warehouseCode);
            return Result.success(stock);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询仓库库存失败: " + e.getMessage());
        }
    }

    // ==================== 入库单管理 ====================

    /**
     * 生成入库单号
     */
    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sql = "SELECT COUNT(*) FROM inbound_order WHERE order_no LIKE 'PO-" + date + "%'";
        int count = jdbcTemplate.queryForObject(sql, Integer.class);
        return "PO-" + date + "-" + String.format("%04d", count + 1);
    }

    /**
     * 创建入库单
     */
    @PostMapping("/inbound-order/create")
    public Result<Map<String, Object>> createInboundOrder(@RequestBody WmsInboundOrder order, HttpSession session) {
        try {
            String orderNo = generateOrderNo();
            String username = (String) session.getAttribute("user");
            if (username == null) username = "system";

            // 插入主表
            String sql = "INSERT INTO inbound_order (order_no, inbound_type, supplier_code, warehouse_code, " +
                    "total_quantity, total_boxes, remark, created_by, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending')";
            jdbcTemplate.update(sql, orderNo, order.getInboundType(), order.getSupplierCode(),
                    order.getWarehouseCode(), order.getTotalQuantity(), order.getTotalBoxes(),
                    order.getRemark(), username);

            // 插入明细
            if (order.getDetails() != null) {
                for (WmsInboundOrderDetail detail : order.getDetails()) {
                    String detailSql = "INSERT INTO inbound_order_detail (order_no, part_code, part_name, " +
                            "expected_quantity, packaging_capacity, expected_boxes, unit) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(detailSql, orderNo, detail.getPartCode(), detail.getPartName(),
                            detail.getExpectedQuantity(), detail.getPackagingCapacity(),
                            detail.getExpectedBoxes(), detail.getUnit());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("orderNo", orderNo);
            result.put("success", true);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建入库单失败: " + e.getMessage());
        }
    }

    /**
     * 获取入库单列表
     */
    @GetMapping("/inbound-order/list")
    public Result<Map<String, Object>> getInboundOrderList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String supplierCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT o.*, s.supplier_name, w.warehouse_name FROM inbound_order o " +
                            "LEFT JOIN supplier s ON o.supplier_code = s.supplier_code " +
                            "LEFT JOIN warehouse w ON o.warehouse_code = w.warehouse_code WHERE 1=1 "
            );
            List<Object> params = new ArrayList<>();

            if (status != null && !status.isEmpty()) {
                sql.append("AND o.status = ? ");
                params.add(status);
            }
            if (supplierCode != null && !supplierCode.isEmpty()) {
                sql.append("AND o.supplier_code = ? ");
                params.add(supplierCode);
            }
            if (startDate != null && !startDate.isEmpty()) {
                sql.append("AND DATE(o.created_at) >= ? ");
                params.add(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                sql.append("AND DATE(o.created_at) <= ? ");
                params.add(endDate);
            }

            // 查询总数
            String countSql = sql.toString().replace("SELECT o.*, s.supplier_name, w.warehouse_name", "SELECT COUNT(*)");
            Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

            // 分页查询
            sql.append("ORDER BY o.created_at DESC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            List<WmsInboundOrder> orders = jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
                WmsInboundOrder order = new WmsInboundOrder();
                order.setId(rs.getInt("id"));
                order.setOrderNo(rs.getString("order_no"));
                order.setInboundType(rs.getString("inbound_type"));
                order.setSupplierCode(rs.getString("supplier_code"));
                order.setSupplierName(rs.getString("supplier_name"));
                order.setWarehouseCode(rs.getString("warehouse_code"));
                order.setWarehouseName(rs.getString("warehouse_name"));
                order.setStatus(rs.getString("status"));
                order.setTotalQuantity(rs.getInt("total_quantity"));
                order.setReceivedQuantity(rs.getInt("received_quantity"));
                order.setTotalBoxes(rs.getInt("total_boxes"));
                order.setReceivedBoxes(rs.getInt("received_boxes"));
                order.setRemark(rs.getString("remark"));
                order.setCreatedBy(rs.getString("created_by"));
                order.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
                order.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
                return order;
            });

            Map<String, Object> result = new HashMap<>();
            result.put("list", orders);
            result.put("total", total != null ? total : 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取入库单列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取入库单详情
     */
    @GetMapping("/inbound-order/detail/{orderNo}")
    public Result<Map<String, Object>> getInboundOrderDetail(@PathVariable String orderNo) {
        try {
            // 使用更安全的查询方式，避免 EmptyResultDataAccessException
            String orderSql = "SELECT o.*, s.supplier_name, w.warehouse_name FROM inbound_order o " +
                    "LEFT JOIN supplier s ON o.supplier_code = s.supplier_code " +
                    "LEFT JOIN warehouse w ON o.warehouse_code = w.warehouse_code WHERE o.order_no = ?";

            List<Map<String, Object>> orders = jdbcTemplate.queryForList(orderSql, orderNo);
            if (orders.isEmpty()) {
                return Result.error("入库单不存在: " + orderNo);
            }
            Map<String, Object> order = orders.get(0);

            // 获取明细
            String detailSql = "SELECT * FROM inbound_order_detail WHERE order_no = ?";
            List<Map<String, Object>> details = jdbcTemplate.queryForList(detailSql, orderNo);

            // 获取看板（只获取未入库的原始看板，排除转包看板）
            String kanbanSql = "SELECT k.*, s.supplier_name, w.warehouse_name FROM kanban k " +
                    "LEFT JOIN supplier s ON k.supplier_code = s.supplier_code " +
                    "LEFT JOIN warehouse w ON k.warehouse_code = w.warehouse_code " +
                    "WHERE k.order_no = ? " +
                    "AND (k.parent_kanban_no IS NULL OR k.parent_kanban_no = '') " +
                    "AND k.status NOT IN ('IN_STOCK', 'completed', '入库完成', '已入库', 'scanned')";
            List<Map<String, Object>> kanbans = jdbcTemplate.queryForList(kanbanSql, orderNo);

            Map<String, Object> result = new HashMap<>();
            result.put("order", order);
            result.put("details", details);
            result.put("kanbans", kanbans);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取入库单详情失败: " + e.getMessage());
        }
    }

    /**
     * 修改入库单
     */
    @PutMapping("/inbound-order/update/{orderNo}")
    public Result<Map<String, Object>> updateInboundOrder(@PathVariable String orderNo, @RequestBody WmsInboundOrder order) {
        try {
            // 检查状态
            String statusSql = "SELECT status FROM inbound_order WHERE order_no = ?";
            String status = jdbcTemplate.queryForObject(statusSql, String.class, orderNo);
            if ("completed".equals(status)) {
                return Result.error("已完成入库的单据不能修改");
            }

            // 更新主表
            String sql = "UPDATE inbound_order SET inbound_type=?, warehouse_code=?, remark=? WHERE order_no=?";
            jdbcTemplate.update(sql, order.getInboundType(), order.getWarehouseCode(), order.getRemark(), orderNo);

            // 删除旧明细
            jdbcTemplate.update("DELETE FROM inbound_order_detail WHERE order_no = ?", orderNo);

            // 插入新明细
            if (order.getDetails() != null) {
                for (WmsInboundOrderDetail detail : order.getDetails()) {
                    String detailSql = "INSERT INTO inbound_order_detail (order_no, part_code, part_name, " +
                            "expected_quantity, packaging_capacity, expected_boxes, unit) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(detailSql, orderNo, detail.getPartCode(), detail.getPartName(),
                            detail.getExpectedQuantity(), detail.getPackagingCapacity(),
                            detail.getExpectedBoxes(), detail.getUnit());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("修改入库单失败: " + e.getMessage());
        }
    }

    /**
     * 删除入库单
     */
    @DeleteMapping("/inbound-order/delete/{orderNo}")
    public Result<Map<String, Object>> deleteInboundOrder(@PathVariable String orderNo) {
        try {
            String statusSql = "SELECT status FROM inbound_order WHERE order_no = ?";
            String status = jdbcTemplate.queryForObject(statusSql, String.class, orderNo);
            if ("completed".equals(status)) {
                return Result.error("已完成入库的单据不能删除");
            }

            // 删除关联的看板
            jdbcTemplate.update("DELETE FROM kanban WHERE order_no = ?", orderNo);
            // 删除明细
            jdbcTemplate.update("DELETE FROM inbound_order_detail WHERE order_no = ?", orderNo);
            // 删除主表
            jdbcTemplate.update("DELETE FROM inbound_order WHERE order_no = ?", orderNo);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("删除入库单失败: " + e.getMessage());
        }
    }

    // ==================== 打印功能 ====================

    /**
     * 打印入库单
     */
    @GetMapping("/print/inbound-order/{orderNo}")
    public Result<Map<String, Object>> printInboundOrder(@PathVariable String orderNo) {
        try {
            return getInboundOrderDetail(orderNo);
        } catch (Exception e) {
            return Result.error("获取打印数据失败: " + e.getMessage());
        }
    }

    /**
     * 打印出库单
     */
    @GetMapping("/print/outbound-order/{orderNo}")
    public Result<Map<String, Object>> printOutboundOrder(@PathVariable String orderNo) {
        try {
            return getOutboundOrderDetail(orderNo);
        } catch (Exception e) {
            return Result.error("获取打印数据失败: " + e.getMessage());
        }
    }

    /**
     * 打印看板
     * 入库单：生成新的入库看板
     * 出库单：返回已有的入库看板（FIFO推荐），不生成新看板
     */
    @PostMapping("/print/kanban")
    public Result<List<Map<String, String>>> printKanban(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            System.out.println("========== 打印看板请求开始 ==========");
            System.out.println("接收到的请求数据: " + request);

            String orderNo = (String) request.get("orderNo");
            System.out.println("订单号: " + orderNo);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
            System.out.println("items数量: " + (items == null ? 0 : items.size()));

            if (items == null || items.isEmpty()) {
                return Result.error("没有要打印的看板项");
            }

            // 判断是入库单还是出库单
            boolean isInboundOrder = orderNo != null && orderNo.startsWith("PO-");
            boolean isOutboundOrder = orderNo != null && orderNo.startsWith("SO-");

            System.out.println("订单类型: " + (isInboundOrder ? "入库单" : (isOutboundOrder ? "出库单" : "未知")));

            // ============================================
            // 出库单：返回已有的入库看板（FIFO推荐）
            // ============================================
            if (isOutboundOrder) {
                System.out.println("✅ 出库单打印：返回FIFO推荐的入库看板");
                return getFifoKanbansForOutbound(orderNo, items);
            }

            // ============================================
            // 入库单：生成新的入库看板
            // ============================================
            System.out.println("✅ 入库单打印：生成新的入库看板");

            // 获取入库单的仓库代码
            String warehouseCode = null;
            try {
                warehouseCode = jdbcTemplate.queryForObject(
                        "SELECT warehouse_code FROM inbound_order WHERE order_no = ?",
                        String.class, orderNo);
            } catch (Exception e) {
                System.out.println("获取仓库代码失败: " + e.getMessage());
            }

            if (warehouseCode == null || warehouseCode.isEmpty()) {
                warehouseCode = "WH-001";
                System.out.println("使用默认仓库: " + warehouseCode);
            }

            System.out.println("入库单仓库: " + warehouseCode);

            // 删除已存在的看板（防止重复生成）
            String deleteAllSql = "DELETE FROM kanban WHERE order_no = ? AND status = 'pending'";
            int deletedAll = jdbcTemplate.update(deleteAllSql, orderNo);
            System.out.println("已删除 " + deletedAll + " 个已存在的看板 (订单: " + orderNo + ")");

            List<Map<String, String>> kanbans = new ArrayList<>();

            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> item = items.get(i);
                System.out.println("第" + (i+1) + "个item: " + item);

                String partCode = null;
                String partName = null;
                String supplierCode = null;
                Integer quantity = null;
                Integer boxCount = 1;
                String kanbanNo = null;

                if (item.get("partCode") != null) {
                    partCode = (String) item.get("partCode");
                } else if (item.get("part_code") != null) {
                    partCode = (String) item.get("part_code");
                }

                if (item.get("partName") != null) {
                    partName = (String) item.get("partName");
                } else if (item.get("part_name") != null) {
                    partName = (String) item.get("part_name");
                }

                if (item.get("supplierCode") != null) {
                    supplierCode = (String) item.get("supplierCode");
                } else if (item.get("supplier_code") != null) {
                    supplierCode = (String) item.get("supplier_code");
                }

                if (item.get("expectedQuantity") != null) {
                    quantity = ((Number) item.get("expectedQuantity")).intValue();
                } else if (item.get("quantity") != null) {
                    quantity = ((Number) item.get("quantity")).intValue();
                } else if (item.get("expected_quantity") != null) {
                    quantity = ((Number) item.get("expected_quantity")).intValue();
                }

                if (item.get("expectedBoxes") != null) {
                    boxCount = ((Number) item.get("expectedBoxes")).intValue();
                } else if (item.get("boxCount") != null) {
                    boxCount = ((Number) item.get("boxCount")).intValue();
                } else if (item.get("expected_boxes") != null) {
                    boxCount = ((Number) item.get("expected_boxes")).intValue();
                }

                if (item.get("kanbanNo") != null) {
                    kanbanNo = (String) item.get("kanbanNo");
                }

                System.out.println("解析后 - partCode: " + partCode + ", partName: " + partName +
                        ", quantity: " + quantity + ", boxCount: " + boxCount + ", kanbanNo: " + kanbanNo);

                if (partCode == null || partCode.isEmpty()) {
                    return Result.error("零件号不能为空，第" + (i+1) + "个item: " + item);
                }
                if (quantity == null || quantity <= 0) {
                    return Result.error("数量不能为空或为0，第" + (i+1) + "个item");
                }

                if (kanbanNo == null || kanbanNo.isEmpty()) {
                    kanbanNo = "KAN-" + System.currentTimeMillis() + "-" + partCode + "-" + (i + 1);
                }

                String qrContent = kanbanNo;

                String sql = "INSERT INTO kanban (kanban_no, order_no, part_code, part_name, supplier_code, warehouse_code, " +
                        "quantity, box_count, qr_code, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                jdbcTemplate.update(sql, kanbanNo, orderNo, partCode, partName, supplierCode,
                        warehouseCode, quantity, boxCount, qrContent, "pending");

                System.out.println("看板生成成功: " + kanbanNo);

                Integer packagingCapacity = 0;
                try {
                    packagingCapacity = jdbcTemplate.queryForObject(
                            "SELECT packaging_capacity FROM part WHERE part_code = ?",
                            Integer.class, partCode);
                    if (packagingCapacity == null) packagingCapacity = 0;
                } catch (Exception e) {
                    System.out.println("查询包装容量失败: " + e.getMessage());
                    packagingCapacity = 0;
                }

                Map<String, String> kanban = new HashMap<>();
                kanban.put("kanbanNo", kanbanNo);
                kanban.put("partCode", partCode);
                kanban.put("partName", partName);
                kanban.put("quantity", String.valueOf(quantity));
                kanban.put("supplierCode", supplierCode == null ? "" : supplierCode);
                kanban.put("boxCount", String.valueOf(boxCount));
                kanban.put("packagingCapacity", String.valueOf(packagingCapacity));
                kanban.put("qrContent", qrContent);
                kanbans.add(kanban);
            }

            System.out.println("共生成 " + kanbans.size() + " 个看板");
            System.out.println("========== 打印看板请求结束 ==========");

            return Result.success(kanbans);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("生成看板失败: " + e.getMessage());
        }
    }

    /**
     * 为出库单获取FIFO推荐的入库看板
     */
    private Result<List<Map<String, String>>> getFifoKanbansForOutbound(String orderNo, List<Map<String, Object>> items) {
        try {
            System.out.println("========== 获取FIFO入库看板 ==========");
            System.out.println("出库单号: " + orderNo);

            Map<String, Object> order = jdbcTemplate.queryForMap(
                    "SELECT * FROM outbound_order WHERE order_no = ?", orderNo);
            String warehouseCode = (String) order.get("warehouse_code");

            // Get warehouse name
            String warehouseName = null;
            if (warehouseCode != null && !warehouseCode.isEmpty()) {
                try {
                    warehouseName = jdbcTemplate.queryForObject(
                            "SELECT warehouse_name FROM warehouse WHERE warehouse_code = ?",
                            String.class, warehouseCode);
                } catch (Exception e) {
                    warehouseName = warehouseCode;
                }
            }

            System.out.println("🏭 仓库代码: " + warehouseCode + ", 仓库名称: " + warehouseName);

            List<Map<String, String>> kanbans = new ArrayList<>();

            for (Map<String, Object> item : items) {
                String partCode = null;
                String partName = null;

                if (item.get("partCode") != null) {
                    partCode = (String) item.get("partCode");
                } else if (item.get("part_code") != null) {
                    partCode = (String) item.get("part_code");
                }

                if (item.get("partName") != null) {
                    partName = (String) item.get("partName");
                } else if (item.get("part_name") != null) {
                    partName = (String) item.get("part_name");
                }

                if (partCode == null || partCode.isEmpty()) {
                    System.out.println("⚠️ 跳过空零件号");
                    continue;
                }

                Integer requiredQty = null;
                if (item.get("expectedQuantity") != null) {
                    requiredQty = ((Number) item.get("expectedQuantity")).intValue();
                } else if (item.get("quantity") != null) {
                    requiredQty = ((Number) item.get("quantity")).intValue();
                }

                System.out.println("🔍 查询零件: " + partCode + ", 需要数量: " + requiredQty);

                List<Map<String, Object>> kanbanList = queryFifoKanbans(partCode, warehouseCode);

                if (kanbanList.isEmpty()) {
                    System.out.println("⚠️ 仓库 " + warehouseCode + " 没有零件 " + partCode + " 的可用库存");
                    continue;
                }

                System.out.println("📦 找到 " + kanbanList.size() + " 个可用看板");

                List<Map<String, Object>> selectedKanbans = limitFifoKanbansByRequiredQuantity(kanbanList, requiredQty);

                System.out.println("📦 选择 " + selectedKanbans.size() + " 个看板用于出库");

                for (Map<String, Object> kanban : selectedKanbans) {
                    String supplierCode = (String) kanban.get("supplier_code");
                    String supplierName = (String) kanban.get("supplier_name");

                    if ((supplierName == null || supplierName.isEmpty()) && supplierCode != null && !supplierCode.isEmpty()) {
                        try {
                            supplierName = jdbcTemplate.queryForObject(
                                    "SELECT supplier_name FROM supplier WHERE supplier_code = ?",
                                    String.class, supplierCode);
                        } catch (Exception e) {
                            supplierName = supplierCode;
                        }
                    }

                    String kanbanNo = (String) kanban.get("kanban_no");
                    String partNameFromDb = (String) kanban.get("part_name");
                    String warehouseNameFromDb = (String) kanban.get("warehouse_name");

                    Integer availableQty = 0;
                    Object qtyObj = kanban.get("available_quantity");
                    if (qtyObj instanceof Number) {
                        availableQty = ((Number) qtyObj).intValue();
                    }

                    // ============================================
                    // FIX: Set locationName to warehouse name
                    // ============================================
                    String locationName = warehouseNameFromDb != null ? warehouseNameFromDb : warehouseName;
                    if (locationName == null || locationName.isEmpty()) {
                        locationName = "仓库";
                    }

                    Map<String, String> kanbanInfo = new HashMap<>();
                    kanbanInfo.put("kanbanNo", kanbanNo != null ? kanbanNo : "");
                    kanbanInfo.put("partCode", partCode);
                    kanbanInfo.put("partName", partName != null ? partName : (partNameFromDb != null ? partNameFromDb : ""));
                    kanbanInfo.put("quantity", String.valueOf(availableQty));
                    kanbanInfo.put("supplierCode", supplierCode != null ? supplierCode : "");
                    kanbanInfo.put("supplierName", supplierName != null ? supplierName : "");
                    kanbanInfo.put("locationName", locationName);  // ← This is what the frontend uses
                    kanbanInfo.put("locationCode", warehouseCode != null ? warehouseCode : "");
                    kanbanInfo.put("warehouseName", warehouseName != null ? warehouseName : "");
                    kanbanInfo.put("qrContent", kanbanNo != null ? kanbanNo : "");

                    System.out.println("✅ 看板: " + kanbanNo + ", 数量: " + availableQty +
                            ", 供应商: " + supplierName + ", 库位: " + locationName);

                    kanbans.add(kanbanInfo);
                }
            }

            System.out.println("✅ 为出库单 " + orderNo + " 找到 " + kanbans.size() + " 个可用入库看板");
            System.out.println("========== 获取FIFO入库看板结束 ==========");

            if (kanbans.isEmpty()) {
                return Result.error("没有找到可用的入库看板，请先完成入库操作");
            }

            return Result.success(kanbans);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取FIFO看板失败: " + e.getMessage());
        }
    }

    /**
     * 限制FIFO看板数量，只取需要的数量
     */
    private List<Map<String, Object>> limitFifoKanbansByRequiredQuantity(
            List<Map<String, Object>> kanbanList,
            Integer requiredQty
    ) {
        if (requiredQty == null || requiredQty <= 0) {
            return kanbanList;
        }

        List<Map<String, Object>> selected = new ArrayList<>();
        int selectedQty = 0;

        for (Map<String, Object> kanban : kanbanList) {
            if (selectedQty >= requiredQty) {
                break;
            }

            int availableQty = getAvailableQuantity(kanban);
            int remainingNeed = requiredQty - selectedQty;
            int qtyToUse = Math.min(availableQty, remainingNeed);

            Map<String, Object> limitedKanban = new HashMap<>(kanban);
            limitedKanban.put("available_quantity", qtyToUse);
            selected.add(limitedKanban);

            selectedQty += qtyToUse;

            System.out.println("📦 使用看板: " + kanban.get("kanban_no") +
                    ", 可用: " + availableQty + ", 使用: " + qtyToUse +
                    ", 累计: " + selectedQty + "/" + requiredQty);
        }

        return selected;
    }

    /**
     * 获取看板可用数量
     */
    private int getAvailableQuantity(Map<String, Object> kanban) {
        Object value = kanban.get("available_quantity");
        if (!(value instanceof Number)) {
            value = kanban.get("quantity");
        }
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    /**
     * 查询FIFO入库看板
     */
    private List<Map<String, Object>> queryFifoKanbans(String partCode, String warehouseCode) {
        try {
            String kanbanSql = "SELECT " +
                    "ci.kanban_no, " +
                    "ci.part_code, " +
                    "COALESCE(k.part_name, ci.part_name) AS part_name, " +
                    "ci.supplier_code, " +
                    "s.supplier_name, " +
                    "COALESCE(k.quantity, ci.quantity) AS quantity, " +
                    "ci.quantity AS available_quantity, " +
                    "ci.warehouse_code, " +
                    "w.warehouse_name " +  // ← Make sure this is included
                    "FROM current_inventory ci " +
                    "LEFT JOIN kanban k ON ci.kanban_no = k.kanban_no " +
                    "LEFT JOIN supplier s ON ci.supplier_code = s.supplier_code " +
                    "LEFT JOIN warehouse w ON ci.warehouse_code = w.warehouse_code " +
                    "WHERE ci.part_code = ? " +
                    "AND ci.quantity > 0 " +
                    "AND (k.kanban_no IS NULL OR (k.status IN ('scanned', 'stored') AND (k.is_sealed IS NULL OR k.is_sealed = 0)))";

            List<Object> params = new ArrayList<>();
            params.add(partCode);

            if (warehouseCode != null && !warehouseCode.isEmpty()) {
                kanbanSql += " AND ci.warehouse_code = ?";
                params.add(warehouseCode);
            }

            kanbanSql += " ORDER BY COALESCE(k.created_at, ci.created_at) ASC, ci.id ASC";

            System.out.println("🔍 执行FIFO查询SQL: " + kanbanSql);
            System.out.println("📦 参数: partCode=" + partCode + ", warehouseCode=" + warehouseCode);

            return jdbcTemplate.queryForList(kanbanSql, params.toArray());
        } catch (Exception e) {
            System.err.println("❌ FIFO查询失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 获取看板列表（支持分页）
     */
    @GetMapping("/kanban/list")
    public Result<Map<String, Object>> getKanbanList(@RequestParam(required = false) String orderNo,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) Integer isSealed,
                                                 @RequestParam(required = false) String kanbanNo,
                                                 @RequestParam(required = false) String partCode,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int pageSize) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT k.*, s.supplier_name, w.warehouse_name FROM kanban k " +
                            "LEFT JOIN supplier s ON k.supplier_code = s.supplier_code " +
                            "LEFT JOIN warehouse w ON k.warehouse_code = w.warehouse_code WHERE 1=1"
            );
            List<Object> params = new ArrayList<>();

            if (orderNo != null && !orderNo.isEmpty()) {
                sql.append(" AND k.order_no = ?");
                params.add(orderNo);
            }
            if (status != null && !status.isEmpty()) {
                sql.append(" AND k.status = ?");
                params.add(status);
            }
            if (isSealed != null) {
                sql.append(" AND k.is_sealed = ?");
                params.add(isSealed);
            }
            if (kanbanNo != null && !kanbanNo.isEmpty()) {
                sql.append(" AND k.kanban_no LIKE ?");
                params.add("%" + kanbanNo + "%");
            }
            if (partCode != null && !partCode.isEmpty()) {
                sql.append(" AND k.part_code LIKE ?");
                params.add("%" + partCode + "%");
            }
            
            // 查询总数（独立构建WHERE条件）
            StringBuilder countWhere = new StringBuilder(" WHERE 1=1");
            List<Object> countParams = new ArrayList<>();
            if (orderNo != null && !orderNo.isEmpty()) {
                countWhere.append(" AND k.order_no = ?");
                countParams.add(orderNo);
            }
            if (status != null && !status.isEmpty()) {
                countWhere.append(" AND k.status = ?");
                countParams.add(status);
            }
            if (isSealed != null) {
                countWhere.append(" AND k.is_sealed = ?");
                countParams.add(isSealed);
            }
            if (kanbanNo != null && !kanbanNo.isEmpty()) {
                countWhere.append(" AND k.kanban_no LIKE ?");
                countParams.add("%" + kanbanNo + "%");
            }
            if (partCode != null && !partCode.isEmpty()) {
                countWhere.append(" AND k.part_code LIKE ?");
                countParams.add("%" + partCode + "%");
            }
            String countSql = "SELECT COUNT(*) FROM kanban k" + countWhere.toString();
            Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, countParams.toArray());
            
            // 分页查询
            sql.append(" ORDER BY k.created_at DESC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            List<Map<String, Object>> kanbans = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            
            // 为每个看板添加入库/出库时间
            for (Map<String, Object> kanban : kanbans) {
                String kanbanNo2 = (String) kanban.get("kanban_no");
                
                // 查询入库时间（最早的INBOUND记录）
                try {
                    LocalDateTime inboundTime = jdbcTemplate.queryForObject(
                        "SELECT MIN(action_time) FROM inventory_trace WHERE kanban_no = ? AND action_type = 'INBOUND'",
                        LocalDateTime.class,
                        kanbanNo2
                    );
                    kanban.put("inboundTime", inboundTime);
                } catch (Exception e) {
                    kanban.put("inboundTime", null);
                }
                
                // 查询出库时间（最早的OUTBOUND记录）
                try {
                    LocalDateTime outboundTime = jdbcTemplate.queryForObject(
                        "SELECT MIN(action_time) FROM inventory_trace WHERE kanban_no = ? AND action_type = 'OUTBOUND'",
                        LocalDateTime.class,
                        kanbanNo2
                    );
                    kanban.put("outboundTime", outboundTime);
                } catch (Exception e) {
                    kanban.put("outboundTime", null);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("list", kanbans);
            result.put("total", total != null ? total : 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取看板列表失败: " + e.getMessage());
        }
    }

    // ==================== 扫码入库 ====================

    /**
     * 扫码入库（添加事务保护）
     */
    /**
     * 扫码入库（允许多次入库）
     */
    @Transactional
    @PostMapping("/scan/inbound")
    public Result<Map<String, Object>> scanInbound(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            String kanbanNo = (String) request.get("kanbanNo");
            String locationCode = (String) request.get("locationCode");
            String username = (String) session.getAttribute("user");
            if (username == null) username = "system";

            // Get quantity from request, default to full kanban quantity if not provided
            Integer requestQuantity = null;
            if (request.get("quantity") != null) {
                requestQuantity = ((Number) request.get("quantity")).intValue();
            }

            // 查询看板（只允许入库看板，order_no以IO-或PO-开头，状态为pending或scanned）
            Map<String, Object> kanban = jdbcTemplate.queryForMap(
                    "SELECT * FROM kanban WHERE kanban_no = ? AND status IN ('pending', 'scanned') AND (order_no LIKE 'IO-%' OR order_no LIKE 'PO-%')",
                    kanbanNo);

            String orderNo = (String) kanban.get("order_no");
            Integer kanbanQuantity = (Integer) kanban.get("quantity");
            String partCode = (String) kanban.get("part_code");

            // 获取入库单的仓库代码
            String warehouseCode = jdbcTemplate.queryForObject(
                    "SELECT warehouse_code FROM inbound_order WHERE order_no = ?",
                    String.class, orderNo);
            
            if (warehouseCode == null || warehouseCode.isEmpty()) {
                return Result.error("入库单未指定仓库");
            }

            // Get current received quantity for this kanban from inventory_trace or current_inventory
            Integer alreadyReceived = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(quantity), 0) FROM inventory_trace WHERE kanban_no = ? AND action_type = 'INBOUND'",
                    Integer.class, kanbanNo);

            // Calculate remaining quantity
            Integer remainingQuantity = kanbanQuantity - alreadyReceived;

            // Determine inbound quantity
            Integer quantity;
            if (requestQuantity != null) {
                // Use the quantity from request
                quantity = requestQuantity;
                // Validate quantity
                if (quantity <= 0) {
                    return Result.error("入库数量必须大于0");
                }
                if (quantity > remainingQuantity) {
                    return Result.error("入库数量不能超过剩余数量，剩余: " + remainingQuantity);
                }
            } else {
                // If no quantity provided, use remaining quantity (full remaining)
                quantity = remainingQuantity;
                if (quantity <= 0) {
                    return Result.error("该看板已全部入库完成");
                }
            }

            // ============================================
            // REMOVE the strict validation that forces full quantity
            // ============================================
            // OLD CODE - REMOVE THIS:
            // if (quantity < kanbanQuantity) {
            //     return Result.error("零件 " + partCode + " 的看板数量（" + kanbanQuantity + "）与入库数量（" + quantity + "）不匹配，必须一次性入库");
            // }

            // NEW CODE - Allow partial inbound:
            // We already validated quantity <= remainingQuantity above

            // 验证入库单剩余数量（防止超量入库）
            Integer totalQuantity = jdbcTemplate.queryForObject(
                    "SELECT total_quantity FROM inbound_order WHERE order_no = ?", Integer.class, orderNo);
            Integer receivedQuantity = jdbcTemplate.queryForObject(
                    "SELECT received_quantity FROM inbound_order WHERE order_no = ?", Integer.class, orderNo);
            Integer orderRemaining = totalQuantity - (receivedQuantity != null ? receivedQuantity : 0);

            if (quantity > orderRemaining) {
                return Result.error("入库数量超过入库单剩余数量，当前订单剩余: " + orderRemaining);
            }

            // 验证零件明细剩余数量
            Integer expectedQty = jdbcTemplate.queryForObject(
                    "SELECT expected_quantity FROM inbound_order_detail WHERE order_no = ? AND part_code = ?",
                    Integer.class, orderNo, partCode);
            Integer receivedQty = jdbcTemplate.queryForObject(
                    "SELECT received_quantity FROM inbound_order_detail WHERE order_no = ? AND part_code = ?",
                    Integer.class, orderNo, partCode);
            Integer partRemaining = expectedQty - (receivedQty != null ? receivedQty : 0);

            if (quantity > partRemaining) {
                return Result.error("入库数量超过零件剩余数量，零件 " + partCode + " 剩余: " + partRemaining);
            }

            // 计算箱数（按数量比例）
            Integer boxCount = (Integer) kanban.get("box_count");
            if (boxCount != null && boxCount > 0) {
                boxCount = (int) Math.ceil((double) quantity / kanbanQuantity * boxCount);
            } else {
                boxCount = 1;
            }

            // 更新看板状态
            String currentStatus = (String) kanban.get("status");
            if ("pending".equals(currentStatus)) {
                jdbcTemplate.update("UPDATE kanban SET status = 'scanned', scan_time = ?, scan_by = ? WHERE kanban_no = ?",
                        LocalDateTime.now(), username, kanbanNo);
            }

            // 更新入库单入库数量（处理NULL值）
            jdbcTemplate.update("UPDATE inbound_order SET received_quantity = IFNULL(received_quantity, 0) + ?, " +
                    "received_boxes = IFNULL(received_boxes, 0) + ? WHERE order_no = ?", quantity, boxCount, orderNo);

            // 更新明细（处理NULL值）
            jdbcTemplate.update("UPDATE inbound_order_detail SET received_quantity = IFNULL(received_quantity, 0) + ?, " +
                            "received_boxes = IFNULL(received_boxes, 0) + ? WHERE order_no = ? AND part_code = ?",
                    quantity, boxCount, orderNo, partCode);

            // 检查库存是否已存在（避免重复插入）
            Integer existsCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM current_inventory WHERE kanban_no = ?", Integer.class, kanbanNo);

            if (existsCount != null && existsCount > 0) {
                // 已存在，更新数量
                jdbcTemplate.update("UPDATE current_inventory SET quantity = quantity + ? WHERE kanban_no = ?",
                        quantity, kanbanNo);
            } else {
                // 不存在，插入新记录
                jdbcTemplate.update("INSERT INTO current_inventory (kanban_no, part_code, part_name, supplier_code, quantity, warehouse_code) " +
                                "VALUES (?, ?, ?, ?, ?, ?)", kanbanNo, partCode, kanban.get("part_name"),
                        kanban.get("supplier_code"), quantity, warehouseCode);
            }

            // 添加追溯记录
            String traceNo = "TRACE-" + System.currentTimeMillis();
            jdbcTemplate.update("INSERT INTO inventory_trace (trace_no, kanban_no, order_no, part_code, part_name, " +
                            "supplier_code, quantity, warehouse_code, action_type, operator) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'INBOUND', ?)",
                    traceNo, kanbanNo, orderNo, partCode, kanban.get("part_name"),
                    kanban.get("supplier_code"), quantity, warehouseCode, username);

            // 检查入库单是否完成
            int newReceivedQuantity = receivedQuantity + quantity;
            if (totalQuantity == newReceivedQuantity) {
                jdbcTemplate.update("UPDATE inbound_order SET status = 'completed' WHERE order_no = ?", orderNo);
                jdbcTemplate.update("UPDATE kanban SET status = 'stored' WHERE order_no = ? AND status IN ('pending', 'scanned')", orderNo);
            } else {
                jdbcTemplate.update("UPDATE inbound_order SET status = 'partial' WHERE order_no = ?", orderNo);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "入库成功");
            result.put("orderNo", orderNo);
            result.put("partCode", partCode);
            result.put("partName", kanban.get("part_name"));
            result.put("quantity", quantity);
            result.put("remainingQuantity", remainingQuantity - quantity);
            result.put("operator", username);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("扫码入库失败: " + e.getMessage());
        }
    }

    /**
     * 验证看板是否有效
     */
    @GetMapping("/kanban/validate/{kanbanNo}")
    public Result<Map<String, Object>> validateKanban(@PathVariable String kanbanNo) {
        try {
            Map<String, Object> kanban = jdbcTemplate.queryForMap(
                    "SELECT k.*, s.supplier_name, w.warehouse_name FROM kanban k " +
                            "LEFT JOIN supplier s ON k.supplier_code = s.supplier_code " +
                            "LEFT JOIN warehouse w ON k.warehouse_code = w.warehouse_code " +
                            "WHERE k.kanban_no = ? AND (k.order_no LIKE 'IO-%' OR k.order_no LIKE 'PO-%')", kanbanNo);

            String status = (String) kanban.get("status");
            if (!"pending".equals(status) && !"scanned".equals(status)) {
                return Result.error("看板已使用或无效");
            }

            // Get already received quantity for this kanban
            Integer alreadyReceived = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(quantity), 0) FROM inventory_trace WHERE kanban_no = ? AND action_type = 'INBOUND'",
                    Integer.class, kanbanNo);

            // 获取入库单的仓库代码
            String warehouseCode = jdbcTemplate.queryForObject(
                    "SELECT warehouse_code FROM inbound_order WHERE order_no = ?",
                    String.class, kanban.get("order_no"));

            // 获取仓库名称
            String warehouseName = jdbcTemplate.queryForObject(
                    "SELECT warehouse_name FROM warehouse WHERE warehouse_code = ?",
                    String.class, warehouseCode);

            Map<String, Object> result = new HashMap<>();
            result.put("valid", true);
            result.put("kanbanNo", kanbanNo);
            result.put("partCode", kanban.get("part_code"));
            result.put("partName", kanban.get("part_name"));
            result.put("supplierName", kanban.get("supplier_name"));
            result.put("quantity", kanban.get("quantity"));
            result.put("receivedQuantity", alreadyReceived != null ? alreadyReceived : 0);
            result.put("orderNo", kanban.get("order_no"));
            result.put("warehouseCode", warehouseCode);  // 返回仓库代码
            result.put("warehouseName", warehouseName);  // 返回仓库名称
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("看板无效或不存在");
        }
    }

    // ==================== 库存追溯 ====================

    /**
     * 获取库存追溯记录
     */
    /**
     * 验证看板是否可出库：必须已经形成当前库存，且库存数量大于0。
     */
    @GetMapping("/kanban/outbound-validate/{kanbanNo}")
    public Result<Map<String, Object>> validateOutboundKanban(
            @PathVariable String kanbanNo,
            @RequestParam(required = false) String orderNo) {
        try {
            System.out.println("========== 验证出库看板 ==========");
            System.out.println("看板号: " + kanbanNo);
            System.out.println("订单号: " + orderNo);

            // First check if this kanban exists in inventory (already received/stocked)
            String sql = "SELECT i.*, k.status, s.supplier_name, i.warehouse_code, w.warehouse_name FROM current_inventory i " +
                    "LEFT JOIN kanban k ON i.kanban_no = k.kanban_no " +
                    "LEFT JOIN supplier s ON i.supplier_code = s.supplier_code " +
                    "LEFT JOIN warehouse w ON i.warehouse_code = w.warehouse_code " +
                    "WHERE i.kanban_no = ? AND i.quantity > 0";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, kanbanNo);

            if (!rows.isEmpty()) {
                Map<String, Object> inventory = rows.get(0);

                // Get customer name from the outbound order if orderNo is provided
                String customerName = null;
                String warehouseName = null;
                if (orderNo != null && !orderNo.isEmpty()) {
                    try {
                        Map<String, Object> orderInfo = jdbcTemplate.queryForMap(
                                "SELECT customer_name, warehouse_code FROM outbound_order WHERE order_no = ?",
                                orderNo);
                        customerName = (String) orderInfo.get("customer_name");

                        // Get warehouse name
                        String warehouseCode = (String) orderInfo.get("warehouse_code");
                        if (warehouseCode != null && !warehouseCode.isEmpty()) {
                            try {
                                warehouseName = jdbcTemplate.queryForObject(
                                        "SELECT warehouse_name FROM warehouse WHERE warehouse_code = ?",
                                        String.class, warehouseCode);
                            } catch (Exception e) {
                                warehouseName = warehouseCode;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Could not fetch order details: " + e.getMessage());
                    }
                }

                Map<String, Object> result = new HashMap<>();
                result.put("valid", true);
                result.put("sourceType", "inventory");
                result.put("kanbanNo", inventory.get("kanban_no"));
                result.put("partCode", inventory.get("part_code"));
                result.put("partName", inventory.get("part_name"));
                result.put("supplierCode", inventory.get("supplier_code"));
                result.put("supplierName", inventory.get("supplier_name"));
                result.put("customerName", customerName != null ? customerName : "");
                result.put("quantity", inventory.get("quantity"));
                result.put("warehouseCode", inventory.get("warehouse_code"));
                result.put("warehouseName", inventory.get("warehouse_name") != null ? inventory.get("warehouse_name") : "");
                result.put("status", inventory.get("status"));

                System.out.println("返回库存看板信息: " + result);
                return Result.success(result);
            }

            // If not in inventory, check if it's a pending outbound kanban
            String labelSql = "SELECT k.*, s.supplier_name, o.status AS order_status, o.customer_name, o.warehouse_code FROM kanban k " +
                    "LEFT JOIN supplier s ON k.supplier_code = s.supplier_code " +
                    "LEFT JOIN outbound_order o ON k.order_no = o.order_no " +
                    "WHERE k.kanban_no = ? AND k.status IN ('pending', 'outbound') AND k.order_no LIKE 'SO-%'";
            List<Map<String, Object>> labels = jdbcTemplate.queryForList(labelSql, kanbanNo);

            if (labels.isEmpty()) {
                return Result.error("看板不存在、未入库或当前无可用库存");
            }

            Map<String, Object> label = labels.get(0);
            String labelOrderNo = String.valueOf(label.get("order_no"));
            String orderStatus = label.get("order_status") == null ? "" : String.valueOf(label.get("order_status"));
            String customerName = label.get("customer_name") != null ? String.valueOf(label.get("customer_name")) : "";

            // Get warehouse name
            String warehouseCode = label.get("warehouse_code") != null ? String.valueOf(label.get("warehouse_code")) : "";
            String warehouseName = "";
            if (!warehouseCode.isEmpty()) {
                try {
                    warehouseName = jdbcTemplate.queryForObject(
                            "SELECT warehouse_name FROM warehouse WHERE warehouse_code = ?",
                            String.class, warehouseCode);
                } catch (Exception e) {
                    warehouseName = warehouseCode;
                }
            }

            // Validate order matching
            if (orderNo != null && !orderNo.isEmpty() && !orderNo.equals(labelOrderNo)) {
                return Result.error("看板属于出库单 " + labelOrderNo + "，请先选择对应出库单");
            }
            if ("completed".equals(orderStatus)) {
                return Result.error("出库单已完成");
            }

            String partCode = String.valueOf(label.get("part_code"));
            List<Map<String, Object>> detailRows = jdbcTemplate.queryForList(
                    "SELECT * FROM outbound_order_detail WHERE order_no = ? AND part_code = ?",
                    labelOrderNo, partCode);
            if (detailRows.isEmpty()) {
                return Result.error("零件 " + partCode + " 不在该出库单明细中");
            }

            Map<String, Object> detail = detailRows.get(0);
            int expectedQuantity = ((Number) detail.get("expected_quantity")).intValue();
            int shippedQuantity = ((Number) detail.get("shipped_quantity")).intValue();
            int remainingQuantity = expectedQuantity - shippedQuantity;

            if (remainingQuantity <= 0) {
                return Result.error("零件 " + partCode + " 已完成出库，请扫描其他零件");
            }

            // Get kanban quantity
            int kanbanQuantity = ((Number) label.get("quantity")).intValue();
            // Use the smaller of kanban quantity and remaining order quantity
            int availableQuantity = Math.min(kanbanQuantity, remainingQuantity);

            Map<String, Object> result = new HashMap<>();
            result.put("valid", true);
            result.put("sourceType", "outboundLabel");
            result.put("kanbanNo", label.get("kanban_no"));
            result.put("orderNo", labelOrderNo);
            result.put("partCode", partCode);
            result.put("partName", label.get("part_name"));
            result.put("supplierCode", label.get("supplier_code"));
            result.put("supplierName", label.get("supplier_name"));
            result.put("customerName", customerName);
            result.put("quantity", availableQuantity);
            result.put("kanbanQuantity", kanbanQuantity);
            result.put("remainingOrderQuantity", remainingQuantity);
            result.put("warehouseCode", label.get("warehouse_code"));
            result.put("warehouseName", warehouseName);
            result.put("status", label.get("status"));

            System.out.println("返回出库看板信息: " + result);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("验证出库看板失败: " + e.getMessage());
        }
    }

    @GetMapping("/trace/list")
    public Result<Map<String, Object>> getTraceList(
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) String kanbanNo,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT t.*, s.supplier_name, " +
                    "CASE " +
                    "  WHEN t.order_no LIKE 'SO-%' THEN oo.warehouse_code " +
                    "  WHEN t.order_no LIKE 'PO-%' OR t.order_no LIKE 'IO-%' THEN io.warehouse_code " +
                    "  ELSE k.warehouse_code " +
                    "END as warehouse_code, " +
                    "CASE " +
                    "  WHEN t.order_no LIKE 'SO-%' THEN oo_w.warehouse_name " +
                    "  WHEN t.order_no LIKE 'PO-%' OR t.order_no LIKE 'IO-%' THEN io_w.warehouse_name " +
                    "  ELSE k_w.warehouse_name " +
                    "END as warehouse_name " +
                    "FROM inventory_trace t " +
                    "LEFT JOIN supplier s ON t.supplier_code = s.supplier_code " +
                    "LEFT JOIN kanban k ON t.kanban_no = k.kanban_no " +
                    "LEFT JOIN inbound_order io ON t.order_no = io.order_no " +
                    "LEFT JOIN warehouse io_w ON io.warehouse_code = io_w.warehouse_code " +
                    "LEFT JOIN outbound_order oo ON t.order_no = oo.order_no " +
                    "LEFT JOIN warehouse oo_w ON oo.warehouse_code = oo_w.warehouse_code " +
                    "LEFT JOIN warehouse k_w ON k.warehouse_code = k_w.warehouse_code " +
                    "WHERE 1=1"
            );
            List<Object> params = new ArrayList<>();

            if (partCode != null && !partCode.isEmpty()) {
                sql.append(" AND t.part_code LIKE ?");
                params.add("%" + partCode + "%");
            }
            if (kanbanNo != null && !kanbanNo.isEmpty()) {
                sql.append(" AND t.kanban_no LIKE ?");
                params.add("%" + kanbanNo + "%");
            }
            if (actionType != null && !actionType.isEmpty()) {
                sql.append(" AND t.action_type = ?");
                params.add(actionType);
            }
            if (startDate != null && !startDate.isEmpty()) {
                sql.append(" AND DATE(t.action_time) >= ?");
                params.add(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                sql.append(" AND DATE(t.action_time) <= ?");
                params.add(endDate);
            }

            // 构建countSql的WHERE条件
            StringBuilder countWhere = new StringBuilder(" WHERE 1=1");
            List<Object> countParams = new ArrayList<>();
            if (partCode != null && !partCode.isEmpty()) {
                countWhere.append(" AND t.part_code LIKE ?");
                countParams.add("%" + partCode + "%");
            }
            if (kanbanNo != null && !kanbanNo.isEmpty()) {
                countWhere.append(" AND t.kanban_no LIKE ?");
                countParams.add("%" + kanbanNo + "%");
            }
            if (actionType != null && !actionType.isEmpty()) {
                countWhere.append(" AND t.action_type = ?");
                countParams.add(actionType);
            }
            if (startDate != null && !startDate.isEmpty()) {
                countWhere.append(" AND DATE(t.action_time) >= ?");
                countParams.add(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                countWhere.append(" AND DATE(t.action_time) <= ?");
                countParams.add(endDate);
            }

            // 查询总数
            String countSql = "SELECT COUNT(*) FROM inventory_trace t" + countWhere.toString();
            Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, countParams.toArray());

            // 分页查询
            sql.append(" ORDER BY t.action_time DESC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            List<WmsInventoryTrace> traces = jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
                WmsInventoryTrace trace = new WmsInventoryTrace();
                trace.setId(rs.getInt("id"));
                trace.setTraceNo(rs.getString("trace_no"));
                trace.setKanbanNo(rs.getString("kanban_no"));
                trace.setOrderNo(rs.getString("order_no"));
                trace.setPartCode(rs.getString("part_code"));
                trace.setPartName(rs.getString("part_name"));
                trace.setSupplierCode(rs.getString("supplier_code"));
                trace.setSupplierName(rs.getString("supplier_name"));
                trace.setQuantity(rs.getInt("quantity"));
                trace.setWarehouseCode(rs.getString("warehouse_code"));
                trace.setWarehouseName(rs.getString("warehouse_name"));
                trace.setActionType(rs.getString("action_type"));
                trace.setActionTime(rs.getTimestamp("action_time") != null ? rs.getTimestamp("action_time").toLocalDateTime() : null);
                trace.setOperator(rs.getString("operator"));
                trace.setRemark(rs.getString("remark"));
                return trace;
            });

            Map<String, Object> result = new HashMap<>();
            result.put("list", traces);
            result.put("total", total != null ? total : 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取追溯记录失败: " + e.getMessage());
        }
    }

    // ==================== 出库单管理 ====================

    /**
     * 生成出库单号（带重试机制，避免并发冲突）
     */
    private String generateOutboundOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int maxRetries = 3;

        for (int i = 0; i < maxRetries; i++) {
            String sql = "SELECT COUNT(*) FROM outbound_order WHERE order_no LIKE CONCAT('SO-', ?, '%')";
            int count = jdbcTemplate.queryForObject(sql, Integer.class, date);
            String orderNo = "SO-" + date + "-" + String.format("%04d", count + 1);

            // 检查单号是否已存在
            String checkSql = "SELECT COUNT(*) FROM outbound_order WHERE order_no = ?";
            Integer exists = jdbcTemplate.queryForObject(checkSql, Integer.class, orderNo);

            if (exists == null || exists == 0) {
                return orderNo; // 单号可用
            }

            // 单号已存在，等待10ms后重试
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 重试失败，使用时间戳作为后缀
        return "SO-" + date + "-" + System.currentTimeMillis() % 10000;
    }

    /**
     * 创建出库单
     */
    @PostMapping("/outbound-order/create")
    public Result<Map<String, Object>> createOutboundOrder(@RequestBody WmsOutboundOrder order, HttpSession session) {
        try {
            String orderNo = generateOutboundOrderNo();
            String username = (String) session.getAttribute("user");
            if (username == null) username = "system";

            String warehouseCode = order.getWarehouseCode();
            if (warehouseCode == null || warehouseCode.isEmpty()) {
                return Result.error("请选择目标仓库");
            }

            // 验证库存：检查目标仓库是否有足够的库存
            if (order.getDetails() != null && !order.getDetails().isEmpty()) {
                for (WmsOutboundOrderDetail detail : order.getDetails()) {
                    String partCode = detail.getPartCode();
                    Integer expectedQty = detail.getExpectedQuantity();

                    if (partCode == null || partCode.isEmpty()) {
                        return Result.error("零件号不能为空");
                    }
                    if (expectedQty == null || expectedQty <= 0) {
                        return Result.error("零件 " + partCode + " 的预期数量必须大于0");
                    }

                    // 查询目标仓库的可用库存
                    String checkSql = "SELECT SUM(ci.quantity) as available_qty " +
                            "FROM current_inventory ci " +
                            "WHERE ci.part_code = ? " +
                            "AND ci.warehouse_code = ? " +
                            "AND ci.quantity > 0";

                    Integer availableQty = jdbcTemplate.queryForObject(checkSql, Integer.class, partCode, warehouseCode);

                    if (availableQty == null || availableQty <= 0) {
                        return Result.error("仓库 " + warehouseCode + " 没有零件 " + partCode + " 的库存，无法创建出库单");
                    }

                    if (availableQty < expectedQty) {
                        return Result.error("仓库 " + warehouseCode + " 的零件 " + partCode + " 库存不足，" +
                                "当前库存: " + availableQty + ", 需要: " + expectedQty);
                    }
                }
            }

            // 计算总数量和总箱数
            int totalQuantity = 0;
            int totalBoxes = 0;
            if (order.getDetails() != null) {
                for (WmsOutboundOrderDetail detail : order.getDetails()) {
                    totalQuantity += (detail.getExpectedQuantity() != null ? detail.getExpectedQuantity() : 0);
                    totalBoxes += (detail.getExpectedBoxes() != null ? detail.getExpectedBoxes() : 0);
                }
            }

            // 插入主表
            String sql = "INSERT INTO outbound_order (order_no, outbound_type, customer_code, customer_name, warehouse_code, " +
                    "total_quantity, total_boxes, remark, created_by, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')";
            jdbcTemplate.update(sql, orderNo, order.getOutboundType(), order.getCustomerCode(),
                    order.getCustomerName(), order.getWarehouseCode(), totalQuantity, totalBoxes,
                    order.getRemark(), username);

            // 插入明细
            if (order.getDetails() != null) {
                for (WmsOutboundOrderDetail detail : order.getDetails()) {
                    String detailSql = "INSERT INTO outbound_order_detail (order_no, part_code, part_name, " +
                            "expected_quantity, packaging_capacity, expected_boxes, unit) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(detailSql, orderNo, detail.getPartCode(), detail.getPartName(),
                            detail.getExpectedQuantity(), detail.getPackagingCapacity(),
                            detail.getExpectedBoxes(), detail.getUnit());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("orderNo", orderNo);
            result.put("success", true);
            return Result.success(result);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            return Result.error("出库单号重复，请重试");
        } catch (Exception e) {
            e.printStackTrace();
            // 提取简洁的错误信息
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Duplicate entry")) {
                return Result.error("数据重复，请检查后重试");
            }
            if (errorMsg != null && errorMsg.length() > 100) {
                return Result.error("操作失败: " + errorMsg.substring(0, 100));
            }
            return Result.error("创建出库单失败: " + (errorMsg != null ? errorMsg : "未知错误"));
        }
    }

    /**
     * 获取出库单列表
     */
    @GetMapping("/outbound-order/list")
    public Result<Map<String, Object>> getOutboundOrderList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT o.*, w.warehouse_name FROM outbound_order o " +
                            "LEFT JOIN warehouse w ON o.warehouse_code = w.warehouse_code WHERE 1=1 "
            );
            List<Object> params = new ArrayList<>();

            if (status != null && !status.isEmpty()) {
                sql.append("AND o.status = ? ");
                params.add(status);
            }
            if (customerCode != null && !customerCode.isEmpty()) {
                sql.append("AND o.customer_code = ? ");
                params.add(customerCode);
            }
            if (startDate != null && !startDate.isEmpty()) {
                sql.append("AND DATE(o.created_at) >= ? ");
                params.add(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                sql.append("AND DATE(o.created_at) <= ? ");
                params.add(endDate);
            }

            // 查询总数
            String countSql = sql.toString().replace("SELECT o.*, w.warehouse_name", "SELECT COUNT(*)");
            Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

            // 分页查询
            sql.append("ORDER BY o.created_at DESC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            List<WmsOutboundOrder> orders = jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
                WmsOutboundOrder order = new WmsOutboundOrder();
                order.setId(rs.getInt("id"));
                order.setOrderNo(rs.getString("order_no"));
                order.setOutboundType(rs.getString("outbound_type"));
                order.setCustomerCode(rs.getString("customer_code"));
                order.setCustomerName(rs.getString("customer_name"));
                order.setWarehouseCode(rs.getString("warehouse_code"));
                order.setWarehouseName(rs.getString("warehouse_name"));
                order.setStatus(rs.getString("status"));
                order.setTotalQuantity(rs.getInt("total_quantity"));
                order.setShippedQuantity(rs.getInt("shipped_quantity"));
                order.setTotalBoxes(rs.getInt("total_boxes"));
                order.setShippedBoxes(rs.getInt("shipped_boxes"));
                order.setRemark(rs.getString("remark"));
                order.setCreatedBy(rs.getString("created_by"));
                order.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
                order.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
                return order;
            });

            Map<String, Object> result = new HashMap<>();
            result.put("list", orders);
            result.put("total", total != null ? total : 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取出库单列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取出库单详情
     */
    @GetMapping("/outbound-order/detail/{orderNo}")
    public Result<Map<String, Object>> getOutboundOrderDetail(@PathVariable String orderNo) {
        try {
            String orderSql = "SELECT o.*, w.warehouse_name FROM outbound_order o " +
                    "LEFT JOIN warehouse w ON o.warehouse_code = w.warehouse_code WHERE o.order_no = ?";

            List<Map<String, Object>> orders = jdbcTemplate.queryForList(orderSql, orderNo);
            if (orders.isEmpty()) {
                return Result.error("出库单不存在: " + orderNo);
            }
            Map<String, Object> order = orders.get(0);

            // 获取明细
            String detailSql = "SELECT * FROM outbound_order_detail WHERE order_no = ?";
            List<Map<String, Object>> details = jdbcTemplate.queryForList(detailSql, orderNo);

            // 获取可用库存看板（FIFO先进先出）
            List<Map<String, Object>> availableKanbans = new ArrayList<>();
            String warehouseCode = (String) order.get("warehouse_code");
            
            for (Map<String, Object> detail : details) {
                String partCode = (String) detail.get("part_code");
                int expectedQty = ((Number) detail.get("expected_quantity")).intValue();
                int shippedQty = ((Number) detail.get("shipped_quantity")).intValue();
                int remainingQty = expectedQty - shippedQty;
                
                if (remainingQty > 0) {
                    // 查询该零件的可用库存看板（按入库时间排序）
                    String kanbanSql = "SELECT ci.kanban_no, k.order_no, ci.part_code, COALESCE(k.part_name, ci.part_name) AS part_name, ci.supplier_code, " +
                            "ci.warehouse_code, s.supplier_name, " +
                            "COALESCE(k.quantity, ci.quantity) AS quantity, COALESCE(k.status, ci.status) AS status, COALESCE(k.is_sealed, 0) AS is_sealed, COALESCE(k.created_at, ci.created_at) AS created_at, " +
                            "ci.quantity as available_quantity " +
                            "FROM current_inventory ci " +
                            "LEFT JOIN kanban k ON ci.kanban_no = k.kanban_no " +
                            "LEFT JOIN supplier s ON ci.supplier_code = s.supplier_code " +
                            "WHERE ci.part_code = ? " +
                            "  AND ci.quantity > 0 " +
                            "  AND (k.kanban_no IS NULL OR (k.status IN ('scanned', 'stored') AND k.is_sealed = 0))";
                    
                    if (warehouseCode != null && !warehouseCode.isEmpty()) {
                        kanbanSql += " AND ci.warehouse_code = ?";
                    }
                    
                    kanbanSql += " ORDER BY COALESCE(k.created_at, ci.created_at) ASC, ci.id ASC";
                    
                    List<Map<String, Object>> kanbans;
                    if (warehouseCode != null && !warehouseCode.isEmpty()) {
                        kanbans = jdbcTemplate.queryForList(kanbanSql, partCode, warehouseCode);
                    } else {
                        kanbans = jdbcTemplate.queryForList(kanbanSql, partCode);
                    }
                    
                    availableKanbans.addAll(limitFifoKanbansByRequiredQuantity(kanbans, remainingQty));
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("order", order);
            result.put("details", details);
            result.put("availableKanbans", availableKanbans);  // 可用的入库看板
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取出库单详情失败: " + e.getMessage());
        }
    }

    /**
     * 修改出库单
     */
    @PutMapping("/outbound-order/update/{orderNo}")
    public Result<Map<String, Object>> updateOutboundOrder(@PathVariable String orderNo, @RequestBody WmsOutboundOrder order) {
        try {
            // 检查状态
            String statusSql = "SELECT status FROM outbound_order WHERE order_no = ?";
            String status = jdbcTemplate.queryForObject(statusSql, String.class, orderNo);
            if ("completed".equals(status)) {
                return Result.error("已完成出库的单据不能修改");
            }

            // 更新主表
            String sql = "UPDATE outbound_order SET outbound_type=?, warehouse_code=?, remark=? WHERE order_no=?";
            jdbcTemplate.update(sql, order.getOutboundType(), order.getWarehouseCode(), order.getRemark(), orderNo);

            // 删除旧明细
            jdbcTemplate.update("DELETE FROM outbound_order_detail WHERE order_no = ?", orderNo);

            // 插入新明细
            if (order.getDetails() != null) {
                for (WmsOutboundOrderDetail detail : order.getDetails()) {
                    String detailSql = "INSERT INTO outbound_order_detail (order_no, part_code, part_name, " +
                            "expected_quantity, packaging_capacity, expected_boxes, unit) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(detailSql, orderNo, detail.getPartCode(), detail.getPartName(),
                            detail.getExpectedQuantity(), detail.getPackagingCapacity(),
                            detail.getExpectedBoxes(), detail.getUnit());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("修改出库单失败: " + e.getMessage());
        }
    }

    /**
     * 删除出库单
     */
    @DeleteMapping("/outbound-order/delete/{orderNo}")
    public Result<Map<String, Object>> deleteOutboundOrder(@PathVariable String orderNo) {
        try {
            String statusSql = "SELECT status FROM outbound_order WHERE order_no = ?";
            String status = jdbcTemplate.queryForObject(statusSql, String.class, orderNo);
            if ("completed".equals(status)) {
                return Result.error("已完成出库的单据不能删除");
            }

            // 删除明细
            jdbcTemplate.update("DELETE FROM outbound_order_detail WHERE order_no = ?", orderNo);
            // 删除主表
            jdbcTemplate.update("DELETE FROM outbound_order WHERE order_no = ?", orderNo);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("删除出库单失败: " + e.getMessage());
        }
    }

    // ==================== 扫码出库 ====================

    /**
     * 扫码出库 - 执行FIFO先进先出
     */
    @Transactional
    @PostMapping("/scan/outbound")
    public Result<Map<String, Object>> scanOutbound(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            String orderNo = (String) request.get("orderNo");
            String partCode = (String) request.get("partCode");
            Object kanbanValue = request.get("kanbanNo");
            String scannedKanbanNo = kanbanValue == null ? "" : kanbanValue.toString().trim();
            Object quantityValue = request.get("quantity");
            if (!(quantityValue instanceof Number)) {
                return Result.error("出库数量必须为数字");
            }
            Integer quantity = ((Number) quantityValue).intValue();
            String username = (String) session.getAttribute("user");
            if (username == null) username = "system";

            if (orderNo == null || orderNo.isEmpty() || partCode == null || partCode.isEmpty()) {
                return Result.error("出库单号和零件号不能为空");
            }
            if (scannedKanbanNo.isEmpty()) {
                return Result.error("请先扫描库存看板后再出库");
            }
            if (quantity == null || quantity <= 0) {
                return Result.error("出库数量必须大于0");
            }

            System.out.println("========== 扫码出库开始 ==========");
            System.out.println("订单号: " + orderNo);
            System.out.println("零件号: " + partCode);
            System.out.println("扫描的看板号(入库看板): " + scannedKanbanNo);
            System.out.println("出库数量: " + quantity);
            System.out.println("✅ 单看板策略：直接扫描入库看板完成出库");

            // 1. 验证出库单状态并获取仓库代码
            Map<String, Object> orderInfo = jdbcTemplate.queryForMap(
                    "SELECT status, warehouse_code FROM outbound_order WHERE order_no = ?", orderNo);
            String orderStatus = (String) orderInfo.get("status");
            String warehouseCode = (String) orderInfo.get("warehouse_code");

            if ("completed".equals(orderStatus)) {
                return Result.error("出库单已完成");
            }

            if (warehouseCode == null || warehouseCode.isEmpty()) {
                return Result.error("出库单未指定仓库");
            }

            // 2. 查询出库单明细
            List<Map<String, Object>> detailRows = jdbcTemplate.queryForList(
                    "SELECT * FROM outbound_order_detail WHERE order_no = ? AND part_code = ?",
                    orderNo, partCode);
            if (detailRows.isEmpty()) {
                return Result.error("零件 " + partCode + " 不在该出库单明细中");
            }

            Map<String, Object> detail = detailRows.get(0);
            int expectedQuantity = ((Number) detail.get("expected_quantity")).intValue();
            int shippedInDetail = ((Number) detail.get("shipped_quantity")).intValue();
            int remainingInOrder = expectedQuantity - shippedInDetail;

            System.out.println("订单剩余数量: " + remainingInOrder);

            if (remainingInOrder <= 0) {
                return Result.error("零件 " + partCode + " 已完成出库");
            }

            // 允许部分出库
            if (quantity > remainingInOrder) {
                return Result.error("出库数量(" + quantity + ")超过订单剩余数量(" + remainingInOrder + ")");
            }

            int packagingCapacity = ((Number) detail.get("packaging_capacity")).intValue();
            if (packagingCapacity <= 0) {
                packagingCapacity = 1;
            }

            // 3. 查询当前库存，只查询出库单指定仓库的库存
            // 必须关联kanban表，确保只查询状态为scanned/stored的看板
            // FIFO排序：先按入库时间，时间相同时按ID（保证同一秒入库的看板也有固定顺序）
            // 包含：原始看板、转包看板、合并看板（只要未封存都可以出库）
            String inventorySql = "SELECT ci.* FROM current_inventory ci " +
                    "LEFT JOIN kanban k ON ci.kanban_no = k.kanban_no " +
                    "WHERE ci.part_code = ? " +
                    "AND ci.warehouse_code = ? " +
                    "AND ci.quantity > 0 " +
                    "AND (k.kanban_no IS NULL OR (k.status IN ('scanned', 'stored') AND k.is_sealed = 0)) " +
                    "ORDER BY COALESCE(k.created_at, ci.created_at) ASC, ci.id ASC";
            List<Map<String, Object>> inventoryList = jdbcTemplate.queryForList(inventorySql, partCode, warehouseCode);

            if (inventoryList.isEmpty()) {
                return Result.error("零件 " + partCode + " 库存不足");
            }

            // 4. 计算可用库存总量
            int totalAvailable = inventoryList.stream()
                    .mapToInt(inv -> ((Number) inv.get("quantity")).intValue())
                    .sum();

            System.out.println("可用库存总量: " + totalAvailable);

            if (totalAvailable < quantity) {
                return Result.error("库存不足,当前可用: " + totalAvailable + ", 需要: " + quantity);
            }

            // 5. 检查扫描的入库看板是否符合FIFO
            String fifoKanbanNo = String.valueOf(inventoryList.get(0).get("kanban_no"));
            if (!scannedKanbanNo.equals(fifoKanbanNo)) {
                return Result.error("不符合FIFO先进先出，请先出库最早入库看板 " + fifoKanbanNo);
            }

            // 验证扫描的看板是否在库存中
            Map<String, Object> scannedInventory = null;
            for (Map<String, Object> inventory : inventoryList) {
                if (scannedKanbanNo.equals(String.valueOf(inventory.get("kanban_no")))) {
                    scannedInventory = inventory;
                    break;
                }
            }

            if (scannedInventory == null) {
                return Result.error("扫描的看板 " + scannedKanbanNo + " 不在当前库存中");
            }

            int scannedAvailable = ((Number) scannedInventory.get("quantity")).intValue();
            if (quantity > scannedAvailable) {
                return Result.error("出库数量超过看板当前库存，当前看板可用: " + scannedAvailable);
            }

            // 6. 按FIFO原则扣减库存
            int remainingQuantity = quantity;
            int totalBoxes = 0;

            for (Map<String, Object> inventory : inventoryList) {
                if (remainingQuantity <= 0) break;

                int currentQty = ((Number) inventory.get("quantity")).intValue();
                String kanbanNo2 = (String) inventory.get("kanban_no");
                String locationCode2 = (String) inventory.get("warehouse_code");
                int inventoryQty = Math.min(remainingQuantity, currentQty);
                int boxes = (int) Math.ceil((double) inventoryQty / packagingCapacity);

                // 更新当前库存
                if (inventoryQty == currentQty) {
                    // 全部出库，删除记录
                    jdbcTemplate.update("DELETE FROM current_inventory WHERE kanban_no = ? AND quantity >= ?",
                            kanbanNo2, inventoryQty);
                    // 更新看板状态为outbound（如果不是待出库看板）
                    jdbcTemplate.update("UPDATE kanban SET status = 'outbound' WHERE kanban_no = ? AND status != 'outbound'", kanbanNo2);
                } else {
                    // 部分出库
                    int updated = jdbcTemplate.update(
                            "UPDATE current_inventory SET quantity = quantity - ? WHERE kanban_no = ? AND quantity >= ?",
                            inventoryQty, kanbanNo2, inventoryQty);

                    if (updated == 0) {
                        return Result.error("库存不足，扣减失败（看板: " + kanbanNo2 + "）");
                    }
                    // 部分出库不更新看板状态，允许继续扫码
                }

                remainingQuantity -= inventoryQty;
                totalBoxes += boxes;

                // ============================================
                // 添加出库追溯记录（单看板策略）
                // kanban_no: 直接存储扫描的入库看板号
                // ============================================
                String traceNo = "TRACE-OUT-" + System.currentTimeMillis() + "-" + kanbanNo2;

                jdbcTemplate.update("INSERT INTO inventory_trace (trace_no, kanban_no, order_no, part_code, part_name, " +
                                "supplier_code, quantity, warehouse_code, action_type, operator) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OUTBOUND', ?)",
                        traceNo,
                        kanbanNo2,  // 入库看板号
                        orderNo,
                        partCode,
                        inventory.get("part_name"),
                        inventory.get("supplier_code"),
                        inventoryQty,
                        warehouseCode,  // 仓库代码
                        username);

                System.out.println("✅ 创建出库追溯记录: " + traceNo);
                System.out.println("   看板号(入库): " + kanbanNo2);
                System.out.println("   出库单: " + orderNo);
                System.out.println("   数量: " + inventoryQty);
            }

            // 7. 更新出库单已出库数量
            jdbcTemplate.update("UPDATE outbound_order SET shipped_quantity = IFNULL(shipped_quantity, 0) + ?, " +
                    "shipped_boxes = IFNULL(shipped_boxes, 0) + ? WHERE order_no = ?", quantity, totalBoxes, orderNo);

            jdbcTemplate.update("UPDATE outbound_order_detail SET shipped_quantity = IFNULL(shipped_quantity, 0) + ?, " +
                            "shipped_boxes = IFNULL(shipped_boxes, 0) + ? WHERE order_no = ? AND part_code = ?",
                    quantity, totalBoxes, orderNo, partCode);

            // 8. 检查出库单是否完成
            Integer totalQuantity = jdbcTemplate.queryForObject(
                    "SELECT total_quantity FROM outbound_order WHERE order_no = ?", Integer.class, orderNo);
            Integer shippedQuantity = jdbcTemplate.queryForObject(
                    "SELECT shipped_quantity FROM outbound_order WHERE order_no = ?", Integer.class, orderNo);

            if (shippedQuantity != null && totalQuantity != null && shippedQuantity >= totalQuantity) {
                jdbcTemplate.update("UPDATE outbound_order SET status = 'completed' WHERE order_no = ?", orderNo);
                // Update all pending kanbans for this order to outbound
                jdbcTemplate.update("UPDATE kanban SET status = 'outbound' WHERE order_no = ? AND status = 'pending'", orderNo);
            } else {
                jdbcTemplate.update("UPDATE outbound_order SET status = 'partial' WHERE order_no = ?", orderNo);
            }

            System.out.println("✅ 出库成功: " + quantity + " 个, 剩余订单数量: " + (remainingInOrder - quantity));
            System.out.println("========== 扫码出库结束 ==========");

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "出库成功");
            result.put("orderNo", orderNo);
            result.put("kanbanNo", scannedKanbanNo);
            result.put("partCode", partCode);
            result.put("quantity", quantity);
            result.put("boxes", totalBoxes);
            result.put("remainingOrderQuantity", remainingInOrder - quantity);
            return Result.success(result);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            return Result.error("扫码出库失败: " + e.getMessage());
        }
    }

    /**
     * 验证出库单是否有效
     */
    @GetMapping("/outbound-order/validate/{orderNo}")
    public Result<Map<String, Object>> validateOutboundOrder(@PathVariable String orderNo) {
        try {
            Map<String, Object> order = jdbcTemplate.queryForMap(
                    "SELECT o.*, w.warehouse_name FROM outbound_order o " +
                            "LEFT JOIN warehouse w ON o.warehouse_code = w.warehouse_code " +
                            "WHERE o.order_no = ?", orderNo);

            String status = (String) order.get("status");
            if ("completed".equals(status)) {
                return Result.error("出库单已完成");
            }

            // 获取明细
            String detailSql = "SELECT * FROM outbound_order_detail WHERE order_no = ?";
            List<Map<String, Object>> details = jdbcTemplate.queryForList(detailSql, orderNo);

            String warehouseCode = (String) order.get("warehouse_code");
            
            // 为每个零件查询可用库存看板（FIFO）
            List<Map<String, Object>> fifoRecommendations = new ArrayList<>();
            
            for (Map<String, Object> detail : details) {
                String partCode = (String) detail.get("part_code");
                String partName = (String) detail.get("part_name");
                int expectedQty = ((Number) detail.get("expected_quantity")).intValue();
                int shippedQty = ((Number) detail.get("shipped_quantity")).intValue();
                int remainingQty = expectedQty - shippedQty;
                
                if (remainingQty <= 0) continue;
                
                // 查询可用库存看板（按入库时间排序）
                String kanbanSql = "SELECT k.kanban_no, k.part_code, k.part_name, k.supplier_code, " +
                        "k.warehouse_code, s.supplier_name, " +
                        "ci.quantity as available_quantity, k.created_at " +
                        "FROM kanban k " +
                        "INNER JOIN current_inventory ci ON k.kanban_no = ci.kanban_no " +
                        "LEFT JOIN supplier s ON k.supplier_code = s.supplier_code " +
                        "WHERE k.part_code = ? " +
                        "  AND k.status IN ('scanned', 'stored') " +
                        "  AND k.is_sealed = 0 " +
                        "  AND ci.quantity > 0";
                
                List<Object> params = new ArrayList<>();
                params.add(partCode);
                
                if (warehouseCode != null && !warehouseCode.isEmpty()) {
                    kanbanSql += " AND ci.warehouse_code = ?";
                    params.add(warehouseCode);
                }
                
                kanbanSql += " ORDER BY k.created_at ASC";
                
                List<Map<String, Object>> kanbans = jdbcTemplate.queryForList(kanbanSql, params.toArray());
                
                // 计算该零件的总可用库存
                int totalAvailable = kanbans.stream()
                        .mapToInt(k -> ((Number) k.get("available_quantity")).intValue())
                        .sum();
                
                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("partCode", partCode);
                recommendation.put("partName", partName);
                recommendation.put("requiredQuantity", remainingQty);
                recommendation.put("totalAvailable", totalAvailable);
                recommendation.put("isSufficient", totalAvailable >= remainingQty);
                recommendation.put("kanbans", kanbans);
                
                fifoRecommendations.add(recommendation);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("valid", true);
            result.put("orderNo", orderNo);
            result.put("outboundType", order.get("outbound_type"));
            result.put("customerName", order.get("customer_name"));
            result.put("warehouseName", order.get("warehouse_name"));
            result.put("totalQuantity", order.get("total_quantity"));
            result.put("shippedQuantity", order.get("shipped_quantity"));
            result.put("details", details);
            result.put("fifoRecommendations", fifoRecommendations);  // FIFO推荐的入库看板
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("出库单无效或不存在");
        }
    }

    /**
     * 获取FIFO库存列表
     */
    @GetMapping("/inventory/fifo-list")
    public Result<List<Map<String, Object>>> getFifoInventoryList(
            @RequestParam String partCode,
            @RequestParam(required = false) String warehouseCode) {
        try {
            // FIFO排序：使用kanban.created_at（实际入库时间），不是current_inventory.created_at
            // 使用LEFT JOIN兼容看板可能不存在的情况
            StringBuilder sql = new StringBuilder(
                    "SELECT i.*, COALESCE(k.part_name, i.part_name) AS part_name, s.supplier_name, w.warehouse_name " +
                    "FROM current_inventory i " +
                    "LEFT JOIN kanban k ON i.kanban_no = k.kanban_no " +
                    "LEFT JOIN supplier s ON i.supplier_code = s.supplier_code " +
                    "LEFT JOIN warehouse w ON i.warehouse_code = w.warehouse_code " +
                    "WHERE i.part_code = ? AND i.quantity > 0 " +
                    "  AND (k.kanban_no IS NULL OR (k.status IN ('scanned', 'stored') AND k.is_sealed = 0))"
            );
            
            List<Object> params = new ArrayList<>();
            params.add(partCode);
            
            // 如果指定了仓库，加上仓库过滤（与scanOutbound保持一致）
            if (warehouseCode != null && !warehouseCode.isEmpty()) {
                sql.append(" AND i.warehouse_code = ?");
                params.add(warehouseCode);
            }
            
            sql.append(" ORDER BY COALESCE(k.created_at, i.created_at) ASC, i.id ASC");
            
            List<Map<String, Object>> inventory = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            return Result.success(inventory);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取FIFO库存列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前库存
     */
    @GetMapping("/inventory/current")
    public Result<Map<String, Object>> getCurrentInventory(
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) String locationCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT i.kanban_no, i.part_code, COALESCE(k.part_name, i.part_name) AS part_name, i.supplier_code, s.supplier_name, i.quantity, i.warehouse_code, w.warehouse_name, i.status, i.created_at, i.updated_at FROM current_inventory i " +
                            "LEFT JOIN kanban k ON i.kanban_no = k.kanban_no " +
                            "LEFT JOIN supplier s ON i.supplier_code = s.supplier_code " +
                            "LEFT JOIN warehouse w ON i.warehouse_code = w.warehouse_code WHERE i.quantity > 0 AND (k.kanban_no IS NULL OR k.is_sealed = 0)"
            );
            List<Object> params = new ArrayList<>();

            if (partCode != null && !partCode.isEmpty()) {
                sql.append(" AND i.part_code LIKE ?");
                params.add("%" + partCode + "%");
            }
            if (locationCode != null && !locationCode.isEmpty()) {
                sql.append(" AND i.warehouse_code = ?");
                params.add(locationCode);
            }

            // 查询总数
            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM current_inventory i LEFT JOIN kanban k ON i.kanban_no = k.kanban_no WHERE i.quantity > 0 AND (k.kanban_no IS NULL OR k.is_sealed = 0)");
            if (partCode != null && !partCode.isEmpty()) {
                countSql.append(" AND i.part_code LIKE ?");
            }
            if (locationCode != null && !locationCode.isEmpty()) {
                countSql.append(" AND i.warehouse_code = ?");
            }
            Integer total = jdbcTemplate.queryForObject(countSql.toString(), Integer.class, params.toArray());

            // 分页查询
            sql.append(" ORDER BY i.created_at DESC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            List<Map<String, Object>> inventory = jdbcTemplate.queryForList(sql.toString(), params.toArray());

            // 统计汇总（应用筛选条件，不受分页影响）
            StringBuilder sumSql = new StringBuilder(
                    "SELECT " +
                    "COALESCE(SUM(i.quantity), 0) as total_quantity, " +
                    "COUNT(*) as total_count, " +
                    "COUNT(DISTINCT i.part_code) as part_types, " +
                    "COUNT(DISTINCT i.warehouse_code) as warehouse_count " +
                    "FROM current_inventory i " +
                    "LEFT JOIN kanban k ON i.kanban_no = k.kanban_no " +
                    "LEFT JOIN supplier s ON i.supplier_code = s.supplier_code " +
                    "WHERE i.quantity > 0 AND (k.kanban_no IS NULL OR k.is_sealed = 0)"
            );
            
            // 构建统计SQL的参数列表（只包含筛选条件参数，不包含分页参数）
            List<Object> sumParams = new ArrayList<>();
            if (partCode != null && !partCode.isEmpty()) {
                sumSql.append(" AND i.part_code LIKE ?");
                sumParams.add("%" + partCode + "%");
            }
            if (locationCode != null && !locationCode.isEmpty()) {
                sumSql.append(" AND i.warehouse_code = ?");
                sumParams.add(locationCode);
            }
            
            Map<String, Object> summary = jdbcTemplate.queryForMap(sumSql.toString(), sumParams.toArray());

            Map<String, Object> result = new HashMap<>();
            result.put("list", inventory);
            result.put("total", total != null ? total : 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("summary", summary);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取库存失败: " + e.getMessage());
        }
    }

    /**
     * 根据看板追溯
     */
    @GetMapping("/kanban/lifecycle/{kanbanNo}")
    public Result<Map<String, Object>> getKanbanLifecycle(@PathVariable String kanbanNo) {
        try {
            System.out.println("========== 获取看板生命周期 ==========");
            System.out.println("看板号: " + kanbanNo);

            // 1. 获取看板基本信息
            String kanbanSql = "SELECT k.* FROM kanban k WHERE k.kanban_no = ?";
            List<Map<String, Object>> kanbans = jdbcTemplate.queryForList(kanbanSql, kanbanNo);
            if (kanbans.isEmpty()) {
                return Result.error("看板不存在");
            }

            Map<String, Object> kanban = kanbans.get(0);
            System.out.println("看板信息: " + kanban);

            String orderNo = (String) kanban.get("order_no");
            String partCode = (String) kanban.get("part_code");
            String status = (String) kanban.get("status");
            boolean isOutbound = orderNo != null && orderNo.startsWith("SO-");

            // 2. 获取客户名称（出库单）或供应商名称（入库单）
            String customerName = null;
            String supplierName = null;
            String warehouseName = null;

            if (isOutbound) {
                // 出库单 - 获取客户信息
                try {
                    Map<String, Object> orderInfo = jdbcTemplate.queryForMap(
                            "SELECT customer_name, warehouse_code FROM outbound_order WHERE order_no = ?",
                            orderNo);
                    customerName = (String) orderInfo.get("customer_name");

                    // 获取仓库信息
                    String warehouseCode = (String) orderInfo.get("warehouse_code");
                    if (warehouseCode != null && !warehouseCode.isEmpty()) {
                        try {
                            warehouseName = jdbcTemplate.queryForObject(
                                    "SELECT warehouse_name FROM warehouse WHERE warehouse_code = ?",
                                    String.class, warehouseCode);
                        } catch (Exception e) {
                            warehouseName = warehouseCode;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("获取出库单信息失败: " + e.getMessage());
                }

                // 尝试从 kanban 表获取 supplier_name（如果有）
                if (kanban.get("supplier_code") != null) {
                    try {
                        supplierName = jdbcTemplate.queryForObject(
                                "SELECT supplier_name FROM supplier WHERE supplier_code = ?",
                                String.class, kanban.get("supplier_code"));
                    } catch (Exception e) {
                        // Supplier not found
                    }
                }

            } else {
                // 入库单 - 获取供应商信息
                if (kanban.get("supplier_code") != null) {
                    try {
                        supplierName = jdbcTemplate.queryForObject(
                                "SELECT supplier_name FROM supplier WHERE supplier_code = ?",
                                String.class, kanban.get("supplier_code"));
                    } catch (Exception e) {
                        System.out.println("获取供应商失败: " + e.getMessage());
                    }
                }

                // 获取仓库信息
                if (kanban.get("warehouse_code") != null) {
                    try {
                        warehouseName = jdbcTemplate.queryForObject(
                                "SELECT warehouse_name FROM warehouse WHERE warehouse_code = ?",
                                String.class, kanban.get("warehouse_code"));
                    } catch (Exception e) {
                        warehouseName = (String) kanban.get("warehouse_code");
                    }
                }
            }

            // 设置显示名称
            String displayName = isOutbound ? customerName : supplierName;
            if (displayName == null || displayName.isEmpty()) {
                displayName = isOutbound ? "客户" : "供应商";
            }

            // 更新看板信息
            if (isOutbound) {
                kanban.put("customer_name", customerName != null ? customerName : "-");
                kanban.put("supplier_name", customerName != null ? customerName : "-");
            } else {
                kanban.put("supplier_name", supplierName != null ? supplierName : "-");
            }
            kanban.put("warehouse_name", warehouseName != null ? warehouseName : "-");
            kanban.put("is_outbound", isOutbound);

            // 3. 获取当前库存
            List<Map<String, Object>> currentInventory;
            if (isOutbound) {
                // 出库看板不在current_inventory表中，返回空列表
                currentInventory = new java.util.ArrayList<>();
                System.out.println("出库看板，current_inventory为空");
            } else {
                // 入库看板：查询当前库存
                String inventorySql = "SELECT i.*, k.part_name, s.supplier_name, w.warehouse_name FROM current_inventory i " +
                        "INNER JOIN kanban k ON i.kanban_no = k.kanban_no " +
                        "LEFT JOIN supplier s ON i.supplier_code = s.supplier_code " +
                        "LEFT JOIN warehouse w ON i.warehouse_code = w.warehouse_code " +
                        "WHERE i.kanban_no = ? AND i.quantity > 0";
                currentInventory = jdbcTemplate.queryForList(inventorySql, kanbanNo);
                System.out.println("入库看板当前库存: " + currentInventory.size() + " 条");
            }

            // 4. 获取所有追溯记录（单看板策略：直接查询kanban_no）
            String traceSql = "SELECT t.*, s.supplier_name, " +
                    "CASE " +
                    "  WHEN t.order_no LIKE 'SO-%' THEN oo.warehouse_code " +
                    "  WHEN t.order_no LIKE 'PO-%' OR t.order_no LIKE 'IO-%' THEN io.warehouse_code " +
                    "  ELSE k.warehouse_code " +
                    "END as warehouse_code, " +
                    "CASE " +
                    "  WHEN t.order_no LIKE 'SO-%' THEN oo_w.warehouse_name " +
                    "  WHEN t.order_no LIKE 'PO-%' OR t.order_no LIKE 'IO-%' THEN io_w.warehouse_name " +
                    "  ELSE k_w.warehouse_name " +
                    "END as warehouse_name " +
                    "FROM inventory_trace t " +
                    "LEFT JOIN supplier s ON t.supplier_code = s.supplier_code " +
                    "LEFT JOIN kanban k ON t.kanban_no = k.kanban_no " +
                    "LEFT JOIN inbound_order io ON t.order_no = io.order_no " +
                    "LEFT JOIN warehouse io_w ON io.warehouse_code = io_w.warehouse_code " +
                    "LEFT JOIN outbound_order oo ON t.order_no = oo.order_no " +
                    "LEFT JOIN warehouse oo_w ON oo.warehouse_code = oo_w.warehouse_code " +
                    "LEFT JOIN warehouse k_w ON k.warehouse_code = k_w.warehouse_code " +
                    "WHERE t.kanban_no = ? ORDER BY t.action_time ASC";
            List<Map<String, Object>> traces = jdbcTemplate.queryForList(traceSql, kanbanNo);
            
            System.out.println("========== 查询看板追溯记录 ==========");
            System.out.println("看板号: " + kanbanNo);
            System.out.println("SQL: " + traceSql);
            System.out.println("找到追溯记录数量: " + traces.size());
            if (!traces.isEmpty()) {
                System.out.println("前3条追溯记录:");
                for (int i = 0; i < Math.min(3, traces.size()); i++) {
                    Map<String, Object> trace = traces.get(i);
                    System.out.println("  [" + (i+1) + "] trace_no=" + trace.get("trace_no") + 
                            ", kanban_no=" + trace.get("kanban_no") + 
                            ", action_type=" + trace.get("action_type") + 
                            ", quantity=" + trace.get("quantity") +
                            ", order_no=" + trace.get("order_no"));
                }
            } else {
                System.out.println("⚠️ 未找到任何追溯记录！");
            }

            // 5. 为追溯记录补充信息
            for (Map<String, Object> trace : traces) {
                // 对出库看板追溯，补充客户名称
                if (isOutbound) {
                    trace.put("customer_name", customerName != null ? customerName : "-");
                    trace.put("supplier_name", customerName != null ? customerName : "-");
                    trace.put("warehouse_name", trace.get("warehouse_name") != null ? trace.get("warehouse_name") : "-");
                } else {
                    if (trace.get("supplier_name") == null || ((String) trace.get("supplier_name")).isEmpty()) {
                        trace.put("supplier_name", supplierName != null ? supplierName : "-");
                    }
                    if (trace.get("warehouse_code") == null || ((String) trace.get("warehouse_code")).isEmpty()) {
                        trace.put("warehouse_code", kanban.get("warehouse_code") != null ? kanban.get("warehouse_code") : "-");
                    }
                    if (trace.get("warehouse_name") == null || ((String) trace.get("warehouse_name")).isEmpty()) {
                        trace.put("warehouse_name", warehouseName != null ? warehouseName : "-");
                    }
                }
            }

            // 6. 如果没有追溯记录，添加合成记录
            if (traces.isEmpty() && ("outbound".equals(status) || "stored".equals(status))) {
                System.out.println("看板状态为 " + status + "，添加合成记录");

                // 合成入库记录
                Map<String, Object> syntheticInbound = new HashMap<>();
                syntheticInbound.put("id", 0);
                syntheticInbound.put("trace_no", "TRACE-IN-" + System.currentTimeMillis());
                syntheticInbound.put("kanban_no", kanbanNo);
                syntheticInbound.put("order_no", orderNo);
                syntheticInbound.put("part_code", partCode);
                syntheticInbound.put("part_name", kanban.get("part_name"));
                syntheticInbound.put("quantity", kanban.get("quantity"));
                syntheticInbound.put("action_type", "INBOUND");
                syntheticInbound.put("action_time", kanban.get("created_at") != null ? kanban.get("created_at") : LocalDateTime.now());
                syntheticInbound.put("operator", "system");

                if (isOutbound) {
                    syntheticInbound.put("customer_name", customerName != null ? customerName : "-");
                    syntheticInbound.put("supplier_name", customerName != null ? customerName : "-");
                    syntheticInbound.put("warehouse_name", warehouseName != null ? warehouseName : "-");
                } else {
                    syntheticInbound.put("supplier_name", supplierName != null ? supplierName : "-");
                    syntheticInbound.put("warehouse_code", kanban.get("warehouse_code") != null ? kanban.get("warehouse_code") : "-");
                }
                traces.add(syntheticInbound);

                // 如果状态是 outbound，添加合成出库记录
                if ("outbound".equals(status)) {
                    Map<String, Object> syntheticOutbound = new HashMap<>();
                    syntheticOutbound.put("id", 1);
                    syntheticOutbound.put("trace_no", "TRACE-OUT-" + System.currentTimeMillis());
                    syntheticOutbound.put("kanban_no", kanbanNo);
                    syntheticOutbound.put("order_no", orderNo);
                    syntheticOutbound.put("part_code", partCode);
                    syntheticOutbound.put("part_name", kanban.get("part_name"));
                    syntheticOutbound.put("quantity", kanban.get("quantity"));
                    syntheticOutbound.put("action_type", "OUTBOUND");
                    syntheticOutbound.put("action_time", kanban.get("updated_at") != null ? kanban.get("updated_at") : LocalDateTime.now());
                    syntheticOutbound.put("operator", "system");

                    if (isOutbound) {
                        syntheticOutbound.put("customer_name", customerName != null ? customerName : "-");
                        syntheticOutbound.put("supplier_name", customerName != null ? customerName : "-");
                        syntheticOutbound.put("warehouse_name", warehouseName != null ? warehouseName : "-");
                    } else {
                        syntheticOutbound.put("supplier_name", supplierName != null ? supplierName : "-");
                        syntheticOutbound.put("warehouse_code", kanban.get("warehouse_code") != null ? kanban.get("warehouse_code") : "-");
                    }
                    traces.add(syntheticOutbound);
                }
            }

            // 7. 如果状态是 outbound 但只有一条入库记录
            if ("outbound".equals(status) && traces.size() == 1) {
                Map<String, Object> firstTrace = traces.get(0);
                if ("INBOUND".equals(firstTrace.get("action_type"))) {
                    Map<String, Object> syntheticOutbound = new HashMap<>();
                    syntheticOutbound.put("id", 2);
                    syntheticOutbound.put("trace_no", "TRACE-OUT-" + System.currentTimeMillis());
                    syntheticOutbound.put("kanban_no", kanbanNo);
                    syntheticOutbound.put("order_no", orderNo);
                    syntheticOutbound.put("part_code", partCode);
                    syntheticOutbound.put("part_name", kanban.get("part_name"));
                    syntheticOutbound.put("quantity", kanban.get("quantity"));
                    syntheticOutbound.put("action_type", "OUTBOUND");
                    syntheticOutbound.put("action_time", kanban.get("updated_at") != null ? kanban.get("updated_at") : LocalDateTime.now());
                    syntheticOutbound.put("operator", "system");

                    if (isOutbound) {
                        syntheticOutbound.put("customer_name", customerName != null ? customerName : "-");
                        syntheticOutbound.put("supplier_name", customerName != null ? customerName : "-");
                        syntheticOutbound.put("warehouse_name", warehouseName != null ? warehouseName : "-");
                    } else {
                        syntheticOutbound.put("supplier_name", supplierName != null ? supplierName : "-");
                        syntheticOutbound.put("warehouse_code", kanban.get("warehouse_code") != null ? kanban.get("warehouse_code") : "-");
                    }
                    traces.add(syntheticOutbound);
                }
            }

            System.out.println("最终追溯记录数量: " + traces.size());
            System.out.println("========== 获取看板生命周期结束 ==========");

            Map<String, Object> result = new HashMap<>();
            result.put("kanban", kanban);
            result.put("currentInventory", currentInventory);
            result.put("traces", traces);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取看板生命周期失败: " + e.getMessage());
        }
    }

    @GetMapping("/trace/by-kanban/{kanbanNo}")
    public Result<List<WmsInventoryTrace>> getTraceByKanban(@PathVariable String kanbanNo) {
        try {
            String sql = "SELECT t.*, s.supplier_name FROM inventory_trace t " +
                    "LEFT JOIN supplier s ON t.supplier_code = s.supplier_code " +
                    "WHERE t.kanban_no = ? ORDER BY t.action_time ASC";
            List<WmsInventoryTrace> traces = jdbcTemplate.query(sql, new Object[]{kanbanNo}, (rs, rowNum) -> {
                WmsInventoryTrace trace = new WmsInventoryTrace();
                trace.setId(rs.getInt("id"));
                trace.setTraceNo(rs.getString("trace_no"));
                trace.setKanbanNo(rs.getString("kanban_no"));
                trace.setOrderNo(rs.getString("order_no"));
                trace.setPartCode(rs.getString("part_code"));
                trace.setPartName(rs.getString("part_name"));
                trace.setSupplierCode(rs.getString("supplier_code"));
                trace.setSupplierName(rs.getString("supplier_name"));
                trace.setQuantity(rs.getInt("quantity"));
                trace.setWarehouseCode(rs.getString("warehouse_code"));
                trace.setWarehouseName(rs.getString("warehouse_name"));
                trace.setActionType(rs.getString("action_type"));
                trace.setActionTime(rs.getTimestamp("action_time") != null ? rs.getTimestamp("action_time").toLocalDateTime() : null);
                trace.setOperator(rs.getString("operator"));
                return trace;
            });
            return Result.success(traces);
        } catch (Exception e) {
            return Result.error("获取看板追溯失败: " + e.getMessage());
        }
    }

    // ============================================
    // 转包功能
    // ============================================

    /**
     * 向下装包（拆分）
     * 将一个看板拆分成多个看板
     */
    @Transactional
    @PostMapping("/transfer/split")
    public Result<Map<String, Object>> splitKanban(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            String fromKanbanNo = (String) request.get("fromKanbanNo");
            @SuppressWarnings("unchecked")
            List<Integer> newQuantities = (List<Integer>) request.get("newQuantities");
            String username = (String) session.getAttribute("user");
            if (username == null) username = "system";

            if (fromKanbanNo == null || fromKanbanNo.isEmpty()) {
                return Result.error("源看板号不能为空");
            }
            if (newQuantities == null || newQuantities.isEmpty()) {
                return Result.error("请指定拆分后的数量列表");
            }

            // 1. 查询源看板
            Map<String, Object> sourceKanban = jdbcTemplate.queryForMap(
                    "SELECT * FROM kanban WHERE kanban_no = ? AND status IN ('scanned', 'stored')", fromKanbanNo);

            int originalQty = ((Number) sourceKanban.get("quantity")).intValue();
            int totalNewQty = newQuantities.stream().mapToInt(Integer::intValue).sum();

            if (totalNewQty != originalQty) {
                return Result.error("拆分后的总数量(" + totalNewQty + ")必须等于原数量(" + originalQty + ")");
            }

            // 2. 生成转包单号
            String orderNo = "TO-" + System.currentTimeMillis();

            // 3. 更新源看板状态为已转包，并删除库存记录
            jdbcTemplate.update("UPDATE kanban SET status = 'transferred' WHERE kanban_no = ?", fromKanbanNo);
            jdbcTemplate.update("DELETE FROM current_inventory WHERE kanban_no = ?", fromKanbanNo);

            // 4. 创建新看板
            List<String> newKanbanNos = new ArrayList<>();
            for (int i = 0; i < newQuantities.size(); i++) {
                String newKanbanNo = fromKanbanNo + "-" + (i + 1);
                jdbcTemplate.update(
                        "INSERT INTO kanban (kanban_no, order_no, part_code, part_name, quantity, supplier_code, warehouse_code, status, transfer_type, parent_kanban_no) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, 'stored', 'split', ?)",
                        newKanbanNo,
                        sourceKanban.get("order_no"),
                        sourceKanban.get("part_code"),
                        sourceKanban.get("part_name"),
                        newQuantities.get(i),
                        sourceKanban.get("supplier_code"),
                        sourceKanban.get("warehouse_code"),
                        fromKanbanNo);

                // 创建库存记录
                jdbcTemplate.update(
                        "INSERT INTO current_inventory (kanban_no, part_code, part_name, supplier_code, quantity, warehouse_code) VALUES (?, ?, ?, ?, ?, ?)",
                        newKanbanNo,
                        sourceKanban.get("part_code"),
                        sourceKanban.get("part_name"),
                        sourceKanban.get("supplier_code"),
                        newQuantities.get(i),
                        sourceKanban.get("warehouse_code"));

                newKanbanNos.add(newKanbanNo);
            }

            // 5. 创建转包单记录
            String toKanbanNosJson = String.join(",", newKanbanNos);
            String newQuantitiesJson = newQuantities.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
            jdbcTemplate.update(
                    "INSERT INTO transfer_order (order_no, from_kanban_no, to_kanban_nos, transfer_type, original_quantity, new_quantities, status, operator) " +
                    "VALUES (?, ?, ?, 'split', ?, ?, 'completed', ?)",
                    orderNo, fromKanbanNo, toKanbanNosJson, originalQty, newQuantitiesJson, username);

            // 6. 添加追溯记录
            for (int i = 0; i < newKanbanNos.size(); i++) {
                String newKanbanNo = newKanbanNos.get(i);
                String traceNo = "TRACE-TRANSFER-" + System.currentTimeMillis() + "-" + newKanbanNo;
                jdbcTemplate.update(
                        "INSERT INTO inventory_trace (trace_no, kanban_no, part_code, part_name, quantity, warehouse_code, action_type, operator, remark) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'TRANSFER_SPLIT', ?, ?)",
                        traceNo, newKanbanNo, sourceKanban.get("part_code"), sourceKanban.get("part_name"),
                        newQuantities.get(i), sourceKanban.get("warehouse_code"), username,
                        "从看板" + fromKanbanNo + "拆分，数量" + newQuantities.get(i));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("orderNo", orderNo);
            result.put("newKanbanNos", newKanbanNos);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("转包拆分失败: " + e.getMessage());
        }
    }

    /**
     * 向上装包（合并）
     * 将多个看板合并成一个看板
     */
    @Transactional
    @PostMapping("/transfer/merge")
    public Result<Map<String, Object>> mergeKanban(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            @SuppressWarnings("unchecked")
            List<String> fromKanbanNos = (List<String>) request.get("fromKanbanNos");
            String username = (String) session.getAttribute("user");
            if (username == null) username = "system";

            if (fromKanbanNos == null || fromKanbanNos.size() < 2) {
                return Result.error("请至少选择2个看板进行合并");
            }

            // 1. 查询所有源看板
            List<Map<String, Object>> sourceKanbans = new ArrayList<>();
            int totalQty = 0;
            String partCode = null;
            String warehouseCode = null;

            for (String kanbanNo : fromKanbanNos) {
                Map<String, Object> kanban = jdbcTemplate.queryForMap(
                        "SELECT * FROM kanban WHERE kanban_no = ? AND status IN ('scanned', 'stored')", kanbanNo);
                sourceKanbans.add(kanban);
                totalQty += ((Number) kanban.get("quantity")).intValue();

                if (partCode == null) {
                    partCode = (String) kanban.get("part_code");
                    warehouseCode = (String) kanban.get("warehouse_code");
                } else if (!partCode.equals(kanban.get("part_code"))) {
                    return Result.error("只能合并相同零件的看板");
                }
            }

            // 2. 生成新看板号
            String newKanbanNo = "KAN-" + System.currentTimeMillis();

            // 3. 创建新看板
            jdbcTemplate.update(
                    "INSERT INTO kanban (kanban_no, order_no, part_code, part_name, quantity, supplier_code, warehouse_code, status, transfer_type) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'stored', 'merge')",
                    newKanbanNo, sourceKanbans.get(0).get("order_no"), partCode, sourceKanbans.get(0).get("part_name"), totalQty,
                    sourceKanbans.get(0).get("supplier_code"), warehouseCode);

            // 4. 创建库存记录
            jdbcTemplate.update(
                    "INSERT INTO current_inventory (kanban_no, part_code, part_name, supplier_code, quantity, warehouse_code) VALUES (?, ?, ?, ?, ?, ?)",
                    newKanbanNo, partCode, sourceKanbans.get(0).get("part_name"), sourceKanbans.get(0).get("supplier_code"), totalQty, warehouseCode);

            // 5. 更新源看板状态
            for (String kanbanNo : fromKanbanNos) {
                jdbcTemplate.update("UPDATE kanban SET status = 'merged' WHERE kanban_no = ?", kanbanNo);
                jdbcTemplate.update("DELETE FROM current_inventory WHERE kanban_no = ?", kanbanNo);
            }

            // 6. 创建转包单记录
            String fromKanbanNosJson = String.join(",", fromKanbanNos);
            String orderNo = "TO-" + System.currentTimeMillis();
            jdbcTemplate.update(
                    "INSERT INTO transfer_order (order_no, from_kanban_no, to_kanban_nos, transfer_type, original_quantity, new_quantities, status, operator) " +
                    "VALUES (?, ?, ?, 'merge', ?, ?, 'completed', ?)",
                    orderNo, fromKanbanNosJson, newKanbanNo, totalQty, totalQty, username);

            // 7. 添加追溯记录
            String traceNo = "TRACE-TRANSFER-" + System.currentTimeMillis() + "-" + newKanbanNo;
            jdbcTemplate.update(
                    "INSERT INTO inventory_trace (trace_no, kanban_no, part_code, part_name, quantity, warehouse_code, action_type, operator, remark) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'TRANSFER_MERGE', ?, ?)",
                    traceNo, newKanbanNo, partCode, sourceKanbans.get(0).get("part_name"),
                    totalQty, warehouseCode, username,
                    "合并看板: " + fromKanbanNosJson);

            Map<String, Object> result = new HashMap<>();
            result.put("orderNo", orderNo);
            result.put("newKanbanNo", newKanbanNo);
            result.put("quantity", totalQty);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("转包合并失败: " + e.getMessage());
        }
    }

    /**
     * 获取转包单列表
     */
    @GetMapping("/transfer/list")
    public Result<List<Map<String, Object>>> getTransferOrderList(
            @RequestParam(required = false) String transferType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            String sql = "SELECT * FROM transfer_order WHERE 1=1";
            List<Object> params = new ArrayList<>();

            if (transferType != null && !transferType.isEmpty()) {
                sql += " AND transfer_type = ?";
                params.add(transferType);
            }
            if (status != null && !status.isEmpty()) {
                sql += " AND status = ?";
                params.add(status);
            }

            sql += " ORDER BY created_at DESC LIMIT ? OFFSET ?";
            params.add(size);
            params.add((page - 1) * size);

            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, params.toArray());
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("获取转包单列表失败: " + e.getMessage());
        }
    }

    // ============================================
    // 封存/解封功能
    // ============================================

    /**
     * 封存看板
     */
    @PostMapping("/kanban/seal")
    public Result<String> sealKanban(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            String kanbanNo = (String) request.get("kanbanNo");
            String remark = (String) request.get("remark");
            String username = (String) session.getAttribute("user");
            if (username == null) username = "system";

            if (kanbanNo == null || kanbanNo.isEmpty()) {
                return Result.error("看板号不能为空");
            }

            // 更新看板封存状态
            int updated = jdbcTemplate.update(
                    "UPDATE kanban SET is_sealed = 1 WHERE kanban_no = ? AND status IN ('scanned', 'stored')",
                    kanbanNo);

            if (updated == 0) {
                return Result.error("看板不存在或状态不允许封存");
            }

            // 查询看板信息用于追溯记录
            Map<String, Object> kanban = jdbcTemplate.queryForMap(
                    "SELECT part_code, part_name, supplier_code, warehouse_code, quantity FROM kanban WHERE kanban_no = ?",
                    kanbanNo);

            // 记录封存历史
            jdbcTemplate.update(
                    "INSERT INTO seal_history (kanban_no, action, operator, remark) VALUES (?, 'SEAL', ?, ?)",
                    kanbanNo, username, remark);

            // 添加追溯记录
            String traceNo = "TRACE-SEAL-" + System.currentTimeMillis() + "-" + kanbanNo;
            jdbcTemplate.update(
                    "INSERT INTO inventory_trace (trace_no, kanban_no, part_code, part_name, supplier_code, quantity, warehouse_code, action_type, operator, remark) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'SEAL', ?, ?)",
                    traceNo, kanbanNo,
                    kanban.get("part_code"), kanban.get("part_name"),
                    kanban.get("supplier_code"), kanban.get("quantity"),
                    kanban.get("warehouse_code"),
                    username, "看板封存: " + (remark != null ? remark : "无备注"));

            return Result.success("看板 " + kanbanNo + " 已封存");
        } catch (Exception e) {
            return Result.error("封存失败: " + e.getMessage());
        }
    }

    /**
     * 解封看板
     */
    @PostMapping("/kanban/unseal")
    public Result<String> unsealKanban(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            String kanbanNo = (String) request.get("kanbanNo");
            String remark = (String) request.get("remark");
            String username = (String) session.getAttribute("user");
            if (username == null) username = "system";

            if (kanbanNo == null || kanbanNo.isEmpty()) {
                return Result.error("看板号不能为空");
            }

            // 更新看板封存状态
            int updated = jdbcTemplate.update(
                    "UPDATE kanban SET is_sealed = 0 WHERE kanban_no = ? AND is_sealed = 1",
                    kanbanNo);

            if (updated == 0) {
                return Result.error("看板未封存或不存在");
            }

            // 查询看板信息用于追溯记录
            Map<String, Object> kanban = jdbcTemplate.queryForMap(
                    "SELECT part_code, part_name, supplier_code, warehouse_code, quantity FROM kanban WHERE kanban_no = ?",
                    kanbanNo);

            // 记录解封历史
            jdbcTemplate.update(
                    "INSERT INTO seal_history (kanban_no, action, operator, remark) VALUES (?, 'UNSEAL', ?, ?)",
                    kanbanNo, username, remark);

            // 添加追溯记录
            String traceNo = "TRACE-UNSEAL-" + System.currentTimeMillis() + "-" + kanbanNo;
            jdbcTemplate.update(
                    "INSERT INTO inventory_trace (trace_no, kanban_no, part_code, part_name, supplier_code, quantity, warehouse_code, action_type, operator, remark) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'UNSEAL', ?, ?)",
                    traceNo, kanbanNo,
                    kanban.get("part_code"), kanban.get("part_name"),
                    kanban.get("supplier_code"), kanban.get("quantity"),
                    kanban.get("warehouse_code"),
                    username, "看板解封: " + (remark != null ? remark : "无备注"));

            return Result.success("看板 " + kanbanNo + " 已解封");
        } catch (Exception e) {
            return Result.error("解封失败: " + e.getMessage());
        }
    }

    /**
     * 获取封存历史记录
     */
    @GetMapping("/kanban/seal-history")
    public Result<List<Map<String, Object>>> getSealHistory(
            @RequestParam(required = false) String kanbanNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            String sql = "SELECT * FROM seal_history WHERE 1=1";
            List<Object> params = new ArrayList<>();

            if (kanbanNo != null && !kanbanNo.isEmpty()) {
                sql += " AND kanban_no = ?";
                params.add(kanbanNo);
            }

            sql += " ORDER BY created_at DESC LIMIT ? OFFSET ?";
            params.add(size);
            params.add((page - 1) * size);

            List<Map<String, Object>> history = jdbcTemplate.queryForList(sql, params.toArray());
            return Result.success(history);
        } catch (Exception e) {
            return Result.error("获取封存历史失败: " + e.getMessage());
        }
    }

}
