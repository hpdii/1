package com.donk.app.models;

public class UpdateVideoRequest {
    private String title;
    private String description;
    private Boolean isPrivate;

    public UpdateVideoRequest(String title, String description, Boolean isPrivate) {
        this.title = title;
        this.description = description;
        this.isPrivate = isPrivate;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }
}
