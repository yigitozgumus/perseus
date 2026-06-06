package com.yigitozgumus.perseus.sample.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.yigitozgumus.perseus.api.RouterKey
import com.yigitozgumus.perseus.api.ScreenProvider
import com.yigitozgumus.perseus.sample.keys.ProfileKey

class ProfileFragmentProvider : ScreenProvider<ProfileKey> {
    override fun canProvide(key: RouterKey) = key is ProfileKey

    override fun provide(key: ProfileKey): Fragment = ProfileFragment()
}

class ProfileFragment : Fragment() {

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
                text = "This is a legacy Fragment wrapped by Perseus.\nIt receives its RouterKey via fragment arguments."
                textSize = 14f
                setPadding(0, 32, 0, 32)
            })

            addView(Button(context).apply {
                text = "Send Result Back"
                setOnClickListener {
                    // Demonstrates fragment sending a result
                    parentFragmentManager.setFragmentResult("profile_result", Bundle().apply {
                        putString("message", "Result from ProfileFragment")
                    })
                }
            })
        }
    }
}
