package greed.client.render;

import greed.client.render.ShapeRenderer;

import net.minecraft.client.gui.DrawContext;

public record ShapeDrawData(
   float x,
   float y,
   float width,
   float height,
   float[] spans,
   int spanCount,
   float innerRadius,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   int color,
   float globalAlpha,
   float fresnelPower,
   int fresnelColor,
   float baseAlpha,
   boolean fresnelInvert,
   float fresnelMix,
   float distortStrength,
   float squirt,
   float z,
   float blurRadius,
   int secondColor,
   float colorOffset,
   float leftAligned,
   float bottomAnchored,
   int splitIndex
) {
   private static final float[] NO_SPANS = new float[0];

   public ShapeDrawData withSpans(float[] floatArray, int local5705_2, float local5705_3) {
      return new ShapeDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         floatArray,
         local5705_2,
         local5705_3,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.globalAlpha,
         this.fresnelPower,
         this.fresnelColor,
         this.baseAlpha,
         this.fresnelInvert,
         this.fresnelMix,
         this.distortStrength,
         this.squirt,
         this.z,
         this.blurRadius,
         this.secondColor,
         this.colorOffset,
         this.leftAligned,
         this.bottomAnchored,
         this.splitIndex
      );
   }

   public ShapeDrawData withAlignment(boolean local5712_1, boolean local5712_2) {
      return new ShapeDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.spans,
         this.spanCount,
         this.innerRadius,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.globalAlpha,
         this.fresnelPower,
         this.fresnelColor,
         this.baseAlpha,
         this.fresnelInvert,
         this.fresnelMix,
         this.distortStrength,
         this.squirt,
         this.z,
         this.blurRadius,
         this.secondColor,
         this.colorOffset,
         local5712_1 ? 1.0F : 0.0F,
         local5712_2 ? 1.0F : 0.0F,
         this.splitIndex
      );
   }

   public boolean visible() {
      return this.width > 0.0F
         && this.height > 0.0F
         && this.globalAlpha > 0.0F
         && (this.baseAlpha > 0.0F || this.fresnelColor >>> 24 != 0 || this.color >>> 24 != 0);
   }

   public ShapeDrawData(
      float local5725_1,
      float local5725_2,
      float local5725_3,
      float local5725_4,
      float local5725_5,
      int local5725_6,
      float local5725_7,
      float local5725_8,
      int local5725_9,
      float local5725_10,
      boolean local5725_11,
      float local5725_12,
      float local5725_13,
      float local5725_14,
      float local5725_15
   ) {
      this(
         local5725_1,
         local5725_2,
         local5725_3,
         local5725_4,
         NO_SPANS,
         0,
         0.0F,
         local5725_5,
         local5725_5,
         local5725_5,
         local5725_5,
         local5725_6,
         local5725_7,
         local5725_8,
         local5725_9,
         local5725_10,
         local5725_11,
         local5725_12,
         local5725_13,
         local5725_14,
         local5725_15,
         30.0F,
         local5725_6,
         0.0F,
         1.0F,
         0.0F,
         0
      );
   }

   public ShapeDrawData withBlurRadius(float local5730_1) {
      return new ShapeDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.spans,
         this.spanCount,
         this.innerRadius,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.globalAlpha,
         this.fresnelPower,
         this.fresnelColor,
         this.baseAlpha,
         this.fresnelInvert,
         this.fresnelMix,
         this.distortStrength,
         this.squirt,
         this.z,
         local5730_1,
         this.secondColor,
         this.colorOffset,
         this.leftAligned,
         this.bottomAnchored,
         this.splitIndex
      );
   }

   public void render(DrawContext drawContext) {
      ShapeRenderer.getShapeRenderer().renderElement(drawContext, this);
   }

   public ShapeDrawData withRainbow(float local5734_1, float local5734_2) {
      int local5734_3 = Math.max(0, Math.min(255, Math.round(local5734_1 * 255.0F)));
      int local5734_4 = Math.max(0, Math.min(255, Math.round(local5734_2 * 255.0F)));
      int local5734_5 = 0xFF000000 | local5734_3 << 16 | local5734_4 << 8;
      return new ShapeDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.spans,
         this.spanCount,
         this.innerRadius,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.globalAlpha,
         this.fresnelPower,
         this.fresnelColor,
         this.baseAlpha,
         this.fresnelInvert,
         this.fresnelMix,
         this.distortStrength,
         this.squirt,
         1.0F,
         this.blurRadius,
         local5734_5,
         this.colorOffset,
         this.leftAligned,
         this.bottomAnchored,
         this.splitIndex
      );
   }

   public ShapeDrawData withSplitIndex(int local5742_1) {
      return new ShapeDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.spans,
         this.spanCount,
         this.innerRadius,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.globalAlpha,
         this.fresnelPower,
         this.fresnelColor,
         this.baseAlpha,
         this.fresnelInvert,
         this.fresnelMix,
         this.distortStrength,
         this.squirt,
         this.z,
         this.blurRadius,
         this.secondColor,
         this.colorOffset,
         this.leftAligned,
         this.bottomAnchored,
         local5742_1
      );
   }

   public ShapeDrawData withSecondColor(int local5744_1, float local5744_2) {
      return new ShapeDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.spans,
         this.spanCount,
         this.innerRadius,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.globalAlpha,
         this.fresnelPower,
         this.fresnelColor,
         this.baseAlpha,
         this.fresnelInvert,
         this.fresnelMix,
         this.distortStrength,
         this.squirt,
         this.z,
         this.blurRadius,
         local5744_1,
         local5744_2,
         this.leftAligned,
         this.bottomAnchored,
         this.splitIndex
      );
   }
}
