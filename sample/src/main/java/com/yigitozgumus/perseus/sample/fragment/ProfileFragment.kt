package com.yigitozgumus.perseus.sample.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.yigitozgumus.perseus.api.NavigationContext
import com.yigitozgumus.perseus.api.PerseusNavigator
import com.yigitozgumus.perseus.api.RouterKey
import com.yigitozgumus.perseus.api.ScreenProvider
import com.yigitozgumus.perseus.api.getNavigationContext
import com.yigitozgumus.perseus.sample.keys.ProfileKey
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

@Single
class ProfileFragmentProvider : ScreenProvider<ProfileKey> {
    override fun canProvide(key: RouterKey) = key is ProfileKey

    override fun provide(key: ProfileKey): Fragment = ProfileFragment()
}

class ProfileFragment : Fragment(), KoinComponent {

    private val navigator: PerseusNavigator by inject()
    private val navContext: NavigationContext<ProfileKey> by lazy {
        requireArguments().getNavigationContext()
    }

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
                text = "This is a legacy Fragment wrapped by Perseus.\nNavigated via: ${navContext.key::class.simpleName}"
                textSize = 14f
                setPadding(0, 32, 0, 32)
            })

            addView(Button(context).apply {
                text = "Send Result Back"
                setOnClickListener {
                    navigator.sendResult(navContext, "Result from ProfileFragment!")
                    navigator.pop()
                }
            })
        }
    }
}
