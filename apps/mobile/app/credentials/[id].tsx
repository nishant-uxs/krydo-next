import { useLocalSearchParams, useRouter } from "expo-router";
import { useEffect, useState } from "react";
import { ScrollView, StyleSheet, View } from "react-native";
import {
  Body,
  Caption,
  Card,
  DemoBadge,
  ErrorState,
  Label,
  LoadingState,
  PrimaryButton,
  Screen,
} from "../../src/components/ui";
import { getCredentialStore } from "../../src/store/memoryCredentialStore";
import type { StoredCredential } from "../../src/data/demo";
import { spacing } from "../../src/theme/tokens";

function Row({ label, value }: { label: string; value: string }) {
  return (
    <View style={{ gap: 4 }}>
      <Label>{label}</Label>
      <Body>{value}</Body>
    </View>
  );
}

export default function CredentialDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const [cred, setCred] = useState<StoredCredential | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const found = await getCredentialStore().get(String(id));
        if (cancelled) return;
        if (!found) setError("Credential not found");
        else setCred(found);
      } catch {
        if (!cancelled) setError("Failed to load credential");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]);

  if (loading) {
    return (
      <Screen>
        <LoadingState />
      </Screen>
    );
  }

  if (error || !cred) {
    return (
      <Screen>
        <ErrorState message={error ?? "Unknown credential"} onRetry={() => router.back()} />
      </Screen>
    );
  }

  return (
    <Screen style={{ padding: 0 }}>
      <ScrollView contentContainerStyle={styles.content}>
        <DemoBadge />
        <Card style={{ gap: spacing.md }}>
          <Row label="Type" value={cred.title} />
          <Row label="Claim type" value={cred.claimType} />
          <Row label="Issuer" value={cred.issuerName} />
          <Row label="Subject" value={cred.holderName} />
          <Row label="Status" value={cred.status} />
          <Row
            label="Issued"
            value={new Date(cred.issuedAt).toLocaleDateString()}
          />
          <Row
            label="Expires"
            value={
              cred.expiresAt
                ? new Date(cred.expiresAt).toLocaleDateString()
                : "—"
            }
          />
          <View style={{ gap: 4 }}>
            <Label>Credential hash</Label>
            <Caption>{cred.credentialHash.slice(0, 18)}…</Caption>
          </View>
          <Caption>{cred.displaySummary}</Caption>
        </Card>
        <PrimaryButton
          label="Use in a proof"
          onPress={() => router.push("/prove")}
        />
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { padding: spacing.lg, gap: spacing.lg },
});
