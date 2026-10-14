package com.example.flowwallpaper;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.ArrayList;

/**
 * Native Canvas live wallpaper: thousands of particles advected through a
 * seeded 3D Perlin noise vector field, leaving semi-transparent trails.
 * Colours / particle count / speed / seed are read live from SharedPreferences.
 */
public class FlowWallpaperService extends WallpaperService {

    public static final String PREFS = "flow_prefs";
    public static final String KEY_COLORS = "colors";
    public static final String KEY_SEED = "seed";
    public static final String KEY_PARTICLES = "particles";
    public static final String KEY_SPEED = "speed";

    @Override
    public Engine onCreateEngine() {
        return new FlowEngine();
    }

    private static class Particle {
        float x, y, px, py;
        int bucket;
    }

    private class FlowEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {

        private final Handler handler = new Handler();
        private boolean visible = false;

        private Perlin perlin;
        private FlowPalette palette;
        private Particle[] particles;
        private ArrayList<Particle>[] buckets;
        private final Paint fadePaint = new Paint();
        private final Paint linePaint = new Paint();

        private int W, H;
        private long seed = 0;
        private float colorOffX, colorOffY;
        private final int bgColor = 0xFF07060d;
        private int particleCount = 3200;
        private float speed = 1.35f;

        private static final float NOISE_SCALE = 0.0016f;
        private static final float TIME_SCALE = 0.00009f;
        private static final float TURNS = 2.4f;
        private static final float FADE = 0.03f;
        private static final float TRAIL = 0.55f;
        private static final int BUCKETS = 48;
        private static final float R = 150f;
        private static final float FORCE = 7f;
        private static final long FRAME_DELAY = 16; // ~60fps, battery friendly

        private long frame = 0;
        private float pointerX = -1, pointerY = -1;
        private boolean pointerActive = false;
        private boolean clearRequested = false;
        private final int[] bucketColors = new int[BUCKETS];

        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                drawFrame();
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setTouchEventsEnabled(true);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(1.3f);
            linePaint.setStrokeCap(Paint.Cap.ROUND);
            linePaint.setAntiAlias(false);
            fadePaint.setAntiAlias(false);

            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            prefs.registerOnSharedPreferenceChangeListener(this);
            loadPrefs(prefs);
        }

        private void loadPrefs(SharedPreferences prefs) {
            String csv = prefs.getString(KEY_COLORS, null);
            int[] colors = (csv == null) ? defaultColors() : parseColors(csv);
            if (palette == null) palette = new FlowPalette(colors, BUCKETS);
            else palette.setColors(colors);

            long s = prefs.getLong(KEY_SEED, 0);
            if (s == 0) {
                s = System.currentTimeMillis();
                prefs.edit().putLong(KEY_SEED, s).apply();
            }
            if (s != seed || perlin == null) {
                seed = s;
                perlin = new Perlin(seed);
                colorOffX = (seed & 0xFFFF) * 0.013f;
                colorOffY = ((seed >> 16) & 0xFFFF) * 0.013f;
                clearRequested = true;
            }

            particleCount = prefs.getInt(KEY_PARTICLES, 3200);
            speed = prefs.getFloat(KEY_SPEED, 1.35f);

            if (buckets == null) {
                buckets = new ArrayList[BUCKETS];
                for (int b = 0; b < BUCKETS; b++) buckets[b] = new ArrayList<Particle>();
            }
            recomputeBucketColors();
        }

        private void recomputeBucketColors() {
            for (int b = 0; b < BUCKETS; b++) {
                int[] c = palette.colorAt(b);
                bucketColors[b] = Color.argb((int) (TRAIL * 255), c[0], c[1], c[2]);
            }
        }

        private int[] defaultColors() {
            return new int[]{255, 111, 97, 255, 179, 71, 232, 74, 138, 91, 64, 153};
        }

