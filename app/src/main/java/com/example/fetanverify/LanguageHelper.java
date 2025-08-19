package com.example.fetanverify;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;

public class LanguageHelper {
    private static final String PREFS_NAME = "language_prefs";
    private static final String LANGUAGE_KEY = "selected_language";
    
    public static final String ENGLISH = "en";
    public static final String AMHARIC = "am";
    public static final String OROMO = "om";
    
    public static void setLanguage(Context context, String languageCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(LANGUAGE_KEY, languageCode).apply();
        
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
    
    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LANGUAGE_KEY, ENGLISH);
    }
    
    public static void applyLanguage(Context context) {
        String languageCode = getLanguage(context);
        setLanguage(context, languageCode);
    }
}