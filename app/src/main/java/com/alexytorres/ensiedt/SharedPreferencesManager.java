package com.alexytorres.ensiedt;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {
    // Singleton and shared preferences object
    private static SharedPreferencesManager instance;

    private static final String globalKey = "com.alexytorres.ensiedt";
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    @SuppressLint("CommitPrefEdits")
    private SharedPreferencesManager(Context context) {
        prefs = context.getSharedPreferences(globalKey, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }
    public static SharedPreferencesManager getInstance(Context context) {
        if(instance == null)
            instance = new SharedPreferencesManager(context);
        return instance;
    }

    public void setAutoconnect(boolean autoconnect) {
        editor.putBoolean(globalKey + ".autoconnect", autoconnect);
    }
    public boolean getAutoconnect() {
        return prefs.getBoolean(globalKey + ".autoconnect", false);
    }

    public void setLogin(String login) {
        editor.putString(globalKey + ".login", login);
    }
    public String getLogin() {
        return prefs.getString(globalKey + ".login", "");
    }

    public void setPassword(String password) {
        editor.putString(globalKey + ".password", password);
    }
    public String getPassword() {
        return prefs.getString(globalKey + ".password", "");
    }

    // Commit editing
    public void commit() {
        editor.commit();
    }

    public void reset() {
        setLogin("");
        setPassword("");
        commit();
    }
}
