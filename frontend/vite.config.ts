import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Toda requisição que começa com /produtos é repassada ao backend Spring.
      // Assim o navegador só conversa com o Vite (mesma origem) e não há bloqueio de CORS.
      '/produtos': 'http://localhost:8080',
    },
  },
})
