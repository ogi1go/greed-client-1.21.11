package greed.client.module.render;

import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.render.FontFace;
import greed.client.event.HudRenderEvent;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PlayerTickEvent;
import greed.client.util.GuiScale;
import greed.client.render.FontText;
import greed.client.ui.hud.NotificationHud;
import greed.client.util.SoundPlayer;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;

@ModuleInfo(name = "ArmorBar", description = "Броня на уровне хотбара или над голодом ванильными текстурами.", category = ModuleCategory.RENDER)
public final class ArmorBarModule extends Module {
   private static ArmorBarModule armorBarModule;
   private static final EquipmentSlot[] equipmentSlotArray = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final Identifier[] identifierArray = new Identifier[]{
      PlayerScreenHandler.EMPTY_HELMET_SLOT_TEXTURE,
      PlayerScreenHandler.EMPTY_CHESTPLATE_SLOT_TEXTURE,
      PlayerScreenHandler.EMPTY_LEGGINGS_SLOT_TEXTURE,
      PlayerScreenHandler.EMPTY_BOOTS_SLOT_TEXTURE
   };
   private static final String[] strValArray = new String[]{"Шлем", "Нагрудник", "Поножи", "Ботинки"};
   public ModeSetting modeSetting;
   public BooleanSetting booleanSetting;
   private final boolean[] flagArray = new boolean[4];
   private final EventListener<HudRenderEvent> hudRenderListener = hudRenderEvent -> {
      if (mc.player != null && !mc.options.hudHidden) {
         for (int index = 0; index < 4; index++) {
            if (this.modeSetting.isSelected("Тип 1")) {
               this.processData(
                  mc.player.getEquippedStack(equipmentSlotArray[index]),
                  (
                        mc.player.getMainArm() == Arm.LEFT
                           ? hudRenderEvent.computeInt2() / 2.0F - 91.0F - 4.0F - 82.0F
                           : hudRenderEvent.computeInt2() / 2.0F + 91.0F + 4.0F
                     )
                     + 3.0F
                     + index * 20.0F,
                  hudRenderEvent.computeInt() - 22.0F + 3.0F
               );
            } else {
               this.processData(
                  mc.player.getEquippedStack(equipmentSlotArray[index]),
                  hudRenderEvent.computeInt2() / 2.0F + 91.0F - 70.0F + index * 18.0F,
                  hudRenderEvent.computeInt() - 57.0F - (Math.round(mc.player.getHungerManager().getSaturationLevel()) > 0 ? 10.0F : 0.0F)
               );
            }
         }
      }
   };
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && this.booleanSetting.isChecked()) {
         for (int index2 = 0; index2 < 4; index2++) {
            ItemStack equippedStack = mc.player.getEquippedStack(equipmentSlotArray[index2]);
            if (!equippedStack.isEmpty() && equippedStack.isDamageable()) {
               if (1.0F - (float)equippedStack.getDamage() / equippedStack.getMaxDamage() > 0.3F) {
                  this.flagArray[index2] = false;
               } else if (!this.flagArray[index2]) {
                  this.flagArray[index2] = true;
                  NotificationHud.processData3(
                     this.category.getIcon(),
                     strValArray[index2],
                     "прочность " + Math.round(100.0F * (1.0F - (float)equippedStack.getDamage() / equippedStack.getMaxDamage())) + "%, скоро сломается",
                     true
                  );
                  SoundPlayer.processData("notify_type1_enabled");
               }
            } else {
               this.flagArray[index2] = false;
            }
         }
      }
   };

   public void renderElement(DrawContext drawContext, RenderTickCounter renderTickCounter) {
      if (mc.player != null) {
         if (this.modeSetting.isSelected("Тип 1")) {
            this.renderElement3(drawContext);
         } else {
            this.renderElement2(drawContext);
         }
      }
   }

   public static ArmorBarModule getInstance() {
      if (armorBarModule == null) {
         armorBarModule = new ArmorBarModule();
      }

      return armorBarModule;
   }

   private ArmorBarModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Тип", "Панель сбоку от хотбара.", "Тип 1", "Тип 1"));
      this.modeSetting.withVisibility(() -> false);
      this.booleanSetting = this.addSetting(new BooleanSetting("Уведомлять о прочности", "Уведомление, когда прочность элемента брони падает ниже 30%.", true));
   }

   private void processData(ItemStack itemStack, float local6902_2, float local6902_3) {
      if (!itemStack.isEmpty() && itemStack.isDamageable()) {
         float local6902_4 = GuiScale.computeInt() / GuiScale.computeFloat();
         String local6902_5 = Math.round(100.0F * (1.0F - (float)itemStack.getDamage() / itemStack.getMaxDamage())) + "%";
         float local6902_6 = 5.0F * local6902_4;
         float local6902_7 = 0.7F * local6902_4;
         float local6902_8 = (local6902_2 + 1.0F) * local6902_4;
         float local6902_9 = (local6902_3 + 2.5F) * local6902_4 - local6902_6 / 2.0F;
         int local6902_10 = Math.round(100.0F * (1.0F - (float)itemStack.getDamage() / itemStack.getMaxDamage())) > 50
            ? -12517568
            : (Math.round(100.0F * (1.0F - (float)itemStack.getDamage() / itemStack.getMaxDamage())) > 30 ? -20440 : -50384);
         FontText.processData3(FontFace.BOLD, local6902_5, local6902_8 - local6902_7, local6902_9, local6902_6, -16777216);
         FontText.processData3(FontFace.BOLD, local6902_5, local6902_8 + local6902_7, local6902_9, local6902_6, -16777216);
         FontText.processData3(FontFace.BOLD, local6902_5, local6902_8, local6902_9 - local6902_7, local6902_6, -16777216);
         FontText.processData3(FontFace.BOLD, local6902_5, local6902_8, local6902_9 + local6902_7, local6902_6, -16777216);
         FontText.processData3(FontFace.BOLD, local6902_5, local6902_8, local6902_9, local6902_6, local6902_10);
      }
   }

   private void renderElement2(DrawContext drawContext) {
      float local6903_2 = drawContext.getScaledWindowWidth() / 2.0F + 91.0F - 70.0F;
      float local6903_3 = drawContext.getScaledWindowHeight()
         - 57.0F
         - (Math.round(mc.player.getHungerManager().getSaturationLevel()) > 0 ? 10.0F : 0.0F);

      for (int index = 0; index < 4; index++) {
         if (mc.player.getEquippedStack(equipmentSlotArray[index]).isEmpty()) {
            drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifierArray[index], (int)local6903_2 + index * 18, (int)local6903_3, 16, 16);
         } else {
            drawContext.drawItem(mc.player.getEquippedStack(equipmentSlotArray[index]), (int)local6903_2 + index * 18, (int)local6903_3);
            drawContext.drawStackOverlay(
               mc.textRenderer, mc.player.getEquippedStack(equipmentSlotArray[index]), (int)local6903_2 + index * 18, (int)local6903_3
            );
         }
      }
   }

   private void renderElement3(DrawContext drawContext) {
      float local6904_2 = mc.player.getMainArm() == Arm.LEFT
         ? drawContext.getScaledWindowWidth() / 2.0F - 91.0F - 4.0F - 82.0F
         : drawContext.getScaledWindowWidth() / 2.0F + 91.0F + 4.0F;
      float local6904_3 = drawContext.getScaledWindowHeight() - 22.0F;
      drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, Identifier.ofVanilla("hud/hotbar"), 182, 22, 0, 0, (int)local6904_2, (int)local6904_3, 81, 22);
      drawContext.drawGuiTexture(
         RenderPipelines.GUI_TEXTURED, Identifier.ofVanilla("hud/hotbar"), 182, 22, 181, 0, (int)local6904_2 + 81, (int)local6904_3, 1, 22
      );

      for (int index = 0; index < 4; index++) {
         drawContext.drawItem(mc.player.getEquippedStack(equipmentSlotArray[index]), (int)local6904_2 + 3 + index * 20, (int)local6904_3 + 3);
         if (!mc.player.getEquippedStack(equipmentSlotArray[index]).isEmpty()) {
            drawContext.drawStackOverlay(
               mc.textRenderer,
               mc.player.getEquippedStack(equipmentSlotArray[index]),
               (int)local6904_2 + 3 + index * 20,
               (int)local6904_3 + 3
            );
         }
      }
   }
}
