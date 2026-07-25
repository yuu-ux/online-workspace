import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  cacheDir: "/tmp/vite-cache",
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": "http://backend:8080",
      "/ws": {
        target: "http://backend:8080",
        ws: true
      }
    }
  }
});
