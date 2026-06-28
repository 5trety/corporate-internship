-- ============================================
-- WMS系统数据迁移脚本（双看板→单看板）
-- 执行前请务必备份数据库！
-- ============================================

-- 1. 备份现有数据
CREATE TABLE kanban_backup AS SELECT * FROM kanban;
CREATE TABLE inventory_trace_backup AS SELECT * FROM inventory_trace;
CREATE TABLE current_inventory_backup AS SELECT * FROM current_inventory;

-- 2. 添加新字段到kanban表
ALTER TABLE kanban 
ADD COLUMN is_sealed TINYINT DEFAULT 0 COMMENT '是否封存(0-未封存,1-已封存)',
ADD COLUMN original_quantity INT COMMENT '原始数量(转包前数量)',
ADD COLUMN transfer_type VARCHAR(20) COMMENT '转包类型(split-拆分,merge-合并,NULL-非转包)',
ADD COLUMN parent_kanban_no VARCHAR(100) COMMENT '父看板号(转包产生)';

-- 3. 添加转包表
CREATE TABLE IF NOT EXISTS transfer_order (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) UNIQUE NOT NULL COMMENT '转包单号(TO-yyyyMMdd-0001)',
    from_kanban_no VARCHAR(100) NOT NULL COMMENT '源看板号',
    to_kanban_nos TEXT COMMENT '目标看板号列表(JSON数组)',
    transfer_type VARCHAR(20) NOT NULL COMMENT '转包类型(split-拆分,merge-合并)',
    original_quantity INT NOT NULL COMMENT '原始数量',
    new_quantities TEXT NOT NULL COMMENT '新数量列表(JSON数组)',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态(pending-待执行,completed-已完成)',
    operator VARCHAR(50) COMMENT '操作人',
    remark TEXT COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_from_kanban (from_kanban_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转包单表';

-- 4. 添加封存历史表
CREATE TABLE IF NOT EXISTS seal_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    kanban_no VARCHAR(100) NOT NULL COMMENT '看板号',
    action VARCHAR(20) NOT NULL COMMENT '操作(seal-封存,unseal-解封)',
    operator VARCHAR(50) COMMENT '操作人',
    remark TEXT COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_kanban_no (kanban_no),
    INDEX idx_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='封存历史表';

-- 5. 零件表添加高低储字段
ALTER TABLE part 
ADD COLUMN min_stock INT DEFAULT 0 COMMENT '最低库存(低于此值预警)',
ADD COLUMN max_stock INT DEFAULT 0 COMMENT '最高库存(高于此值预警)';

-- ============================================
-- 数据迁移逻辑（双看板→单看板）
-- ============================================

-- 6. 处理出库看板：将出库看板的状态和订单信息更新到对应的入库看板
-- 逻辑：出库看板关联的入库看板，其状态应该更新为outbound
UPDATE kanban k_inbound
INNER JOIN kanban k_outbound ON k_inbound.part_code = k_outbound.part_code 
    AND k_outbound.order_no LIKE 'SO-%'
    AND k_outbound.status = 'outbound'
SET k_inbound.status = 'outbound'
WHERE k_inbound.order_no LIKE 'PO-%' 
    AND k_inbound.status = 'stored';

-- 7. 删除所有出库看板（order_no以SO-开头的）
-- 注意：这会删除出库看板记录，但保留入库看板
DELETE FROM kanban WHERE order_no LIKE 'SO-%';

-- 8. 更新inventory_trace表：统一kanban_no为入库看板号
-- 将出库记录的kanban_no从出库看板号改为入库看板号
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

-- 9. 清空remark字段（单看板策略不需要remark关联）
UPDATE inventory_trace SET remark = NULL WHERE remark IS NOT NULL;

-- ============================================
-- 验证数据迁移结果
-- ============================================

-- 10. 检查迁移后的看板数量
SELECT 
    '迁移前看板总数' as description,
    COUNT(*) as count
FROM kanban_backup
UNION ALL
SELECT 
    '迁移后看板总数',
    COUNT(*)
FROM kanban
UNION ALL
SELECT 
    '删除的出库看板数',
    COUNT(*)
FROM kanban_backup
WHERE order_no LIKE 'SO-%';

-- 11. 检查看板状态分布
SELECT 
    status,
    COUNT(*) as count
FROM kanban
GROUP BY status;

-- 12. 检查追溯记录
SELECT 
    action_type,
    COUNT(*) as count
FROM inventory_trace
GROUP BY action_type;

-- ============================================
-- 注意事项
-- ============================================
-- 1. 执行前务必备份数据库
-- 2. 建议在测试环境先执行验证
-- 3. 迁移后需要测试完整业务流程
-- 4. 如果有Android手持APP，需要同步更新
-- 5. 迁移完成后，检查前端页面是否正常显示
