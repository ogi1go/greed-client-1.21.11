package greed.client.render;

import greed.client.render.SoftGlowRenderer;

import net.minecraft.client.gui.DrawContext;

public record SoftGlowDrawData(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   int color,
   float softness,
   float intensity,
   float mode,
   float dirX,
   float dirY,
   float dirStrength,
   float falloff
) {
   public static final float MODE_FILL = 0.0F;
   public static final float MODE_RING = 1.0F;
   public static final float DEFAULT_FALLOFF = 2.0F;

   public float pad() {
      return Math.max(this.softness * 4.0F, 2.0F);
   }

   public SoftGlowDrawData asFill() {
      return new SoftGlowDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.softness,
         this.intensity,
         0.0F,
         this.dirX,
         this.dirY,
         this.dirStrength,
         this.falloff
      );
   }

   public boolean visible() {
      return this.width >= 0.0F && this.height >= 0.0F && this.softness > 0.0F && this.intensity > 0.0F && this.color >>> 24 != 0;
   }

   public float extentX() {
      return this.width / 2.0F + this.pad();
   }

   public SoftGlowDrawData(float local4592_1, float local4592_2, float local4592_3, float local4592_4, float local4592_5, int local4592_6, float local4592_7) {
      this(
         local4592_1,
         local4592_2,
         local4592_3,
         local4592_4,
         local4592_5,
         local4592_5,
         local4592_5,
         local4592_5,
         local4592_6,
         local4592_7,
         1.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         2.0F
      );
   }

   public SoftGlowDrawData withIntensity(float local4597_1) {
      return new SoftGlowDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.softness,
         local4597_1,
         this.mode,
         this.dirX,
         this.dirY,
         this.dirStrength,
         this.falloff
      );
   }

   public SoftGlowDrawData asRing() {
      return new SoftGlowDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.softness,
         this.intensity,
         1.0F,
         this.dirX,
         this.dirY,
         this.dirStrength,
         this.falloff
      );
   }

   public SoftGlowDrawData withFalloff(float local4607_1) {
      return new SoftGlowDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.softness,
         this.intensity,
         this.mode,
         this.dirX,
         this.dirY,
         this.dirStrength,
         local4607_1
      );
   }

   public void render(DrawContext drawContext) {
      SoftGlowRenderer.getSoftGlowRenderer().renderElement3(drawContext, this);
   }

   public SoftGlowDrawData withRadii(float local4609_1, float local4609_2, float local4609_3, float local4609_4) {
      return new SoftGlowDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         local4609_1,
         local4609_2,
         local4609_3,
         local4609_4,
         this.color,
         this.softness,
         this.intensity,
         this.mode,
         this.dirX,
         this.dirY,
         this.dirStrength,
         this.falloff
      );
   }

   public SoftGlowDrawData withSoftness(float local4612_1) {
      return new SoftGlowDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         local4612_1,
         this.intensity,
         this.mode,
         this.dirX,
         this.dirY,
         this.dirStrength,
         this.falloff
      );
   }

   public float centerX() {
      return this.x + this.width / 2.0F;
   }

   public float extentY() {
      return this.height / 2.0F + this.pad();
   }

   public float centerY() {
      return this.y + this.height / 2.0F;
   }

   public SoftGlowDrawData withDirection(float local4620_1, float local4620_2, float local4620_3) {
      return new SoftGlowDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.softness,
         this.intensity,
         this.mode,
         local4620_1,
         local4620_2,
         local4620_3,
         this.falloff
      );
   }
}
