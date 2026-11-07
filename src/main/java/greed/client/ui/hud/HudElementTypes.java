package greed.client.ui.hud;

import greed.client.ui.AnimatedValue;
import greed.client.ui.setting.SettingRenderContext;
import greed.client.ui.setting.SettingRendererRegistry;
import greed.client.render.ShapeDrawing;
import greed.client.ui.UiTheme;

import greed.client.module.render.InterfaceModule;
import greed.client.render.Render2D;
import greed.client.setting.Setting;

import java.util.ArrayList;

public final class HudElementTypes {
   private static final AnimatedValue animatedValue = new AnimatedValue(180L);
   private static final SettingRenderContext settingRenderContext = new SettingRenderContext();
   private static String strVal;
   private static boolean flag;
   private static float floatVal;
   private static float floatVal2;
   private static float floatVal3 = 60.0F;
   private static boolean flag2;
   private static float floatVal4;
   private static float floatVal5;
   private static String strVal2;
   private static float floatVal6;
   private static float floatVal7;

   public static boolean matchesCriteria(double local8044_0, double local8044_2, int local8044_4) {
      float[] floatArray = HudManager.computeFloat11(local8044_0, local8044_2);
      float local8044_6 = floatArray[0];
      float local8044_7 = floatArray[1];
      boolean local8044_8 = strVal != null
         && flag
         && local8044_6 >= floatVal
         && local8044_6 <= floatVal + 103.3F
         && local8044_7 >= floatVal2
         && local8044_7 <= floatVal2 + floatVal3;
      if (local8044_4 == 1) {
         if (local8044_8) {
            for (int index = settingRenderContext.list.size() - 1; index >= 0; index--) {
               SettingRenderContext.DataRecord2 local8044_10 = settingRenderContext.list.get(index);
               if (local8044_10.rightAction() != null
                  && UiTheme.matchesCriteria(local8044_6, local8044_7, local8044_10.x(), local8044_10.y(), local8044_10.w(), local8044_10.h())) {
                  local8044_10.rightAction().run();
                  return true;
               }
            }

            updateState();
            return true;
         } else {
            String local8044_9 = HudManager.findSnapAnchor(local8044_6, local8044_7);
            if (local8044_9 != null && !getArrayList(local8044_9).isEmpty()) {
               processData2(local8044_9, local8044_6, local8044_7);
               return true;
            } else if (strVal != null && flag) {
               updateState();
               return true;
            } else {
               return false;
            }
         }
      } else if (local8044_4 == 0 && strVal != null && flag) {
         if (settingRenderContext.object != null) {
            for (int index2 = settingRenderContext.list.size() - 1; index2 >= 0; index2--) {
               SettingRenderContext.DataRecord2 local8044_10 = settingRenderContext.list.get(index2);
               if (UiTheme.matchesCriteria(local8044_6, local8044_7, local8044_10.x(), local8044_10.y(), local8044_10.w(), local8044_10.h())) {
                  local8044_10.action().run();
                  return true;
               }
            }

            return true;
         } else {
            if (!local8044_8) {
               updateState();
               return false;
            }

            for (int index3 = settingRenderContext.list.size() - 1; index3 >= 0; index3--) {
               SettingRenderContext.DataRecord2 local8044_10 = settingRenderContext.list.get(index3);
               if (UiTheme.matchesCriteria(local8044_6, local8044_7, local8044_10.x(), local8044_10.y(), local8044_10.w(), local8044_10.h())) {
                  local8044_10.action().run();
                  return true;
               }
            }

            flag2 = true;
            floatVal4 = local8044_6 - floatVal;
            floatVal5 = local8044_7 - floatVal2;
            return true;
         }
      } else {
         return false;
      }
   }

