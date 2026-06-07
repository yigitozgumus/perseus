package com.yigitozgumus.perseus.sample.recipe.interop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.dropUnlessResumed
import com.yigitozgumus.perseus.PerseusNavHost
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.interop.DefaultFragmentEntryFactory
import com.yigitozgumus.perseus.interop.ScreenProvider
import com.yigitozgumus.perseus.key.RouterKey
import com.yigitozgumus.perseus.provider.ComposeScreenProvider
import com.yigitozgumus.perseus.provider.FragmentProviderMarker
import com.yigitozgumus.perseus.sample.keys.FragmentScreenKey
import com.yigitozgumus.perseus.sample.keys.HomeKey
import com.yigitozgumus.perseus.sample.recipe.createController

@OptIn(ExperimentalMaterial3Api::class)
class InteropActivity : FragmentActivity() {

    private val controller = createController(
        composeProviders = listOf(InteropHomeProvider()),
        fragmentProviders = listOf(SampleFragmentProvider()),
        fragmentEntryFactory = DefaultFragmentEntryFactory,
    )
    private val navigator: PerseusNavigator = controller.navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PerseusNavHost(
                controller = controller,
                initialKey = HomeKey,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    inner class InteropHomeProvider : ComposeScreenProvider<HomeKey> {
        override fun canProvide(key: RouterKey) = key is HomeKey

        @Composable
        override fun Content(key: HomeKey) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Fragment Interop") },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                ) {
                    item {
                        Text(
                            text = "Open Fragment Screen",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .clickable(
                                    onClick = dropUnlessResumed {
                                        navigator.navigateTo(FragmentScreenKey)
                                    }
                                )
                                .padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class SampleFragmentProvider : ScreenProvider<FragmentScreenKey> {
    override fun canProvide(key: RouterKey) = key is FragmentScreenKey
    override fun provide(key: FragmentScreenKey): Fragment = InteropFragment()
}

@OptIn(ExperimentalMaterial3Api::class)
class InteropFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, state: Bundle?,
    ): View = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(48, 48, 48, 48)
        addView(TextView(context).apply {
            text = "Fragment Screen"
            textSize = 24f
        })
        addView(TextView(context).apply {
            text = "This Fragment is wrapped by Perseus\nand rendered inside a NavDisplay."
            textSize = 14f
            setPadding(0, 32, 0, 32)
        })
    }
}
