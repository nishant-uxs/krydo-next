/**
 * Krydo mobile design tokens — fintech / security product look.
 * Clean, minimal, trustworthy. No crypto-bro aesthetic.
 */

export const colors = {
  bg: "#F4F6F8",
  surface: "#FFFFFF",
  ink: "#0B1220",
  muted: "#5B6575",
  border: "#E2E8F0",
  primary: "#0F3D68",
  primarySoft: "#E8F0F7",
  success: "#0F7B4B",
  successSoft: "#E6F6EE",
  danger: "#B42318",
  dangerSoft: "#FEE4E2",
  warning: "#B54708",
  warningSoft: "#FFFAEB",
  demo: "#6941C6",
  demoSoft: "#F4EBFF",
} as const;

export const spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
} as const;

export const radius = {
  sm: 8,
  md: 12,
  lg: 16,
} as const;

export const typography = {
  title: { fontSize: 28, fontWeight: "700" as const, color: colors.ink },
  heading: { fontSize: 20, fontWeight: "700" as const, color: colors.ink },
  body: { fontSize: 16, fontWeight: "400" as const, color: colors.ink },
  caption: { fontSize: 13, fontWeight: "500" as const, color: colors.muted },
  label: {
    fontSize: 12,
    fontWeight: "600" as const,
    color: colors.muted,
    letterSpacing: 0.4,
    textTransform: "uppercase" as const,
  },
};
