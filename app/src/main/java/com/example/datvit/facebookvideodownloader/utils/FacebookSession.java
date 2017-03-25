package com.example.datvit.facebookvideodownloader.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by DatVIT on 11/15/2016.
 */

public class FacebookSession {
    private static final String API_EMAIL = "email";
    private static final String API_ID = "id";
    private static final String API_TOKEN = "token";
    private static final String API_USERNAME = "username";
    private static final String API_USER_PICTURE = "user_picture";
    private static final String API_USER_COVER = "user_cover";
    private static final String SHARED = "facebook_Preferences";
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPref;

    public FacebookSession(Context context) {
        this.sharedPref = context.getSharedPreferences(SHARED, 0);
        this.editor = this.sharedPref.edit();
    }

    public void storeAccessToken(String username, String email, String userpicpath, String token, String id, String user_cover) {
        this.editor.putString(API_TOKEN, token);
        this.editor.putString(API_EMAIL, email);
        this.editor.putString(API_USERNAME, username);
        this.editor.putString(API_USER_PICTURE, userpicpath);
        this.editor.putString(API_ID, id);
        this.editor.putString(API_USER_COVER, user_cover);
        this.editor.commit();
    }

    public void resetAccessToken() {
        this.editor.putString(API_TOKEN, null);
        this.editor.putString(API_EMAIL, null);
        this.editor.putString(API_USERNAME, null);
        this.editor.putString(API_USER_PICTURE, null);
        this.editor.putString(API_ID, null);
        this.editor.putString(API_USER_COVER, null);
        this.editor.commit();
    }

    public String getUsername() {
        return this.sharedPref.getString(API_USERNAME, null);
    }

    public String getToken() {
        return this.sharedPref.getString(API_TOKEN, null);
    }

    public String getEmail() {
        return this.sharedPref.getString(API_EMAIL, null);
    }

    public String getUserPicture() {
        return this.sharedPref.getString(API_USER_PICTURE, null);
    }

    public String getUserId() {
        return this.sharedPref.getString(API_ID, null);
    }

    public String getUserCover() {
        return this.sharedPref.getString(API_USER_COVER, null);
    }
}