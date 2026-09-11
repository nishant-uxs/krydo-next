import { describe, it, expect, beforeEach } from "vitest";
import {
  MemoryCredentialStore,
  __resetCredentialStoreForTests,
  getCredentialStore,
} from "../src/store/memoryCredentialStore";
import { DEMO_CREDENTIALS } from "../src/data/demo";

describe("CredentialStore", () => {
  beforeEach(() => {
    __resetCredentialStoreForTests(new MemoryCredentialStore([]));
  });

  it("saves and retrieves", async () => {
    const store = getCredentialStore();
    await store.save(DEMO_CREDENTIALS[0]);
    const got = await store.get(DEMO_CREDENTIALS[0].id);
    expect(got?.title).toBe("B.Tech Computer Science");
  });

  it("lists credentials", async () => {
    const store = getCredentialStore();
    await store.save(DEMO_CREDENTIALS[0]);
    await store.save(DEMO_CREDENTIALS[1]);
    const list = await store.list();
    expect(list).toHaveLength(2);
  });

  it("deletes credentials", async () => {
    const store = getCredentialStore();
    await store.save(DEMO_CREDENTIALS[0]);
    await store.remove(DEMO_CREDENTIALS[0].id);
    expect(await store.get(DEMO_CREDENTIALS[0].id)).toBeNull();
  });

  it("clears all", async () => {
    const store = getCredentialStore();
    await store.save(DEMO_CREDENTIALS[0]);
    await store.clear();
    expect(await store.list()).toEqual([]);
  });
});
