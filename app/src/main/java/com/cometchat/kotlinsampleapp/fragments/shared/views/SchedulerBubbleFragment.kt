package com.cometchat.kotlinsampleapp.fragments.shared.views

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.models.interactiveactions.APIAction
import com.cometchat.chatuikit.shared.models.interactiveelements.ButtonElement
import com.cometchat.chatuikit.shared.models.interactivemessage.SchedulerMessage
import com.cometchat.chatuikit.shared.models.interactivemessage.TimeRange
import com.cometchat.chatuikit.shared.resources.theme.CometChatTheme
import com.cometchat.chatuikit.shared.views.CometChatAvatar.AvatarStyle
import com.cometchat.chatuikit.shared.views.CometChatQuickView.QuickViewStyle
import com.cometchat.chatuikit.shared.views.CometChatSchedulerBubble.CometChatSchedulerBubble
import com.cometchat.chatuikit.shared.views.CometChatSchedulerBubble.ScheduleStyle
import com.cometchat.chatuikit.shared.views.CometChatSchedulerBubble.SchedulerBubbleStyle
import com.cometchat.chatuikit.shared.views.calender.CalenderStyle
import com.cometchat.chatuikit.shared.views.timeslotitem.TimeSlotItemStyle
import com.cometchat.chatuikit.shared.views.timeslotselector.TimeSlotSelectorStyle
import com.cometchat.kotlinsampleapp.AppUtils
import com.cometchat.kotlinsampleapp.R
import com.google.android.material.card.MaterialCardView
import java.util.TimeZone

