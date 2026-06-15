package com.yigitozgumus.perseus

import com.yigitozgumus.perseus.key.NavigationKey
import java.util.UUID

/** Durable identifier for a navigation stack scope. */
@JvmInline
public value class StackScopeId(public val value: String) {
    public companion object {
        public fun create(): StackScopeId = StackScopeId(UUID.randomUUID().toString())
    }
}

/** Describes a stack scope to create or replace. */
public sealed interface StackScopeSpec {
    public val id: StackScopeId?
    public val backBehavior: PerseusBackBehavior
}

/** A scope with one back stack. */
public data class SingleStackSpec(
    val initialKey: NavigationKey,
    override val id: StackScopeId? = null,
    val restorePolicy: ScopeRestorePolicy = ScopeRestorePolicy.RestoreSavedState,
    override val backBehavior: PerseusBackBehavior = PerseusBackBehavior(),
) : StackScopeSpec

/** A scope with multiple sibling back stacks. */
public data class MultiStackSpec(
    val rootKeys: List<NavigationKey>,
    val initialStackIndex: Int = 0,
    override val id: StackScopeId? = null,
    val restorePolicy: ScopeRestorePolicy = ScopeRestorePolicy.RestoreSavedState,
    override val backBehavior: PerseusBackBehavior = PerseusBackBehavior(),
) : StackScopeSpec {
    init {
        require(rootKeys.isNotEmpty()) { "MultiStackSpec requires at least one root key." }
        require(initialStackIndex in rootKeys.indices) { "initialStackIndex must be within rootKeys indices." }
    }
}

/** Public snapshot of the current stack scope. */
public data class StackScopeSnapshot(
    val id: StackScopeId,
    val kind: StackScopeKind,
    val currentStackIndex: Int?,
    val rootKeys: List<NavigationKey>,
    val currentBackStack: List<NavigationKey>,
    val backBehavior: PerseusBackBehavior = PerseusBackBehavior(),
)

public enum class StackScopeKind {
    SingleStack,
    MultiStack,
}
