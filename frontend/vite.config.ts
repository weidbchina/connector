import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  base: '/accounting-tool/',
  plugins: [react()],
  server: {
    proxy: {
      '/accounting-tool/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})
