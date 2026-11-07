package greed.client.render;

import greed.client.render.CircleRenderer;

import net.minecraft.client.gui.DrawContext;

public record CircleDrawData(float x, float y, float radius, float thickness, float smoothness, int color) {
   public static final float DEFAULT_SMOOTHNESS = 0.5F;

   public void render(DrawContext drawContext) {
      CircleRenderer.getCircleRenderer().renderElement(drawContext, this);
   }

   public boolean visible() {
      return this.radius > 0.0F && this.color >>> 24 != 0;
   }

   public CircleDrawData withSmoothness(float local3201_1) {
      return new CircleDrawData(this.x, this.y, this.radius, this.thickness, local3201_1, this.color);
   }

   public CircleDrawData(float local3202_1, float local3202_2, float local3202_3, int local3202_4) {
      this(local3202_1, local3202_2, local3202_3, 0.0F, 0.5F, local3202_4);
   }

   public CircleDrawData withThickness(float local3204_1) {
      return new CircleDrawData(this.x, this.y, this.radius, local3204_1, this.smoothness, this.color);
   }
}
