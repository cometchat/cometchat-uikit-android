package com.cometchat.kotlinsampleapp.fragments.shared.views

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.views.CometChatAvatar.CometChatAvatar
import com.cometchat.kotlinsampleapp.AppUtils
import com.cometchat.kotlinsampleapp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AvatarFragment : Fragment() {
    private lateinit var cornerRadiusLayout: TextInputLayout
    private lateinit var parentView: LinearLayout
    private lateinit var avatar: CometChatAvatar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_avatar, container, false)
        avatar = view.findViewById(R.id.avataricon)
        parentView = view.findViewById(R.id.parent_view)
        val loggedInUserName = view.findViewById<TextView>(R.id.loggedInUserName)
        if (CometChatUIKit.getLoggedInUser() != null) {
            loggedInUserName.text = "Name : " + CometChatUIKit.getLoggedInUser().name
            avatar.setImage(
                CometChatUIKit.getLoggedInUser().avatar,
                CometChatUIKit.getLoggedInUser().name
            ) // set Logged in user image to Avatar
        }
        avatar.setBorderWidth(10) // set Avatar Border Width
        avatar.setCornerRadius(0f) // set Avatar Corner Radius
        avatar.elevation = 0f
        avatar.setTextAppearance(androidx.appcompat.R.style.Base_TextAppearance_AppCompat_Large)
        cornerRadiusLayout = view.findViewById(R.id.borderRadiusLayout)
        val radioGroup = view.findViewById<RadioGroup>(R.id.toggle)
        radioGroup.setOnCheckedChangeListener { _: RadioGroup?, i: Int ->
            if (i == R.id.image) {
                avatar.setImage(CometChatUIKit.getLoggedInUser().avatar)
            } else if (i == R.id.name) {
                avatar.setName(CometChatUIKit.getLoggedInUser().name)
            }
        }
        val cornerRadius = view.findViewById<TextInputEditText>(R.id.borderRadius)
        cornerRadiusLayout = view.findViewById(R.id.borderRadiusLayout)
        cornerRadius.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                try {
                    val radius = charSequence.toString().toInt()
                    avatar.radius = radius.toFloat()
                } catch (e: Exception) {
                    avatar.radius = 0f
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        setUpUI(view)
        return view
    }

    private fun setUpUI(view: View) {
        if (AppUtils.isNightMode(requireContext())) {
            AppUtils.changeTextColorToWhite(requireContext(), view.findViewById(R.id.avatar_text))
            AppUtils.changeTextColorToWhite(
                requireContext(),
                view.findViewById(R.id.avatar_text_description)
            )
            AppUtils.changeTextColorToWhite(
                requireContext(),
                view.findViewById(R.id.avatar_text_toggle)
            )
            AppUtils.changeTextColorToWhite(
                requireContext(),
                view.findViewById(R.id.loggedInUserName)
            )
            avatar.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.app_background_dark
                )
            )
            cornerRadiusLayout.setBoxStrokeColorStateList(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                    )
                )
            )
            cornerRadiusLayout.hintTextColor =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
            cornerRadiusLayout.editText!!.setTextColor(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                    )
                )
            )
            parentView.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.app_background_dark
                    )
            )
        } else {
            AppUtils.changeTextColorToBlack(requireContext(), view.findViewById(R.id.avatar_text))
            AppUtils.changeTextColorToBlack(
                requireContext(),
                view.findViewById(R.id.avatar_text_toggle)
            )
            AppUtils.changeTextColorToBlack(
                requireContext(),
                view.findViewById(R.id.loggedInUserName)
            )
            parentView.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.app_background
                    )
            )
        }
    }
}
