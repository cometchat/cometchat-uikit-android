package com.cometchat.kotlinsampleapp.fragments.shared.resources

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.shared.resources.theme.CometChatTheme
import com.cometchat.chatuikit.shared.resources.theme.Palette
import com.cometchat.chatuikit.shared.resources.theme.Typography
import com.cometchat.kotlinsampleapp.AppUtils
import com.cometchat.kotlinsampleapp.R
import com.cometchat.kotlinsampleapp.activity.ComponentLaunchActivity

class ThemeFragment : Fragment() {
    var palette: Palette? = null

    @ColorInt
    var accent = 0

    @ColorInt
    var background = 0

    @ColorInt
    var primary = 0

    @StyleRes
    var heading = 0

    @StyleRes
    var name = 0

    @StyleRes
    var text1 = 0

    @StyleRes
    var text2 = 0
    private var parentView: LinearLayout? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_theme, container, false)
        palette = Palette.getInstance()
        parentView = view.findViewById(R.id.parent_view)
        setUpUI(view)
        setTheme(true)
        val radioGroup = view.findViewById<RadioGroup>(R.id.toggle)
        radioGroup.setOnCheckedChangeListener { _: RadioGroup?, i: Int ->
            if (i == R.id.defaultTheme) {
                setTheme(true)
            } else if (i == R.id.customTheme) {
                setTheme(false)
            }
        }
        view.findViewById<View>(R.id.button).setOnClickListener {
            val intent = Intent(
                activity, ComponentLaunchActivity::class.java
            )
            intent.putExtra("component", R.id.userWithMessages)
            startActivity(intent)
        }
        return view
    }

    private fun setTheme(isDefault: Boolean) {
        if (isDefault) {
            accent = ContextCompat.getColor(
                requireContext(), com.cometchat.chatuikit.R.color.cometchat_accent
            )
            background = ContextCompat.getColor(requireContext(), R.color.white)
            primary = ContextCompat.getColor(
                requireContext(), com.cometchat.chatuikit.R.color.cometchat_accent
            )
            heading = R.style.Heading
            name = R.style.Name
            text1 = R.style.Text1
            text2 = R.style.Text2
        } else {
            accent = ContextCompat.getColor(
                requireContext(), com.cometchat.chatuikit.R.color.cometchat_online_green
            )
            background = Color.parseColor("#021E20")
            primary = ContextCompat.getColor(requireContext(), android.R.color.black)
            heading = R.style.AppHeading
            name = R.style.AppName
            text1 = R.style.AppText1
            text2 = R.style.AppText2
        }
        palette!!.background(background)
        palette!!.accent(accent)
        palette!!.primary(primary)
        palette!!.accent50(ColorUtils.setAlphaComponent(accent, 10))
        palette!!.accent100(ColorUtils.setAlphaComponent(accent, 20))
        palette!!.accent200(ColorUtils.setAlphaComponent(accent, 38))
        palette!!.accent300(ColorUtils.setAlphaComponent(accent, 61))
        palette!!.accent400(ColorUtils.setAlphaComponent(accent, 84))
        palette!!.accent500(ColorUtils.setAlphaComponent(accent, 117))
        palette!!.accent600(ColorUtils.setAlphaComponent(accent, 148))
        palette!!.accent700(ColorUtils.setAlphaComponent(accent, 176))
        palette!!.accent800(ColorUtils.setAlphaComponent(accent, 209))
        palette!!.secondary(
            ContextCompat.getColor(
                requireContext(), com.cometchat.chatuikit.R.color.cometchat_secondary
            )
        )
        palette!!.accent900(ColorUtils.setAlphaComponent(accent, 255))
        val typography = Typography.getInstance()
        typography.heading = heading
        typography.name = name
        typography.text1 = text1
        typography.text2 = text2
        val cometChatTheme = CometChatTheme.getInstance()
        cometChatTheme.palette = palette
        cometChatTheme.typography = typography
    }

    private fun setUpUI(view: View) {
        if (AppUtils.isNightMode(requireContext())) {
            AppUtils.changeTextColorToWhite(context, view.findViewById(R.id.theme_text))
            AppUtils.changeTextColorToWhite(context, view.findViewById(R.id.theme_text_2))
            AppUtils.changeTextColorToWhite(context, view.findViewById(R.id.theme_text_description))
            parentView!!.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.app_background_dark
                )
            )
        } else {
            AppUtils.changeTextColorToBlack(context, view.findViewById(R.id.theme_text))
            AppUtils.changeTextColorToBlack(context, view.findViewById(R.id.theme_text_2))
            parentView!!.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(), R.color.app_background
                )
            )
        }
    }
}
