import { useState } from "react";
import { useWallet, shortenAddress } from "@/lib/wallet";
import { reownConfigured } from "@/lib/reown";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Badge } from "@/components/ui/badge";
import { LogOut, Copy, Check, Wallet } from "lucide-react";
import { SiStellar } from "react-icons/si";
import { EvmConnectActions } from "@/components/evm-connect-actions";

export function WalletButton() {
  const {
    address,
    role,
    label,
    isConnected,
    isConnecting,
    connect,
    disconnect,
  } = useWallet();
  const [copied, setCopied] = useState(false);
  const [open, setOpen] = useState(false);

  const copyAddress = () => {
    if (address) {
      navigator.clipboard.writeText(address);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  if (!isConnected) {
    return (
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogTrigger asChild>
          <Button
            size="sm"
            className="rounded-full shrink-0 bg-[#2563EB] hover:bg-[#1D4ED8]"
            data-testid="button-connect-wallet"
          >
            <Wallet className="w-4 h-4 sm:mr-2" />
            <span className="hidden sm:inline">Connect Wallet</span>
            <span className="sm:hidden">Connect</span>
          </Button>
        </DialogTrigger>
        <DialogContent className="sm:max-w-md border-white/10 bg-[#0D111B] text-[#F8FAFC]">
          <DialogHeader>
            <DialogTitle>Connect Wallet</DialogTitle>
            <DialogDescription className="text-[#94A3B8]">
              Stellar uses SIWS. EVM uses Reown AppKit + SIWE when configured.
            </DialogDescription>
          </DialogHeader>
          <div className="flex flex-col gap-3 pt-2">
            <Button
              className="w-full justify-start gap-3 rounded-full h-12 bg-[#2563EB] hover:bg-[#1D4ED8]"
              disabled={isConnecting}
              onClick={async () => {
                await connect();
                setOpen(false);
              }}
              data-testid="button-connect-stellar"
            >
              <SiStellar className="w-5 h-5" />
              {isConnecting ? "Signing in…" : "Connect Stellar"}
            </Button>
            {reownConfigured ? (
              <EvmConnectActions onAuthenticated={() => setOpen(false)} />
            ) : (
              <Button
                variant="outline"
                className="w-full rounded-full h-12 border-[#2563EB]/40 opacity-60"
                disabled
              >
                EVM (set VITE_REOWN_PROJECT_ID)
              </Button>
            )}
            <p className="text-[11px] text-[#94A3B8] leading-relaxed">
              Networks: Stellar · Ethereum · Polygon · Base · Arbitrum · Optimism · BNB · Avalanche
            </p>
          </div>
        </DialogContent>
      </Dialog>
    );
  }

  const roleColors: Record<string, string> = {
    root: "bg-chart-5/15 text-chart-5",
    issuer: "bg-chart-1/15 text-chart-1",
    user: "bg-chart-3/15 text-chart-3",
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="outline"
          size="sm"
          className="rounded-full max-w-[min(100%,11rem)] sm:max-w-none"
          data-testid="button-wallet-menu"
        >
          <div className="flex items-center gap-1.5 sm:gap-2 min-w-0">
            <SiStellar className="w-3 h-3 text-primary shrink-0" />
            <span className="font-mono text-[10px] sm:text-xs truncate">
              {shortenAddress(address!)}
            </span>
            <Badge
              variant="secondary"
              className={`hidden sm:inline-flex text-[10px] px-1.5 py-0 shrink-0 ${roleColors[role || "user"]}`}
            >
              {role}
            </Badge>
          </div>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-64">
        <div className="px-3 py-2">
          <p className="text-xs text-muted-foreground">Connected</p>
          <p className="font-mono text-xs mt-1 break-all">{address}</p>
          {label && <p className="text-xs text-muted-foreground mt-1">{label}</p>}
        </div>
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={copyAddress}>
          {copied ? <Check className="w-4 h-4 mr-2" /> : <Copy className="w-4 h-4 mr-2" />}
          Copy address
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={() => disconnect()}>
          <LogOut className="w-4 h-4 mr-2" />
          Disconnect
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
