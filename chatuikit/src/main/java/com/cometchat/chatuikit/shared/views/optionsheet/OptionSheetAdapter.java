package com.cometchat.chatuikit.shared.views.optionsheet;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.databinding.CometchatActionListItemBinding;

import java.util.List;

/**
 * Adapter for displaying OptionSheetMenuItem objects in a RecyclerView.
 */
@SuppressWarnings("unused")
public class OptionSheetAdapter extends RecyclerView.Adapter<OptionSheetAdapter.ActionItemViewHolder> {
    private static final String TAG = OptionSheetAdapter.class.getSimpleName();
    private final Context context;
    private final List<OptionSheetMenuItem> optionItems;

    private boolean hideText;
    private boolean hideDivider = true;
    private int textAlignment;

    private @ColorInt int iconTint;
    private @ColorInt int textColor;
    private @StyleRes int textAppearance;

    /**
     * Creates an instance of ActionSheetAdapter.
     *
     * @param context     The context to use for loading resources.
     * @param optionItems The list of OptionSheetMenuItem objects to display.
     */
    public OptionSheetAdapter(Context context, List<OptionSheetMenuItem> optionItems) {
        this.context = context;
        this.optionItems = optionItems;
        textAlignment = View.TEXT_ALIGNMENT_TEXT_START;
    }

    /**
     * Creates a new {@link ActionItemViewHolder} instance. This method inflates the
     * appropriate layout based on the {@code isGridLayout} flag and returns a new
     * instance of {@link ActionItemViewHolder}.
     *
     * @param parent   The parent {@link ViewGroup} into which the new view will be
     *                 added.
     * @param viewType The type of the view to create. This is generally used when
     *                 different view types are needed.
     * @return A new {@link ActionItemViewHolder} instance.
     */
    @NonNull
    @Override
    public ActionItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CometchatActionListItemBinding binding = CometchatActionListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ActionItemViewHolder(binding.getRoot());
    }

    /**
     * Binds data to the {@link ActionItemViewHolder} at the specified position.
     * This method sets the visibility of the action text, updates the icon
     * resource, tint color, text color, and text appearance based on the provided
     * {@link OptionSheetMenuItem} object.
     *
     * @param holder   The {@link ActionItemViewHolder} to bind data to.
     * @param position The position in the data set for the item to bind.
     */
    @Override
    public void onBindViewHolder(@NonNull ActionItemViewHolder holder, int position) {
        OptionSheetMenuItem item = optionItems.get(position);
        if (item == null) return;
        holder.binding.text.setTextAlignment(textAlignment);
        holder.binding.text.setVisibility(hideText ? View.GONE : View.VISIBLE);
        holder.binding.text.setText(item.getText());
        holder.binding.icon.setImageResource(item.getStartIcon());
        holder.binding.text.setTextAppearance(textAppearance);
        if (item.getText().equals(context.getString(R.string.cometchat_delete))) {
            holder.binding.text.setTextColor(context.getResources().getColor(R.color.cometchat_color_error, context.getTheme()));
            holder.binding.icon.setImageTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.cometchat_color_error, context.getTheme())));
        } else {
            holder.binding.text.setTextColor(textColor);
            holder.binding.icon.setImageTintList(ColorStateList.valueOf(iconTint));
        }
        holder.binding.divider.setVisibility(hideDivider ? View.GONE : View.VISIBLE);
        holder.itemView.setTag(R.string.cometchat_action_item, item);
    }

    /**
     * Returns the total number of items in the data set. This method is used by the
     * {@link RecyclerView} to determine how many items it should display.
     *
     * @return The number of items in the data set.
     */
    @Override
    public int getItemCount() {
        return optionItems.size();
    }

    /**
     * Hides or shows the text in the items.
     *
     * @param hideText Boolean indicating whether to hide the text.
     */
    public void hideText(boolean hideText) {
        if (this.hideText != hideText) {
            this.hideText = hideText;
            notifyItemRangeChanged(0, optionItems.size());
        }
    }

    /**
     * Hides or shows the Divider in the items.
     *
     * @param hideDivider Boolean indicating whether to hide Divider or not.
     */
    public void hideDivider(boolean hideDivider) {
        if (this.hideDivider != hideDivider) {
            this.hideDivider = hideDivider;
            notifyItemRangeChanged(0, optionItems.size());
        }
    }

    /**
     * Adds a new OptionSheetMenuItem to the list.
     *
     * @param optionItem The OptionSheetMenuItem to add.
     */
    public void addOptionItem(OptionSheetMenuItem optionItem) {
        int position = optionItems.size();
        optionItems.add(optionItem);
        notifyItemInserted(position);
    }

    /**
     * Updates an existing OptionSheetMenuItem in the list.
     *
     * @param optionItem The OptionSheetMenuItem with updated data.
     */
    public void updateOptionItem(OptionSheetMenuItem optionItem) {
        for (int i = 0; i < optionItems.size(); i++) {
            OptionSheetMenuItem item = optionItems.get(i);
            if (item.getId().equals(optionItem.getId())) {
                item.setText(optionItem.getText());
                item.setStartIcon(optionItem.getStartIcon());
                notifyItemChanged(i);
                break;
            }
        }
    }

    /**
     * Returns the current color of the icon.
     *
     * @return The color of the icon as an integer value.
     */
    public int getIconTint() {
        return iconTint;
    }

    /**
     * Sets the color of the icon.
     *
     * @param iconTint The color to set for the icon. This should be a color resource or
     *                 a color value.
     */
    public void setIconTint(int iconTint) {
        this.iconTint = iconTint;
    }

    /**
     * Returns the current color of the text.
     *
     * @return The color of the text as an integer value.
     */
    public int getTextColor() {
        return textColor;
    }

    /**
     * Sets the color of the text.
     *
     * @param textColor The color to set for the text. This should be a color resource or
     *                  a color value.
     */
    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    /**
     * Returns the current style of the text.
     *
     * @return The text style as an integer value. This typically represents a text
     * appearance or style resource.
     */
    public int getTextAppearance() {
        return textAppearance;
    }

    /**
     * Sets the style of the text.
     *
     * @param textAppearance The style to set for the text. This should be a text appearance or
     *                       style resource.
     */
    public void setTextAppearance(int textAppearance) {
        this.textAppearance = textAppearance;
    }

    /**
     * ViewHolder for OptionSheetMenuItem views.
     */
    public static class ActionItemViewHolder extends RecyclerView.ViewHolder {
        private final CometchatActionListItemBinding binding;

        public ActionItemViewHolder(@NonNull View view) {
            super(view);
            binding = CometchatActionListItemBinding.bind(itemView);
        }
    }

    /**
     * Sets the text alignment for the items.
     */
    public void setItemTextAlignment(int textAlignment) {
        this.textAlignment = textAlignment;
        notifyItemRangeChanged(0, optionItems.size());
    }
}
