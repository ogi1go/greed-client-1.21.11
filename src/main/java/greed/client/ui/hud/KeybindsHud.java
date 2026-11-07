package greed.client.ui.hud;

import greed.client.render.FontText;
import greed.client.render.ThemeColors;
import greed.client.ui.UiTheme;

import greed.client.util.Animation;

import greed.client.util.Easings;
import greed.client.render.FontFace;
import greed.client.module.Module;
import greed.client.module.ModuleManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class KeybindsHud {
   private static final String[] strValArray = new String[]{"A", "B", "C", "D", "E"};
   private static final KeybindsHud.SubContext nested0446 = new KeybindsHud.SubContext(null, "Example Module");
   private static final Map<Module, KeybindsHud.SubContext> map = new IdentityHashMap<>();
   private static final List<KeybindsHud.SubContext> list = new ArrayList<>();
   private static final Animation animator = new Animation();
   private static final Animation animator2 = new Animation();
   private static final Animation animator3 = new Animation();
   private static final Animation animator4 = new Animation();
   private static float floatVal;
   private static float floatVal2;

   private KeybindsHud() {
   }

   public static float computeFloat(float local4126_0, float local4126_1) {
      updateState();
      float titleWidth = 16.0F + FontText.computeFloat(FontFace.SEMI_BOLD, "Hotkeys", 6.0F) + 4.0F;
      float local4126_3 = 0.0F;
      float local4126_4 = 0.0F;
      float local4126_5 = 13.0F;

      for (KeybindsHud.SubContext local4126_7 : list) {
         local4126_3 = Math.max(local4126_3, HudStyle.computeFloat(10.0F, computeFloat2(local4126_7)));
         local4126_4 = Math.max(local4126_4, HudStyle.computeFloat4(computeFloat3(local4126_7)));
         local4126_5 += 14.0F * local4126_7.visibilityAnimation.floatValue();
      }

      float local4126_6 = local4126_3 + (local4126_3 > 0.0F && local4126_4 > 0.0F ? 2.0F : 0.0F) + local4126_4;
      float local4126_7 = Math.max(titleWidth, Math.max(local4126_6, 1.0F));
      boolean local4126_8 = !list.isEmpty() || HudRenderContext.checkState();
      animator.update();
      animator.animateTo(local4126_8 ? 1.0 : 0.0, 0.4, Easings.EASE_OUT_QUART);
      animator2.update();
      if (!list.isEmpty()) {
         animator2.animateTo(Math.max(local4126_7, 1.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      animator3.update();
      if (!list.isEmpty()) {
         animator3.animateTo(Math.max(titleWidth, 1.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      animator4.update();
      if (!list.isEmpty()) {
         animator4.animateTo(Math.max(local4126_3, 0.0F), 0.6, Easings.EASE_OUT_QUART);
      }

      floatVal = animator2.floatValue();
      floatVal2 = Math.max(local4126_5, 1.0F);
      float local4126_9 = animator.floatValue();
      if (local4126_9 <= 0.001F) {
         return 0.0F;
      }

      float[] floatArray = HudManager.computeFloat5("keybinds", local4126_0, local4126_1 - floatVal2);
      float local4126_11 = floatArray[0];
      float local4126_12 = floatArray[1];
      HudManager.processData9("keybinds", local4126_11, local4126_12, floatVal, floatVal2);
      HudRenderContext.processData4(local4126_11 + floatVal / 2.0F, local4126_12 + floatVal2 / 2.0F, local4126_9);
      HudStyle.processData7(local4126_11, local4126_12, titleWidth, local4126_9, HudIcons.KEYBINDS, "Hotkeys");
      float local4126_13 = local4126_12 + 15.0F;

      for (KeybindsHud.SubContext local4126_15 : list) {
         processData(local4126_11, local4126_13, local4126_9, local4126_15);
         local4126_13 += 13.0F * local4126_15.visibilityAnimation.floatValue();
      }

      HudRenderContext.updateState();
      return floatVal2 * local4126_9;
   }

   static {
      animator2.resetTo(1.0);
   }

   private static void updateState() {
      list.clear();

      for (Module currentModule : ModuleManager.getModules()) {
         boolean local4129_2 = currentModule.hasKeybind() && currentModule.isEnabled();
         KeybindsHud.SubContext local4129_3 = map.get(currentModule);
         if (local4129_3 == null) {
            if (!local4129_2) {
               continue;
            }

            local4129_3 = new KeybindsHud.SubContext(currentModule, currentModule.name);
            map.put(currentModule, local4129_3);
         }

         local4129_3.strVal2 = currentModule.hasKeybind() ? currentModule.getKeyName() : local4129_3.strVal2;
         local4129_3.visible = local4129_2;
         if (local4129_2 || local4129_3.visibilityAnimation.floatValue() > 0.001) {
            list.add(local4129_3);
         }
      }

      map.entrySet()
         .removeIf(
            local4130_0 -> !local4130_0.getValue().visible
               && local4130_0.getValue().visibilityAnimation.floatValue() <= 0.001
               && local4130_0.getValue().visibilityAnimation.isFinished()
         );
      nested0446.visible = list.isEmpty() && HudRenderContext.checkState();
      if (nested0446.visible) {
         nested0446.strVal2 = strValArray[HudRenderContext.computeInt() % strValArray.length];
      }

      if (nested0446.visible || nested0446.visibilityAnimation.floatValue() > 0.001) {
         list.add(nested0446);
      }

      list.sort(Comparator.<KeybindsHud.SubContext>comparingDouble(local4128_0 -> computeFloat3(local4128_0)).reversed());

      for (KeybindsHud.SubContext local4129_1 : list) {
         local4129_1.visibilityAnimation.update();
         local4129_1.visibilityAnimation.animateTo(local4129_1.visible ? 1.0 : 0.0, 0.6, Easings.EASE_OUT_QUART);
         boolean local4129_2 = local4129_1.module == null || local4129_1.module.isEnabled();
         local4129_1.animation.update();
         if (local4129_1.flag) {
            local4129_1.animation.animateTo(local4129_2 ? 1.0 : 0.0, 0.4, Easings.EASE_OUT_QUART);
         } else {
            local4129_1.flag = true;
            local4129_1.animation.resetTo(local4129_2 ? 1.0 : 0.0);
         }
      }
   }

   private static float computeFloat2(KeybindsHud.SubContext local4131_0) {
      return FontText.computeFloat(FontFace.SEMI_BOLD, local4131_0.strVal2, 6.0F);
   }

   private static void processData(float local4132_0, float local4132_1, float local4132_2, KeybindsHud.SubContext local4132_3) {
      float local4132_4 = local4132_3.visibilityAnimation.floatValue() * local4132_2;
      if (!(local4132_4 <= 0.001F)) {
         float local4132_5 = HudStyle.computeFloat2(local4132_4);
         float local4132_6 = computeFloat2(local4132_3);
         float local4132_7 = HudStyle.computeFloat(10.0F, local4132_6);
         float local4132_8 = HudStyle.computeFloat4(computeFloat3(local4132_3));
         float local4132_9 = local4132_0 - local4132_5;
         float local4132_10 = local4132_0 + animator4.floatValue() + 2.0F - local4132_5;
         float local4132_11 = HudStyle.computeFloat3(local4132_1);
         HudStyle.processData6(local4132_9, local4132_1, local4132_7, 11.0F, local4132_4);
         float local4132_12 = local4132_9 + 2.0F + 10.0F + 1.0F;
         float local4132_13 = local4132_12 + 0.25F;
         HudStyle.processData(local4132_12, local4132_1 + 5.5F, local4132_4, 3.0F);
         float local4132_14 = local4132_9 + (local4132_13 - local4132_9 - 10.0F) / 2.0F;
         KeybindAnimationSprite.TOGGLE
            .processData(
               local4132_14,
               local4132_1 + 5.5F - 5.0F,
               10.0F,
               10.0F,
               UiTheme.computeInt(ThemeColors.intVal9, local4132_4),
               local4132_3.animation.floatValue()
            );
         FontText.processData3(
            FontFace.SEMI_BOLD,
            local4132_3.strVal2,
            local4132_13 + (local4132_9 + local4132_7 - local4132_13 - local4132_6) / 2.0F,
            local4132_11,
            6.0F,
            UiTheme.computeInt(HudStyle.computeInt4(1.0F), local4132_4)
         );
         HudStyle.processData6(local4132_10, local4132_1, local4132_8, 11.0F, local4132_4);
         FontText.processData3(
            FontFace.SEMI_BOLD,
            local4132_3.strVal,
            local4132_10 + 4.0F,
            local4132_11,
            6.0F,
            UiTheme.computeInt(HudStyle.computeInt4(1.0F), local4132_4)
         );
      }
   }

   private static float computeFloat3(KeybindsHud.SubContext local4133_0) {
      return FontText.computeFloat(FontFace.SEMI_BOLD, local4133_0.strVal, 6.0F);
   }

   private static final class SubContext extends DraggableHudElement {
      final Module module;
      final String strVal;
      String strVal2 = "";
      final Animation animation = new Animation();
      boolean flag;

      SubContext(Module initialModule, String local4134_2) {
         this.module = initialModule;
         this.strVal = local4134_2;
      }
   }
}
