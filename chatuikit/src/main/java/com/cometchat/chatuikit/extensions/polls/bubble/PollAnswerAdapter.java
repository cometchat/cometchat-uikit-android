package com.cometchat.chatuikit.extensions.polls.bubble;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chat.models.CustomMessage;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.extensions.ExtensionConstants;
import com.cometchat.chatuikit.extensions.Extensions;
import com.cometchat.chatuikit.extensions.polls.OnOptionClick;
import com.cometchat.chatuikit.extensions.polls.bubble.chipviews.ImageAndCountView;
import com.cometchat.chatuikit.extensions.polls.bubble.chipviews.ImageTextPoJo;
import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressLint("NotifyDataSetChanged")
public class PollAnswerAdapter extends RecyclerView.Adapter<PollAnswerAdapter.MyViewHolder> {
    private static final String TAG = PollAnswerAdapter.class.getSimpleName();

    private CustomMessage customMessage;
    private JSONObject optionsJson;
    private int myChosenOptionPosition = -1;

    private final List<String> options;
    private final OnOptionClick onOptionClick;

    private @ColorInt int progressColor;
    private @ColorInt int progressBackgroundColor;
    private Drawable selectedStateDrawable;
    private Drawable unselectedStateDrawable;
    private @ColorInt int voteCountTextColor;
    private @ColorInt int selectedIconTint;
    private @ColorInt int selectedRadioButtonStrokeColor;
    private @Dimension int selectedRadioButtonCornerRadius;
    private @Dimension int selectedRadioButtonStrokeWidth;
    private @ColorInt int unselectedRadioButtonStrokeColor;
    private @ColorInt int unselectedIconTint;
    private @Dimension int unselectedRadioButtonCornerRadius;
    private @Dimension int unselectedRadioButtonStrokeWidth;
    private @StyleRes int optionAvatarStyle;
    private @ColorInt int optionTextColor;
    private @StyleRes int optionTextAppearance;
    private @StyleRes int voteCountTextAppearance;
    private @ColorInt int progressIndeterminateTint;

    public PollAnswerAdapter(OnOptionClick onOptionClick) {
        this.options = new ArrayList<>();
        optionsJson = new JSONObject();
        this.onOptionClick = onOptionClick;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.cometchat_poll_answer_row_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.bindView(options.get(position), position);
    }

    private List<ImageTextPoJo> getReactors(int position) {
        List<ImageTextPoJo> list = new ArrayList<>();
        JSONObject result = Extensions.getPollsResult(customMessage);
        try {
            if (result.has(ExtensionConstants.ExtensionJSONField.OPTIONS)) {
                JSONObject options = result.getJSONObject(ExtensionConstants.ExtensionJSONField.OPTIONS);
                JSONObject optionK = options.getJSONObject(String.valueOf(position + 1));
                if (optionK.has(ExtensionConstants.ExtensionJSONField.VOTERS)) {
                    JSONObject voters = optionK.getJSONObject(ExtensionConstants.ExtensionJSONField.VOTERS);
                    int loopSize = 0;
                    Iterator<String> keys = voters.keys();
                    while (keys.hasNext() && loopSize < 3) {
                        String key = keys.next();
                        JSONObject voter = voters.getJSONObject(key);
                        ImageTextPoJo imageTextPoJo = new ImageTextPoJo();
                        if (voter.has(ExtensionConstants.ExtensionJSONField.AVATAR))
                            imageTextPoJo.setImageUrl(voter.getString(ExtensionConstants.ExtensionJSONField.AVATAR));
                        if (voter.has(ExtensionConstants.ExtensionJSONField.NAME))
                            imageTextPoJo.setText(voter.getString(ExtensionConstants.ExtensionJSONField.NAME));
                        list.add(imageTextPoJo);
                        loopSize++;
                    }
                }
            }
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }
        return list;
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    public void setMessage(CustomMessage baseMessage) {
        if (baseMessage != null) {
            options.clear();
            this.customMessage = baseMessage;
            try {
                JSONObject jsonObject = baseMessage.getCustomData();
                optionsJson = jsonObject.getJSONObject(ExtensionConstants.ExtensionJSONField.OPTIONS);
                for (int i = 1; i <= optionsJson.length(); i++) {
                    this.options.add(optionsJson.getString(i + ""));
                }
                notifyDataSetChanged();
            } catch (Exception e) {
                CometChatLogger.e(TAG, e.toString());
            }
        }
    }

    public void setMyChosenOptionPosition(int myChosenOptionPosition) {
        this.myChosenOptionPosition = myChosenOptionPosition;
        notifyDataSetChanged();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView radioButton;
        private final ImageAndCountView imageAndCountView;
        private final ProgressBar optionProgressIndicator;
        private final TextView optionText;
        private final GradientDrawable progressBackgroundDrawable;
        private final ScaleDrawable progressLayerDrawable;
        private final ProgressBar progressIndicator;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.check_button);
            imageAndCountView = itemView.findViewById(R.id.image_and_count_view);
            optionProgressIndicator = itemView.findViewById(R.id.progress_bar);
            progressIndicator = itemView.findViewById(R.id.progress_indicator);
            optionText = itemView.findViewById(R.id.option_text);
            LayerDrawable progressDrawable = (LayerDrawable) optionProgressIndicator.getProgressDrawable();
            // Change the background color programmatically (the first layer)
            progressBackgroundDrawable = (GradientDrawable) progressDrawable.findDrawableByLayerId(R.id.background);
            progressLayerDrawable = (ScaleDrawable) progressDrawable.findDrawableByLayerId(android.R.id.progress);
            Utils.initMaterialCard(radioButton);
        }

