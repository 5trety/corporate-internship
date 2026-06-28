import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const readSource = (path) => readFileSync(resolve(process.cwd(), path), 'utf8')

describe('AI prediction proxy configuration', () => {
  it('uses a same-origin relative path for AI prediction APIs', () => {
    const aiApiSource = readSource('src/api/aiPredict.js')
    const httpSource = readSource('src/api/http.js')

    expect(httpSource).toContain('export const createHttpClient')
    expect(aiApiSource).toContain("createHttpClient('/ai-api')")
    expect(aiApiSource).not.toContain('localhost:8001')
    expect(aiApiSource).not.toContain('http://')
    expect(aiApiSource).not.toContain('https://')
  })

  it('proxies /ai-api requests to the local FastAPI service in development', () => {
    const viteConfigSource = readSource('vite.config.js')

    expect(viteConfigSource).toContain("'/ai-api'")
    expect(viteConfigSource).toContain("target: 'http://localhost:8001'")
    expect(viteConfigSource).toContain("rewrite: (path) => path.replace(/^\\/ai-api/, '')")
  })
})
