-- Seed complete random AI/WMS test data for local demos.
-- Re-running this script is safe because it clears previous AI-FULL-TEST rows first.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @ai_marker = 'AI-FULL-TEST';
SET @supplier_code = '001';
SET @warehouse_code = 'WH-A';
SET @operator = 'ai-test';
SET @run_token = DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s%f');

START TRANSACTION;

DELETE FROM inventory_trace
WHERE order_no LIKE 'PO-AIFULL-%'
   OR order_no LIKE 'SO-AIFULL-%'
   OR trace_no LIKE CONCAT(@ai_marker, '-%')
   OR remark LIKE CONCAT('%', @ai_marker, '%')
   OR part_code IN ('AITEST-001', 'AITEST-002', 'AITEST-003', 'AITEST-004', 'AITEST-005');

DELETE FROM current_inventory
WHERE kanban_no LIKE CONCAT('KAN-', @ai_marker, '-%')
   OR part_code IN ('AITEST-001', 'AITEST-002', 'AITEST-003', 'AITEST-004', 'AITEST-005');

DELETE FROM kanban
WHERE kanban_no LIKE CONCAT('KAN-', @ai_marker, '-%')
   OR order_no LIKE 'PO-AIFULL-%'
   OR order_no LIKE 'SO-AIFULL-%'
   OR part_code IN ('AITEST-001', 'AITEST-002', 'AITEST-003', 'AITEST-004', 'AITEST-005');

DELETE FROM inbound_order_detail
WHERE order_no LIKE 'PO-AIFULL-%'
   OR part_code IN ('AITEST-001', 'AITEST-002', 'AITEST-003', 'AITEST-004', 'AITEST-005');

DELETE FROM outbound_order_detail
WHERE order_no LIKE 'SO-AIFULL-%'
   OR part_code IN ('AITEST-001', 'AITEST-002', 'AITEST-003', 'AITEST-004', 'AITEST-005');

DELETE FROM inbound_order
WHERE order_no LIKE 'PO-AIFULL-%'
   OR remark LIKE CONCAT('%', @ai_marker, '%');

DELETE FROM outbound_order
WHERE order_no LIKE 'SO-AIFULL-%'
   OR remark LIKE CONCAT('%', @ai_marker, '%');

DELETE FROM part
WHERE part_code IN ('AITEST-001', 'AITEST-002', 'AITEST-003', 'AITEST-004', 'AITEST-005')
  AND part_name IN (
      'AI Full Test Stable',
      'AI Full Test Rising',
      'AI Full Test Volatile',
      'AI Full Test Shortage',
      'AI Full Test Obsolete',
      'AI Full Test Fluctuation',
      'AI Full Test Rising Demand',
      'AI Full Test Falling Demand'
  );

DROP TEMPORARY TABLE IF EXISTS ai_full_test_parts;
CREATE TEMPORARY TABLE ai_full_test_parts (
    part_index INT NOT NULL,
    part_code VARCHAR(100) NOT NULL,
    part_name VARCHAR(200) NOT NULL,
    warning_target VARCHAR(20) NOT NULL,
    pattern_name VARCHAR(50) NOT NULL
);

INSERT INTO ai_full_test_parts (part_index, part_code, part_name, warning_target, pattern_name) VALUES
    (1, 'AITEST-001', 'AI Full Test Shortage', 'SHORTAGE', 'shortage warning demo'),
    (2, 'AITEST-002', 'AI Full Test Obsolete', 'OBSOLETE', 'obsolete warning demo'),
    (3, 'AITEST-003', 'AI Full Test Fluctuation', 'FLUCTUATION', 'fluctuation warning demo'),
    (4, 'AITEST-004', 'AI Full Test Rising Demand', 'RISING', 'rising demand prediction demo'),
    (5, 'AITEST-005', 'AI Full Test Falling Demand', 'FALLING', 'falling demand prediction demo');

DROP TEMPORARY TABLE IF EXISTS ai_full_test_days;
CREATE TEMPORARY TABLE ai_full_test_days (day_offset INT NOT NULL);

INSERT INTO ai_full_test_days (day_offset) VALUES
    (6), (5), (4), (3), (2), (1), (0);

