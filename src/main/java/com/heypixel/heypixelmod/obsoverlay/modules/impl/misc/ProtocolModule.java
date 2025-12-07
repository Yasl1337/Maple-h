package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;

@ModuleInfo(
        name = "Protocol",
        description = "Enable/Disable all features under protocol package",
        category = Category.MISC
)
public class ProtocolModule extends Module {

    // Sub-switches
    private final BooleanValue sessionSending = ValueBuilder.create(this, "Session Sending").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue hardwareSpoof = ValueBuilder.create(this, "Hardware Spoof").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue cpuSpoof = ValueBuilder.create(this, "CPU Spoof").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue baseboardSpoof = ValueBuilder.create(this, "Baseboard Spoof").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue networkSpoof = ValueBuilder.create(this, "Network Spoof").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue diskSpoof = ValueBuilder.create(this, "Disk Spoof").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue emailSpoof = ValueBuilder.create(this, "Email Spoof").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue debugMode = ValueBuilder.create(this, "Debug Mode").setDefaultBooleanValue(false).build().getBooleanValue();

    @Override
    public void onEnable() {
        // optional: notify/debug
    }

    @Override
    public void onDisable() {
        // optional: notify/debug
    }

    // Static helpers for global access without holding reference
    private static ProtocolModule get() {
        try {
            return (ProtocolModule) Naven.getInstance().getModuleManager().getModule(ProtocolModule.class);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isProtocolEnabled() {
        ProtocolModule m = get();
        return m != null && m.isEnabled();
    }

    public static boolean isSessionSendingEnabled() {
        ProtocolModule m = get();
        return m != null && m.sessionSending.getCurrentValue();
    }

    public static boolean isHardwareSpoofEnabled() {
        ProtocolModule m = get();
        return m != null && m.hardwareSpoof.getCurrentValue();
    }

    public static boolean isCpuSpoofEnabled() {
        ProtocolModule m = get();
        return m != null && m.cpuSpoof.getCurrentValue();
    }

    public static boolean isBaseboardSpoofEnabled() {
        ProtocolModule m = get();
        return m != null && m.baseboardSpoof.getCurrentValue();
    }

    public static boolean isNetworkSpoofEnabled() {
        ProtocolModule m = get();
        return m != null && m.networkSpoof.getCurrentValue();
    }

    public static boolean isDiskSpoofEnabled() {
        ProtocolModule m = get();
        return m != null && m.diskSpoof.getCurrentValue();
    }

    public static boolean isEmailSpoofEnabled() {
        ProtocolModule m = get();
        return m != null && m.emailSpoof.getCurrentValue();
    }

    public static boolean isDebugEnabled() {
        ProtocolModule m = get();
        return m != null && m.debugMode.getCurrentValue();
    }
}
