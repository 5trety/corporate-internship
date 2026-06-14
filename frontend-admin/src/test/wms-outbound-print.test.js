import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const readOutboundListView = () =>
  readFileSync(resolve(process.cwd(), 'src/views/wms/OutboundOrderList.vue'), 'utf8')

describe('WMS outbound kanban printing', () => {
  it('prints only remaining outbound details', () => {
    const source = readOutboundListView()

    expect(source).toContain('remainingQuantity')
    expect(source).toContain('filter(item => item.remainingQuantity > 0)')
    expect(source).toContain('expectedQuantity: item.remainingQuantity')
  })
})
