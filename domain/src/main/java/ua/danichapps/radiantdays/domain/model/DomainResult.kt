package ua.danichapps.radiantdays.domain.model

/**
 * Generic result wrapper for domain operations that may fail.
 *
 * Keeps error handling explicit and prevents the use of unchecked exceptions
 * across layer boundaries.
 *
 * Usage:
 * ```kotlin
 * when (val result = addEventUseCase(event)) {
 *     is DomainResult.Success -> navigate back
 *     is DomainResult.Error   -> show error
 * }
 * ```
 */
sealed class DomainResult<out T> {

    /** Operation completed successfully with [data] as the payload. */
    data class Success<out T>(val data: T) : DomainResult<T>()

    /** Operation failed. [exception] is the root cause; [message] is human-readable. */
    data class Error(
        val exception: Throwable,
        val message: String = exception.message ?: "Unknown error",
    ) : DomainResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean   get() = this is Error
}

// в”Ђв”Ђ Extension helpers в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

/** Runs [action] only when the result is [DomainResult.Success]. Chainable. */
inline fun <T> DomainResult<T>.onSuccess(action: (T) -> Unit): DomainResult<T> {
    if (this is DomainResult.Success) action(data)
    return this
}

/** Runs [action] only when the result is [DomainResult.Error]. Chainable. */
inline fun <T> DomainResult<T>.onError(
    action: (exception: Throwable, message: String) -> Unit,
): DomainResult<T> {
    if (this is DomainResult.Error) action(exception, message)
    return this
}

/** Transforms a [DomainResult.Success] value; propagates [DomainResult.Error] as-is. */
inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> = when (this) {
    is DomainResult.Success -> DomainResult.Success(transform(data))
    is DomainResult.Error   -> this
}

/** Returns [data] if [DomainResult.Success], or [default] otherwise. */
fun <T> DomainResult<T>.getOrDefault(default: T): T =
    if (this is DomainResult.Success) data else default
