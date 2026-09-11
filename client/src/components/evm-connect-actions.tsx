import { Button } from "@/components/ui/button";
import { useEvmSiwe } from "@/lib/evm-auth";
import { shortenAddress } from "@/lib/wallet";
import { Wallet } from "lucide-react";

/** Only mount under WagmiProvider (EvmProviders when Reown is configured). */
export function EvmConnectActions({ onAuthenticated }: { onAuthenticated?: () => void }) {
  const evm = useEvmSiwe();

  return (
    <>
      <Button
        variant="outline"
        className="w-full justify-start gap-3 rounded-full h-12 border-[#2563EB]/40"
        disabled={evm.busy}
        onClick={() => evm.openModal()}
        data-testid="button-connect-evm"
      >
        <Wallet className="w-5 h-5 text-[#38BDF8]" />
        Connect EVM (AppKit)
      </Button>
      {evm.evmConnected && evm.evmAddress && (
        <Button
          className="w-full rounded-full bg-[#2563EB] hover:bg-[#1D4ED8]"
          disabled={evm.busy}
          onClick={async () => {
            const ok = await evm.runSiwe();
            if (ok) {
              onAuthenticated?.();
              window.location.reload();
            }
          }}
        >
          {evm.busy ? "SIWE…" : `Sign in as ${shortenAddress(evm.evmAddress)}`}
        </Button>
      )}
    </>
  );
}
