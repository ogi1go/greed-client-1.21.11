package greed.client.render;

import greed.client.render.FontFace;
import greed.client.util.FormattedText;
import greed.client.render.Render2D;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class FontText {
   public static void processData(
      FontFace fontFace,
      String text,
      float x,
      float y,
      float size,
      int color,
      float rotationDegrees,
      float rotationOriginX,
      float rotationOriginY
   ) {
      Render2D.drawTextRotated(
         fontFace.getResourceName(), text, x, y, size, color, rotationDegrees, rotationOriginX, rotationOriginY
      );
   }

   private FontText() {
   }

   public static void processData2(
      FontFace fontFace,
      String text,
      float x,
      float y,
      float size,
      int color,
      int shadowColor,
      float shadowOffset
   ) {
      Render2D.drawText(fontFace.getResourceName(), text, x + shadowOffset, y + shadowOffset, size, shadowColor);
      Render2D.drawText(fontFace.getResourceName(), text, x, y, size, color);
   }

   public static float computeFloat(FontFace fontFace, String text, float size) {
      return Render2D.measureText(fontFace.getResourceName(), text, size);
   }

   public static void processData3(FontFace fontFace, String text, float x, float y, float size, int color) {
      Render2D.drawText(fontFace.getResourceName(), text, x, y, size, color);
   }

   public static void processData4(FontFace fontFace, String text, float x, float y, float size, int color) {
      float shadowScale = Math.max(0.5F, size / 16.0F);
      float shadowOffset = Math.round(shadowScale * 2.0F) / 2.0F;
      int shadowColor = computeDimmedColor(color);
      processData2(fontFace, text, x, y, size, color, shadowColor, shadowOffset);
   }

   private static int computeDimmedColor(int color) {
      int alpha = color >>> 24 & 0xFF;
      int dimmedAlpha = Math.round(alpha * 0.55F);
      return dimmedAlpha << 24;
   }

   public static void processData5(FontFace fontFace, Text newText, float x, float y, float size, int color) {
      StringBuilder stringBuilder = new StringBuilder();

      for (FormattedText.DataRecord dataRecord : FormattedText.getArrayList2(newText, color)) {
         if (dataRecord.obfuscated()) {
            Render2D.drawObfuscatedText(
               fontFace.getResourceName(),
               dataRecord.text(),
               x + computeFloat(fontFace, stringBuilder.toString(), size),
               y,
               size,
               dataRecord.color() & 16777215 | color & 0xFF000000
            );
         } else {
            processData3(
               fontFace,
               dataRecord.text(),
               x + computeFloat(fontFace, stringBuilder.toString(), size),
               y,
               size,
               dataRecord.color() & 16777215 | color & 0xFF000000
            );
         }

         stringBuilder.append(dataRecord.text());
      }
   }

   public static void renderElement(
      DrawContext drawContext, FontFace fontFace, String text, float x, float y, float size, int color
   ) {
      Render2D.flush(drawContext);
      processData3(fontFace, text, x, y, size, color);
      Render2D.endFrame();
   }

   public static void processData6(
      FontFace fontFace,
      String text,
      float x,
      float y,
      float size,
      int color,
      float fadeLeftX,
      float fadeRightX,
      float fadeWidth,
      float fadeLeftStrength,
      float fadeRightStrength
   ) {
      Render2D.drawTextFadedFormatted(
         fontFace.getResourceName(),
         text,
         x,
         y,
         size,
         color,
         fadeLeftX,
         fadeRightX,
         fadeWidth,
         fadeLeftStrength,
         fadeRightStrength
      );
   }

   public static void processData7(
      FontFace fontFace,
      String text,
      float x,
      float y,
      float size,
      int color,
      float fadeLeftX,
      float fadeRightX,
      float fadeWidth,
      boolean fadeLeftEnabled,
      boolean fadeRightEnabled
   ) {
      Render2D.drawTextFadedSides(
         fontFace.getResourceName(),
         text,
         x,
         y,
         size,
         color,
         fadeLeftX,
         fadeRightX,
         fadeWidth,
         fadeLeftEnabled,
         fadeRightEnabled
      );
   }

   public static void processData8(
      FontFace fontFace,
      String text,
      float x,
      float y,
      float size,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft
   ) {
      Render2D.drawGradientText(
         fontFace.getResourceName(), text, x, y, size, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft
      );
   }

   public static void processData9(FontFace fontFace, String text, float x, float y, float size, int color) {
      float textWidth = computeFloat(fontFace, text, size);
      processData3(fontFace, text, x - textWidth * 0.5F, y, size, color);
   }
}
