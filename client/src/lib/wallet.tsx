/**
 * Krydo WalletProvider — Stellar (SIWS) + multi-account shell for EVM (SIWE).
 *
 * Same `useWallet()` shape the app already uses. Internally:
 *   1. Connect opens Stellar Wallets Kit auth modal (Freighter, xBull, Lobstr, …).
 *   2. Sign-in is SIWS: server nonce → canonical message → kit.signMessage (SEP-53
 *      for Freighter-class wallets) → JWT.
 *   3. Contract calls go through the same kit (`lib/contracts.ts`).
 *   4. EVM: `connectEvm` opens Reown AppKit; SIWE completes in WalletButton / evm-auth.
 */

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { apiRequest, queryClient } from "./queryClient";
import { apiUrl } from "./api-base";
import { setAuthToken, getAuthToken } from "./auth-token";
import { NETWORK_LABEL, STELLAR_NETWORK } from "./stellar";
import {
  ensureWalletKit,
  rememberWalletId,
  expectedPassphrase,
  KitEventType,
} from "./wallet-kit";
import { openEvmConnect, reownConfigured } from "./reown";
import { useToast } from "@/hooks/use-toast";
import { TxConfirmDialog, type TxConfirmInfo } from "@/components/tx-confirm-dialog";
import { anchorRoleViaWallet } from "./contracts";
import type { WalletAccount } from "@shared/wallet";
import { stellarCaip2 } from "@shared/wallet";
import { returnToKrydoMobileApp, wantsMobileReturn } from "./mobile-return";

const STORAGE_KEY = "krydo_wallet";
const ACCOUNTS_KEY = "krydo_wallet_accounts";

interface StoredWallet {
  address: string;
  role: string;
  label: string | null;
  onChainTxHash: string | null;
  chain?: string;
}

interface WalletContextType {
  address: string | null;
  role: string | null;
  label: string | null;
  onChainTxHash: string | null;
  isConnected: boolean;
  isConnecting: boolean;
  /** True once at least one wallet module reports available (or after first modal). */
  hasWallet: boolean;
  /** Active kit module id, e.g. "freighter". */
  walletId: string | null;
  /** Linked accounts shell (Stellar + EVM). No silent identity merge. */
  accounts: WalletAccount[];
  /** Stellar SIWS connect — opens multi-wallet kit modal. */
  connect: () => Promise<void>;
  connectStellar: () => Promise<void>;
  /**
   * Connect a specific Stellar kit module (e.g. "freighter", "lobstr", "wallet_connect").
   * Triggers that wallet's own connection popup (Freighter requestAccess / WC deep link).
   */
  connectStellarWallet: (walletId: string) => Promise<void>;
  /** Opens Reown AppKit; SIWE sign-in is completed via WalletButton. */
  connectEvm: () => void;
  /** Disconnect active session, or remove one linked account when provided. */
  disconnect: (account?: WalletAccount) => void;
}

function loadAccounts(): WalletAccount[] {
  try {
    const raw = localStorage.getItem(ACCOUNTS_KEY);
    if (!raw) return [];
    return JSON.parse(raw) as WalletAccount[];
  } catch {
    return [];
  }
}

function saveAccounts(list: WalletAccount[]) {
  localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(list));
}

function stellarChainId(): string {
  return stellarCaip2(STELLAR_NETWORK === "mainnet" ? "mainnet" : "testnet");
}

const WalletContext = createContext<WalletContextType>({
  address: null,
  role: null,
  label: null,
  onChainTxHash: null,
  isConnected: false,
  isConnecting: false,
  hasWallet: true,
  walletId: null,
  accounts: [],
  connect: async () => {},
  connectStellar: async () => {},
  connectStellarWallet: async () => {},
  connectEvm: () => {},
  disconnect: () => {},
});

/** Normalise kit signed-message payload to a base64 signature string. */
function signatureToBase64(sig: unknown): string {
  if (typeof sig === "string") return sig;
  if (sig instanceof Uint8Array) {
    let bin = "";
    sig.forEach((b) => (bin += String.fromCharCode(b)));
    return btoa(bin);
  }
  const maybe = sig as { data?: number[] } | null;
  if (maybe?.data) {
    return btoa(String.fromCharCode(...maybe.data));
  }
  return String(sig);
}

function errMessage(err: unknown): string {
  if (err instanceof Error) return err.message;
  if (err && typeof err === "object" && "message" in err) {
    return String((err as { message: unknown }).message);
  }
  return String(err);
}

