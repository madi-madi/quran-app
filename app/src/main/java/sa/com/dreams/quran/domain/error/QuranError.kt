package sa.com.dreams.quran.domain.error

/**
 * Every failure the data layer reports to the UI. Data sources translate their own
 * exceptions (IOException, HTTP errors, SQLite errors, parse errors) into these, so
 * screens can show a localized message without knowing which API or database is used.
 */
sealed class QuranError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    /** No network, DNS failure, timeout or dropped connection. Usually worth retrying. */
    class NoConnection(cause: Throwable? = null) : QuranError("No connection", cause)

    /** The server answered with an error status. */
    class Server(val httpCode: Int, cause: Throwable? = null) : QuranError("Server error $httpCode", cause)

    /** The requested surah, ayah or edition does not exist at the source. */
    class NotFound(what: String) : QuranError("Not found: $what")

    /** The response failed parsing or integrity checks; it is never stored. */
    class InvalidData(reason: String, cause: Throwable? = null) : QuranError("Invalid data: $reason", cause)

    /** Reading or writing the local database failed. */
    class Database(cause: Throwable? = null) : QuranError("Database error", cause)

    /** Anything unexpected. */
    class Unknown(cause: Throwable? = null) : QuranError("Unknown error", cause)

    /** True when retrying later (e.g. once back online) may succeed. */
    val isRetryable: Boolean
        get() = this is NoConnection || (this is Server && httpCode >= 500)
}
