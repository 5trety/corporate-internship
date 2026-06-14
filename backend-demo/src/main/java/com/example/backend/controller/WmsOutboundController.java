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
@RequestMapping("/wms/outbound")
public class WmsOutboundController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ==================== 出库单管理 ====================

    /**
     * 创建出库单
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createOutboundOrder(@RequestBody Map<String, Object> requestData, HttpSession session) {
        try {
            // 打印接收到的原始数据
            System.out.println("===== 接收到的请求数据 =====");
            System.out.println("requestData类型: " + requestData.getClass().getName());
            System.out.println("requestData大小: " + requestData.size());
            System.out.println("所有key: " + requestData.keySet());
            
            // 检查每个字段的类型
            for (String key : requestData.keySet()) {
                Object value = requestData.get(key);
                System.out.println("字段 [" + key + "] 类型: " + (value != null ? value.getClass().getName() : "null"));
                System.out.println("字段 [" + key + "] 值: " + value);
            }
            
            String outboundType = (String) requestData.get("outboundType");
            String customerCode = (String) requestData.get("customerCode");
            String customerName = (String) requestData.get("customerName");
            String warehouseCode = (String) requestData.get("warehouseCode");
            String remark = (String) requestData.get("remark");
            
            // 处理details，支持多种数据类型
            Object detailsObj = requestData.get("details");
            List<Map<String, Object>> details = new ArrayList<>();
            
            System.out.println("\n===== 处理details =====");
            
            if (detailsObj == null) {
                System.err.println("错误: details为null");
                return Result.error("出库明细不能为空");
            }
            
            System.out.println("details类型: " + detailsObj.getClass().getName());
            System.out.println("details值: " + detailsObj);
            
            if (detailsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> detailList = (List<Object>) detailsObj;
                System.out.println("details数组长度: " + detailList.size());
                
                for (int i = 0; i < detailList.size(); i++) {
                    Object item = detailList.get(i);
                    System.out.println("\n第" + i + "个元素:");
                    System.out.println("  类型: " + (item != null ? item.getClass().getName() : "null"));
                    System.out.println("  值: " + item);
                    
                    if (item instanceof Map) {
                        details.add((Map<String, Object>) item);
                        System.out.println("  ✓ 成功转换为Map");
                    } else {
                        System.err.println("  ✗ 错误: 元素不是Map类型，实际类型: " + (item != null ? item.getClass().getName() : "null"));
                        return Result.error("出库明细格式错误，第" + (i+1) + "行数据格式不正确。期望收到对象(Map)类型，但实际收到: " + (item != null ? item.getClass().getName() : "null"));
                    }
                }
            } else if (detailsObj instanceof String) {
                System.err.println("错误: details是String类型，应该是数组");
                return Result.error("出库明细格式错误，应该是数组格式");
            } else {
                System.err.println("错误: details类型未知: " + detailsObj.getClass().getName());
                return Result.error("出库明细格式错误");
            }

            if (details.isEmpty()) {
                return Result.error("出库明细不能为空");
            }

            // 生成出库单号
            String orderNo = generateOrderNo();

            // 获取当前用户
            String username = getCurrentUser(session);

            // 计算总数量和总箱数
            int totalQuantity = 0;
            int totalBoxes = 0;
            for (Map<String, Object> detail : details) {
                // 使用 Number 类型安全转换
                Object quantityObj = detail.get("expectedQuantity");
                Object boxesObj = detail.get("expectedBoxes");
                
                int quantity = 0;
                int boxes = 0;
                
                if (quantityObj instanceof Number) {
                    quantity = ((Number) quantityObj).intValue();
                } else if (quantityObj instanceof String) {
                    try {
                        quantity = Integer.parseInt((String) quantityObj);
                    } catch (NumberFormatException e) {
                        quantity = 0;
                    }
                }
                
                if (boxesObj instanceof Number) {
                    boxes = ((Number) boxesObj).intValue();
                } else if (boxesObj instanceof String) {
                    try {
                        boxes = Integer.parseInt((String) boxesObj);
                    } catch (NumberFormatException e) {
                        boxes = 0;
                    }
                }
                
                totalQuantity += quantity;
                totalBoxes += boxes;
            }

            // 插入出库单
            String orderSql = "INSERT INTO outbound_order (order_no, outbound_type, customer_code, customer_name, " +
                    "warehouse_code, status, total_quantity, shipped_quantity, total_boxes, shipped_boxes, remark, created_by) " +
                    "VALUES (?, ?, ?, ?, ?, 'pending', ?, 0, ?, 0, ?, ?)";
            jdbcTemplate.update(orderSql, orderNo, outboundType, customerCode, customerName,
                    warehouseCode, totalQuantity, totalBoxes, remark, username);

            // 插入出库单明细
            String detailSql = "INSERT INTO outbound_order_detail (order_no, part_code, part_name, " +
                    "expected_quantity, shipped_quantity, packaging_capacity, expected_boxes, shipped_boxes, unit) " +
                    "VALUES (?, ?, ?, ?, 0, ?, ?, 0, ?)";
            for (Map<String, Object> detail : details) {
                // 安全转换数字类型
                Object expectedQtyObj = detail.get("expectedQuantity");
                Object packagingCapObj = detail.get("packagingCapacity");
                Object expectedBoxesObj = detail.get("expectedBoxes");
                
                int expectedQty = 0;
                int packagingCap = 0;
                int expectedBoxes = 0;
                
                if (expectedQtyObj instanceof Number) {
                    expectedQty = ((Number) expectedQtyObj).intValue();
                } else if (expectedQtyObj instanceof String) {
                    try {
                        expectedQty = Integer.parseInt((String) expectedQtyObj);
                    } catch (NumberFormatException e) {
                        expectedQty = 0;
                    }
                }
                
                if (packagingCapObj instanceof Number) {
                    packagingCap = ((Number) packagingCapObj).intValue();
                } else if (packagingCapObj instanceof String) {
                    try {
                        packagingCap = Integer.parseInt((String) packagingCapObj);
                    } catch (NumberFormatException e) {
                        packagingCap = 0;
                    }
                }
                
                if (expectedBoxesObj instanceof Number) {
                    expectedBoxes = ((Number) expectedBoxesObj).intValue();
                } else if (expectedBoxesObj instanceof String) {
                    try {
                        expectedBoxes = Integer.parseInt((String) expectedBoxesObj);
                    } catch (NumberFormatException e) {
                        expectedBoxes = 0;
                    }
                }
                
                jdbcTemplate.update(detailSql,
                        orderNo,
                        detail.get("partCode"),
                        detail.get("partName"),
                        expectedQty,
                        packagingCap,
                        expectedBoxes,
                        detail.get("unit") != null ? detail.get("unit") : "个"
                );
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
    @GetMapping("/list")
    public Result<Map<String, Object>> getOutboundOrderList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpSession session) {
        try {
            String sql = "SELECT o.*, w.warehouse_name FROM outbound_order o " +
                    "LEFT JOIN warehouse w ON o.warehouse_code = w.warehouse_code WHERE 1=1";
            List<Object> params = new ArrayList<>();

            if (status != null && !status.isEmpty()) {
                sql += " AND o.status = ?";
                params.add(status);
            }
            if (customerCode != null && !customerCode.isEmpty()) {
                sql += " AND o.customer_code = ?";
                params.add(customerCode);
            }
            if (startDate != null && !startDate.isEmpty()) {
                sql += " AND o.created_at >= ?";
                params.add(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                sql += " AND o.created_at <= ?";
                params.add(endDate + " 23:59:59");
            }

            sql += " ORDER BY o.created_at DESC";

            // 分页查询
            String pageSql = sql + " LIMIT ? OFFSET ?";
            List<Object> pageParams = new ArrayList<>(params);
            pageParams.add(pageSize);
            pageParams.add((page - 1) * pageSize);

            List<Map<String, Object>> orders = jdbcTemplate.queryForList(pageSql, pageParams.toArray());

            // 查询总数
            String countSql = "SELECT COUNT(*) FROM (" + sql + ") t";
            int total = jdbcTemplate.queryForObject(countSql, params.toArray(), Integer.class);

            Map<String, Object> result = new HashMap<>();
            result.put("list", orders);
            result.put("total", total);
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
    @GetMapping("/detail/{orderNo}")
    public Result<Map<String, Object>> getOutboundOrderDetail(@PathVariable String orderNo) {
        try {
            // 查询出库单基本信息
            String orderSql = "SELECT o.*, w.warehouse_name FROM outbound_order o " +
                    "LEFT JOIN warehouse w ON o.warehouse_code = w.warehouse_code " +
                    "WHERE o.order_no = ?";
            Map<String, Object> order = jdbcTemplate.queryForMap(orderSql, orderNo);

            // 查询出库单明细
            String detailSql = "SELECT * FROM outbound_order_detail WHERE order_no = ? ORDER BY id";
            List<Map<String, Object>> details = jdbcTemplate.queryForList(detailSql, orderNo);

            Map<String, Object> result = new HashMap<>();
            result.put("order", order);
            result.put("details", details);
            return Result.success(result);

        } catch (Exception e) {
            return Result.error("获取出库单详情失败: " + e.getMessage());
        }
    }

    /**
     * 删除出库单（仅待出库状态可删除）
     */
    @DeleteMapping("/delete/{orderNo}")
    public Result<Map<String, Object>> deleteOutboundOrder(@PathVariable String orderNo) {
        try {
            // 检查出库单状态
            String statusSql = "SELECT status FROM outbound_order WHERE order_no = ?";
            String status = jdbcTemplate.queryForObject(statusSql, String.class, orderNo);

            if (!"pending".equals(status)) {
                return Result.error("只有待出库状态的出库单才能删除");
            }

            // 删除明细
            jdbcTemplate.update("DELETE FROM outbound_order_detail WHERE order_no = ?", orderNo);
            // 删除主单
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
     * 打印出库单（生成出库二维码数据）
     */
    @GetMapping("/print/outbound-order/{orderNo}")
    public Result<Map<String, Object>> printOutboundOrder(@PathVariable String orderNo, HttpSession session) {
        try {
            // 查询出库单详情
            Result<Map<String, Object>> detailResult = getOutboundOrderDetail(orderNo);
            if (detailResult.getCode() != 200) {
                return detailResult;
            }

            Map<String, Object> data = detailResult.getData();
            Map<String, Object> order = (Map<String, Object>) data.get("order");
            List<Map<String, Object>> details = (List<Map<String, Object>>) data.get("details");

            // 为每个明细生成二维码内容
            List<Map<String, Object>> detailWithQR = new ArrayList<>();
            for (Map<String, Object> detail : details) {
                Map<String, Object> item = new HashMap<>(detail);
                
                // 生成出库二维码内容
                // 格式：OUTBOUND|出库单号|零件号|预期数量|时间戳
                String qrContent = String.format(
                    "OUTBOUND|%s|%s|%s|%s",
                    orderNo,
                    detail.get("part_code"),
                    detail.get("expected_quantity"),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                );
                
                item.put("qrContent", qrContent);
                item.put("qrType", "OUTBOUND");
                detailWithQR.add(item);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("order", order);
            result.put("details", detailWithQR);
            result.put("qrType", "OUTBOUND_ORDER");
            return Result.success(result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取打印数据失败: " + e.getMessage());
        }
    }

    /**
     * 批量生成出库二维码
     */
    @PostMapping("/generate-qr-codes")
    public Result<List<Map<String, Object>>> generateQRCodes(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            String orderNo = (String) request.get("orderNo");
            @SuppressWarnings("unchecked")
            List<String> partCodes = (List<String>) request.get("partCodes");

            if (orderNo == null || partCodes == null || partCodes.isEmpty()) {
                return Result.error("参数不完整");
            }

            // 查询出库单明细
            String detailSql = "SELECT * FROM outbound_order_detail WHERE order_no = ? AND part_code IN (?)";
            List<Map<String, Object>> details = jdbcTemplate.queryForList(detailSql, orderNo, String.join(",", partCodes));

            // 生成二维码数据
            List<Map<String, Object>> qrCodes = new ArrayList<>();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            for (Map<String, Object> detail : details) {
                Map<String, Object> qrData = new HashMap<>();
                
                // 二维码内容：OUTBOUND|出库单号|零件号|数量|时间戳
                String qrContent = String.format(
                    "OUTBOUND|%s|%s|%s|%s",
                    orderNo,
                    detail.get("part_code"),
                    detail.get("expected_quantity"),
                    timestamp
                );

                qrData.put("orderNo", orderNo);
                qrData.put("partCode", detail.get("part_code"));
                qrData.put("partName", detail.get("part_name"));
                qrData.put("quantity", detail.get("expected_quantity"));
                qrData.put("unit", detail.get("unit"));
                qrData.put("qrContent", qrContent);
                
                qrCodes.add(qrData);
            }

            return Result.success(qrCodes);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("生成二维码失败: " + e.getMessage());
        }
    }

    /**
     * 验证看板是否可以出库
     * 先进先出（FIFO）原则：优先出库早入库的看板
     */
    @GetMapping("/kanban/validate/{kanbanNo}")
    public Result<Map<String, Object>> validateKanbanForOutbound(@PathVariable String kanbanNo) {
        try {
            // 查询看板信息
            String kanbanSql = "SELECT k.*, c.quantity as current_quantity, c.location_code " +
                    "FROM kanban k " +
                    "LEFT JOIN current_inventory c ON k.kanban_no = c.kanban_no " +
                    "WHERE k.kanban_no = ?";
            
            List<Map<String, Object>> kanbans = jdbcTemplate.queryForList(kanbanSql, kanbanNo);
            if (kanbans.isEmpty()) {
                return Result.error("看板号不存在");
            }

            Map<String, Object> kanban = kanbans.get(0);
            String status = (String) kanban.get("status");

            // 检查看板状态（必须已入库才能出库）
            if (!"stored".equals(status)) {
                return Result.error("看板状态不是已入库，无法出库");
            }

            Integer currentQuantity = (Integer) kanban.get("current_quantity");
            if (currentQuantity == null || currentQuantity <= 0) {
                return Result.error("该看板库存数量为0，无法出库");
            }

            // 查询入库时间（用于FIFO排序）
            String inboundTimeSql = "SELECT action_time FROM inventory_trace " +
                    "WHERE kanban_no = ? AND action_type = 'INBOUND' ORDER BY action_time ASC LIMIT 1";
            List<Map<String, Object>> inboundRecords = jdbcTemplate.queryForList(inboundTimeSql, kanbanNo);
            
            Map<String, Object> result = new HashMap<>(kanban);
            if (!inboundRecords.isEmpty()) {
                result.put("inboundTime", inboundRecords.get(0).get("action_time"));
            }

            return Result.success(result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("验证看板失败: " + e.getMessage());
        }
    }

    /**
     * 扫码出库
     * 实现先进先出（FIFO）逻辑
     */
    @PostMapping("/scan/outbound")
    public Result<Map<String, Object>> scanOutbound(@RequestBody Map<String, Object> requestData, HttpSession session) {
        try {
            String kanbanNo = (String) requestData.get("kanbanNo");
            String orderNo = (String) requestData.get("orderNo");  // 关联的出库单号
            Integer outboundQuantity = (Integer) requestData.get("outboundQuantity");  // 出库数量

            // 1. 验证看板
            Result<Map<String, Object>> validateResult = validateKanbanForOutbound(kanbanNo);
            if (validateResult.getCode() != 200) {
                return Result.error(validateResult.getMessage());
            }

            Map<String, Object> kanban = validateResult.getData();
            Integer currentQuantity = (Integer) kanban.get("current_quantity");

            // 2. 检查出库数量
            if (outboundQuantity == null || outboundQuantity <= 0) {
                outboundQuantity = currentQuantity;  // 默认全部出库
            }
            if (outboundQuantity > currentQuantity) {
                return Result.error("出库数量不能大于库存数量");
            }

            // 3. 先进先出检查：查询是否有更早入库的同零件看板
            String partCode = (String) kanban.get("part_code");
            String inboundTime = kanban.get("inbound_time") != null ? kanban.get("inbound_time").toString() : null;
            
            if (inboundTime != null) {
                String fifoCheckSql = "SELECT COUNT(*) as cnt FROM kanban k " +
                        "LEFT JOIN current_inventory c ON k.kanban_no = c.kanban_no " +
                        "WHERE k.part_code = ? " +
                        "AND k.status = 'stored' " +
                        "AND c.quantity > 0 " +
                        "AND k.created_at < ? " +
                        "AND k.kanban_no != ?";
                
                Integer earlierCount = jdbcTemplate.queryForObject(fifoCheckSql, 
                        Integer.class, partCode, inboundTime, kanbanNo);
                
                if (earlierCount != null && earlierCount > 0) {
                    return Result.error("存在更早入库的同零件看板，请优先出库（先进先出原则）");
                }
            }

            // 4. 更新当前库存
            String updateInventorySql = "UPDATE current_inventory SET quantity = quantity - ?, " +
                    "updated_at = NOW() WHERE kanban_no = ?";
            jdbcTemplate.update(updateInventorySql, outboundQuantity, kanbanNo);

            // 5. 更新看板状态
            String newStatus = currentQuantity.equals(outboundQuantity) ? "outbound" : "stored";
            String updateKanbanSql = "UPDATE kanban SET status = ?, scan_time = NOW(), " +
                    "scan_by = ? WHERE kanban_no = ?";
            jdbcTemplate.update(updateKanbanSql, newStatus, getCurrentUser(session), kanbanNo);

            // 6. 记录出库追溯
            String traceNo = generateTraceNo();
            String traceSql = "INSERT INTO inventory_trace (trace_no, kanban_no, order_no, part_code, " +
                    "part_name, supplier_code, quantity, location_code, action_type, operator, remark) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OUTBOUND', ?, ?)";
            jdbcTemplate.update(traceSql,
                    traceNo,
                    kanbanNo,
                    orderNo,
                    kanban.get("part_code"),
                    kanban.get("part_name"),
                    kanban.get("supplier_code"),
                    outboundQuantity,
                    kanban.get("location_code"),
                    getCurrentUser(session),
                    "出库" + (orderNo != null ? "，出库单：" + orderNo : "")
            );

            // 7. 如果关联了出库单，更新出库单明细和状态
            if (orderNo != null && !orderNo.isEmpty()) {
                updateOutboundOrderDetail(orderNo, partCode, outboundQuantity);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("kanbanNo", kanbanNo);
            result.put("outboundQuantity", outboundQuantity);
            result.put("traceNo", traceNo);
            return Result.success(result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("出库失败: " + e.getMessage());
        }
    }

    /**
     * 获取FIFO推荐的出库看板列表
     * 按照入库时间排序，优先推荐早入库的
     */
    @GetMapping("/kanban/fifo-list")
    public Result<List<Map<String, Object>>> getFifoKanbanList(
            @RequestParam String partCode,
            @RequestParam(required = false) Integer quantity) {
        try {
            // 查询该零件的所有已入库看板，按入库时间升序（先进先出）
            String sql = "SELECT k.*, c.quantity as current_quantity, c.location_code, " +
                    "(SELECT action_time FROM inventory_trace " +
                    " WHERE kanban_no = k.kanban_no AND action_type = 'INBOUND' " +
                    " ORDER BY action_time ASC LIMIT 1) as inbound_time " +
                    "FROM kanban k " +
                    "LEFT JOIN current_inventory c ON k.kanban_no = c.kanban_no " +
                    "WHERE k.part_code = ? AND k.status = 'stored' AND c.quantity > 0 " +
                    "ORDER BY inbound_time ASC";

            List<Map<String, Object>> kanbans = jdbcTemplate.queryForList(sql, partCode);

            // 如果指定了数量，计算需要哪些看板
            if (quantity != null && quantity > 0) {
                int remaining = quantity;
                List<Map<String, Object>> recommended = new ArrayList<>();
                
                for (Map<String, Object> kanban : kanbans) {
                    if (remaining <= 0) break;
                    
                    Integer availableQty = (Integer) kanban.get("current_quantity");
                    if (availableQty != null && availableQty > 0) {
                        Map<String, Object> item = new HashMap<>(kanban);
                        int useQty = Math.min(remaining, availableQty);
                        item.put("useQuantity", useQty);
                        recommended.add(item);
                        remaining -= useQty;
                    }
                }
                
                return Result.success(recommended);
            }

            return Result.success(kanbans);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取FIFO看板列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有待出库的FIFO看板列表（前端扫码出库页面使用）
     */
    @GetMapping("/fifo/list")
    public Result<List<Map<String, Object>>> getFifoKanbanListAll(
            @RequestParam(required = false) String orderNo) {
        try {
            String sql;
            List<Object> params = new ArrayList<>();
            
            if (orderNo != null && !orderNo.isEmpty()) {
                // 如果指定了出库单号，查询该出库单关联的零件的看板
                sql = "SELECT k.*, c.quantity as current_quantity, c.location_code, " +
                        "(SELECT action_time FROM inventory_trace " +
                        " WHERE kanban_no = k.kanban_no AND action_type = 'INBOUND' " +
                        " ORDER BY action_time ASC LIMIT 1) as inbound_time " +
                        "FROM kanban k " +
                        "LEFT JOIN current_inventory c ON k.kanban_no = c.kanban_no " +
                        "WHERE k.status = 'stored' AND c.quantity > 0 " +
                        "AND k.part_code IN (SELECT DISTINCT part_code FROM outbound_order_detail WHERE order_no = ?) " +
                        "ORDER BY inbound_time ASC";
                params.add(orderNo);
            } else {
                // 查询所有已入库的看板
                sql = "SELECT k.*, c.quantity as current_quantity, c.location_code, " +
                        "(SELECT action_time FROM inventory_trace " +
                        " WHERE kanban_no = k.kanban_no AND action_type = 'INBOUND' " +
                        " ORDER BY action_time ASC LIMIT 1) as inbound_time " +
                        "FROM kanban k " +
                        "LEFT JOIN current_inventory c ON k.kanban_no = c.kanban_no " +
                        "WHERE k.status = 'stored' AND c.quantity > 0 " +
                        "ORDER BY inbound_time ASC " +
                        "LIMIT 100";
            }

            List<Map<String, Object>> kanbans = jdbcTemplate.queryForList(sql, params.toArray());
            return Result.success(kanbans);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取FIFO看板列表失败: " + e.getMessage());
        }
    }

    // ==================== 库存追溯 ====================

    /**
     * 获取出库追溯记录
     */
    @GetMapping("/trace/list")
    public Result<Map<String, Object>> getOutboundTraceList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String kanbanNo,
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) String orderNo) {
        try {
            String sql = "SELECT * FROM inventory_trace WHERE action_type = 'OUTBOUND'";
            List<Object> params = new ArrayList<>();

            if (kanbanNo != null && !kanbanNo.isEmpty()) {
                sql += " AND kanban_no = ?";
                params.add(kanbanNo);
            }
            if (partCode != null && !partCode.isEmpty()) {
                sql += " AND part_code = ?";
                params.add(partCode);
            }
            if (orderNo != null && !orderNo.isEmpty()) {
                sql += " AND order_no = ?";
                params.add(orderNo);
            }

            sql += " ORDER BY action_time DESC";

            // 分页
            String pageSql = sql + " LIMIT ? OFFSET ?";
            List<Object> pageParams = new ArrayList<>(params);
            pageParams.add(pageSize);
            pageParams.add((page - 1) * pageSize);

            List<Map<String, Object>> traces = jdbcTemplate.queryForList(pageSql, pageParams.toArray());

            // 总数
            String countSql = "SELECT COUNT(*) FROM (" + sql + ") t";
            int total = jdbcTemplate.queryForObject(countSql, params.toArray(), Integer.class);

            Map<String, Object> result = new HashMap<>();
            result.put("list", traces);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            return Result.success(result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取出库追溯记录失败: " + e.getMessage());
        }
    }

    /**
     * 按看板号追溯完整流转记录（入库+出库）
     */
    @GetMapping("/trace/by-kanban/{kanbanNo}")
    public Result<List<Map<String, Object>>> getTraceByKanban(@PathVariable String kanbanNo) {
        try {
            String sql = "SELECT * FROM inventory_trace WHERE kanban_no = ? " +
                    "ORDER BY action_time ASC";
            List<Map<String, Object>> traces = jdbcTemplate.queryForList(sql, kanbanNo);
            return Result.success(traces);

        } catch (Exception e) {
            return Result.error("获取追溯记录失败: " + e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 更新出库单明细和状态
     */
    private void updateOutboundOrderDetail(String orderNo, String partCode, Integer shippedQuantity) {
        try {
            // 更新明细
            String detailSql = "UPDATE outbound_order_detail SET shipped_quantity = shipped_quantity + ?, " +
                    "shipped_boxes = shipped_boxes + CEIL(? / NULLIF(packaging_capacity, 0)) " +
                    "WHERE order_no = ? AND part_code = ?";
            jdbcTemplate.update(detailSql, shippedQuantity, shippedQuantity, orderNo, partCode);

            // 重新计算出库单总出库数量
            String updateOrderSql = "UPDATE outbound_order o " +
                    "SET shipped_quantity = (SELECT COALESCE(SUM(shipped_quantity), 0) " +
                    "FROM outbound_order_detail WHERE order_no = ?), " +
                    "shipped_boxes = (SELECT COALESCE(SUM(shipped_boxes), 0) " +
                    "FROM outbound_order_detail WHERE order_no = ?), " +
                    "status = CASE " +
                    "WHEN shipped_quantity >= total_quantity THEN 'completed' " +
                    "WHEN shipped_quantity > 0 THEN 'partial' " +
                    "ELSE 'pending' END, " +
                    "updated_at = NOW() " +
                    "WHERE order_no = ?";
            jdbcTemplate.update(updateOrderSql, orderNo, orderNo, orderNo);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成出库单号
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "OUT" + timestamp + random;
    }

    /**
     * 生成追溯单号
     */
    private String generateTraceNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "TRACE" + timestamp + random;
    }

    /**
     * 获取当前登录用户
     */
    private String getCurrentUser(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (userObj == null) {
            return "system";
        }
        // 检查 user 对象的类型
        if (userObj instanceof Map) {
            Map<String, Object> user = (Map<String, Object>) userObj;
            return (String) user.getOrDefault("username", "system");
        } else if (userObj instanceof String) {
            // 如果是字符串，直接返回
            return (String) userObj;
        } else {
            // 其他类型，返回默认值
            return "system";
        }
    }
}
