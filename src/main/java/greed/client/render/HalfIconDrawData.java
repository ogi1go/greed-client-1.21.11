package greed.client.render;

import greed.client.render.RectHalfIconRenderer;

import net.minecraft.client.gui.DrawContext;

public record HalfIconDrawData(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   int colorTopLeft,
   int colorTopRight,
   int colorBottomRight,
   int colorBottomLeft,
   float smoothness,
   String fontName,
   String icon,
   float iconSize,
   int iconColor,
   float spacingX,
   float spacingY,
   float paddingX,
   float paddingY,
   float jitterX,
   float jitterY,
   float rotationMaxDegrees,
   long seed
) {
   public static final float DEFAULT_SMOOTHNESS = 0.0F;
   public static final float DEFAULT_ICON_SIZE = 8.0F;
   public static final float DEFAULT_SPACING = 14.0F;
   public static final float DEFAULT_PADDING = 2.0F;

   public HalfIconDrawData(
      float local5023_1,
      float local5023_2,
      float local5023_3,
      float local5023_4,
      float local5023_5,
      int local5023_6,
      String local5023_7,
      String local5023_8,
      float local5023_9,
      int local5023_10,
      float local5023_11,
      float local5023_12,
      float local5023_13
   ) {
      this(
         local5023_1,
         local5023_2,
         local5023_3,
         local5023_4,
         local5023_5,
         local5023_5,
         local5023_5,
         local5023_5,
         local5023_6,
         local5023_6,
         local5023_6,
         local5023_6,
         0.0F,
         local5023_7,
         local5023_8,
         local5023_9,
         local5023_10,
         local5023_11,
         local5023_12,
         2.0F,
         2.0F,
         0.0F,
         0.0F,
         local5023_13,
         0L
      );
   }

   public HalfIconDrawData withBackground(int local5025_1) {
      return new HalfIconDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         local5025_1,
         local5025_1,
         local5025_1,
         local5025_1,
         this.smoothness,
         this.fontName,
         this.icon,
         this.iconSize,
         this.iconColor,
         this.spacingX,
         this.spacingY,
         this.paddingX,
         this.paddingY,
         this.jitterX,
         this.jitterY,
         this.rotationMaxDegrees,
         this.seed
      );
   }

   public boolean visible() {
      return this.backgroundVisible() || this.iconsVisible();
   }

   public boolean backgroundVisible() {
      return this.width > 0.0F
         && this.height > 0.0F
         && ((this.colorTopLeft | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft) >>> 24 & 0xFF) > 0;
   }

   public void render(DrawContext drawContext) {
      RectHalfIconRenderer.getRectHalfIconRenderer().renderElement2(drawContext, this);
   }

   public HalfIconDrawData withPadding(float local5044_1, float local5044_2) {
      return new HalfIconDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft,
         this.smoothness,
         this.fontName,
         this.icon,
         this.iconSize,
         this.iconColor,
         this.spacingX,
         this.spacingY,
         local5044_1,
         local5044_2,
         this.jitterX,
         this.jitterY,
         this.rotationMaxDegrees,
         this.seed
      );
   }

   public HalfIconDrawData withIcon(int local5047_1, float local5047_2, float local5047_3, float local5047_4) {
      return new HalfIconDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft,
         this.smoothness,
         this.fontName,
         this.icon,
         local5047_2,
         local5047_1,
         local5047_3,
         local5047_4,
         this.paddingX,
         this.paddingY,
         this.jitterX,
         this.jitterY,
         this.rotationMaxDegrees,
         this.seed
      );
   }

   public boolean iconsVisible() {
      return this.width > 0.0F
         && this.height > 0.0F
         && this.fontName != null
         && this.icon != null
         && !this.icon.isEmpty()
         && this.iconSize > 0.0F
         && this.iconColor >>> 24 != 0;
   }

   public HalfIconDrawData withRandomization(float local5052_1, float local5052_2, float local5052_3, long local5052_4) {
      return new HalfIconDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft,
         this.smoothness,
         this.fontName,
         this.icon,
         this.iconSize,
         this.iconColor,
         this.spacingX,
         this.spacingY,
         this.paddingX,
         this.paddingY,
         local5052_2,
         local5052_3,
         local5052_1,
         local5052_4
      );
   }
}
