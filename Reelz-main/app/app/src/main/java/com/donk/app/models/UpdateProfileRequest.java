package com.donk.app.models;

public class UpdateProfileRequest {
    private String displayName;
    private String bio;

    public UpdateProfileRequest(String displayName, String bio) {
        this.displayName = displayName;
        this.bio = bio;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBio() {
        return bio;
    }
}