        public void bindView(String option, int position) {
            optionText.setText(option);
            radioButton.setTag(option);
            radioButton.setVisibility(View.VISIBLE);
            progressIndicator.setVisibility(View.GONE);
            int voteCount = Extensions.getVoteCount(customMessage);
            ArrayList<String> voterInfo = Extensions.getVoterInfo(customMessage, optionsJson.length());
            int votedOnOption = Integer.parseInt(voterInfo.get(position));
            applyStyle();
            if (votedOnOption == 0) {
                imageAndCountView.setVisibility(View.GONE);
            } else {
                imageAndCountView.setVisibility(View.VISIBLE);
                imageAndCountView.setData(getReactors(position), votedOnOption);
            }
            int percentage = 0;
            if (voteCount != 0) percentage = Math.round((float) (votedOnOption * 100) / voteCount);
            // animateProgress(optionProgressIndicator, percentage);
            optionProgressIndicator.setProgress(percentage);

            if (myChosenOptionPosition > -1) {
                if (myChosenOptionPosition == position) {
                    applySelectedStyle();
                } else {
                    applyUnselectedStyle();
                }
            }

            radioButton.setOnClickListener(v -> {
                if (myChosenOptionPosition != position) {
                    myChosenOptionPosition = position;
                    if (onOptionClick != null)
                        onOptionClick.onClick(customMessage, options.get(position), position);
                    radioButton.setVisibility(View.GONE);
                    progressIndicator.setVisibility(View.VISIBLE);
                }
            });
        }

        private void applyStyle() {
            optionText.setTextAppearance(optionTextAppearance);
            optionText.setTextColor(optionTextColor);
            imageAndCountView.setAvatarStyle(optionAvatarStyle);
            imageAndCountView.setCountTextAppearance(voteCountTextAppearance);
            imageAndCountView.setCountTextColor(voteCountTextColor);
            progressBackgroundDrawable.setColor(progressBackgroundColor);
            progressLayerDrawable.setColorFilter(progressColor, android.graphics.PorterDuff.Mode.SRC_IN);
            progressIndicator.getIndeterminateDrawable().setColorFilter(progressIndeterminateTint, android.graphics.PorterDuff.Mode.SRC_IN);
            applyUnselectedStyle();
        }

        private void applyUnselectedStyle() {
            radioButton.setBackgroundDrawable(unselectedStateDrawable);
            radioButton.setBackgroundTintList(ColorStateList.valueOf(unselectedIconTint));
            radioButton.setStrokeColor(unselectedRadioButtonStrokeColor);
            radioButton.setStrokeWidth(unselectedRadioButtonStrokeWidth);
            radioButton.setRadius(unselectedRadioButtonCornerRadius);
        }

