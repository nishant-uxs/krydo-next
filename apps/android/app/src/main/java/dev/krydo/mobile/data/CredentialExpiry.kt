package dev.krydo.mobile.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

enum class ExpiryUrgency {
    None,
    Soon,
    Expired,
}

object CredentialExpiry {
    /** Days before expiry to flag as "soon". */
    const val SOON_DAYS: Long = 30

    fun urgency(expiresAt: String?, withinDays: Long = SOON_DAYS): ExpiryUrgency {
        val end = parseInstant(expiresAt) ?: return ExpiryUrgency.None
        val now = Instant.now()
        if (end.isBefore(now)) return ExpiryUrgency.Expired
        val days = ChronoUnit.DAYS.between(now, end)
        return if (days <= withinDays) ExpiryUrgency.Soon else ExpiryUrgency.None
    }

    fun label(expiresAt: String?): String {
        val date = expiresAt?.take(10).orEmpty().ifBlank { "—" }
        return when (urgency(expiresAt)) {
            ExpiryUrgency.Expired -> "Expired $date"
            ExpiryUrgency.Soon -> "Expires soon · $date"
            ExpiryUrgency.None -> "Expires $date"
        }
    }

    private fun parseInstant(raw: String?): Instant? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        return try {
            Instant.parse(value)
        } catch (_: DateTimeParseException) {
            try {
                LocalDate.parse(value.take(10)).atStartOfDay().toInstant(ZoneOffset.UTC)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}
