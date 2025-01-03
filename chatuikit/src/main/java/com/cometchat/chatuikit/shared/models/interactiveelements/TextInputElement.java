package com.cometchat.chatuikit.shared.models.interactiveelements;

import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.models.interactivemessage.InteractiveConstants;

import org.json.JSONObject;

public class TextInputElement extends BaseInputElement<String> {
    private static final String TAG = TextInputElement.class.getSimpleName();
    private String label;
    private int maxLines;
    private PlaceHolder placeHolder;

    private TextInputElement() {
        super(UIKitConstants.UIElementsType.UI_ELEMENT_TEXT_INPUT, null, null);
    }

    public TextInputElement(String elementId, String label) {
        super(UIKitConstants.UIElementsType.UI_ELEMENT_TEXT_INPUT, elementId, null);
        this.label = label;
        this.maxLines = 1;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    public void setPlaceHolder(PlaceHolder placeHolder) {
        this.placeHolder = placeHolder;
    }

    public String getLabel() {
        return label;
    }

    public int getMaxLines() {
        return maxLines;
    }

    public PlaceHolder getPlaceHolder() {
        return placeHolder;
    }

    public static TextInputElement fromJson(JSONObject jsonObject) {
        if (jsonObject == null) return null;
        TextInputElement textInputElement = new TextInputElement();
        if (jsonObject.has(InteractiveConstants.ELEMENT_ID)) {
            textInputElement.setElementId(jsonObject.optString(InteractiveConstants.ELEMENT_ID));
        }
        if (jsonObject.has(InteractiveConstants.TextInputUIConstants.LABEL)) {
            textInputElement.setLabel(jsonObject.optString(InteractiveConstants.TextInputUIConstants.LABEL));
        }
        if (jsonObject.has(InteractiveConstants.TextInputUIConstants.OPTIONAL)) {
            textInputElement.setOptional(jsonObject.optBoolean(InteractiveConstants.TextInputUIConstants.OPTIONAL));
        }
        if (jsonObject.has(InteractiveConstants.TextInputUIConstants.MAX_LINES)) {
            textInputElement.setMaxLines(jsonObject.optInt(InteractiveConstants.TextInputUIConstants.MAX_LINES));
        }
        if (jsonObject.has(InteractiveConstants.DEFAULT_VALUE)) {
            textInputElement.setDefaultValue(jsonObject.optString(InteractiveConstants.DEFAULT_VALUE));
        }
        if (jsonObject.has(InteractiveConstants.TextInputUIConstants.PLACEHOLDER)) {
            JSONObject placeHolderObject = jsonObject.optJSONObject(InteractiveConstants.TextInputUIConstants.PLACEHOLDER);
            if (placeHolderObject != null) {
                textInputElement.setPlaceHolder(PlaceHolder.fromJson(placeHolderObject));
            }
        }
        return textInputElement;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(InteractiveConstants.ELEMENT_TYPE, getElementType());
            if (getElementId() != null && !getElementId().isEmpty()) {
                jsonObject.put(InteractiveConstants.ELEMENT_ID, getElementId());
            }
            if (getDefaultValue() != null) {
                jsonObject.put(InteractiveConstants.DEFAULT_VALUE, getDefaultValue());
            }
            if (getLabel() != null && !getLabel().isEmpty()) {
                jsonObject.put(InteractiveConstants.TextInputUIConstants.LABEL, getLabel());
            }
            jsonObject.put(InteractiveConstants.TextInputUIConstants.OPTIONAL, isOptional());

            jsonObject.put(InteractiveConstants.TextInputUIConstants.MAX_LINES, getMaxLines());

            PlaceHolder placeholder = getPlaceHolder();
            if (placeholder != null) {
                jsonObject.put(InteractiveConstants.TextInputUIConstants.PLACEHOLDER, placeholder.toJson());
            }

        } catch (Exception ignored) {

        }
        return jsonObject;
    }
}
