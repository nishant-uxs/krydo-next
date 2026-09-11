import { useState } from "react";
import { StyleSheet, TextInput, View } from "react-native";
import { useRouter } from "expo-router";
import {
  Body,
  Caption,
  Card,
  Heading,
  PrimaryButton,
  Screen,
  SecondaryButton,
} from "../../../src/components/ui";
import { colors, radius, spacing } from "../../../src/theme/tokens";
import { parseKrydoPresentationUri } from "../../../src/qr/QRRequestParser";

const DEMO_URI = "krydo://present?request=req_demo001";

export default function ProveIndexScreen() {
  const router = useRouter();
  const [requestId, setRequestId] = useState(DEMO_URI);
  const [error, setError] = useState<string | null>(null);

  function continueWithId(raw: string) {
    setError(null);
    const trimmed = raw.trim();
    if (!trimmed) {
      setError("Enter a request id or Krydo URI");
      return;
    }

    // Users often paste the Metro bundler URL here by mistake.
    if (/^https?:\/\/.+:\d+/i.test(trimmed) || /localhost:\d+/i.test(trimmed)) {
      setError(
        "That looks like a Metro/dev-server URL, not a Krydo request. Use krydo://present?request=… or tap Try demo request.",
      );
      return;
    }

    if (trimmed.includes("://")) {
      const parsed = parseKrydoPresentationUri(trimmed);
      if (!parsed.ok) {
        setError(parsed.reason);
        return;
      }
      router.push(`/prove/${parsed.requestId}`);
      return;
    }

    router.push(`/prove/${encodeURIComponent(trimmed)}`);
  }

  return (
    <Screen>
      <View style={styles.wrap}>
        <Heading>Prove</Heading>
        <Body>
          Open a presentation request from a verifier, then select a credential and
          create a presentation.
        </Body>

        <Card style={{ gap: spacing.md }}>
          <Caption>Request ID or krydo:// URI</Caption>
          <TextInput
            accessibilityLabel="Presentation request id"
            value={requestId}
            onChangeText={setRequestId}
            placeholder="krydo://present?request=…"
            placeholderTextColor={colors.muted}
            autoCapitalize="none"
            autoCorrect={false}
            style={styles.input}
          />
          {error ? (
            <Caption accessibilityLabel="Error">{error}</Caption>
          ) : null}
          <PrimaryButton
            label="Continue"
            onPress={() => continueWithId(requestId)}
          />
          <SecondaryButton
            label="Try demo request"
            onPress={() => {
              setRequestId(DEMO_URI);
              continueWithId(DEMO_URI);
            }}
          />
          <SecondaryButton
            label="Scan instead"
            onPress={() => router.push("/scan")}
          />
        </Card>

        <Caption>
          Current proofs use DEMO / MOCK PROOF — not cryptographically valid.
        </Caption>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  wrap: { gap: spacing.lg },
  input: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    minHeight: 48,
    paddingHorizontal: spacing.md,
    color: colors.ink,
    backgroundColor: colors.surface,
  },
});
