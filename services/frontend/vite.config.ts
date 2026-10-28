import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    port: 5173,
    proxy: {
      '/api/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/auth/, '/auth'),
      },
      '/api/accounts': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/accounts/, '/accounts'),
      },
      '/api/transactions': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/transactions/, '/transactions'),
      },
      '/api/ai': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
      '/api/notifications': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
    },
  },
})
