package com.cometchat.chatuikit.shared.views.schedulerbubble;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.Interaction;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.extensions.collaborative.CometChatWebViewActivity;
import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.models.interactiveactions.APIAction;
import com.cometchat.chatuikit.shared.models.interactiveactions.DeepLinkingAction;
import com.cometchat.chatuikit.shared.models.interactiveactions.URLNavigationAction;
import com.cometchat.chatuikit.shared.models.interactiveelements.ButtonElement;
import com.cometchat.chatuikit.shared.models.interactivemessage.InteractiveConstants;
import com.cometchat.chatuikit.shared.models.interactivemessage.SchedulerMessage;
import com.cometchat.chatuikit.shared.resources.apicontroller.ApiController;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.views.calender.CometChatCalender;
import com.cometchat.chatuikit.shared.views.quickview.CometChatQuickView;
import com.cometchat.chatuikit.shared.views.quickview.QuickViewStyle;
import com.cometchat.chatuikit.shared.views.timeslotitem.CometChatTimeSlotItem;
import com.cometchat.chatuikit.shared.views.timeslotitem.TimeSlotItemStyle;
import com.cometchat.chatuikit.shared.views.timeslotselector.CometChatTimeSlotSelector;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CometChatSchedulerBubble extends MaterialCardView {
    private static final String TAG = CometChatSchedulerBubble.class.getSimpleName();
    private final HashMap<String, List<DateTimeRange>> occupiedIcsMeetings = new HashMap<>();
    private SchedulerBubbleStyle style;
    private LinearLayout loadingView, meetSchedulerView;
    private LinearLayout linearLayoutTimeZone, meetingContainer;
    private TextView tvMoreTimes, tvTimeZone2, viewDivider;
    private SchedulerBubbleHeader header;
    private SchedulerMessage schedulerMessage;
    private CometChatTimeSlotSelector timeSlot;
    private ScheduleView scheduleView;
    private LinearLayout initialSlots, quickViewContainer, emptyQuickSlotContainer;
    private TextView quickViewMessage, quickSlotAvailableText;
    private CometChatQuickView quickView;
    private CometChatCalender calender;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat sendingFormat;
    private SimpleDateFormat timeSlotTimeFormat;
    private int currentView = 1;
    private LinearLayout scheduleHeaderContainer;
    private boolean allowSenderInteraction = false, isBooked;

    public CometChatSchedulerBubble(Context context) {
        super(context);
        init(context);
    }

    public CometChatSchedulerBubble(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CometChatSchedulerBubble(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View view = View.inflate(context, R.layout.cometchat_meeting_bubble, null);
        dateFormat = new SimpleDateFormat("EEE, MMM dd 'at' h:mm a", Locale.US);
        timeSlotTimeFormat = new SimpleDateFormat("dd MMMM yyyy, EEEE", Locale.US);
        sendingFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);

        tvMoreTimes = view.findViewById(R.id.tv_more_time);
        viewDivider = view.findViewById(R.id.view_divider);
        tvTimeZone2 = view.findViewById(R.id.tv_time_zone3);
        loadingView = view.findViewById(R.id.view_loading);
        emptyQuickSlotContainer = view.findViewById(R.id.empty_quick_slot_container);
        quickSlotAvailableText = view.findViewById(R.id.tv_no_quick_time_slots_available);
        meetingContainer = view.findViewById(R.id.meeting_bubble_container);
        quickViewContainer = view.findViewById(R.id.quick_view_container);
        quickView = view.findViewById(R.id.quick_view);
        scheduleHeaderContainer = view.findViewById(R.id.schedule_header_container);
        quickViewMessage = view.findViewById(R.id.quick_view_message);
        timeSlot = new CometChatTimeSlotSelector(context);
        scheduleView = new ScheduleView(context);
        calender = new CometChatCalender(context);
        initialSlots = new LinearLayout(context);
        initialSlots.setOrientation(LinearLayout.VERTICAL);
        meetSchedulerView = view.findViewById(R.id.view_meet_schedule);
        header = view.findViewById(R.id.meeting_bubble_header);
        emptyQuickSlotContainer.setVisibility(GONE);
        bubbleHeaderInitialSetup();
        tvMoreTimes.setVisibility(View.GONE);
        tvTimeZone2.setText(TimeZone.getDefault().getDisplayName());
        linearLayoutTimeZone = view.findViewById(R.id.ll_time_zone);
        Utils.initMaterialCard(this);
        timeSlot.setOnSelectionListener((context1, view1, dateTimeRange, position) -> createScheduleView(dateTimeRange));

        scheduleView.setOnSubmitClick((context12, baseInputElements, dateTimeRange) -> {
            if (!isBooked) {
                scheduleView.setButtonTitleVisibility(View.GONE);
                scheduleView.setProgressBarVisibility(View.VISIBLE);
                SchedulerUtils.readIcsFile(schedulerMessage.getIcsFileUrl(), result -> {
                    SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    String key = dateKeyFormat.format(dateTimeRange.getFrom().getTime());
                    List<DateTimeRange> dateTimeRanges = new ArrayList<>();
                    if (result.containsKey(key)) {
                        dateTimeRanges = result.get(key);
                    }
                    if (dateTimeRanges != null && !SchedulerUtils.checkInsideOrEquals(dateTimeRanges, dateTimeRange)) {
                        performAction(schedulerMessage.getScheduleElement(), dateTimeRange);
                    } else {
                        resetScheduleButton();
                        changeButtonColorToDisable(scheduleView.getButtonCard());
                        scheduleView.setErrorText(getContext().getString(R.string.cometchat_time_slot_no_longer_available_please_choose_another));
                        scheduleView.setButtonTitle(getContext().getString(R.string.cometchat_book_new_slot));
                        isBooked = true;
                    }
                });
            } else {
                loadViewStack(1);
            }
        });

        tvMoreTimes.setOnClickListener(v -> {
            meetSchedulerView.removeAllViews();
            createCalender();
            tvMoreTimes.setVisibility(GONE);
            linearLayoutTimeZone.setVisibility(VISIBLE);
            header.showInHorizontalMode();
            emptyQuickSlotContainer.setVisibility(GONE);
        });

        header.getBackArrowView().setOnClickListener(view12 -> {
            currentView--;
            loadViewStack(currentView);
        });
        addView(view);
    }

    private void resetScheduleButton() {
        scheduleView.enableButton(true);
        scheduleView.setProgressBarVisibility(GONE);
        scheduleView.setButtonTitleVisibility(VISIBLE);
    }

    private void performAction(ButtonElement buttonElement, DateTimeRange dateTimeRange) {
        if (buttonElement.getAction() != null && !buttonElement.getAction().getActionType().isEmpty()) {
            switch (buttonElement.getAction().getActionType()) {
                case InteractiveConstants.ACTION_TYPE_API_ACTION:
                    APIAction apiAction = (APIAction) buttonElement.getAction();
                    JSONObject jsonObject = Utils.getInteractiveRequestPayload(apiAction.getPayload(),
                                                                               schedulerMessage.getScheduleElement().getElementId(),
                                                                               schedulerMessage);
                    try {
                        JSONObject responseJson = jsonObject.getJSONObject(InteractiveConstants.InteractiveRequestPayload.DATA);
                        JSONObject schedulerData = new JSONObject();
                        schedulerData.put(InteractiveConstants.SchedulerConstant.DURATION, schedulerMessage.getDuration());
                        schedulerData.put(InteractiveConstants.SchedulerConstant.MEET_START_AT,
                                          sendingFormat.format(dateTimeRange.getFrom().getTime()));
                        responseJson.put(InteractiveConstants.InteractiveRequestPayload.SCHEDULER_DATA, schedulerData);
                        jsonObject.put(InteractiveConstants.InteractiveRequestPayload.DATA, responseJson);
                    } catch (Exception e) {
                        CometChatLogger.e(TAG, e.toString());
                    }
                    ApiController
                        .getInstance()
                        .call(apiAction.getMethod(), apiAction.getUrl(), jsonObject, apiAction.getHeaders(), new ApiController.APICallback() {
                            @Override
                            public void onSuccess(String response) {
                                markInteract();
                            }

                            @Override
                            public void onError(Exception e) {
                                scheduleView.enableButton(true);
                                scheduleView.setProgressBarVisibility(GONE);
                                scheduleView.setButtonTitleVisibility(VISIBLE);
                                scheduleView.setErrorText(getResources().getString(R.string.cometchat_something_went_wrong_please_try_again));
                                scheduleView.setErrorVisibility(View.VISIBLE);
                                scheduleView.setButtonTitle(getResources().getString(R.string.cometchat_try_again));
                            }
                        });

                    break;
                case InteractiveConstants.ACTION_TYPE_URL_NAVIGATION:
                    URLNavigationAction urlNavigationAction = (URLNavigationAction) buttonElement.getAction();
                    if (urlNavigationAction.getUrl() != null && !urlNavigationAction.getUrl().isEmpty()) {
                        Intent intent = new Intent(getContext(), CometChatWebViewActivity.class);
                        intent.putExtra(UIKitConstants.IntentStrings.URL, urlNavigationAction.getUrl());
                        getContext().startActivity(intent);
                        markInteract();
                    }
                    break;
                case InteractiveConstants.ACTION_TYPE_DEEP_LINKING:
                    DeepLinkingAction deepLinking = (DeepLinkingAction) buttonElement.getAction();
                    if (deepLinking.getUrl() != null && !deepLinking.getUrl().isEmpty()) {
                        String url = deepLinking.getUrl();
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        if (i.resolveActivity(getContext().getPackageManager()) != null) {
                            getContext().startActivity(i);
                            markInteract();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void markInteract() {
        CometChat.markAsInteracted(schedulerMessage.getId(),
                                   schedulerMessage.getScheduleElement().getElementId(),
                                   new CometChat.CallbackListener<Void>() {
                                       @Override
                                       public void onSuccess(Void unused) {
                                           schedulerMessage
                                               .getInteractions()
                                               .add(new Interaction(schedulerMessage.getScheduleElement().getElementId(),
                                                                    System.currentTimeMillis() / 100));
                                           disableButton(schedulerMessage.getScheduleElement());
                                       }

                                       @Override
                                       public void onError(CometChatException e) {
                                           scheduleView.setProgressBarVisibility(GONE);
                                           scheduleView.setButtonTitleVisibility(VISIBLE);
                                           scheduleView.setErrorText(getResources().getString(R.string.cometchat_something_went_wrong));
                                           scheduleView.setErrorVisibility(View.VISIBLE);
                                       }
                                   });
    }

    public void disableButton(ButtonElement buttonElement) {
        if (!Utils.isGoalCompleted(schedulerMessage)) {
            if (buttonElement.isDisableAfterInteracted()) {
                scheduleView.enableButton(false);
                scheduleView.setProgressBarVisibility(GONE);
                scheduleView.setButtonTitleVisibility(VISIBLE);
                changeButtonColorToDisable(scheduleView.getButtonCard());
            }
        } else {
            meetingContainer.setVisibility(GONE);
            quickViewContainer.setVisibility(VISIBLE);
        }
    }

    private void changeButtonColorToDisable(MaterialCardView button) {
        ColorDrawable colorDrawable = (ColorDrawable) button.getBackground();
        int newColor = Utils.multiplyColorAlpha(colorDrawable.getColor(), 150);
        button.setBackgroundColor(newColor);
        button.setEnabled(false);
    }

    public void createCalender() {
        changeTitleAppearanceToName();
        scheduleHeaderContainer.setPadding(Utils.convertDpToPx(getContext(), 4),
                                           Utils.convertDpToPx(getContext(), 10),
                                           Utils.convertDpToPx(getContext(), 4),
                                           Utils.convertDpToPx(getContext(), 10));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(this.getWidth(), LinearLayout.LayoutParams.WRAP_CONTENT);
        currentView = 2;
        calender.setTitle(getContext().getString(R.string.cometchat_select_a_day));
        calender.setMinDate(SchedulerUtils.getDateFromString(schedulerMessage.getDateRangeStart(), "0000", schedulerMessage.getTimezoneCode()));
        calender.setMaxDate(SchedulerUtils.getDateFromString(schedulerMessage.getDateRangeEnd(), "2359", schedulerMessage.getTimezoneCode()));
        calender.setOnDateChangeListener((calendarView, year, month, dayOfMonth) -> {
            Calendar fromCalender = Calendar.getInstance();
            fromCalender.set(year, month, dayOfMonth, 0, 0, 0);
            timeSlot.setSelectedDate(timeSlotTimeFormat.format(fromCalender.getTime()));
            HashMap<String, List<DateTimeRange>> range = SchedulerUtils.getMeetingRange(fromCalender,
                                                                                        schedulerMessage.getTimezoneCode(),
                                                                                        schedulerMessage.getAvailability(),
                                                                                        occupiedIcsMeetings,
                                                                                        schedulerMessage.getDuration());
            meetSchedulerView.removeAllViews();
            timeSlot.setTime(range.get(UIKitConstants.SchedulerConstants.AVAILABLE),
                             range.get(UIKitConstants.SchedulerConstants.OCCUPIED),
                             schedulerMessage.getDuration(),
                             schedulerMessage.getBufferTime());
            createSlots();
        });
        header.showInHorizontalMode();
        meetSchedulerView.addView(calender);
        calender.setLayoutParams(layoutParams);
    }

    private void createSlots() {
        changeTitleAppearanceToName();
        scheduleHeaderContainer.setPadding(Utils.convertDpToPx(getContext(), 4),
                                           Utils.convertDpToPx(getContext(), 10),
                                           Utils.convertDpToPx(getContext(), 4),
                                           Utils.convertDpToPx(getContext(), 10));
        header.showInHorizontalMode();
        tvMoreTimes.setVisibility(GONE);
        linearLayoutTimeZone.setVisibility(VISIBLE);
        Utils.removeParentFromView(timeSlot);
        meetSchedulerView.addView(timeSlot);
        linearLayoutTimeZone.setVisibility(View.VISIBLE);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(this.getWidth(), LinearLayout.LayoutParams.WRAP_CONTENT);
        timeSlot.setLayoutParams(layoutParams);
        currentView = 3;
    }

    public void setSchedulerMessage(SchedulerMessage schedulerMessage) {
        if (schedulerMessage != null) {
            this.schedulerMessage = schedulerMessage;
            if (schedulerMessage.getIcsFileUrl() != null && !schedulerMessage.getIcsFileUrl().isEmpty()) {
                SchedulerUtils.readIcsFile(schedulerMessage.getIcsFileUrl(), result -> {
                    occupiedIcsMeetings.clear();
                    occupiedIcsMeetings.putAll(result);
                    createFirstThreeSlots();
                });
            } else {
                createFirstThreeSlots();
            }
            header.setTitle(schedulerMessage.getTitle() != null ? schedulerMessage.getTitle() : getContext().getString(R.string.cometchat_meet_with) + schedulerMessage
                .getSender()
                .getName());
            header.setAvatar(schedulerMessage.getAvatarUrl() != null ? schedulerMessage.getAvatarUrl() : schedulerMessage.getSender().getAvatar(),
                             schedulerMessage.getSender().getName());
            header.setSubTitle(schedulerMessage.getDuration() + " min");
            scheduleView.setSubmitButton(schedulerMessage.getScheduleElement());

            quickView.setTitle(CometChatUIKit.getLoggedInUser().getName());
            quickView.setSubTitle(schedulerMessage.getTitle());
            quickView.setCloseIconVisibility(View.GONE);
            quickViewMessage.setText(schedulerMessage.getGoalCompletionText() == null ? getContext().getString(R.string.cometchat_meeting_scheduled) : schedulerMessage.getGoalCompletionText());

            if (Utils.isGoalCompleted(schedulerMessage)) {
                meetingContainer.setVisibility(GONE);
                quickViewContainer.setVisibility(VISIBLE);
            } else {
                meetingContainer.setVisibility(VISIBLE);
                quickViewContainer.setVisibility(GONE);
            }
            allowSenderInteraction = schedulerMessage.isAllowSenderInteraction() || schedulerMessage.getSender() == null || !schedulerMessage
                .getSender()
                .getUid()
                .equals(CometChatUIKit.getLoggedInUser().getUid());
            tvMoreTimes.setEnabled(allowSenderInteraction);
            if (style != null) {
                if (style.getMoreTextColor() != 0) {
                    tvMoreTimes.setTextColor(style.getMoreTextColor());
                }
                if (!allowSenderInteraction && style.getDisableColor() != 0)
                    tvMoreTimes.setTextColor(style.getDisableColor());
            }
        }
    }

    private void createFirstThreeSlots() {
        changeTitleAppearanceToHeading();
        scheduleHeaderContainer.setPadding(Utils.convertDpToPx(getContext(), 16),
                                           Utils.convertDpToPx(getContext(), 16),
                                           Utils.convertDpToPx(getContext(), 16),
                                           Utils.convertDpToPx(getContext(), 16));
        initialSlots.removeAllViews();
        isBooked = false;
        bubbleHeaderInitialSetup();
        meetSchedulerView.removeAllViews();

        SchedulerUtils.processMeetingAsync(schedulerMessage, occupiedIcsMeetings, result -> {
            List<DateTimeRange> availableRanges = result.get(UIKitConstants.SchedulerConstants.AVAILABLE);
            int loopCount;
            if (availableRanges != null) {
                loopCount = Math.min(availableRanges.size(), 3);
            } else {
                loopCount = 0;
            }
            if (loopCount > 0) {
                emptyQuickSlotContainer.setVisibility(GONE);
                for (int i = 0; i < loopCount; i++) {
                    CometChatTimeSlotItem timeSchedulerItem = new CometChatTimeSlotItem(getContext());
                    initialSlots.addView(timeSchedulerItem);
                    timeSchedulerItem.setTime(dateFormat.format(availableRanges.get(i).getFrom().getTime()));
                    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) timeSchedulerItem.getLayoutParams();
                    if (i != 0) layoutParams.topMargin = Utils.convertDpToPx(getContext(), 12);
                    else layoutParams.topMargin = Utils.convertDpToPx(getContext(), 16);
                    layoutParams.leftMargin = Utils.convertDpToPx(getContext(), 16);
                    layoutParams.rightMargin = Utils.convertDpToPx(getContext(), 16);
                    timeSchedulerItem.setTimeTextSize(13);
                    timeSchedulerItem.setLayoutParams(layoutParams);
                    timeSchedulerItem.setPadding(Utils.convertDpToPx(getContext(), 40),
                                                 Utils.convertDpToPx(getContext(), 0),
                                                 Utils.convertDpToPx(getContext(), 40),
                                                 Utils.convertDpToPx(getContext(), 0));
                    int finalI = i;
                    timeSchedulerItem.setOnClickListener(v -> {
                        createScheduleView(availableRanges.get(finalI));
                        currentView = 2;
                    });
                    timeSchedulerItem.setEnabled(allowSenderInteraction);

                    if (style != null) {
                        TimeSlotItemStyle slotItemStyle = style.getInitialSlotsItemStyle();
                        if (!allowSenderInteraction && slotItemStyle != null) {
                            slotItemStyle.setStrokeColor(style.getDisableColor());
                            slotItemStyle.setTimeColor(style.getDisableColor());
                        }
                        timeSchedulerItem.setStyle(style.getInitialSlotsItemStyle());
                    }
                }
            } else {
                emptyQuickSlotContainer.setVisibility(VISIBLE);
                quickSlotAvailableText.setText(getContext().getString(R.string.cometchat_no_quick_time_slots_available));
            }
            TextView textView = new TextView(getContext());
            initialSlots.addView(textView);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) textView.getLayoutParams();
            layoutParams.topMargin = Utils.convertDpToPx(getContext(), 2);
            layoutParams.leftMargin = Utils.convertDpToPx(getContext(), 16);
            layoutParams.rightMargin = Utils.convertDpToPx(getContext(), 16);
            textView.setText(schedulerMessage.getDuration() + getContext().getString(R.string.cometchat_min_meeting) + " • " + TimeZone
                .getDefault()
                .getDisplayName());
            if (style != null && style.getDurationTimeTextColor() != 0) {
                textView.setTextColor(style.getDurationTimeTextColor());
            }
            if (style != null && style.getDurationTimeTextAppearance() != 0) {
                textView.setTextAppearance(style.getDurationTimeTextColor());
            }
            textView.setTextSize(11);
            Utils.removeParentFromView(initialSlots);
            meetSchedulerView.addView(initialSlots);
            tvMoreTimes.setVisibility(View.VISIBLE);
            linearLayoutTimeZone.setVisibility(View.GONE);
            loadingView.setVisibility(View.GONE);
        });
    }

    private void changeTitleAppearanceToHeading() {
        if (style != null && style.getTitleAppearance() != 0) {
            header.setTitleTextAppearance(style.getTitleAppearance());
            header.setTextAppearance(Typeface.NORMAL);
        }
        if (style != null && style.getTitleColor() != 0)
            header.setTitleTextColor(style.getTitleColor());
    }

    private void bubbleHeaderInitialSetup() {
        header.showInVerticalMode();
    }

    private void createScheduleView(DateTimeRange dateTimeRange) {
        if (dateTimeRange != null && dateTimeRange.getFrom() != null) {
            changeTitleAppearanceToName();
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(this.getWidth(), LinearLayout.LayoutParams.WRAP_CONTENT);

            scheduleHeaderContainer.setPadding(Utils.convertDpToPx(getContext(), 7),
                                               Utils.convertDpToPx(getContext(), 16),
                                               Utils.convertDpToPx(getContext(), 7),
                                               Utils.convertDpToPx(getContext(), 16));
            scheduleView.setErrorVisibility(GONE);
            scheduleView.setTime(dateTimeRange);
            scheduleView.setDuration(schedulerMessage.getDuration() + "");
            scheduleView.setTimeZone(TimeZone.getDefault().getDisplayName());
            meetSchedulerView.removeAllViews();
            meetSchedulerView.addView(scheduleView);
            scheduleView.setLayoutParams(layoutParams);
            linearLayoutTimeZone.setVisibility(GONE);
            tvMoreTimes.setVisibility(View.GONE);
            currentView = 4;
            header.showInHorizontalMode();
        }
    }

    private void changeTitleAppearanceToName() {
        if (style != null && style.getNameAppearance() != 0) {
            header.setTitleTextAppearance(style.getNameAppearance());
            header.setTextAppearance(Typeface.NORMAL);
        }
        if (style != null && style.getNameColor() != 0)
            header.setTitleTextColor(style.getNameColor());
    }

    private void loadViewStack(int stackPos) {
        switch (stackPos) {
            case 1:
                meetSchedulerView.removeAllViews();
                createFirstThreeSlots();
                break;
            case 2:
                meetSchedulerView.removeAllViews();
                createCalender();
                break;
            case 3:
                meetSchedulerView.removeAllViews();
                createSlots();
                break;
            case 4:
                meetSchedulerView.removeAllViews();
                meetSchedulerView.addView(scheduleView);
                break;
        }
    }

    public void setStyle(SchedulerBubbleStyle style) {
        if (style != null) {
            this.style = style;
            scheduleView.setStyle(style.getScheduleStyle());
            calender.setStyle(style.getCalenderStyle());
            timeSlot.setStyle(style.getTimeSlotSelectorStyle());
            timeSlot.setSelectedSlotStyle(style.getSelectedSlotStyle());
            timeSlot.setSlotStyle(style.getSlotStyle());
            header.setTitleTextColor(style.getTitleColor());
            header.setTitleTextAppearance(style.getTitleAppearance());
            header.setBackArrowTint(style.getBackIconTint());
            header.setAvatarStyle(style.getAvatarStyle());
            header.setSubtitleImageTint(style.getClockIconTint());
            header.setSubtitleTextColor(style.getSubtitleTextColor());
            header.setSubTitleTextAppearance(style.getSubtitleTextAppearance());
            setQuickSlotAvailableTextAppearance(style.getQuickSlotAvailableAppearance());
            setQuickSlotAvailableTextColor(style.getQuickSlotAvailableTextColor());
            QuickViewStyle quickViewStyle = style.getQuickViewStyle();
            quickView.setStyle(quickViewStyle);
            if (quickViewStyle != null) {
                if (quickViewStyle.getSubtitleColor() != 0) {
                    quickViewMessage.setTextColor(quickViewStyle.getSubtitleColor());
                }
                if (quickViewStyle.getSubtitleAppearance() != 0) {
                    quickViewMessage.setTextAppearance(quickViewStyle.getSubtitleAppearance());
                }
            }
            if (style.getMoreTextAppearance() != 0)
                tvMoreTimes.setTextAppearance(style.getMoreTextAppearance());
            if (style.getTimeZoneTextColor() != 0) {
                tvTimeZone2.setTextColor(style.getTimeZoneTextColor());
            }
            if (style.getTimeZoneTextAppearance() != 0) {
                tvTimeZone2.setTextAppearance(style.getTimeZoneTextAppearance());
            }
            if (style.getSeparatorColor() != 0)
                viewDivider.setBackgroundColor(style.getSeparatorColor());
            if (style.getDrawableBackground() != null)
                this.setBackground(style.getDrawableBackground());
            else if (style.getBackground() != 0) this.setCardBackgroundColor(style.getBackground());
            if (style.getStrokeWidth() >= 0) this.setStrokeWidth(style.getStrokeWidth());
            if (style.getCornerRadius() >= 0) this.setRadius(style.getCornerRadius());
            if (style.getStrokeColor() != 0) this.setStrokeColor(style.getStrokeColor());
        }
    }

    public void setQuickSlotAvailableTextAppearance(int textAppearance) {
        if (textAppearance != 0) quickSlotAvailableText.setTextAppearance(textAppearance);
    }

    public void setQuickSlotAvailableTextColor(@ColorInt int color) {
        if (color != 0) quickSlotAvailableText.setTextColor(color);
    }
}
