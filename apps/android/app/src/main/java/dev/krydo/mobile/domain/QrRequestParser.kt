package dev.krydo.mobile.domain

object QrRequestParser {
    data class ParseResult(val requestId: String)

    fun parse(raw: String): Result<ParseResult> {
        val input = raw.trim()
        if (input.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a request id or krydo:// URI"))
        }

        val lower = input.lowercase()
        if (
            lower.contains(":8081") ||
            lower.contains("metro") ||
            lower.matches(Regex("""https?://.*(localhost|127\.0\.0\.1|10\.\d+\.\d+\.\d+).*"""))
        ) {
            return Result.failure(
                IllegalArgumentException(
                    "That looks like a Metro / bundler URL. Paste krydo://present?request=… or a request id instead.",
                ),
            )
        }

        if (input.startsWith("krydo://", ignoreCase = true) ||
            input.startsWith("https://krydo.dev/", ignoreCase = true) ||
            input.startsWith("https://www.krydo.in/", ignoreCase = true) ||
            input.startsWith("https://krydo.in/", ignoreCase = true)
        ) {
            val uri = android.net.Uri.parse(input)
            val id = uri.getQueryParameter("request")
                ?: uri.lastPathSegment?.takeIf { it.isNotBlank() && it != "present" }
            if (id.isNullOrBlank()) {
                return Result.failure(IllegalArgumentException("Deep link missing request id"))
            }
            return Result.success(ParseResult(id))
        }

        if (input.contains("://")) {
            return Result.failure(
                IllegalArgumentException("Unsupported URI. Use krydo://present?request=<id>"),
            )
        }

        return Result.success(ParseResult(input))
    }
}
