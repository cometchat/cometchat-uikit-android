package com.cometchat.chatuikit.extensions.polls.bubble.chipviews;

public class ImageTextPoJo {
    private static final String TAG = ImageTextPoJo.class.getSimpleName();
    private String text;
    private String imageUrl;

    public ImageTextPoJo(String text, String imageUrl) {
        this.text = text;
        this.imageUrl = imageUrl;
    }

    public ImageTextPoJo() {
    }

    public String getText() {
        return text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
