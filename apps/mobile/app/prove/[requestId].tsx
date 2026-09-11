import { useLocalSearchParams, useRouter } from "expo-router";
import { useEffect, useMemo, useState } from "react";
import { Pressable, ScrollView, StyleSheet, View } from "react-native";
import {
  Body,
  Caption,
  Card,
  ErrorState,
  Heading,
  Label,
  LoadingState,
  MockProofBadge,
  PrimaryButton,
  Screen,
  SecondaryButton,
} from "../../src/components/ui";
import {
  ApiError,
  getVerificationClient,
  type PresentationRequestDto,
} from "../../src/api/VerificationClient";
import { getCredentialStore } from "../../src/store/memoryCredentialStore";
import type { StoredCredential } from "../../src/data/demo";
import { getProofProver } from "../../src/prover/ProofProver";
import { buildHolderPresentation } from "../../src/api/buildPresentation";
import { colors, spacing } from "../../src/theme/tokens";

type Step = "review" | "select" | "present";

export default function ProveRequestScreen() {
  const { requestId } = useLocalSearchParams<{ requestId: string }>();
  const router = useRouter();
  const id = String(requestId ?? "");

  const [step, setStep] = useState<Step>("review");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [request, setRequest] = useState<PresentationRequestDto | null>(null);
  const [credentials, setCredentials] = useState<StoredCredential[]>([]);
  const [selected, setSelected] = useState<StoredCredential | null>(null);
  const [busy, setBusy] = useState(false);
  const [presentationJson, setPresentationJson] = useState<string | null>(null);
  const [mockLabel, setMockLabel] = useState(false);

  const expired = useMemo(() => {
    if (!request) return false;
    return new Date(request.expiresAt).getTime() <= Date.now();
  }, [request]);

  const matchingCredentials = useMemo(() => {
    if (!request) return credentials;
    const matched = credentials.filter((c) =>
      request.requestedCredentials.some((r) => r.claimType === c.claimType),
    );
    return matched.length > 0 ? matched : credentials;
  }, [credentials, request]);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const req = await getVerificationClient().getPresentationRequest(id);
      const list = await getCredentialStore().list();
      setRequest(req);
      setCredentials(list);
      setStep("review");
    } catch (e) {
      const msg =
        e instanceof ApiError
          ? e.message
          : "Could not load presentation request";
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function generatePresentation(cred: StoredCredential) {
    if (!request) return;
    setSelected(cred);
    setBusy(true);
    setError(null);
    try {
      const proof = await getProofProver().prove({
        requestId: request.id,
        credentialId: cred.id,
        claimType: cred.claimType,
        policyKind: request.policy.kind,
        challenge: request.challenge,
        audience: request.audience,
      });
      const built = buildHolderPresentation({
        request,
        credential: cred,
        proof,
      });
      setMockLabel(built.isMock);
      setPresentationJson(JSON.stringify(built.presentation, null, 2));
      setStep("present");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to build presentation");
    } finally {
      setBusy(false);
    }
  }

  async function submitVerify() {
    if (!presentationJson) return;
    setBusy(true);
    setError(null);
    try {
      const presentation = JSON.parse(presentationJson) as unknown;
      const result = await getVerificationClient().verifyPresentation(presentation);
      router.push({
        pathname: "/result",
        params: {
          payload: JSON.stringify(result),
          mock: mockLabel ? "1" : "0",
        },
      });
    } catch (e) {
      const msg =
        e instanceof ApiError ? e.message : "Verification request failed";
      setError(msg);
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return (
      <Screen>
        <LoadingState label="Loading request…" />
      </Screen>
    );
  }

  if (error && !request) {
    return (
      <Screen>
        <ErrorState message={error} onRetry={() => void load()} />
      </Screen>
    );
  }

  if (!request) {
    return (
      <Screen>
        <ErrorState message="Unknown request" onRetry={() => router.back()} />
      </Screen>
    );
  }

  return (
    <Screen style={{ padding: 0 }}>
      <ScrollView contentContainerStyle={styles.content}>
        {step === "review" && (
          <>
            <Heading>Verification Request</Heading>
            <Card style={{ gap: spacing.sm }}>
              <Field label="Requested by" value={`${request.verifier.slice(0, 12)}…`} />
              <Field label="Audience" value={request.audience} />
              <Field
                label="Credential type"
                value={request.requestedCredentials.map((c) => c.claimType).join(", ")}
              />
              <Field label="Policy" value={request.policy.kind} />
              <Field label="Purpose" value={request.reason ?? "No reason provided"} />
              <Field
                label="Expires"
                value={new Date(request.expiresAt).toLocaleString()}
              />
            </Card>
            {expired ? <Caption>This request has expired.</Caption> : null}
            {error ? <Caption>{error}</Caption> : null}
            <PrimaryButton
              label="Continue"
              disabled={expired}
              onPress={() => setStep("select")}
            />
            <SecondaryButton label="Cancel" onPress={() => router.back()} />
          </>
        )}

        {step === "select" && (
          <>
            <Heading>Select credential</Heading>
            <Body>Choose which credential to present for this request.</Body>
            {matchingCredentials.map((c) => (
              <Pressable
                key={c.id}
                accessibilityRole="button"
                accessibilityLabel={`Select ${c.title}`}
                disabled={busy}
                onPress={() => void generatePresentation(c)}
                style={({ pressed }) => [
                  styles.credBtn,
                  pressed && { opacity: 0.85 },
                  selected?.id === c.id && styles.credSelected,
                ]}
              >
                <Body>{c.title}</Body>
                <Caption>
                  {c.issuerName} · {c.status}
                </Caption>
              </Pressable>
            ))}
            {busy ? <LoadingState label="Building presentation…" /> : null}
            {error ? <Caption>{error}</Caption> : null}
          </>
        )}

        {step === "present" && presentationJson && (
          <>
            <Heading>Presentation ready</Heading>
            <MockProofBadge />
            <Caption>
              This presentation was produced with a mock prover and is not
              cryptographically valid.
            </Caption>
            <Card>
              <Caption>{presentationJson.slice(0, 480)}…</Caption>
            </Card>
            {error ? <Caption>{error}</Caption> : null}
            <PrimaryButton
              label={busy ? "Submitting…" : "Present / Verify"}
              disabled={busy}
              onPress={() => void submitVerify()}
            />
            <SecondaryButton label="Back" onPress={() => setStep("select")} />
          </>
        )}
      </ScrollView>
    </Screen>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <View style={{ gap: 4 }}>
      <Label>{label}</Label>
      <Body>{value}</Body>
    </View>
  );
}

const styles = StyleSheet.create({
  content: {
    padding: spacing.lg,
    gap: spacing.md,
  },
  credBtn: {
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 12,
    padding: spacing.md,
    minHeight: 56,
    gap: 4,
  },
  credSelected: {
    borderColor: colors.primary,
    backgroundColor: colors.primarySoft,
  },
});
