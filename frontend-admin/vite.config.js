import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import basicSsl from '@vitejs/plugin-basic-ssl'

export default defineConfig({
  plugins: [vue(), basicSsl()],
  server: {
    host: '0.0.0.0',  // 允许局域网访问
    port: 5173,
    https: true,  // 启用 HTTPS
    proxy: {
      '/api': {
        target: process.env.VITE_API_PROXY_TARGET || 'http://localhost:8081',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            const cookies = proxyRes.headers['set-cookie']
            if (!cookies) return

            proxyRes.headers['set-cookie'] = cookies.map((cookie) =>
              cookie.replace(/;\s*SameSite=None/gi, '; SameSite=Lax')
            )
          })
        }
      }
    }
  }
})
