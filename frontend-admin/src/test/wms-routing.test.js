import { beforeAll, describe, expect, it, vi } from 'vitest'

describe('WMS routing', () => {
  let router

  beforeAll(async () => {
    vi.stubGlobal('location', {
      protocol: 'https:',
      host: 'localhost:5174',
      pathname: '/',
      search: '',
      hash: ''
    })
    vi.stubGlobal('history', {
      state: null,
      pushState: vi.fn(),
      replaceState: vi.fn(),
      go: vi.fn()
    })
    vi.stubGlobal('window', {
      location: globalThis.location,
      history: globalThis.history,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    })
    vi.stubGlobal('document', {
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      querySelector: vi.fn(() => null),
      documentElement: { style: {} },
      createElement: vi.fn(() => ({
        innerHTML: '',
        content: { firstChild: null },
        setAttribute: vi.fn(),
        removeAttribute: vi.fn()
      }))
    })

    router = (await import('../router')).default
  })

  it('exposes inventory tracing as an independent module', () => {
    const traceRoute = router.getRoutes().find((route) => route.name === 'inventory-trace')

    expect(traceRoute?.path).toBe('/inventory-trace')
  })

  it('keeps outbound management under the outbound module', () => {
    const outboundCreateRoute = router
      .getRoutes()
      .find((route) => route.name === 'outbound-order-create')

    expect(outboundCreateRoute?.path).toBe('/wms-outbound/outbound-order/create')
  })

  it('does not expose outbound history as an outbound management route', () => {
    const outboundHistoryRoute = router
      .getRoutes()
      .find((route) => route.name === 'outbound-history')

    expect(outboundHistoryRoute).toBeUndefined()
  })
})
