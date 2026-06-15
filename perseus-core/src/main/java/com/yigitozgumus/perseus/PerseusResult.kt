package com.yigitozgumus.perseus

/** One-shot result state returned by typed Perseus result APIs. */
public sealed interface PerseusResult<out T> {
    /** A result value was delivered by the child entry or scope. */
    public data class Success<T>(public val value: T) : PerseusResult<T>

    /** The child entry or scope was removed before delivering a result. */
    public data object Cancelled : PerseusResult<Nothing>
}
