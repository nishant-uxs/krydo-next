import { useEffect, useRef, useState } from "react";
import { useWallet } from "@/lib/wallet";
import { reownConfigured } from "@/lib/reown";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { ChevronRight } from "lucide-react";
import { SiStellar } from "react-icons/si";
import { EvmConnectActions } from "@/components/evm-connect-actions";
import { isConnected as freighterIsConnected } from "@stellar/freighter-api";

type StellarOption = {
  id: string;
  name: string;
  hint: string;
  needsWc?: boolean;
};

const STELLAR_OPTIONS: StellarOption[] = [
  { id: "freighter", name: "Freighter", hint: "Extension → connection popup" },
  { id: "xbull", name: "xBull", hint: "Extension / PWA" },
  { id: "lobstr", name: "LOBSTR", hint: "Extension" },
  { id: "rabet", name: "Rabet", hint: "Extension" },
  {
    id: "wallet_connect",
    name: "Freighter Mobile / WalletConnect",
    hint: "QR or deep-link popup in Freighter app",
    needsWc: true,
  },
];

/** Shared multi-wallet picker (Stellar kit + EVM AppKit). */
export function ConnectWalletDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const { isConnecting, connect, connectStellarWallet } = useWallet();
  const [freighterReady, setFreighterReady] = useState<boolean | null>(null);
  const autoStarted = useRef(false);

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    (async () => {
      try {
        const res = await freighterIsConnected();
        if (!cancelled) setFreighterReady(!res.error && !!res.isConnected);
      } catch {
        if (!cancelled) setFreighterReady(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [open]);

  // Mobile / deep-link: ?connect=freighter | wallet_connect | stellar
  useEffect(() => {
    if (!open || autoStarted.current || isConnecting) return;
    const params = new URLSearchParams(window.location.search);
    const target = (params.get("connect") || "").toLowerCase();
    if (!target || target === "1" || target === "true") return;
    autoStarted.current = true;
    const run = async () => {
      onOpenChange(false);
      await new Promise<void>((r) => setTimeout(r, 50));
      if (target === "stellar" || target === "all") {
        await connect();
        return;
      }
      if (target === "wc" || target === "wallet_connect" || target === "freighter_mobile") {
        if (reownConfigured) await connectStellarWallet("wallet_connect");
        else await connect();
        return;
      }
      await connectStellarWallet(target);
    };
    void run();
  }, [open, isConnecting, connect, connectStellarWallet, onOpenChange]);

  const pickStellar = async (walletId: string) => {
    onOpenChange(false);
    await new Promise<void>((r) => setTimeout(r, 50));
    if (walletId === "__all__") {
      await connect();
      return;
    }
    await connectStellarWallet(walletId);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md border-white/10 bg-[#0D111B] text-[#F8FAFC]">
        <DialogHeader>
          <DialogTitle>Connect Wallet</DialogTitle>
          <DialogDescription className="text-[#94A3B8]">
            Pick a wallet — Freighter / MetaMask will show their own connection popup.
          </DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-2 pt-2 max-h-[70vh] overflow-y-auto">
          <p className="text-[11px] font-semibold tracking-wide text-[#38BDF8] uppercase">
            Stellar
          </p>
          {STELLAR_OPTIONS.filter((o) => !o.needsWc || reownConfigured).map((opt) => (
            <Button
              key={opt.id}
              variant="outline"
              className="w-full justify-between gap-3 rounded-xl h-14 border-[#2563EB]/35 bg-[#111827]/80 hover:bg-[#1e293b]"
              disabled={isConnecting}
              onClick={() => void pickStellar(opt.id)}
              data-testid={`button-connect-${opt.id}`}
            >
              <span className="flex items-center gap-3 min-w-0">
                <SiStellar className="w-5 h-5 shrink-0 text-[#2563EB]" />
                <span className="flex flex-col items-start min-w-0">
                  <span className="font-medium flex items-center gap-2">
                    {opt.name}
                    {opt.id === "freighter" && freighterReady === true && (
                      <Badge className="bg-emerald-500/20 text-emerald-300 text-[10px] px-1.5 py-0">
                        Detected
                      </Badge>
                    )}
                    {opt.id === "freighter" && freighterReady === false && (
                      <Badge className="bg-amber-500/20 text-amber-200 text-[10px] px-1.5 py-0">
                        Install / unlock
                      </Badge>
                    )}
                  </span>
                  <span className="text-[11px] text-[#94A3B8] truncate">{opt.hint}</span>
                </span>
              </span>
              <ChevronRight className="w-4 h-4 text-[#64748B] shrink-0" />
            </Button>
          ))}
          <Button
            variant="ghost"
            className="w-full justify-start text-[#94A3B8] hover:text-white"
            disabled={isConnecting}
            onClick={() => void pickStellar("__all__")}
            data-testid="button-connect-stellar-all"
          >
            {isConnecting ? "Opening wallets…" : "Browse all Stellar wallets…"}
          </Button>

          <p className="text-[11px] font-semibold tracking-wide text-[#38BDF8] uppercase pt-2">
            EVM
          </p>
          {reownConfigured ? (
            <EvmConnectActions onAuthenticated={() => onOpenChange(false)} />
          ) : (
            <div className="rounded-xl border border-dashed border-[#2563EB]/30 px-4 py-3 text-[12px] text-[#94A3B8] leading-relaxed">
              MetaMask, Rainbow, Trust, Coinbase and more need{" "}
              <code className="text-[#38BDF8]">VITE_REOWN_PROJECT_ID</code> on Vercel (from{" "}
              <a
                className="underline text-[#38BDF8]"
                href="https://dashboard.reown.com"
                target="_blank"
                rel="noreferrer"
              >
                dashboard.reown.com
              </a>
              ).
            </div>
          )}
          <p className="text-[11px] text-[#64748B] leading-relaxed pt-1">
            Networks: Stellar · Ethereum · Polygon · Base · Arbitrum · Optimism · BNB · Avalanche
          </p>
        </div>
      </DialogContent>
    </Dialog>
  );
}
