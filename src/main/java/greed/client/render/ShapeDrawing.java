package greed.client.render;

import greed.client.util.GuiScale;
import greed.client.ui.hud.HudRenderContext;

import greed.client.render.EasingTexture;

import greed.client.util.Easings;
import greed.client.render.RoundedShapeRenderer;
import greed.client.render.PieShapeRenderer;
import greed.client.render.TexturedShapeRenderer;
import greed.client.render.GlassShapeRenderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

public final class ShapeDrawing {
   public static void processData(
      ShapeDrawing.TypeOption typeOption,
      float centerX,
      float centerY,
      float innerRadius,
      float radius,
      float startAngle,
      float sweepAngle,
      float thickness,
      float cornerRadius,
      int color,
      float tintMix,
      float alpha
   ) {
      TextureSetup textureSetup = ShapeBlurManager.INSTANCE.getTextureSetup2(typeOption.ordinal(), false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      if (textureSetup == null) {
         PieShapeRenderer.renderElement(
            getDrawContext(),
            null,
            getMatrix3x2f(),
            centerX,
            centerY,
            innerRadius,
            radius,
            startAngle,
            sweepAngle,
            thickness,
            cornerRadius,
            color,
            alpha * tintMix,
            1.0F
         );
      } else {
         PieShapeRenderer.renderElement(
            getDrawContext(),
            textureSetup,
            getMatrix3x2f(),
            centerX,
            centerY,
            innerRadius,
            radius,
            startAngle,
            sweepAngle,
            thickness,
            cornerRadius,
            color,
            alpha,
            tintMix
         );
      }
   }

   public static int computeInt(int color, float alpha) {
      int scaledAlpha = Math.round((color >>> 24 & 0xFF) * MathHelper.clamp(alpha, 0.0F, 1.0F));
      return scaledAlpha << 24 | color & 16777215;
   }

   private static void renderRoundedShape(
      Matrix3x2f matrix3x2f,
      float x,
      float y,
      float width,
      float height,
      int color,
      float lineWidth,
      float alpha,
      CornerRadii cornerRadii,
      ShapeDrawing.TypeOption typeOption,
      boolean useCache
   ) {
      TextureSetup textureSetup = getTextureSetup(matrix3x2f, x, y, width, height, 0.0F, cornerRadii, typeOption, useCache);
      if (textureSetup != null) {
         RoundedShapeRenderer.renderElement(
            getDrawContext(),
            textureSetup,
            matrix3x2f,
            x,
            y,
            width,
            height,
            0.0F,
            0.0F,
            1.0F,
            1.0F,
            cornerRadii,
            lineWidth,
            -0.5F,
            0.5F,
            alpha,
            color,
            color,
            3,
            useCache
         );
      }
   }

   private static DrawContext getDrawContext() {
      return HudRenderContext.getDrawContext();
   }

   public static void processData3(
      ShapeDrawing.TypeOption typeOption,
      float centerX,
      float centerY,
      float radius,
      float startAngle,
      float sweepAngle,
      float thickness,
      float cornerRadius,
      int color,
      float tintMix,
      float alpha
   ) {
      TextureSetup textureSetup = ShapeBlurManager.INSTANCE.getTextureSetup2(typeOption.ordinal(), false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      if (textureSetup == null) {
         PieShapeRenderer.renderElement2(
            getDrawContext(),
            null,
            getMatrix3x2f(),
            centerX,
            centerY,
            radius,
            startAngle,
            sweepAngle,
            thickness,
            cornerRadius,
            color,
            alpha * tintMix,
            1.0F
         );
      } else {
         PieShapeRenderer.renderElement2(
            getDrawContext(),
            textureSetup,
            getMatrix3x2f(),
            centerX,
            centerY,
            radius,
            startAngle,
            sweepAngle,
            thickness,
            cornerRadius,
            color,
            alpha,
            tintMix
         );
      }
   }

   private ShapeDrawing() {
   }

   public static void processData4(
      float x,
      float y,
      float width,
      float height,
      float alpha,
      CornerRadii cornerRadii,
      ShapeDrawing.TypeOption typeOption,
      boolean useCache
   ) {
      renderRoundedShape(getMatrix3x2f(), x, y, width, height, -1, 0.0F, alpha, cornerRadii, typeOption, useCache);
   }

   public static void processData5(
      float x,
      float y,
      float width,
      float height,
      float spread,
      int color,
      float alpha,
      CornerRadii cornerRadii
   ) {
      TexturedShapeRenderer.renderElement(
         getDrawContext(),
         getMatrix3x2f(),
         x - spread,
         y - spread,
         width + spread * 2.0F,
         height + spread * 2.0F,
         cornerRadii,
         spread,
         0.0F,
         alpha,
         color,
         color,
         color,
         color,
         1
      );
   }

   public static void processData6(
      float x,
      float y,
      float width,
      float height,
      int colorBottomRight,
      int colorTopRight,
      int colorBottomLeft,
      int colorTopLeft,
      float alpha,
      CornerRadii cornerRadii
   ) {
      TexturedShapeRenderer.renderElement(
         getDrawContext(),
         getMatrix3x2f(),
         x,
         y,
         width,
         height,
         cornerRadii,
         0.0F,
         0.0F,
         alpha,
         colorTopLeft,
         colorBottomLeft,
         colorBottomRight,
         colorTopRight,
         0
      );
   }

   private static float roundToHalf(float value) {
      return Math.round(value * 2.0F) / 2.0F;
   }

   private static Matrix3x2f getMatrix3x2f() {
      DrawContext drawContext = getDrawContext();
      return drawContext == null ? new Matrix3x2f() : new Matrix3x2f(GuiScale.renderElement(drawContext));
   }

   public static short getShort(float value, boolean relative) {
      int scaledValue = MathHelper.clamp(Math.round(Math.max(0.0F, value) * 16.0F), 0, 32767);
      return (short)(relative ? -scaledValue : scaledValue);
   }

   public static void processData7(
      ShapeDrawing.TypeOption typeOption,
      boolean useCache,
      float x,
      float y,
      float width,
      float height,
      float alpha,
      CornerRadii cornerRadii,
      int color,
      float tintAmount,
      int shadowColor
   ) {
      x = roundToHalf(x);
      y = roundToHalf(y);
      width = roundToHalf(width);
      height = roundToHalf(height);
      float glowRadius = cornerRadii.maxRadius() * 0.65F;
      float glowAlpha = alpha * alpha * 0.8F;
      Matrix3x2f matrix3x2f = getMatrix3x2f();
      if (!(glowRadius <= 0.0F) && !(glowAlpha <= 5.0E-4F)) {
         TextureSetup textureSetup = getTextureSetup(matrix3x2f, x, y, width, height, 0.0F, cornerRadii, typeOption, useCache);
         TextureSetup textureSetup2 = EasingTexture.getTexture(Easings.EASE_IN_QUAD);
         if (textureSetup != null && textureSetup.texure0() != null && textureSetup2 != null) {
            TextureSetup textureSetup3 = TextureSetup.of(textureSetup.texure0(), textureSetup.sampler0(), textureSetup2.texure0(), textureSetup2.sampler0());
            float shadowAlpha = (shadowColor >>> 24 & 0xFF) / 255.0F * glowAlpha;
            GlassShapeRenderer.renderElement(
               getDrawContext(),
               textureSetup3,
               matrix3x2f,
               x - glowRadius,
               y - glowRadius,
               width + glowRadius * 2.0F,
               height + glowRadius * 2.0F,
               cornerRadii,
               glowRadius,
               color,
               alpha,
               tintAmount,
               shadowColor,
               shadowAlpha,
               useCache
            );
         }
      } else {
         renderRoundedShape(
            matrix3x2f, x, y, width, height, color, tintAmount, alpha, cornerRadii, typeOption, useCache
         );
      }
   }

   private static TextureSetup getTextureSetup(
      Matrix3x2f matrix3x2f,
      float x,
      float y,
      float width,
      float height,
      float blurExpand,
      CornerRadii cornerRadii,
      ShapeDrawing.TypeOption typeOption,
      boolean useCache
   ) {
      float rectMinX = 0.0F;
      float rectMinY = 0.0F;
      float rectMaxX = 0.0F;
      float rectMaxY = 0.0F;
      float rectExtra = 0.0F;
      if (useCache) {
         float expandedX = x - blurExpand;
         float expandedY = y - blurExpand;
         float expandedWidth = width + blurExpand * 2.0F;
         float expandedHeight = height + blurExpand * 2.0F;
         Vector2f vector2f = matrix3x2f.transformPosition(expandedX, expandedY, new Vector2f());
         Vector2f vector2f2 = matrix3x2f.transformPosition(expandedX + expandedWidth, expandedY + expandedHeight, new Vector2f());
         float minX = Math.min(vector2f.x, vector2f2.x);
         float minY = Math.min(vector2f.y, vector2f2.y);
         float maxX = Math.max(vector2f.x, vector2f2.x);
         float maxY = Math.max(vector2f.y, vector2f2.y);
         Window window = MinecraftClient.getInstance().getWindow();
         float scaleFactor = window.getScaleFactor();
         float scaledWidth = scaleFactor <= 0.0F ? window.getScaledWidth() : window.getFramebufferWidth() / scaleFactor;
         float scaledHeight = scaleFactor <= 0.0F ? window.getScaledHeight() : window.getFramebufferHeight() / scaleFactor;
         rectMinX = minX / scaledWidth;
         rectMaxX = maxX / scaledWidth;
         rectMinY = 1.0F - maxY / scaledHeight;
         rectMaxY = 1.0F - minY / scaledHeight;
         rectExtra = cornerRadii.maxRadius() * scaleFactor * matrix3x2f.m00();
      }

      return ShapeBlurManager.INSTANCE.getTextureSetup2(typeOption.ordinal(), useCache, rectMinX, rectMinY, rectMaxX, rectMaxY, rectExtra);
   }

   public static ScreenRect getScreenRect(Matrix3x2f matrix3x2f, ScreenRect screenRect, float x, float y, float width, float height) {
      ScreenRect screenRect2 = new ScreenRect(
            Math.round(x), Math.round(y), Math.max(1, Math.round(width)), Math.max(1, Math.round(height))
         )
         .transformEachVertex(matrix3x2f);
      return screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
   }

   public static void processData8(
      float centerX,
      float centerY,
      float radius,
      float startAngle,
      float sweepAngle,
      float thickness,
      float cornerRadius,
      int color,
      float alpha
   ) {
      PieShapeRenderer.renderElement2(
         getDrawContext(),
         null,
         getMatrix3x2f(),
         centerX,
         centerY,
         radius,
         startAngle,
         sweepAngle,
         thickness,
         cornerRadius,
         color,
         alpha,
         1.0F
      );
   }

   public static void processData9(
      float x, float y, float width, float height, int color, float alpha, CornerRadii cornerRadii
   ) {
      TexturedShapeRenderer.renderElement(
         getDrawContext(),
         getMatrix3x2f(),
         x,
         y,
         width,
         height,
         cornerRadii,
         0.0F,
         0.0F,
         alpha,
         color,
         color,
         color,
         color,
         0
      );
   }

   public static void processData10(
      float x,
      float y,
      float width,
      float height,
      float lineWidth,
      int color,
      float alpha,
      CornerRadii cornerRadii
   ) {
      TexturedShapeRenderer.renderElement(
         getDrawContext(),
         getMatrix3x2f(),
         x,
         y,
         width,
         height,
         cornerRadii,
         0.0F,
         lineWidth,
         alpha,
         color,
         color,
         color,
         color,
         4
      );
   }

   public enum TypeOption {
      LOW,
      MIDDLE,
      MAX;
   }
}
