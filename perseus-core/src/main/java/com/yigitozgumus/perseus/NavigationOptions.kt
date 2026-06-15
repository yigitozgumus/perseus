package com.yigitozgumus.perseus

import com.yigitozgumus.perseus.key.NavigationKey
import kotlin.reflect.KClass

/** Controls how [PerseusNavigator.navigateTo] handles an existing top entry. */
public enum class LaunchMode {
    /** Always add a new entry. */
    Standard,

    /** Reuse the current top entry when it has the same route key. */
    SingleTop,
}

/** Describes entries to remove before a new navigation entry is added. */
public sealed interface PopUpTo {
    /** Remove every entry above the current stack root. */
    public data object Root : PopUpTo

    /** Remove entries above the top-most [key], optionally including the matched key. */
    public data class Key(
        public val key: NavigationKey,
        public val inclusive: Boolean = true,
    ) : PopUpTo

    /** Remove entries above the top-most [keyClass], optionally including the matched key. */
    public data class KeyType(
        public val keyClass: KClass<out NavigationKey>,
        public val inclusive: Boolean = true,
    ) : PopUpTo
}
