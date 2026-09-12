package dev.krydo.mobile.data

/**
 * Human labels + section order for credential claim types.
 * Mirrors shared/schema claimTypeLabels (Android-side copy for UI grouping).
 */
object ClaimCategories {
    private val labels = linkedMapOf(
        "credit_score" to "Credit",
        "income_verification" to "Income",
        "identity_verification" to "Identity",
        "asset_proof" to "Assets",
        "debt_ratio" to "Debt ratio",
        "payment_history" to "Payment history",
    )

    fun labelFor(claimType: String): String =
        labels[claimType] ?: claimType
            .replace('_', ' ')
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    /** Known claim types in display order (for filter chips). */
    fun knownClaimTypes(): List<String> = labels.keys.toList()

    /** Stable section order; unknown types sort last alphabetically. */
    fun sectionOrder(claimType: String): Int {
        val idx = labels.keys.indexOf(claimType)
        return if (idx >= 0) idx else labels.size + claimType.hashCode().and(0xffff)
    }

    fun groupByCategory(credentials: List<StoredCredential>): List<Pair<String, List<StoredCredential>>> =
        credentials
            .groupBy { it.claimType }
            .entries
            .sortedWith(compareBy({ sectionOrder(it.key) }, { it.key }))
            .map { (type, list) ->
                labelFor(type) to list.sortedByDescending { it.issuedAt }
            }
}
