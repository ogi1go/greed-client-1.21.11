package greed.client.render;

import greed.client.render.RectDefaultRenderer;

import net.minecraft.client.gui.DrawContext;

public record RectDrawData(
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
   float scissorX,
   float scissorY,
   float scissorWidth,
   float scissorHeight,
   float scissorRadiusTopLeft,
   float scissorRadiusTopRight,
   float scissorRadiusBottomRight,
   float scissorRadiusBottomLeft,
   int paletteMode,
   float paletteTint,
   float paletteAlpha
) {
   public static final float DEFAULT_SMOOTHNESS = 0.5F;
   public static final int DEFAULT_COLOR = -1;
   public static final int PALETTE_NONE = 0;
   public static final int PALETTE_VERTICAL = 1;
   public static final int PALETTE_HORIZONTAL = 2;

   public RectDrawData withColor(int local5981_1) {
      return new RectDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         local5981_1,
         local5981_1,
         local5981_1,
         local5981_1,
         this.smoothness,
         this.scissorX,
         this.scissorY,
         this.scissorWidth,
         this.scissorHeight,
         this.scissorRadiusTopLeft,
         this.scissorRadiusTopRight,
         this.scissorRadiusBottomRight,
         this.scissorRadiusBottomLeft,
         this.paletteMode,
         this.paletteTint,
         this.paletteAlpha
      );
   }

   public boolean visible() {
      if (this.width <= 0.0F || this.height <= 0.0F) {
         return false;
      } else {
         return this.paletteMode != 0
            ? this.paletteAlpha > 0.0F
            : (this.colorTopLeft | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft) >>> 24 != 0;
      }
   }

   public RectDrawData(
      float local5991_1,
      float local5991_2,
      float local5991_3,
      float local5991_4,
      float local5991_5,
      float local5991_6,
      float local5991_7,
      float local5991_8,
      int local5991_9
   ) {
      this(
         local5991_1,
         local5991_2,
         local5991_3,
         local5991_4,
         local5991_5,
         local5991_6,
         local5991_7,
         local5991_8,
         local5991_9,
         local5991_9,
         local5991_9,
         local5991_9,
         0.5F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0,
         1.0F,
         1.0F
      );
   }

   public void render(DrawContext drawContext) {
      RectDefaultRenderer.getRectDefaultRenderer().renderElement2(drawContext, this);
   }

   public RectDrawData(float local6001_1, float local6001_2, float local6001_3, float local6001_4, float local6001_5, int local6001_6) {
      this(
         local6001_1,
         local6001_2,
         local6001_3,
         local6001_4,
         local6001_5,
         local6001_5,
         local6001_5,
         local6001_5,
         local6001_6,
         local6001_6,
         local6001_6,
         local6001_6,
         0.5F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0,
         1.0F,
         1.0F
      );
   }

   public RectDrawData withSmoothness(float local6007_1) {
      return new RectDrawData(
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
         local6007_1,
         this.scissorX,
         this.scissorY,
         this.scissorWidth,
         this.scissorHeight,
         this.scissorRadiusTopLeft,
         this.scissorRadiusTopRight,
         this.scissorRadiusBottomRight,
         this.scissorRadiusBottomLeft,
         this.paletteMode,
         this.paletteTint,
         this.paletteAlpha
      );
   }

   public RectDrawData(
      float local6013_1,
      float local6013_2,
      float local6013_3,
      float local6013_4,
      float local6013_5,
      float local6013_6,
      float local6013_7,
      float local6013_8,
      int local6013_9,
      int local6013_10,
      int local6013_11,
      int local6013_12,
      float local6013_13
   ) {
      this(
         local6013_1,
         local6013_2,
         local6013_3,
         local6013_4,
         local6013_5,
         local6013_6,
         local6013_7,
         local6013_8,
         local6013_9,
         local6013_10,
         local6013_11,
         local6013_12,
         local6013_13,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0,
         1.0F,
         1.0F
      );
   }

   public RectDrawData withPaletteGradient(int local6014_1, float local6014_2, float local6014_3) {
      return new RectDrawData(
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
         this.scissorX,
         this.scissorY,
         this.scissorWidth,
         this.scissorHeight,
         this.scissorRadiusTopLeft,
         this.scissorRadiusTopRight,
         this.scissorRadiusBottomRight,
         this.scissorRadiusBottomLeft,
         local6014_1,
         local6014_2,
         local6014_3
      );
   }
}
