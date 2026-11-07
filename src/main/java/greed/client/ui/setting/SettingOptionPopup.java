package greed.client.ui.setting;

import greed.client.ui.ClickGuiStyle;
import greed.client.render.FontText;
import greed.client.ui.UiTheme;

import greed.client.render.FontFace;
import greed.client.render.Render2D;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

public final class SettingOptionPopup {
   public static void processData(
      SettingRenderContext settingRenderContext,
      float local7275_1,
      float local7275_2,
      List<String> local7275_3,
      IntPredicate intPredicate,
      IntConsumer intConsumer,
      boolean local7275_6,
      float local7275_7,
      boolean local7275_8
   ) {
      settingRenderContext.list2
         .add(
            () -> {
               float local7274_9 = settingRenderContext.scale;
               float local7274_10 = 0.0F;

               for (String local7274_12 : local7275_3) {
                  local7274_10 = Math.max(local7274_10, FontText.computeFloat(FontFace.SF, local7274_12, 6.0F * local7274_9));
               }

               float local7274_11 = 8.93F * local7274_9;
               float local7274_12 = 10.74F * local7274_9;
               float local7274_13 = SettingRenderContext.computeFloat(
                  local7274_10 + 5.3F * local7274_9 + 4.0F * local7274_9 + local7274_12 + 5.3F * local7274_9, 52.4F * local7274_9, 110.0F * local7274_9
               );
               float local7274_14 = Math.min(local7275_3.size(), 10) * local7274_11;
               float local7274_15 = 6.6F * local7274_9 + local7274_14;
               float local7274_16 = local7275_1 - local7274_13 - 2.2F * local7274_9;
               float local7274_17 = Math.max(4.0F * local7274_9, local7275_2 - local7274_15 - 1.0F * local7274_9) + (1.0F - local7275_7) * 5.0F * local7274_9;
               float local7274_18 = local7274_17 + 3.3F * local7274_9;
               float local7274_19 = Math.max(0.0F, local7275_3.size() * local7274_11 - local7274_14);
               float local7274_20 = SettingRenderContext.computeFloat(settingRenderContext.map10.getOrDefault(local7275_3, 0.0F), 0.0F, local7274_19);
               settingRenderContext.map10.put(local7275_3, local7274_20);
               float local7274_21 = settingRenderContext.computeFloat7("popupScroll:" + System.identityHashCode(local7275_3), local7274_20);
               if (local7275_8) {
                  settingRenderContext.processData7(-100000.0F, -100000.0F, 200000.0F, 200000.0F, null, () -> settingRenderContext.object = null);
                  settingRenderContext.object2 = local7275_3;
                  settingRenderContext.floatValArray = new float[]{local7274_16, local7274_17, local7274_13, local7274_15};
                  settingRenderContext.floatVal2 = local7274_19;
               }

               Render2D.pushAlpha(local7275_7);
               Render2D.drawBlurSmooth(
                  local7274_16 + 0.4F * local7274_9,
                  local7274_17 + 0.4F * local7274_9,
                  local7274_13 - 0.8F * local7274_9,
                  local7274_15 - 0.8F * local7274_9,
                  4.1F * local7274_9,
                  15.0F,
                  1.0F,
                  -1
               );
               Render2D.drawRect(local7274_16, local7274_17, local7274_13, local7274_15, 4.5F * local7274_9, ClickGuiStyle.intVal32);
               Render2D.drawOutline(local7274_16, local7274_17, local7274_13, local7274_15, 4.5F * local7274_9, 0.5F, ClickGuiStyle.intVal33);
               float[] floatArray = new float[]{local7274_16, local7274_18, local7274_13, local7274_14};
               Render2D.beginScissor(settingRenderContext.drawContext, floatArray[0], floatArray[1], floatArray[2], floatArray[3]);

               for (int index = 0; index < local7275_3.size(); index++) {
                  float local7274_24 = local7274_17 + 4.3F * local7274_9 + index * local7274_11 - local7274_21;
                  if (!(local7274_24 + local7274_11 < local7274_18) && !(local7274_24 - local7274_11 > local7274_18 + local7274_14)) {
                     float local7274_25 = settingRenderContext.computeFloat2(
                        "popupOpt:" + System.identityHashCode(local7275_3) + ":" + index, intPredicate.test(index)
                     );
                     settingRenderContext.processData15(
                        local7275_3.get(index),
                        local7274_16 + 5.3F * local7274_9,
                        local7274_24,
                        6.0F * local7274_9,
                        UiTheme.computeInt2(ClickGuiStyle.intVal28, ClickGuiStyle.intVal27, local7274_25),
                        local7274_13 - 5.3F * local7274_9 - 4.0F * local7274_9 - local7274_12 - 5.3F * local7274_9
                     );
                     settingRenderContext.processData18(local7274_16 + local7274_13 - 5.3F * local7274_9 - local7274_12, local7274_24 - 0.2F * local7274_9, local7274_25);
                     int index2 = index;
                     if (local7275_8) {
                        settingRenderContext.processData7(local7274_16, local7274_24 - 1.2F * local7274_9, local7274_13, local7274_11, floatArray, () -> {
                           intConsumer.accept(index2);
                           if (local7275_6) {
                              settingRenderContext.object = null;
                           }
                        });
                     }
                  }
               }

               Render2D.endScissor(settingRenderContext.drawContext);
               if (local7274_19 > 0.0F) {
                  float local7274_23 = local7274_14 - 2.0F * local7274_9;
                  float local7274_24 = Math.max(6.0F * local7274_9, local7274_23 * local7274_14 / (local7275_3.size() * local7274_11));
                  Render2D.drawRect(
                     local7274_16 + local7274_13 - 2.4F * local7274_9,
                     local7274_18 + 1.0F * local7274_9 + (local7274_23 - local7274_24) * (local7274_21 / local7274_19),
                     1.1F * local7274_9,
                     local7274_24,
                     0.55F * local7274_9,
                     UiTheme.computeInt(UiTheme.intVal, 0.32F)
                  );
               }

               Render2D.popAlpha();
            }
         );
   }

   private SettingOptionPopup() {
   }
}
