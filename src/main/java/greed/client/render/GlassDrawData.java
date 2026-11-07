package greed.client.render;

import greed.client.render.GlassRenderer;

import net.minecraft.client.gui.DrawContext;

public record GlassDrawData(
   float x,
   float y,
   float width,
   float height,
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
   int splitIndex
) {
   private static float radiusValue(float[] floatArray, int radiusIndex) {
      return floatArray != null && radiusIndex >= 0 && radiusIndex < floatArray.length ? floatArray[radiusIndex] : 0.0F;
   }

   public GlassDrawData withBlurRadius(float newBlurRadius) {
      return new GlassDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
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
         newBlurRadius,
         this.secondColor,
         this.colorOffset
      );
   }

   public GlassDrawData withRainbow(float redFraction, float greenFraction) {
      int red = Math.max(0, Math.min(255, Math.round(redFraction * 255.0F)));
      int green = Math.max(0, Math.min(255, Math.round(greenFraction * 255.0F)));
      int rainbowColor = 0xFF000000 | red << 16 | green << 8;
      return new GlassDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
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
         rainbowColor,
         this.colorOffset
      );
   }

   public GlassDrawData withSplitIndex(int newSplitIndex) {
      return new GlassDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
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
         newSplitIndex
      );
   }

   public GlassDrawData(
      float x,
      float y,
      float width,
      float height,
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
      float z
   ) {
      this(
         x,
         y,
         width,
         height,
         radiusTopLeft,
         radiusTopRight,
         radiusBottomRight,
         radiusBottomLeft,
         color,
         globalAlpha,
         fresnelPower,
         fresnelColor,
         baseAlpha,
         fresnelInvert,
         fresnelMix,
         distortStrength,
         squirt,
         z,
         30.0F,
         color,
         0.0F
      );
   }

   public void render(DrawContext drawContext) {
      GlassRenderer.getGlassRenderer().renderElement2(drawContext, this);
   }

   public GlassDrawData(
      float x,
      float y,
      float width,
      float height,
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
      float colorOffset
   ) {
      this(
         x,
         y,
         width,
         height,
         radiusTopLeft,
         radiusTopRight,
         radiusBottomRight,
         radiusBottomLeft,
         color,
         globalAlpha,
         fresnelPower,
         fresnelColor,
         baseAlpha,
         fresnelInvert,
         fresnelMix,
         distortStrength,
         squirt,
         z,
         blurRadius,
         secondColor,
         colorOffset,
         0
      );
   }

   public GlassDrawData withSecondColor(int newSecondColor, float newColorOffset) {
      return new GlassDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
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
         newSecondColor,
         newColorOffset
      );
   }

   public GlassDrawData(
      float x,
      float y,
      float width,
      float height,
      float[] floatArray,
      int color,
      float globalAlpha,
      float fresnelPower,
      int fresnelColor,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float squirt,
      float z
   ) {
      this(
         x,
         y,
         width,
         height,
         radiusValue(floatArray, 0),
         radiusValue(floatArray, 1),
         radiusValue(floatArray, 2),
         radiusValue(floatArray, 3),
         color,
         globalAlpha,
         fresnelPower,
         fresnelColor,
         baseAlpha,
         fresnelInvert,
         fresnelMix,
         distortStrength,
         squirt,
         z
      );
   }

   public boolean visible() {
      return this.width > 0.0F
         && this.height > 0.0F
         && this.globalAlpha > 0.0F
         && (this.baseAlpha > 0.0F || this.fresnelColor >>> 24 != 0 || this.color >>> 24 != 0);
   }
}
