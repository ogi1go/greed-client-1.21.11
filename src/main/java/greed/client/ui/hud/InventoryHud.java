package greed.client.ui.hud;

import greed.client.render.FontText;
import greed.client.render.ItemRenderCache;

import greed.client.util.Animation;

import greed.client.util.Easings;
import greed.client.render.FontFace;
import greed.client.util.ItemDrawOptions;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

public final class InventoryHud {
   private static final Animation animation = new Animation();

   public static void processData(MinecraftClient minecraftClient, float x, float y) {
      if (minecraftClient.player != null) {
         boolean hasItems = false;

         for (int index = 9; index < 36 && !hasItems; index++) {
            hasItems = !minecraftClient.player.getInventory().getStack(index).isEmpty();
         }

         animation.update();
         animation.animateTo(!hasItems && !HudRenderContext.checkState() ? 0.0 : 1.0, 0.4, Easings.EASE_OUT_QUART);
         float progress = animation.floatValue();
         if (!(progress <= 0.001F)) {
            float gridWidth = 111.0F;
            float titleWidth = FontText.computeFloat(FontFace.SEMI_BOLD, "Inventory", 6.0F) + 16.0F + 5.0F;
            float panelWidth = Math.max(gridWidth, titleWidth);
            float panelHeight = 51.0F;
            float[] floatArray = HudManager.computeFloat5("inventory", x, y);
            HudManager.processData9("inventory", floatArray[0], floatArray[1], panelWidth, panelHeight);
            HudRenderContext.processData4(floatArray[0] + panelWidth / 2.0F, floatArray[1] + panelHeight / 2.0F, progress);
            HudStyle.processData7(floatArray[0], floatArray[1], titleWidth, progress, HudIcons.INVENTORY, "Inventory");
            float gridX = floatArray[0] + (panelWidth - 111.0F) / 2.0F;
            float gridY = floatArray[1] + 13.0F + 2.0F;

            for (int index2 = 0; index2 < 3; index2++) {
               float rowY = gridY + index2 * 12.5F;
               HudStyle.processData6(gridX, rowY, 111.0F, 11.0F, progress);
               float slotY = rowY + 5.5F;

               for (int index3 = 0; index3 < 8; index3++) {
                  HudStyle.processData(gridX + index3 * 12.5F + 11.0F + 0.75F - 0.25F, slotY, progress, 1.5F);
               }

               for (int index4 = 0; index4 < 9; index4++) {
                  ItemStack stack = minecraftClient.player.getInventory().getStack(9 + index2 * 9 + index4);
                  if (!stack.isEmpty()) {
                     float itemX = Math.round((gridX + index4 * 12.5F + 1.5F) * 2.0F) / 2.0F;
                     float itemY = Math.round((rowY + 1.5F) * 2.0F) / 2.0F;
                     ItemRenderCache.processData(stack, itemX, itemY, 8.0F, ItemDrawOptions.decorated(progress));
                  }
               }
            }

            HudRenderContext.updateState();
         }
      }
   }
}