export function WalletProvider({ children }: { children: ReactNode }) {
  const { toast } = useToast();
  const [address, setAddress] = useState<string | null>(null);
  const [role, setRole] = useState<string | null>(null);
  const [label, setLabel] = useState<string | null>(null);
  const [onChainTxHash, setOnChainTxHash] = useState<string | null>(null);
  const [isConnecting, setIsConnecting] = useState(false);
  const [hasWallet, setHasWallet] = useState(true);
  const [walletId, setWalletId] = useState<string | null>(null);
  const [accounts, setAccounts] = useState<WalletAccount[]>([]);
  const [roleConfirmOpen, setRoleConfirmOpen] = useState(false);
  const [roleConfirmInfo, setRoleConfirmInfo] = useState<TxConfirmInfo | null>(null);
  const [roleAnchorPending, setRoleAnchorPending] = useState(false);
  const pendingRoleAnchor = useRef<{
    address: string;
    role: string;
    label: string | null;
  } | null>(null);

  const addressRef = useRef<string | null>(null);
  const signInInFlightFor = useRef<string | null>(null);

  // Bootstrap kit + detect available wallets once.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const kit = ensureWalletKit();
        const wallets = await kit.refreshSupportedWallets();
        if (!cancelled) {
          setHasWallet(wallets.some((w) => w.isAvailable) || wallets.length > 0);
        }
      } catch {
        if (!cancelled) setHasWallet(true);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  // Hydrate from localStorage when a JWT is still present.
  useEffect(() => {
    try {
      setAccounts(loadAccounts());
      const stored = localStorage.getItem(STORAGE_KEY);
      if (!stored) return;
      const token = getAuthToken();
      if (!token) {
        localStorage.removeItem(STORAGE_KEY);
        return;
      }
      const data = JSON.parse(stored) as StoredWallet;
      setAddress(data.address);
      setRole(data.role);
      setLabel(data.label);
      setOnChainTxHash(data.onChainTxHash || null);
      addressRef.current = data.address;
      ensureWalletKit();
      const id = localStorage.getItem("krydo_wallet_id");
      if (id) setWalletId(id);
    } catch {
      localStorage.removeItem(STORAGE_KEY);
    }
  }, []);

  const upsertAccount = useCallback((account: WalletAccount) => {
    setAccounts((prev) => {
      const next = prev.filter(
        (a) =>
          !(
            a.chainId === account.chainId &&
            a.address.toLowerCase() === account.address.toLowerCase()
          ),
      );
      next.push(account);
      saveAccounts(next);
      return next;
    });
  }, []);

  const clearLocalSession = useCallback(() => {
    setAddress(null);
    setRole(null);
    setLabel(null);
    setOnChainTxHash(null);
    addressRef.current = null;
    setWalletId(null);
    setAccounts([]);
    setAuthToken(null);
    localStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem(ACCOUNTS_KEY);
    rememberWalletId(null);
    queryClient.invalidateQueries({ queryKey: ["/api"] });
  }, []);

  // If the kit disconnects (profile modal / wallet), clear Krydo session.
  useEffect(() => {
    const kit = ensureWalletKit();
    const unsub = kit.on(KitEventType.DISCONNECT, () => {
      clearLocalSession();
    });
    const unsubWallet = kit.on(KitEventType.WALLET_SELECTED, (ev) => {
      const id = (ev as { payload?: { id?: string } }).payload?.id;
      if (id) {
        rememberWalletId(id);
        setWalletId(id);
      }
    });
    return () => {
      if (typeof unsub === "function") unsub();
      if (typeof unsubWallet === "function") unsubWallet();
    };
  }, [clearLocalSession]);

  const runSiwsFlow = useCallback(
    async (walletAddr: string) => {
      if (signInInFlightFor.current === walletAddr) return;
      signInInFlightFor.current = walletAddr;
      setIsConnecting(true);
      const kit = ensureWalletKit();
      try {
        // Prefer wallet-reported network when the module supports getNetwork.
        try {
          const net = await kit.getNetwork();
          if (net.networkPassphrase && net.networkPassphrase !== expectedPassphrase()) {
            throw new Error(
              `Please switch your wallet to ${NETWORK_LABEL} and try again.`,
            );
          }
        } catch (e) {
          // Some modules don't implement getNetwork — only rethrow our switch hint.
          const msg = errMessage(e);
          if (msg.includes("switch your wallet")) throw e;
        }

        const nonceRes = await fetch(
          apiUrl(`/api/auth/nonce?address=${encodeURIComponent(walletAddr)}`),
        );
        if (!nonceRes.ok) throw new Error("Failed to fetch auth nonce");
        const { nonce } = (await nonceRes.json()) as { nonce: string };

        const message = [
          `${window.location.host} wants you to sign in with your Stellar account:`,
          walletAddr,
          ``,
          `Sign in to Krydo to prove ownership of this wallet.`,
          ``,
          `URI: ${window.location.origin}`,
          `Version: 1`,
          `Network: ${STELLAR_NETWORK}`,
          `Nonce: ${nonce}`,
          `Issued At: ${new Date().toISOString()}`,
        ].join("\n");

        const signed = await kit.signMessage(message, {
          address: walletAddr,
          networkPassphrase: expectedPassphrase(),
        });
        if (!signed.signedMessage) {
          throw new Error("Wallet returned an empty signature");
        }
        const signature = signatureToBase64(signed.signedMessage);

        const verifyRes = await apiRequest("POST", "/api/auth/verify", {
          address: walletAddr,
          message,
          signature,
        });
        const { token, wallet, needsRoleAnchor } = (await verifyRes.json()) as {
          token: string;
          wallet: StoredWallet;
          needsRoleAnchor?: boolean;
        };

        setAuthToken(token);
        setAddress(wallet.address);
        setRole(wallet.role);
        setLabel(wallet.label);
        setOnChainTxHash(wallet.onChainTxHash || null);
        addressRef.current = wallet.address;
        localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...wallet, chain: "stellar" }));
        upsertAccount({
          chainType: "STELLAR",
          chainId: stellarChainId(),
          address: wallet.address,
          walletProvider: localStorage.getItem("krydo_wallet_id") || undefined,
          label: wallet.label,
          capabilities: ["siws", "soroban"],
        });
        queryClient.invalidateQueries({ queryKey: ["/api"] });

        // Android Custom Tab / Freighter in-app browser: hand session back — no JWT paste.
        if (wantsMobileReturn()) {
          returnToKrydoMobileApp(wallet.address, token);
          return;
        }

        if (needsRoleAnchor) {
          pendingRoleAnchor.current = {
            address: wallet.address,
            role: wallet.role,
            label: wallet.label,
          };
          setRoleConfirmInfo({
            action: "role_anchor",
            title: "Anchor Role On-Chain",
            description:
              "Record your Krydo role on Stellar so others can verify your authority. Your wallet will sign this transaction.",
            details: [
              { label: "Action", value: "Anchor role assignment" },
              { label: "Wallet", value: wallet.address, mono: true },
              { label: "Role", value: wallet.role },
              ...(wallet.label
                ? [{ label: "Label", value: wallet.label }]
                : []),
              { label: "Contract", value: "KrydoAudit", mono: true },
              { label: "Network", value: NETWORK_LABEL },
            ],
          });
          setRoleConfirmOpen(true);
        }
      } catch (err) {
        const msg = errMessage(err);
        // eslint-disable-next-line no-console
        console.error("Wallet sign-in failed:", err);
        setAuthToken(null);
        localStorage.removeItem(STORAGE_KEY);
        toast({
          title: "Sign-in failed",
          description: msg,
          variant: "destructive",
        });
        throw err;
      } finally {
        setIsConnecting(false);
        signInInFlightFor.current = null;
      }
    },
    [toast, upsertAccount],
  );

  const connect = useCallback(async () => {
    setIsConnecting(true);
    const kit = ensureWalletKit();
    try {
      // Let any parent Dialog unmount first so the kit modal isn't trapped under it.
      await new Promise<void>((r) => requestAnimationFrame(() => r()));
      const { address: addr } = await kit.authModal();
      if (!addr) {
        throw new Error("No Stellar account selected. Pick a wallet and try again.");
      }
      await runSiwsFlow(addr);
    } catch (err) {
      const msg = errMessage(err);
      // User closed the modal — not an error toast.
      if (msg.toLowerCase().includes("closed the modal")) {
        return;
      }
      // eslint-disable-next-line no-console
      console.error("Wallet connect failed:", err);
      if (!msg.startsWith("Sign-in failed")) {
        toast({
          title: "Connect failed",
          description: msg,
          variant: "destructive",
        });
      }
    } finally {
      setIsConnecting(false);
    }
  }, [runSiwsFlow, toast]);

  /**
   * Pick a specific kit module → wallet shows its own connect popup
   * (Freighter extension dialog, Freighter Mobile WC deep-link, etc.).
   */
  const connectStellarWallet = useCallback(
    async (walletId: string) => {
      setIsConnecting(true);
      const kit = ensureWalletKit();
      try {
        await new Promise<void>((r) => requestAnimationFrame(() => r()));
        kit.setWallet(walletId);
        rememberWalletId(walletId);
        setWalletId(walletId);
        const { address: addr } = await kit.getAddress();
        if (!addr) {
          throw new Error("No Stellar account returned. Approve the connection in your wallet.");
        }
        await runSiwsFlow(addr);
      } catch (err) {
        const msg = errMessage(err);
        if (
          msg.toLowerCase().includes("closed the modal") ||
          msg.toLowerCase().includes("user rejected") ||
          msg.toLowerCase().includes("rejected by user")
        ) {
          return;
        }
        // eslint-disable-next-line no-console
        console.error("Wallet connect failed:", err);
        toast({
          title: walletId === "freighter" ? "Freighter connect failed" : "Connect failed",
          description:
            msg.includes("not connected") || msg.includes("not available")
              ? "Install / unlock Freighter (Chrome extension) or use WalletConnect for Freighter Mobile, then try again."
              : msg,
          variant: "destructive",
        });
      } finally {
        setIsConnecting(false);
      }
    },
    [runSiwsFlow, toast],
  );

  const connectEvm = useCallback(() => {
    if (!reownConfigured) {
      toast({
        title: "EVM wallets unavailable",
        description: "Set VITE_REOWN_PROJECT_ID (https://dashboard.reown.com)",
        variant: "destructive",
      });
      return;
    }
    if (!openEvmConnect()) {
      toast({
        title: "Could not open AppKit",
        description: "Check VITE_REOWN_PROJECT_ID and reload.",
        variant: "destructive",
      });
    }
  }, [toast]);

  const disconnect = useCallback(
    (account?: WalletAccount) => {
      if (account) {
        setAccounts((prev) => {
          const next = prev.filter(
            (a) =>
              !(
                a.chainId === account.chainId &&
                a.address.toLowerCase() === account.address.toLowerCase()
              ),
          );
          saveAccounts(next);
          return next;
        });
        const active =
          address &&
          address.toLowerCase() === account.address.toLowerCase();
        if (!active) return;
      }
      try {
        ensureWalletKit().disconnect();
      } catch {
        /* ignore */
      }
      clearLocalSession();
      queryClient.clear();
    },
    [address, clearLocalSession],
  );

  const confirmRoleAnchor = useCallback(async () => {
    const pending = pendingRoleAnchor.current;
    if (!pending) {
      setRoleConfirmOpen(false);
      return;
    }
    setRoleAnchorPending(true);
    try {
      const tx = await anchorRoleViaWallet(
        pending.address,
        pending.role,
        pending.label || pending.role,
      );
      const res = await apiRequest("POST", "/api/auth/role-anchor", {
        txHash: tx.txHash,
      });
      const data = (await res.json()) as {
        wallet?: StoredWallet;
        txHash?: string;
      };
      const nextHash = data.wallet?.onChainTxHash || data.txHash || tx.txHash;
      setOnChainTxHash(nextHash);
      if (data.wallet) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(data.wallet));
      } else {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
          const w = JSON.parse(stored) as StoredWallet;
          w.onChainTxHash = nextHash;
          localStorage.setItem(STORAGE_KEY, JSON.stringify(w));
        }
      }
      toast({
        title: "Role anchored",
        description: "Your role is recorded on Stellar.",
      });
      queryClient.invalidateQueries({ queryKey: ["/api"] });
      setRoleConfirmOpen(false);
      pendingRoleAnchor.current = null;
    } catch (err) {
      toast({
        title: "Role anchor failed",
        description: errMessage(err),
        variant: "destructive",
      });
    } finally {
      setRoleAnchorPending(false);
    }
  }, [toast]);

  return (
    <WalletContext.Provider
      value={{
        address,
        role,
        label,
        onChainTxHash,
        isConnected: !!address && !!role,
        isConnecting,
        hasWallet,
        walletId,
        accounts,
        connect,
        connectStellar: connect,
        connectStellarWallet,
        connectEvm,
        disconnect,
      }}
    >
      {children}
      <TxConfirmDialog
        open={roleConfirmOpen}
        onOpenChange={(open) => {
          if (roleAnchorPending) return;
          setRoleConfirmOpen(open);
          if (!open) pendingRoleAnchor.current = null;
        }}
        info={roleConfirmInfo}
        isPending={roleAnchorPending}
        onConfirm={() => {
          void confirmRoleAnchor();
        }}
      />
    </WalletContext.Provider>
  );
}

export function useWallet() {
  return useContext(WalletContext);
}

export function shortenAddress(addr: string) {
  return `${addr.slice(0, 6)}...${addr.slice(-4)}`;
}
