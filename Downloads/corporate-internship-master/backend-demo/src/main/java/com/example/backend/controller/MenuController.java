package com.example.backend.controller;

import com.example.backend.entity.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class MenuController {

    @GetMapping("/menus")
    public Result<List<Map<String, Object>>> getMenus(HttpSession session) {
        if (session.getAttribute("user") == null) {
            return Result.error("未登录");
        }

        List<Map<String, Object>> menus = new ArrayList<>();

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("id", 1);
        dashboard.put("name", "工作台");
        dashboard.put("path", "/dashboard");
        dashboard.put("icon", "House");
        dashboard.put("component", "Dashboard");
        menus.add(dashboard);

        Map<String, Object> system = new LinkedHashMap<>();
        system.put("id", 2);
        system.put("name", "系统管理");
        system.put("path", null);
        system.put("icon", "Setting");
        system.put("component", null);

        List<Map<String, Object>> systemChildren = new ArrayList<>();
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", 21);
        user.put("name", "用户管理");
        user.put("path", "/system/user");
        user.put("icon", "User");
        user.put("component", "UserManage");
        systemChildren.add(user);

        Map<String, Object> role = new LinkedHashMap<>();
        role.put("id", 22);
        role.put("name", "角色管理");
        role.put("path", "/system/role");
        role.put("icon", "Key");
        role.put("component", "RoleManage");
        systemChildren.add(role);

        system.put("children", systemChildren);
        menus.add(system);

        Map<String, Object> tabsDemo = new LinkedHashMap<>();
        tabsDemo.put("id", 3);
        tabsDemo.put("name", "标签页示例");
        tabsDemo.put("path", "/tabs");
        tabsDemo.put("icon", "Document");
        tabsDemo.put("component", "TabsDemo");
        menus.add(tabsDemo);

        Map<String, Object> wmsInbound = new LinkedHashMap<>();
        wmsInbound.put("id", 4);
        wmsInbound.put("name", "WMS入库管理");
        wmsInbound.put("path", null);
        wmsInbound.put("icon", "Box");
        wmsInbound.put("component", null);

        List<Map<String, Object>> inboundChildren = new ArrayList<>();
        inboundChildren.add(menu(41, "供应商管理", "/wms/supplier", "Document", "SupplierManage"));
        inboundChildren.add(menu(42, "零件管理", "/wms/part", "Goods", "PartManage"));
        inboundChildren.add(menu(43, "入库单列表", "/wms/inbound-order/list", "Document", "InboundOrderList"));
        inboundChildren.add(menu(44, "创建入库单", "/wms/inbound-order/create", "Plus", "InboundOrderForm"));
        inboundChildren.add(menu(45, "扫码入库", "/wms/scan", "Camera", "ScanInbound"));
        wmsInbound.put("children", inboundChildren);
        menus.add(wmsInbound);

        Map<String, Object> wmsOutbound = new LinkedHashMap<>();
        wmsOutbound.put("id", 5);
        wmsOutbound.put("name", "WMS出库管理");
        wmsOutbound.put("path", null);
        wmsOutbound.put("icon", "Box");
        wmsOutbound.put("component", null);

        List<Map<String, Object>> outboundChildren = new ArrayList<>();
        outboundChildren.add(menu(51, "出库单列表", "/wms-outbound/outbound-order/list", "Document", "OutboundOrderList"));
        outboundChildren.add(menu(52, "创建出库单", "/wms-outbound/outbound-order/create", "Plus", "OutboundOrderForm"));
        outboundChildren.add(menu(53, "扫码出库", "/wms-outbound/scan", "Camera", "ScanOutbound"));
        wmsOutbound.put("children", outboundChildren);
        menus.add(wmsOutbound);

        // 实时库存总览（独立一级菜单）
        Map<String, Object> currentInventory = new LinkedHashMap<>();
        currentInventory.put("id", 7);
        currentInventory.put("name", "实时库存总览");
        currentInventory.put("path", "/current-inventory");
        currentInventory.put("icon", "TrendCharts");
        currentInventory.put("component", "CurrentInventory");
        menus.add(currentInventory);

        // 库存追溯（独立一级菜单）
        Map<String, Object> inventoryTrace = new LinkedHashMap<>();
        inventoryTrace.put("id", 8);
        inventoryTrace.put("name", "库存追溯");
        inventoryTrace.put("path", null);
        inventoryTrace.put("icon", "Search");
        inventoryTrace.put("component", null);

        List<Map<String, Object>> traceChildren = new ArrayList<>();
        traceChildren.add(menu(81, "全部流水", "/inventory-trace", "List", "InventoryTrace"));
        traceChildren.add(menu(84, "看板生命周期", "/inventory-trace/lifecycle", "Refresh", "InventoryTraceLifecycle"));
        inventoryTrace.put("children", traceChildren);
        menus.add(inventoryTrace);

        // 转包管理（独立一级菜单）
        Map<String, Object> transferManage = new LinkedHashMap<>();
        transferManage.put("id", 9);
        transferManage.put("name", "转包管理");
        transferManage.put("path", "/transfer/list");
        transferManage.put("icon", "Refresh");
        transferManage.put("component", "TransferOrderList");
        menus.add(transferManage);

        // 封存管理（独立一级菜单）
        Map<String, Object> sealedManage = new LinkedHashMap<>();
        sealedManage.put("id", 10);
        sealedManage.put("name", "封存管理");
        sealedManage.put("path", "/sealed-kanban");
        sealedManage.put("icon", "Box");
        sealedManage.put("component", "SealedKanbanList");
        menus.add(sealedManage);

        // AI需求预测（独立一级菜单）
        Map<String, Object> aiPredict = new LinkedHashMap<>();
        aiPredict.put("id", 11);
        aiPredict.put("name", "AI需求预测");
        aiPredict.put("path", "/ai-predict");
        aiPredict.put("icon", "TrendCharts");
        aiPredict.put("component", "AIPredictDashboard");
        menus.add(aiPredict);


        return Result.success(menus);
    }

    private Map<String, Object> menu(int id, String name, String path, String icon, String component) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("name", name);
        item.put("path", path);
        item.put("icon", icon);
        item.put("component", component);
        return item;
    }
}
