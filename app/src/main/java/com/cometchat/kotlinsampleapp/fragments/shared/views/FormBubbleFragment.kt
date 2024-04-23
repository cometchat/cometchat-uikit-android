package com.cometchat.kotlinsampleapp.fragments.shared.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.InteractionGoal
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.constants.UIKitConstants
import com.cometchat.chatuikit.shared.models.interactiveactions.URLNavigationAction
import com.cometchat.chatuikit.shared.models.interactiveelements.ButtonElement
import com.cometchat.chatuikit.shared.models.interactiveelements.CheckboxElement
import com.cometchat.chatuikit.shared.models.interactiveelements.ElementEntity
import com.cometchat.chatuikit.shared.models.interactiveelements.OptionElement
import com.cometchat.chatuikit.shared.models.interactiveelements.PlaceHolder
import com.cometchat.chatuikit.shared.models.interactiveelements.RadioButtonElement
import com.cometchat.chatuikit.shared.models.interactiveelements.SingleSelectElement
import com.cometchat.chatuikit.shared.models.interactiveelements.SpinnerElement
import com.cometchat.chatuikit.shared.models.interactiveelements.TextInputElement
import com.cometchat.chatuikit.shared.models.interactivemessage.FormMessage
import com.cometchat.chatuikit.shared.resources.theme.CometChatTheme
import com.cometchat.chatuikit.shared.views.CometChatFormbubble.CometChatFormBubble
import com.cometchat.chatuikit.shared.views.CometChatFormbubble.FormBubbleStyle
import com.cometchat.chatuikit.shared.views.CometChatSingleSelect.SingleSelectStyle
import com.cometchat.kotlinsampleapp.AppUtils
import com.cometchat.kotlinsampleapp.R

