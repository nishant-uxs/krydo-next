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
} from "../../src/components/ui";
import { parseKrydoPresentationUri } from "../../src/qr/QRRequestParser";
import { colors, radius, spacing } from "../../src/theme/tokens";

const DEMO_URI = "krydo://present?request=req_demo001";

/**
 * Scan screen — camera QR later. Paste / parser works now over USB + Metro.
 */
export default function ScanScreen() {
  const router = useRouter();
  const [raw, setRaw] = useState(DEMO_URI);
  const [error, setError] = useState<string | null>(null);

  function onParse(value = raw) {
    setError(null);
    const trimmed = value.trim();

    if (/^https?:\/\/.+:\d+/i.test(trimmed) || /localhost:\d+/i.test(trimmed)) {
      setError(
        "That is a Metro/dev-server URL. Paste a Krydo link like krydo://present?request=req_demo001",
      );
      return;
    }

    const parsed = parseKrydoPresentationUri(trimmed);
    if (!parsed.ok) {
      setError(parsed.reason);
      return;
    }
    router.push(`/prove/${parsed.requestId}`);
  }

  return (
    <Screen>
      <View style={styles.wrap}>
        <Heading>Scan a Krydo request</Heading>
        <Body>
          Paste a Krydo presentation request URI, or tap the demo button.
          Camera QR scanning comes in a later native build pass.
        </Body>

        <Card style={{ gap: spacing.md, minHeight: 120, justifyContent: "center" }}>
          <Caption>
            Tip: do not paste http://IP:8081 here — that is only for connecting
            the development server.
          </Caption>
        </Card>

        <Caption>Krydo URI</Caption>
        <TextInput
          accessibilityLabel="Krydo request URI"
          value={raw}
          onChangeText={setRaw}
          autoCapitalize="none"
          autoCorrect={false}
          style={styles.input}
        />
        {error ? <Caption>{error}</Caption> : null}
        <PrimaryButton label="Open request" onPress={() => onParse()} />
        <SecondaryButton
          label="Try demo request"
          onPress={() => {
            setRaw(DEMO_URI);
            onParse(DEMO_URI);
          }}
        />
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
