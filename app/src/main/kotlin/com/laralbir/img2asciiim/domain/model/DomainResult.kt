package com.laralbir.img2asciiim.domain.model

/**
 * Pure Kotlin typed sum type representing either a successful value [T] or domain error [E].
 */
sealed interface DomainResult<out T, out E> {
  data class Success<out T>(val value: T) : DomainResult<T, Nothing>
  data class Failure<out E>(val error: E) : DomainResult<Nothing, E>

  val isSuccess: Boolean
    get() = this is Success

  val isFailure: Boolean
    get() = this is Failure

  fun getOrNull(): T? = (this as? Success)?.value

  fun errorOrNull(): E? = (this as? Failure)?.error
}

inline fun <T, E, R> DomainResult<T, E>.fold(
  onSuccess: (T) -> R,
  onFailure: (E) -> R,
): R = when (this) {
  is DomainResult.Success -> onSuccess(value)
  is DomainResult.Failure -> onFailure(error)
}

typealias ImageDecodeOutcome = DomainResult<DecodedImage, ImageDecodeError>
