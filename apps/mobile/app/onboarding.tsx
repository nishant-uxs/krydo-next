import { useRouter } from "expo-router";
import { StyleSheet, View } from "react-native";
import {
  Body,
  Caption,
  Card,
  Heading,
  PrimaryButton,
  Screen,
  Title,
} from "../src/components/ui";
import { setOnboardingCompleted } from "../src/store/onboarding";
import { spacing } from "../src/theme/tokens";

export default function OnboardingScreen() {
  const router = useRouter();

  async function finish() {
    await setOnboardingCompleted();
    router.replace("/");
  }

  return (
    <Screen>
      <View style={styles.wrap}>
        <Title>Krydo</Title>
        <Heading>Prove claims carefully</Heading>
        <Body>
          Krydo lets you prove credential claims without unnecessarily revealing
          the underlying data.
        </Body>

        <Card style={{ gap: spacing.sm }}>
          <Caption>Early mobile wallet foundation</Caption>
          <Body>
            This app is a client for the existing Krydo presentation protocol.
            Current proof generation may still involve the Krydo backend. Device-only
            proving is a planned privacy hardening step — not what this build does.
          </Body>
        </Card>

        <Card style={{ gap: spacing.sm }}>
          <Caption>What you can do now</Caption>
          <Body>1. Browse demo credentials</Body>
          <Body>2. Open a presentation request</Body>
          <Body>3. Create a DEMO / MOCK presentation</Body>
        </Card>

        <PrimaryButton label="Continue" onPress={() => void finish()} />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  wrap: {
    flex: 1,
    justifyContent: "center",
    gap: spacing.lg,
  },
});
