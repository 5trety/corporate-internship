-- ============================================
-- WMS数据迁移 - 剩余执行步骤
-- 请在MySQL客户端中执行以下SQL
-- ============================================

USE backend_db;

-- 步骤1：删除出库看板（order_no以SO-开头的）
DELETE FROM kanban WHERE order_no LIKE 'SO-%';

-- 步骤2：更新inventory_trace表，统一kanban_no为入库看板号
UPDATE inventory_trace t
INNER JOIN (
    SELECT 
        it.id,
        SUBSTRING_INDEX(it.remark, '入库看板:', -1) as inbound_kanban_no
    FROM inventory_trace it
    WHERE it.action_type = 'OUTBOUND' 
        AND it.remark LIKE '%入库看板:%'
) sub ON t.id = sub.id
SET t.kanban_no = sub.inbound_kanban_no
WHERE t.action_type = 'OUTBOUND';

-- 步骤3：清空remark字段
UPDATE inventory_trace SET remark = NULL WHERE remark IS NOT NULL;

-- ============================================
-- 验证迁移结果
-- ============================================

-- 检查看板总数
SELECT '迁移后看板总数' as description, COUNT(*) as count FROM kanban;

-- 检查看板状态分布
SELECT status, COUNT(*) as count FROM kanban GROUP BY status;

-- 检查是否还有出库看板（应该为0）
SELECT '出库看板数量' as description, COUNT(*) as count FROM kanban WHERE order_no LIKE 'SO-%';

-- 检查追溯记录
SELECT action_type, COUNT(*) as count FROM inventory_trace GROUP BY action_type;

-- 检查新表
SHOW TABLES LIKE 'transfer_order';
SHOW TABLES LIKE 'seal_history';

-- 检查新字段
SHOW COLUMNS FROM kanban LIKE 'is_sealed';
SHOW COLUMNS FROM part LIKE 'min_stock';
