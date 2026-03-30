package com.kiwmodz.exe;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnowEffectView extends View {

    // Konfigurasi Salju
    private static final int NUM_SNOWFLAKES = 150; // Jumlah butiran
    private static final int DELAY = 10; // Kecepatan refresh (ms) - semakin kecil semakin smooth

    private List<Snowflake> snowflakes;
    private Paint paint;
    private Random random;

    public SnowEffectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.WHITE); // Warna salju
        paint.setAntiAlias(true);
        random = new Random();
        snowflakes = new ArrayList<>();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Buat butiran salju saat ukuran layar diketahui
        snowflakes.clear();
        for (int i = 0; i < NUM_SNOWFLAKES; i++) {
            snowflakes.add(new Snowflake(w, h, random));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Gambar setiap butiran salju
        for (Snowflake flake : snowflakes) {
            flake.fall(); // Update posisi jatuh
            // Gambar lingkaran kecil (salju)
            canvas.drawCircle(flake.x, flake.y, flake.radius, paint);
        }

        // Panggil kembali onDraw setelah DELAY agar animasi berjalan
        postInvalidateDelayed(DELAY);
    }

    // --- Inner Class untuk mendefinisikan properti satu butiran salju ---
    private static class Snowflake {
        float x, y, radius, speed;
        int width, height;
        Random random;

        public Snowflake(int width, int height, Random random) {
            this.width = width;
            this.height = height;
            this.random = random;
            reset(true); // Reset awal, posisi Y acak di seluruh layar
        }

        // Mengatur posisi jatuh
        public void fall() {
            y += speed;
            // Jika sudah melewati bawah layar, reset ke atas
            if (y > height) {
                reset(false);
            }
        }

        // Reset butiran ke atas layar
        private void reset(boolean firstTime) {
            radius = random.nextFloat() * 4 + 2; // Ukuran acak 2-6
            speed = random.nextFloat() * 3 + 1;  // Kecepatan acak 1-4
            x = random.nextInt(width);          // Posisi X acak

            if (firstTime) {
                y = random.nextInt(height);      // Saat app buka, salju sudah ada di mana-mana
            } else {
                y = -radius;                    // Saat reset, mulai dari sedikit di atas layar
            }
        }
    }
}