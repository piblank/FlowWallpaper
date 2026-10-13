package com.example.flowwallpaper;

/**
 * Maps a parameter t in [0,1] across a list of RGB stops and precomputes
 * a fixed number of evenly spaced bucket colours (for cheap batched drawing).
 */
public class FlowPalette {

    private final int[] stops;
    private final int bucketCount;
    private final int[][] bucket;

    public FlowPalette(int[] colors, int bucketCount) {
        this.stops = colors.clone();
        this.bucketCount = bucketCount;
        this.bucket = new int[bucketCount][3];
        rebuild();
    }

    public void setColors(int[] colors) {
        System.arraycopy(colors, 0, stops, 0, Math.min(colors.length, stops.length));
        rebuild();
    }

    private void rebuild() {
        int segs = stops.length / 3 - 1;
        if (segs < 1) segs = 1;
        for (int i = 0; i < bucketCount; i++) {
            double t = (double) i / bucketCount;
            double s = t * segs;
            int idx = (int) Math.floor(s);
            double f = s - idx;
            int a0 = idx * 3;
            int b0 = Math.min(idx + 1, segs) * 3;
            bucket[i][0] = (int) (stops[a0] + (stops[b0] - stops[a0]) * f);
            bucket[i][1] = (int) (stops[a0 + 1] + (stops[b0 + 1] - stops[a0 + 1]) * f);
            bucket[i][2] = (int) (stops[a0 + 2] + (stops[b0 + 2] - stops[a0 + 2]) * f);
        }
    }

    public int[] colorAt(int b) {
        return bucket[b];
    }

    public int bucketCount() {
        return bucketCount;
    }
}
