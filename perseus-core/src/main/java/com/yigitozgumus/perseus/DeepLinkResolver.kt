package com.yigitozgumus.perseus

import android.net.Uri
import com.yigitozgumus.perseus.key.RouterKey

/** Resolves an external URI into a Perseus navigation target. */
public fun interface DeepLinkResolver {
    public fun resolve(uri: Uri): DeepLinkTarget?
}

public sealed interface DeepLinkTarget {
    public data class Key(val key: RouterKey) : DeepLinkTarget
    public data class Scope(val scope: StackScopeSpec) : DeepLinkTarget
}

public fun PerseusNavigator.handleDeepLink(
    uri: Uri,
    resolver: DeepLinkResolver,
): NavigationHandle? = when (val target = resolver.resolve(uri)) {
    is DeepLinkTarget.Key -> navigateTo(target.key)
    is DeepLinkTarget.Scope, null -> null
}

public fun PerseusScopeNavigator.handleDeepLink(
    uri: Uri,
    resolver: DeepLinkResolver,
): Boolean = when (val target = resolver.resolve(uri)) {
    is DeepLinkTarget.Scope -> {
        setRootScope(target.scope)
        true
    }
    is DeepLinkTarget.Key, null -> false
}
