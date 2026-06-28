# WMS数据迁移执行脚本
# 执行前请确保MySQL正在运行

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "WMS数据迁移脚本（双看板→单看板）" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 数据库配置
$DB_HOST = "localhost"
$DB_PORT = "3306"
$DB_NAME = "backend_db"
$DB_USER = "root"
$DB_PASS = "root"

# MySQL路径（根据你的安装路径修改）
$MYSQL_PATH = "mysql"
$MYSQLDUMP_PATH = "mysqldump"

# 检查MySQL是否可用
Write-Host "[1/5] 检查MySQL连接..." -ForegroundColor Yellow
try {
    & $MYSQL_PATH --version 2>&1 | Out-Null
    Write-Host "✓ MySQL客户端已安装" -ForegroundColor Green
} catch {
    Write-Host "✗ 错误：MySQL客户端未安装或不在PATH中" -ForegroundColor Red
    Write-Host ""
    Write-Host "请使用以下方法之一：" -ForegroundColor Yellow
    Write-Host "1. 安装MySQL Command Line Client" -ForegroundColor White
    Write-Host "2. 使用MySQL Workbench手动执行SQL脚本" -ForegroundColor White
    Write-Host "3. 修改此脚本中的MYSQL_PATH变量" -ForegroundColor White
    Write-Host ""
    Write-Host "SQL脚本位置：" -ForegroundColor Yellow
    Write-Host "$PSScriptRoot\..\backend-demo\src\main\resources\migration-dual-to-single-kanban.sql" -ForegroundColor White
    exit 1
}

# 测试数据库连接
Write-Host ""
Write-Host "[2/5] 测试数据库连接..." -ForegroundColor Yellow
try {
    $testResult = & $MYSQL_PATH -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS -e "SELECT 1;" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ 数据库连接成功" -ForegroundColor Green
    } else {
        throw "连接失败"
    }
} catch {
    Write-Host "✗ 错误：无法连接到数据库" -ForegroundColor Red
    Write-Host "请检查：" -ForegroundColor Yellow
    Write-Host "  - MySQL服务是否运行" -ForegroundColor White
    Write-Host "  - 数据库用户名和密码是否正确" -ForegroundColor White
    Write-Host "  - 数据库 $DB_NAME 是否存在" -ForegroundColor White
    exit 1
}

# 备份数据库
Write-Host ""
Write-Host "[3/5] 备份数据库..." -ForegroundColor Yellow
$backupFile = "$PSScriptRoot\wms_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql"
try {
    & $MYSQLDUMP_PATH -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS $DB_NAME > $backupFile 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ 数据库备份成功: $backupFile" -ForegroundColor Green
    } else {
        throw "备份失败"
    }
} catch {
    Write-Host "✗ 警告：数据库备份失败" -ForegroundColor Red
    Write-Host "  建议手动备份后再继续" -ForegroundColor Yellow
    $continue = Read-Host "是否继续执行迁移？(y/n)"
    if ($continue -ne "y") {
        Write-Host "迁移已取消" -ForegroundColor Yellow
        exit 0
    }
}

# 执行迁移脚本
Write-Host ""
Write-Host "[4/5] 执行数据迁移..." -ForegroundColor Yellow
$sqlFile = "$PSScriptRoot\..\backend-demo\src\main\resources\migration-dual-to-single-kanban.sql"

if (-not (Test-Path $sqlFile)) {
    Write-Host "✗ 错误：找不到SQL脚本文件" -ForegroundColor Red
    Write-Host "  路径: $sqlFile" -ForegroundColor White
    exit 1
}

try {
    $migrationOutput = & $MYSQL_PATH -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS $DB_NAME < $sqlFile 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ 数据迁移执行成功" -ForegroundColor Green
    } else {
        Write-Host "✗ 错误：数据迁移执行失败" -ForegroundColor Red
        Write-Host $migrationOutput -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "✗ 错误：数据迁移执行异常" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

# 验证迁移结果
Write-Host ""
Write-Host "[5/5] 验证迁移结果..." -ForegroundColor Yellow
try {
    # 检查看板数量
    $kanbanCount = & $MYSQL_PATH -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS -N -e "SELECT COUNT(*) FROM kanban;" $DB_NAME 2>&1
    Write-Host "  迁移后看板总数: $kanbanCount" -ForegroundColor White
    
    # 检查看板状态分布
    Write-Host ""
    Write-Host "  看板状态分布：" -ForegroundColor White
    $statusDist = & $MYSQL_PATH -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS -e "SELECT status, COUNT(*) as count FROM kanban GROUP BY status;" $DB_NAME 2>&1
    Write-Host $statusDist -ForegroundColor White
    
    # 检查追溯记录
    Write-Host ""
    Write-Host "  追溯记录分布：" -ForegroundColor White
    $traceDist = & $MYSQL_PATH -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS -e "SELECT action_type, COUNT(*) as count FROM inventory_trace GROUP BY action_type;" $DB_NAME 2>&1
    Write-Host $traceDist -ForegroundColor White
    
    Write-Host ""
    Write-Host "✓ 迁移验证完成" -ForegroundColor Green
} catch {
    Write-Host "⚠ 验证失败，但不影响迁移结果" -ForegroundColor Yellow
    Write-Host "  请手动检查数据库" -ForegroundColor White
}

# 完成
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "数据迁移完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "下一步：" -ForegroundColor Yellow
Write-Host "1. 重启后端服务" -ForegroundColor White
Write-Host "2. 检查后端日志是否有错误" -ForegroundColor White
Write-Host "3. 测试入库流程" -ForegroundColor White
Write-Host "4. 测试出库流程" -ForegroundColor White
Write-Host "5. 测试追溯功能" -ForegroundColor White
Write-Host ""
Write-Host "备份文件位置：" -ForegroundColor Yellow
Write-Host "$backupFile" -ForegroundColor White
Write-Host ""
