package dev.krydo.mobile.data

data class StoredCredential(
    val id: String,
    val title: String,
    val claimType: String,
    val issuerName: String,
    val issuerAddress: String,
    val holderAddress: String,
    val holderName: String,
    val status: String,
    val issuedAt: String,
    val expiresAt: String?,
    val credentialHash: String,
    val displaySummary: String,
)

object DemoData {
    const val HOLDER_NAME = "Alex Demo"
    const val HOLDER_ADDRESS = "GBRPYHIL2CI3FNQ4BXLFMNDLFJUNPU2HY3ZMFSHONUCEOASW7QC7OX2H"
    const val DEMO_REQUEST_ID = "req_demo001"

    val credentials: List<StoredCredential> = listOf(
        StoredCredential(
            id = "11111111-1111-4111-8111-111111111111",
            title = "B.Tech Computer Science",
            claimType = "identity_verification",
            issuerName = "Example University",
            issuerAddress = "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
            holderAddress = HOLDER_ADDRESS,
            holderName = HOLDER_NAME,
            status = "active",
            issuedAt = "2024-06-15T00:00:00.000Z",
            expiresAt = "2030-06-15T00:00:00.000Z",
            credentialHash = "a1".repeat(32),
            displaySummary = "Degree credential (demo)",
        ),
        StoredCredential(
            id = "22222222-2222-4222-8222-222222222222",
            title = "Credit Score Range",
            claimType = "credit_score",
            issuerName = "Example Credit Bureau",
            issuerAddress = "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWHF",
            holderAddress = HOLDER_ADDRESS,
            holderName = HOLDER_NAME,
            status = "active",
            issuedAt = "2025-01-10T00:00:00.000Z",
            expiresAt = "2027-01-10T00:00:00.000Z",
            credentialHash = "b2".repeat(32),
            displaySummary = "Score band credential (demo)",
        ),
    )
}