        private int[] parseColors(String csv) {
            try {
                String[] parts = csv.split(",");
                int[] arr = new int[parts.length];
                for (int i = 0; i < parts.length; i++) arr[i] = Integer.parseInt(parts[i].trim());
                if (arr.length < 3 || arr.length % 3 != 0) return defaultColors();
                return arr;
            } catch (Exception e) {
                return defaultColors();
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            loadPrefs(prefs);
            initParticles();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            W = width;
            H = height;
            initParticles();
            if (visible) drawFrame();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            visible = false;
            handler.removeCallbacks(drawRunner);
        }

        @Override
        public void onVisibilityChanged(boolean v) {
            visible = v;
            if (visible) {
                if (isReady()) drawFrame();
                else scheduleFrame();
            } else {
                handler.removeCallbacks(drawRunner);
            }
        }

        private boolean isReady() {
            return W > 0 && H > 0 && particles != null && perlin != null && palette != null && buckets != null;
        }

        private void scheduleFrame() {
            handler.removeCallbacks(drawRunner);
            handler.postDelayed(drawRunner, FRAME_DELAY);
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            pointerX = event.getX();
            pointerY = event.getY();
            int a = event.getAction();
            pointerActive = (a != MotionEvent.ACTION_UP && a != MotionEvent.ACTION_CANCEL);
        }

        private void initParticles() {
            if (W <= 0 || H <= 0 || perlin == null) return;
            if (particles == null || particles.length != particleCount) {
                particles = new Particle[particleCount];
                for (int i = 0; i < particleCount; i++) particles[i] = new Particle();
            }
            for (int b = 0; b < BUCKETS; b++) buckets[b].clear();
            for (int i = 0; i < particleCount; i++) {
                Particle p = particles[i];
                p.x = (float) (Math.random() * W);
                p.y = (float) (Math.random() * H);
                p.px = p.x;
                p.py = p.y;
                double t = (perlin.noise(p.x * 0.0013 + colorOffX, p.y * 0.0013 + colorOffY, 5.5) + 1) * 0.5;
                p.bucket = Math.min(BUCKETS - 1, (int) (t * BUCKETS));
            }
        }

        private void drawFrame() {
            if (!visible) return;
            if (!isReady()) {
                scheduleFrame();
                return;
            }
            SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c == null) {
                    scheduleFrame();
                    return;
                }

                if (clearRequested) {
                    fadePaint.setAlpha(255);
                    fadePaint.setColor(bgColor);
                    c.drawRect(0, 0, W, H, fadePaint);
                    clearRequested = false;
                } else {
                    fadePaint.setAlpha((int) (FADE * 255));
                    fadePaint.setColor(bgColor);
                    c.drawRect(0, 0, W, H, fadePaint);
                }

                for (int b = 0; b < BUCKETS; b++) buckets[b].clear();

                double z = frame * TIME_SCALE;
                float tr = (float) (Math.PI * 2 * TURNS);
                boolean pa = pointerActive;
                for (int i = 0; i < particleCount; i++) {
                    Particle p = particles[i];
                    double angle = perlin.noise(p.x * NOISE_SCALE, p.y * NOISE_SCALE, z) * tr;
                    p.px = p.x;
                    p.py = p.y;
                    float nx = p.x + (float) Math.cos(angle) * speed;
                    float ny = p.y + (float) Math.sin(angle) * speed;

                    if (pa) {
                        float dx = nx - pointerX;
                        float dy = ny - pointerY;
                        float d2 = dx * dx + dy * dy;
                        if (d2 < R * R && d2 > 0.01f) {
                            float d = (float) Math.sqrt(d2);
                            float push = (1 - d / R) * FORCE;
                            nx += (dx / d) * push;
                            ny += (dy / d) * push;
                        }
                    }

                    p.x = nx;
                    p.y = ny;
                    if (p.x < 0 || p.x >= W || p.y < 0 || p.y >= H) {
                        p.x = (float) (Math.random() * W);
                        p.y = (float) (Math.random() * H);
                        p.px = p.x;
                        p.py = p.y;
                        double t = (perlin.noise(p.x * 0.0013 + colorOffX, p.y * 0.0013 + colorOffY, 5.5) + 1) * 0.5;
                        p.bucket = Math.min(BUCKETS - 1, (int) (t * BUCKETS));
                    } else {
                        buckets[p.bucket].add(p);
                    }
                }

                Path path = new Path();
                for (int b = 0; b < BUCKETS; b++) {
                    ArrayList<Particle> list = buckets[b];
                    int n = list.size();
                    if (n == 0) continue;
                    path.reset();
                    for (int i = 0; i < n; i++) {
                        Particle p = list.get(i);
                        path.moveTo(p.px, p.py);
                        path.lineTo(p.x, p.y);
                    }
                    linePaint.setColor(bucketColors[b]);
                    c.drawPath(path, linePaint);
                }
                frame++;
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }
            scheduleFrame();
        }
    }
}
