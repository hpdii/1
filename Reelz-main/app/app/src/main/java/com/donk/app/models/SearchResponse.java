package com.donk.app.models;

import java.util.Collections;
import java.util.List;

public class SearchResponse {
    private List<UserProfileDto> users;
    private List<VideoDto> videos;

    public List<UserProfileDto> getUsers() {
        return users != null ? users : Collections.emptyList();
    }

    public void setUsers(List<UserProfileDto> users) {
        this.users = users;
    }

    public List<VideoDto> getVideos() {
        return videos != null ? videos : Collections.emptyList();
    }

    public void setVideos(List<VideoDto> videos) {
        this.videos = videos;
    }
}
