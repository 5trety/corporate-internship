# 数据迁移执行指南

## 📋 迁移前准备

### 1. 确认MySQL正在运行

**方法1：使用Docker**
```powershell
# 检查Docker容器
docker ps | Select-String mysql

# 如果没有运行，启动容器
docker start mysql-container
```

**方法2：使用本地MySQL服务**
```powershell
# 检查MySQL服务状态
Get-Service -Name MySQL*

# 如果未运行，启动服务
Start-Service -Name MySQL80  # 或你的MySQL服务名
```

---

## 🚀 执行迁移（3种方法）

### 方法1：使用PowerShell脚本（推荐）

```powershell
# 进入项目目录
cd "d:\1\study\企业实训\5\3"

# 执行迁移脚本
.\scripts\execute-migration.ps1
```

**脚本会自动完成**：
- ✅ 检查MySQL连接
- ✅ 测试数据库连接
- ✅ 备份数据库
- ✅ 执行迁移SQL
- ✅ 验证迁移结果

---

### 方法2：使用MySQL Workbench（图形界面）

1. **打开MySQL Workbench**
2. **连接到本地数据库**（localhost:3306）
3. **打开SQL脚本**：
   - File → Open SQL Script
   - 选择：`backend-demo/src/main/resources/migration-dual-to-single-kanban.sql`
4. **执行脚本**：
   - 点击⚡图标或按 `Ctrl + Shift + Enter`
5. **查看输出**，确认没有错误

---

### 方法3：使用命令行手动执行

```powershell
# 1. 进入SQL脚本目录
cd "d:\1\study\企业实训\5\3\backend-demo\src\main\resources"

# 2. 备份数据库
mysqldump -u root -proot backend_db > backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql

# 3. 执行迁移脚本
mysql -u root -proot backend_db < migration-dual-to-single-kanban.sql

# 4. 验证迁移结果
mysql -u root -proot backend_db -e "SELECT status, COUNT(*) FROM kanban GROUP BY status;"
```

---

## ✅ 验证迁移结果

执行以下SQL检查迁移是否成功：

```sql
-- 1. 检查看板总数
SELECT COUNT(*) as total_kanban FROM kanban;

-- 2. 检查看板状态分布
SELECT status, COUNT(*) as count 
FROM kanban 
GROUP BY status;

-- 3. 检查是否有出库看板（应该为0）
SELECT COUNT(*) as outbound_kanban 
FROM kanban 
WHERE order_no LIKE 'SO-%';

-- 4. 检查追溯记录
SELECT action_type, COUNT(*) as count 
FROM inventory_trace 
GROUP BY action_type;

-- 5. 检查新字段是否添加成功
SHOW COLUMNS FROM kanban LIKE 'is_sealed';
SHOW COLUMNS FROM kanban LIKE 'original_quantity';

-- 6. 检查新表是否创建成功
SHOW TABLES LIKE 'transfer_order';
SHOW TABLES LIKE 'seal_history';
```

---

## ⚠️ 常见问题

### Q1: MySQL命令找不到
**解决方案**：
- 使用MySQL Workbench（方法2）
- 或者添加MySQL到系统PATH

### Q2: 连接失败
**检查清单**：
- [ ] MySQL服务是否运行
- [ ] 端口是否正确（3306）
- [ ] 用户名密码是否正确（root/root）
- [ ] 数据库是否存在（backend_db）

### Q3: 执行过程中断
**解决方案**：
1. 恢复备份：
   ```powershell
   mysql -u root -proot backend_db < backup_xxxx.sql
   ```
2. 重新执行迁移脚本

### Q4: 数据丢失
**解决方案**：
- 从备份恢复：
  ```powershell
  mysql -u root -proot backend_db < backup_xxxx.sql
  ```
- 或联系开发团队

---

## 📊 迁移后检查清单

执行迁移后，请检查：

- [ ] 数据库备份文件已生成
- [ ] 看板表中没有出库看板（order_no LIKE 'SO-%'）
- [ ] 新字段已添加（is_sealed, original_quantity等）
- [ ] 新表已创建（transfer_order, seal_history）
- [ ] 追溯记录已更新（kanban_no统一为入库看板号）
- [ ] 后端服务可以正常启动
- [ ] 前端页面可以正常访问

---

## 🎯 迁移完成后的下一步

1. **重启后端服务**
   ```powershell
   # 停止现有服务
   .\scripts\stop-local.ps1
   
   # 重新启动服务
   .\scripts\start-local.ps1
   ```

2. **检查后端日志**
   - 查看是否有SQL错误
   - 确认数据库连接正常

3. **测试核心功能**
   - [ ] 创建入库单
   - [ ] 生成看板
   - [ ] 扫码入库
   - [ ] 扫码出库
   - [ ] 查看追溯记录

4. **如果发现问题**
   - 查看后端日志
   - 检查数据库数据
   - 必要时恢复备份

---

## 📞 需要帮助？

如果迁移过程中遇到任何问题：

1. **查看错误日志**
2. **截图错误信息**
3. **联系开发团队**

**祝迁移顺利！** 🎉
