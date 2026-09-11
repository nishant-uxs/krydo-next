import { useCallback, useEffect, useState } from "react";
import { useAccount, useSignMessage, useDisconnect, useChainId } from "wagmi";
import { useAppKit } from "@reown/appkit/react";
import { ensureReownAppKit, reownConfigured } from "./reown";
import { apiRequest } from "./queryClient";
import { apiUrl } from "./api-base";
import { setAuthToken } from "./auth-token";
import { useToast } from "@/hooks/use-toast";

const ACCOUNTS_KEY = "krydo_wallet_accounts";

export interface LinkedAccount {
  chainType: "STELLAR" | "EVM";
  chainId: string;
  address: string;
  label?: string | null;
}

function loadAccounts(): LinkedAccount[] {
  try {
    const raw = localStorage.getItem(ACCOUNTS_KEY);
    if (!raw) return [];
    return JSON.parse(raw) as LinkedAccount[];
  } catch {
    return [];
  }
}

export function upsertLinkedAccount(account: LinkedAccount) {
  const list = loadAccounts().filter(
    (a) =>
      !(
        a.chainId === account.chainId &&
        a.address.toLowerCase() === account.address.toLowerCase()
      ),
  );
  list.push(account);
  localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(list));
  return list;
}

/** Hook: EVM connect modal + SIWE. Must render under WagmiProvider when Reown is configured. */
export function useEvmSiwe() {
  const { address, isConnected } = useAccount();
  const chainId = useChainId();
  const { signMessageAsync } = useSignMessage();
  const { disconnect } = useDisconnect();
  const { open } = useAppKit();
  const { toast } = useToast();
  const [busy, setBusy] = useState(false);
  const [accounts, setAccounts] = useState<LinkedAccount[]>(() =>
    typeof window !== "undefined" ? loadAccounts() : [],
  );

  useEffect(() => {
    ensureReownAppKit();
  }, []);

  const openModal = useCallback(() => {
    if (!reownConfigured) {
      toast({
        title: "EVM wallets unavailable",
        description: "Set VITE_REOWN_PROJECT_ID (https://dashboard.reown.com)",
        variant: "destructive",
      });
      return;
    }
    ensureReownAppKit();
    void open({ view: "Connect" });
  }, [open, toast]);

  const runSiwe = useCallback(async () => {
    if (!address || !chainId) {
      toast({ title: "Connect an EVM wallet first", variant: "destructive" });
      return null;
    }
    setBusy(true);
    try {
      const nonceRes = await fetch(
        apiUrl(
          `/api/auth/siwe/nonce?address=${encodeURIComponent(address)}&chainId=${chainId}`,
        ),
      );
      if (!nonceRes.ok) {
        const err = await nonceRes.json().catch(() => ({}));
        throw new Error((err as { message?: string }).message || "SIWE nonce failed");
      }
      const { nonce, domain, uri, statement } = (await nonceRes.json()) as {
        nonce: string;
        domain: string;
        uri: string;
        statement: string;
      };
      const message = [
        `${domain} wants you to sign in with your Ethereum account:`,
        address,
        ``,
        statement,
        ``,
        `URI: ${uri}`,
        `Version: 1`,
        `Chain ID: ${chainId}`,
        `Nonce: ${nonce}`,
        `Issued At: ${new Date().toISOString()}`,
      ].join("\n");

      const signature = await signMessageAsync({ message });
      const verifyRes = await apiRequest("POST", "/api/auth/siwe/verify", {
        message,
        signature,
      });
      const data = (await verifyRes.json()) as {
        token: string;
        wallet: { address: string; chain: string; label?: string };
      };
      setAuthToken(data.token);
      const next = upsertLinkedAccount({
        chainType: "EVM",
        chainId: data.wallet.chain,
        address: data.wallet.address,
        label: data.wallet.label ?? "EVM Wallet",
      });
      setAccounts(next);
      localStorage.setItem(
        "krydo_wallet",
        JSON.stringify({
          address: data.wallet.address,
          role: "user",
          label: data.wallet.label ?? "EVM Wallet",
          onChainTxHash: null,
          chain: data.wallet.chain,
        }),
      );
      toast({ title: "EVM signed in", description: data.wallet.address });
      return data;
    } catch (e) {
      toast({
        title: "SIWE failed",
        description: e instanceof Error ? e.message : String(e),
        variant: "destructive",
      });
      return null;
    } finally {
      setBusy(false);
    }
  }, [address, chainId, signMessageAsync, toast]);

  return {
    reownConfigured,
    evmAddress: address,
    evmConnected: isConnected,
    chainId,
    busy,
    accounts,
    openModal,
    runSiwe,
    disconnectEvm: disconnect,
    refreshAccounts: () => setAccounts(loadAccounts()),
  };
}
