package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.files.FileManager;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FontLoader {
    private static final Map<String, CustomTextRenderer> loadedFonts = new HashMap<>();
    private static boolean initialized = false;
    private static String[] availableFonts = null;

    
    public static String[] getAvailableFonts() {
        return getAvailableFonts(false);
    }

    
    public static String[] getCJKFonts() {
        String[] allFonts = getAvailableFonts();
        List<String> cjkFonts = new ArrayList<>();
        
        for (String font : allFonts) {
            if (isCJKFont(font)) {
                cjkFonts.add(font);
            }
        }
        
        return cjkFonts.toArray(new String[0]);
    }

    
    private static boolean isCJKFont(String fontName) {
        return "HYWenHei 85W".equals(fontName) || 
               "harmony".equals(fontName) || 
               fontName.contains("中") || 
               fontName.contains("汉");
    }
    
    
    public static String[] getAvailableFonts(boolean refresh) {

        if (availableFonts == null || refresh) {

            String[] builtinFonts = new String[] {
                    "opensans", "Consolas", "GoogleSans-Bold", "GoogleSans-Regular",
                    "HYWenHei 85W", "Product", "Tahoma", "Tahomabold",
                    "Verdana", "harmony"
            };
            

            File fontsDir = new File(FileManager.clientFolder, "fonts");
            if (!fontsDir.exists()) {
                fontsDir.mkdirs();
            }

            List<String> externalFonts = new ArrayList<>();
            if (fontsDir.exists() && fontsDir.isDirectory()) {
                File[] files = fontsDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        String fileName = file.getName();
                        if (fileName.endsWith(".ttf") || fileName.endsWith(".otf") || fileName.endsWith(".ttc")) {
                            String fontName = fileName.substring(0, fileName.lastIndexOf('.'));
                            externalFonts.add(fontName);
                        }
                    }
                }
            }
            

            String[] allFonts = new String[builtinFonts.length + externalFonts.size()];
            System.arraycopy(builtinFonts, 0, allFonts, 0, builtinFonts.length);
            for (int i = 0; i < externalFonts.size(); i++) {
                allFonts[builtinFonts.length + i] = externalFonts.get(i);
            }
            
            availableFonts = allFonts;
        }
        return availableFonts;
    }

    
    public static void loadFonts() {

        if (initialized) {
            return;
        }

        try {

            String[] fontNames = getAvailableFonts();

            for (String fontName : fontNames) {
                try {


                    loadedFonts.put(fontName, null);
                } catch (Exception e) {
                    System.err.println("Failed to prepare font: " + fontName);
                    e.printStackTrace();
                }
            }

            initialized = true;
        } catch (Exception e) {
            System.err.println("Failed to initialize FontLoader");
            e.printStackTrace();
        }
    }

    
    public static CustomTextRenderer getFont(String fontName) {

        if (!initialized) {
            loadFonts();
        }
        return loadedFonts.get(fontName);
    }
}