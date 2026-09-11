import React from "react";
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  View,
  type ViewStyle,
} from "react-native";
import { colors, radius, spacing, typography } from "../theme/tokens";

export function Screen({
  children,
  style,
}: {
  children: React.ReactNode;
  style?: ViewStyle;
}) {
  return <View style={[styles.screen, style]}>{children}</View>;
}

export function Card({
  children,
  style,
}: {
  children: React.ReactNode;
  style?: ViewStyle;
}) {
  return <View style={[styles.card, style]}>{children}</View>;
}

export function Title({ children }: { children: React.ReactNode }) {
  return <Text style={typography.title}>{children}</Text>;
}

export function Heading({ children }: { children: React.ReactNode }) {
  return <Text style={typography.heading}>{children}</Text>;
}

export function Body({ children }: { children: React.ReactNode }) {
  return <Text style={typography.body}>{children}</Text>;
}

export function Caption({
  children,
  accessibilityLabel,
}: {
  children: React.ReactNode;
  accessibilityLabel?: string;
}) {
  return (
    <Text style={typography.caption} accessibilityLabel={accessibilityLabel}>
      {children}
    </Text>
  );
}

export function Label({ children }: { children: React.ReactNode }) {
  return <Text style={typography.label}>{children}</Text>;
}

export function PrimaryButton({
  label,
  onPress,
  disabled,
  accessibilityHint,
}: {
  label: string;
  onPress: () => void;
  disabled?: boolean;
  accessibilityHint?: string;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityHint={accessibilityHint}
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.primaryBtn,
        disabled && styles.btnDisabled,
        pressed && !disabled && styles.btnPressed,
      ]}
    >
      <Text style={styles.primaryBtnText}>{label}</Text>
    </Pressable>
  );
}

export function SecondaryButton({
  label,
  onPress,
  disabled,
}: {
  label: string;
  onPress: () => void;
  disabled?: boolean;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.secondaryBtn,
        disabled && styles.btnDisabled,
        pressed && !disabled && styles.btnPressed,
      ]}
    >
      <Text style={styles.secondaryBtnText}>{label}</Text>
    </Pressable>
  );
}

export function DemoBadge() {
  return (
    <View
      style={styles.demoBadge}
      accessibilityRole="text"
      accessibilityLabel="Demo or mock data"
    >
      <Text style={styles.demoBadgeText}>DEMO</Text>
    </View>
  );
}

export function MockProofBadge() {
  return (
    <View
      style={styles.mockBadge}
      accessibilityRole="text"
      accessibilityLabel="Demo mock proof — not cryptographically valid"
    >
      <Text style={styles.mockBadgeText}>DEMO / MOCK PROOF</Text>
    </View>
  );
}

export function LoadingState({ label = "Loading…" }: { label?: string }) {
  return (
    <View style={styles.center} accessibilityRole="progressbar" accessibilityLabel={label}>
      <ActivityIndicator size="large" color={colors.primary} />
      <Caption>{label}</Caption>
    </View>
  );
}

export function EmptyState({
  title,
  detail,
}: {
  title: string;
  detail?: string;
}) {
  return (
    <View style={styles.center}>
      <Heading>{title}</Heading>
      {detail ? <Caption>{detail}</Caption> : null}
    </View>
  );
}

export function ErrorState({
  message,
  onRetry,
}: {
  message: string;
  onRetry?: () => void;
}) {
  return (
    <View style={styles.center}>
      <Heading>Something went wrong</Heading>
      <Caption>{message}</Caption>
      {onRetry ? (
        <View style={{ marginTop: spacing.md }}>
          <PrimaryButton label="Retry" onPress={onRetry} accessibilityHint="Retry the failed action" />
        </View>
      ) : null}
    </View>
  );
}

export function StatTile({
  label,
  value,
}: {
  label: string;
  value: string | number;
}) {
  return (
    <Card style={styles.stat}>
      <Label>{label}</Label>
      <Text style={styles.statValue}>{value}</Text>
    </Card>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.bg,
    padding: spacing.lg,
  },
  card: {
    backgroundColor: colors.surface,
    borderRadius: radius.lg,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.md,
  },
  primaryBtn: {
    backgroundColor: colors.primary,
    minHeight: 48,
    borderRadius: radius.md,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: spacing.lg,
  },
  primaryBtnText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "700",
  },
  secondaryBtn: {
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    minHeight: 48,
    borderRadius: radius.md,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: spacing.lg,
  },
  secondaryBtnText: {
    color: colors.ink,
    fontSize: 16,
    fontWeight: "600",
  },
  btnDisabled: { opacity: 0.45 },
  btnPressed: { opacity: 0.85 },
  demoBadge: {
    alignSelf: "flex-start",
    backgroundColor: colors.demoSoft,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 999,
  },
  demoBadgeText: {
    color: colors.demo,
    fontSize: 11,
    fontWeight: "700",
  },
  mockBadge: {
    alignSelf: "flex-start",
    backgroundColor: colors.warningSoft,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: radius.sm,
    borderWidth: 1,
    borderColor: "#FEC84B",
  },
  mockBadgeText: {
    color: colors.warning,
    fontSize: 12,
    fontWeight: "800",
  },
  center: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    gap: spacing.sm,
    padding: spacing.lg,
  },
  stat: { flex: 1, gap: spacing.xs },
  statValue: {
    fontSize: 28,
    fontWeight: "700",
    color: colors.ink,
    marginTop: spacing.xs,
  },
});
