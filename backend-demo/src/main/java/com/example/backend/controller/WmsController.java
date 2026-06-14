package com.example.backend.controller;

import com.example.backend.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
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
     * 获取库位列表
     */
    @GetMapping("/location/list")
    public Result<List<Map<String, Object>>> getLocationList(@RequestParam(required = false) String warehouseCode) {
        try {
            String sql = "SELECT l.*, w.warehouse_name FROM storage_location l LEFT JOIN warehouse w ON l.warehouse_code = w.warehouse_code WHERE l.status = 1";
            if (warehouseCode != null && !warehouseCode.isEmpty()) {
                sql += " AND l.warehouse_code = '" + warehouseCode + "'";
            }
            List<Map<String, Object>> locations = jdbcTemplate.queryForList(sql);
            return Result.success(locations);
        } catch (Exception e) {
            return Result.error("获取库位列表失败: " + e.getMessage());
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

            // 获取看板
            String kanbanSql = "SELECT * FROM kanban WHERE order_no = ?";
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

            List<Map<String, String>> kanbans = new ArrayList<>();

            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> item = items.get(i);
                System.out.println("第" + (i+1) + "个item: " + item);

                // 尝试多种可能的字段名
                String partCode = null;
                String partName = null;
                String supplierCode = null;
                Integer quantity = null;
                Integer boxCount = 1;

                // 获取 partCode
                if (item.get("partCode") != null) {
                    partCode = (String) item.get("partCode");
                } else if (item.get("part_code") != null) {
                    partCode = (String) item.get("part_code");
                }

                // 获取 partName
                if (item.get("partName") != null) {
                    partName = (String) item.get("partName");
                } else if (item.get("part_name") != null) {
                    partName = (String) item.get("part_name");
                }

                // 获取 supplierCode
                if (item.get("supplierCode") != null) {
                    supplierCode = (String) item.get("supplierCode");
                } else if (item.get("supplier_code") != null) {
                    supplierCode = (String) item.get("supplier_code");
                }

                // 获取 quantity
                if (item.get("expectedQuantity") != null) {
                    quantity = ((Number) item.get("expectedQuantity")).intValue();
                } else if (item.get("quantity") != null) {
                    quantity = ((Number) item.get("quantity")).intValue();
                } else if (item.get("expected_quantity") != null) {
                    quantity = ((Number) item.get("expected_quantity")).intValue();
                }

                // 获取 boxCount
                if (item.get("expectedBoxes") != null) {
                    boxCount = ((Number) item.get("expectedBoxes")).intValue();
                } else if (item.get("boxCount") != null) {
                    boxCount = ((Number) item.get("boxCount")).intValue();
                } else if (item.get("expected_boxes") != null) {
                    boxCount = ((Number) item.get("expected_boxes")).intValue();
                }

                System.out.println("解析后 - partCode: " + partCode + ", partName: " + partName +
                        ", quantity: " + quantity + ", boxCount: " + boxCount);

                // 验证必填字段
                if (partCode == null || partCode.isEmpty()) {
                    return Result.error("零件号不能为空，第" + (i+1) + "个item: " + item);
                }
                if (quantity == null || quantity <= 0) {
                    return Result.error("数量不能为空或为0，第" + (i+1) + "个item");
                }

                // 生成看板号
                String kanbanNo = "KAN-" + System.currentTimeMillis() + "-" + partCode;

                // 生成二维码内容
                String qrContent = kanbanNo;

                // 保存看板
                String sql = "INSERT INTO kanban (kanban_no, order_no, part_code, part_name, supplier_code, " +
                        "quantity, box_count, qr_code, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                jdbcTemplate.update(sql, kanbanNo, orderNo, partCode, partName, supplierCode,
                        quantity, boxCount, qrContent, "pending");

                System.out.println("看板生成成功: " + kanbanNo);

                Map<String, String> kanban = new HashMap<>();
                kanban.put("kanbanNo", kanbanNo);
                kanban.put("partCode", partCode);
                kanban.put("partName", partName);
                kanban.put("quantity", String.valueOf(quantity));
                kanban.put("supplierCode", supplierCode == null ? "" : supplierCode);
                kanban.put("boxCount", String.valueOf(boxCount));
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
     * 获取看板列表
     */
    @GetMapping("/kanban/list")
    public Result<List<WmsKanban>> getKanbanList(@RequestParam(required = false) String orderNo,
                                                 @RequestParam(required = false) String status) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT k.*, s.supplier_name, l.location_name FROM kanban k " +
                            "LEFT JOIN supplier s ON k.supplier_code = s.supplier_code " +
                            "LEFT JOIN storage_location l ON k.location_code = l.location_code WHERE 1=1"
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
            sql.append(" ORDER BY k.created_at DESC");

            List<WmsKanban> kanbans = jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
                WmsKanban kanban = new WmsKanban();
                kanban.setId(rs.getInt("id"));
                kanban.setKanbanNo(rs.getString("kanban_no"));
                kanban.setOrderNo(rs.getString("order_no"));
                kanban.setPartCode(rs.getString("part_code"));
                kanban.setPartName(rs.getString("part_name"));
                kanban.setSupplierCode(rs.getString("supplier_code"));
                kanban.setSupplierName(rs.getString("supplier_name"));
                kanban.setQuantity(rs.getInt("quantity"));
                kanban.setBoxCount(rs.getInt("box_count"));
                kanban.setQrCode(rs.getString("qr_code"));
                kanban.setStatus(rs.getString("status"));
                kanban.setLocationCode(rs.getString("location_code"));
                kanban.setLocationName(rs.getString("location_name"));
                kanban.setScanTime(rs.getTimestamp("scan_time") != null ? rs.getTimestamp("scan_time").toLocalDateTime() : null);
                kanban.setScanBy(rs.getString("scan_by"));
                return kanban;
            });
            return Result.success(kanbans);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取看板列表失败: " + e.getMessage());
        }
    }

    // ==================== 扫码入库 ====================

    /**
     * 扫码入库
     */
    @PostMapping("/scan/inbound")
    public Result<Map<String, Object>> scanInbound(@RequestBody Map<String, String> request, HttpSession session) {
        try {
            String kanbanNo = request.get("kanbanNo");
            String locationCode = request.get("locationCode");
            String username = (String) session.getAttribute("user");
            if (username == null) username = "system";

            // 查询看板
            Map<String, Object> kanban = jdbcTemplate.queryForMap(
                    "SELECT * FROM kanban WHERE kanban_no = ? AND status = 'pending'", kanbanNo);

            // 更新看板状态
            jdbcTemplate.update("UPDATE kanban SET status = 'scanned', location_code = ?, scan_time = ?, scan_by = ? WHERE kanban_no = ?",
                    locationCode, LocalDateTime.now(), username, kanbanNo);

            String orderNo = (String) kanban.get("order_no");
            Integer quantity = (Integer) kanban.get("quantity");
            Integer boxCount = (Integer) kanban.get("box_count");
            String partCode = (String) kanban.get("part_code");

            // 更新入库单入库数量
            jdbcTemplate.update("UPDATE inbound_order SET received_quantity = received_quantity + ?, " +
                    "received_boxes = received_boxes + ? WHERE order_no = ?", quantity, boxCount, orderNo);

            // 更新明细
            jdbcTemplate.update("UPDATE inbound_order_detail SET received_quantity = received_quantity + ?, " +
                            "received_boxes = received_boxes + ? WHERE order_no = ? AND part_code = ?",
                    quantity, boxCount, orderNo, partCode);

            // 添加到当前库存
            jdbcTemplate.update("INSERT INTO current_inventory (kanban_no, part_code, part_name, supplier_code, quantity, location_code) " +
                            "VALUES (?, ?, ?, ?, ?, ?)", kanbanNo, partCode, kanban.get("part_name"),
                    kanban.get("supplier_code"), quantity, locationCode);

            // 添加追溯记录
            String traceNo = "TRACE-" + System.currentTimeMillis();
            jdbcTemplate.update("INSERT INTO inventory_trace (trace_no, kanban_no, order_no, part_code, part_name, " +
                            "supplier_code, quantity, location_code, action_type, operator) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'INBOUND', ?)",
                    traceNo, kanbanNo, orderNo, partCode, kanban.get("part_name"),
                    kanban.get("supplier_code"), quantity, locationCode, username);

            // 检查入库单是否完成
            Integer totalQuantity = jdbcTemplate.queryForObject(
                    "SELECT total_quantity FROM inbound_order WHERE order_no = ?", Integer.class, orderNo);
            Integer receivedQuantity = jdbcTemplate.queryForObject(
                    "SELECT received_quantity FROM inbound_order WHERE order_no = ?", Integer.class, orderNo);

            if (totalQuantity.equals(receivedQuantity)) {
                jdbcTemplate.update("UPDATE inbound_order SET status = 'completed' WHERE order_no = ?", orderNo);
                jdbcTemplate.update("UPDATE kanban SET status = 'stored' WHERE order_no = ? AND status = 'scanned'", orderNo);
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
                    "SELECT k.*, s.supplier_name FROM kanban k " +
                            "LEFT JOIN supplier s ON k.supplier_code = s.supplier_code " +
                            "WHERE k.kanban_no = ?", kanbanNo);

            String status = (String) kanban.get("status");
            if (!"pending".equals(status)) {
                return Result.error("看板已使用或无效");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("valid", true);
            result.put("kanbanNo", kanbanNo);
            result.put("partCode", kanban.get("part_code"));
            result.put("partName", kanban.get("part_name"));
            result.put("supplierName", kanban.get("supplier_name"));
            result.put("quantity", kanban.get("quantity"));
            result.put("orderNo", kanban.get("order_no"));
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("看板无效或不存在");
        }
    }

    // ==================== 库存追溯 ====================

    /**
     * 获取库存追溯记录
     */
    @GetMapping("/trace/list")
    public Result<Map<String, Object>> getTraceList(
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) String kanbanNo,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT t.*, s.supplier_name FROM inventory_trace t " +
                            "LEFT JOIN supplier s ON t.supplier_code = s.supplier_code WHERE 1=1"
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
            if (startDate != null && !startDate.isEmpty()) {
                sql.append(" AND DATE(t.action_time) >= ?");
                params.add(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                sql.append(" AND DATE(t.action_time) <= ?");
                params.add(endDate);
            }

            // 查询总数
            String countSql = sql.toString().replace("SELECT t.*, s.supplier_name", "SELECT COUNT(*)");
            Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

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
                trace.setLocationCode(rs.getString("location_code"));
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
     * 生成出库单号
     */
    private String generateOutboundOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sql = "SELECT COUNT(*) FROM outbound_order WHERE order_no LIKE CONCAT('SO-', ?, '%')";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, date);
        return "SO-" + date + "-" + String.format("%04d", count + 1);
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

            // 插入主表
            String sql = "INSERT INTO outbound_order (order_no, outbound_type, customer_code, customer_name, warehouse_code, " +
                    "total_quantity, total_boxes, remark, created_by, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')";
            jdbcTemplate.update(sql, orderNo, order.getOutboundType(), order.getCustomerCode(),
                    order.getCustomerName(), order.getWarehouseCode(), order.getTotalQuantity(), order.getTotalBoxes(),
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
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建出库单失败: " + e.getMessage());
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

            Map<String, Object> result = new HashMap<>();
            result.put("order", order);
            result.put("details", details);
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
    @PostMapping("/scan/outbound")
    public Result<Map<String, Object>> scanOutbound(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            String orderNo = (String) request.get("orderNo");
            String partCode = (String) request.get("partCode");
            Integer quantity = ((Number) request.get("quantity")).intValue();
            String username = (String) session.getAttribute("user");
            if (username == null) username = "system";

            // 1. 验证出库单状态
            String orderStatus = jdbcTemplate.queryForObject(
                    "SELECT status FROM outbound_order WHERE order_no = ?", String.class, orderNo);
            if ("completed".equals(orderStatus)) {
                return Result.error("出库单已完成");
            }

            // 2. 查询当前库存,按FIFO原则(按入库时间排序)
            String inventorySql = "SELECT * FROM current_inventory WHERE part_code = ? AND quantity > 0 ORDER BY created_at ASC";
            List<Map<String, Object>> inventoryList = jdbcTemplate.queryForList(inventorySql, partCode);

            if (inventoryList.isEmpty()) {
                return Result.error("零件 " + partCode + " 库存不足");
            }

            // 3. 计算可用库存总量
            int totalAvailable = inventoryList.stream()
                    .mapToInt(inv -> ((Number) inv.get("quantity")).intValue())
                    .sum();

            if (totalAvailable < quantity) {
                return Result.error("库存不足,当前可用: " + totalAvailable + ", 需要: " + quantity);
            }

            // 4. 按FIFO原则扣减库存
            int remainingQuantity = quantity;
            int totalBoxes = 0;

            for (Map<String, Object> inventory : inventoryList) {
                if (remainingQuantity <= 0) break;

                int currentQty = ((Number) inventory.get("quantity")).intValue();
                String kanbanNo = (String) inventory.get("kanban_no");
                String locationCode = (String) inventory.get("location_code");
                int inventoryQty = Math.min(remainingQuantity, currentQty);
                int boxes = (int) Math.ceil((double) inventoryQty / currentQty);

                // 更新当前库存
                if (inventoryQty == currentQty) {
                    // 全部出库,删除记录
                    jdbcTemplate.update("DELETE FROM current_inventory WHERE kanban_no = ?", kanbanNo);
                    // 更新看板状态为outbound
                    jdbcTemplate.update("UPDATE kanban SET status = 'outbound' WHERE kanban_no = ?", kanbanNo);
                } else {
                    // 部分出库
                    jdbcTemplate.update("UPDATE current_inventory SET quantity = quantity - ? WHERE kanban_no = ?",
                            inventoryQty, kanbanNo);
                }

                remainingQuantity -= inventoryQty;
                totalBoxes += boxes;

                // 添加出库追溯记录
                String traceNo = "TRACE-OUT-" + System.currentTimeMillis() + "-" + kanbanNo;
                jdbcTemplate.update("INSERT INTO inventory_trace (trace_no, kanban_no, order_no, part_code, part_name, " +
                                "supplier_code, quantity, location_code, action_type, operator) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OUTBOUND', ?)",
                        traceNo, kanbanNo, orderNo, partCode, inventory.get("part_name"),
                        inventory.get("supplier_code"), inventoryQty, locationCode, username);
            }

            // 5. 更新出库单已出库数量
            jdbcTemplate.update("UPDATE outbound_order SET shipped_quantity = shipped_quantity + ?, " +
                    "shipped_boxes = shipped_boxes + ? WHERE order_no = ?", quantity, totalBoxes, orderNo);

            // 更新明细
            jdbcTemplate.update("UPDATE outbound_order_detail SET shipped_quantity = shipped_quantity + ?, " +
                            "shipped_boxes = shipped_boxes + ? WHERE order_no = ? AND part_code = ?",
                    quantity, totalBoxes, orderNo, partCode);

            // 6. 检查出库单是否完成
            Integer totalQuantity = jdbcTemplate.queryForObject(
                    "SELECT total_quantity FROM outbound_order WHERE order_no = ?", Integer.class, orderNo);
            Integer shippedQuantity = jdbcTemplate.queryForObject(
                    "SELECT shipped_quantity FROM outbound_order WHERE order_no = ?", Integer.class, orderNo);

            if (totalQuantity.equals(shippedQuantity)) {
                jdbcTemplate.update("UPDATE outbound_order SET status = 'completed' WHERE order_no = ?", orderNo);
            } else {
                jdbcTemplate.update("UPDATE outbound_order SET status = 'partial' WHERE order_no = ?", orderNo);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "出库成功");
            result.put("orderNo", orderNo);
            result.put("partCode", partCode);
            result.put("quantity", quantity);
            result.put("boxes", totalBoxes);
            return Result.success(result);
        } catch (Exception e) {
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

            Map<String, Object> result = new HashMap<>();
            result.put("valid", true);
            result.put("orderNo", orderNo);
            result.put("outboundType", order.get("outbound_type"));
            result.put("customerName", order.get("customer_name"));
            result.put("warehouseName", order.get("warehouse_name"));
            result.put("totalQuantity", order.get("total_quantity"));
            result.put("shippedQuantity", order.get("shipped_quantity"));
            result.put("details", details);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("出库单无效或不存在");
        }
    }

    /**
     * 获取FIFO库存列表
     */
    @GetMapping("/inventory/fifo-list")
    public Result<List<Map<String, Object>>> getFifoInventoryList(@RequestParam String partCode) {
        try {
            String sql = "SELECT i.*, s.supplier_name FROM current_inventory i " +
                    "LEFT JOIN supplier s ON i.supplier_code = s.supplier_code " +
                    "WHERE i.part_code = ? AND i.quantity > 0 " +
                    "ORDER BY i.created_at ASC";
            List<Map<String, Object>> inventory = jdbcTemplate.queryForList(sql, partCode);
            return Result.success(inventory);
        } catch (Exception e) {
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
                    "SELECT i.*, s.supplier_name, l.location_name FROM current_inventory i " +
                            "LEFT JOIN supplier s ON i.supplier_code = s.supplier_code " +
                            "LEFT JOIN storage_location l ON i.location_code = l.location_code WHERE 1=1"
            );
            List<Object> params = new ArrayList<>();

            if (partCode != null && !partCode.isEmpty()) {
                sql.append(" AND i.part_code LIKE ?");
                params.add("%" + partCode + "%");
            }
            if (locationCode != null && !locationCode.isEmpty()) {
                sql.append(" AND i.location_code = ?");
                params.add(locationCode);
            }

            // 查询总数
            String countSql = sql.toString().replace("SELECT i.*, s.supplier_name, l.location_name", "SELECT COUNT(*)");
            Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

            // 分页查询
            sql.append(" ORDER BY i.created_at DESC LIMIT ? OFFSET ?");
            params.add(pageSize);
            params.add((page - 1) * pageSize);

            List<Map<String, Object>> inventory = jdbcTemplate.queryForList(sql.toString(), params.toArray());

            // 统计汇总
            String sumSql = "SELECT SUM(quantity) as total_quantity, COUNT(*) as total_count FROM current_inventory";
            Map<String, Object> summary = jdbcTemplate.queryForMap(sumSql);

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
                trace.setLocationCode(rs.getString("location_code"));
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

}