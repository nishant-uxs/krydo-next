/**
 * Mobile app handoff: after SIWS in a Custom Tab / Freighter in-app browser,
 * bounce back into Krydo Android with address + JWT (no paste).
 */
export function wantsMobileReturn(): boolean {
  if (typeof window === "undefined") return false;
  const p = new URLSearchParams(window.location.search);
  return p.get("mobile_return") === "1" || p.get("return") === "krydo";
}

export function mobileAuthDeepLink(address: string, token: string): string {
  const q = new URLSearchParams({
    address,
    token,
  });
  return `krydo://auth?${q.toString()}`;
}

/** Redirect into the Krydo Android app after a successful SIWS. */
export function returnToKrydoMobileApp(address: string, token: string): void {
  const deepLink = mobileAuthDeepLink(address, token);
  // Prefer assign so Custom Tabs / Freighter in-app browser hand off cleanly.
  window.location.assign(deepLink);
  // Fallback UI if the OS does not open the app.
  window.setTimeout(() => {
    const el = document.getElementById("krydo-mobile-return");
    if (el) el.style.display = "block";
  }, 800);
}
