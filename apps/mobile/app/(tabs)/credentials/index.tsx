import { useCallback, useState } from "react";
import { FlatList, Pressable, StyleSheet, View } from "react-native";
import { useFocusEffect, useRouter } from "expo-router";
import {
  Body,
  Caption,
  Card,
  DemoBadge,
  EmptyState,
  ErrorState,
  LoadingState,
  Screen,
} from "../../../src/components/ui";
import { getCredentialStore } from "../../../src/store/memoryCredentialStore";
import type { StoredCredential } from "../../../src/data/demo";
import { colors, spacing } from "../../../src/theme/tokens";

export default function CredentialsScreen() {
  const router = useRouter();
  const [items, setItems] = useState<StoredCredential[]>([]);
  const [state, setState] = useState<"loading" | "ready" | "error">("loading");
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setState("loading");
    setError(null);
    try {
      const list = await getCredentialStore().list();
      setItems(list);
      setState("ready");
    } catch {
      setError("Could not load credentials");
      setState("error");
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      void load();
    }, [load]),
  );

  if (state === "loading") {
    return (
      <Screen>
        <LoadingState label="Loading credentials…" />
      </Screen>
    );
  }

  if (state === "error") {
    return (
      <Screen>
        <ErrorState message={error ?? "Unknown error"} onRetry={() => void load()} />
      </Screen>
    );
  }

  if (items.length === 0) {
    return (
      <Screen>
        <EmptyState
          title="No credentials yet"
          detail="Demo credentials will appear here when seeded."
        />
      </Screen>
    );
  }

  return (
    <Screen style={{ padding: 0 }}>
      <FlatList
        contentContainerStyle={styles.list}
        data={items}
        keyExtractor={(c) => c.id}
        ListHeaderComponent={
          <View style={styles.header}>
            <DemoBadge />
            <Caption>Synthetic demo credentials — not real PII</Caption>
          </View>
        }
        renderItem={({ item }) => (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`${item.title}, ${item.status}`}
            onPress={() => router.push(`/credentials/${item.id}`)}
            style={({ pressed }) => [pressed && { opacity: 0.85 }]}
          >
            <Card style={styles.card}>
              <Body>{item.title}</Body>
              <Caption>Issuer: {item.issuerName}</Caption>
              <Caption>
                Status: {item.status}
                {item.expiresAt
                  ? ` · Expires: ${new Date(item.expiresAt).getFullYear()}`
                  : ""}
              </Caption>
            </Card>
          </Pressable>
        )}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  list: { padding: spacing.lg, gap: spacing.md },
  header: { gap: spacing.sm, marginBottom: spacing.sm },
  card: { gap: 4, borderColor: colors.border },
});
