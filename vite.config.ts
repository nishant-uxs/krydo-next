import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

export default defineConfig({
  plugins: [react()],
  // @stellar/stellar-sdk expects a Node-style `global`; map it to globalThis in
  // the browser build. `Buffer` is polyfilled at runtime in client/src/lib/polyfills.ts.
  define: {
    global: "globalThis",
  },
  resolve: {
    alias: {
      "@": path.resolve(import.meta.dirname, "client", "src"),
      "@shared": path.resolve(import.meta.dirname, "shared"),
      buffer: "buffer",
      // @reown/appkit-adapter-wagmi may pull @wagmi/connectors@8 which imports
      // `@wagmi/core/tempo` (wagmi v3). Stub so Vite can build with wagmi v2.
      "@wagmi/core/tempo": path.resolve(
        import.meta.dirname,
        "client",
        "src",
        "lib",
        "wagmi-tempo-stub.ts",
      ),
    },
  },
  optimizeDeps: {
    include: [
      "@creit.tech/stellar-wallets-kit",
      "@creit.tech/stellar-wallets-kit/modules/utils",
      "@creit.tech/stellar-wallets-kit/modules/wallet-connect",
      "@stellar/freighter-api",
      "buffer",
    ],
    esbuildOptions: {
      define: { global: "globalThis" },
    },
  },
  root: path.resolve(import.meta.dirname, "client"),
  build: {
    outDir: path.resolve(import.meta.dirname, "dist/public"),
    emptyOutDir: true,
  },
  server: {
    fs: {
      strict: true,
      deny: ["**/.*"],
    },
    proxy: {
      "/api": {
        target: process.env.VITE_DEV_API_PROXY || "http://localhost:5000",
        changeOrigin: true,
      },
      "/healthz": {
        target: process.env.VITE_DEV_API_PROXY || "http://localhost:5000",
        changeOrigin: true,
      },
    },
  },
});
