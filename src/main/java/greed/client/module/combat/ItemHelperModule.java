package greed.client.module.combat;

import greed.client.setting.BooleanSetting;
import greed.client.event.ContainerRenderEvent;
import greed.client.event.EventListener;
import greed.client.setting.KeybindSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.NumberSetting;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.util.InventoryActionQueue;
import greed.client.setting.SectionSetting;
import greed.client.module.player.ServerHelperModule;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.slot.Slot;

@ModuleInfo(name = "ItemHelper", description = "Использует предмет, пока зажата его клавиша.", category = ModuleCategory.COMBAT)
public final class ItemHelperModule extends Module {
   private static ItemHelperModule itemHelperModule;
   public BooleanSetting releaseOnComplete;
   public NumberSetting numberSetting;
   public BooleanSetting healingPotion;
   public BooleanSetting enchantedApple;
   public BooleanSetting goldenApple;
   public BooleanSetting chorusFruit;
   public final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && mc.interactionManager != null) {
         for (ItemHelperModule.ItemActionType local6148_5 : ItemHelperModule.ItemActionType.values()) {
            boolean local6148_6 = mc.currentScreen == null && local6148_5.keybindSetting.getBinding().isPressed(mc.getWindow().getHandle());
            if (!local6148_6) {
               if (local6148_5.flag) {
                  mc.options.useKey.setPressed(false);
                  local6148_5.flag = false;
               }

               this.processData(local6148_5);
            } else {
               local6148_5.flag = true;
               int local6148_7 = local6148_5 == ItemHelperModule.ItemActionType.HEAL_POTION
                  ? InventoryUtils.findOffhandSlot(this::matchesCriteria2)
                  : InventoryUtils.findInventorySlot(local6148_5.item2);
               if (local6148_7 != -1 && local6148_5.intVal2 < 0) {
                  if (local6148_7 <= 8) {
                     local6148_5.intVal2 = InventoryUtils.getSelectedSlot();
                     mc.player.getInventory().setSelectedSlot(local6148_7);
                  } else if (mc.player.currentScreenHandler == mc.player.playerScreenHandler && !InventoryActionQueue.getInstance().hasPendingActions()) {
                     local6148_5.intVal2 = InventoryUtils.getSelectedSlot();
                     local6148_5.intVal = local6148_7;
                     InventoryActionQueue.getInstance().queueSlotSwap(local6148_7, local6148_5.intVal2, 1);
                  }
               }

               if (mc.player.getMainHandStack().getItem() == local6148_5.item2) {
                  if (local6148_5 == ItemHelperModule.ItemActionType.CROSSBOW
                     && mc.player.isUsingItem()
                     && (
                        CrossbowItem.isCharged(mc.player.getMainHandStack())
                           || mc.player.getItemUseTime() > CrossbowItem.getPullTime(mc.player.getMainHandStack(), mc.player) + 3
                     )) {
                     if (mc.player.isUsingItem()) {
                        mc.interactionManager.stopUsingItem(mc.player);
                     }

                     mc.options.useKey.setPressed(false);
                  } else {
                     mc.options.useKey.setPressed(true);
                     if (this.releaseOnComplete.isChecked() && local6148_5 != ItemHelperModule.ItemActionType.CROSSBOW && mc.player.getItemUseTime() >= 33) {
                        mc.interactionManager.stopUsingItem(mc.player);
                        mc.options.useKey.setPressed(false);
                        local6148_5.flag = false;
                     }
                  }
               }
            }
         }
      }
   };
   public final EventListener<ContainerRenderEvent> containerRenderListener = containerRenderEvent -> {
      if (this.checkState()) {
         for (Slot slot : containerRenderEvent.getHandledScreen().getScreenHandler().slots) {
            if (slot.hasStack()) {
               int local6153_2 = this.computeInt(slot.getStack());
               if (local6153_2 != -1) {
                  containerRenderEvent.getDrawContext().fill(slot.x, slot.y, slot.x + 16, slot.y + 16, -2147483648 | local6153_2);
               }
            }
         }
      }
   };

   public int computeInt(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return -1;
      } else if (this.enchantedApple.isChecked() && itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
         return 58879;
      } else if (this.healingPotion.isChecked() && this.matchesCriteria2(itemStack)) {
         return itemStack.get(DataComponentTypes.POTION_CONTENTS).getColor() & 16777215;
      } else if (this.goldenApple.isChecked() && itemStack.getItem() == Items.GOLDEN_APPLE) {
         return 14725168;
      } else {
         return this.chorusFruit.isChecked() && itemStack.getItem() == Items.CHORUS_FRUIT ? 10049023 : -1;
      }
   }

   private ItemHelperModule() {
      this.addSetting(new SectionSetting("Предметы"));

      for (ItemHelperModule.ItemActionType local6146_4 : ItemHelperModule.ItemActionType.values()) {
         local6146_4.keybindSetting = this.addSetting(new KeybindSetting(local6146_4.strVal, "Держи возьмёт предмет в руку и использует."));
      }

      this.addSetting(new SectionSetting("Подсвечивание"));
      this.numberSetting = this.addSetting(
         new NumberSetting("Подсвечивать при здоровье", "Подсвечивает лечащие предметы, когда здоровье не выше этого.")
            .withRange(0.0F, 20.0F)
            .withStep(1.0F)
            .setValue(8.0F)
      );
      this.healingPotion = this.addSetting(new BooleanSetting("Зелье исцеление", "Подсвечивает зелья мгновенного исцеления.", false));
      this.enchantedApple = this.addSetting(new BooleanSetting("Чарка", "Подсвечивает зачарованные яблоки.", false));
      this.goldenApple = this.addSetting(new BooleanSetting("Гепл", "Подсвечивает золотые яблоки.", false));
      this.chorusFruit = this.addSetting(new BooleanSetting("Хорус", "Подсвечивает плоды хоруса.", false));
      this.releaseOnComplete = this.addSetting(new BooleanSetting("Отжимать при выполнении", "Сам отпускает использование, когда предмет доеден.", true));
   }

   public int computeInt2() {
      if (this.isEnabled() && mc.player != null) {
         for (ItemHelperModule.ItemActionType local6147_4 : ItemHelperModule.ItemActionType.values()) {
            if (local6147_4.flag
               && local6147_4.intVal == -1
               && local6147_4.intVal2 != -1
               && local6147_4.intVal2 != InventoryUtils.getSelectedSlot()) {
               return local6147_4.intVal2;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public boolean matchesCriteria(ItemStack itemStack) {
      return mc.world != null && itemStack.getItem() == Items.MACE
         ? EnchantmentHelper.getLevel(mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.BREACH), itemStack)
               >= 3
            && EnchantmentHelper.getLevel(
                  mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.DENSITY), itemStack
               )
               >= 5
         : false;
   }

   public static ItemHelperModule getInstance() {
      if (itemHelperModule == null) {
         itemHelperModule = new ItemHelperModule();
      }

      return itemHelperModule;
   }

   public boolean checkState() {
      return mc.player != null
         && (
            mc.player.getHealth() <= this.numberSetting.floatValue()
               || ServerHelperModule.getInstance().isEnabled() && ServerHelperModule.getInstance().modeSetting.isSelected("SpookyTime")
         );
   }

   public void renderElement(DrawContext drawContext) {
      if (this.checkState()) {
         int local6152_2 = drawContext.getScaledWindowWidth() / 2 - 91;

         for (int index = 0; index < 9; index++) {
            int local6152_3 = this.computeInt(mc.player.getInventory().getStack(index));
            if (local6152_3 != -1) {
               drawContext.fill(
                  local6152_2 + 3 + index * 20,
                  drawContext.getScaledWindowHeight() - 19,
                  local6152_2 + 3 + index * 20 + 16,
                  drawContext.getScaledWindowHeight() - 3,
                  -2147483648 | local6152_3
               );
            }
         }
      }
   }

   public ItemHelperModule.ItemActionType[] getNested0503() {
      return ItemHelperModule.ItemActionType.values();
   }

   private boolean matchesCriteria2(ItemStack itemStack) {
      if (itemStack.getItem() == Items.POTION && itemStack.get(DataComponentTypes.POTION_CONTENTS) != null) {
         for (StatusEffectInstance statusEffectInstance : itemStack.get(DataComponentTypes.POTION_CONTENTS).getEffects()) {
            if (statusEffectInstance.getEffectType() == StatusEffects.INSTANT_HEALTH) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean checkState2() {
      if (!this.isEnabled()) {
         return false;
      }

      for (ItemHelperModule.ItemActionType local6156_4 : ItemHelperModule.ItemActionType.values()) {
         if (local6156_4.flag) {
            return true;
         }
      }

      return false;
   }

   private void processData(ItemHelperModule.ItemActionType local6157_1) {
      if (local6157_1.intVal2 != -1) {
         if (local6157_1.intVal == -1) {
            mc.player.getInventory().setSelectedSlot(local6157_1.intVal2);
            local6157_1.intVal2 = -1;
         } else if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
            InventoryActionQueue.getInstance().queueSlotSwap(local6157_1.intVal, local6157_1.intVal2, 1);
            local6157_1.intVal = -1;
            local6157_1.intVal2 = -1;
         }
      }
   }

   public enum ItemActionType {
      CROSSBOW(Items.CROSSBOW, "Арбалет"),
      CHORUS_FRUIT(Items.CHORUS_FRUIT, "Плод хоруса"),
      GOLDEN_APPLE(Items.GOLDEN_APPLE, "Золотое яблоко"),
      HEAL_POTION(Items.POTION, "Зелье исцеления"),
      ENCHANTED_GOLDEN_APPLE(Items.ENCHANTED_GOLDEN_APPLE, "Зачар. яблоко");

      final Item item2;
      final String strVal;
      KeybindSetting keybindSetting;
      boolean flag;
      boolean flag2;
      int intVal = -1;
      int intVal2 = -1;

      ItemActionType(Item item, String local6160_4) {
         this.item2 = item;
         this.strVal = local6160_4;
      }

      public boolean matchesCriteria3(ItemStack itemStack) {
         if (itemStack.isEmpty()) {
            return false;
         } else {
            return this == HEAL_POTION ? ItemHelperModule.getInstance().matchesCriteria2(itemStack) : itemStack.getItem() == this.item2;
         }
      }

         public Item getItem2() {
         return this.item2;
      }

         public KeybindSetting getKeybindSetting() {
         return this.keybindSetting;
      }
   }
}
