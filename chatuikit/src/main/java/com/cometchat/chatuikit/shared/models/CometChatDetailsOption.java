package com.cometchat.chatuikit.shared.models;

import android.content.Context;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.StyleRes;

import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.shared.interfaces.Function3;
import com.cometchat.chatuikit.shared.interfaces.OnDetailOptionClick;

/**
 * CometChatDetailsOption is a class representing an option for displaying
 * details option in Details component.
 *
 * <p>
 * It extends the CometChatOption class.
 *
 * <pre>
 * {
 * 	&#64;code
 * 	// CometChatDetailsOption class has total of 3 constructor
 * 	// 1st CometChatDetailsOption(String id, String title, @DrawableRes int
 * 	// startIcon, @DrawableRes int endIcon, OnDetailOptionClick onClick)
 * 	CometChatDetailsOption option = new CometChatDetailsOption("id1", "Title", R.drawable.icon, R.drawable.icon,
 * 			new OnDetailOptionClick() {
 * 				&#64;Override
 * 				public void onClick(User user, Group group, String templateId, CometChatDetailsOption option,
 * 						Context context) {
 *
 *                }
 *            });
 *
 * 	// 2nd CometChatDetailsOption(String id, String title, int startIcon, int
 * 	// endIcon, int titleColor, String titleFont, int titleAppearance, int
 * 	// startIconTint, int endIconTint, OnDetailOptionClick onClick)
 * 	CometChatDetailsOption option = new CometChatDetailsOption(UIKitConstants.UserOption.VIEW_PROFILE,
 * 			getString(R.string.title), R.drawable.start_icon, R.drawable.end_icon,
 * 			getResources().getColor(R.color.black), null, R.style.Name, 0, getResources().getColor(R.color.black),
 * 			new OnDetailOptionClick() {
 * 				&#64;Override
 * 				public void onClick(User user, Group group, String templateId, CometChatDetailsOption option,
 * 						Context context) {
 *
 *                }
 *            });
 * 	// 3rd CometChatDetailsOption(Function3< Context, User, Group, View> view,
 * 	// OnDetailOptionClick onClick)
 * 	CometChatDetailsOption option = new CometChatDetailsOption(new Function3<Context, User, Group, View>() {
 * 		&#64;Override
 * 		public View apply(Context context, User user, Group group) {
 * 			TextView textView = new TextView(context);
 * 			textView.setText("this call back is use to return any custom view that need to be shown in details");
 * 			return textView;
 *        }
 *    }, new OnDetailOptionClick() {
 * 		&#64;Override
 * 		public void onClick(User user, Group group, String templateId, CometChatDetailsOption option,
 * 				Context context) {
 *
 *        }
 *    });
 *
 * }
 * </pre>
 */
public class CometChatDetailsOption extends CometChatOption {
    private static final String TAG = CometChatDetailsOption.class.getSimpleName();


    private Function3<Context, User, Group, View> view;
    private final OnDetailOptionClick onClick;
    private @ColorInt int titleColor;
    private String titleFont;
    private @StyleRes int titleAppearance;
    private @ColorInt int startIconTint;
    private @ColorInt int endIconTint;
    private @DrawableRes int endIcon;

    /**
     * Constructor for CometChatDetailsOption with minimal parameters.
     *
     * @param id        The unique identifier of the option.
     * @param title     The title of the option.
     * @param startIcon The resource ID of the start icon.
     * @param endIcon   The resource ID of the end icon.
     * @param onClick   The callback function for handling option click events.
     */
    public CometChatDetailsOption(String id, String title, @DrawableRes int startIcon, @DrawableRes int endIcon, OnDetailOptionClick onClick) {
        super(id, title, startIcon);
        this.onClick = onClick;
        this.endIcon = endIcon;
    }

    /**
     * Constructor for CometChatDetailsOption with all parameters.
     *
     * @param id              The unique identifier of the option.
     * @param title           The title of the option.
     * @param startIcon       The resource ID of the start icon.
     * @param endIcon         The resource ID of the end icon.
     * @param titleColor      The color of the option title.
     * @param titleFont       The font of the option title.
     * @param titleAppearance The appearance style of the option title.
     * @param startIconTint   The tint color of the start icon.
     * @param endIconTint     The tint color of the end icon.
     * @param onClick         The callback function for handling option click events.
     */
    public CometChatDetailsOption(String id, String title, int startIcon, int endIcon, int titleColor, String titleFont, int titleAppearance, int startIconTint, int endIconTint, OnDetailOptionClick onClick) {
        super(id, title, startIcon);
        this.onClick = onClick;
        this.titleColor = titleColor;
        this.titleFont = titleFont;
        this.titleAppearance = titleAppearance;
        this.startIconTint = startIconTint;
        this.endIconTint = endIconTint;
        this.endIcon = endIcon;
    }

    /**
     * Constructor for CometChatDetailsOption with view parameter.
     *
     * @param view    The function that returns a custom view for the option.
     * @param onClick The callback function for handling option click events.
     * @see Function3
     * @see OnDetailOptionClick
     */
    public CometChatDetailsOption(Function3<Context, User, Group, View> view, OnDetailOptionClick onClick) {
        this.view = view;
        this.onClick = onClick;
    }

    /**
     * Returns the function that provides a custom view for the option.
     *
     * @return The function that provides a custom view.
     */
    public Function3<Context, User, Group, View> getView() {
        return view;
    }

    /**
     * Returns the custom view for the option based on the provided context, user,
     * and group.
     *
     * @param context The context in which the view will be inflated.
     * @param user    The User object associated with the view.
     * @param group   The Group object associated with the view.
     * @return The custom view for the option.
     */
    public View getView(Context context, User user, Group group) {
        View view2 = null;
        if (view != null) view2 = view.apply(context, user, group);
        return view2;
    }

    /**
     * Returns the callback function for handling option click events.
     *
     * @return The OnDetailOptionClick callback function.
     */
    public OnDetailOptionClick getOnClick() {
        return onClick;
    }

    /**
     * Returns the resource ID of the end icon.
     *
     * @return The resource ID of the end icon.
     */
    public int getEndIcon() {
        return endIcon;
    }

    /**
     * Returns the color of the option title.
     *
     * @return The color of the option title.
     */
    public int getTitleColor() {
        return titleColor;
    }

    /**
     * Returns the font of the option title.
     *
     * @return The font of the option title.
     */
    public String getTitleFont() {
        return titleFont;
    }

    /**
     * Returns the appearance style of the option title.
     *
     * @return The appearance style of the option title.
     */
    public int getTitleAppearance() {
        return titleAppearance;
    }

    /**
     * Returns the tint color of the start icon.
     *
     * @return The tint color of the start icon.
     */
    public int getStartIconTint() {
        return startIconTint;
    }

    /**
     * Returns the tint color of the end icon.
     *
     * @return The tint color of the end icon.
     */
    public int getEndIconTint() {
        return endIconTint;
    }
}
