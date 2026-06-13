package com.yigitozgumus.perseus.sample.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.yigitozgumus.perseus.NavigationContext
import com.yigitozgumus.perseus.PerseusNavigator
import com.yigitozgumus.perseus.getNavigationContext
import com.yigitozgumus.perseus.interop.ScreenProvider
import com.yigitozgumus.perseus.interop.requirePerseusViewModelStoreOwner
import com.yigitozgumus.perseus.key.NavigationKey
import com.yigitozgumus.perseus.sample.keys.ProfileKey
import java.util.UUID
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.annotation.Single
import org.koin.core.parameter.parametersOf

@Single
class ProfileFragmentProvider : ScreenProvider<ProfileKey> {
    override fun canProvide(key: NavigationKey) = key is ProfileKey

    override fun provide(key: ProfileKey): Fragment = ProfileFragment()
}

class ProfileFragment : Fragment() {

    private val navContext: NavigationContext<ProfileKey> by lazy {
        requireArguments().getNavigationContext()
    }

    private val viewModel: ProfileViewModel by viewModel(
        ownerProducer = { requirePerseusViewModelStoreOwner() },
        parameters = { parametersOf(navContext.key) },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)

            addView(TextView(context).apply {
                text = "Profile (Fragment)"
                textSize = 24f
            })

            addView(TextView(context).apply {
                text = buildString {
                    appendLine("This is a legacy Fragment wrapped by Perseus.")
                    appendLine("Navigated via: ${navContext.key::class.simpleName}")
                    appendLine("Koin ViewModel id: ${viewModel.instanceId}")
                    append("The ViewModel is scoped to the Perseus entry, not the Fragment instance.")
                }
                textSize = 14f
                setPadding(0, 32, 0, 32)
            })

            addView(Button(context).apply {
                text = "Send Result Back"
                setOnClickListener {
                    viewModel.sendResult(navContext)
                }
            })
        }
    }
}

class ProfileViewModel(
    private val navigator: PerseusNavigator,
    private val key: ProfileKey,
) : ViewModel() {

    val instanceId: String = UUID.randomUUID().toString().take(8)

    fun sendResult(context: NavigationContext<ProfileKey>) {
        navigator.sendResult(context, "Result from ProfileFragment ${key::class.simpleName} vm=$instanceId")
        navigator.pop()
    }
}
