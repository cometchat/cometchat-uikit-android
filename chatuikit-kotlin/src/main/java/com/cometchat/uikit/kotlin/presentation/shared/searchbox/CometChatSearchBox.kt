package com.cometchat.uikit.kotlin.presentation.shared.searchbox

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.databinding.CometchatSearchBoxBinding
import com.cometchat.uikit.kotlin.shared.resources.utils.Utils
import com.google.android.material.card.MaterialCardView

/**
 * CometChatSearchBox is a custom search input component with search icon,
 * clear button, and customizable styling.
 */
public class CometChatSearchBox @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    public companion object {
        private val TAG = CometChatSearchBox::class.java.simpleName
    }

    public val binding: CometchatSearchBoxBinding

    @StyleRes private var searchInputTextAppearance: Int = 0
    @ColorInt private var searchInputTextColor: Int = 0
    @StyleRes private var searchInputPlaceHolderTextAppearance: Int = 0
    @ColorInt private var searchInputPlaceHolderTextColor: Int = 0
    private var searchInputStartIcon: Drawable? = null
    private var searchInputEndIcon: Drawable? = null
    @ColorInt private var searchInputStartIconTint: Int = 0
    @ColorInt private var searchInputEndIconTint: Int = 0

    private var eventListener: OnSearch? = null
    private var onSearchClick: (() -> Unit)? = null

    init {
        binding = CometchatSearchBoxBinding.inflate(LayoutInflater.from(context), this, true)
        Utils.initMaterialCard(this)
        setupListeners()
    }

    private fun setupListeners() {
        binding.etSearch.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (textView.text.toString().isNotEmpty()) {
                    eventListener?.onSearch(SearchState.FILTER, textView.text.toString())
                }
                true
            } else {
                false
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                eventListener?.let { listener ->
                    if (s.toString().isEmpty()) {
                        setSearchInputEndIconVisibility(View.GONE)
                    } else {
                        setSearchInputEndIconVisibility(View.VISIBLE)
                    }
                    listener.onSearch(SearchState.TEXT_CHANGE, s.toString())
                }
            }
        })

        binding.ivClear.setOnClickListener {
            binding.etSearch.setText("")
            eventListener?.onSearch(SearchState.CLEAR, "")
        }

        binding.searchBoxParent.setOnClickListener {
            onSearchClick?.invoke()
        }

        binding.etSearch.setOnClickListener {
            onSearchClick?.invoke()
        }
    }

    public fun setOnSearchClick(onClick: (() -> Unit)?) {
        this.onSearchClick = onClick
    }

    public fun setSearchInputEndIconVisibility(visibility: Int) {
        binding.ivClear.visibility = visibility
    }

    public fun setSearchInputText(text: String) {
        binding.etSearch.setText(text)
    }

    public fun getSearchInputTextAppearance(): Int = searchInputTextAppearance

    public fun setSearchInputTextAppearance(@StyleRes appearance: Int) {
        this.searchInputTextAppearance = appearance
        binding.etSearch.setTextAppearance(appearance)
    }

    public fun getSearchInputTextColor(): Int = searchInputTextColor

    public fun setSearchInputTextColor(@ColorInt color: Int) {
        this.searchInputTextColor = color
        binding.etSearch.setTextColor(color)
    }

    public fun getSearchInputPlaceHolderTextAppearance(): Int = searchInputPlaceHolderTextAppearance

    public fun setSearchInputPlaceHolderTextAppearance(@StyleRes appearance: Int) {
        this.searchInputPlaceHolderTextAppearance = appearance
        if (appearance != 0) {
            // Note: TextAppearance for hint is typically set via the same setTextAppearance
            // as it affects the overall text styling. The hint color is set separately.
            binding.etSearch.setTextAppearance(appearance)
        }
    }

    public fun getSearchInputPlaceHolderTextColor(): Int = searchInputPlaceHolderTextColor

    public fun setSearchInputPlaceHolderTextColor(@ColorInt color: Int) {
        this.searchInputPlaceHolderTextColor = color
        binding.etSearch.setHintTextColor(color)
    }

    public fun getSearchInputStartIcon(): Drawable? = searchInputStartIcon

    public fun setSearchInputStartIcon(icon: Drawable?) {
        this.searchInputStartIcon = icon
        binding.ivSearch.background = icon
    }

    public fun getSearchInputStartIconTint(): Int = searchInputStartIconTint

    public fun setSearchInputStartIconTint(@ColorInt tint: Int) {
        this.searchInputStartIconTint = tint
        binding.ivSearch.backgroundTintList = ColorStateList.valueOf(tint)
    }

    public fun getSearchInputEndIcon(): Drawable? = searchInputEndIcon

    public fun setSearchInputEndIcon(icon: Drawable?) {
        this.searchInputEndIcon = icon
        binding.ivClear.background = icon
    }

    public fun getSearchInputEndIconTint(): Int = searchInputEndIconTint

    public fun setSearchInputEndIconTint(@ColorInt tint: Int) {
        this.searchInputEndIconTint = tint
        binding.ivClear.backgroundTintList = ColorStateList.valueOf(tint)
    }

    /**
     * Sets the placeholder text for the search input field.
     */
    public fun setSearchPlaceholderText(placeholder: String?) {
        placeholder?.let { binding.etSearch.hint = it }
    }

    /**
     * Sets an event listener for search events.
     */
    public fun addOnSearchListener(listener: OnSearch?) {
        listener?.let { this.eventListener = it }
    }

    /**
     * Search state constants.
     */
    public object SearchState {
        public const val FILTER: String = "filter"
        public const val CLEAR: String = "clear"
        public const val TEXT_CHANGE: String = "textchange"
    }

    /**
     * Interface for search event callbacks.
     */
    public fun interface OnSearch {
        public fun onSearch(state: String, text: String)
    }
}
