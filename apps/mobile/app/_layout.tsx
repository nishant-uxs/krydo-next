import { Stack, useRouter, useRootNavigationState } from "expo-router";
import { useEffect, useState } from "react";
import { StatusBar } from "expo-status-bar";
import * as SplashScreen from "expo-splash-screen";
import { hasCompletedOnboarding } from "../src/store/onboarding";
import { colors } from "../src/theme/tokens";

SplashScreen.preventAutoHideAsync().catch(() => undefined);

/**
 * Root layout — wait for navigation mount before any redirect.
 * Early router.replace() before the root navigator is ready causes a blank screen.
 */
export default function RootLayout() {
  const router = useRouter();
  const nav = useRootNavigationState();
  const [bootstrapped, setBootstrapped] = useState(false);

  useEffect(() => {
    if (!nav?.key || bootstrapped) return;

    let cancelled = false;
    (async () => {
      try {
        const done = await hasCompletedOnboarding();
        if (cancelled) return;
        if (!done) {
          router.replace("/onboarding");
        }
      } catch {
        // AsyncStorage failure — still enter the app with demo data.
        if (!cancelled) router.replace("/");
      } finally {
        if (!cancelled) {
          setBootstrapped(true);
          SplashScreen.hideAsync().catch(() => undefined);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [nav?.key, bootstrapped, router]);

  return (
    <>
      <StatusBar style="dark" />
      <Stack
        screenOptions={{
          headerStyle: { backgroundColor: colors.surface },
          headerTintColor: colors.ink,
          headerTitleStyle: { fontWeight: "700" },
          contentStyle: { backgroundColor: colors.bg },
        }}
      >
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen name="onboarding" options={{ headerShown: false }} />
        <Stack.Screen name="present" options={{ title: "Open request" }} />
        <Stack.Screen
          name="credentials/[id]"
          options={{ title: "Credential" }}
        />
        <Stack.Screen
          name="prove/[requestId]"
          options={{ title: "Verification request" }}
        />
        <Stack.Screen name="result" options={{ title: "Verification result" }} />
      </Stack>
    </>
  );
}
