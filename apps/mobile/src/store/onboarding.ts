import AsyncStorage from "@react-native-async-storage/async-storage";

const ONBOARDING_KEY = "krydo.onboarding.completed";

export async function hasCompletedOnboarding(): Promise<boolean> {
  const v = await AsyncStorage.getItem(ONBOARDING_KEY);
  return v === "1";
}

export async function setOnboardingCompleted(): Promise<void> {
  await AsyncStorage.setItem(ONBOARDING_KEY, "1");
}

export async function resetOnboardingForTests(): Promise<void> {
  await AsyncStorage.removeItem(ONBOARDING_KEY);
}
