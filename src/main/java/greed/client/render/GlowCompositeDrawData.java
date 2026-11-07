package greed.client.render;

import greed.client.render.GlowCompositeRenderer;

import net.minecraft.client.gui.DrawContext;

public record GlowCompositeDrawData(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   int color,
   float intensity,
   float blurRadius,
   float alpha,
   int secondColor,
   float colorOffset,
   float[] spans,
   int spanCount,
   float leftAligned,
   float bottomAnchored,
   int splitIndex
) {
   public GlowCompositeDrawData withSecondColor(int local7145_1, float local7145_2) {
      return new GlowCompositeDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.intensity,
         this.blurRadius,
         this.alpha,
         local7145_1,
         local7145_2,
         this.spans,
         this.spanCount,
         this.leftAligned,
         this.bottomAnchored,
         this.splitIndex
      );
   }

   public GlowCompositeDrawData(
      float local7149_1,
      float local7149_2,
      float local7149_3,
      float local7149_4,
      float[] floatArray,
      int local7149_6,
      float local7149_7,
      float local7149_8,
      float local7149_9
   ) {
      this(
         local7149_1,
         local7149_2,
         local7149_3,
         local7149_4,
         radiusValue(floatArray, 0),
         radiusValue(floatArray, 1),
         radiusValue(floatArray, 2),
         radiusValue(floatArray, 3),
         local7149_6,
         local7149_7,
         local7149_8,
         local7149_9,
         local7149_6,
         0.0F,
         null,
         0,
         1.0F,
         0.0F,
         0
      );
   }

   public float effectivePad() {
      return Math.max(this.blurRadius, 6.0F);
   }

   public void render(DrawContext drawContext) {
      GlowCompositeRenderer.getGlowCompositeRenderer().renderElement2(drawContext, this);
   }

   public GlowCompositeDrawData withBoxesMode() {
      return new GlowCompositeDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.intensity,
         this.blurRadius,
         this.alpha,
         this.secondColor,
         this.colorOffset,
         this.spans,
         this.spanCount,
         -1.0F,
         this.bottomAnchored,
         this.splitIndex
      );
   }

   public boolean visible() {
      return this.width > 0.0F && this.height > 0.0F && this.alpha > 0.0F && this.intensity > 0.0F && this.color >>> 24 != 0;
   }

   private static float radiusValue(float[] floatArray, int local7165_1) {
      return floatArray != null && local7165_1 >= 0 && local7165_1 < floatArray.length ? floatArray[local7165_1] : 0.0F;
   }

   public GlowCompositeDrawData withAlignment(boolean local7168_1, boolean local7168_2) {
      return new GlowCompositeDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.intensity,
         this.blurRadius,
         this.alpha,
         this.secondColor,
         this.colorOffset,
         this.spans,
         this.spanCount,
         local7168_1 ? 1.0F : 0.0F,
         local7168_2 ? 1.0F : 0.0F,
         this.splitIndex
      );
   }

   public GlowCompositeDrawData(float local7172_1, float local7172_2, float local7172_3, float local7172_4, float local7172_5, int local7172_6, float local7172_7) {
      this(
         local7172_1,
         local7172_2,
         local7172_3,
         local7172_4,
         local7172_5,
         local7172_5,
         local7172_5,
         local7172_5,
         local7172_6,
         1.0F,
         16.0F,
         local7172_7,
         local7172_6,
         0.0F,
         null,
         0,
         1.0F,
         0.0F,
         0
      );
   }

   public GlowCompositeDrawData withSplitIndex(int local7173_1) {
      return new GlowCompositeDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.intensity,
         this.blurRadius,
         this.alpha,
         this.secondColor,
         this.colorOffset,
         this.spans,
         this.spanCount,
         this.leftAligned,
         this.bottomAnchored,
         local7173_1
      );
   }

   public GlowCompositeDrawData withSpans(float[] floatArray, int local7174_2) {
      return new GlowCompositeDrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.color,
         this.intensity,
         this.blurRadius,
         this.alpha,
         this.secondColor,
         this.colorOffset,
         floatArray,
         local7174_2,
         this.leftAligned,
         this.bottomAnchored,
         this.splitIndex
      );
   }
}
