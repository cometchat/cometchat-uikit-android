package com.cometchat.kotlinsampleapp.fragments.shared.views

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.shared.views.CometChatStatusIndicator.CometChatStatusIndicator
import com.cometchat.kotlinsampleapp.AppUtils
import com.cometchat.kotlinsampleapp.R

class StatusIndicatorFragment : Fragment() {
    private var parentView: LinearLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_status_indicator, container, false)
        val statusIndicator = view.findViewById<CometChatStatusIndicator>(R.id.statusIndicator)
        parentView = view.findViewById(R.id.parent_view)
        statusIndicator.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                com.cometchat.chatuikit.R.color.cometchat_online_green
            )
        )
        val statusChangeGroup = view.findViewById<RadioGroup>(R.id.toggle)
        statusIndicator.setBorderWidth(0)
        statusChangeGroup.setOnCheckedChangeListener { _: RadioGroup?, i: Int ->
            if (i == R.id.online) {
                statusIndicator.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        com.cometchat.chatuikit.R.color.cometchat_online_green
                    )
                )
            } else if (i == R.id.offline) {
                statusIndicator.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.darker_gray
                    )
                )
            }
        }
        setUpUI(view)
        return view
    }

    private fun setUpUI(view: View) {
        if (AppUtils.isNightMode(requireContext())) {
            AppUtils.changeTextColorToWhite(
                requireContext(),
                view.findViewById(R.id.status_indicator_text)
            )
            AppUtils.changeTextColorToWhite(requireContext(), view.findViewById(R.id.status_desc))
            AppUtils.changeTextColorToWhite(requireContext(), view.findViewById(R.id.status_title))
            parentView!!.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.app_background_dark
                    )
            )
        } else {
            AppUtils.changeTextColorToBlack(
                requireContext(),
                view.findViewById(R.id.status_indicator_text)
            )
            AppUtils.changeTextColorToBlack(requireContext(), view.findViewById(R.id.status_title))
            parentView!!.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.app_background
                )
            )
        }
    }
}