DROP TEMPORARY TABLE IF EXISTS ai_full_test_daily;
CREATE TEMPORARY TABLE ai_full_test_daily AS
SELECT
    p.part_index,
    p.part_code,
    p.part_name,
    p.warning_target,
    p.pattern_name,
    d.day_offset,
    CASE p.warning_target
        WHEN 'SHORTAGE' THEN
            CASE WHEN d.day_offset IN (6, 4, 2) THEN 3 + FLOOR(RAND() * 3) ELSE 0 END
        WHEN 'OBSOLETE' THEN
            CASE WHEN d.day_offset = 6 THEN 60 + FLOOR(RAND() * 31) ELSE 0 END
        WHEN 'FLUCTUATION' THEN
            CASE WHEN d.day_offset IN (6, 3) THEN 1 + FLOOR(RAND() * 2) ELSE 0 END
        WHEN 'RISING' THEN
            CASE WHEN d.day_offset IN (6, 3) THEN 2 + FLOOR(RAND() * 2) ELSE 0 END
        WHEN 'FALLING' THEN
            CASE WHEN d.day_offset IN (6, 3) THEN 2 + FLOOR(RAND() * 2) ELSE 0 END
        ELSE 0
    END AS inbound_qty,
    CASE p.warning_target
        WHEN 'SHORTAGE' THEN 6 + FLOOR(RAND() * 2)
        WHEN 'OBSOLETE' THEN 0
        WHEN 'FLUCTUATION' THEN
            CASE d.day_offset
                WHEN 6 THEN 5 + FLOOR(RAND() * 2)
                WHEN 5 THEN 20 + FLOOR(RAND() * 5)
                WHEN 4 THEN 4 + FLOOR(RAND() * 2)
                WHEN 3 THEN 22 + FLOOR(RAND() * 5)
                WHEN 2 THEN 5 + FLOOR(RAND() * 2)
                WHEN 1 THEN 18 + FLOOR(RAND() * 5)
                ELSE 4 + FLOOR(RAND() * 2)
            END
        WHEN 'RISING' THEN 4 + ((6 - d.day_offset) * 2) + FLOOR(RAND() * 2)
        WHEN 'FALLING' THEN 18 - ((6 - d.day_offset) * 2) + FLOOR(RAND() * 2)
        ELSE 0
    END AS outbound_qty
FROM ai_full_test_parts p
CROSS JOIN ai_full_test_days d;

DROP TEMPORARY TABLE IF EXISTS ai_full_test_stock;
CREATE TEMPORARY TABLE ai_full_test_stock AS
SELECT
    part_index,
    part_code,
    part_name,
    warning_target,
    pattern_name,
    SUM(inbound_qty) AS inbound_total,
    SUM(outbound_qty) AS outbound_total,
    CASE warning_target
        WHEN 'SHORTAGE' THEN 8 + FLOOR(RAND() * 5)
        WHEN 'OBSOLETE' THEN 120 + FLOOR(RAND() * 41)
        WHEN 'FLUCTUATION' THEN 150 + FLOOR(RAND() * 41)
        WHEN 'RISING' THEN 260 + FLOOR(RAND() * 21)
        WHEN 'FALLING' THEN 180 + FLOOR(RAND() * 41)
        ELSE 25 + FLOOR(RAND() * 16)
    END AS current_stock
FROM ai_full_test_daily
GROUP BY part_index, part_code, part_name, warning_target, pattern_name;

INSERT INTO part (
    part_code,
    part_name,
    supplier_code,
    packaging_capacity,
    unit,
    status,
    min_stock,
    max_stock
)
SELECT
    part_code,
    part_name,
    @supplier_code,
    1,
    'pcs',
    1,
    0,
    200
FROM ai_full_test_parts
ON DUPLICATE KEY UPDATE
    part_name = VALUES(part_name),
    supplier_code = VALUES(supplier_code),
    packaging_capacity = VALUES(packaging_capacity),
    unit = VALUES(unit),
    status = VALUES(status),
    min_stock = VALUES(min_stock),
    max_stock = VALUES(max_stock);

INSERT INTO inbound_order (
    order_no,
    inbound_type,
    supplier_code,
    warehouse_code,
    status,
    total_quantity,
    received_quantity,
    total_boxes,
    received_boxes,
    remark,
    created_by,
    created_at,
    updated_at
)
SELECT
    CONCAT('PO-AIFULL-', LPAD(part_index, 3, '0')),
    'purchase',
    @supplier_code,
    @warehouse_code,
    'completed',
    inbound_total,
    inbound_total,
    inbound_total,
    inbound_total,
    CONCAT(@ai_marker, ' inbound order for ', pattern_name),
    @operator,
    DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 6 DAY), INTERVAL 8 HOUR),
    NOW()
FROM ai_full_test_stock;

INSERT INTO inbound_order_detail (
    order_no,
    part_code,
    part_name,
    expected_quantity,
    received_quantity,
    packaging_capacity,
    expected_boxes,
    received_boxes,
    unit
)
SELECT
    CONCAT('PO-AIFULL-', LPAD(part_index, 3, '0')),
    part_code,
    part_name,
    inbound_total,
    inbound_total,
    1,
    inbound_total,
    inbound_total,
    'pcs'
FROM ai_full_test_stock;

INSERT INTO outbound_order (
    order_no,
    outbound_type,
    customer_code,
    customer_name,
    warehouse_code,
    status,
    total_quantity,
    shipped_quantity,
    total_boxes,
    shipped_boxes,
    remark,
    created_by,
    created_at,
    updated_at
)
SELECT
    CONCAT('SO-AIFULL-', LPAD(part_index, 3, '0')),
    'sales',
    'AI-CUSTOMER',
    'AI Full Test Customer',
    @warehouse_code,
    'completed',
    outbound_total,
    outbound_total,
    outbound_total,
    outbound_total,
    CONCAT(@ai_marker, ' outbound order for ', pattern_name),
    @operator,
    DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 6 DAY), INTERVAL 8 HOUR),
    NOW()
