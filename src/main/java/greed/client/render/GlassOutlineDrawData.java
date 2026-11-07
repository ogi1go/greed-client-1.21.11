package greed.client.render;

import greed.client.render.OutlineGlassRenderer;

import net.minecraft.client.gui.DrawContext;

public record GlassOutlineDrawData(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   float thickness,
   int color,
   float globalAlpha,
   float fresnelPower,
   int fresnelColor,
   float baseAlpha,
   boolean fresnelInvert,
   float fresnelMix,
   float distortStrength,
   float squirt,
   float smoothness,
   float z
) {
   public static final float DEFAULT_SMOOTHNESS = 0.5F;

   public boolean visible() {
      return this.width > 0.0F
         && this.height > 0.0F
         && this.thickness > 0.0F
         && this.globalAlpha > 0.0F
         && (this.color >>> 24 != 0 || this.baseAlpha > 0.0F || this.fresnelColor >>> 24 != 0);
   }

   public GlassOutlineDrawData(
      float local6432_1,
      float local6432_2,
      float local6432_3,
      float local6432_4,
      float[] floatArray,
      float local6432_6,
      int local6432_7,
      float local6432_8,
      float local6432_9,
      int local6432_10,
      float local6432_11,
      boolean local6432_12,
      float local6432_13,
      float local6432_14,
      float local6432_15,
      float local6432_16
   ) {
      this(
         local6432_1,
         local6432_2,
         local6432_3,
         local6432_4,
         radiusValue(floatArray, 0),
         radiusValue(floatArray, 1),
         radiusValue(floatArray, 2),
         radiusValue(floatArray, 3),
         local6432_6,
         local6432_7,
         local6432_8,
         local6432_9,
         local6432_10,
         local6432_11,
         local6432_12,
         local6432_13,
         local6432_14,
         local6432_15,
         0.5F,
         local6432_16
      );
   }

   private static float radiusValue(float[] floatArray, int local6436_1) {
      return floatArray != null && local6436_1 >= 0 && local6436_1 < floatArray.length ? floatArray[local6436_1] : 0.0F;
   }

   public void render(DrawContext drawContext) {
      OutlineGlassRenderer.getOutlineGlassRenderer().renderElement2(drawContext, this);
   }
}
