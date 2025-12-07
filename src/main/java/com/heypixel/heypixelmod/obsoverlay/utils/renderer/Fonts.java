package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.FontSelect;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;

import java.awt.*;
import java.io.IOException;

public class Fonts {
    public static CustomTextRenderer opensans;
    public static CustomTextRenderer harmony;
    public static CustomTextRenderer icons;

    public static CustomTextRenderer chinese;
    public static CustomTextRenderer googleSansBold;

    public static void loadFonts() throws IOException, FontFormatException {

        try {
            opensans = new CustomTextRenderer("opensans", 32, 0, 255, 512);
        } catch (Exception e) {
            System.err.println("Failed to load opensans font, using default");
            e.printStackTrace();

            opensans = null;
        }

        try {
            harmony = new CustomTextRenderer("harmony", 32, 0, 65535, 16384);
        } catch (Exception e) {
            System.err.println("Failed to load harmony font, using default");
            e.printStackTrace();

            harmony = null;
        }

        try {
            icons = new CustomTextRenderer("icon", 32, 59648, 59652, 512);
        } catch (Exception e) {
            System.err.println("Failed to load icons font, using default");
            e.printStackTrace();

            icons = null;
        }


        try {
            chinese = new CustomTextRenderer("HYWenHei 85W", 32, 0x4E00, 0x9FFF, 16384);
        } catch (Exception e) {
            System.err.println("Failed to load HYWenHei font, using harmony as fallback");
            e.printStackTrace();
            chinese = harmony;
        }

        try {
            googleSansBold = new CustomTextRenderer("GoogleSans-Bold", 32, 0, 255, 512);
        } catch (Exception e) {
            System.err.println("Failed to load GoogleSans-Bold font, using opensans as fallback");
            e.printStackTrace();
            googleSansBold = opensans;
        }


        if (opensans == null) {
            System.err.println("opensans font is null, this should not happen");
        }

        if (harmony == null) {
            System.err.println("harmony font is null, this should not happen");
        }

        if (icons == null) {
            System.err.println("icons font is null, this should not happen");
        }


        if (chinese == null) {
            System.err.println("chinese font is null, falling back to harmony");
            chinese = harmony;
        }

        if (googleSansBold == null) {
            System.err.println("googleSansBold font is null, falling back to opensans");
            googleSansBold = opensans;
        }


        applyUserSelectedFont();
    }


