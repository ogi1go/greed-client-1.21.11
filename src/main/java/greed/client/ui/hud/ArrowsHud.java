package greed.client.ui.hud;

import greed.client.render.FontText;
import greed.client.render.ItemRenderCache;

import greed.client.util.Animation;

import greed.client.util.Easings;
import greed.client.render.FontFace;
import greed.client.util.ItemDrawOptions;
import greed.client.module.player.UseTrackerModule;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class ArrowsHud {
   private static final Animation animation = new Animation();
   private static ItemStack itemStack = Items.ARROW.getDefaultStack();
   private static int intVal;

   private ArrowsHud() {
   }

   public static void processData(float local1128_0, float local1128_1) {
      UseTrackerModule useTrackerModule = UseTrackerModule.getInstance();
      boolean local1128_3 = useTrackerModule.computeInt() > 0;
      if (local1128_3) {
         itemStack = useTrackerModule.getItemStack2();
         intVal = useTrackerModule.computeInt();
      } else if (HudRenderContext.checkState()) {
         itemStack = Items.ARROW.getDefaultStack();
         intVal = 3;
      }

      animation.update();
      animation.animateTo(!local1128_3 && !HudRenderContext.checkState() ? 0.0 : 1.0, 0.4, Easings.EASE_OUT_QUART);
      float local1128_4 = animation.floatValue();
      String local1128_5 = String.valueOf(intVal);
      float local1128_6 = 13.0F + FontText.computeFloat(FontFace.SEMI_BOLD, local1128_5, 7.0F);
      float local1128_7 = 10.0F;
      float[] floatArray = HudManager.computeFloat5("arrows", local1128_0 / 2.0F - local1128_6 / 2.0F, local1128_1 / 2.0F + 18.0F);
      HudManager.processData9("arrows", floatArray[0], floatArray[1], local1128_6, local1128_7);
      if (!(local1128_4 <= 0.001F)) {
         HudRenderContext.processData4(floatArray[0] + local1128_6 / 2.0F, floatArray[1] + local1128_7 / 2.0F, local1128_4);
         ItemRenderCache.processData(
            itemStack,
            Math.round(floatArray[0] * 2.0F) / 2.0F,
            Math.round(floatArray[1] * 2.0F) / 2.0F,
            10.0F,
            ItemDrawOptions.noDecorations(local1128_4)
         );
         FontText.processData3(
            FontFace.SEMI_BOLD, local1128_5, floatArray[0] + 13.0F, floatArray[1] + 1.5F, 7.0F, HudStyle.computeInt4(local1128_4)
         );
         HudRenderContext.updateState();
      }
   }
}
