package com.kiwmodz.exe;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SessionManager {
    private static final String PREF_NAME = "KeyAuthSession";
    private static final String KEY_SESSION_DATA = "session_data";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_USERNAME = "saved_username";
    private static final String KEY_PASSWORD = "saved_password";

    private SharedPreferences prefs;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Simpan session login - PERBAIKAN: Gunakan KeyAuth.SessionData
    public void saveSession(KeyAuth.SessionData sessionData) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(sessionData);
            oos.close();
            String encoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            prefs.edit().putString(KEY_SESSION_DATA, encoded).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Ambil session yang tersimpan - PERBAIKAN: Gunakan KeyAuth.SessionData
    public KeyAuth.SessionData getSession() {
        String encoded = prefs.getString(KEY_SESSION_DATA, null);
        if (encoded != null) {
            try {
                byte[] data = Base64.decode(encoded, Base64.DEFAULT);
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
                KeyAuth.SessionData sessionData = (KeyAuth.SessionData) ois.readObject();
                ois.close();
                return sessionData;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // Hapus session
    public void clearSession() {
        prefs.edit().remove(KEY_SESSION_DATA).apply();
    }

    // Simpan remember me credentials
    public void saveRememberMe(String username, String password, boolean remember) {
        SharedPreferences.Editor editor = prefs.edit();
        if (remember) {
            String encodedUser = Base64.encodeToString(username.getBytes(), Base64.DEFAULT);
            String encodedPass = Base64.encodeToString(password.getBytes(), Base64.DEFAULT);
            editor.putString(KEY_USERNAME, encodedUser);
            editor.putString(KEY_PASSWORD, encodedPass);
            editor.putBoolean(KEY_REMEMBER_ME, true);
        } else {
            editor.remove(KEY_USERNAME);
            editor.remove(KEY_PASSWORD);
            editor.putBoolean(KEY_REMEMBER_ME, false);
        }
        editor.apply();
    }

    // Ambil remember me credentials
    public String[] getRememberMe() {
        if (prefs.getBoolean(KEY_REMEMBER_ME, false)) {
            String encodedUser = prefs.getString(KEY_USERNAME, null);
            String encodedPass = prefs.getString(KEY_PASSWORD, null);
            if (encodedUser != null && encodedPass != null) {
                try {
                    String username = new String(Base64.decode(encodedUser, Base64.DEFAULT));
                    String password = new String(Base64.decode(encodedPass, Base64.DEFAULT));
                    return new String[]{username, password};
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public boolean isRememberMeChecked() {
        return prefs.getBoolean(KEY_REMEMBER_ME, false);
    }
}