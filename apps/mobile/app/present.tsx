import { Redirect, useLocalSearchParams } from "expo-router";
import { parseKrydoPresentationUri } from "../src/qr/QRRequestParser";
import { ErrorState, Screen } from "../src/components/ui";

/**
 * Deep-link landing for:
 *   krydo://present?request=<id>
 *   https://krydo.dev/present?request=<id>
 */
export default function PresentDeepLinkScreen() {
  const params = useLocalSearchParams<{ request?: string }>();
  const request = typeof params.request === "string" ? params.request : "";

  if (request) {
    return <Redirect href={`/prove/${encodeURIComponent(request)}`} />;
  }

  // If the OS delivered a full URL somehow via path, try parsing.
  const parsed = parseKrydoPresentationUri(
    `krydo://present?request=${request || ""}`,
  );
  if (parsed.ok) {
    return <Redirect href={`/prove/${encodeURIComponent(parsed.requestId)}`} />;
  }

  return (
    <Screen>
      <ErrorState message="Missing request id in deep link" />
    </Screen>
  );
}
