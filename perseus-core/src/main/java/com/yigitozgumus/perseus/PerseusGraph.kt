package com.yigitozgumus.perseus

import androidx.compose.runtime.Composable
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.provider.ComposeScreenProvider

/** Result of the lightweight declarative graph builder. */
public data class PerseusGraph(
    val composeProviders: List<ComposeScreenProvider<*>>,
)

/** Builds Compose screen providers with a declarative, typed registry style. */
public fun perseusGraph(block: PerseusGraphBuilder.() -> Unit): PerseusGraph =
    PerseusGraphBuilder().apply(block).build()

public class PerseusGraphBuilder internal constructor() {
    @PublishedApi
    internal val composeProviders: MutableList<ComposeScreenProvider<*>> = mutableListOf()

    public inline fun <reified K : RouterKey> screen(
        noinline content: @Composable (K) -> Unit,
    ): Unit {
        composeProviders += object : ComposeScreenProvider<K> {
            override fun canProvide(key: RouterKey): Boolean = key is K

            @Composable
            override fun Content(key: K) {
                content(key)
            }
        }
    }

    public fun build(): PerseusGraph = PerseusGraph(composeProviders.toList())
}
