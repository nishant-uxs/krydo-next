import { useLocalSearchParams, useRouter } from "expo-router";
import { useMemo } from "react";
import { ScrollView, StyleSheet, View } from "react-native";
import {
  Body,
  Caption,
  Card,
  Heading,
  Label,
  MockProofBadge,
  PrimaryButton,
  Screen,
} from "../src/components/ui";
import type { PresentationVerifyResultDto } from "../src/api/VerificationClient";
import { colors, spacing } from "../src/theme/tokens";

export default function ResultScreen() {
  const router = useRouter();
  const { payload, mock } = useLocalSearchParams<{
    payload?: string;
    mock?: string;
  }>();

  const result = useMemo(() => {
    try {
      return JSON.parse(String(payload ?? "{}")) as PresentationVerifyResultDto;
    } catch {
      return null;
    }
  }, [payload]);

  if (!result) {
    return (
      <Screen>
        <Heading>Verification failed</Heading>
        <Caption>Missing or malformed result payload.</Caption>
        <PrimaryButton label="Back home" onPress={() => router.replace("/")} />
      </Screen>
    );
  }

  const ok = result.valid;

  return (
    <Screen style={{ padding: 0 }}>
      <ScrollView contentContainerStyle={styles.content}>
        <View
          style={[styles.banner, ok ? styles.ok : styles.bad]}
          accessibilityRole="summary"
          accessibilityLabel={ok ? "Verified" : "Verification failed"}
        >
          <Heading>{ok ? "✓ Verified" : "✕ Verification failed"}</Heading>
          <Body>{result.message}</Body>
        </View>

        {mock === "1" ? <MockProofBadge /> : null}

        <Card style={{ gap: spacing.sm }}>
          <Field
            label="Credential"
            value={result.credential?.claimType ?? "—"}
          />
          <Field label="Issuer" value={result.issuerName ?? result.credential?.issuerAddress ?? "—"} />
          <Field label="Status" value={result.credential?.status ?? "—"} />
          <Field
            label="Proof"
            value={result.checks?.proof ? "Valid" : "Invalid / unchecked"}
          />
          <Field
            label="Challenge"
            value={result.checks?.challenge ? "Valid" : "Invalid"}
          />
          <Field
            label="Audience"
            value={result.checks?.audience ? "Valid" : "Invalid"}
          />
        </Card>

        <PrimaryButton label="Done" onPress={() => router.replace("/")} />
      </ScrollView>
    </Screen>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <View style={{ gap: 4 }}>
      <Label>{label}</Label>
      <Body>{value}</Body>
    </View>
  );
}

const styles = StyleSheet.create({
  content: { padding: spacing.lg, gap: spacing.lg },
  banner: {
    borderRadius: 16,
    padding: spacing.lg,
    gap: spacing.sm,
  },
  ok: { backgroundColor: colors.successSoft },
  bad: { backgroundColor: colors.dangerSoft },
});