class SchedulerBubbleFragment : Fragment() {
    private lateinit var schedulerBubble: CometChatSchedulerBubble
    private lateinit var cardView: MaterialCardView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_scheduler_bubble, container, false)
        schedulerBubble = view.findViewById(R.id.scheduler_bubble)
        cardView = view.findViewById(R.id.scheduler_bubble_card)
        schedulerBubble.setStyle(schedulerBubbleStyle)
        schedulerBubble.setSchedulerMessage(schedulerMessage)
        cardView.setCardBackgroundColor(CometChatTheme.getInstance().palette.getAccent50(context))
        return view
    }

    private val schedulerBubbleStyle: SchedulerBubbleStyle
        get() {
            val theme = CometChatTheme.getInstance()
            val schedulerBubbleStyle = SchedulerBubbleStyle()
            schedulerBubbleStyle.setAvatarStyle(
                AvatarStyle().setOuterCornerRadius(100f)
                    .setInnerBackgroundColor(theme.palette.getAccent600(context))
                    .setTextColor(theme.palette.getAccent900(context)).setTextAppearance(theme.typography.name)
            )
            schedulerBubbleStyle.setCalenderStyle(
                CalenderStyle().setTitleTextAppearance(theme.typography.name)
                    .setTitleTextColor(theme.palette.getAccent(context))
            )
            schedulerBubbleStyle.setTimeSlotSelectorStyle(
                TimeSlotSelectorStyle()
                    .setCalenderImageTint(theme.palette.getAccent(context))
                    .setEmptyTimeSlotIconColor(theme.palette.getAccent500(context))
                    .setChosenDateTextAppearance(theme.typography.subtitle1)
                    .setChosenDateTextColor(theme.palette.getAccent(context))
                    .setSeparatorColor(theme.palette.getAccent100(context))
                    .setTitleColor(theme.palette.getAccent(context))
                    .setTitleTextAppearance(theme.typography.name)
                    .setEmptyTimeSlotTextColor(theme.palette.getAccent500(context))
                    .setEmptyTimeSlotTextAppearance(theme.typography.text1)
            )
            schedulerBubbleStyle.setSlotStyle(
                TimeSlotItemStyle().setCornerRadius(20f).setBackground(theme.palette.getBackground(context))
                    .setTimeColor(theme.palette.getAccent(context))
            )
            schedulerBubbleStyle.setSelectedSlotStyle(
                TimeSlotItemStyle().setCornerRadius(20f).setBackground(theme.palette.getPrimary(context))
                    .setTimeColor(
                        Color.WHITE
                    )
            )
            schedulerBubbleStyle.setScheduleStyle(
                ScheduleStyle()
                    .setProgressBarTintColor(Color.WHITE)
                    .setButtonBackgroundColor(theme.palette.getPrimary(context))
                    .setButtonTextColor(Color.WHITE)
                    .setCalendarIconTint(theme.palette.getAccent(context))
                    .setClockIconTint(theme.palette.getAccent(context))
                    .setTimeZoneIconTint(theme.palette.getAccent(context))
                    .setDurationTextColor(theme.palette.getAccent(context))
                    .setTimeTextColor(theme.palette.getAccent(context))
                    .setTimeZoneTextColor(theme.palette.getAccent(context))
                    .setErrorTextColor(theme.palette.getError(context))
                    .setButtonTextAppearance(theme.typography.subtitle1)
                    .setErrorTextAppearance(theme.typography.caption1)
                    .setDurationTextAppearance(theme.typography.subtitle1)
                    .setTimeTextAppearance(theme.typography.subtitle1)
                    .setTimeZoneTextAppearance(theme.typography.subtitle1)
            )
            schedulerBubbleStyle.setTitleAppearance(theme.typography.heading)
            schedulerBubbleStyle.setNameAppearance(theme.typography.name)
            schedulerBubbleStyle.setNameColor(theme.palette.getAccent(context))
            schedulerBubbleStyle.setTitleColor(theme.palette.getAccent(context))
            schedulerBubbleStyle.setBackIconTint(theme.palette.getPrimary(context))
            schedulerBubbleStyle.setSubtitleTextColor(theme.palette.getAccent600(context))
            schedulerBubbleStyle.setClockIconTint(theme.palette.getAccent600(context))
            schedulerBubbleStyle.setSubtitleTextAppearance(theme.typography.subtitle1)
            schedulerBubbleStyle.setSeparatorColor(theme.palette.getAccent100(context))
            schedulerBubbleStyle.setInitialSlotsItemStyle(
                TimeSlotItemStyle().setBackground(theme.palette.getBackground(context))
                    .setTimeColor(theme.palette.getPrimary(context))
                    .setTimeTextAppearance(theme.typography.subtitle2)
                    .setBorderColor(theme.palette.getPrimary(context)).setBorderWidth(2).setCornerRadius(25f)
            )
            schedulerBubbleStyle.setMoreTextColor(theme.palette.getPrimary(context))
            schedulerBubbleStyle.setDurationTimeTextColor(theme.palette.getAccent500(context))
            schedulerBubbleStyle.setGlobeIconTint(theme.palette.getAccent(context))
            schedulerBubbleStyle.setTimeZoneTextColor(theme.palette.getAccent(context))
            schedulerBubbleStyle.setMoreTextAppearance(theme.typography.subtitle2)
            schedulerBubbleStyle.setDurationTimeTextAppearance(theme.typography.caption1)
            schedulerBubbleStyle.setTimeZoneTextAppearance(theme.typography.subtitle2)
            schedulerBubbleStyle.setQuickViewStyle(
                QuickViewStyle().setCornerRadius(16f).setBackground(theme.palette.getBackground(context))
                    .setLeadingBarTint(theme.palette.getPrimary(context)).setTitleColor(theme.palette.getAccent(context))
                    .setTitleAppearance(theme.typography.text1)
                    .setSubtitleColor(theme.palette.getAccent500(context))
                    .setSubtitleAppearance(theme.typography.subtitle1)
            )
            schedulerBubbleStyle.setCornerRadius(10f)
            schedulerBubbleStyle.setQuickSlotAvailableAppearance(theme.typography.text1)
            schedulerBubbleStyle.setQuickSlotAvailableTextColor(theme.palette.getAccent500(context))
            schedulerBubbleStyle.setDisableColor(theme.palette.getAccent500(context))
            return schedulerBubbleStyle
        }
    private val schedulerMessage: SchedulerMessage
        get() {
            val schedulerMessage = SchedulerMessage()
            schedulerMessage.duration = 60
            schedulerMessage.isAllowSenderInteraction =
                true // true to set the sender as the scheduler
            schedulerMessage.title = "Meet Dr. Jackob"
            schedulerMessage.bufferTime = 15
            schedulerMessage.avatarUrl =
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRdRz0HEBl1wvncmX6rU8wFrRDxt2cvn2Dq9w&usqp=CAU"
            schedulerMessage.goalCompletionText = "Meeting Scheduled Successfully!!"
            val timeZone = TimeZone.getDefault()
            schedulerMessage.timezoneCode = timeZone.id
            schedulerMessage.dateRangeStart = "2024-01-01"
            schedulerMessage.dateRangeEnd = "2024-12-31"
            schedulerMessage.receiverUid = "superhero1"
            schedulerMessage.receiverType = CometChatConstants.RECEIVER_TYPE_USER
            schedulerMessage.sender = CometChatUIKit.getLoggedInUser()
            schedulerMessage.receiver = AppUtils.defaultUser
            val availability = HashMap<String, List<TimeRange>>()
            availability["monday"] = listOf(TimeRange("0000", "1359"))
            availability["tuesday"] = listOf(TimeRange("0000", "1559"))
            availability["wednesday"] = listOf(TimeRange("0000", "0659"))
            availability["thursday"] = listOf(TimeRange("0000", "0959"))
            availability["friday"] = listOf(TimeRange("0000", "1059"))
            schedulerMessage.availability = availability
            val clickAction = APIAction("https://www.example.com", "POST", "data")
            val scheduleElement = ButtonElement("21", "Submit", clickAction)
            schedulerMessage.scheduleElement = scheduleElement
            return schedulerMessage
        }
}