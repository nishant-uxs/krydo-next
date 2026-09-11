package dev.krydo.mobile.domain

import dev.krydo.mobile.data.StoredCredential
import dev.krydo.mobile.network.PresentationRequestDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.time.Instant
import java.util.UUID

object DemoPresentationBuilder {
    private val json = Json { encodeDefaults = true }

    fun build(
        request: PresentationRequestDto,
        credential: StoredCredential,
    ): Pair<JsonElement, String> {
        val created = Instant.now().toString()
        val presentation = buildJsonObject {
            put(
                "@context",
                buildJsonArray {
                    add(JsonPrimitive("https://www.w3.org/ns/credentials/v2"))
                    add(JsonPrimitive("https://krydo.dev/credentials/v1"))
                },
            )
            put(
                "type",
                buildJsonArray {
                    add(JsonPrimitive("VerifiablePresentation"))
                    add(JsonPrimitive("KrydoPresentation"))
                },
            )
            put("id", JsonPrimitive("urn:uuid:demo-${credential.id}"))
            put("holder", JsonPrimitive("did:pkh:stellar:testnet:${credential.holderAddress}"))
            put("requestId", JsonPrimitive(request.id))
            put("challenge", JsonPrimitive(request.challenge))
            put("domain", JsonPrimitive(request.audience))
            put("created", JsonPrimitive(created))
            put("expiresAt", JsonPrimitive(request.expiresAt))
            put(
                "verifiableCredential",
                buildJsonObject {
                    put("id", JsonPrimitive("urn:uuid:${credential.id}"))
                    put("credentialHash", JsonPrimitive(credential.credentialHash))
                    put("claimType", JsonPrimitive(credential.claimType))
                    put("issuerAddress", JsonPrimitive(credential.issuerAddress))
                    put("holderAddress", JsonPrimitive(credential.holderAddress))
                    put("status", JsonPrimitive(credential.status))
                },
            )
            put(
                "proof",
                buildJsonObject {
                    put("type", JsonPrimitive("KrydoMockProof2026"))
                    put("proofId", JsonPrimitive("mock-${UUID.randomUUID()}"))
                    put("created", JsonPrimitive(created))
                    put("demo", JsonPrimitive(true))
                    put("note", JsonPrimitive("DEMO / MOCK PROOF — not cryptographically valid"))
                },
            )
            put("demo", JsonPrimitive(true))
            put("label", JsonPrimitive("DEMO / MOCK PROOF"))
        }
        return presentation to "DEMO / MOCK PROOF"
    }

    fun toPrettyJson(element: JsonElement): String =
        json.encodeToString(JsonElement.serializer(), element)
}
