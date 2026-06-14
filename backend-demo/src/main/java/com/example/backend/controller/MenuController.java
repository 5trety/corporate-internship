package com.example.backend.controller;

import com.example.backend.entity.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.util.*;

@RestController
public class MenuController {

    // 获取菜单接口（需登录）
    @GetMapping("/menus")
    public Result<List<Map<String, Object>>> getMenus(HttpSession session) {
        // 检查是否登录
        if (session.getAttribute("user") == null) {
            return Result.error("未登录");
        }

        // 构建菜单数据（树形结构，支持多层）
        List<Map<String, Object>> menus = new ArrayList<>();

        // 一级菜单 1：工作台
        Map<String, Object> dash = new LinkedHashMap<>();
        dash.put("id", 1);
        dash.put("name", "工作台");
        dash.put("path", "/dashboard");
        dash.put("icon", "House");
        dash.put("component", "Dashboard");
        menus.add(dash);

        // 一级菜单 2：系统管理（含二级菜单）
        Map<String, Object> sys = new LinkedHashMap<>();
        sys.put("id", 2);
        sys.put("name", "系统管理");
        sys.put("path", null);
        sys.put("icon", "Setting");
        sys.put("component", null);

        List<Map<String, Object>> children = new ArrayList<>();
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", 21);
        user.put("name", "用户管理");
        user.put("path", "/system/user");
        user.put("icon", "User");
        user.put("component", "UserManage");
        children.add(user);

        Map<String, Object> role = new LinkedHashMap<>();
        role.put("id", 22);
        role.put("name", "角色管理");
        role.put("path", "/system/role");
        role.put("icon", "Key");
        role.put("component", "RoleManage");
        children.add(role);

        sys.put("children", children);
        menus.add(sys);

        // 一级菜单 3：标签页示例（用于演示联动）
        Map<String, Object> tabsDemo = new LinkedHashMap<>();
        tabsDemo.put("id", 3);
        tabsDemo.put("name", "标签页示例");
        tabsDemo.put("path", "/tabs");
        tabsDemo.put("icon", "Document");
        tabsDemo.put("component", "TabsDemo");
        menus.add(tabsDemo);

        // ==================== 一级菜单 4：WMS入库管理 ====================
        Map<String, Object> wms = new LinkedHashMap<>();
        wms.put("id", 4);
        wms.put("name", "WMS入库管理");
        wms.put("path", null);
        wms.put("icon", "Box");
        wms.put("component", null);

        List<Map<String, Object>> wmsChildren = new ArrayList<>();

        Map<String, Object> supplier = new LinkedHashMap<>();
        supplier.put("id", 41);
        supplier.put("name", "供应商管理");
        supplier.put("path", "/wms/supplier");
        supplier.put("icon", "Document");
        supplier.put("component", "SupplierManage");
        wmsChildren.add(supplier);

        Map<String, Object> part = new LinkedHashMap<>();
        part.put("id", 42);
        part.put("name", "零件管理");
        part.put("path", "/wms/part");
        part.put("icon", "Goods");
        part.put("component", "PartManage");
        wmsChildren.add(part);

        Map<String, Object> inboundList = new LinkedHashMap<>();
        inboundList.put("id", 43);
        inboundList.put("name", "入库单列表");
        inboundList.put("path", "/wms/inbound-order/list");
        inboundList.put("icon", "Document");
        inboundList.put("component", "InboundOrderList");
        wmsChildren.add(inboundList);

        Map<String, Object> createOrder = new LinkedHashMap<>();
        createOrder.put("id", 44);
        createOrder.put("name", "创建入库单");
        createOrder.put("path", "/wms/inbound-order/create");
        createOrder.put("icon", "Plus");
        createOrder.put("component", "InboundOrderForm");
        wmsChildren.add(createOrder);

        Map<String, Object> scan = new LinkedHashMap<>();
        scan.put("id", 45);
        scan.put("name", "扫码入库");
        scan.put("path", "/wms/scan");
        scan.put("icon", "Camera");
        scan.put("component", "ScanInbound");
        wmsChildren.add(scan);

        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("id", 46);
        trace.put("name", "库存追溯");
        trace.put("path", "/wms/trace");
        trace.put("icon", "Search");
        trace.put("component", "InventoryTrace");
        wmsChildren.add(trace);

        wms.put("children", wmsChildren);
        menus.add(wms);

        // ==================== 一级菜单 5：WMS出库管理 ====================
        Map<String, Object> wmsOutbound = new LinkedHashMap<>();
        wmsOutbound.put("id", 5);
        wmsOutbound.put("name", "WMS出库管理");
        wmsOutbound.put("path", null);
        wmsOutbound.put("icon", "Box");
        wmsOutbound.put("component", null);

        List<Map<String, Object>> outboundChildren = new ArrayList<>();

        Map<String, Object> outboundList = new LinkedHashMap<>();
        outboundList.put("id", 51);
        outboundList.put("name", "出库单列表");
        outboundList.put("path", "/wms-outbound/outbound-order/list");
        outboundList.put("icon", "Document");
        outboundList.put("component", "OutboundOrderList");
        outboundChildren.add(outboundList);

        Map<String, Object> createOutbound = new LinkedHashMap<>();
        createOutbound.put("id", 52);
        createOutbound.put("name", "创建出库单");
        createOutbound.put("path", "/wms-outbound/outbound-order/create");
        createOutbound.put("icon", "Plus");
        createOutbound.put("component", "OutboundOrderForm");
        outboundChildren.add(createOutbound);

        Map<String, Object> scanOutbound = new LinkedHashMap<>();
        scanOutbound.put("id", 53);
        scanOutbound.put("name", "扫码出库");
        scanOutbound.put("path", "/wms-outbound/scan-outbound");
        scanOutbound.put("icon", "Camera");
        scanOutbound.put("component", "ScanOutbound");
        outboundChildren.add(scanOutbound);

        wmsOutbound.put("children", outboundChildren);
        menus.add(wmsOutbound);

        return Result.success(menus);
    }
}