package me.totalchaos01.chaosclient.util.render;

import net.minecraft.client.MinecraftClient;

/**
 * FPS-compensated animation utility with multiple easing functions.
 * Ported from Rise Client with modern delta-time compensation.
 */
public class Animate {

    private double value;
    private double target;
    private double speed;
    private long lastTime;

    public Animate() {
        this.value = 0;
        this.target = 0;
        this.speed = 0.15;
        this.lastTime = System.currentTimeMillis();
    }

    public Animate(double initialValue, double speed) {
        this.value = initialValue;
        this.target = initialValue;
        this.speed = speed;
        this.lastTime = System.currentTimeMillis();
    }

    /**
     * FPS-compensated update — smooth at any framerate.
     */
    public void update() {
        long now = System.currentTimeMillis();
        double delta = (now - lastTime) / 16.667; // normalized to 60fps
        lastTime = now;
        delta = Math.max(0.1, Math.min(delta, 5.0)); // clamp

        value += (target - value) * Math.min(speed * delta, 1.0);
        if (Math.abs(target - value) < 0.001) {
            value = target;
        }
    }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; this.lastTime = System.currentTimeMillis(); }
    public double getTarget() { return target; }
    public void setTarget(double target) { this.target = target; }
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public boolean isFinished() { return value == target; }

    /**
     * Instant jump to target.
     */
    public void snap() { this.value = this.target; }

    /**
     * Set both value and target (no animation).
     */
    public void reset(double v) { this.value = v; this.target = v; }

    // ─── Static helpers ───────────────────────────────────────

    /**
     * FPS-compensated lerp — use in render methods.
     */
    public static double lerp(double current, double target, double speed) {
        int fps = Math.max(1, MinecraftClient.getInstance().getCurrentFps());
        double factor = Math.min(speed * (60.0 / fps), 1.0);
        return current + (target - current) * factor;
    }

    /**
     * Basic lerp (not FPS-compensated) — for normalized t values.
     */
    public static double mix(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * Smooth clamp between 0 and 1.
     */
    public static double smoothStep(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    public static double easeOut(double t) {
        return 1.0 - Math.pow(1.0 - t, 3);
    }

    public static double easeIn(double t) {
        return t * t * t;
    }

    public static double easeInOut(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    /**
     * Elastic ease-out — bouncy overshoot effect.
     */
    public static double easeOutElastic(double t) {
        if (t == 0 || t == 1) return t;
        return Math.pow(2, -10 * t) * Math.sin((t - 0.075) * (2 * Math.PI) / 0.3) + 1;
    }

    /**
     * Back ease-out — slight overshoot.
     */
    public static double easeOutBack(double t) {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }

    /**
     * Bounce ease-out.
     */
    public static double easeOutBounce(double t) {
        if (t < 1 / 2.75) return 7.5625 * t * t;
        if (t < 2 / 2.75) { t -= 1.5 / 2.75; return 7.5625 * t * t + 0.75; }
        if (t < 2.5 / 2.75) { t -= 2.25 / 2.75; return 7.5625 * t * t + 0.9375; }
        t -= 2.625 / 2.75;
        return 7.5625 * t * t + 0.984375;
    }

    /**
     * Exponential ease-in-out — punchy, starts slow, hits hard, ends slow.
     */
    public static double easeInOutExpo(double t) {
        if (t == 0) return 0;
        if (t == 1) return 1;
        if (t < 0.5) return Math.pow(2, 20 * t - 10) / 2;
        return (2 - Math.pow(2, -20 * t + 10)) / 2;
    }

    /**
     * Sine ease-in-out — smooth and natural.
     */
    public static double easeInOutSine(double t) {
        return -(Math.cos(Math.PI * t) - 1) / 2;
    }

    /**
     * Quintic ease-out — fast start, smooth end.
     */
    public static double easeOutQuint(double t) {
        return 1 - Math.pow(1 - t, 5);
    }
}
