package com.heypixel.heypixelmod.obsoverlay.utils.renderer.IslandHUD;

public interface Easing {
    double apply(double t);
    default double interpolate(double start, double end, double t) {
        double k = apply(clamp01(t));
        return start + (end - start) * k;
    }
    default float interpolate(float start, float end, float t) {
        double k = apply(clamp01(t));
        return (float)(start + (end - start) * k);
    }
    default double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}