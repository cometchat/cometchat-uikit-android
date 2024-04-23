package com.cometchat.kotlinsampleapp.fragments.shared.views

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.resources.theme.CometChatTheme
import com.cometchat.chatuikit.shared.utils.ConversationTailView
import com.cometchat.chatuikit.shared.views.CometChatBadge.BadgeStyle
import com.cometchat.chatuikit.shared.views.CometChatDate.DateStyle
import com.cometchat.chatuikit.shared.views.CometChatDate.Pattern
import com.cometchat.chatuikit.shared.views.CometChatListItem.CometChatListItem
import com.cometchat.kotlinsampleapp.AppUtils
import com.cometchat.kotlinsampleapp.R

class ListItemFragment : Fragment() {
    private var parentView: LinearLayout? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_list_item, container, false)
        parentView = view.findViewById(R.id.parent_view)
        val theme = CometChatTheme.getInstance()
        val groupListItem = view.findViewById<CometChatListItem>(R.id.group_list_item)
        groupListItem.setTitle("Superhero")
        groupListItem.setTitleColor(theme.palette.getAccent(context))
        groupListItem.setSubtitleView(getTextView("8 members"))
        groupListItem.setAvatar(
            "https://data-us.cometchat.io/2379614bd4db65dd/media/1682517838_2050398854_08d684e835e3c003f70f2478f937ed57.jpeg",
            "Superhero"
        )
        groupListItem.hideStatusIndicator(true)
        val name = CometChatUIKit.getLoggedInUser().name
        val userListItem = view.findViewById<CometChatListItem>(R.id.user_list_Item)
        userListItem.setAvatar(CometChatUIKit.getLoggedInUser().avatar, name)
        userListItem.setSubtitleView(getTextView(CometChatUIKit.getLoggedInUser().status))
        userListItem.setTitle(name)
        userListItem.setTitleColor(theme.palette.getAccent(context))
        userListItem.setStatusIndicatorColor(
            ContextCompat.getColor(
                requireContext(),
                com.cometchat.chatuikit.R.color.cometchat_online_green
            )
        )
        val conversationListItem = view.findViewById<CometChatListItem>(R.id.conversation_list_item)
        val tailView = ConversationTailView(requireContext())
        tailView.setBadgeCount(100)
        tailView.badge.setStyle(
            BadgeStyle().setTextColor(theme.palette.getAccent(context))
                .setBackground(theme.palette.getPrimary(context))
                .setCornerRadius(100f)
        )
        tailView.date.setDate(System.currentTimeMillis() / 1000, Pattern.DAY_DATE_TIME)
        tailView.date.setStyle(
            DateStyle().setTextAppearance(theme.typography.subtitle1)
                .setTextColor(theme.palette.getAccent600(context))
        )
        conversationListItem.setTitle(name)
        conversationListItem.setTitleColor(theme.palette.getAccent(context))
        conversationListItem.setAvatar(CometChatUIKit.getLoggedInUser().avatar, name)
        conversationListItem.setTailView(tailView)
        conversationListItem.setSubtitleView(getTextView("Hey, How are you?"))
        conversationListItem.setStatusIndicatorColor(
            ContextCompat.getColor(
                requireContext(),
                com.cometchat.chatuikit.R.color.cometchat_online_green
            )
        )
        setUpUI(view)
        return view
    }

    private fun getTextView(name: String): View {
        val textView = TextView(requireContext())
        textView.text = name
        return textView
    }

    private fun setUpUI(view: View) {
        if (AppUtils.isNightMode(requireContext())) {
            AppUtils.changeTextColorToWhite(requireContext(), view.findViewById(R.id.conversation))
            AppUtils.changeTextColorToWhite(requireContext(), view.findViewById(R.id.group))
            AppUtils.changeTextColorToWhite(requireContext(), view.findViewById(R.id.user))
            AppUtils.changeTextColorToWhite(requireContext(), view.findViewById(R.id.list_item))
            AppUtils.changeTextColorToWhite(
                requireContext(),
                view.findViewById(R.id.list_item_desc)
            )
            parentView!!.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.app_background_dark
                    )
            )
        } else {
            AppUtils.changeTextColorToBlack(requireContext(), view.findViewById(R.id.conversation))
            AppUtils.changeTextColorToBlack(requireContext(), view.findViewById(R.id.group))
            AppUtils.changeTextColorToBlack(requireContext(), view.findViewById(R.id.user))
            AppUtils.changeTextColorToBlack(requireContext(), view.findViewById(R.id.list_item))
            parentView!!.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.app_background
                )
            )
        }
    }
}