package com.cometchat.chatuikit.shared.views.singleselect;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.models.interactiveelements.OptionElement;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class CometChatSingleSelect extends MaterialCardView {
    private static final String TAG = CometChatSingleSelect.class.getSimpleName();

    private Context context;
    private CompoundButton.OnCheckedChangeListener listener;
    private RadioGroup radioGroup;
    private TextView title;
    private SingleSelectStyle style;

    public CometChatSingleSelect(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setCardBackgroundColor(Color.TRANSPARENT);
        setCardElevation(0);
        setRadius(0);
        setStrokeWidth(0);
        this.context = context;
        View view = View.inflate(context, R.layout.cometchat_single_select_layout, null);
        radioGroup = view.findViewById(R.id.radio_group);
        title = view.findViewById(R.id.title_text);
        addView(view);
    }

    public CometChatSingleSelect(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CometChatSingleSelect(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setOptions(@Nullable List<OptionElement> options) {
        if (options != null && !options.isEmpty()) {
            for (int i = 0; i < options.size(); i++) {
                OptionElement optionElement = options.get(i);
                if (optionElement.getValue() != null && !optionElement.getValue().isEmpty() && optionElement.getId() != null && !optionElement
                    .getId()
                    .isEmpty()) {
                    RadioButton radioButton = new RadioButton(context);
                    Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.cometchat_single_select_all_side);
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                                           ViewGroup.LayoutParams.WRAP_CONTENT);

                    if (options.size() == 2) {
                        layoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
                        layoutParams.weight = 1;
                        if (i == 0)
                            drawable = AppCompatResources.getDrawable(context, R.drawable.cometchat_single_select_1);
                        else if (i == 1)
                            drawable = AppCompatResources.getDrawable(context, R.drawable.cometchat_single_select_2);
                    }

                    layoutParams.topMargin = Utils.convertDpToPx(context, 3);
                    radioButton.setLayoutParams(layoutParams);
                    if (style != null && style.getButtonStrokeColor() != 0) {
                        if (drawable != null) {
                            drawable.setTint(style.getButtonStrokeColor());
                        }
                    }

                    radioButton.setBackground(drawable);
                    radioButton.setPadding(20, 20, 20, 20);
                    radioButton.setButtonDrawable(null);
                    radioButton.setText(optionElement.getValue());
                    radioButton.setTextAlignment(TEXT_ALIGNMENT_CENTER);
                    radioButton.setTag(optionElement.getId());

                    setRadioButtonStyle(radioButton);

                    radioButton.setOnCheckedChangeListener((compoundButton, selected) -> {
                        if (selected) {
                            resetSingleSelectElement();
                            if (style != null) {
                                if (style.getSelectedOptionTextColor() != 0)
                                    compoundButton.setTextColor(style.getSelectedOptionTextColor());
                                if (style.getSelectedOptionTextAppearance() != 0)
                                    compoundButton.setTextAppearance(style.getSelectedOptionTextAppearance());
                            }
                        } else {
                            if (style != null) {
                                if (style.getOptionTextColor() != 0)
                                    compoundButton.setTextColor(style.getOptionTextColor());
                                if (style.getOptionTextAppearance() != 0)
                                    compoundButton.setTextAppearance(style.getOptionTextAppearance());
                            }
                        }

                        if (listener != null) listener.onCheckedChanged(compoundButton, selected);
                    });

                    radioGroup.addView(radioButton);
                }
            }
            if (radioGroup.getChildCount() == 2) {
                radioGroup.setOrientation(LinearLayout.HORIZONTAL);
            } else {
                radioGroup.setOrientation(LinearLayout.VERTICAL);
            }
        }
    }

    private void setRadioButtonStyle(RadioButton radioButton) {
        if (style != null) {
            if (style.getOptionTextColor() != 0)
                radioButton.setTextColor(style.getOptionTextColor());
            if (style.getOptionTextAppearance() != 0)
                radioButton.setTextAppearance(style.getOptionTextAppearance());
        }
    }

    public void resetSingleSelectElement() {
        if (radioGroup != null) {
            for (int i = 0; i < radioGroup.getChildCount(); i++) {
                RadioButton radioButton = (RadioButton) radioGroup.getChildAt(i);
                if (style != null) {
                    if (style.getOptionTextColor() != 0) {
                        radioButton.setTextColor(style.getOptionTextColor());
                    }
                    if (style.getOptionTextAppearance() != 0) {
                        radioButton.setTextAppearance(style.getOptionTextAppearance());
                    }
                    if (style != null && style.getButtonStrokeColor() != 0) {
                        Drawable drawable = radioButton.getBackground();
                        drawable.setTint(style.getButtonStrokeColor());
                        radioButton.setBackground(drawable);
                    }
                }
            }
        }
    }

    public void setOnSelectChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        if (listener != null) {
            this.listener = listener;
        }
    }

    public RadioGroup getRadioGroup() {
        return radioGroup;
    }

    public void setTitle(String title) {
        if (title != null && !title.isEmpty()) {
            this.title.setVisibility(VISIBLE);
            this.title.setText(title);
            if (style != null) {
                if (style.getTitleAppearance() != 0)
                    this.title.setTextAppearance(style.getTitleAppearance());
                if (style.getTitleColor() != 0) this.title.setTextColor(style.getTitleColor());
            }
        } else {
            this.title.setVisibility(GONE);
        }
    }

    public void setStyle(SingleSelectStyle style) {
        if (style != null) {
            setTitleColor(style.getTitleColor());
            setTitleAppearance(style.getTitleAppearance());
            this.style = style;
            if (style.getDrawableBackground() != null)
                this.setBackground(style.getDrawableBackground());
            else if (style.getBackground() != 0) this.setCardBackgroundColor(style.getBackground());
            if (style.getStrokeWidth() >= 0) this.setStrokeWidth(style.getStrokeWidth());
            if (style.getCornerRadius() >= 0) this.setRadius(style.getCornerRadius());
            if (style.getStrokeColor() != 0) this.setStrokeColor(style.getStrokeColor());
        }
    }

    private void setTitleColor(int titleColor) {
        if (titleColor != 0) title.setTextColor(titleColor);
    }

    private void setTitleAppearance(int titleAppearance) {
        if (titleAppearance != 0) title.setTextAppearance(titleAppearance);
    }
}
