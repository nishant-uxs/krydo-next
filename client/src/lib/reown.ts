/**
 * Reown AppKit (EVM) bootstrap for Krydo web.
 */
import { createAppKit } from "@reown/appkit/react";
import { WagmiAdapter } from "@reown/appkit-adapter-wagmi";
import {
  mainnet,
  polygon,
  base,
  arbitrum,
  optimism,
  bsc,
  avalanche,
} from "@reown/appkit/networks";
import type { AppKitNetwork } from "@reown/appkit/networks";

const projectId = (import.meta.env.VITE_REOWN_PROJECT_ID as string | undefined)?.trim();

export const evmNetworks = [
  mainnet,
  polygon,
  base,
  arbitrum,
  optimism,
  bsc,
  avalanche,
] as [AppKitNetwork, ...AppKitNetwork[]];

export const reownConfigured = Boolean(projectId && projectId.length > 8);

export const wagmiAdapter = reownConfigured
  ? new WagmiAdapter({
      projectId: projectId!,
      networks: evmNetworks,
    })
  : null;

let initialized = false;
let appKit: ReturnType<typeof createAppKit> | null = null;

export function ensureReownAppKit(): boolean {
  if (!reownConfigured || !wagmiAdapter) return false;
  if (initialized) return true;
  appKit = createAppKit({
    adapters: [wagmiAdapter],
    networks: evmNetworks,
    projectId: projectId!,
    metadata: {
      name: "Krydo",
      description: "Privacy-preserving credentials — Stellar + EVM",
      url: typeof window !== "undefined" ? window.location.origin : "https://krydo.onrender.com",
      icons: ["https://krydo.onrender.com/favicon.ico"],
    },
    features: { analytics: false },
    themeMode: "dark",
    themeVariables: {
      "--w3m-accent": "#2563EB",
    },
  });
  initialized = true;
  return true;
}

/** Open Reown AppKit connect modal (EVM). No-op when project id is missing. */
export function openEvmConnect(): boolean {
  if (!ensureReownAppKit() || !appKit) return false;
  void appKit.open({ view: "Connect" });
  return true;
}