class FormBubbleFragment : Fragment() {
    private lateinit var formBubble: CometChatFormBubble
    private lateinit var parentLayout: LinearLayout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_form_bubble, container, false)
        parentLayout = view.findViewById(R.id.parent_layout)
        formBubble = view.findViewById(R.id.form_bubble)
        val theme = CometChatTheme.getInstance()
        //create style object for Form bubble
        val formBubbleStyle = FormBubbleStyle()
            .setTitleAppearance(theme.typography.heading)
            .setTitleColor(theme.palette.getAccent(context))
            .setLabelAppearance(theme.typography.subtitle1)
            .setLabelColor(theme.palette.getAccent(context))
            .setInputTextAppearance(theme.typography.subtitle1)
            .setInputTextColor(theme.palette.getAccent(context))
            .setInputHintColor(theme.palette.getAccent500(context))
            .setErrorColor(theme.palette.getError(context))
            .setInputStrokeColor(theme.palette.getAccent600(context))
            .setActiveInputStrokeColor(theme.palette.getAccent(context))
            .setDefaultCheckboxButtonTint(theme.palette.getAccent500(context))
            .setSelectedCheckboxButtonTint(theme.palette.getPrimary(context))
            .setErrorCheckboxButtonTint(theme.palette.getError(context))
            .setCheckboxTextColor(theme.palette.getAccent(context))
            .setCheckboxTextAppearance(theme.typography.subtitle1)
            .setButtonBackgroundColor(theme.palette.getPrimary(context))
            .setButtonTextColor(theme.palette.getAccent900(context))
            .setButtonTextAppearance(theme.typography.subtitle1)
            .setProgressBarTintColor(theme.palette.getAccent900(context))
            .setRadioButtonTint(theme.palette.getAccent500(context))
            .setRadioButtonTextColor(theme.palette.getAccent(context))
            .setRadioButtonTextAppearance(theme.typography.subtitle1)
            .setSelectedRadioButtonTint(theme.palette.getPrimary(context))
            .setSpinnerTextColor(theme.palette.getAccent(context))
            .setSpinnerTextAppearance(theme.typography.subtitle1)
            .setSpinnerBackgroundColor(theme.palette.getAccent500(context))
            .setBackground(theme.palette.getBackground(context))
            .setBackground(theme.palette.gradientBackground)
            .setSingleSelectStyle(
                SingleSelectStyle()
                    .setOptionTextAppearance(theme.typography.subtitle1)
                    .setOptionTextColor(theme.palette.getAccent500(context))
                    .setSelectedOptionTextAppearance(theme.typography.subtitle1)
                    .setSelectedOptionTextColor(theme.palette.getAccent(context))
                    .setButtonStrokeColor(theme.palette.getAccent600(context))
                    .setTitleColor(theme.palette.getAccent(context))
                    .setTitleAppearance(theme.typography.subtitle1)
            )
        formBubble.style = formBubbleStyle

        //create elements for form bubble
        val elementEntities: MutableList<ElementEntity> = ArrayList()
        val textInputElement1 = TextInputElement("element1", "Name")
        textInputElement1.placeHolder = PlaceHolder("write your name here")
        val textInputElement2 = TextInputElement("element2", "Last Name")
        val textInputElement3 = TextInputElement("element3", "Address")
        textInputElement3.maxLines = 5
        textInputElement1.defaultValue = "vivek"
        elementEntities.add(textInputElement1)
        elementEntities.add(textInputElement2)
        elementEntities.add(textInputElement3)
        val optionElementList: MutableList<OptionElement> = ArrayList()
        optionElementList.add(OptionElement("option1", "INDIA"))
        optionElementList.add(OptionElement("option2", "AUSTRALIA"))
        optionElementList.add(OptionElement("option3", "RUSSIA"))
        optionElementList.add(OptionElement("option4", "AMERICA"))
        val spinnerElement = SpinnerElement("element4", "Country", optionElementList, "option1")
        elementEntities.add(spinnerElement)
        val checkBox: MutableList<OptionElement> = ArrayList()
        checkBox.add(OptionElement("option1", "Garbage"))
        checkBox.add(OptionElement("option2", "Electricity Bill"))
        checkBox.add(OptionElement("option3", "Lift"))
        val checkboxElement = CheckboxElement(
            "element5", "Services", checkBox, ArrayList(
                mutableListOf("option1", "option2")
            )
        )
        elementEntities.add(checkboxElement)
        val optionElementList2: MutableList<OptionElement> = ArrayList()
        optionElementList2.add(OptionElement("option1", "A"))
        optionElementList2.add(OptionElement("option2", "B"))
        val singleSelectElement =
            SingleSelectElement("element6", "Wing", optionElementList2, "option1")
        elementEntities.add(singleSelectElement)
        val radioButtonElement =
            RadioButtonElement("element7", "Country", optionElementList, "option1")
        elementEntities.add(radioButtonElement)
        val urlNavigationAction = URLNavigationAction("https://www.cometchat.com/")
        val submitButton = ButtonElement("element9", "About us", urlNavigationAction)
        submitButton.isDisableAfterInteracted = true
        elementEntities.add(submitButton)
        val formMessage = FormMessage(
            if (AppUtils.defaultUser != null) AppUtils.defaultUser!!
                .uid else if (AppUtils.defaultGroup != null) AppUtils.defaultGroup!!
                .guid else null,
            if (AppUtils.defaultUser != null) UIKitConstants.ReceiverType.USER else UIKitConstants.ReceiverType.GROUP,
            elementEntities,
            submitButton
        )
        formMessage.title = "Society Survey"
        formMessage.isAllowSenderInteraction = true
        formMessage.sender = CometChatUIKit.getLoggedInUser()
        formMessage.sentAt = System.currentTimeMillis() / 1000
        formMessage.receiver =
            if (AppUtils.defaultUser != null) AppUtils.defaultUser else if (AppUtils.defaultGroup != null) AppUtils.defaultGroup else null
        formMessage.interactionGoal =
            InteractionGoal(
                CometChatConstants.INTERACTION_TYPE_All_OF,
                mutableListOf("element8", "element9")
            )
        formBubble.formMessage = formMessage
        return view
    }
}