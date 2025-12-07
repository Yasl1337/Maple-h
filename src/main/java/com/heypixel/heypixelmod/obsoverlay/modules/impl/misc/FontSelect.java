package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.FontLoader;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;

@ModuleInfo(
        name = "FontSelect",
        description = "Select custom font for UI rendering",
        category = Category.MISC
)
public class FontSelect extends Module {


    private final ModeValue fontOption = ValueBuilder.create(this, "Font")
            .setModes(FontLoader.getAvailableFonts())
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    public FontSelect() {
        super("FontSelect", "Select custom font for UI rendering", Category.MISC);

        if (this.isEnabled()) {
            updateFont();
        }
    }
    @Override
    public void onEnable() {
        updateFont();
    }
    @Override
    public void onDisable() {
        try {
            Fonts.reloadFonts("opensans");
        } catch (Exception e) {
            System.err.println("Error resetting fonts to default");
            e.printStackTrace();
        }
    }
    public void onValueChange() {
        if (this.isEnabled()) {
            updateFont();
        }
    }


    public void updateFont() {
        try {
            if (fontOption != null) {
                String selectedFont = fontOption.getCurrentMode();
                if (selectedFont != null && !selectedFont.isEmpty()) {

                    if (isChineseFont(selectedFont)) {
                        Fonts.reloadFonts(selectedFont);
                        if (Fonts.harmony != null) {
                            Fonts.harmony = new com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer(
                                    selectedFont, 32, 0, 65535, 16384);
                        }
                    } else {
                        Fonts.reloadFonts(selectedFont);
                    }
                } else {
                    Fonts.reloadFonts("opensans");
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating font");
            e.printStackTrace();

            try {
                Fonts.reloadFonts("opensans");
            } catch (Exception ex) {
                System.err.println("Error resetting to default font");
                ex.printStackTrace();
            }
        }
    }

    private boolean isChineseFont(String fontName) {

        return "HYWenHei 85W".equals(fontName) || "harmony".equals(fontName) || fontName.contains("中") || fontName.contains("汉");
    }
    public String getSelectedFont() {
        if (fontOption != null) {
            return fontOption.getCurrentMode();
        }
        return "opensans";
    }
}