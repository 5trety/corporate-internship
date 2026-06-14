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

        menus.add(menu(6, "库存追溯", "/inventory-trace", "Search", "InventoryTrace"));

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
