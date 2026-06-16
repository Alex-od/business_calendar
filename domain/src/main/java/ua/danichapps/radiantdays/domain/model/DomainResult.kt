package ua.danichapps.radiantdays.domain.model

/**
 * Generic result wrapper for domain operations that may fail.
 */
sealed class DomainResult<out T> {

    data class Success<out T>(val data: T) : DomainResult<T>()

    data class Error(
        val exception: Throwable,
        val messageKey: MessageKey = MessageKey.UNKNOWN,
        val messageArgs: List<String> = emptyList(),
    ) : DomainResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
}

inline fun <T> DomainResult<T>.onSuccess(action: (T) -> Unit): DomainResult<T> {
    if (this is DomainResult.Success) action(data)
    return this
}

inline fun <T> DomainResult<T>.onError(
    action: (exception: Throwable, messageKey: MessageKey, messageArgs: List<String>) -> Unit,
): DomainResult<T> {
    if (this is DomainResult.Error) action(exception, messageKey, messageArgs)
    return this
}

inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> = when (this) {
    is DomainResult.Success -> DomainResult.Success(transform(data))
    is DomainResult.Error -> this
}

fun <T> DomainResult<T>.getOrDefault(default: T): T =
    if (this is DomainResult.Success) data else default
