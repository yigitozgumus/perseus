package com.yigitozgumus.perseus

import androidx.compose.runtime.Composable
import com.yigitozgumus.perseus.key.NavigationKey
import com.yigitozgumus.perseus.provider.ScreenProvider

/** Result of the lightweight declarative graph builder. */
public data class PerseusGraph(
    val composeProviders: List<ScreenProvider<*>>,
)

/** Builds Compose screen providers with a declarative, typed registry style. */
public fun perseusGraph(block: PerseusGraphBuilder.() -> Unit): PerseusGraph =
    PerseusGraphBuilder().apply(block).build()

public class PerseusGraphBuilder internal constructor() {
    @PublishedApi
    internal val composeProviders: MutableList<ScreenProvider<*>> = mutableListOf()

    public inline fun <reified K : NavigationKey> screen(
        noinline content: @Composable (K) -> Unit,
    ): Unit {
        composeProviders += object : ScreenProvider<K> {
            override fun canProvide(key: NavigationKey): Boolean = key is K

            @Composable
            override fun Content(key: K) {
                content(key)
            }
        }
    }

    public fun build(): PerseusGraph = PerseusGraph(composeProviders.toList())
}
