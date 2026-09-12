package dev.krydo.mobile.network

import dev.krydo.mobile.BuildConfig

object ZkShareLinks {
    fun verifyUrl(proofId: String): String {
        val base = BuildConfig.WEB_APP_URL.trimEnd('/')
        return "$base/verify/$proofId"
    }

    /** Accepts raw UUID, /verify/{id}, or full krydo verify URL. */
    fun parseProofId(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        val uuid = Regex(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
        ).find(trimmed)?.value
        if (uuid != null) return uuid
        val path = trimmed.substringAfterLast("/verify/", missingDelimiterValue = "")
            .substringBefore('?')
            .substringBefore('#')
            .trim()
        return path.takeIf { it.isNotBlank() && it.length >= 8 }
    }
}
