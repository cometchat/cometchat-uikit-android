package com.cometchat.chatuikit.shared.views.schedulerbubble;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.models.interactiveelements.BaseInteractiveElement;
import com.cometchat.chatuikit.shared.models.interactiveelements.ButtonElement;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ScheduleView extends MaterialCardView {
    private static final String TAG = ScheduleView.class.getSimpleName();


    private ImageView clockImageView, calendarImageView, timeZoneImageView;
    private TextView timeZoneTextView, timeTextView, durationTextView;
    private LinearLayout scheduleButton, scheduleButtonHolder;
    private ScheduleStyle scheduleStyle;
    private OnSubmitClick onSubmitClick;
    private TextView errorTextView;
    private SimpleDateFormat scheduleTimeFormat;
    private DateTimeRange dateTimeRange;
    private MaterialCardView buttonCard;
    private TextView buttonTitle;
    private ProgressBar progressBar;

    public ScheduleView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        Utils.initMaterialCard(this);
        View view = View.inflate(context, R.layout.cometchat_schedule_view, null);
        scheduleButton = view.findViewById(R.id.schedule_btn);
        scheduleTimeFormat = new SimpleDateFormat("hh:mm a, EEEE, dd MMMM yyyy", Locale.US);
        scheduleButtonHolder = view.findViewById(R.id.button_view_holder);
        clockImageView = view.findViewById(R.id.clock_image);
        calendarImageView = view.findViewById(R.id.calender_image);
        timeZoneImageView = view.findViewById(R.id.time_zone_icon);
        timeZoneTextView = view.findViewById(R.id.time_zone_text);
        timeTextView = view.findViewById(R.id.schedule_time);
        durationTextView = view.findViewById(R.id.duration);
        errorTextView = view.findViewById(R.id.error_message);
        addView(view);
    }

    public ScheduleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ScheduleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setTimeZone(String timeZone) {
        if (timeZone != null && !timeZone.isEmpty()) {
            timeZoneTextView.setText(timeZone);
        }
    }

    public void setTime(DateTimeRange time) {
        if (time != null) {
            this.dateTimeRange = time;
            timeTextView.setText(scheduleTimeFormat.format(time.getFrom().getTime()));
        }
    }

    public void setDuration(String duration) {
        if (duration != null && !duration.isEmpty()) {
            durationTextView.setText(duration + " min");
        }
    }

    public void setTimeZoneIcon(int icon) {
        if (icon != 0) {
            timeZoneImageView.setImageResource(icon);
        }
    }

    public void setClockIcon(int icon) {
        if (icon != 0) {
            clockImageView.setImageResource(icon);
        }
    }

    public void setCalendarIcon(int icon) {
        if (icon != 0) {
            calendarImageView.setImageResource(icon);
        }
    }

    public void setDurationTextSize(int size) {
        if (size != 0) {
            durationTextView.setTextSize(size);
        }
    }

    public void setTimeTextSize(int size) {
        if (size != 0) {
            timeTextView.setTextSize(size);
        }
    }

    public void setErrorText(String errorText) {
        if (errorText != null && !errorText.isEmpty()) {
            errorTextView.setText(errorText);
        }
    }

    public void setErrorTextSize(int size) {
        if (size != 0) {
            errorTextView.setTextSize(size);
        }
    }

    public void setErrorVisibility(int visibility) {
        errorTextView.setVisibility(visibility);
    }

    public void setTimeZoneVisibility(int visibility) {
        timeZoneTextView.setVisibility(visibility);
        timeZoneImageView.setVisibility(visibility);
    }

    public void setClockVisibility(int visibility) {
        clockImageView.setVisibility(visibility);
    }

    public void setCalendarVisibility(int visibility) {
        calendarImageView.setVisibility(visibility);
    }

    public void setTimeVisibility(int visibility) {
        timeTextView.setVisibility(visibility);
    }

    public void setDurationVisibility(int visibility) {
        durationTextView.setVisibility(visibility);
    }

    public void setScheduleButtonVisibility(int visibility) {
        scheduleButton.setVisibility(visibility);
    }

    public void setProgressBarVisibility(int visibility) {
        if (progressBar != null) {
            progressBar.setVisibility(visibility);
        }
    }

    public void setButtonTitleVisibility(int visibility) {
        if (buttonTitle != null) {
            buttonTitle.setVisibility(visibility);
        }
    }

    public void enableButton(boolean enable) {
        if (buttonCard != null) {
            buttonCard.setEnabled(enable);
        }
    }

    public void setTimeZoneTextSize(int size) {
        if (size != 0) {
            timeZoneTextView.setTextSize(size);
        }
    }

    public void setButtonTitle(String title) {
        if (title != null && !title.isEmpty()) buttonTitle.setText(title);
    }

    public MaterialCardView getButtonCard() {
        return buttonCard;
    }

    public void setSubmitButton(@Nullable ButtonElement buttonElement) {
        if (buttonElement != null) {

            View buttonView = View.inflate(getContext(), R.layout.cometchat_form_button_material, null);
            scheduleButtonHolder.setVisibility(VISIBLE);
            MaterialCardView button = buttonView.findViewById(R.id.form_button);
            buttonCard = buttonView.findViewById(R.id.form_button_card);
            buttonTitle = buttonView.findViewById(R.id.button_text);
            progressBar = buttonView.findViewById(R.id.button_progress);
            if (scheduleStyle != null) {
                if (scheduleStyle.getProgressBarTintColor() != 0)
                    progressBar
                        .getIndeterminateDrawable()
                        .setColorFilter(scheduleStyle.getProgressBarTintColor(), android.graphics.PorterDuff.Mode.MULTIPLY);

                if (scheduleStyle.getButtonBackgroundColor() != 0)
                    buttonCard.setBackgroundColor(scheduleStyle.getButtonBackgroundColor());

                if (scheduleStyle.getButtonTextColor() != 0)
                    buttonTitle.setTextColor(scheduleStyle.getButtonTextColor());

                if (scheduleStyle.getButtonTextAppearance() != 0)
                    buttonTitle.setTextAppearance(scheduleStyle.getButtonTextAppearance());
            }
            button.setCardBackgroundColor(Color.TRANSPARENT);
            buttonTitle.setText(buttonElement.getText());
            button.setTag(buttonElement.getElementId());
            buttonCard.setOnClickListener(view -> {
                if (onSubmitClick != null) {
                    onSubmitClick.onSubmitClick(getContext(), buttonElement, dateTimeRange);
                } else {
                    Toast.makeText(getContext(), "Please implement OnSubmitClick", Toast.LENGTH_SHORT).show();
                }
            });
            addViewToFormLayout(buttonView, 4);
            button.setRadius(16);
            buttonCard.setRadius(16);
        } else {
            scheduleButtonHolder.setVisibility(GONE);
        }
    }

    private void addViewToFormLayout(View view, int topMargin) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                               ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = Utils.convertDpToPx(getContext(), topMargin);
        view.setLayoutParams(layoutParams);
        scheduleButton.removeAllViews();
        scheduleButton.addView(view);
    }

    public void setOnSubmitClick(OnSubmitClick onSubmitClick) {
        if (onSubmitClick != null) this.onSubmitClick = onSubmitClick;
    }

    public void setStyle(ScheduleStyle style) {
        if (style != null) {
            this.scheduleStyle = style;

            setErrorTextColor(style.getErrorTextColor());
            setErrorTextAppearance(style.getErrorTextAppearance());

            setDurationTextColor(style.getDurationTextColor());
            setDurationTextAppearance(style.getDurationTextAppearance());

            setTimeTextColor(style.getTimeTextColor());
            setTimeTextAppearance(style.getTimeTextAppearance());

            setCalendarIconTint(style.getCalendarIconTint());
            setClockIconTint(style.getClockIconTint());
            setTimeZoneIconTint(style.getTimeZoneIconTint());
            setTimeZoneTextAppearance(style.getTimeTextAppearance());
            setTimeZoneTextColor(style.getTimeTextColor());
            if (style.getDrawableBackground() != null)
                this.setBackground(style.getDrawableBackground());
            else if (style.getBackground() != 0) this.setCardBackgroundColor(style.getBackground());
            if (style.getStrokeWidth() >= 0) this.setStrokeWidth(style.getStrokeWidth());
            if (style.getCornerRadius() >= 0) this.setRadius(style.getCornerRadius());
            if (style.getStrokeColor() != 0) this.setStrokeColor(style.getStrokeColor());
        }
    }

    public void setErrorTextColor(int color) {
        if (color != 0) {
            errorTextView.setTextColor(color);
        }
    }

    public void setErrorTextAppearance(int style) {
        if (style != 0) {
            errorTextView.setTextAppearance(style);
        }
    }

    public void setDurationTextColor(int color) {
        if (color != 0) {
            durationTextView.setTextColor(color);
        }
    }

    public void setDurationTextAppearance(int style) {
        if (style != 0) {
            durationTextView.setTextAppearance(style);
        }
    }

    public void setTimeTextColor(int color) {
        if (color != 0) {
            timeTextView.setTextColor(color);
        }
    }

    public void setTimeTextAppearance(int style) {
        if (style != 0) {
            timeTextView.setTextAppearance(style);
        }
    }

    public void setCalendarIconTint(int color) {
        if (color != 0) {
            calendarImageView.setColorFilter(color);
        }
    }

    public void setClockIconTint(int color) {
        if (color != 0) {
            clockImageView.setColorFilter(color);
        }
    }

    public void setTimeZoneIconTint(int color) {
        if (color != 0) {
            timeZoneImageView.setColorFilter(color);
        }
    }

    public void setTimeZoneTextAppearance(int style) {
        if (style != 0) {
            timeZoneTextView.setTextAppearance(style);
        }
    }

    public void setTimeZoneTextColor(int color) {
        if (color != 0) {
            timeZoneTextView.setTextColor(color);
        }
    }

    public interface OnSubmitClick {
        void onSubmitClick(Context context, BaseInteractiveElement baseInputElements, DateTimeRange dateTimeRange);
    }
}
