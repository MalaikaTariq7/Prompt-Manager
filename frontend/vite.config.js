import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const stripOriginHeader = (proxy) => {
  proxy.on('proxyReq', (proxyReq) => {
    proxyReq.removeHeader('origin')
  })
}

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 3000,
    strictPort: true,
    allowedHosts: true,
    proxy: {
      '/api/prompts': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        configure: stripOriginHeader,
      },
      '/api/reviews': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        configure: stripOriginHeader,
      },
    }
  }
})