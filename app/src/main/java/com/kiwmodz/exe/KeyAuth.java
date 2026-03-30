package com.kiwmodz.exe;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class KeyAuth {
    private static final String TAG = "KeyAuth";

    private String name;
    private String ownerid;
    private String secret;
    private String version;

    private boolean isSuccess = false;
    private String errorMessage = "Unknown Error";
    private JSONObject userData = null;
    private String sessionid = "";

    private final String apiUrl = "https://keyauth.win/api/1.2/";

    // Constructor
    public KeyAuth(String name, String ownerid, String secret, String version) {
        this.name = name;
        this.ownerid = ownerid;
        this.secret = secret;
        this.version = version;
    }

    // Getter methods
    public boolean isSuccess() {
        return isSuccess;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public JSONObject getUserData() {
        return userData;
    }

    public String getSessionId() {
        return sessionid;
    }

    // FUNGSI 1: Meminta Session ID ke Server (WAJIB sebelum login)
    public void init() {
        try {
            String formData = "type=init&" +
                    "ver=" + URLEncoder.encode(version, "UTF-8") + "&" +
                    "name=" + URLEncoder.encode(name, "UTF-8") + "&" +
                    "ownerid=" + URLEncoder.encode(ownerid, "UTF-8");

            Log.d(TAG, "Init request: " + formData);

            String response = makeRequest(formData);

            if (response != null && !response.isEmpty()) {
                Log.d(TAG, "Init response: " + response);

                JSONObject json = new JSONObject(response);
                if (json.getBoolean("success")) {
                    sessionid = json.getString("sessionid");
                    isSuccess = true;
                    errorMessage = "";
                    Log.i(TAG, "✅ Init successful, sessionid: " + sessionid);
                } else {
                    isSuccess = false;
                    errorMessage = json.getString("message");
                    Log.e(TAG, "❌ Init failed: " + errorMessage);
                }
            } else {
                isSuccess = false;
                errorMessage = "Empty response from server";
                Log.e(TAG, errorMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
            isSuccess = false;
            errorMessage = "Gagal inisiasi: " + e.getMessage();
            Log.e(TAG, errorMessage);
        }
    }

    // FUNGSI 2: Mengirim Data Login & HWID HP
    public void login(String username, String pass, String hwid) {
        if (sessionid == null || sessionid.isEmpty()) {
            isSuccess = false;
            errorMessage = "Session Invalid. Pastikan init() berhasil.";
            Log.e(TAG, errorMessage);
            return;
        }

        try {
            String formData = "type=login&" +
                    "username=" + URLEncoder.encode(username, "UTF-8") + "&" +
                    "pass=" + URLEncoder.encode(pass, "UTF-8") + "&" +
                    "hwid=" + URLEncoder.encode(hwid, "UTF-8") + "&" +
                    "sessionid=" + URLEncoder.encode(sessionid, "UTF-8") + "&" +
                    "name=" + URLEncoder.encode(name, "UTF-8") + "&" +
                    "ownerid=" + URLEncoder.encode(ownerid, "UTF-8");

            Log.d(TAG, "Login request for user: " + username);

            String response = makeRequest(formData);

            if (response != null && !response.isEmpty()) {
                Log.d(TAG, "Login response: " + response);

                JSONObject json = new JSONObject(response);
                isSuccess = json.getBoolean("success");

                if (isSuccess) {
                    if (json.has("info")) {
                        userData = json.getJSONObject("info");
                    }
                    errorMessage = "";
                    Log.i(TAG, "✅ Login successful for user: " + username);
                } else {
                    userData = null;
                    errorMessage = json.getString("message");
                    Log.e(TAG, "❌ Login failed: " + errorMessage);
                }
            } else {
                isSuccess = false;
                errorMessage = "Empty response from server";
                Log.e(TAG, errorMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
            isSuccess = false;
            errorMessage = "Koneksi gagal: " + e.getMessage();
            userData = null;
            Log.e(TAG, errorMessage);
        }
    }

    // Overload method login tanpa HWID
    public void login(String username, String pass) {
        login(username, pass, "");
    }

    // FUNGSI 3: Register akun baru
    public void register(String username, String pass, String key) {
        if (sessionid == null || sessionid.isEmpty()) {
            isSuccess = false;
            errorMessage = "Session Invalid. Pastikan init() berhasil.";
            Log.e(TAG, errorMessage);
            return;
        }

        try {
            String formData = "type=register&" +
                    "username=" + URLEncoder.encode(username, "UTF-8") + "&" +
                    "pass=" + URLEncoder.encode(pass, "UTF-8") + "&" +
                    "key=" + URLEncoder.encode(key, "UTF-8") + "&" +
                    "sessionid=" + URLEncoder.encode(sessionid, "UTF-8") + "&" +
                    "name=" + URLEncoder.encode(name, "UTF-8") + "&" +
                    "ownerid=" + URLEncoder.encode(ownerid, "UTF-8");

            Log.d(TAG, "Register request for user: " + username);

            String response = makeRequest(formData);

            if (response != null && !response.isEmpty()) {
                Log.d(TAG, "Register response: " + response);

                JSONObject json = new JSONObject(response);
                isSuccess = json.getBoolean("success");

                if (isSuccess) {
                    errorMessage = "";
                    Log.i(TAG, "✅ Register successful for user: " + username);
                } else {
                    errorMessage = json.getString("message");
                    Log.e(TAG, "❌ Register failed: " + errorMessage);
                }
            } else {
                isSuccess = false;
                errorMessage = "Empty response from server";
                Log.e(TAG, errorMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
            isSuccess = false;
            errorMessage = "Koneksi gagal: " + e.getMessage();
            Log.e(TAG, errorMessage);
        }
    }

    // FUNGSI 4: Cek session (untuk auto-login)
    public void checkSession() {
        if (sessionid == null || sessionid.isEmpty()) {
            isSuccess = false;
            errorMessage = "Session Invalid. Pastikan init() berhasil.";
            return;
        }

        try {
            String formData = "type=check&" +
                    "sessionid=" + URLEncoder.encode(sessionid, "UTF-8") + "&" +
                    "name=" + URLEncoder.encode(name, "UTF-8") + "&" +
                    "ownerid=" + URLEncoder.encode(ownerid, "UTF-8");

            Log.d(TAG, "Check session request");

            String response = makeRequest(formData);

            if (response != null && !response.isEmpty()) {
                Log.d(TAG, "Check session response: " + response);

                JSONObject json = new JSONObject(response);
                isSuccess = json.getBoolean("success");

                if (isSuccess) {
                    if (json.has("info")) {
                        userData = json.getJSONObject("info");
                    }
                    errorMessage = "";
                    Log.i(TAG, "✅ Session valid");
                } else {
                    errorMessage = json.getString("message");
                    Log.e(TAG, "❌ Session invalid: " + errorMessage);
                }
            } else {
                isSuccess = false;
                errorMessage = "Empty response from server";
            }
        } catch (Exception e) {
            e.printStackTrace();
            isSuccess = false;
            errorMessage = "Koneksi gagal: " + e.getMessage();
        }
    }

    // FUNGSI 5: Logout
    public void logout() {
        if (sessionid == null || sessionid.isEmpty()) {
            isSuccess = false;
            errorMessage = "Session Invalid";
            return;
        }

        try {
            String formData = "type=logout&" +
                    "sessionid=" + URLEncoder.encode(sessionid, "UTF-8") + "&" +
                    "name=" + URLEncoder.encode(name, "UTF-8") + "&" +
                    "ownerid=" + URLEncoder.encode(ownerid, "UTF-8");

            Log.d(TAG, "Logout request");

            String response = makeRequest(formData);

            if (response != null && !response.isEmpty()) {
                Log.d(TAG, "Logout response: " + response);

                JSONObject json = new JSONObject(response);
                isSuccess = json.getBoolean("success");

                if (isSuccess) {
                    sessionid = "";
                    errorMessage = "";
                    Log.i(TAG, "✅ Logout successful");
                } else {
                    errorMessage = json.getString("message");
                    Log.e(TAG, "❌ Logout failed: " + errorMessage);
                }
            } else {
                isSuccess = false;
                errorMessage = "Empty response from server";
            }
        } catch (Exception e) {
            e.printStackTrace();
            isSuccess = false;
            errorMessage = "Koneksi gagal: " + e.getMessage();
        }
    }

    // Method untuk membuat request HTTP ke KeyAuth API
    private String makeRequest(String formData) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("User-Agent", "KeyAuth-Android/1.0");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setUseCaches(false);

            // Kirim data
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = formData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            // Baca response
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();

        } catch (Exception e) {
            Log.e(TAG, "Request error: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // ============= CLASS SESSIONDATA UNTUK MENYIMPAN SESSION =============
    public static class SessionData implements Serializable {
        private static final long serialVersionUID = 1L;

        public String username;
        public String sessionId;
        public long timestamp;

        public SessionData(String username, String sessionId) {
            this.username = username;
            this.sessionId = sessionId;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isValid() {
            // Session valid untuk 7 hari
            long diff = System.currentTimeMillis() - timestamp;
            return diff < 7 * 24 * 60 * 60 * 1000L;
        }

        public String getUsername() {
            return username;
        }

        public String getSessionId() {
            return sessionId;
        }
    }
}