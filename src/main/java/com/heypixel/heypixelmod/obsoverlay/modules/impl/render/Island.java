package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.NavenTitle;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.IslandHUD.EaseCube;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.IslandHUD.EaseOutExpo;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleInfo(
        name = "Island",
        description = "ISLAND~~~",
        category = Category.RENDER
)
public class Island extends Module {
    private static Island INSTANCE;
    private final FloatValue x = ValueBuilder.create(this, "X")
            .setDefaultFloatValue(0.0F)
            .setMinFloatValue(-1000.0F)
            .setMaxFloatValue(1000.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    private final FloatValue y = ValueBuilder.create(this, "Y")
            .setDefaultFloatValue(68.0F)
            .setMinFloatValue(-1000.0F)
            .setMaxFloatValue(1000.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    private final FloatValue width = ValueBuilder.create(this, "Width")
            .setDefaultFloatValue(220.0F)
            .setMinFloatValue(80.0F)
            .setMaxFloatValue(600.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    private final FloatValue height = ValueBuilder.create(this, "Height")
            .setDefaultFloatValue(30.0F)
            .setMinFloatValue(14.0F)
            .setMaxFloatValue(200.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    private final FloatValue radius = ValueBuilder.create(this, "Radius")
            .setDefaultFloatValue(10.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    private final BooleanValue centerX = ValueBuilder.create(this, "Center X").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue centerTitle = ValueBuilder.create(this, "Center Title").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue blurMask = ValueBuilder.create(this, "Blur Mask").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue bloomMask = ValueBuilder.create(this, "Bloom Mask").setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue bloomStrength = ValueBuilder.create(this, "Bloom Strength")
            .setVisibility(this.bloomMask::getCurrentValue)
            .setDefaultFloatValue(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.05F)
            .build()
            .getFloatValue();
    private final FloatValue backgroundAlpha = ValueBuilder.create(this, "Background Alpha")
            .setDefaultFloatValue(55.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(255.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    private final BooleanValue autoWidth = ValueBuilder.create(this, "Auto Width").setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue padding = ValueBuilder.create(this, "Padding")
            .setDefaultFloatValue(9.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(40.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    private final FloatValue textScale = ValueBuilder.create(this, "Text Scale")
            .setDefaultFloatValue(0.45F)
            .setMinFloatValue(0.2F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.01F)
            .build()
            .getFloatValue();
    private final BooleanValue showHeaderBar = ValueBuilder.create(this, "Show Header Bar").setDefaultBooleanValue(false).build().getBooleanValue();
    private final FloatValue headerBarHeight = ValueBuilder.create(this, "Header Bar Height")
            .setVisibility(this.showHeaderBar::getCurrentValue)
            .setDefaultFloatValue(4.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    private final BooleanValue autoHeight = ValueBuilder.create(this, "Auto Height").setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue vPadding = ValueBuilder.create(this, "Vertical Padding")
            .setDefaultFloatValue(6.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(40.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    private final BooleanValue noticesEnabled = ValueBuilder.create(this, "Notices").setDefaultBooleanValue(true).build().getBooleanValue();
    private final FloatValue arrayVertical = ValueBuilder.create(this, "Array Vertical")
            .setDefaultFloatValue(10.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(40.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    private final FloatValue noticeHoldSeconds = ValueBuilder.create(this, "Notice Hold (s)")
            .setDefaultFloatValue(0.7F)
            .setMinFloatValue(0.5F)
            .setMaxFloatValue(10.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    private final FloatValue noticeEnterMs = ValueBuilder.create(this, "Notice Enter (ms)")
            .setDefaultFloatValue(250.0F)
            .setMinFloatValue(50.0F)
            .setMaxFloatValue(1000.0F)
            .setFloatStep(10.0F)
            .build()
            .getFloatValue();
    private final FloatValue noticeExitMs = ValueBuilder.create(this, "Notice Exit (ms)")
            .setDefaultFloatValue(230.0F)
            .setMinFloatValue(50.0F)
            .setMaxFloatValue(1500.0F)
            .setFloatStep(10.0F)
            .build()
            .getFloatValue();
    private final FloatValue noticeScale = ValueBuilder.create(this, "Notice Text Scale")
            .setDefaultFloatValue(0.40F)
            .setMinFloatValue(0.2F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.01F)
            .build()
            .getFloatValue();
    private final FloatValue noticeSpacing = ValueBuilder.create(this, "Notice Spacing")
            .setDefaultFloatValue(2.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(20.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    private final FloatValue noticeInnerPadX = ValueBuilder.create(this, "Notice Inner Padding X")
            .setDefaultFloatValue(8.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(40.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();
    private final List<Notice> notices = new ArrayList<>();
    public Island() {
        INSTANCE = this;
    }
    public static Island getInstance() {
        return INSTANCE;
    }
    public static void postNotice(String msg) {
        if (INSTANCE != null) INSTANCE.addNotice(msg);
    }
    public void addNotice(String msg) {
        if (!this.noticesEnabled.getCurrentValue()) return;
        this.notices.add(new Notice(msg, System.currentTimeMillis(),
                (long) (this.noticeEnterMs.getCurrentValue()),
                (long) (this.noticeHoldSeconds.getCurrentValue() * 1000.0F),
                (long) (this.noticeExitMs.getCurrentValue())));
    }
    private final NavenTitle titleProvider = new NavenTitle();
    private final SmoothAnimationTimer animW = new SmoothAnimationTimer(240.0F);
    private final SmoothAnimationTimer animH = new SmoothAnimationTimer(240.0F);
    private boolean lastOverride = false;
    private long transitionStartMs = 0L;
    private float prevTargetW = 0F, prevTargetH = 0F;
    private boolean hasLastBox = false;
    private float lastBoxW = 0F, lastBoxH = 0F;
    private long activeTransDurMs = 320L;
    private float easeOutExpo(float x) {
        if (x >= 1f) return 1f;
        return (float) (1 - Math.pow(2, -10 * x));
    }
    private float easeInExpo(float x) {
        if (x <= 0f) return 0f;
        return (float) (Math.pow(2, 10 * (x - 1)));
    }
    private float easeCube(float x) {
        return x * x * x;
    }
    private float bounce(float x) {
        return (float) (Math.sin(x * Math.PI * 2.0) * Math.pow(1.0 - Math.min(1.0, x), 2.0) * 0.08);
    }
    private static final int POS_DUR_MS = 220;
    private float computeLeft(float boxWidth) {
        if (this.centerX.getCurrentValue()) {
            return (float) mc.getWindow().getGuiScaledWidth() / 2.0F - boxWidth / 2.0F + this.x.getCurrentValue();
        }
        return this.x.getCurrentValue();
    }
    @EventTarget
    public void onShader(EventShader e) {
        
        CustomTextRenderer font = Fonts.opensans;
        float[] wh = computeAnimatedBoxWH(font);
        float w = wh[0];
        float h = wh[1];
        float left = computeLeft(w);
        float top = this.y.getCurrentValue();
        float r = Math.max(0.0F, Math.min(this.radius.getCurrentValue(), Math.min(w, h) / 2.0F - 0.5F));
        if (e.getType() == EventType.BLUR && this.blurMask.getCurrentValue()) {
            RenderUtils.drawRoundedRect(e.getStack(), left, top, w, h, r, Integer.MIN_VALUE);
        }
        if (e.getType() == EventType.SHADOW && this.bloomMask.getCurrentValue()) {
            float s = Math.max(0.0F, Math.min(1.0F, this.bloomStrength.getCurrentValue()));
            int bgA = (int)Math.max(0.0F, Math.min(255.0F, this.backgroundAlpha.getCurrentValue()));
            int aBase = (int)(bgA * s);
            if (aBase > 0) {
                RenderUtils.drawRoundedRect(e.getStack(), left, top, w, h, r, new Color(0, 0, 0, aBase).getRGB());
            }
        }
    }
    @EventTarget
    public void onRender2D(EventRender2D e) {
        CustomTextRenderer font = Fonts.opensans;
        float[] wh = computeAnimatedBoxWH(font);
        float w = wh[0];
        float h = wh[1];
        String text = "";
        if (titleProvider != null) {
            text = titleProvider.getTitle();
        }
        double scale = this.textScale.getCurrentValue();
        float textWidth = font.getWidth(text, scale);
        float textHeight = (float) font.getHeight(true, scale);
        boolean overrideByNotices = noticesActive() && getActiveNoticeCount() > 0;
        float left = computeLeft(w);
        float top = this.y.getCurrentValue();
        float r = Math.max(0.0F, Math.min(this.radius.getCurrentValue(), Math.min(w, h) / 2.0F - 0.5F));
        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(e.getStack(), left, top, w, h, r, Integer.MIN_VALUE);
        StencilUtils.erase(true);
        int bgA = (int) Math.max(0.0F, Math.min(255.0F, this.backgroundAlpha.getCurrentValue()));
        RenderUtils.fillBound(e.getStack(), left, top, w, h, new Color(0, 0, 0, bgA).getRGB());
        if (this.showHeaderBar.getCurrentValue()) {
            float hb = Math.min(this.headerBarHeight.getCurrentValue(), h);
            RenderUtils.fill(e.getStack(), left, top, left + w, top + hb, new Color(150, 45, 45, 160).getRGB());
        }
        if (!overrideByNotices) {
            float tx = this.centerTitle.getCurrentValue() ? left + (w - textWidth) / 2.0F : left + this.padding.getCurrentValue();
            float ty = top + (h - textHeight) / 2.0F;
            font.render(e.getStack(), text, tx, ty, Color.WHITE, true, scale);
        }
        if (overrideByNotices) {
            renderNotices(e, font, left, top, w, h);
        }
        StencilUtils.dispose();
    }
    private float[] computeAnimatedBoxWH(CustomTextRenderer font) {
        float w = this.width.getCurrentValue();
        float h = this.height.getCurrentValue();
        String text = "";
        if (titleProvider != null) {
            text = titleProvider.getTitle();
        }
        double scale = this.textScale.getCurrentValue();
        float textWidth = font.getWidth(text, scale);
        float textHeight = (float) font.getHeight(true, scale);
        long now = System.currentTimeMillis();
        boolean rawActive = noticesActive() && getActiveNoticeCount() > 0;
        long holdMs = (long)(this.noticeHoldSeconds.getCurrentValue() * 1000.0F);
        long enterDurMs = (long)(this.noticeEnterMs.getCurrentValue());
        long exitDurMs = (long)(this.noticeExitMs.getCurrentValue());
        boolean effectiveActive = rawActive;
        if (!rawActive && this.lastOverride) {
            long sinceStart = now - this.transitionStartMs;
            if (sinceStart < holdMs) {
                effectiveActive = true;
            }
        }

        if (effectiveActive != this.lastOverride) {
            this.transitionStartMs = now;
            this.prevTargetW = this.hasLastBox ? this.lastBoxW : w;
            this.prevTargetH = this.hasLastBox ? this.lastBoxH : h;
            this.activeTransDurMs = effectiveActive ? enterDurMs : exitDurMs;
            this.lastOverride = effectiveActive;
        }
        if (effectiveActive) {
            double ns = this.noticeScale.getCurrentValue();
            float nh = (float) font.getHeight(true, ns);
            int active = getActiveNoticeCount();
            float maxNw = 0.0F;
            for (Notice n : this.notices) {
                if (!n.isFinished(now)) {
                    float nw = computeNoticeVisualWidth(n.msg, font, ns);
                    if (nw > maxNw) maxNw = nw;
                }
            }
            w = maxNw + (this.padding.getCurrentValue() + this.noticeInnerPadX.getCurrentValue()) * 2.0F;
            float spacingY = this.arrayVertical.getCurrentValue();
            float blockH = active > 0 ? (nh * active + spacingY * (active - 1)) : 0.0F;
            float motionRoom = nh * 1.0F;
            h = this.vPadding.getCurrentValue() * 2.0F + blockH + motionRoom;
        } else {
            if (this.autoWidth.getCurrentValue()) {
                w = Math.max(w, textWidth + this.padding.getCurrentValue() * 2.0F);
            }
            if (this.autoHeight.getCurrentValue()) {
                h = Math.max(h, textHeight + this.vPadding.getCurrentValue() * 2.0F);
            }
        }
        long transDur = Math.max(0L, this.activeTransDurMs);
        long elapsed = Math.max(0L, now - this.transitionStartMs);
        if (elapsed < transDur) {
            float t = Math.min(1.0f, elapsed / (float) transDur);
            float eased;
            if (this.lastOverride) {
                eased = easeOutExpo(t) + bounce(t);
            } else {
                eased = (easeInExpo(t) * 0.6f + easeCube(t) * 0.4f) + bounce(t);
            }
            float cw = this.prevTargetW + (w - this.prevTargetW) * eased;
            float ch = this.prevTargetH + (h - this.prevTargetH) * eased;
            w = Math.max(1.0F, cw);
            h = Math.max(1.0F, ch);
        } else if (!effectiveActive) {
            this.animW.target = w;
            this.animH.target = h;
            this.animW.update(true);
            this.animH.update(true);
            w = Math.max(1.0F, this.animW.value);
            h = Math.max(1.0F, this.animH.value);
        }
        this.lastBoxW = w;
        this.lastBoxH = h;
        this.hasLastBox = true;
        return new float[]{w, h};
    }
    private int getActiveNoticeCount() {
        long now = System.currentTimeMillis();
        int count = 0;
        for (Notice n : this.notices) {
            if (!n.isFinished(now)) count++;
        }
        return count;
    }
    private boolean noticesActive() {
        if (!this.noticesEnabled.getCurrentValue()) return false;
        try {
            HUD hud = (HUD) Naven.getInstance().getModuleManager().getModule(HUD.class);
            if (hud != null) {
                return hud.notification.getCurrentValue();
            }
        } catch (Throwable ignored) {
        }
        return true;
    }
    private void renderNotices(EventRender2D e, CustomTextRenderer font, float left, float top, float w, float h) {
        if (this.notices.isEmpty()) return;
        long now = System.currentTimeMillis();
        double ns = this.noticeScale.getCurrentValue();
        float nh = (float) font.getHeight(true, ns);
        float curY = top + this.vPadding.getCurrentValue() + nh * 0.25F;
        float spacing = this.arrayVertical.getCurrentValue();
        Iterator<Notice> it = this.notices.iterator();
        int idx = 0;
        while (it.hasNext()) {
            Notice n = it.next();
            if (n.isFinished(now)) {
                it.remove();
                continue;
            }
            if (!n.positionInitialized) {
                n.posStartIdx = idx;
                n.targetIdx = idx;
                n.posStartMs = now;
                n.positionInitialized = true;
            } else if (n.targetIdx != idx) {
                double curIdxInterp = n.interpIndex(now, POS_DUR_MS);
                n.posStartIdx = (float) curIdxInterp;
                n.targetIdx = idx;
                n.posStartMs = now;
            }
            double alpha = n.alpha(now);
            double offset = n.offset(now, nh * 0.9F);
            double idxInterp = n.interpIndex(now, POS_DUR_MS);
            float rowShift = (float) ((idxInterp - idx) * (nh + spacing));
            float ny = curY + (float) offset + rowShift;
            String msg = n.msg;
            String moduleName = null;
            String status = null;
            if (msg.endsWith(" Enabled!")) {
                moduleName = msg.substring(0, msg.length() - " Enabled!".length());
                status = "Enabled";
            } else if (msg.endsWith(" Disabled!")) {
                moduleName = msg.substring(0, msg.length() - " Disabled!".length());
                status = "Disabled";
            }
            float lineWidth = computeNoticeVisualWidth(msg, font, ns);
            float nx = left + (w - lineWidth) / 2.0F;
            long tLocal = now - n.start;
            double enter = n.enterMs;
            double hold = n.holdMs;
            double exit = n.exitMs;
            double wText;
            if (tLocal <= enter) {
                double p = Math.max(0.0, Math.min(1.0, tLocal / enter));
                wText = Math.max(0.0, Math.min(1.0, easeOutExpo((float) p)));
            } else if (tLocal <= enter + hold) {
                wText = 1.0;
            } else {
                double p = Math.max(0.0, Math.min(1.0, (tLocal - enter - hold) / exit));
                wText = Math.max(0.0, Math.min(1.0, easeCube((float) (1.0 - p))));
            }
            int aText = (int) Math.max(0, Math.min(255, Math.round(alpha * wText * 255.0)));
            if (status == null) {
                font.render(e.getStack(), msg, nx, ny, new Color(255, 255, 255, aText), true, ns);
            } else {
                float gap = 6.0F;
                float modW = font.getWidth(moduleName, ns);
                font.render(e.getStack(), moduleName, nx, ny, new Color(255, 255, 255, aText), true, ns);
                String pillText = status;
                float pillPadX = 6.0F;
                float pillTextW = font.getWidth(pillText, ns);
                float pillW = pillTextW + pillPadX * 2.0F;
                float pillH = nh;
                float pillX = nx + modW + gap;
                float pillY = ny;
                double delay = 80.0;
                double tPill = tLocal - delay;
                double wPill;
                if (tPill <= 0) {
                    wPill = 0.0;
                } else if (tPill <= enter) {
                    double p = Math.max(0.0, Math.min(1.0, tPill / enter));
                    wPill = Math.max(0.0, Math.min(1.0, easeOutExpo((float) p)));
                } else if (tPill <= enter + hold) {
                    wPill = 1.0;
                } else {
                    double p = Math.max(0.0, Math.min(1.0, (tPill - enter - hold) / exit));
                    wPill = Math.max(0.0, Math.min(1.0, easeCube((float) (1.0 - p))));
                }
                int aPill = (int) Math.max(0, Math.min(255, Math.round(alpha * wPill * 255.0)));
                float scalePill = 0.94F + (float) (0.06 * wPill);
                int baseR = status.equals("Enabled") ? 76 : 244;
                int baseG = status.equals("Enabled") ? 175 : 67;
                int baseB = status.equals("Enabled") ? 80 : 54;
                if (aPill > 0) {
                    float cx = pillX + pillW / 2.0F;
                    float cy = pillY + pillH / 2.0F;
                    float pw = pillW * scalePill;
                    float ph = pillH * scalePill;
                    float px = cx - pw / 2.0F;
                    float py = cy - ph / 2.0F;
                    Color pillColor = new Color(baseR, baseG, baseB, aPill);
                    RenderUtils.drawRoundedRect(e.getStack(), px, py, pw, ph, ph / 2.0F, pillColor.getRGB());
                    float padScaled = pillPadX * scalePill;
                    font.render(e.getStack(), pillText, px + padScaled, ny + (pillH - ph) / 2.0F, new Color(255, 255, 255, aPill), true, ns);
                }
            }
            curY += nh + spacing;
            idx++;
        }
    }
    private float computeNoticeVisualWidth(String msg, CustomTextRenderer font, double ns) {
        if (msg == null) return 0.0F;
        if (msg.endsWith(" Enabled!")) {
            String moduleName = msg.substring(0, msg.length() - " Enabled!".length());
            return widthWithPill(moduleName, "Enabled", font, ns);
        } else if (msg.endsWith(" Disabled!")) {
            String moduleName = msg.substring(0, msg.length() - " Disabled!".length());
            return widthWithPill(moduleName, "Disabled", font, ns);
        }
        return font.getWidth(msg, ns);
    }
    private float widthWithPill(String moduleName, String status, CustomTextRenderer font, double ns) {
        float gap = 6.0F;
        float modW = font.getWidth(moduleName, ns);
        float pillPadX = 6.0F;
        float pillTextW = font.getWidth(status, ns);
        float pillW = pillTextW + pillPadX * 2.0F;
        return modW + gap + pillW;
    }
    private float tyBase(float top, float h, float noticeH, CustomTextRenderer font) {
        double scale = this.textScale.getCurrentValue();
        float textHeight = (float) font.getHeight(true, scale);
        float titleY = top + (h - textHeight) / 2.0F;
        return titleY + textHeight + this.noticeSpacing.getCurrentValue();
    }
    private static class Notice {
        final String msg;
        final long start;
        final long enterMs;
        final long holdMs;
        final long exitMs;
        boolean positionInitialized = false;
        float posStartIdx = 0f;
        float targetIdx = 0f;
        long posStartMs = 0L;
        Notice(String msg, long start, long enterMs, long holdMs, long exitMs) {
            this.msg = msg;
            this.start = start;
            this.enterMs = enterMs;
            this.holdMs = holdMs;
            this.exitMs = exitMs;
        }
        boolean isFinished(long now) {
            return now - start >= (enterMs + holdMs + exitMs);
        }
        double alpha(long now) {
            long t = now - start;
            if (t <= enterMs) {
                double p = (double) t / (double) enterMs;
                double base = new EaseOutExpo().apply(p);
                double bounce = bounceSmall(p) * 0.08;
                double v = base + bounce;
                return Math.max(0.0, Math.min(1.0, v));
            }
            if (t <= enterMs + holdMs) return 1.0;
            double p = (double) (t - enterMs - holdMs) / (double) exitMs;
            double base = new EaseCube().apply(1.0 - p);
            double bounce = bounceSmall(1.0 - p) * 0.06;
            double v = base + bounce;
            return Math.max(0.0, Math.min(1.0, v));
        }
        double offset(long now, float lineH) {
            long t = now - start;
            if (t <= enterMs) {
                double p = (double) t / (double) enterMs;
                double base = -lineH * (1.0 - new EaseOutExpo().apply(p));
                double j = lineH * 0.06 * bounceSmall(p);
                return base + j;
            }
            if (t <= enterMs + holdMs) return 0.0;
            double p = (double) (t - enterMs - holdMs) / (double) exitMs;
            double base = -lineH * new EaseCube().apply(p);
            double j = -lineH * 0.04 * bounceSmall(p);
            return base + j;
        }
        double interpIndex(long now, int durMs) {
            if (!positionInitialized) return targetIdx;
            long dt = Math.max(0L, now - posStartMs);
            double p = durMs <= 0 ? 1.0 : Math.min(1.0, (double) dt / (double) durMs);
            double eased = new EaseOutExpo().apply(p);
            return posStartIdx + (targetIdx - posStartIdx) * eased;
        }
        private static double bounceSmall(double x) {
            x = Math.max(0.0, Math.min(1.0, x));
            return Math.sin(x * Math.PI * 2.0) * Math.pow(1.0 - x, 2.0);
        }
    }
}