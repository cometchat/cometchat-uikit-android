package com.cometchat.chatuikit.extensions.polls;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chatuikit.databinding.CometchatPollsOptionBinding;
import com.cometchat.chatuikit.shared.resources.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class PollOptionsAdapter extends RecyclerView.Adapter<PollOptionsAdapter.OptionViewHolder> {
    private static final String TAG = PollOptionsAdapter.class.getSimpleName();


    private final List<String> optionsArrayList = new ArrayList<>();

    private @StyleRes int optionTextAppearance;
    private @ColorInt int optionTextColor;
    private @ColorInt int optionHintColor;
    private @ColorInt int optionCornerRadius;
    private @ColorInt int optionStrokeWidth;
    private @ColorInt int optionStrokeColor;
    private @ColorInt int optionBackgroundColor;
    private Drawable backgroundDrawable;
    private Drawable dragIcon;
    private @ColorInt int dragIconTint;
    private final RecyclerView recyclerView;
    private static final int MAX_OPTIONS = 12;
    private final MutableLiveData<Boolean> isOptionFilled;

    public PollOptionsAdapter(MutableLiveData<Boolean> isOptionFilled, RecyclerView recyclerView) {
        optionsArrayList.add("");
        optionsArrayList.add("");
        this.recyclerView = recyclerView;
        this.isOptionFilled = isOptionFilled;
    }

    private void remove(int position) {
        if (position >= 0 && position < optionsArrayList.size()) {
            optionsArrayList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, optionsArrayList.size() - position);
        }
    }

    private void add(int position) {
        optionsArrayList.add("");
        notifyItemInserted(position + 1);
        recyclerView.scrollToPosition(position + 1);
    }

    public void moveItem(int fromPosition, int toPosition) {
        String item = optionsArrayList.remove(fromPosition);
        optionsArrayList.add(toPosition, item);
        notifyItemMoved(fromPosition, toPosition);
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CometchatPollsOptionBinding binding = CometchatPollsOptionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new OptionViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
        String option = optionsArrayList.get(position);
        holder.bindView(option);
    }

    public void setOptionTextAppearance(@StyleRes int optionTextAppearance) {
        this.optionTextAppearance = optionTextAppearance;
    }

    public void setOptionTextColor(@ColorInt int optionTextColor) {
        this.optionTextColor = optionTextColor;
    }

    public void setOptionHintColor(@ColorInt int optionHintColor) {
        this.optionHintColor = optionHintColor;
    }

    public void setOptionCornerRadius(@Dimension int optionCornerRadius) {
        this.optionCornerRadius = optionCornerRadius;
    }

    public void setOptionStrokeWidth(@Dimension int optionStrokeWidth) {
        this.optionStrokeWidth = optionStrokeWidth;
    }

    public void setOptionStrokeColor(@ColorInt int optionStrokeColor) {
        this.optionStrokeColor = optionStrokeColor;
    }

    public void setDragIcon(Drawable dragIcon) {
        this.dragIcon = dragIcon;
    }

    public void setOptionBackgroundColor(@ColorInt int backgroundColor) {
        this.optionBackgroundColor = backgroundColor;
    }

    public void setDragIconTint(@ColorInt int dragIconTint) {
        this.dragIconTint = dragIconTint;
    }

    @NonNull
    public List<String> getOptionsArrayList() {
        return optionsArrayList;
    }

    @Override
    public int getItemCount() {
        return optionsArrayList.size();
    }

    public void setBackgroundDrawable(Drawable backgroundDrawable) {
        this.backgroundDrawable = backgroundDrawable;
    }

    public class OptionViewHolder extends RecyclerView.ViewHolder {
        public CometchatPollsOptionBinding binding;
        private boolean isAddOperationPending = false;

        OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = CometchatPollsOptionBinding.bind(itemView);
            Utils.initMaterialCard(binding.optionCard);
        }

        public void bindView(String option) {
            binding.etOption.setText(option);
            binding.etOption.setTextAppearance(optionTextAppearance);
            binding.etOption.setHintTextColor(optionHintColor);
            binding.etOption.setTextColor(optionTextColor);
            binding.optionCard.setStrokeColor(optionStrokeColor);
            binding.optionCard.setStrokeWidth(optionStrokeWidth);
            binding.optionCard.setRadius(optionCornerRadius);
            binding.optionCard.setCardBackgroundColor(optionBackgroundColor);
            binding.drag.setImageDrawable(dragIcon);
            binding.drag.setImageTintList(ColorStateList.valueOf(dragIconTint));

            if (backgroundDrawable != null)
                binding.optionCard.setBackgroundDrawable(backgroundDrawable);

            if (option.isEmpty()) binding.drag.setVisibility(View.GONE);
            else binding.drag.setVisibility(View.VISIBLE);
            isAddOperationPending = true;
            binding.etOption.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    int position = getAbsoluteAdapterPosition();
                    optionsArrayList.set(position, charSequence.toString());
                    setSendButtonState();
                    if (charSequence.length() > 0) {
                        binding.drag.setVisibility(View.VISIBLE);
                        if (isAddOperationPending) {
                            if (position == 0 || position == 1) {
                                if (!optionsArrayList.get(0).trim().isEmpty() && !optionsArrayList.get(1).trim().isEmpty()) {
                                    if (optionsArrayList.size() == 2) {
                                        isAddOperationPending = false;
                                        recyclerView.post(() -> add(optionsArrayList.size()));
                                    }
                                }
                            } else if (position == optionsArrayList.size() - 1 && optionsArrayList.size() < MAX_OPTIONS) {
                                isAddOperationPending = false;
                                recyclerView.post(() -> add(position));
                            }
                        }
                    } else {
                        binding.drag.setVisibility(View.GONE);
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });

            binding.etOption.setOnFocusChangeListener((view, hasFocus) -> {
                if (!hasFocus) {
                    checkIfAnyPositionIsEmpty(getAbsoluteAdapterPosition());
                }
            });
        }
    }

    private void checkIfAnyPositionIsEmpty(int currentPosition) {
        if (optionsArrayList.get(currentPosition).isEmpty()) {
            if (optionsArrayList.size() != 2 && currentPosition != optionsArrayList.size() - 1 && currentPosition != -1) {
                optionsArrayList.size();
                recyclerView.post(() -> remove(currentPosition));
            }
        }
    }

    private void setSendButtonState() {
        if (!optionsArrayList.get(0).trim().isEmpty() && !optionsArrayList.get(1).trim().isEmpty()) {
            isOptionFilled.setValue(true);
        } else {
            isOptionFilled.setValue(false);
        }
    }
}
