-- 仓库表
CREATE TABLE IF NOT EXISTS `warehouse` (
  `id` int NOT NULL AUTO_INCREMENT,
  `warehouse_code` varchar(50) NOT NULL,
  `warehouse_name` varchar(100) NOT NULL,
  `address` varchar(200) DEFAULT NULL,
  `remark` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_warehouse_code` (`warehouse_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库表';

-- 供应商表
CREATE TABLE IF NOT EXISTS `supplier` (
  `id` int NOT NULL AUTO_INCREMENT,
  `supplier_code` varchar(50) NOT NULL,
  `supplier_name` varchar(100) NOT NULL,
  `contact` varchar(50) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `address` varchar(200) DEFAULT NULL,
  `remark` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_supplier_code` (`supplier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

-- 零件表
CREATE TABLE IF NOT EXISTS `part` (
  `id` int NOT NULL AUTO_INCREMENT,
  `part_code` varchar(100) NOT NULL,
  `part_name` varchar(200) NOT NULL,
  `supplier_code` varchar(50) DEFAULT NULL,
  `unit` varchar(10) DEFAULT '个',
  `packaging_capacity` int DEFAULT 1 COMMENT '包装容量',
  `remark` text,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_part_code` (`part_code`),
  KEY `idx_supplier_code` (`supplier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='零件表';

-- 看板表
CREATE TABLE IF NOT EXISTS `kanban` (
  `id` int NOT NULL AUTO_INCREMENT,
  `kanban_no` varchar(50) NOT NULL,
  `part_code` varchar(100) NOT NULL,
  `part_name` varchar(200) DEFAULT NULL,
  `warehouse_code` varchar(50) DEFAULT NULL,
  `supplier_code` varchar(50) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态：pending待入库, stored已入库, outbound已出库',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kanban_no` (`kanban_no`),
  KEY `idx_part_code` (`part_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='看板表';

-- 当前库存表
CREATE TABLE IF NOT EXISTS `current_inventory` (
  `id` int NOT NULL AUTO_INCREMENT,
  `kanban_no` varchar(50) NOT NULL,
  `part_code` varchar(100) NOT NULL,
  `quantity` int DEFAULT 0,
  `location_code` varchar(50) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kanban_no` (`kanban_no`),
  KEY `idx_part_code` (`part_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='当前库存表';

-- 库存追溯表
CREATE TABLE IF NOT EXISTS `inventory_trace` (
  `id` int NOT NULL AUTO_INCREMENT,
  `trace_no` varchar(50) NOT NULL,
  `kanban_no` varchar(50) NOT NULL,
  `part_code` varchar(100) NOT NULL,
  `action_type` varchar(20) NOT NULL COMMENT 'INBOUND入库, OUTBOUND出库',
  `quantity` int DEFAULT 0,
  `order_no` varchar(50) DEFAULT NULL,
  `operator` varchar(50) DEFAULT NULL,
  `action_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `remark` text,
  PRIMARY KEY (`id`),
  KEY `idx_kanban_no` (`kanban_no`),
  KEY `idx_part_code` (`part_code`),
  KEY `idx_action_type` (`action_type`),
  KEY `idx_action_time` (`action_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存追溯表';

-- 入库单表
CREATE TABLE IF NOT EXISTS `inbound_order` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_no` varchar(50) NOT NULL,
  `inbound_type` varchar(50) NOT NULL COMMENT '入库类型：采购入库、生产入库、退货入库',
  `supplier_code` varchar(50) DEFAULT NULL,
  `supplier_name` varchar(100) DEFAULT NULL,
  `warehouse_code` varchar(50) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态：pending待入库, partial部分入库, completed已完成',
  `total_quantity` int DEFAULT 0,
  `received_quantity` int DEFAULT 0,
  `total_boxes` int DEFAULT 0,
  `received_boxes` int DEFAULT 0,
  `remark` text,
  `created_by` varchar(50) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单表';

-- 入库单明细表
CREATE TABLE IF NOT EXISTS `inbound_order_detail` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order_no` varchar(50) NOT NULL,
  `part_code` varchar(100) NOT NULL,
  `part_name` varchar(200) DEFAULT NULL,
  `expected_quantity` int DEFAULT 0,
  `received_quantity` int DEFAULT 0,
  `packaging_capacity` int DEFAULT 0,
  `expected_boxes` int DEFAULT 0,
  `received_boxes` int DEFAULT 0,
  `unit` varchar(10) DEFAULT '个',
  PRIMARY KEY (`id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_part_code` (`part_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单明细表';

-- 插入默认仓库
INSERT INTO `warehouse` (`warehouse_code`, `warehouse_name`, `address`) VALUES 
('WH001', '默认仓库', '系统默认仓库');
