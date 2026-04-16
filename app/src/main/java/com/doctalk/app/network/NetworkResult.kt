package com.doctalk.app.network

/**
 * Sealed class representing network operation results
 */
sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error<T>(val message: String, val exception: Throwable? = null) : NetworkResult<T>()
    data class Loading<T>(val isLoading: Boolean = true) : NetworkResult<T>()

    companion object {
        /**
         * Creates a success result
         */
        fun <T> success(data: T): NetworkResult<T> = Success(data)

        /**
         * Creates an error result
         */
        fun <T> error(message: String, exception: Throwable? = null): NetworkResult<T> = Error(message, exception)

        /**
         * Creates a loading result
         */
        fun <T> loading(isLoading: Boolean = true): NetworkResult<T> = Loading(isLoading)
    }
}

/**
 * Extension function to handle network results
 */
inline fun <T> NetworkResult<T>.onSuccess(action: (T) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Success) {
        action(data)
    }
    return this
}

/**
 * Extension function to handle network errors
 */
inline fun <T> NetworkResult<T>.onError(action: (String, Throwable?) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Error) {
        action(message, exception)
    }
    return this
}

/**
 * Extension function to handle loading states
 */
inline fun <T> NetworkResult<T>.onLoading(action: (Boolean) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Loading) {
        action(isLoading)
    }
    return this
}
