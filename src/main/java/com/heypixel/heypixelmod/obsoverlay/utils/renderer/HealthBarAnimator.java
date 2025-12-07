package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

public class HealthBarAnimator {
    private float displayedHealth;
    private long lastUpdateTime;
    private final float animationSpeed;

    /**
     * Constructs a new HealthBarAnimator.
     * @param initialHealth The starting health value.
     * @param animationSpeed The speed of the animation. A higher value means faster transition.
     */
    public HealthBarAnimator(float initialHealth, float animationSpeed) {
        this.displayedHealth = initialHealth;
        this.lastUpdateTime = System.currentTimeMillis();
        this.animationSpeed = animationSpeed;
    }

    /**
     * Updates the displayed health value based on the target health and elapsed time.
     * @param targetHealth The health value to animate towards.
     */
    public void update(float targetHealth) {
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - this.lastUpdateTime;
        this.lastUpdateTime = currentTime;

        // 如果目标血量与当前显示血量相差不大，则直接更新，避免抖动
        if (Math.abs(targetHealth - this.displayedHealth) < 0.1f) {
            this.displayedHealth = targetHealth;
            return;
        }

        // 使用线性插值平滑过渡
        float change = (targetHealth - this.displayedHealth) * (deltaTime / 1000.0f) * this.animationSpeed;
        this.displayedHealth += change;

        // 确保血量不会超过目标值
        if (targetHealth > this.displayedHealth && change < 0) {
            this.displayedHealth = targetHealth;
        } else if (targetHealth < this.displayedHealth && change > 0) {
            this.displayedHealth = targetHealth;
        }
    }

    /**
     * Resets the animator to a new immediate health value.
     * @param newHealth The new health value.
     */
    public void reset(float newHealth) {
        this.displayedHealth = newHealth;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Gets the current animated health value.
     * @return The smoothly interpolated health value.
     */
    public float getDisplayedHealth() {
        return displayedHealth;
    }
}