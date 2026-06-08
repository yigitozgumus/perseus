package com.yigitozgumus.perseus.sample

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import com.yigitozgumus.perseus.MultiStackSpec
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.PerseusNavHost
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.sample.di.SampleModule
import com.yigitozgumus.perseus.sample.di.infrastructureModule
import com.yigitozgumus.perseus.sample.keys.HomeKey
import com.yigitozgumus.perseus.sample.keys.ProfileKey
import com.yigitozgumus.perseus.sample.keys.SearchKey
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.ksp.generated.com_yigitozgumus_perseus_sample_di_SampleModule
import org.koin.ksp.generated.module

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SampleApplication)
            modules(
                SampleModule().module,
                infrastructureModule
            )
        }
    }
}

class SampleActivity : FragmentActivity(), KoinComponent {

    private val navigator: PerseusNavigator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        navigator.setRootScope(MultiStackSpec(listOf(HomeKey, SearchKey, ProfileKey)))

        setContent {
            var selectedTab by rememberSaveable { mutableIntStateOf(0) }

            PerseusNavHost(
                navigator = navigator,
                initialKey = HomeKey,
                modifier = Modifier.fillMaxSize(),
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
                            icon = { Icon(Icons.Default.Search, "Search") },
                            label = { Text("Search") }
                        )
                        NavigationBarItem(
                            selected = currentIndex == 2,
                            onClick = { onTabSelected(2) },
                            icon = { Icon(Icons.Default.Person, "Profile") },
                            label = { Text("Profile") }
                        )
                    }
                },
                onTabChanged = { selectedTab = it },
            )
        }
    }
}
