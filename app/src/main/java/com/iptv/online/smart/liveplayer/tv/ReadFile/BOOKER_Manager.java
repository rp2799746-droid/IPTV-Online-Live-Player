package com.iptv.online.smart.liveplayer.tv.ReadFile;

import android.content.Context;
import android.content.SharedPreferences;

public class BOOKER_Manager {

    public static String LANGUAGE_SELECTED = "language_selected";
    static SharedPreferences.Editor spEdit;
    static SharedPreferences sharedPreferences;

    public static void initializingSharedPreference(Context context) {
        SharedPreferences sharedPreferences2 = context.getSharedPreferences("MySharedPref123", 0);
        sharedPreferences = sharedPreferences2;
        spEdit = sharedPreferences2.edit();
    }


    public static void setLanguageSelected(boolean z) {
        sharedPreferences.edit().putBoolean(LANGUAGE_SELECTED, z).apply();
    }


}
