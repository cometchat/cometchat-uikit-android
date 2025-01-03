package com.cometchat.chatuikit.shared.models.interactiveelements;

import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.models.interactivemessage.InteractiveConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RadioButtonElement extends BaseInputElement<String> {
    private static final String TAG = RadioButtonElement.class.getSimpleName();
    private String label;
    private List<OptionElement> optionElements;

    private RadioButtonElement() {
        super(UIKitConstants.UIElementsType.UI_ELEMENT_RADIO_BUTTON, null, null);
    }

    public RadioButtonElement(String elementId, String label, List<OptionElement> optionElements, String defaultValue) {
        super(UIKitConstants.UIElementsType.UI_ELEMENT_RADIO_BUTTON, elementId, defaultValue);
        setLabel(label);
        setOptions(optionElements == null ? new ArrayList<>() : optionElements);
    }

    public String getLabel() {
        return label;
    }

    public List<OptionElement> getOptions() {
        return optionElements;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setOptions(List<OptionElement> optionElements) {
        this.optionElements = optionElements;
    }

    public static RadioButtonElement fromJson(JSONObject json) {
        RadioButtonElement radioButtonElement = new RadioButtonElement();
        if (json != null) {
            try {
                if (json.has(InteractiveConstants.ELEMENT_ID))
                    radioButtonElement.setElementId(json.getString(InteractiveConstants.ELEMENT_ID));
                if (json.has(InteractiveConstants.RadioButtonUIConstants.OPTIONAL))
                    radioButtonElement.setOptional(json.getBoolean(InteractiveConstants.RadioButtonUIConstants.OPTIONAL));
                if (json.has(InteractiveConstants.RadioButtonUIConstants.LABEL))
                    radioButtonElement.setLabel(json.getString(InteractiveConstants.RadioButtonUIConstants.LABEL));
                if (json.has(InteractiveConstants.RadioButtonUIConstants.OPTIONS)) {
                    List<OptionElement> optionElements = new ArrayList<>();
                    JSONArray options = json.getJSONArray(InteractiveConstants.RadioButtonUIConstants.OPTIONS);
                    for (int i = 0; i < options.length(); i++) {
                        JSONObject option = options.getJSONObject(i);
                        OptionElement optionElement = OptionElement.fromJson(option);
                        optionElements.add(optionElement);
                    }
                    radioButtonElement.setOptions(optionElements);
                }
                if (json.has(InteractiveConstants.DEFAULT_VALUE))
                    radioButtonElement.setDefaultValue(json.getString(InteractiveConstants.DEFAULT_VALUE));
            } catch (Exception e) {
                CometChatLogger.e(TAG, e.toString());
            }
        }
        return radioButtonElement;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(InteractiveConstants.ELEMENT_TYPE, getElementType());
            if (getElementId() != null && !getElementId().isEmpty()) {
                jsonObject.put(InteractiveConstants.ELEMENT_ID, getElementId());
            }

            jsonObject.put(InteractiveConstants.RadioButtonUIConstants.OPTIONAL, isOptional());

            if (getLabel() != null && !getLabel().isEmpty()) {
                jsonObject.put(InteractiveConstants.RadioButtonUIConstants.LABEL, getLabel());
            }

            if (getDefaultValue() != null && !getDefaultValue().isEmpty()) {
                jsonObject.put(InteractiveConstants.DEFAULT_VALUE, getDefaultValue());
            }

            if (getOptions() != null && !getOptions().isEmpty()) {
                JSONArray optionJSONArray = new JSONArray();
                for (OptionElement optionElement : getOptions()) {
                    JSONObject optionJSON = optionElement.toJson();
                    if (optionJSON != null) {
                        optionJSONArray.put(optionJSON);
                    }
                }
                jsonObject.put(InteractiveConstants.RadioButtonUIConstants.OPTIONS, optionJSONArray);
            }

        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }

        return jsonObject;
    }
}
