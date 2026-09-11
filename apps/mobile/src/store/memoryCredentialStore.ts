import type { CredentialStore } from "./CredentialStore";
import type { StoredCredential } from "../data/demo";
import { DEMO_CREDENTIALS } from "../data/demo";

/**
 * In-memory + optional AsyncStorage-backed store for Mobile v1.
 * Secrets/private keys must never go here — use SecureStore for those later.
 */
export class MemoryCredentialStore implements CredentialStore {
  private items = new Map<string, StoredCredential>();

  constructor(seed: StoredCredential[] = DEMO_CREDENTIALS) {
    for (const c of seed) this.items.set(c.id, { ...c });
  }

  async list(): Promise<StoredCredential[]> {
    return [...this.items.values()].sort((a, b) =>
      a.title.localeCompare(b.title),
    );
  }

  async get(id: string): Promise<StoredCredential | null> {
    return this.items.get(id) ?? null;
  }

  async save(credential: StoredCredential): Promise<void> {
    this.items.set(credential.id, { ...credential });
  }

  async remove(id: string): Promise<void> {
    this.items.delete(id);
  }

  async clear(): Promise<void> {
    this.items.clear();
  }
}

let singleton: CredentialStore | null = null;

export function getCredentialStore(): CredentialStore {
  if (!singleton) singleton = new MemoryCredentialStore();
  return singleton;
}

/** Test helper */
export function __resetCredentialStoreForTests(store?: CredentialStore): void {
  singleton = store ?? new MemoryCredentialStore([]);
}
