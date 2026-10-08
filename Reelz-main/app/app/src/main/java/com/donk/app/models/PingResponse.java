package com.donk.app.models;

public class PingResponse {
    private boolean pong;
    private String service;
    private String time;

    public boolean isPong() {
        return pong;
    }

    public String getService() {
        return service;
    }

    public String getTime() {
        return time;
    }
}
