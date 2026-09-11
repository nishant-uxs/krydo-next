package dev.krydo.mobile.data

class CredentialRepository {
    fun list(): List<StoredCredential> = DemoData.credentials

    fun get(id: String): StoredCredential? = DemoData.credentials.find { it.id == id }

    fun matchForClaim(claimType: String): List<StoredCredential> =
        DemoData.credentials.filter { it.claimType == claimType && it.status == "active" }
}
