package com.yigitozgumus.perseus.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation3.runtime.NavEntry
import com.yigitozgumus.perseus.api.PerseusNavigator
import com.yigitozgumus.perseus.api.RouterKey
import com.yigitozgumus.perseus.impl.PerseusEntryProviderRegistry
import com.yigitozgumus.perseus.impl.PerseusNavigationStateHolder
import com.yigitozgumus.perseus.impl.PerseusNavHost
import com.yigitozgumus.perseus.sample.di.infrastructureModule
import com.yigitozgumus.perseus.sample.keys.HomeKey
import org.koin.ksp.generated.com_yigitozgumus_perseus_sample_di_SampleModule
import com.yigitozgumus.perseus.sample.keys.ProfileKey
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.activityRetainedScope
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.scope.Scope
import org.koin.dsl.koinApplication

class SampleActivity : FragmentActivity(), AndroidScopeComponent, KoinComponent {

    companion object {
        private val localKoin = koinApplication {
            modules(
                com_yigitozgumus_perseus_sample_di_SampleModule,
                infrastructureModule
            )
        }.koin
    }

    override fun getKoin(): Koin = localKoin
    override val scope: Scope by activityRetainedScope()

    private val navigator: PerseusNavigator by inject()
    private val stateHolder: PerseusNavigationStateHolder by inject()
    private val entryRegistry: PerseusEntryProviderRegistry by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tabRoots = listOf<RouterKey>(HomeKey, ProfileKey)
        stateHolder.transitionToAuthenticated(tabRoots)

        setContent {
            var selectedTab by rememberSaveable { mutableIntStateOf(0) }

            PerseusNavHost(
                stateHolder = stateHolder,
                entryRegistry = entryRegistry,
                onPop = { navigator.pop() },
                initialKey = HomeKey,
                bottomBar = { currentIndex, onTabSelected ->
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentIndex == 0,
                            onClick = { onTabSelected(0) },
                            icon = { Icon(Icons.Default.Home, "Home") },
                            label = { Text("Home") }
                        )
                        NavigationBarItem(
                            selected = currentIndex == 1,
                            onClick = { onTabSelected(1) },
                            icon = { Icon(Icons.Default.Person, "Profile") },
                            label = { Text("Profile") }
                        )
                    }
                },
                onTabChanged = { selectedTab = it },
                onSwitchTab = { navigator.switchTab(it) },
                onResetCurrentTab = { navigator.resetCurrentTab(resetRoot = false) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