   static void processData(float mouseX, float mouseY, boolean local8045_2) {
      animatedValue.processData(flag && local8045_2 && strVal != null ? 1.0 : 0.0);
      float local8045_3 = (float)animatedValue.computeDouble();
      if (strVal != null && !(local8045_3 < 0.01F)) {
         if (flag2) {
            floatVal = Math.max(5.0F, Math.min(Math.max(5.0F, HudManager.computeFloat2() - 103.3F - 5.0F), mouseX - floatVal4));
            floatVal2 = Math.max(5.0F, Math.min(Math.max(5.0F, HudManager.computeFloat4() - floatVal3 - 5.0F), mouseY - floatVal5));
         }

         ArrayList<Setting<?>> local8045_4 = getArrayList(strVal);
         float local8045_5 = 0.0F;

         for (int index = 0; index < local8045_4.size(); index++) {
            local8045_5 += SettingRendererRegistry.computeFloat2(settingRenderContext, local8045_4.get(index), 90.8F)
               + (index < local8045_4.size() - 1 ? 3.5F : 0.0F);
         }

         float local8045_6 = 5.1F + local8045_5 + 5.1F;
         floatVal3 = local8045_6;
         settingRenderContext.renderElement(HudRenderContext.getDrawContext(), 1.0F, mouseX, mouseY);
         HudRenderContext.processData3(floatVal + 51.65F, floatVal2 + local8045_6 / 2.0F, 0.96F + 0.04F * local8045_3);
         HudStyle.processData2(floatVal, floatVal2, 103.3F, local8045_6, local8045_3, 4.5F, ShapeDrawing.TypeOption.LOW);
         Render2D.pushAlpha(local8045_3);
         float local8045_7 = floatVal2 + 5.1F;

         for (Setting<?> local8045_9 : local8045_4) {
            local8045_7 += SettingRendererRegistry.computeFloat(settingRenderContext, local8045_9, 1.0F, floatVal + 6.3F, local8045_7, 90.8F, null) + 3.5F;
         }

         settingRenderContext.updateState();
         Render2D.popAlpha();
         HudRenderContext.updateState();
      } else {
         if (!flag) {
            strVal = null;
            if (strVal2 != null && local8045_2) {
               processData3(strVal2, floatVal6, floatVal7);
            }
         }
      }
   }

   public static void updateState() {
      flag = false;
      flag2 = false;
      strVal2 = null;
      settingRenderContext.object = null;
   }

   private HudElementTypes() {
   }

   public static void processData2(String local8048_0, float local8048_1, float local8048_2) {
      if (strVal != null && flag && animatedValue.computeDouble() > 0.05) {
         strVal2 = local8048_0;
         floatVal6 = local8048_1;
         floatVal7 = local8048_2;
         flag = false;
      } else {
         processData3(local8048_0, local8048_1, local8048_2);
      }
   }

   private static void processData3(String local8049_0, float local8049_1, float local8049_2) {
      strVal2 = null;
      strVal = local8049_0;
      flag = true;
      settingRenderContext.object = null;
      floatVal = Math.max(5.0F, Math.min(Math.max(5.0F, HudManager.computeFloat2() - 103.3F - 5.0F), local8049_1 + 2.0F));
      floatVal2 = Math.max(5.0F, Math.min(Math.max(5.0F, HudManager.computeFloat4() - floatVal3 - 5.0F), local8049_2 + 2.0F));
   }

   private static ArrayList<Setting<?>> getArrayList(String local8050_0) {
      ArrayList<Setting<?>> local8050_1 = new ArrayList<>();
      if ("watermark".equals(local8050_0)) {
         local8050_1.add(InterfaceModule.getInstance().centerAlignment);
         local8050_1.add(InterfaceModule.getInstance().components);
      }

      if ("potions".equals(local8050_0)) {
         local8050_1.add(InterfaceModule.getInstance().showDonationPotions);
      }

      if ("target".equals(local8050_0)) {
         local8050_1.add(InterfaceModule.getInstance().headParticlesOnHit);
      }

      return local8050_1;
   }

   public static void updateState2() {
      flag2 = false;
   }

   public static boolean checkState() {
      return strVal != null && animatedValue.computeDouble() > 0.01;
   }
}
