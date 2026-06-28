# AI 需求预测测试数据脚本说明

本文档说明 `seed-ai-full-test-data.sql` 和 `clear-ai-full-test-data.sql` 的用途、执行顺序、三类预警覆盖方式，以及 AI 需求预测趋势线的测试用例。

## 测试数据覆盖目标

`seed-ai-full-test-data.sql` 会生成五组隔离测试零件，零件号固定，数据每次执行都会在安全范围内随机变化：

| 零件号 | 零件名称 | 覆盖目标 | 数据特征 |
| --- | --- | --- | --- |
| `AITEST-001` | `AI Full Test Shortage` | 缺货预警 | 当前库存较低，近 7 天持续出库 |
| `AITEST-002` | `AI Full Test Obsolete` | 呆滞预警 | 当前库存较高，近 7 天无出库 |
| `AITEST-003` | `AI Full Test Fluctuation` | 波动预警 + 起伏预测 | 当前库存充足，近 7 天出库量低高交替、有起有伏 |
| `AITEST-004` | `AI Full Test Rising Demand` | 上升预测 | 近 7 天出库量持续上升，用于观察红色 AI 需求预测线继续上升 |
| `AITEST-005` | `AI Full Test Falling Demand` | 下降预测 | 近 7 天出库量持续下降，用于观察红色 AI 需求预测线继续下降 |

这些数据会写入入库单、出库单、看板、实时库存和库存追溯相关表，因此可以在入库单列表、出库单列表、库存追溯、实时库存总览和 AI 需求预测中联动查看。

## 推荐执行顺序

在 CMD 中执行：

```cmd
cd /d D:\code\corporate-internship-master\corporate-internship
"mysql所在的文件夹\mysql.exe" --default-character-set=utf8mb4 -uroot -proot backend_db < scripts\clear-ai-full-test-data.sql
"mysql所在的文件夹\mysql.exe" --default-character-set=utf8mb4 -uroot -proot backend_db < scripts\seed-ai-full-test-data.sql
```

执行完成后刷新前端页面，进入 `AI需求预测`，理论上三张预警卡应分别出现：

- 缺货预警：至少包含 `AITEST-001`
- 呆滞预警：至少包含 `AITEST-002`
- 波动预警：至少包含 `AITEST-003`

趋势弹窗中可重点查看：

- `AITEST-003`：红色 `AI需求预测（出库量）` 线应呈现有起有伏的起伏趋势。
- `AITEST-004`：红色 `AI需求预测（出库量）` 线应呈现上升趋势。
- `AITEST-005`：红色 `AI需求预测（出库量）` 线应呈现下降趋势。

## 只清理 AI full 测试数据

如果只想删除这五组 AI full 测试用例：

```cmd
cd /d D:\code\corporate-internship-master\corporate-internship
"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" --default-character-set=utf8mb4 -uroot -proot backend_db < scripts\clear-ai-full-test-data.sql
```

清理范围只包含：

- `AI-FULL-TEST` 标记的数据
- `PO-AIFULL-%` 入库单
- `SO-AIFULL-%` 出库单
- `AITEST-001` 到 `AITEST-005` 五个测试零件及其库存、看板、追溯记录

## 注意事项

- 这两个脚本使用 `SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;`，用于避免 MySQL 8.4 下的排序规则混用问题。
- 执行 seed 前会自动清理旧的 `AI-FULL-TEST` 数据，所以可以重复执行。
- 每次 seed 的具体数量会随机变化，但会被限制在能稳定触发对应预警或趋势形态的范围内。
