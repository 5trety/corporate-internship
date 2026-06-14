INSERT IGNORE INTO `user` (`username`, `password`) VALUES
  ('admin', 'admin');

INSERT IGNORE INTO `supplier` (`supplier_code`, `supplier_name`, `contact_person`, `phone`, `address`) VALUES
  ('SUP001', '默认供应商', '张三', '13800000000', '默认地址');

INSERT IGNORE INTO `part` (`part_code`, `part_name`, `supplier_code`, `packaging_capacity`, `unit`, `price`, `weight`) VALUES
  ('PART001', '测试零件A', 'SUP001', 50, '个', 10.00, 1.20),
  ('PART002', '测试零件B', 'SUP001', 100, '个', 5.00, 0.80);

INSERT IGNORE INTO `warehouse` (`warehouse_code`, `warehouse_name`) VALUES
  ('WH01', '默认仓库');

INSERT IGNORE INTO `storage_location` (`location_code`, `location_name`, `warehouse_code`) VALUES
  ('LOC-A01', 'A区一号库位', 'WH01'),
  ('LOC-A02', 'A区二号库位', 'WH01');
