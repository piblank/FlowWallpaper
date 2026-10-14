package com.example.flowwallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Lets the user customise the wallpaper: colour stops (RGB CSV),
 * particle count, flow speed, and reseed the field. All changes are
 * stored in SharedPreferences and picked up live by the running engine.
 */
public class SettingsActivity extends Activity {

    private EditText etColors;
    private EditText etParticles;
    private EditText etSpeed;

    private static final String DEFAULT_COLORS = "255,100,90,255,190,60,255,50,170,80,120,255";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etColors = findViewById(R.id.etColors);
        etParticles = findViewById(R.id.etParticles);
        etSpeed = findViewById(R.id.etSpeed);

        // Open the system live-wallpaper picker, which DOES list this third-party
        // wallpaper (unlike the MIUI/HyperOS home-screen wallpaper entry).
        Button setWp = findViewById(R.id.btnSetWallpaper);
        setWp.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "无法打开系统动态壁纸选择器：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        SharedPreferences prefs = getSharedPreferences(FlowWallpaperService.PREFS, MODE_PRIVATE);
        etColors.setText(prefs.getString(FlowWallpaperService.KEY_COLORS, DEFAULT_COLORS));
        etParticles.setText(String.valueOf(prefs.getInt(FlowWallpaperService.KEY_PARTICLES, 2000)));
        etSpeed.setText(String.valueOf(prefs.getFloat(FlowWallpaperService.KEY_SPEED, 1.55f)));

        Button apply = findViewById(R.id.btnApply);
        apply.setOnClickListener(v -> {
            SharedPreferences.Editor e = prefs.edit();
            e.putString(FlowWallpaperService.KEY_COLORS, etColors.getText().toString().trim());
            try {
                e.putInt(FlowWallpaperService.KEY_PARTICLES,
                        Integer.parseInt(etParticles.getText().toString().trim()));
            } catch (Exception ignored) {
            }
            try {
                e.putFloat(FlowWallpaperService.KEY_SPEED,
                        Float.parseFloat(etSpeed.getText().toString().trim()));
            } catch (Exception ignored) {
            }
            e.apply();
            Toast.makeText(this, "已应用，返回桌面查看效果", Toast.LENGTH_SHORT).show();
        });

        Button reseed = findViewById(R.id.btnReseed);
        reseed.setOnClickListener(v -> {
            prefs.edit().putLong(FlowWallpaperService.KEY_SEED, System.currentTimeMillis()).apply();
            Toast.makeText(this, "已重新生成流场", Toast.LENGTH_SHORT).show();
        });

        setupPreset(R.id.presetSunset, DEFAULT_COLORS);
        setupPreset(R.id.presetOcean, "30,144,255,0,206,209,70,130,180,20,40,90");
        setupPreset(R.id.presetNeon, "255,0,170,0,255,255,138,43,226,255,110,199");
        setupPreset(R.id.presetMono, "230,230,255,150,170,255,90,110,200,40,50,120");
    }

    private void setupPreset(int id, String csv) {
        Button b = findViewById(id);
        b.setOnClickListener(v -> etColors.setText(csv));
    }
}
