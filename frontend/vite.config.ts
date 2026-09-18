import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    // Portul și ținta se pot muta din mediu, ca o sesiune care lucrează lângă alta să-și poată
    // porni stiva ei (`E2E_PORT=5199 E2E_API=http://localhost:8099 npm run dev`) fără să atingă
    // fișierul — până pe 18.09.2026 se edita de mână, și o dată a și rămas editat.
    port: Number(process.env.E2E_PORT ?? 5173),
    proxy: {
      // Dev convenience: proxy API calls to the Spring Boot backend.
      "/api": process.env.E2E_API ?? "http://localhost:8080",
    },
  },
});
