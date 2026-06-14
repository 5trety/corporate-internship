package com.example.backend.controller;

import com.example.backend.entity.Result;
import com.example.backend.entity.WmsSupplier;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WmsControllerTest {

    @Test
    void saveSupplierReactivatesSoftDeletedSupplierCode() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        WmsController controller = new WmsController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", jdbcTemplate);

        WmsSupplier supplier = new WmsSupplier();
        supplier.setSupplierCode("SUP002");
        supplier.setSupplierName("供应商");
        supplier.setContactPerson("李四");
        supplier.setPhone("12300000000");
        supplier.setAddress("地址1");

        when(jdbcTemplate.queryForList(
                eq("SELECT id, status FROM supplier WHERE supplier_code = ?"),
                eq("SUP002")
        )).thenReturn(List.of(Map.of("id", 2, "status", 0)));

        Result<Map<String, Object>> result = controller.saveSupplier(supplier);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        verify(jdbcTemplate).update(
                eq("UPDATE supplier SET supplier_name=?, contact_person=?, phone=?, address=?, status=1 WHERE id=?"),
                eq("供应商"),
                eq("李四"),
                eq("12300000000"),
                eq("地址1"),
                eq(2)
        );
    }

    @Test
    void scanOutboundRejectsScannedKanbanWhenItIsNotFirstInFifo() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        HttpSession session = mock(HttpSession.class);
        WmsController controller = new WmsController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", jdbcTemplate);

        when(session.getAttribute("user")).thenReturn("admin");
        when(jdbcTemplate.queryForObject(
                eq("SELECT status FROM outbound_order WHERE order_no = ?"),
                eq(String.class),
                eq("SO-1")
        )).thenReturn("pending");
        when(jdbcTemplate.queryForList(
                eq("SELECT * FROM outbound_order_detail WHERE order_no = ? AND part_code = ?"),
                eq("SO-1"),
                eq("PART001")
        )).thenReturn(List.of(Map.of(
                "expected_quantity", 100,
                "shipped_quantity", 0,
                "packaging_capacity", 10
        )));
        when(jdbcTemplate.queryForList(
                eq("SELECT * FROM current_inventory WHERE part_code = ? AND quantity > 0 ORDER BY created_at ASC"),
                eq("PART001")
        )).thenReturn(List.of(
                Map.of("kanban_no", "KAN-A", "part_code", "PART001", "part_name", "Part A",
                        "supplier_code", "SUP001", "quantity", 10, "location_code", "LOC-A01"),
                Map.of("kanban_no", "KAN-B", "part_code", "PART001", "part_name", "Part A",
                        "supplier_code", "SUP001", "quantity", 10, "location_code", "LOC-A02")
        ));

        Result<Map<String, Object>> result = controller.scanOutbound(Map.of(
                "orderNo", "SO-1",
                "kanbanNo", "KAN-B",
                "partCode", "PART001",
                "quantity", 10
        ), session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("FIFO"));
        assertTrue(result.getMessage().contains("KAN-A"));
    }

    @Test
    void scanOutboundAcceptsOutboundOrderKanbanAndConsumesFifoInventory() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        HttpSession session = mock(HttpSession.class);
        WmsController controller = new WmsController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", jdbcTemplate);

        when(session.getAttribute("user")).thenReturn("admin");
        when(jdbcTemplate.queryForObject(
                eq("SELECT status FROM outbound_order WHERE order_no = ?"),
                eq(String.class),
                eq("SO-2")
        )).thenReturn("pending");
        when(jdbcTemplate.queryForList(
                eq("SELECT * FROM outbound_order_detail WHERE order_no = ? AND part_code = ?"),
                eq("SO-2"),
                eq("PART001")
        )).thenReturn(List.of(Map.of(
                "expected_quantity", 50,
                "shipped_quantity", 0,
                "packaging_capacity", 50
        )));
        when(jdbcTemplate.queryForList(
                eq("SELECT * FROM current_inventory WHERE part_code = ? AND quantity > 0 ORDER BY created_at ASC"),
                eq("PART001")
        )).thenReturn(List.of(
                Map.of("kanban_no", "KAN-STOCK", "part_code", "PART001", "part_name", "Part A",
                        "supplier_code", "SUP001", "quantity", 50, "location_code", "LOC-A01")
        ));
        when(jdbcTemplate.queryForList(
                eq("SELECT * FROM kanban WHERE kanban_no = ? AND status = 'pending'"),
                eq("KAN-LABEL")
        )).thenReturn(List.of(Map.of(
                "kanban_no", "KAN-LABEL",
                "order_no", "SO-2",
                "part_code", "PART001",
                "quantity", 50
        )));
        when(jdbcTemplate.queryForObject(
                eq("SELECT total_quantity FROM outbound_order WHERE order_no = ?"),
                eq(Integer.class),
                eq("SO-2")
        )).thenReturn(50);
        when(jdbcTemplate.queryForObject(
                eq("SELECT shipped_quantity FROM outbound_order WHERE order_no = ?"),
                eq(Integer.class),
                eq("SO-2")
        )).thenReturn(50);

        Result<Map<String, Object>> result = controller.scanOutbound(Map.of(
                "orderNo", "SO-2",
                "kanbanNo", "KAN-LABEL",
                "partCode", "PART001",
                "quantity", 50
        ), session);

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals("KAN-LABEL", result.getData().get("kanbanNo"));
        verify(jdbcTemplate).update(
                eq("UPDATE kanban SET status = 'outbound' WHERE kanban_no = ?"),
                eq("KAN-LABEL")
        );
    }

    @Test
    void printKanbanRejectsCompletedOutboundDetail() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        HttpSession session = mock(HttpSession.class);
        WmsController controller = new WmsController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", jdbcTemplate);

        when(jdbcTemplate.queryForList(
                eq("SELECT * FROM outbound_order_detail WHERE order_no = ? AND part_code = ?"),
                eq("SO-1"),
                eq("PART001")
        )).thenReturn(List.of(Map.of(
                "expected_quantity", 50,
                "shipped_quantity", 50,
                "packaging_capacity", 50
        )));

        Result<List<Map<String, String>>> result = controller.printKanban(Map.of(
                "orderNo", "SO-1",
                "items", List.of(Map.of(
                        "partCode", "PART001",
                        "partName", "Part A",
                        "quantity", 50,
                        "boxCount", 1
                ))
        ), session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("PART001"));
        assertTrue(result.getMessage().contains("已完成出库"));
    }

    @Test
    void printKanbanRejectsOutboundDetailWithoutInventory() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        HttpSession session = mock(HttpSession.class);
        WmsController controller = new WmsController();
        ReflectionTestUtils.setField(controller, "jdbcTemplate", jdbcTemplate);

        when(jdbcTemplate.queryForList(
                eq("SELECT * FROM outbound_order_detail WHERE order_no = ? AND part_code = ?"),
                eq("SO-1"),
                eq("PART002")
        )).thenReturn(List.of(Map.of(
                "expected_quantity", 101,
                "shipped_quantity", 0,
                "packaging_capacity", 100
        )));
        when(jdbcTemplate.queryForObject(
                eq("SELECT COALESCE(SUM(quantity), 0) FROM current_inventory WHERE part_code = ? AND quantity > 0"),
                eq(Integer.class),
                eq("PART002")
        )).thenReturn(0);

        Result<List<Map<String, String>>> result = controller.printKanban(Map.of(
                "orderNo", "SO-1",
                "items", List.of(Map.of(
                        "partCode", "PART002",
                        "partName", "Part B",
                        "quantity", 101,
                        "boxCount", 2
                ))
        ), session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("PART002"));
        assertTrue(result.getMessage().contains("库存不足"));
    }
}
