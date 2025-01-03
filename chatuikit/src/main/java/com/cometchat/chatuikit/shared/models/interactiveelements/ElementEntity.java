package com.cometchat.chatuikit.shared.models.interactiveelements;

public class ElementEntity {
    private static final String TAG = ElementEntity.class.getSimpleName();


    private final String elementType;
    private String elementId;

    public ElementEntity(String elementType, String elementId) {
        this.elementType = elementType;
        this.elementId = elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public String getElementType() {
        return elementType;
    }

    public String getElementId() {
        return elementId;
    }
}