        private void applySelectedStyle() {
            radioButton.setBackgroundDrawable(selectedStateDrawable);
            radioButton.setBackgroundTintList(ColorStateList.valueOf(selectedIconTint));
            radioButton.setStrokeColor(selectedRadioButtonStrokeColor);
            radioButton.setStrokeWidth(selectedRadioButtonStrokeWidth);
            radioButton.setRadius(selectedRadioButtonCornerRadius);
        }

        private void animateProgress(ProgressBar progressBar, int toProgress) {
            int PROGRESS_ANIMATION_SPEED = 500;
            String ANIMATION_NAME = "progress";
            ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, ANIMATION_NAME, progressBar.getProgress(), toProgress);
            animation.setDuration(PROGRESS_ANIMATION_SPEED);
            animation.setInterpolator(new android.view.animation.LinearInterpolator());
            animation.start();
        }
    }

    public void setProgressColor(@ColorInt int progressColor) {
        this.progressColor = progressColor;
    }

    public void setProgressBackgroundColor(@ColorInt int progressBackgroundColor) {
        this.progressBackgroundColor = progressBackgroundColor;
    }

    public void setSelectedStateDrawable(Drawable selectedStateDrawable) {
        this.selectedStateDrawable = selectedStateDrawable;
    }

    public void setUnselectedStateDrawable(Drawable unselectedStateDrawable) {
        this.unselectedStateDrawable = unselectedStateDrawable;
    }

    public void setSelectedIconTint(@ColorInt int selectedIconTint) {
        this.selectedIconTint = selectedIconTint;
    }

    public void setSelectedRadioButtonStrokeColor(@ColorInt int selectedRadioButtonStrokeColor) {
        this.selectedRadioButtonStrokeColor = selectedRadioButtonStrokeColor;
    }

    public void setVoteCountTextColor(@ColorInt int voteCountTextColor) {
        this.voteCountTextColor = voteCountTextColor;
    }

    public void setSelectedRadioButtonCornerRadius(@Dimension int selectedRadioButtonCornerRadius) {
        this.selectedRadioButtonCornerRadius = selectedRadioButtonCornerRadius;
    }

    public void setSelectedRadioButtonStrokeWidth(@Dimension int selectedRadioButtonStrokeWidth) {
        this.selectedRadioButtonStrokeWidth = selectedRadioButtonStrokeWidth;
    }

    public void setOptionAvatarStyle(@StyleRes int optionAvatarStyle) {
        this.optionAvatarStyle = optionAvatarStyle;
    }

    public void setOptionTextColor(@ColorInt int optionTextColor) {
        this.optionTextColor = optionTextColor;
    }

    public void setOptionTextAppearance(@StyleRes int optionTextAppearance) {
        this.optionTextAppearance = optionTextAppearance;
    }

    public void setVoteCountTextAppearance(@StyleRes int voteCountTextAppearance) {
        this.voteCountTextAppearance = voteCountTextAppearance;
    }

    public void setUnselectedIconTint(@ColorInt int unselectedIconTint) {
        this.unselectedIconTint = unselectedIconTint;
    }

    public void setUnselectedRadioButtonStrokeColor(@ColorInt int unselectedRadioButtonStrokeColor) {
        this.unselectedRadioButtonStrokeColor = unselectedRadioButtonStrokeColor;
    }

    public void setUnselectedRadioButtonCornerRadius(@Dimension int unselectedRadioButtonCornerRadius) {
        this.unselectedRadioButtonCornerRadius = unselectedRadioButtonCornerRadius;
    }

    public void setUnselectedRadioButtonStrokeWidth(@Dimension int unselectedRadioButtonStrokeWidth) {
        this.unselectedRadioButtonStrokeWidth = unselectedRadioButtonStrokeWidth;
    }

    public void setProgressIndeterminateTint(@ColorInt int color) {
        this.progressIndeterminateTint = color;
    }
}