FROM ai_full_test_stock;

INSERT INTO outbound_order_detail (
    order_no,
    part_code,
    part_name,
    expected_quantity,
    shipped_quantity,
    packaging_capacity,
    expected_boxes,
    shipped_boxes,
    unit
)
SELECT
    CONCAT('SO-AIFULL-', LPAD(part_index, 3, '0')),
    part_code,
    part_name,
    outbound_total,
    outbound_total,
    1,
    outbound_total,
    outbound_total,
    'pcs'
FROM ai_full_test_stock;

INSERT INTO kanban (
    kanban_no,
    order_no,
    part_code,
    part_name,
    supplier_code,
    warehouse_code,
    quantity,
    box_count,
    qr_code,
    status,
    scan_time,
    scan_by,
    created_at,
    is_sealed,
    original_quantity
)
SELECT
    CONCAT('KAN-', @ai_marker, '-', part_code, '-STOCK'),
    CONCAT('PO-AIFULL-', LPAD(part_index, 3, '0')),
    part_code,
    part_name,
    @supplier_code,
    @warehouse_code,
    current_stock,
    current_stock,
    CONCAT('KAN-', @ai_marker, '-', part_code, '-STOCK'),
    'stored',
    NOW(),
    @operator,
    DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 6 DAY), INTERVAL 10 HOUR),
    0,
    current_stock
FROM ai_full_test_stock;

INSERT INTO current_inventory (
    kanban_no,
    part_code,
    part_name,
    supplier_code,
    quantity,
    warehouse_code,
    status,
    created_at,
    updated_at
)
SELECT
    CONCAT('KAN-', @ai_marker, '-', part_code, '-STOCK'),
    part_code,
    part_name,
    @supplier_code,
    current_stock,
    @warehouse_code,
    'stored',
    DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 6 DAY), INTERVAL 10 HOUR),
    NOW()
FROM ai_full_test_stock;

INSERT INTO inventory_trace (
    trace_no,
    kanban_no,
    order_no,
    part_code,
    part_name,
    supplier_code,
    quantity,
    warehouse_code,
    action_type,
    action_time,
    operator,
    remark
)
SELECT
    CONCAT(@ai_marker, '-', part_code, '-IN-D', day_offset, '-', @run_token),
    CONCAT('KAN-', @ai_marker, '-', part_code, '-STOCK'),
    CONCAT('PO-AIFULL-', LPAD(part_index, 3, '0')),
    part_code,
    part_name,
    @supplier_code,
    inbound_qty,
    @warehouse_code,
    'INBOUND',
    DATE_ADD(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL day_offset DAY), INTERVAL 9 HOUR), INTERVAL FLOOR(RAND() * 45) MINUTE),
    @operator,
    CONCAT(@ai_marker, ' randomized inbound data')
FROM ai_full_test_daily
WHERE inbound_qty > 0;

INSERT INTO inventory_trace (
    trace_no,
    kanban_no,
    order_no,
    part_code,
    part_name,
    supplier_code,
    quantity,
    warehouse_code,
    action_type,
    action_time,
    operator,
    remark
)
SELECT
    CONCAT(@ai_marker, '-', part_code, '-OUT-D', day_offset, '-', @run_token),
    CONCAT('KAN-', @ai_marker, '-', part_code, '-STOCK'),
    CONCAT('SO-AIFULL-', LPAD(part_index, 3, '0')),
    part_code,
    part_name,
    @supplier_code,
    outbound_qty,
    @warehouse_code,
    'OUTBOUND',
    DATE_ADD(DATE_ADD(DATE_SUB(CURDATE(), INTERVAL day_offset DAY), INTERVAL 16 HOUR), INTERVAL FLOOR(RAND() * 45) MINUTE),
    @operator,
    CONCAT(@ai_marker, ' randomized outbound data')
FROM ai_full_test_daily
WHERE outbound_qty > 0;

COMMIT;

SELECT
    part_code,
    warning_target,
    pattern_name,
    SUM(inbound_qty) AS seven_day_inbound,
    SUM(outbound_qty) AS seven_day_outbound
FROM ai_full_test_daily
GROUP BY part_code, warning_target, pattern_name
ORDER BY part_code;

SELECT 'inbound_order' AS table_name, COUNT(*) AS rows_count
FROM inbound_order
WHERE order_no LIKE 'PO-AIFULL-%'
UNION ALL
SELECT 'outbound_order', COUNT(*)
FROM outbound_order
WHERE order_no LIKE 'SO-AIFULL-%'
UNION ALL
SELECT 'inventory_trace', COUNT(*)
FROM inventory_trace
WHERE remark LIKE CONCAT('%', @ai_marker, '%')
UNION ALL
SELECT 'current_inventory', COUNT(*)
FROM current_inventory
WHERE part_code IN ('AITEST-001', 'AITEST-002', 'AITEST-003', 'AITEST-004', 'AITEST-005');
