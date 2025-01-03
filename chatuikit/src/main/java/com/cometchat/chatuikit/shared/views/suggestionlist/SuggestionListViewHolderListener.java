package com.cometchat.chatuikit.shared.views.suggestionlist;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chatuikit.databinding.CometchatSuggestionListItemsBinding;
import com.cometchat.chatuikit.shared.interfaces.ViewHolderCallBack;

import java.util.List;

/**
 * An abstract class that serves as a listener for managing the creation and
 * binding of views within a suggestion list. It implements the
 * {@link ViewHolderCallBack} interface. This class provides methods to create
 * and bind views for individual items in a suggestion list, allowing for
 * customization of item appearance and behavior.
 */
public abstract class SuggestionListViewHolderListener implements ViewHolderCallBack {
    private static final String TAG = SuggestionListViewHolderListener.class.getSimpleName();

    /**
     * Creates a new view for the suggestion list item.
     *
     * @param context the context in which the view will be created
     * @param view    the binding for the suggestion list item
     * @return the created view
     */
    public abstract View createView(Context context, CometchatSuggestionListItemsBinding view);

    /**
     * Binds data to the created view for a suggestion list item.
     *
     * @param context            the context in which the view is bound
     * @param createdView        the view that has been created for the item
     * @param item               the data item to bind to the view
     * @param holder             the ViewHolder that holds the view
     * @param suggestionItemList the list of suggestion items
     * @param position           the position of the item in the list
     */
    public abstract void bindView(Context context, View createdView, CometchatSuggestionListItemsBinding item, RecyclerView.ViewHolder holder, List<SuggestionItem> suggestionItemList, int position);
}
