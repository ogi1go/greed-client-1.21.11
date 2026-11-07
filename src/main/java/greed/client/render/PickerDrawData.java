package greed.client.render;

import greed.client.render.PickerRenderer;

import net.minecraft.client.gui.DrawContext;

public record PickerDrawData(float x, float y, float w, float h, int mode, float radius, float smoothness, float alpha, int solidColor, float checkerPx) {
   public void render(DrawContext drawContext) {
      PickerRenderer.getPickerRenderer().renderElement3(drawContext, this);
   }

   public static PickerDrawData hue(float local3154_0, float local3154_1, float local3154_2, float local3154_3, float local3154_4) {
      return new PickerDrawData(local3154_0, local3154_1, local3154_2, local3154_3, 0, local3154_3 * 0.25F, 0.75F, local3154_4, 0, local3154_3);
   }

   public boolean visible() {
      return this.w > 0.0F && this.h > 0.0F && this.alpha > 0.0F;
   }

   public static PickerDrawData alpha(float local3166_0, float local3166_1, float local3166_2, float local3166_3, int local3166_4, float local3166_5) {
      return new PickerDrawData(
         local3166_0, local3166_1, local3166_2, local3166_3, 1, local3166_3 * 0.25F, 0.75F, local3166_5, local3166_4, local3166_3 * 0.5F
      );
   }
}
