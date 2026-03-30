package com.kiwmodz.exe;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

public class FloatingMenuService extends Service {
    private WindowManager windowManager;
    private View floatingMenuView;
    private View floatingIconView;
    private WindowManager.LayoutParams menuParams;
    private WindowManager.LayoutParams iconParams;

    private LinearLayout featureListContainer;
    private TextView tvStatusInfo;

    private LinearLayout tabMemory, tabVisual, tabExtra;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutFlag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        menuParams = new WindowManager.LayoutParams(
                dpToPx(340),
                dpToPx(240),
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        menuParams.gravity = Gravity.CENTER;

        iconParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        iconParams.gravity = Gravity.TOP | Gravity.START;
        iconParams.x = 0; iconParams.y = 100;

        floatingMenuView = LayoutInflater.from(this).inflate(R.layout.layout_floating_menu, null);
        floatingIconView = LayoutInflater.from(this).inflate(R.layout.layout_floating_icon, null);

        featureListContainer = floatingMenuView.findViewById(R.id.featureListContainer);
        tvStatusInfo = floatingMenuView.findViewById(R.id.tvStatusInfo);

        tabMemory = floatingMenuView.findViewById(R.id.tabMemory);
        tabVisual = floatingMenuView.findViewById(R.id.tabVisual);
        tabExtra = floatingMenuView.findViewById(R.id.tabExtra);

        setupIconLogic();
        setupMenuLogic();
        setupMenuDragLogic();

        windowManager.addView(floatingIconView, iconParams);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void setupIconLogic() {
        View iconContainer = floatingIconView.findViewById(R.id.floatingIconContainer);
        iconContainer.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isClick;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = iconParams.x; initialY = iconParams.y;
                        initialTouchX = event.getRawX(); initialTouchY = event.getRawY();
                        isClick = true; return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) isClick = false;
                        iconParams.x = initialX + (int) dx; iconParams.y = initialY + (int) dy;
                        windowManager.updateViewLayout(floatingIconView, iconParams); return true;
                    case MotionEvent.ACTION_UP:
                        if (isClick) {
                            if (floatingIconView.getParent() != null) windowManager.removeView(floatingIconView);
                            if (floatingMenuView.getParent() == null) windowManager.addView(floatingMenuView, menuParams);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void setupMenuDragLogic() {
        View headerTitle = floatingMenuView.findViewById(R.id.tvHeaderTitle);
        headerTitle.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = menuParams.x;
                        initialY = menuParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        menuParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        menuParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingMenuView, menuParams);
                        return true;
                }
                return false;
            }
        });
    }

    private void setupMenuLogic() {
        tabMemory.setOnClickListener(v -> { setActiveTab(tabMemory); renderMemory(); });
        tabVisual.setOnClickListener(v -> { setActiveTab(tabVisual); renderVisual(); });
        tabExtra.setOnClickListener(v -> { setActiveTab(tabExtra); renderExtra(); });

        floatingMenuView.findViewById(R.id.btnCloseMenu).setOnClickListener(v -> {
            if (floatingMenuView.getParent() != null) windowManager.removeView(floatingMenuView);
            if (floatingIconView.getParent() == null) windowManager.addView(floatingIconView, iconParams);
        });

        resetAllTabs();
        featureListContainer.removeAllViews();
        tvStatusInfo.setText("Status: Ready");
    }

    private void resetAllTabs() {
        tabMemory.setAlpha(0.4f);
        tabVisual.setAlpha(0.4f);
        tabExtra.setAlpha(0.4f);
    }

    private void setActiveTab(LinearLayout activeTab) {
        resetAllTabs(); activeTab.setAlpha(1.0f);
    }

    private void renderMemory() {
        featureListContainer.removeAllViews();
        addToggle("Less Recoil", Config.isLessRecoil, isChecked -> Config.isLessRecoil = isChecked);
        addToggle("Antenna", Config.isAntenna, isChecked -> Config.isAntenna = isChecked);
        addToggle("Magic Bullet", Config.isMagicBullet, isChecked -> Config.isMagicBullet = isChecked);
        addToggle("Aimbot Master", Config.isAimbotMaster, isChecked -> Config.isAimbotMaster = isChecked);
    }

    private void renderVisual() {
        featureListContainer.removeAllViews();
        addToggle("ESP Line", Config.isEspLine, isChecked -> Config.isEspLine = isChecked);
        addToggle("ESP Box", Config.isEspBox, isChecked -> Config.isEspBox = isChecked);
        addSlider("ESP Distance", 0, 500, Config.espDistance, value -> Config.espDistance = value);
    }

    private void renderExtra() {
        featureListContainer.removeAllViews();
        addToggle("Show Loot", Config.isShowLoot, isChecked -> Config.isShowLoot = isChecked);
        addToggle("Show Vehicles", Config.isShowVehicles, isChecked -> Config.isShowVehicles = isChecked);
        addDropdown("Camera View", new String[]{"Normal", "Wide", "Ultra Wide"}, Config.cameraView, position -> Config.cameraView = position);
    }

    private interface OnToggleListener { void onToggle(boolean isChecked); }
    private void addToggle(String name, boolean initialState, OnToggleListener listener) {
        Button btn = new Button(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(30));
        lp.setMargins(dpToPx(10), dpToPx(3), dpToPx(10), dpToPx(3));
        btn.setLayoutParams(lp);
        btn.setAllCaps(false);
        btn.setTextSize(10f);
        btn.setPadding(0, 0, 0, 0);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);

        final boolean[] state = {initialState};
        updateToggleStyle(btn, name, state[0]);

        btn.setOnClickListener(v -> {
            state[0] = !state[0];
            updateToggleStyle(btn, name, state[0]);

            // LOGIKA BARU: MENGUBAH TEKS STATUS SAAT TOMBOL DIKLIK
            tvStatusInfo.setText("Status: " + name + (state[0] ? " ON" : " OFF"));

            if (listener != null) listener.onToggle(state[0]);
        });
        featureListContainer.addView(btn);
    }

    private void updateToggleStyle(Button btn, String name, boolean isOn) {
        btn.setBackgroundResource(isOn ? R.drawable.btn_neon_green : R.drawable.btn_neon_red);
        btn.setText(name + (isOn ? " [ON]" : " [OFF]"));
        btn.setTextColor(isOn ? Color.parseColor("#00FF00") : Color.parseColor("#FF0000"));
    }

    private interface OnSliderListener { void onValueChanged(float value); }
    private void addSlider(String name, float min, float max, float currentValue, OnSliderListener listener) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        layout.setLayoutParams(lp);

        TextView tv = new TextView(this);
        tv.setTextColor(Color.WHITE);
        tv.setText(name + ": " + (int) currentValue);
        tv.setTextSize(10f);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(dpToPx(5), 0, 0, 0);
        layout.addView(tv);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setPadding(dpToPx(8), 0, dpToPx(8), 0);
        seekBar.setMax((int) (max - min));
        seekBar.setProgress((int) (currentValue - min));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float actualValue = progress + min;
                tv.setText(name + ": " + (int) actualValue);
                if (listener != null) listener.onValueChanged(actualValue);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        layout.addView(seekBar);
        featureListContainer.addView(layout);
    }

    private interface OnDropdownListener { void onItemSelected(int position); }
    private void addDropdown(String title, String[] options, int initialSelection, OnDropdownListener listener) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        layout.setLayoutParams(lp);

        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(10f);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(dpToPx(5), 0, 0, dpToPx(2));
        layout.addView(tv);

        Spinner spinner = new Spinner(this);
        spinner.setBackgroundResource(R.drawable.bg_outline);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Color.WHITE);
                view.setTextSize(11f);
                view.setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(Color.WHITE);
                view.setBackgroundColor(Color.parseColor("#E6000000"));
                view.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
                return view;
            }
        };

        spinner.setAdapter(adapter);
        spinner.setSelection(initialSelection);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (listener != null) listener.onItemSelected(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        layout.addView(spinner);
        featureListContainer.addView(layout);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingMenuView != null && floatingMenuView.getParent() != null) windowManager.removeView(floatingMenuView);
        if (floatingIconView != null && floatingIconView.getParent() != null) windowManager.removeView(floatingIconView);
    }
}