    private static void applyUserSelectedFont() {
        try {


            FontSelect fontSelectModule = getFontSelectModule();
            if (fontSelectModule != null && fontSelectModule.isEnabled()) {
                String selectedFont = fontSelectModule.getSelectedFont();
                if (selectedFont != null && !selectedFont.isEmpty() && !"opensans".equals(selectedFont)) {

                    reloadFonts(selectedFont);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to apply user selected font on startup");
            e.printStackTrace();
        }
    }


    private static FontSelect getFontSelectModule() {
        try {


            return null;
        } catch (Exception e) {
            return null;
        }
    }


    public static void reloadFonts(String fontName) {
        try {

            if (fontName == null || fontName.isEmpty()) {
                fontName = "opensans";
            }

            CustomTextRenderer newOpensans = null;
            CustomTextRenderer newHarmony = null;

            try {
                newOpensans = new CustomTextRenderer(fontName, 32, 0, 255, 512);
            } catch (Exception e) {
                System.err.println("Failed to create opensans renderer with font: " + fontName);
                e.printStackTrace();

                newOpensans = new CustomTextRenderer("opensans", 32, 0, 255, 512);
            }

            try {

                newHarmony = new CustomTextRenderer(fontName, 32, 0, 65535, 16384);
            } catch (Exception e) {
                System.err.println("Failed to create harmony renderer with font: " + fontName);
                e.printStackTrace();

                newHarmony = new CustomTextRenderer("harmony", 32, 0, 65535, 16384);
            }


            if (newOpensans != null) {
                opensans = newOpensans;
            }

            if (newHarmony != null) {
                harmony = newHarmony;
            }


            try {
                if (icons == null) {
                    icons = new CustomTextRenderer("PublicSans-Bold", 32, 59648, 59652, 512);
                }
            } catch (Exception e) {
                System.err.println("Failed to load icons font");
                e.printStackTrace();

                if (icons == null) {

                    icons = new CustomTextRenderer("PublicSans-Bold", 32, 59648, 59652, 512);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to reload fonts with: " + fontName);
            e.printStackTrace();


            try {
                if (opensans == null) {
                    opensans = new CustomTextRenderer("opensans", 32, 0, 255, 512);
                }
                if (harmony == null) {
                    harmony = new CustomTextRenderer("harmony", 32, 0, 65535, 16384);
                }
                if (icons == null) {
                    icons = new CustomTextRenderer("PublicSans-Bold", 32, 59648, 59652, 512);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    public static void renderTextWithIcons(com.mojang.blaze3d.vertex.PoseStack stack, String text, double x, double y, Color color, boolean shadow, double scale) {
        if (icons != null) {
            icons.render(stack, text, x, y, color, shadow, scale);
        } else {

            if (opensans != null) {
                opensans.render(stack, text, x, y, color, shadow, scale);
            }
        }
    }


    public static void renderMixedFont(com.mojang.blaze3d.vertex.PoseStack stack, String text, double x, double y, Color color, boolean shadow, double scale) {
        double currentX = x;
        Color currentColor = color;
        boolean currentShadow = shadow;
        boolean bold = false;
        boolean italic = false;
        boolean underlined = false;
        boolean strikethrough = false;


        class FormatState {
            Color color;
            boolean shadow;
            boolean bold;
            boolean italic;
            boolean underlined;
            boolean strikethrough;

            FormatState(Color color, boolean shadow, boolean bold, boolean italic, boolean underlined, boolean strikethrough) {
                this.color = color;
                this.shadow = shadow;
                this.bold = bold;
                this.italic = italic;
                this.underlined = underlined;
                this.strikethrough = strikethrough;
            }
        }

        java.util.Stack<FormatState> formatStack = new java.util.Stack<>();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);


            if (c == '\u00A7' && i + 1 < text.length()) {
                char ctrl = text.charAt(i + 1);
                net.minecraft.ChatFormatting byCode = net.minecraft.ChatFormatting.getByCode(ctrl);

                if (byCode != null) {
                    switch (byCode) {
                        case BLACK:
                        case DARK_BLUE:
                        case DARK_GREEN:
                        case DARK_AQUA:
                        case DARK_RED:
                        case DARK_PURPLE:
                        case GOLD:
                        case GRAY:
                        case DARK_GRAY:
                        case BLUE:
                        case GREEN:
                        case AQUA:
                        case RED:
                        case LIGHT_PURPLE:
                        case YELLOW:
                        case WHITE:
                            currentColor = new Color(byCode.getColor());
                            break;

                        case OBFUSCATED:

                            break;

                        case BOLD:
                            bold = true;
                            break;

                        case ITALIC:
                            italic = true;
                            break;

                        case UNDERLINE:
                            underlined = true;
                            break;

                        case STRIKETHROUGH:
                            strikethrough = true;
                            break;

                        case RESET:
                            currentColor = color;
                            currentShadow = shadow;
                            bold = false;
                            italic = false;
                            underlined = false;
                            strikethrough = false;
                            break;
                    }
                }

                i++;
                continue;
            }


            CustomTextRenderer renderer = isCJKCharacter(c) ? chinese : opensans;

            if (renderer != null) {
                String charStr = String.valueOf(c);


                int formattedColor = applyFormatting(currentColor.getRGB(), bold, italic, underlined, strikethrough);


                renderer.render(stack, charStr, currentX, y, new Color(formattedColor), currentShadow, scale);
                currentX += renderer.getWidth(charStr, scale);
            }
        }
    }


    private static int applyFormatting(int color, boolean bold, boolean italic, boolean underlined, boolean strikethrough) {


        return color;
    }


    private static boolean isCJKCharacter(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS;
    }
}