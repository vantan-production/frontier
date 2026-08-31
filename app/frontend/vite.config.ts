import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    // フロントから fetch('/api/employees') と書くと、Vite が backend に転送する。
    // ブラウザから見ると常に同一オリジンなので CORS が発生しない。
    // 転送先はホスト側ポート(8081)ではなく、Docker ネットワーク内の
    // サービス名とコンテナ内ポートを指す。
    proxy: {
      '/api': {
        target: 'http://backend:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
