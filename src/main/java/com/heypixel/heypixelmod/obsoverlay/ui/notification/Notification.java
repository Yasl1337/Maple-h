package com.heypixel.heypixelmod.obsoverlay.ui.notification;

import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Notification {
    protected static final Logger LOGGER = LoggerFactory.getLogger(Notification.class);
    
    private NotificationLevel level;
    private String title, description;
    private long maxAge;
    private long createTime = System.currentTimeMillis();
    private SmoothAnimationTimer widthTimer = new SmoothAnimationTimer(0.0F);
    private SmoothAnimationTimer heightTimer = new SmoothAnimationTimer(0.0F);
    private TimeHelper timerUtil = new TimeHelper();

    public Notification(NotificationLevel level, String message, long age) {
        this.level = level;
        if (message.contains(":")) {
            String[] parts = message.split(":", 2);
            this.title = parts[0].trim();
            this.description = parts[1].trim();
        } else {
            this.title = "Notification";
            this.description = message;
        }
        this.maxAge = age;
    }

    public Notification(NotificationLevel level, String title, String description, long age) {
        this.level = level;
        this.title = title;
        this.description = description;
        this.maxAge = age;
    }
    

    public Notification(String message, boolean enabled) {
        this.level = enabled ? NotificationLevel.SUCCESS : NotificationLevel.ERROR;
        if (message.contains(":")) {
            String[] parts = message.split(":", 2);
            this.title = parts[0].trim();
            this.description = parts[1].trim();
        } else {
            this.title = "Notification";
            this.description = message;
        }
        this.maxAge = 2000L;
    }

    public float getWidth() {
        return 100.0F;
    }

    public float getHeight() {
        return 30.0F;
    }

    public NotificationLevel getLevel() {
        return level;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getMessage() {
        return description;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public long getCreateTime() {
        return createTime;
    }

    public SmoothAnimationTimer getWidthTimer() {
        return widthTimer;
    }

    public SmoothAnimationTimer getHeightTimer() {
        return heightTimer;
    }

    public TimeHelper getTimerUtil() {
        return timerUtil;
    }

    public boolean isAlive() {
        return System.currentTimeMillis() - createTime < maxAge;
    }

    public void updateTimers() {
        widthTimer.update(true);
        heightTimer.update(true);
    }

    public float getAlpha() {
        long elapsed = System.currentTimeMillis() - createTime;
        if (elapsed < 500) {
            return Mth.clamp(elapsed / 500.0F, 0.0F, 1.0F);
        } else if (elapsed > maxAge - 500) {
            return Mth.clamp(1.0F - (elapsed - (maxAge - 500)) / 500.0F, 0.0F, 1.0F);
        }
        return 1.0F;
    }

    
    public static Notification create(NotificationLevel level, String message, long age) {
        if (NotificationMode.isNavenMode()) {
            return new NavenNotification(level, message, age);
        } else if (NotificationMode.isCapsuleMode()) {
            return new CapsuleNotification(level, message, age);
        } else {
            return new NewNotification(level, message, age);
        }
    }
    
    public static Notification create(NotificationLevel level, String title, String description, long age) {
        if (NotificationMode.isNavenMode()) {
            return new NavenNotification(level, description, age);
        } else if (NotificationMode.isCapsuleMode()) {
            return new CapsuleNotification(level, description, age);
        } else {
            return new NewNotification(level, description, age);
        }
    }
    
    public static Notification create(String message, boolean enabled) {
        if (NotificationMode.isNavenMode()) {
            return new NavenNotification(message, enabled);
        } else if (NotificationMode.isCapsuleMode()) {
            return new CapsuleNotification(enabled ? NotificationLevel.SUCCESS : NotificationLevel.ERROR, message, 2000L);
        } else {
            return new NewNotification(enabled ? NotificationLevel.SUCCESS : NotificationLevel.ERROR, message, 2000L);
        }
    }

    public void render(PoseStack stack, float x, float y) {

    }

    public void renderShader(PoseStack stack, float x, float y) {

    }
}