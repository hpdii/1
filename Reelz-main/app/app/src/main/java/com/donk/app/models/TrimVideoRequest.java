package com.donk.app.models;

public class TrimVideoRequest {
    private Double startSeconds;
    private Double endSeconds;

    public TrimVideoRequest(Double startSeconds, Double endSeconds) {
        this.startSeconds = startSeconds;
        this.endSeconds = endSeconds;
    }

    public Double getStartSeconds() {
        return startSeconds;
    }

    public Double getEndSeconds() {
        return endSeconds;
    }
}
