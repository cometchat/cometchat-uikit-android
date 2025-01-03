package com.cometchat.chatuikit.shared.models.interactiveelements;

public class BaseInputElement<T> extends ElementEntity {
    private static final String TAG = BaseInputElement.class.getSimpleName();
    private T response;
    private boolean optional;
    private T defaultValue;

    public BaseInputElement(String elementType, String elementId, T defaultValue) {
        super(elementType, elementId);
        this.optional = false;
        setDefaultValue(defaultValue);
    }

    public void setResponse(T response) {
        this.response = response;
    }

    public T getResponse() {
        return response;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        setResponse(defaultValue);
    }

    public T getDefaultValue() {
        return defaultValue;
    }
}
