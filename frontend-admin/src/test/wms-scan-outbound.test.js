import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const readScanOutboundView = () =>
  readFileSync(resolve(process.cwd(), 'src/views/wms/ScanOutbound.vue'), 'utf8')

describe('WMS scan outbound', () => {
  it('submits the scanned kanban number when confirming outbound', () => {
    const source = readScanOutboundView()

    expect(source).toContain('kanbanNo: kanbanInfo.value.kanbanNo')
  })

  it('passes the selected outbound order number when validating a kanban', () => {
    const source = readScanOutboundView()

    expect(source).toContain('validateOutboundKanban(scanCode.value, orderNo.value)')
  })
})
