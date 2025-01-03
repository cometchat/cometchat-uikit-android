package com.cometchat.chatuikit.shared.models.interactiveelements;

import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.models.interactivemessage.InteractiveConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SpinnerElement extends BaseInputElement<String> {
    private static final String TAG = SpinnerElement.class.getSimpleName();


    private String label;
    private List<OptionElement> optionElements;

    private SpinnerElement() {
        super(UIKitConstants.UIElementsType.UI_ELEMENT_SPINNER, null, null);
    }

    public SpinnerElement(String elementId, String label, List<OptionElement> optionElements, String defaultValue) {
        super(UIKitConstants.UIElementsType.UI_ELEMENT_SPINNER, elementId, defaultValue);
        this.label = label;
        this.optionElements = optionElements;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setOptions(List<OptionElement> optionElements) {
        this.optionElements = optionElements;
    }

    public String getLabel() {
        return label;
    }

    public List<OptionElement> getOptions() {
        return optionElements;
    }

    public static SpinnerElement fromJson(JSONObject json) {
        SpinnerElement spinnerElement = new SpinnerElement();
        if (json != null) {
            try {
                if (json.has(InteractiveConstants.ELEMENT_ID))
                    spinnerElement.setElementId(json.getString(InteractiveConstants.ELEMENT_ID));
                if (json.has(InteractiveConstants.DropDownUIConstants.LABEL))
                    spinnerElement.setLabel(json.getString(InteractiveConstants.DropDownUIConstants.LABEL));
                if (json.has(InteractiveConstants.DropDownUIConstants.OPTIONAL)) {
                    spinnerElement.setOptional(json.getBoolean(InteractiveConstants.DropDownUIConstants.OPTIONAL));
                }
                if (json.has(InteractiveConstants.DEFAULT_VALUE))
                    spinnerElement.setDefaultValue(json.getString(InteractiveConstants.DEFAULT_VALUE));
                if (json.has(InteractiveConstants.DEFAULT_VALUE))
                    spinnerElement.setDefaultValue(json.getString(InteractiveConstants.DEFAULT_VALUE));
                if (json.has(InteractiveConstants.DropDownUIConstants.OPTIONS)) {
                    JSONArray options = json.getJSONArray(InteractiveConstants.DropDownUIConstants.OPTIONS);
                    List<OptionElement> optionElements = new ArrayList<>();
                    for (int i = 0; i < options.length(); i++) {
                        optionElements.add(OptionElement.fromJson(options.getJSONObject(i)));
                    }
                    spinnerElement.setOptions(optionElements);
                }
            } catch (Exception e) {
                CometChatLogger.e(TAG, e.getMessage());
            }
        }
        return spinnerElement;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(InteractiveConstants.ELEMENT_TYPE, getElementType());
            if (getElementId() != null && !getElementId().isEmpty()) {
                jsonObject.put(InteractiveConstants.ELEMENT_ID, getElementId());
            }

            if (getLabel() != null && !getLabel().isEmpty()) {
                jsonObject.put(InteractiveConstants.DropDownUIConstants.LABEL, getLabel());
            }

            jsonObject.put(InteractiveConstants.DropDownUIConstants.OPTIONAL, isOptional());

            if (getDefaultValue() != null && !getDefaultValue().isEmpty()) {
                jsonObject.put(InteractiveConstants.DEFAULT_VALUE, getDefaultValue());
            }
            if (getOptions() != null && !getOptions().isEmpty()) {
                JSONArray options = new JSONArray();
                for (OptionElement option : getOptions()) {
                    JSONObject optionJson = option.toJson();
                    if (optionJson != null) {
                        options.put(optionJson);
                    }
                }
                jsonObject.put(InteractiveConstants.DropDownUIConstants.OPTIONS, options);
            }
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }
        return jsonObject;
    }
}
