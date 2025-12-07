package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.FontSelect;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "naven", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LoadFontOnStart {

    public static void loadUserSelectedFont() {
        try {
            // 获取Naven实例
            Naven naven = Naven.getInstance();
            if (naven != null) {
                // 获取FontSelect模块
                FontSelect fontSelectModule = (FontSelect) naven.getModuleManager().getModule(FontSelect.class);
                if (fontSelectModule != null && fontSelectModule.isEnabled()) {
                    // 如果模块启用，则更新字体
                    fontSelectModule.updateFont();
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading user selected font on startup");
            e.printStackTrace();
        }
    }
}