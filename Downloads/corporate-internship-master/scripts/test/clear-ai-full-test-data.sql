-- Clear complete AI/WMS test data.
-- Safe scope: only AI-FULL-TEST rows and isolated AITEST-* parts are removed.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @ai_marker = 'AI-FULL-TEST';

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

DELETE FROM inbound_order
WHERE order_no LIKE 'PO-AIFULL-%'
   OR remark LIKE CONCAT('%', @ai_marker, '%');

DELETE FROM outbound_order_detail
WHERE order_no LIKE 'SO-AIFULL-%'
   OR part_code IN ('AITEST-001', 'AITEST-002', 'AITEST-003', 'AITEST-004', 'AITEST-005');

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

COMMIT;

SELECT 'inventory_trace' AS table_name, COUNT(*) AS remaining_rows
FROM inventory_trace
WHERE order_no LIKE 'PO-AIFULL-%'
   OR order_no LIKE 'SO-AIFULL-%'
   OR trace_no LIKE CONCAT(@ai_marker, '-%')
   OR remark LIKE CONCAT('%', @ai_marker, '%')
   OR part_code IN ('AITEST-001', 'AITEST-002', 'AITEST-003', 'AITEST-004', 'AITEST-005')
UNION ALL
SELECT 'current_inventory', COUNT(*)
FROM current_inventory
WHERE kanban_no LIKE CONCAT('KAN-', @ai_marker, '-%')
   OR part_code IN ('AITEST-001', 'AITEST-002', 'AITEST-003', 'AITEST-004', 'AITEST-005')
UNION ALL
SELECT 'kanban', COUNT(*)
FROM kanban
WHERE kanban_no LIKE CONCAT('KAN-', @ai_marker, '-%')
   OR order_no LIKE 'PO-AIFULL-%'
   OR order_no LIKE 'SO-AIFULL-%'
   OR part_code IN ('AITEST-001', 'AITEST-002', 'AITEST-003', 'AITEST-004', 'AITEST-005')
UNION ALL
SELECT 'inbound_order_detail', COUNT(*)
FROM inbound_order_detail
WHERE order_no LIKE 'PO-AIFULL-%'
   OR part_code IN ('AITEST-001', 'AITEST-002', 'AITEST-003', 'AITEST-004', 'AITEST-005')
UNION ALL
SELECT 'inbound_order', COUNT(*)
FROM inbound_order
WHERE order_no LIKE 'PO-AIFULL-%'
   OR remark LIKE CONCAT('%', @ai_marker, '%')
UNION ALL
SELECT 'outbound_order_detail', COUNT(*)
FROM outbound_order_detail
WHERE order_no LIKE 'SO-AIFULL-%'
   OR part_code IN ('AITEST-001', 'AITEST-002', 'AITEST-003', 'AITEST-004', 'AITEST-005')
UNION ALL
SELECT 'outbound_order', COUNT(*)
FROM outbound_order
WHERE order_no LIKE 'SO-AIFULL-%'
   OR remark LIKE CONCAT('%', @ai_marker, '%')
UNION ALL
SELECT 'part', COUNT(*)
FROM part
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
