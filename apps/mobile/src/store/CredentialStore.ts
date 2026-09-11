import type { StoredCredential } from "../data/demo";

/**
 * Credential wallet storage abstraction.
 * UI must depend on this interface — not AsyncStorage directly —
 * so encrypted wallet storage can replace the impl later.
 */
export interface CredentialStore {
  list(): Promise<StoredCredential[]>;
  get(id: string): Promise<StoredCredential | null>;
  save(credential: StoredCredential): Promise<void>;
  remove(id: string): Promise<void>;
  clear(): Promise<void>;
}
