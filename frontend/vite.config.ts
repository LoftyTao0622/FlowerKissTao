import { fileURLToPath, URL } from 'node:url'

import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '')
  const proxy = Object.fromEntries(['/api', '/uploads'].map((path) => [path, {
    target: env.VITE_API_PROXY_TARGET || 'http://127.0.0.1:8099',
    changeOrigin: true,
  }]))

  return {
    base: env.VITE_PUBLIC_BASE || '/',
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      host: '127.0.0.1',
      port: 5173,
      proxy,
    },
    preview: {
      host: '127.0.0.1',
      port: 4173,
      proxy,
    },
  }
})
