import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": "http://backend:8080",
      "/login": "http://backend:8080",
      "/logout": "http://backend:8080",
      "/ws": {
        target: "http://backend:8080",
        ws: true
      }
    }
  }
});
