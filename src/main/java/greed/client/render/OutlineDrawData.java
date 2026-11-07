package greed.client.render;

import greed.client.render.OutlineDefaultRenderer;

import net.minecraft.client.gui.DrawContext;

public record OutlineDrawData(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   float thickness,
   int colorTopLeft,
   int colorTopRight,
   int colorBottomRight,
   int colorBottomLeft,
   float smoothness,
   float[] scissor
) {
   public static final float DEFAULT_SMOOTHNESS = 0.5F;
   public static final int DEFAULT_COLOR = -1;

   public void render(DrawContext drawContext) {
      OutlineDefaultRenderer.getOutlineDefaultRenderer().renderElement2(drawContext, this);
   }

   public OutlineDrawData withSmoothness(float local7648_1) {
      return new OutlineDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.thickness,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft,
         local7648_1,
         this.scissor
      );
   }

   public OutlineDrawData(float local7649_1, float local7649_2, float local7649_3, float local7649_4, float local7649_5, float local7649_6, int local7649_7) {
      this(
         local7649_1,
         local7649_2,
         local7649_3,
         local7649_4,
         local7649_5,
         local7649_5,
         local7649_5,
         local7649_5,
         local7649_6,
         local7649_7,
         local7649_7,
         local7649_7,
         local7649_7,
         0.5F,
         null
      );
   }

   public OutlineDrawData withColor(int local7655_1) {
      return new OutlineDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.thickness,
         local7655_1,
         local7655_1,
         local7655_1,
         local7655_1,
         this.smoothness,
         this.scissor
      );
   }

   public OutlineDrawData withScissor(float[] floatArray) {
      return new OutlineDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.thickness,
         this.colorTopLeft,
         this.colorTopRight,
         this.colorBottomRight,
         this.colorBottomLeft,
         this.smoothness,
         floatArray
      );
   }

   public OutlineDrawData(
      float local7663_1,
      float local7663_2,
      float local7663_3,
      float local7663_4,
      float local7663_5,
      float local7663_6,
      float local7663_7,
      float local7663_8,
      float local7663_9,
      int local7663_10
   ) {
      this(
         local7663_1,
         local7663_2,
         local7663_3,
         local7663_4,
         local7663_5,
         local7663_6,
         local7663_7,
         local7663_8,
         local7663_9,
         local7663_10,
         local7663_10,
         local7663_10,
         local7663_10,
         0.5F,
         null
      );
   }

   public boolean visible() {
      int local7667_1 = this.colorTopLeft | this.colorTopRight | this.colorBottomRight | this.colorBottomLeft;
      return this.width > 0.0F && this.height > 0.0F && this.thickness > 0.0F && local7667_1 >>> 24 != 0;
   }
}
