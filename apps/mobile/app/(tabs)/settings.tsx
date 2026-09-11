import { Alert, StyleSheet, View } from "react-native";
import {
  Body,
  Caption,
  Card,
  Heading,
  PrimaryButton,
  Screen,
  SecondaryButton,
} from "../../src/components/ui";
import { getConfig } from "../../src/config/env";
import { resetOnboardingForTests } from "../../src/store/onboarding";
import { spacing } from "../../src/theme/tokens";
import { useRouter } from "expo-router";

export default function SettingsScreen() {
  const config = getConfig();
  const router = useRouter();

  return (
    <Screen>
      <View style={styles.wrap}>
        <Heading>Settings</Heading>

        <Card style={{ gap: spacing.sm }}>
          <Caption>App</Caption>
          <Body>Krydo Mobile v1 foundation</Body>
          <Caption>Package: dev.krydo.mobile</Caption>
        </Card>

        <Card style={{ gap: spacing.sm }}>
          <Caption>API</Caption>
          <Body>{config.apiBaseUrl}</Body>
          <Caption>
            Mock data: {config.useMockData ? "enabled" : "disabled"}
          </Caption>
        </Card>

        <Card style={{ gap: spacing.sm }}>
          <Caption>Privacy</Caption>
          <Body>
            Tokens, private keys, claim payloads, and full presentations are not
            logged. Proofs in this build are DEMO / MOCK unless a future native
            prover is enabled.
          </Body>
        </Card>

        <PrimaryButton
          label="Replay onboarding"
          onPress={async () => {
            await resetOnboardingForTests();
            router.replace("/onboarding");
          }}
        />
        <SecondaryButton
          label="About proof architecture"
          onPress={() =>
            Alert.alert(
              "ProofProver",
              "MockProofProver is active. KrydoMobileZkProver is reserved for a future phase. Do not treat mock proofs as real crypto.",
            )
          }
        />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  wrap: { gap: spacing.lg },
});
