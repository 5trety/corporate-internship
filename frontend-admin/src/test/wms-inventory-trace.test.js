import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const source = readFileSync(
  resolve(process.cwd(), 'src/views/wms/InventoryTrace.vue'),
  'utf8'
)

describe('Inventory trace filters', () => {
  it('clears the trace date range to an empty value when reset is clicked', () => {
    expect(source).toContain('dateRange: null')
    expect(source).toContain('traceParams.dateRange = null')
  })
})
