import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const readView = (fileName) =>
  readFileSync(resolve(process.cwd(), 'src/views/wms', fileName), 'utf8')

describe('WMS part selectors', () => {
  it('uses local dropdown options for inbound order details', () => {
    const source = readView('InboundOrderForm.vue')

    expect(source).toContain('v-model="row.partCode"')
    expect(source).toContain('filterable')
    expect(source).not.toContain(':remote-method="searchParts"')
    expect(source).not.toContain(' remote ')
  })

  it('uses local dropdown options for outbound order details', () => {
    const source = readView('OutboundOrderForm.vue')

    expect(source).toContain('v-model="row.partCode"')
    expect(source).toContain('filterable')
    expect(source).not.toContain(':remote-method="searchParts"')
    expect(source).not.toContain(' remote ')
  })
})
