CREATE TABLE IF NOT EXISTS `user` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `supplier` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `supplier_code` VARCHAR(50) NOT NULL,
  `supplier_name` VARCHAR(100) NOT NULL,
  `contact_person` VARCHAR(100) DEFAULT NULL,
  `phone` VARCHAR(50) DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_supplier_code` (`supplier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `part` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `part_code` VARCHAR(100) NOT NULL,
  `part_name` VARCHAR(200) NOT NULL,
  `supplier_code` VARCHAR(50) DEFAULT NULL,
  `packaging_capacity` INT DEFAULT 1,
  `unit` VARCHAR(20) DEFAULT '个',
  `price` DECIMAL(12,2) DEFAULT 0,
  `weight` DECIMAL(12,2) DEFAULT 0,
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_part_code` (`part_code`),
  KEY `idx_part_supplier` (`supplier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `warehouse` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `warehouse_code` VARCHAR(50) NOT NULL,
  `warehouse_name` VARCHAR(100) NOT NULL,
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_warehouse_code` (`warehouse_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `storage_location` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `location_code` VARCHAR(50) NOT NULL,
  `location_name` VARCHAR(100) NOT NULL,
  `warehouse_code` VARCHAR(50) NOT NULL,
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_location_code` (`location_code`),
  KEY `idx_location_warehouse` (`warehouse_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `inbound_order` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(50) NOT NULL,
  `inbound_type` VARCHAR(50) NOT NULL,
  `supplier_code` VARCHAR(50) DEFAULT NULL,
  `warehouse_code` VARCHAR(50) DEFAULT NULL,
  `status` VARCHAR(20) DEFAULT 'pending',
  `total_quantity` INT DEFAULT 0,
  `received_quantity` INT DEFAULT 0,
  `total_boxes` INT DEFAULT 0,
  `received_boxes` INT DEFAULT 0,
  `remark` TEXT DEFAULT NULL,
  `created_by` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inbound_order_no` (`order_no`),
  KEY `idx_inbound_status` (`status`),
  KEY `idx_inbound_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `inbound_order_detail` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(50) NOT NULL,
  `part_code` VARCHAR(100) NOT NULL,
  `part_name` VARCHAR(200) DEFAULT NULL,
  `expected_quantity` INT DEFAULT 0,
  `received_quantity` INT DEFAULT 0,
  `packaging_capacity` INT DEFAULT 1,
  `expected_boxes` INT DEFAULT 0,
  `received_boxes` INT DEFAULT 0,
  `unit` VARCHAR(20) DEFAULT '个',
  PRIMARY KEY (`id`),
  KEY `idx_inbound_detail_order_no` (`order_no`),
  KEY `idx_inbound_detail_part_code` (`part_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `outbound_order` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(50) NOT NULL,
  `outbound_type` VARCHAR(50) NOT NULL,
  `customer_code` VARCHAR(50) DEFAULT NULL,
  `customer_name` VARCHAR(100) DEFAULT NULL,
  `warehouse_code` VARCHAR(50) DEFAULT NULL,
  `status` VARCHAR(20) DEFAULT 'pending',
  `total_quantity` INT DEFAULT 0,
  `shipped_quantity` INT DEFAULT 0,
  `total_boxes` INT DEFAULT 0,
  `shipped_boxes` INT DEFAULT 0,
  `remark` TEXT DEFAULT NULL,
  `created_by` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_outbound_order_no` (`order_no`),
  KEY `idx_outbound_status` (`status`),
  KEY `idx_outbound_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `outbound_order_detail` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(50) NOT NULL,
  `part_code` VARCHAR(100) NOT NULL,
  `part_name` VARCHAR(200) DEFAULT NULL,
  `expected_quantity` INT DEFAULT 0,
  `shipped_quantity` INT DEFAULT 0,
  `packaging_capacity` INT DEFAULT 1,
  `expected_boxes` INT DEFAULT 0,
  `shipped_boxes` INT DEFAULT 0,
  `unit` VARCHAR(20) DEFAULT '个',
  PRIMARY KEY (`id`),
  KEY `idx_outbound_detail_order_no` (`order_no`),
  KEY `idx_outbound_detail_part_code` (`part_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `kanban` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `kanban_no` VARCHAR(100) NOT NULL,
  `order_no` VARCHAR(50) NOT NULL,
  `part_code` VARCHAR(100) NOT NULL,
  `part_name` VARCHAR(200) DEFAULT NULL,
  `supplier_code` VARCHAR(50) DEFAULT NULL,
  `quantity` INT DEFAULT 0,
  `box_count` INT DEFAULT 0,
  `qr_code` TEXT DEFAULT NULL,
  `status` VARCHAR(20) DEFAULT 'pending',
  `location_code` VARCHAR(50) DEFAULT NULL,
  `scan_time` DATETIME DEFAULT NULL,
  `scan_by` VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kanban_no` (`kanban_no`),
  KEY `idx_kanban_order_no` (`order_no`),
  KEY `idx_kanban_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `current_inventory` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `kanban_no` VARCHAR(100) NOT NULL,
  `part_code` VARCHAR(100) NOT NULL,
  `part_name` VARCHAR(200) DEFAULT NULL,
  `supplier_code` VARCHAR(50) DEFAULT NULL,
  `quantity` INT DEFAULT 0,
  `location_code` VARCHAR(50) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inventory_kanban_no` (`kanban_no`),
  KEY `idx_inventory_part_code` (`part_code`),
  KEY `idx_inventory_location_code` (`location_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `inventory_trace` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `trace_no` VARCHAR(150) NOT NULL,
  `kanban_no` VARCHAR(100) DEFAULT NULL,
  `order_no` VARCHAR(50) DEFAULT NULL,
  `part_code` VARCHAR(100) DEFAULT NULL,
  `part_name` VARCHAR(200) DEFAULT NULL,
  `supplier_code` VARCHAR(50) DEFAULT NULL,
  `quantity` INT DEFAULT 0,
  `location_code` VARCHAR(50) DEFAULT NULL,
  `action_type` VARCHAR(20) NOT NULL,
  `action_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `operator` VARCHAR(100) DEFAULT NULL,
  `remark` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trace_no` (`trace_no`),
  KEY `idx_trace_kanban_no` (`kanban_no`),
  KEY `idx_trace_part_code` (`part_code`),
  KEY `idx_trace_action_type` (`action_type`),
  KEY `idx_trace_action_time` (`action_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
