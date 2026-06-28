package com.example.backend.controller;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.backend.entity.Result;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WmsControllerInventoryRegressionTest {

    @Test
    void currentInventoryKeepsRowsWhenKanbanMasterRecordIsMissing() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        WmsController controller = controllerWith(jdbcTemplate);

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(0);
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class))).thenReturn(Map.of(
                "total_quantity", 0,
                "total_count", 0,
                "part_types", 0,
                "warehouse_count", 0
        ));

        controller.getCurrentInventory(null, null, 1, 20);

        ArgumentCaptor<String> listSql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForList(listSql.capture(), any(Object[].class));

        assertTrue(listSql.getValue().contains("LEFT JOIN kanban k ON i.kanban_no = k.kanban_no"));
        assertTrue(listSql.getValue().contains("(k.kanban_no IS NULL OR k.is_sealed = 0)"));
    }

    @Test
    void printInboundKanbanDoesNotDeleteStoredInventoryKanbans() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        WmsController controller = controllerWith(jdbcTemplate);

        when(jdbcTemplate.queryForObject(
                eq("SELECT warehouse_code FROM inbound_order WHERE order_no = ?"),
                eq(String.class),
                eq("PO-TEST")
        )).thenReturn("WH-A");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(1);
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        controller.printKanban(Map.of(
                "orderNo", "PO-TEST",
                "items", List.of(Map.of(
                        "partCode", "0001",
                        "partName", "Part A",
                        "supplierCode", "001",
                        "quantity", 1,
                        "kanbanNo", "KAN-TEST-0001"
                ))
        ), mock(HttpSession.class));

        ArgumentCaptor<String> updateSql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).update(updateSql.capture(), any(Object[].class));

        String deleteSql = updateSql.getAllValues().stream()
                .filter(sql -> sql.startsWith("DELETE FROM kanban"))
                .findFirst()
                .orElse("");

        assertTrue(deleteSql.contains("status = 'pending'"));
        assertTrue(deleteSql.contains("current_inventory"));
    }

    @Test
    void outboundPrintFifoKanbansIncludeInventoryRowsWithoutKanbanMasterRecord() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        WmsController controller = controllerWith(jdbcTemplate);

        when(jdbcTemplate.queryForMap(
                eq("SELECT * FROM outbound_order WHERE order_no = ?"),
                eq("SO-TEST")
        )).thenReturn(Map.of("warehouse_code", "WH-A"));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());

        controller.printKanban(Map.of(
                "orderNo", "SO-TEST",
                "items", List.of(Map.of(
                        "partCode", "0001",
                        "partName", "Part A",
                        "quantity", 9
                ))
        ), mock(HttpSession.class));

        ArgumentCaptor<String> querySql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).queryForList(querySql.capture(), any(Object[].class));

        String fifoSql = querySql.getAllValues().stream()
                .filter(sql -> sql.contains("current_inventory"))
                .findFirst()
                .orElse("");

        assertTrue(fifoSql.contains("FROM current_inventory ci"));
        assertTrue(fifoSql.contains("LEFT JOIN kanban k ON ci.kanban_no = k.kanban_no"));
        assertTrue(fifoSql.contains("(k.kanban_no IS NULL OR (k.status IN ('scanned', 'stored') AND k.is_sealed = 0))"));
    }

    @Test
    void outboundPrintFifoKanbansStopsAtRequestedQuantity() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        WmsController controller = controllerWith(jdbcTemplate);

        when(jdbcTemplate.queryForMap(
                eq("SELECT * FROM outbound_order WHERE order_no = ?"),
                eq("SO-TEST")
        )).thenReturn(Map.of("warehouse_code", "WH-A"));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                Map.of("kanban_no", "KAN-001", "part_code", "0002", "part_name", "Part B", "supplier_code", "001", "available_quantity", 5),
                Map.of("kanban_no", "KAN-002", "part_code", "0002", "part_name", "Part B", "supplier_code", "001", "available_quantity", 5),
                Map.of("kanban_no", "KAN-003", "part_code", "0002", "part_name", "Part B", "supplier_code", "001", "available_quantity", 5)
        ));

        Result<List<Map<String, String>>> result = controller.printKanban(Map.of(
                "orderNo", "SO-TEST",
                "items", List.of(Map.of(
                        "partCode", "0002",
                        "partName", "Part B",
                        "quantity", 10
                ))
        ), mock(HttpSession.class));

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
        assertEquals("KAN-001", result.getData().get(0).get("kanbanNo"));
        assertEquals("KAN-002", result.getData().get(1).get("kanbanNo"));
    }

    private static WmsController controllerWith(JdbcTemplate jdbcTemplate) throws Exception {
        WmsController controller = new WmsController();
        Field field = WmsController.class.getDeclaredField("jdbcTemplate");
        field.setAccessible(true);
        field.set(controller, jdbcTemplate);
        return controller;
    }
}
