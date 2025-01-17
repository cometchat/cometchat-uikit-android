package com.cometchat.chatuikit.shared.views.cardbubble;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

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
import com.cometchat.chatuikit.shared.models.interactiveelements.BaseInteractiveElement;
import com.cometchat.chatuikit.shared.models.interactiveelements.ButtonElement;
import com.cometchat.chatuikit.shared.models.interactiveelements.ElementEntity;
import com.cometchat.chatuikit.shared.models.interactivemessage.CardMessage;
import com.cometchat.chatuikit.shared.models.interactivemessage.InteractiveConstants;
import com.cometchat.chatuikit.shared.resources.apicontroller.ApiController;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.views.imagebubble.CometChatImageBubble;
import com.cometchat.chatuikit.shared.views.quickview.CometChatQuickView;
import com.cometchat.chatuikit.shared.views.quickview.QuickViewStyle;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class CometChatCardBubble extends MaterialCardView {
    private static final String TAG = CometChatCardBubble.class.getSimpleName();

    private LinearLayout cardLayout, quickViewContainer;
    private CometChatQuickView quickView;
    private Context context;
    private TextView text;
    private CometChatImageBubble cardImageView;
    private List<BaseInteractiveElement> baseInteractiveElements;
    private CardBubbleStyle cardBubbleStyle;
    private CardMessage cardMessage;
    private HashMap<String, View> validateViews;
    private TextView goalCompletionTextView;
    private MaterialCardView cardViewLayout, cardBodyContainer;
    private QuickViewStyle quickViewStyle;

    public CometChatCardBubble(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        validateViews = new HashMap<>();
        View view = View.inflate(context, R.layout.cometchat_card_bubble, null);
        cardLayout = view.findViewById(R.id.card_base);
        cardViewLayout = view.findViewById(R.id.form_card);
        cardBodyContainer = view.findViewById(R.id.card_body_container);
        cardBodyContainer.setRadius(50);
        cardImageView = view.findViewById(R.id.card_image);
        quickViewContainer = view.findViewById(R.id.quick_view_container);
        quickView = view.findViewById(R.id.quick_view);
        text = view.findViewById(R.id.card_title);
        goalCompletionTextView = view.findViewById(R.id.quick_view_message);
        initCard(cardViewLayout);
        initCard(this);
        addView(view);
    }

    private void initCard(MaterialCardView cardView) {
        cardView.setCardBackgroundColor(Color.TRANSPARENT);
        cardView.setCardElevation(0);
        cardView.setRadius(0);
        setStrokeWidth(0);
    }

    public CometChatCardBubble(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CometChatCardBubble(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void setImageUrl(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            cardImageView.setVisibility(VISIBLE);
            cardImageView.setImageUrl(imageUrl, false);
        } else {
            cardImageView.setVisibility(GONE);
        }
    }

    public void showQuickView() {
        quickViewContainer.setVisibility(VISIBLE);
        cardViewLayout.setVisibility(GONE);
    }

    public void showCard() {
        quickViewContainer.setVisibility(GONE);
        cardViewLayout.setVisibility(VISIBLE);
    }

    public void setText(String text) {
        if (text != null && !text.isEmpty()) {
            this.text.setVisibility(VISIBLE);
            this.text.setText(text);
            if (cardBubbleStyle != null) {
                if (cardBubbleStyle.getTextColor() != 0)
                    this.text.setTextColor(cardBubbleStyle.getTextColor());
                if (cardBubbleStyle.getTextAppearance() != 0)
                    this.text.setTextAppearance(cardBubbleStyle.getTextAppearance());
            }
        } else {
            this.text.setVisibility(GONE);
        }
    }

    private void buildCard() {
        validateViews.clear();
        cardLayout.removeAllViews();
        for (ElementEntity elementEntity : baseInteractiveElements) {
            if (elementEntity != null) {
                if (elementEntity.getElementType().equals(UIKitConstants.UIElementsType.UI_ELEMENT_BUTTON)) {
                    buildButton((ButtonElement) elementEntity, false);
                } else {
                    Log.i(TAG, "buildForm: UI Type not supported");
                }
            }
        }
    }

    private void buildButton(ButtonElement buttonElement, boolean isSubmitButton) {

        View buttonView = View.inflate(context, R.layout.cometchat_form_button_material, null);
        MaterialCardView button = buttonView.findViewById(R.id.form_button);
        TextView separator = buttonView.findViewById(R.id.button_separator);
        MaterialCardView buttonCard = buttonView.findViewById(R.id.form_button_card);
        TextView buttonTitle = buttonView.findViewById(R.id.button_text);
        ProgressBar progressBar = buttonView.findViewById(R.id.button_progress);
        separator.setVisibility(VISIBLE);
        if (cardBubbleStyle != null) {
            if (cardBubbleStyle.getProgressBarTintColor() != 0) {
                progressBar
                    .getIndeterminateDrawable()
                    .setColorFilter(cardBubbleStyle.getProgressBarTintColor(), android.graphics.PorterDuff.Mode.MULTIPLY);
            }
            if (cardBubbleStyle.getButtonSeparatorColor() != 0) {
                separator.setBackgroundColor(cardBubbleStyle.getButtonSeparatorColor());
            }
            if (cardBubbleStyle.getButtonBackgroundColor() != 0) {
                buttonCard.setBackgroundColor(cardBubbleStyle.getButtonBackgroundColor());
            }
            if (cardBubbleStyle.getButtonTextColor() != 0) {
                buttonTitle.setTextColor(cardBubbleStyle.getButtonTextColor());
            }
            if (cardBubbleStyle.getButtonTextAppearance() != 0) {
                buttonTitle.setTextAppearance(cardBubbleStyle.getButtonTextAppearance());
            }
        }
        buttonTitle.setText(buttonElement.getText());
        button.setTag(buttonElement.getElementId());
        button.setOnClickListener(view -> {
            performAction(buttonElement, false);
        });
        validateViews.put(buttonElement.getElementId(), button);
        validateViews.put(buttonElement.getElementId() + "_text", buttonTitle);
        validateViews.put(buttonElement.getElementId() + "_progress", progressBar);
        disableButton(buttonElement, button);
        addViewToFormLayout(buttonView, 0);
    }

    private void disableButton(ButtonElement buttonElement, MaterialCardView button) {
        if (!Utils.isGoalCompleted(cardMessage)) {
            if (buttonElement.isDisableAfterInteracted()) {
                if (cardMessage.getInteractions() != null && !cardMessage.getInteractions().isEmpty()) {
                    for (int i = 0; i < cardMessage.getInteractions().size(); i++) {
                        Interaction interaction = cardMessage.getInteractions().get(i);
                        if (interaction.getElementId().equals(buttonElement.getElementId())) {
                            disableButton(button);
                            changeButtonTextToDisable((TextView) validateViews.get(buttonElement.getElementId() + "_text"));
                            break;
                        }
                    }
                }
            }

            if (!cardMessage.isAllowSenderInteraction() && cardMessage.getSender() != null && cardMessage
                .getSender()
                .getUid()
                .equals(CometChatUIKit.getLoggedInUser().getUid())) {
                disableButton(button);
                changeButtonTextToDisable((TextView) validateViews.get(buttonElement.getElementId() + "_text"));
            }
        } else {
            showQuickView();
        }
    }

    private void disableButton(MaterialCardView button) {
        button.setEnabled(false);
    }

    private void changeButtonTextToDisable(TextView text) {
        if (cardBubbleStyle != null && cardBubbleStyle.getButtonDisableTextColor() != 0) {
            text.setTextColor(cardBubbleStyle.getButtonDisableTextColor());
        }
    }

    private void performAction(ButtonElement buttonElement, boolean isSubmit) {
        if (buttonElement.getAction() != null && !buttonElement.getAction().getActionType().isEmpty()) {
            switch (buttonElement.getAction().getActionType()) {
                case InteractiveConstants.ACTION_TYPE_API_ACTION:
                    TextView textView = (TextView) validateViews.get(buttonElement.getElementId() + "_text");
                    ProgressBar progressBar = (ProgressBar) validateViews.get(buttonElement.getElementId() + "_progress");
                    MaterialCardView button = (MaterialCardView) validateViews.get(buttonElement.getElementId());
                    if (button != null) {
                        button.setEnabled(false);
                    }
                    if (progressBar != null) {
                        progressBar.setVisibility(VISIBLE);
                    }
                    if (textView != null) {
                        textView.setVisibility(GONE);
                    }
                    APIAction apiAction = (APIAction) buttonElement.getAction();
                    JSONObject jsonObject = Utils.getInteractiveRequestPayload(apiAction.getPayload(), buttonElement.getElementId(), cardMessage);

                    ApiController
                        .getInstance()
                        .call(apiAction.getMethod(), apiAction.getUrl(), jsonObject, apiAction.getHeaders(), new ApiController.APICallback() {
                            @Override
                            public void onSuccess(String response) {
                                if (button != null) {
                                    button.setEnabled(true);
                                }
                                if (progressBar != null) {
                                    progressBar.setVisibility(GONE);
                                }
                                if (textView != null) {
                                    textView.setVisibility(VISIBLE);
                                }
                                markInteract(buttonElement, button);
                            }

                            @Override
                            public void onError(Exception e) {
                                if (button != null) {
                                    button.setEnabled(true);
                                }
                                if (progressBar != null) {
                                    progressBar.setVisibility(GONE);
                                }
                                if (textView != null) {
                                    textView.setVisibility(VISIBLE);
                                }
                                CometChatLogger.e(TAG, e.toString());
                            }
                        });

                    break;
                case InteractiveConstants.ACTION_TYPE_URL_NAVIGATION:
                    URLNavigationAction urlNavigationAction = (URLNavigationAction) buttonElement.getAction();
                    if (urlNavigationAction.getUrl() != null && !urlNavigationAction.getUrl().isEmpty()) {
                        Intent intent = new Intent(context, CometChatWebViewActivity.class);
                        intent.putExtra(UIKitConstants.IntentStrings.URL, urlNavigationAction.getUrl());
                        context.startActivity(intent);
                        markInteract(buttonElement, (MaterialCardView) validateViews.get(buttonElement.getElementId()));
                    }
                    break;
                case InteractiveConstants.ACTION_TYPE_DEEP_LINKING:
                    DeepLinkingAction deepLinking = (DeepLinkingAction) buttonElement.getAction();
                    if (deepLinking.getUrl() != null && !deepLinking.getUrl().isEmpty()) {
                        String url = deepLinking.getUrl();
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        if (i.resolveActivity(context.getPackageManager()) != null) {
                            context.startActivity(i);
                            markInteract(buttonElement, (MaterialCardView) validateViews.get(buttonElement.getElementId()));
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void markInteract(ButtonElement buttonElement, MaterialCardView buttonView) {
        CometChat.markAsInteracted(cardMessage.getId(), buttonElement.getElementId(), new CometChat.CallbackListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                cardMessage.getInteractions().add(new Interaction(buttonElement.getElementId(), System.currentTimeMillis() / 100));
                disableButton(buttonElement, buttonView);
            }

            @Override
            public void onError(CometChatException e) {
                CometChatLogger.e(TAG, e.toString());
            }
        });
    }

    private void addViewToFormLayout(@NonNull View view, int topMargin) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                               ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = Utils.convertDpToPx(context, topMargin);
        view.setLayoutParams(layoutParams);
        cardLayout.addView(view);
    }

    public CardBubbleStyle getStyle() {
        return cardBubbleStyle;
    }

    public void setStyle(CardBubbleStyle style) {
        if (style != null) {
            this.cardBubbleStyle = style;
            // cardImageView.setStyle(style.getImageBubbleStyle());
            if (style.getQuickViewStyle() != null) {
                this.quickViewStyle = style.getQuickViewStyle();
            }
            if (style.getContentRadius() >= 0) {
                cardBodyContainer.setRadius(style.getContentRadius());
            }
            if (style.getContentBackgroundColor() != 0) {
                cardBodyContainer.setCardBackgroundColor(style.getContentBackgroundColor());
            }
            if (style.getDrawableBackground() != null)
                cardViewLayout.setBackground(style.getDrawableBackground());
            else if (style.getBackground() != 0)
                cardViewLayout.setCardBackgroundColor(style.getBackground());
            if (style.getStrokeWidth() >= 0) cardViewLayout.setStrokeWidth(style.getStrokeWidth());
            if (style.getCornerRadius() >= 0) cardViewLayout.setRadius(style.getCornerRadius());
            if (style.getStrokeColor() != 0) cardViewLayout.setStrokeColor(style.getStrokeColor());
        }
    }

    public LinearLayout getCardLayout() {
        return cardLayout;
    }

    public List<BaseInteractiveElement> getUiElements() {
        return baseInteractiveElements;
    }

    private void setUiElements(List<BaseInteractiveElement> elementEntities) {
        this.baseInteractiveElements = elementEntities;
        if (elementEntities != null && !elementEntities.isEmpty()) {
            buildCard();
        }
    }

    public CardMessage getCardMessage() {
        return cardMessage;
    }

    public void setCardMessage(CardMessage cardMessage) {
        if (cardMessage != null) {
            this.cardMessage = cardMessage;
            quickView.setTitle(CometChatUIKit.getLoggedInUser().getName());
            quickView.setSubTitle(cardMessage.getText());
            quickView.setCloseIconVisibility(View.GONE);
            quickView.setStyle(quickViewStyle);
            goalCompletionTextView.setText(cardMessage.getGoalCompletionText() == null ? getResources().getString(R.string.cometchat_form_completion_message) : cardMessage.getGoalCompletionText());
            if (quickViewStyle != null) {
                if (quickViewStyle.getSubtitleColor() != 0) {
                    goalCompletionTextView.setTextColor(quickViewStyle.getSubtitleColor());
                }
                if (quickViewStyle.getSubtitleAppearance() != 0) {
                    goalCompletionTextView.setTextAppearance(quickViewStyle.getSubtitleAppearance());
                }
            }
            if (!Utils.isGoalCompleted(cardMessage)) {
                showCard();
                setText(cardMessage.getText());
                setImageUrl(cardMessage.getImageUrl());
                setUiElements(cardMessage.getCardActions());
            } else {
                showQuickView();
            }
        }
    }
}
