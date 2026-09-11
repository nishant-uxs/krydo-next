import { type ReactNode } from "react";
import { WagmiProvider } from "wagmi";
import { ensureReownAppKit, reownConfigured, wagmiAdapter } from "./reown";

/** Wraps children with Wagmi when Reown is configured; otherwise passthrough. */
export function EvmProviders({ children }: { children: ReactNode }) {
  if (!reownConfigured || !wagmiAdapter) {
    return <>{children}</>;
  }
  ensureReownAppKit();
  return <WagmiProvider config={wagmiAdapter.wagmiConfig}>{children}</WagmiProvider>;
}
