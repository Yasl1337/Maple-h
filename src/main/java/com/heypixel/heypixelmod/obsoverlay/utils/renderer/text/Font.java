package com.heypixel.heypixelmod.obsoverlay.utils.renderer.text;

import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Mesh;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTTPackedchar.Buffer;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

public class Font {
   public AbstractTexture texture;
   private final int height;
   private final float scale;
   private final float ascent;
   private final CharData[] charData;
   private final int from;
   private final HashMap<String, Double> widthCache = new HashMap<>();

   private Font fallbackFont;

   public Font(ByteBuffer buffer, int height, int charRangeFrom, int charRangeTo, int textureSize) {
      this.height = height;
      this.from = charRangeFrom;
      STBTTFontinfo fontInfo = STBTTFontinfo.create();
      STBTruetype.stbtt_InitFont(fontInfo, buffer);
      this.charData = new CharData[charRangeTo + 1 - charRangeFrom];
      Buffer cdata = STBTTPackedchar.create(this.charData.length);
      ByteBuffer bitmap = BufferUtils.createByteBuffer(textureSize * textureSize);
      STBTTPackContext packContext = STBTTPackContext.create();
      STBTruetype.stbtt_PackBegin(packContext, bitmap, textureSize, textureSize, 0, 1);
      STBTruetype.stbtt_PackSetOversampling(packContext, 2, 2);
      STBTruetype.stbtt_PackFontRange(packContext, buffer, 0, (float)height, this.from, cdata);
      STBTruetype.stbtt_PackEnd(packContext);
      this.texture = new BufferedTexture(
         textureSize, textureSize, bitmap, BufferedTexture.Format.A, BufferedTexture.Filter.Linear, BufferedTexture.Filter.Linear
      );
      this.scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, (float)height);
      MemoryStack stack = MemoryStack.stackPush();

      try {
         IntBuffer ascent = stack.mallocInt(1);
         STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, null, null);
         this.ascent = (float)ascent.get(0);
      } catch (Throwable var15) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var14) {
               var15.addSuppressed(var14);
            }
         }

         throw var15;
      }

      if (stack != null) {
         stack.close();
      }

      for (int i = 0; i < this.charData.length; i++) {
         STBTTPackedchar packedChar = (STBTTPackedchar)cdata.get(i);
         float ipw = 1.0F / (float)textureSize;
         float iph = 1.0F / (float)textureSize;
         this.charData[i] = new CharData(
            packedChar.xoff(),
            packedChar.yoff(),
            packedChar.xoff2(),
            packedChar.yoff2(),
            (float)packedChar.x0() * ipw,
            (float)packedChar.y0() * iph,
            (float)packedChar.x1() * ipw,
            (float)packedChar.y1() * iph,
            packedChar.xadvance()
         );
      }
   }

   public double getWidth(String string) {
      if (this.widthCache.containsKey(string)) {
         return this.widthCache.get(string);
      } else {
         double width = 0.0;


         StringBuilder strippedString = new StringBuilder();
         for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == '\u00A7' && i + 1 < string.length()) {

               i++;
            } else {
               strippedString.append(c);
            }
         }
         String processedString = strippedString.toString();

         for (int i = 0; i < processedString.length(); i++) {
            int cp = processedString.charAt(i) - this.from;
            if (cp >= this.charData.length || cp < 0) {

               if (this.fallbackFont != null) {
                  width += this.fallbackFont.getWidth(String.valueOf(processedString.charAt(i)));
               } else {

                  cp = 0;
               }
            }
            

            if (cp >= 0 && cp < this.charData.length) {
               CharData c = this.charData[cp];
               width += (double)c.xAdvance;
            }
         }

         this.widthCache.put(string, width);
         return width;
      }
   }

   public double getHeight() {
      return (double)this.height;
   }

   public double render(Mesh mesh, String string, double x, double y, Color color, double scale, boolean shadow) {
      Color currentColor = color;
      y += (double)(this.ascent * this.scale) * scale;
      
      for (int i = 0; i < string.length(); i++) {
         char ch = string.charAt(i);
         

         if (ch == '\u00A7' && i + 1 < string.length()) {
            char ctrl = string.charAt(i + 1);
            ChatFormatting byCode = ChatFormatting.getByCode(ctrl);
            if (byCode != null && byCode.isColor()) {
               currentColor = new Color(byCode.getColor());
            } else if (byCode == ChatFormatting.RESET) {
               currentColor = color;
            }
            
            i++;
            continue;
         }

         int cp = ch - this.from;
         boolean renderedWithFallback = false;
         if (cp >= this.charData.length || cp < 0) {

            if (this.fallbackFont != null) {

               x = this.fallbackFont.render(mesh, String.valueOf(ch), x, y - (double)(this.ascent * this.scale) * scale, currentColor, scale, shadow);
               renderedWithFallback = true;
            } else {

               cp = 0;
            }
         }

         if (!renderedWithFallback && cp >= 0 && cp < this.charData.length) {
            CharData c = this.charData[cp];
            mesh.quad(
               mesh.vec2(x + (double)c.x0 * scale, y + (double)c.y0 * scale).vec2((double)c.u0, (double)c.v0).color(currentColor).next(),
               mesh.vec2(x + (double)c.x0 * scale, y + (double)c.y1 * scale).vec2((double)c.u0, (double)c.v1).color(currentColor).next(),
               mesh.vec2(x + (double)c.x1 * scale, y + (double)c.y1 * scale).vec2((double)c.u1, (double)c.v1).color(currentColor).next(),
               mesh.vec2(x + (double)c.x1 * scale, y + (double)c.y0 * scale).vec2((double)c.u1, (double)c.v0).color(currentColor).next()
            );
            x += (double)c.xAdvance * scale;
         }
      }

      return x;
   }
   

   public void setFallbackFont(Font fallbackFont) {
      this.fallbackFont = fallbackFont;
   }
}