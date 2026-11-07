package greed.client.render;

import greed.client.render.Outline360Renderer;

import java.util.List;
import net.minecraft.client.gui.DrawContext;

public record Outline360DrawData(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   float thickness,
   int defaultColor,
   float smoothness,
   float blendDegrees,
   float angleOffsetDegrees,
   List<ArcColorSpan> ranges
) {
   public static final float DEFAULT_SMOOTHNESS = 0.5F;
   public static final float DEFAULT_BLEND_DEGREES = 14.0F;
   public static final int DEFAULT_COLOR = -13619152;

   public boolean visible() {
      return this.width > 0.0F && this.height > 0.0F && this.thickness > 0.0F && this.defaultColor >>> 24 != 0;
   }

   public Outline360DrawData withBlendDegrees(float local877_1) {
      return new Outline360DrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.thickness,
         this.defaultColor,
         this.smoothness,
         local877_1,
         this.angleOffsetDegrees,
         this.ranges
      );
   }

   public Outline360DrawData withAngleOffset(float local878_1) {
      return new Outline360DrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.thickness,
         this.defaultColor,
         this.smoothness,
         this.blendDegrees,
         local878_1,
         this.ranges
      );
   }

   public void render(DrawContext drawContext) {
      Outline360Renderer.getOutline360Renderer().renderElement3(drawContext, this);
   }

   public Outline360DrawData(
      float local886_1,
      float local886_2,
      float local886_3,
      float local886_4,
      float local886_5,
      float local886_6,
      float local886_7,
      float local886_8,
      float local886_9,
      int local886_10,
      List<ArcColorSpan> local886_11
   ) {
      this(
         local886_1,
         local886_2,
         local886_3,
         local886_4,
         local886_5,
         local886_6,
         local886_7,
         local886_8,
         local886_9,
         local886_10,
         0.5F,
         14.0F,
         0.0F,
         local886_11
      );
   }

   public Outline360DrawData withSmoothness(float local890_1) {
      return new Outline360DrawData(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.thickness,
         this.defaultColor,
         local890_1,
         this.blendDegrees,
         this.angleOffsetDegrees,
         this.ranges
      );
   }

   public Outline360DrawData {
      ranges = ranges == null ? List.of() : List.copyOf(ranges);
   }

   public Outline360DrawData(
      float local897_1,
      float local897_2,
      float local897_3,
      float local897_4,
      float local897_5,
      float local897_6,
      int local897_7,
      List<ArcColorSpan> local897_8
   ) {
      this(
         local897_1, local897_2, local897_3, local897_4, local897_5, local897_5, local897_5, local897_5, local897_6, local897_7, 0.5F, 14.0F, 0.0F, local897_8
      );
   }
}
