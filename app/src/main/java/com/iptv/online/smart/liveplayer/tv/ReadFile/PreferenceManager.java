package com.iptv.online.smart.liveplayer.tv.ReadFile;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    Context context;
    int PRIVATE_MODE = 0;

    private static final String PREF_NAME = "intro_slider-welcome";

    public PreferenceManager(Context context) {
        this.context = context;
        sharedPreferences = this.context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = sharedPreferences.edit();
    }



}