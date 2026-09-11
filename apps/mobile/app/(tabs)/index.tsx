import { useEffect, useState } from "react";
import { ScrollView, StyleSheet, View } from "react-native";
import { useRouter } from "expo-router";
import {
  Body,
  Caption,
  Card,
  DemoBadge,
  Heading,
  PrimaryButton,
  Screen,
  StatTile,
  Title,
} from "../../src/components/ui";
import { getCredentialStore } from "../../src/store/memoryCredentialStore";
import { DEMO_ACTIVITY } from "../../src/data/demo";
import { spacing } from "../../src/theme/tokens";

export default function HomeScreen() {
  const router = useRouter();
  const [count, setCount] = useState(0);

  useEffect(() => {
    getCredentialStore()
      .list()
      .then((items) => setCount(items.length))
      .catch(() => setCount(0));
  }, []);

  return (
    <Screen style={{ padding: 0 }}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.header}>
          <Title>Krydo</Title>
          <Caption>Store · Prove · Present · Verify</Caption>
          <View style={{ marginTop: spacing.sm }}>
            <DemoBadge />
          </View>
        </View>

        <View style={styles.statsRow}>
          <StatTile label="Your credentials" value={count} />
          <StatTile label="Ready to prove" value={count > 0 ? 1 : 0} />
        </View>

        <Card style={{ gap: spacing.sm }}>
          <Heading>Early mobile wallet</Heading>
          <Body>
            This is the Krydo Mobile v1 foundation. Proof generation currently uses a
            clearly marked mock prover. Real on-device proving is a future step.
          </Body>
          <PrimaryButton
            label="Start a proof"
            onPress={() => router.push("/prove")}
            accessibilityHint="Open the prove flow"
          />
        </Card>

        <View style={{ gap: spacing.sm }}>
          <Heading>Recent activity</Heading>
          {DEMO_ACTIVITY.map((a) => (
            <Card key={a.id} style={{ gap: 4 }}>
              <Body>{a.title}</Body>
              <Caption>{a.detail}</Caption>
            </Card>
          ))}
        </View>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: {
    padding: spacing.lg,
    gap: spacing.lg,
  },
  header: { gap: 4 },
  statsRow: { flexDirection: "row", gap: spacing.md },
});
