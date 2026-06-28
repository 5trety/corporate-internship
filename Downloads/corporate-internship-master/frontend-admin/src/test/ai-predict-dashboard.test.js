import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const readDashboardView = () =>
  readFileSync(resolve(process.cwd(), 'src/views/wms/AIPredictDashboard.vue'), 'utf8')

describe('AI predict dashboard trend chart', () => {
  it('only shows the outbound demand forecast in the trend chart', () => {
    const source = readDashboardView()

    expect(source).toContain('AI需求预测（出库量）')
    expect(source).toContain("const predictionLabel = 'AI需求预测（出库量）'")
    expect(source).toContain('const { historical = [], predicted = [] } = data')
    expect(source).not.toContain('predictionModeOptions')
    expect(source).not.toContain('AI入库预测（入库量）')
    expect(source).not.toContain('AI总流量预测（入库量+出库量）')
    expect(source).not.toContain('data.predictions?.[predictionMode.value]')
  })

  it('keeps inventory as a reference line without a separate right-side stock axis', () => {
    const source = readDashboardView()

    expect(source).toContain("name: '当前库存'")
    expect(source).toContain('data: [...histInventory, ...new Array(predDates.length).fill(null)]')
    expect(source).not.toContain("position: 'right'")
    expect(source).not.toContain("name: '当前库存',\n        position: 'right'")
    expect(source).not.toContain('yAxisIndex: 1')
  })
})
