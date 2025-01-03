package com.cometchat.chatuikit.shared.models.interactiveelements;

import androidx.annotation.Nullable;

import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.models.interactivemessage.InteractiveConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CheckboxElement extends BaseInputElement<List<String>> {
    private static final String TAG = CheckboxElement.class.getSimpleName();

    private String label;
    private List<OptionElement> optionElements;

    private CheckboxElement() {
        super(UIKitConstants.UIElementsType.UI_ELEMENT_CHECKBOX, null, null);
    }

    public CheckboxElement(String elementId, String label, @Nullable List<OptionElement> optionElements, List<String> defaultValues) {
        super(UIKitConstants.UIElementsType.UI_ELEMENT_CHECKBOX, elementId, defaultValues);
        setLabel(label);
        setOptions(optionElements == null ? new ArrayList<>() : optionElements);
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

    public static CheckboxElement fromJson(JSONObject jsonObject) {
        if (jsonObject == null) return null;
        CheckboxElement checkboxElement = new CheckboxElement();
        try {
            if (jsonObject.has(InteractiveConstants.ELEMENT_ID))
                checkboxElement.setElementId(jsonObject.getString(InteractiveConstants.ELEMENT_ID));
            if (jsonObject.has(InteractiveConstants.CheckBoxUIConstants.OPTIONAL))
                checkboxElement.setOptional(jsonObject.getBoolean(InteractiveConstants.CheckBoxUIConstants.OPTIONAL));
            if (jsonObject.has(InteractiveConstants.CheckBoxUIConstants.LABEL))
                checkboxElement.setLabel(jsonObject.getString(InteractiveConstants.CheckBoxUIConstants.LABEL));
            if (jsonObject.has(InteractiveConstants.CheckBoxUIConstants.OPTIONS)) {
                List<OptionElement> optionElements = new ArrayList<>();
                JSONArray options = jsonObject.getJSONArray(InteractiveConstants.CheckBoxUIConstants.OPTIONS);
                for (int i = 0; i < options.length(); i++) {
                    optionElements.add(OptionElement.fromJson(options.getJSONObject(i)));
                }
                checkboxElement.setOptions(optionElements);
            }
            if (jsonObject.has(InteractiveConstants.DEFAULT_VALUE)) {
                List<String> defaultValues = new ArrayList<>();
                JSONArray defaultValuesArray = jsonObject.getJSONArray(InteractiveConstants.DEFAULT_VALUE);
                for (int i = 0; i < defaultValuesArray.length(); i++) {
                    defaultValues.add(defaultValuesArray.getString(i));
                }
                checkboxElement.setDefaultValue(defaultValues);
            }
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }
        return checkboxElement;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(InteractiveConstants.ELEMENT_TYPE, getElementType());
            if (getElementId() != null && !getElementId().isEmpty()) {
                jsonObject.put(InteractiveConstants.ELEMENT_ID, getElementId());
            }

            jsonObject.put(InteractiveConstants.CheckBoxUIConstants.OPTIONAL, isOptional());

            if (getLabel() != null && !getLabel().isEmpty()) {
                jsonObject.put(InteractiveConstants.CheckBoxUIConstants.LABEL, getLabel());
            }

            List<OptionElement> options = getOptions();
            if (options != null && !options.isEmpty()) {
                JSONArray optionJSONArray = new JSONArray();
                for (OptionElement optionElement : options) {
                    JSONObject optionJSON = optionElement.toJson();
                    if (optionJSON != null) {
                        optionJSONArray.put(optionJSON);
                    }
                }
                jsonObject.put(InteractiveConstants.CheckBoxUIConstants.OPTIONS, optionJSONArray);
            }

            List<String> defaultValues = getDefaultValue();
            if (defaultValues != null && !defaultValues.isEmpty()) {
                JSONArray defaultValuesJSONArray = new JSONArray(defaultValues);
                jsonObject.put(InteractiveConstants.DEFAULT_VALUE, defaultValuesJSONArray);
            }

        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }
        return jsonObject;
    }
}
