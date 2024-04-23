package com.cometchat.kotlinsampleapp.fragments.shared.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.constants.UIKitConstants
import com.cometchat.chatuikit.shared.models.interactiveactions.URLNavigationAction
import com.cometchat.chatuikit.shared.models.interactiveelements.BaseInteractiveElement
import com.cometchat.chatuikit.shared.models.interactiveelements.ButtonElement
import com.cometchat.chatuikit.shared.models.interactivemessage.CardMessage
import com.cometchat.chatuikit.shared.resources.theme.CometChatTheme
import com.cometchat.chatuikit.shared.views.CometChatCardBubble.CardBubbleStyle
import com.cometchat.chatuikit.shared.views.CometChatCardBubble.CometChatCardBubble
import com.cometchat.chatuikit.shared.views.CometChatImageBubble.ImageBubbleStyle
import com.cometchat.kotlinsampleapp.AppUtils
import com.cometchat.kotlinsampleapp.R

class CardBubbleFragment : Fragment() {

    private lateinit var parentLayout: LinearLayout
    private lateinit var cardBubble: CometChatCardBubble
    private lateinit var scrollView: ScrollView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_card_bubble, container, false)
        parentLayout = view.findViewById(R.id.parent_layout)
        cardBubble = view.findViewById(R.id.card_bubble)
        scrollView = view.findViewById(R.id.scroll_view)
        scrollView.isVerticalScrollBarEnabled = false
        val theme = CometChatTheme.getInstance()

        //create style object for card bubble
        val cardBubbleStyle = CardBubbleStyle()
            .setTextAppearance(theme.typography.text1)
            .setTextColor(theme.palette.getAccent(context))
            .setContentBackgroundColor(theme.palette.getBackground(context))
            .setCornerRadius(16f)
            .setProgressBarTintColor(theme.palette.getPrimary(context))
            .setButtonSeparatorColor(theme.palette.getAccent100(context))
            .setButtonBackgroundColor(theme.palette.getBackground(context))
            .setButtonTextColor(theme.palette.getPrimary(context))
            .setButtonDisableTextColor(theme.palette.getAccent500(context))
            .setButtonTextAppearance(theme.typography.subtitle1)
            .setBackground(theme.palette.getBackground(context))
            .setBackground(theme.palette.gradientBackground)
            .setImageBubbleStyle(
                ImageBubbleStyle()
                    .setCornerRadius(16f)
            )
        cardBubble.style = cardBubbleStyle

        //create list of interactive elements
        val elementEntities: MutableList<BaseInteractiveElement> = ArrayList()
        val urlNavigationAction = URLNavigationAction("https://www.cometchat.com/")
        val buttonElement1 = ButtonElement("element1", "Navigate", urlNavigationAction)
        buttonElement1.isDisableAfterInteracted = true
        elementEntities.add(buttonElement1)
        val cardMessage = CardMessage(
            if (AppUtils.defaultUser != null) AppUtils.defaultUser!!
                .uid else if (AppUtils.defaultGroup != null) AppUtils.defaultGroup!!
                .guid else null,
            if (AppUtils.defaultUser != null) UIKitConstants.ReceiverType.USER else UIKitConstants.ReceiverType.GROUP,
            null,
            elementEntities
        )
        cardMessage.text = """🌟 Introducing our New Personalized Card Messages! 🌟

Want to make your gifts more special? Now it's easy with our personalized card messages! 💬✍️

Our new feature lets you add a custom message on a beautifully designed card, making your gift-giving extra personal and memorable. Whether it's for a birthday 🎂, anniversary 💍, or just because 🎁, our card messages will express your feelings perfectly.

To start creating your own card message:
 1️⃣ Choose the gift 
2️⃣ Write your heartfelt message 
3️⃣ We'll print it on a high-quality card and include it with your gift

💫 Add a touch of your own sentiments with our personalized card messages. Make every gift unforgettable. Start creating your card message today!

Visit our website [Website Link] or download our app [App Link].

Express more than just words with our Personalized Card Messages. Because it's not just a gift, it's your feelings. ❤️"""
        cardMessage.imageUrl =
            "https://images.unsplash.com/photo-1608755728617-aefab37d2edd?q=80&w=3270&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
        cardMessage.isAllowSenderInteraction = true // sender can interact with the card message
        cardMessage.sender = CometChatUIKit.getLoggedInUser()
        cardMessage.sentAt = System.currentTimeMillis() / 1000
        cardMessage.receiver =
            if (AppUtils.defaultUser != null) AppUtils.defaultUser else if (AppUtils.defaultGroup != null) AppUtils.defaultGroup else null
        cardBubble.cardMessage = cardMessage
        return view
    }
}