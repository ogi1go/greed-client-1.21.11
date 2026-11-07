package greed.client.ui.hud;

import greed.client.render.GuiEffectsState;
import greed.client.util.GuiScale;
import greed.client.ui.ScreenClosingTransition;
import greed.client.util.TargetHudStyle;

import greed.client.event.EventBus;
import greed.client.event.EventListener;
import greed.client.event.EventPriority;
import greed.client.event.HudRenderEvent;
import greed.client.module.render.InterfaceModule;
import greed.client.module.Module;
import greed.client.module.ModuleManager;
import greed.client.render.Render2D;
import greed.client.module.render.StructureInfoModule;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;

public final class HudLayout {
   private static final HudLayout hudLayout = new HudLayout();
   private final Map<Module, Boolean> map = new IdentityHashMap<>();
   private boolean flag;
   private long timeMillis = System.nanoTime() / 1000000L;
   private float floatVal;
   private float floatVal2;
   @EventPriority(Integer.MIN_VALUE)
   private final EventListener<HudRenderEvent> hudRenderListener = event -> {
      GuiEffectsState.processData3(false);
      InterfaceModule.getInstance().updateState();
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null && minecraftClient.player != null && minecraftClient.world != null) {
         if (!(minecraftClient.currentScreen instanceof LevelLoadingScreen)
            && !(minecraftClient.currentScreen instanceof ProgressScreen)
            && !(minecraftClient.currentScreen instanceof MessageScreen)) {
            long local7721_3 = System.nanoTime() / 1000000L;
            float local7721_5 = Math.min(0.05F, (float)(local7721_3 - this.timeMillis) / 1000.0F);
            this.timeMillis = local7721_3;
            float local7721_6 = GuiScale.computeInt() / GuiScale.computeFloat();
            float local7721_7 = event.computeInt2() * local7721_6;
            float local7721_8 = event.computeInt() * local7721_6;
            HudRenderContext.processData(minecraftClient, local7721_5);
            HudRenderContext.renderElement(event.getDrawContext());
            HudManager.saveHudLayout();
            HudManager.processData7(
               (float)(minecraftClient.mouse.getScaledX(minecraftClient.getWindow()) * local7721_6),
               (float)(minecraftClient.mouse.getScaledY(minecraftClient.getWindow()) * local7721_6),
               HudRenderContext.checkState2(),
               local7721_7,
               local7721_8
            );
            NotificationHud.processData(HudRenderContext.checkState2());
            this.updateState();
            if (StructureInfoModule.getInstance().isEnabled()) {
               StructureInfoModule.getInstance().onHudRender(event);
               Render2D.bumpGuiLayer(event.getDrawContext());
            }

            if (!minecraftClient.options.hudHidden && InterfaceModule.getInstance().matchesCriteria("cooldowns")) {
               CooldownsHud.processData5(minecraftClient, event.computeInt2(), event.computeInt());
               Render2D.bumpGuiLayer(event.getDrawContext());
            }

            String local7721_9 = "";
            ArrayList<float[]> local7721_10 = new ArrayList<>();

            for (String local7721_12 : HudManager.getList()) {
               float[] floatArray = (!HudManager.matchesCriteria4(local7721_12) || !minecraftClient.options.hudHidden)
                     && InterfaceModule.getInstance().matchesCriteria(local7721_12)
                  ? HudManager.computeFloat3(local7721_12)
                  : null;
               if (floatArray != null) {
                  for (float[] floatArray2 : local7721_10) {
                     if (floatArray[0] < floatArray2[0] + floatArray2[2]
                        && floatArray[0] + floatArray[2] > floatArray2[0]
                        && floatArray[1] < floatArray2[1] + floatArray2[3]
                        && floatArray[1] + floatArray[3] > floatArray2[1]) {
                        local7721_9 = local7721_12;
                     }
                  }

                  local7721_10.add(floatArray);
               }
            }

            boolean local7721_11 = false;

            for (String local7721_13 : new ArrayList<>(HudManager.getList())) {
               if ((!HudManager.matchesCriteria4(local7721_13) || !minecraftClient.options.hudHidden) && InterfaceModule.getInstance().matchesCriteria(local7721_13)) {
                  if (minecraftClient.currentScreen == null && local7721_13.equals(local7721_9)) {
                     GuiEffectsState.processData3(true);
                     event.getDrawContext().createNewRootLayer();
                     event.getDrawContext().applyBlur();
                     Render2D.setBlurMode(1);
                     local7721_11 = true;
                  } else {
                     event.getDrawContext().createNewRootLayer();
                  }

                  float local7721_14 = HudManager.computeFloat8(local7721_13);
                  float[] floatArray3 = HudManager.computeFloat6(local7721_13);
                  boolean local7721_16 = Math.abs(local7721_14 - 1.0F) > 0.001F && floatArray3 != null;
                  if (local7721_16) {
                     HudRenderContext.processData3(floatArray3[0], floatArray3[1], local7721_14);
                  }

                  switch (local7721_13) {
                     case "watermark":
                        WatermarkHud.processData(minecraftClient, 12.0F, 12.0F);
                        break;
                     case "keybinds":
                        KeybindsHud.computeFloat(12.0F, local7721_8 - 12.0F);
                        break;
                     case "potions":
                        this.floatVal = PotionsHud.computeFloat2(minecraftClient, local7721_7 - 12.0F, 12.0F);
                        break;
                     case "cooldowns":
                        this.floatVal2 = CooldownsHud.computeFloat3(
                           minecraftClient, local7721_7 - 12.0F, 12.0F + (!HudManager.matchesCriteria5("potions") ? this.floatVal : 0.0F)
                        );
                        break;
                     case "staffs":
                        StaffHud.computeFloat3(
                           local7721_7 - 12.0F,
                           12.0F
                              + (!HudManager.matchesCriteria5("potions") ? this.floatVal : 0.0F)
                              + (!HudManager.matchesCriteria5("cooldowns") ? this.floatVal2 : 0.0F)
                        );
                        break;
                     case "serverbinds":
                        ServerKeybindsHud.computeFloat3(minecraftClient, local7721_7 / 2.0F, local7721_8 - 34.0F);
                        break;
                     case "inventory":
                        InventoryHud.processData(minecraftClient, 12.0F, local7721_8 / 2.0F - 20.0F);
                        break;
                     case "Target":
                        TargetHudStyle.processData8(minecraftClient, 12.0F, local7721_8 / 2.0F + 30.0F);
                        break;
                     case "target":
                        TargetHud.processData(minecraftClient, local7721_7, local7721_8, local7721_5);
                        break;
                     case "arrows":
                        ArrowsHud.processData(local7721_7, local7721_8);
                  }

                  if (local7721_16) {
                     HudRenderContext.updateState();
                  }
               }
            }

            if (local7721_11) {
               Render2D.setBlurMode(0);
            }

            event.getDrawContext().createNewRootLayer();
            if (InterfaceModule.getInstance().matchesCriteria("toasts")) {
               boolean local7721_12 = Math.abs(HudManager.computeFloat8("toasts") - 1.0F) > 0.001F && HudManager.computeFloat6("toasts") != null;
               if (local7721_12) {
                  HudRenderContext.processData3(
                     HudManager.computeFloat6("toasts")[0], HudManager.computeFloat6("toasts")[1], HudManager.computeFloat8("toasts")
                  );
               }

               NotificationHud.processData2(local7721_7, local7721_8);
               if (local7721_12) {
                  HudRenderContext.updateState();
               }
            }

            event.getDrawContext().createNewRootLayer();
            HudManager.processData8(
               (float)(minecraftClient.mouse.getScaledX(minecraftClient.getWindow()) * local7721_6),
               (float)(minecraftClient.mouse.getScaledY(minecraftClient.getWindow()) * local7721_6),
               HudRenderContext.checkState2()
            );
            if (HudElementTypes.checkState()) {
               event.getDrawContext().createNewRootLayer();
            }

            HudElementTypes.processData(
               (float)(minecraftClient.mouse.getScaledX(minecraftClient.getWindow()) * local7721_6),
               (float)(minecraftClient.mouse.getScaledY(minecraftClient.getWindow()) * local7721_6),
               HudRenderContext.checkState2()
            );
            if (!ScreenClosingTransition.checkState()) {
               Render2D.bumpGuiLayer(event.getDrawContext());
               ScreenClosingTransition.renderElement(event.getDrawContext());
            }
         }
      } else {
         this.flag = false;
         this.map.clear();
      }
   };

   private void updateState() {
      boolean local7719_1 = !this.flag;

      for (Module module : ModuleManager.getModules()) {
         Boolean local7719_4 = this.map.put(module, module.isEnabled());
         if (!local7719_1 && local7719_4 != null && local7719_4 != module.isEnabled()) {
            NotificationHud.processData3(
               module.category.getIcon(), module.name, module.isEnabled() ? "is Enabled!" : "is Disabled!", module.isEnabled()
            );
         }
      }

      this.flag = true;
   }

   private HudLayout() {
   }

   public static void updateState2() {
      EventBus.getInstance().subscribe(hudLayout);
   }
}
