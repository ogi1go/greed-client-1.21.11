package greed.client.render;

import greed.client.render.BatchedBlurRenderer;

import net.minecraft.client.gui.DrawContext;

public record BlurDrawData(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   float smoothness,
   float blurRadius,
   int colorTopLeft,
   int colorTopRight,
   int colorBottomRight,
   int colorBottomLeft
) {
   public BlurDrawData(
      float local6726_1, float local6726_2, float local6726_3, float local6726_4, float local6726_5, float local6726_6, float local6726_7
   ) {
      this(local6726_1, local6726_2, local6726_3, local6726_4, local6726_5, local6726_5, local6726_5, local6726_5, local6726_6, local6726_7, -1);
   }

   public BlurDrawData withColor(int local6727_1) {
      return this.withColors(local6727_1, local6727_1, local6727_1, local6727_1);
   }

   public void render(DrawContext drawContext) {
      BatchedBlurRenderer.getBatchedBlurRenderer().renderElement2(drawContext, this);
   }

   public BlurDrawData withColors(int local6735_1, int local6735_2, int local6735_3, int local6735_4) {
      return new BlurDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.smoothness,
         this.blurRadius,
         local6735_1,
         local6735_2,
         local6735_3,
         local6735_4
      );
   }

   public boolean visible() {
      int local6739_1 = this.colorTopLeft | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft;
      return this.width > 0.0F && this.height > 0.0F && local6739_1 >>> 24 != 0 && this.blurRadius > 0.0F;
   }

   public BlurDrawData(
      float local6740_1,
      float local6740_2,
      float local6740_3,
      float local6740_4,
      float local6740_5,
      float local6740_6,
      float local6740_7,
      float local6740_8,
      float local6740_9,
      float local6740_10,
      int local6740_11
   ) {
      this(
         local6740_1,
         local6740_2,
         local6740_3,
         local6740_4,
         local6740_5,
         local6740_6,
         local6740_7,
         local6740_8,
         local6740_9,
         local6740_10,
         local6740_11,
         local6740_11,
         local6740_11,
         local6740_11
      );
   }

   public int color() {
      return this.colorTopLeft;
   }
}
