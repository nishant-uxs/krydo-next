/** Stub: @wagmi/connectors@8 may import `@wagmi/core/tempo` which wagmi v2 core lacks. */
export type TempoWalletParameters = Record<string, unknown>;

export function tempoWallet(_params?: TempoWalletParameters): never {
  throw new Error("Tempo wallet is not used by Krydo");
}
