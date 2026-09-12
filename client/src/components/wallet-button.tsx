import { useState } from "react";
import { useWallet, shortenAddress } from "@/lib/wallet";
import { ConnectWalletDialog } from "@/components/connect-wallet-dialog";
import { Button } from "@/components/ui/button";
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

export function WalletButton({
  open: controlledOpen,
  onOpenChange,
  defaultOpen = false,
}: {
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  defaultOpen?: boolean;
} = {}) {
  const { address, role, label, isConnected, disconnect } = useWallet();
  const [copied, setCopied] = useState(false);
  const [uncontrolledOpen, setUncontrolledOpen] = useState(defaultOpen);

  const open = controlledOpen ?? uncontrolledOpen;
  const setOpen = onOpenChange ?? setUncontrolledOpen;

  const copyAddress = () => {
    if (address) {
      navigator.clipboard.writeText(address);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  if (!isConnected) {
    return (
      <>
        <Button
          size="sm"
          className="rounded-full shrink-0 bg-[#2563EB] hover:bg-[#1D4ED8]"
          data-testid="button-connect-wallet"
          onClick={() => setOpen(true)}
        >
          <Wallet className="w-4 h-4 sm:mr-2" />
          <span className="hidden sm:inline">Connect Wallet</span>
          <span className="sm:hidden">Connect</span>
        </Button>
        <ConnectWalletDialog open={open} onOpenChange={setOpen} />
      </>
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
