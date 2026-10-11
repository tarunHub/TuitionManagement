import path from 'node:path'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': path.resolve(__dirname, 'src') },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          data: ['@tanstack/react-query', '@tanstack/react-table', 'axios', 'zod', 'react-hook-form', 'date-fns'],
          charts: ['recharts'],
        },
      },
    },
  },
  server: {
    host: true,
    port: 5173,
    strictPort: true,
    // Local alias (mapped to 127.0.0.1 in /etc/hosts); Vite rejects unknown Host headers otherwise.
    allowedHosts: ['smartstart.com'],
    proxy: {
      // Same-origin API calls in dev; the backend also allows this origin via CORS.
      '/api': { target: process.env.API_PROXY_TARGET ?? 'http://localhost:8080', changeOrigin: true },
    },
  },
})
