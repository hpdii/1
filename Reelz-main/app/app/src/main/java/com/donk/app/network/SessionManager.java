package com.donk.app.network;

import android.content.Context;
import android.content.SharedPreferences;

import com.donk.app.constants.AppConstants;
import com.donk.app.models.UserDto;

public class SessionManager {
    private static SessionManager instance;
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(
                AppConstants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public void saveAuth(String token, UserDto user) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(AppConstants.KEY_TOKEN, token);
        if (user != null) {
            if (user.getId() != null) {
                editor.putLong(AppConstants.KEY_USER_ID, user.getId());
            }
            editor.putString(AppConstants.KEY_USERNAME, user.getUsername());
            editor.putString(AppConstants.KEY_EMAIL, user.getEmail());
            editor.putString(AppConstants.KEY_DISPLAY_NAME, user.getDisplayName());
        }
        editor.apply();
    }

    public String getToken() {
        return prefs.getString(AppConstants.KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    public Long getUserId() {
        long id = prefs.getLong(AppConstants.KEY_USER_ID, -1L);
        return id == -1L ? null : id;
    }

    public String getUsername() {
        return prefs.getString(AppConstants.KEY_USERNAME, "");
    }

    public String getEmail() {
        return prefs.getString(AppConstants.KEY_EMAIL, "");
    }

    public String getDisplayName() {
        return prefs.getString(AppConstants.KEY_DISPLAY_NAME, "");
    }

    public void setDisplayName(String displayName) {
        prefs.edit().putString(AppConstants.KEY_DISPLAY_NAME, displayName).apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
