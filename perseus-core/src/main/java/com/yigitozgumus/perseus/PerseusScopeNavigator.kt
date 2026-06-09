package com.yigitozgumus.perseus

/**
 * Scope-level navigation controls for replacing and stacking navigation containers.
 *
 * A scope is a navigation container, such as a [SingleStackSpec] or [MultiStackSpec].
 * Use this interface for app-flow ownership concerns like replacing Login with the
 * main tabbed app, or pushing/removing a temporary nested flow above the current app.
 */
public interface PerseusScopeNavigator {
    /** Snapshot of the active stack scope. */
    public val currentScope: StackScopeSnapshot

    /** Replaces the root scope and removes all existing scopes. */
    public fun setRootScope(scope: StackScopeSpec)

    /** Semantic alias for [setRootScope] for app/session transitions. */
    public fun replaceApp(scope: StackScopeSpec)

    /** Replaces the current top scope. */
    public fun replaceCurrentScope(scope: StackScopeSpec)

    /** Pushes a new scope above the current scope and returns its id. */
    public fun pushScope(scope: StackScopeSpec): StackScopeId

    /** Pushes a new scope above the current scope and returns a handle for its result. */
    public fun pushScopeForResult(scope: StackScopeSpec): ScopeNavigationHandle

    /** Removes a non-root scope and cleans all entries owned by it. */
    public fun removeScope(scopeId: StackScopeId)

    /** Removes a non-root scope and optionally sends a result to its [ScopeNavigationHandle]. */
    public fun <R : Any> removeScope(scopeId: StackScopeId, result: R)
}

public interface ScopeNavigationHandle : NavigationHandle {
    public val scopeId: StackScopeId
}
