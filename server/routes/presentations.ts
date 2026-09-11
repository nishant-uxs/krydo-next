import type { Express } from "express";
import { z } from "zod";
import {
  createPresentation,
  createPresentationRequestBodySchema,
  presentationRequestHttpsLink,
  verifiablePresentationSchema,
} from "@shared/presentation";
import { requireAuth } from "../auth/jwt";
import { sensitiveLimiter } from "../middleware/security";
import { storage } from "../storage";
import {
  getPresentationRequest,
  issuePresentationRequest,
  toPublicPresentationRequest,
} from "../presentations/request-store";
import { verifyPresentation } from "../presentations/verify";
import { assertNoClaimLeak } from "../privacy/public-credential";
import { childLogger } from "../logger";

const log = childLogger("routes/presentations");

/**
 * Verifiable Presentation protocol routes.
 *
 *   POST /api/presentations/request          — auth: create request + challenge
 *   GET  /api/presentations/request/:id      — public: opaque-id retrieval
 *   POST /api/presentations/create           — auth holder: build VP object
 *   POST /api/presentations/verify           — public: verify VP + consume challenge
 *
 * Privacy: verify responses never include claimData / claimSummary.
 */
export function registerPresentationRoutes(app: Express) {
  /**
   * Create a Presentation Request.
   * Verifier identity and challenge are server-authored — client values ignored.
   */
  app.post(
    "/api/presentations/request",
    requireAuth,
    sensitiveLimiter,
    async (req, res) => {
      try {
        const body = createPresentationRequestBodySchema.parse(req.body ?? {});
        const verifier = req.auth!.sub;
        const audience = body.audience ?? verifier;

        // Policy claimType must appear in requestedCredentials.
        if (
          !body.requestedCredentials.some(
            (r) => r.claimType === body.policy.claimType,
          )
        ) {
          return res.status(400).json({
            message: "policy.claimType must be listed in requestedCredentials",
          });
        }

        const record = issuePresentationRequest({
          verifier,
          audience,
          reason: body.reason ?? null,
          requestedCredentials: body.requestedCredentials,
          policy: body.policy,
          ttlSeconds: body.ttlSeconds,
        });

        const origin =
          typeof req.headers.origin === "string" && req.headers.origin.length > 0
            ? req.headers.origin
            : `${req.protocol}://${req.get("host")}`;

        const publicRequest = toPublicPresentationRequest(record);
        res.status(201).json({
          ...publicRequest,
          httpsLink: presentationRequestHttpsLink(origin, record.id),
        });
      } catch (error: unknown) {
        if (error instanceof z.ZodError) {
          return res.status(400).json({ message: error.issues[0]?.message ?? "Invalid request" });
        }
        log.error(
          { err: error instanceof Error ? error.message : String(error) },
          "create presentation request failed",
        );
        res.status(500).json({ message: "Failed to create presentation request" });
      }
    },
  );

  /**
   * Retrieve a presentation request by opaque ID.
   * Public by design (wallet scan / deep-link). Contains policy metadata +
   * challenge — never credential claims.
   */
  app.get("/api/presentations/request/:requestId", async (req, res) => {
    try {
      const requestId = req.params.requestId as string;
      if (
        !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
          requestId,
        )
      ) {
        return res.status(400).json({ message: "Invalid request id" });
      }

      const record = getPresentationRequest(requestId);
      if (!record) {
        return res.status(404).json({ message: "Presentation request not found" });
      }

      if (new Date(record.expiresAt).getTime() <= Date.now()) {
        return res.status(410).json({ message: "Presentation request expired" });
      }

      if (record.challengeConsumed) {
        return res.status(410).json({ message: "Presentation request already used" });
      }

      const publicRequest = toPublicPresentationRequest(record);
      // Defense: never leak claimData through this path.
      assertNoClaimLeak(publicRequest);
      res.json(publicRequest);
    } catch (error: unknown) {
      log.error(
        { err: error instanceof Error ? error.message : String(error) },
        "get presentation request failed",
      );
      res.status(500).json({ message: "Failed to load presentation request" });
    }
  });

  /**
   * Build a VP for an authenticated holder.
   * Protocol helper for tests + future wallet; does not use holder private key.
   */
  app.post(
    "/api/presentations/create",
    requireAuth,
    sensitiveLimiter,
    async (req, res) => {
      try {
        const schema = z.object({
          requestId: z.string().uuid(),
          credentialId: z.string().uuid(),
          proofId: z.string().uuid().optional(),
        });
        const data = schema.parse(req.body ?? {});
        const holder = req.auth!.sub;

        const request = getPresentationRequest(data.requestId);
        if (!request) {
          return res.status(404).json({ message: "Presentation request not found" });
        }
        if (new Date(request.expiresAt).getTime() <= Date.now()) {
          return res.status(410).json({ message: "Presentation request expired" });
        }
        if (request.challengeConsumed) {
          return res.status(410).json({ message: "Presentation request already used" });
        }

        const credential = await storage.getCredentialById(data.credentialId);
        if (!credential) {
          return res.status(404).json({ message: "Credential not found" });
        }
        if (credential.holderAddress !== holder) {
          return res.status(403).json({
            message: "Only the credential holder can create a presentation",
          });
        }

        let zkProof: { id: string; proofType: string; commitment: string } | undefined;
        if (data.proofId) {
          const proof = await storage.getZkProof(data.proofId);
          if (!proof) {
            return res.status(404).json({ message: "ZK proof not found" });
          }
          if (proof.proverAddress !== holder) {
            return res.status(403).json({ message: "ZK proof not owned by holder" });
          }
          if (proof.credentialId !== credential.id) {
            return res.status(400).json({
              message: "ZK proof is not bound to the selected credential",
            });
          }
          zkProof = {
            id: proof.id,
            proofType: proof.proofType,
            commitment: proof.commitment,
          };
        }

        const vp = createPresentation({
          request: toPublicPresentationRequest(request),
          holderAddress: holder,
          credential: {
            id: credential.id,
            credentialHash: credential.credentialHash,
            claimType: credential.claimType,
            issuerAddress: credential.issuerAddress,
            holderAddress: credential.holderAddress,
          },
          zkProof,
        });

        // VP must not embed claimData.
        assertNoClaimLeak(vp);
        res.status(201).json(vp);
      } catch (error: unknown) {
        if (error instanceof z.ZodError) {
          return res.status(400).json({ message: error.issues[0]?.message ?? "Invalid body" });
        }
        const message = error instanceof Error ? error.message : "Failed to create presentation";
        if (
          /does not match|requires a ZK|expired|not in requested/i.test(message)
        ) {
          return res.status(400).json({ message });
        }
        log.error({ err: message }, "create presentation failed");
        res.status(500).json({ message: "Failed to create presentation" });
      }
    },
  );

  /**
   * Verify a Verifiable Presentation.
   * Public endpoint — returns minimal status, never claimData.
   */
  app.post("/api/presentations/verify", async (req, res) => {
    try {
      const body = z
        .object({
          presentation: z.unknown(),
          // Optional echo of requestId for clients that wrap the VP.
          requestId: z.string().uuid().optional(),
        })
        .parse(req.body ?? {});

      // If a top-level requestId is provided, it must match the VP.
      if (
        body.requestId &&
        typeof body.presentation === "object" &&
        body.presentation !== null &&
        "requestId" in body.presentation &&
        (body.presentation as { requestId?: string }).requestId !== body.requestId
      ) {
        return res.status(400).json({
          valid: false,
          message: "requestId does not match presentation",
        });
      }

      // Soft structural hint before full verify (better 400s).
      const soft = verifiablePresentationSchema.safeParse(body.presentation);
      if (!soft.success && body.presentation === undefined) {
        return res.status(400).json({ message: "presentation is required" });
      }

      const result = await verifyPresentation(body.presentation);
      assertNoClaimLeak(result);

      const status = result.valid ? 200 : 400;
      res.status(status).json(result);
    } catch (error: unknown) {
      if (error instanceof z.ZodError) {
        return res.status(400).json({ message: error.issues[0]?.message ?? "Invalid body" });
      }
      if (error instanceof Error && /Sensitive field leaked/.test(error.message)) {
        log.error({ err: error.message }, "presentation verify privacy guard tripped");
        return res.status(500).json({ message: "Verification failed" });
      }
      log.error(
        { err: error instanceof Error ? error.message : String(error) },
        "presentation verify failed",
      );
      res.status(500).json({ message: "Verification failed" });
    }
  });
}
