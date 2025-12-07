package com.heypixel.heypixelmod.obsoverlay.utils.renderer.text;

import com.heypixel.heypixelmod.obsoverlay.files.FileManager;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class CustomTextRenderer {
   private static final Logger log = LogManager.getLogger(CustomTextRenderer.class);
   private static final Color SHADOW_COLOR = new Color(60, 60, 60, 180);
   private final Mesh mesh = new ShaderMesh(Shaders.TEXT, DrawMode.Triangles, Mesh.Attrib.Vec2, Mesh.Attrib.Vec2, Mesh.Attrib.Color);
   private final Font font;

   public CustomTextRenderer(String name, int size, int from, int to, int textureSize) {

      InputStream in = loadExternalFont(name);
      if (in == null) {
         in = this.getClass().getResourceAsStream("/assets/heypixel/VcX6svVqmeT8/fonts/" + name + ".ttf");
      }
      
      if (in == null) {
         throw new RuntimeException("Font not found: " + name);
      } else {
         byte[] bytes;
         try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];

            int len;
            while ((len = in.read(buffer)) != -1) {
               out.write(buffer, 0, len);
            }

            bytes = out.toByteArray();
         } catch (IOException var11) {
            throw new RuntimeException("Failed to read font: " + name, var11);
         }
         ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length).put(bytes);
         ((Buffer)buffer).flip();
         long startTime = System.currentTimeMillis();
         this.font = new Font(buffer, size, from, to, textureSize);

         try {
            setupFallbackFont(size, textureSize);
         } catch (Exception e) {
            log.warn("Failed to load fallback font HYWenHei 85W for font: " + name, e);
         }
         
         log.info("Loaded font {} in {}ms", name, System.currentTimeMillis() - startTime);
      }
   }

   
   private void setupFallbackFont(int size, int textureSize) {
      try {

         InputStream fallbackIn = loadExternalFont("HYWenHei 85W");
         

         if (fallbackIn == null) {
            fallbackIn = this.getClass().getResourceAsStream("/assets/heypixel/VcX6svVqmeT8/fonts/HYWenHei 85W.ttf");
         }
         
         if (fallbackIn != null) {
            byte[] fallbackBytes;
            try {
               ByteArrayOutputStream fallbackOut = new ByteArrayOutputStream();
               byte[] fallbackBuffer = new byte[1024];
               
               int fallbackLen;
               while ((fallbackLen = fallbackIn.read(fallbackBuffer)) != -1) {
                  fallbackOut.write(fallbackBuffer, 0, fallbackLen);
               }
               
               fallbackBytes = fallbackOut.toByteArray();
            } catch (IOException e) {
               throw new RuntimeException("Failed to read fallback font: HYWenHei 85W", e);
            }
            
            ByteBuffer fallbackBuffer = BufferUtils.createByteBuffer(fallbackBytes.length).put(fallbackBytes);
            ((Buffer)fallbackBuffer).flip();

            Font fallbackFont = new Font(fallbackBuffer, size, 0, 65535, Math.max(textureSize, 4096));
            this.font.setFallbackFont(fallbackFont);
         }
      } catch (Exception e) {
         log.warn("Failed to setup fallback font HYWenHei 85W", e);
      }
   }

   
   private InputStream loadExternalFont(String fontName) {
      try {
         File fontsDir = new File(FileManager.clientFolder, "fonts");
         if (fontsDir.exists() && fontsDir.isDirectory()) {

            String[] extensions = {".ttf", ".otf", ".ttc"};
            for (String extension : extensions) {
               File fontFile = new File(fontsDir, fontName + extension);
               if (fontFile.exists() && fontFile.isFile()) {
                 log.info("Loading external font: {}", fontFile.getAbsolutePath());
                 return new FileInputStream(fontFile);
               }
            }
         }
      } catch (Exception e) {
         log.warn("Failed to load external font: " + fontName, e);
      }
      return null;
   }

   public void setAlpha(float alpha) {
      this.mesh.alpha = (double)alpha;
   }

   public float getWidth(String text, double scale) {
      return (float)this.getWidth(text, false, scale);
   }

   public double getWidth(String text, boolean shadow, double scale) {
      // 移除颜色代码后计算宽度
      StringBuilder strippedText = new StringBuilder();
      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c == '\u00A7' && i + 1 < text.length()) {
            // 跳过颜色代码
            i++;
         } else {
            strippedText.append(c);
         }
      }
      return (this.font.getWidth(strippedText.toString()) + (double)(shadow ? 0.5F : 0.0F)) * scale;
   }

   public double getHeight(boolean shadow, double scale) {
      return (this.font.getHeight() + (double)(shadow ? 0.5F : 0.0F)) * scale;
   }

   public double render(PoseStack stack, String text, double x, double y, Color color, boolean shadow, double scale) {
      Color currentColor = color;
      double currentX = x;
      double totalWidth = 0;
      
      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         
         // 检查是否为颜色控制符
         if (c == '\u00A7' && i + 1 < text.length()) {
            char ctrl = text.charAt(i + 1);
            ChatFormatting byCode = ChatFormatting.getByCode(ctrl);
            if (byCode != null && byCode.isColor()) {
               currentColor = new Color(byCode.getColor());
            } else if (byCode == ChatFormatting.RESET) {
               currentColor = color;
            }
            
            i++; // 跳过颜色码字符
            continue;
         }
         
         if (isCJKCharacter(c) && com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts.chinese != null && com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts.chinese != this) {
            String charStr = String.valueOf(c);
            totalWidth += com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts.chinese.render(stack, charStr, currentX, y, currentColor, shadow, scale);
            currentX += com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts.chinese.getWidth(charStr, scale);
         } else {
            String charStr = String.valueOf(c);
            this.mesh.begin();
            double width;
            if (shadow) {
               width = this.font.render(this.mesh, charStr, currentX + 0.5, y + 0.5, SHADOW_COLOR, scale, true);
               this.font.render(this.mesh, charStr, currentX, y, currentColor, scale, false);
            } else {
               width = this.font.render(this.mesh, charStr, currentX, y, currentColor, scale, false);
            }
            this.mesh.end();
            GL.bindTexture(this.font.texture.getId());
            this.mesh.render(stack);
            totalWidth += width;
            currentX += this.font.getWidth(charStr) * scale;
         }
      }
      return totalWidth;
   }
   
   private static boolean isCJKCharacter(char c) {
      Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
      return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
              || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
              || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
              || block == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS;
   }
}