package greed.client.module.player;

import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.setting.KeybindSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.util.InventoryActionQueue;
import greed.client.ui.hud.NotificationHud;
import greed.client.util.InventorySlotManager;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;

import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

@ModuleInfo(name = "AutoExp", description = "Пока зажат бинд смотрит в пол, кидает пузырьки опыта и чинит броню мендингом.", category = ModuleCategory.PLAYER)
public final class AutoExpModule extends Module {
   private static AutoExpModule autoExpModule;
   private static final EquipmentSlot[] equipmentSlotArray = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   public KeybindSetting keybindSetting;
   public BooleanSetting booleanSetting;
   private boolean flag;
   private int intVal = -1;
   private int intVal2 = -1;
   private int intVal3 = -1;
   private boolean flag2;
   private EquipmentSlot equipmentSlot;
   private int intVal4;
   private final boolean[] flagArray = new boolean[4];
   private final Item[] itemArray = new Item[4];
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && mc.interactionManager != null) {
         boolean local6755_2 = this.keybindSetting.isBound() && this.keybindSetting.getBinding().isPressed(mc.getWindow().getHandle());
         if (!local6755_2) {
            if (this.flag) {
               this.updateState2();
               this.updateState();
            } else if (!InventoryActionQueue.getInstance().hasPendingActions()
               && !InventorySlotManager.getInstance().isLocked()
               && mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
               if (this.intVal3 != -1) {
                  this.processData2(this.intVal2);
               }

               if (this.flagArray[0] || this.flagArray[1] || this.flagArray[2] || this.flagArray[3]) {
                  this.updateState();
               }
            }

            this.flag = false;
         } else {
            if (!this.flag) {
               for (int index = 0; index < 4; index++) {
                  if (this.itemArray[index] == null) {
                     this.itemArray[index] = mc.player.getEquippedStack(equipmentSlotArray[index]).getItem();
                  }
               }
            }

            this.flag = true;
            RotationManager.getRotationManager()
               .setRotationSimple(new RotationAngles(RotationAngles.getCameraYaw(), 90.0F), new RotationSpeed(360.0F, 360.0F, 44.0F, 44.0F));
            if (this.booleanSetting.isChecked()) {
               this.updateState5();
            }

            if (mc.player.getPitch() > 85.0F) {
               this.updateState3();
            }
         }
      }
   };

   public static AutoExpModule getInstance() {
      if (autoExpModule == null) {
         autoExpModule = new AutoExpModule();
      }

      return autoExpModule;
   }

   private void processData(int local6758_1, Item item) {
      if (mc.player != null && mc.interactionManager != null && !this.flag) {
         if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
            if (mc.player.getEquippedStack(equipmentSlotArray[local6758_1]).getItem() != item) {
               if (item == Items.AIR) {
                  int local6758_3 = InventoryUtils.findEmptySlot(9, 35);
                  if (local6758_3 == -1) {
                     return;
                  }

                  InventoryActionQueue.getInstance().processData8(local6758_3, 5 + local6758_1, 1);
               } else {
                  int local6758_3 = InventoryUtils.findSlotMatching(itemStack -> itemStack.getItem() == item, 0, 35);
                  if (local6758_3 != -1) {
                     InventoryActionQueue.getInstance().processData8(local6758_3, 5 + local6758_1, 1);
                  }
               }
            }

            this.flagArray[local6758_1] = false;
            this.itemArray[local6758_1] = null;
         }
      }
   }

   private void updateState() {
      this.equipmentSlot = null;

      for (int index = 0; index < 4; index++) {
         if (!this.flagArray[index]) {
            this.itemArray[index] = null;
         } else if (this.itemArray[index] == null) {
            this.flagArray[index] = false;
         } else {
            this.processData(index, this.itemArray[index]);
         }
      }
   }

   private void updateState2() {
      this.flag2 = false;
      if (this.intVal != -1) {
         InventoryUtils.setSelectedSlot(this.intVal, false);
         this.intVal = -1;
      }

      if (this.intVal3 != -1) {
         this.processData2(this.intVal2);
      } else {
         this.intVal2 = -1;
      }
   }

   private void updateState3() {
      if (!mc.player.isUsingItem()) {
         if (this.intVal2 != -1) {
            if (this.intVal3 == -1) {
               if (!InventoryActionQueue.getInstance().hasPendingActions() && !InventorySlotManager.getInstance().isLocked()) {
                  this.intVal2 = -1;
               }
            } else if (InventoryUtils.getSelectedSlot() != this.intVal3) {
               InventoryUtils.setSelectedSlot(this.intVal3, false);
            } else if (mc.player.getMainHandStack().getItem() != Items.EXPERIENCE_BOTTLE) {
               if (!InventoryActionQueue.getInstance().hasPendingActions() && !InventorySlotManager.getInstance().isLocked()) {
                  this.updateState2();
               }
            } else if (!mc.player.getItemCooldownManager().isCoolingDown(mc.player.getMainHandStack())) {
               InventoryUtils.useItem(Hand.MAIN_HAND);
            }
         } else if (this.intVal == -1) {
            if (InventoryUtils.findHotbarSlot(Items.EXPERIENCE_BOTTLE) == -1) {
               if (InventoryUtils.findMainInventorySlot(Items.EXPERIENCE_BOTTLE) == -1) {
                  if (!InventoryUtils.extractItemFromBundle(Items.EXPERIENCE_BOTTLE) && !this.flag2) {
                     this.flag2 = true;
                     NotificationHud.notifyWithText(
                        this.category.getIcon(),
                        Items.EXPERIENCE_BOTTLE.getDefaultStack().getName().getString(),
                        Text.literal("нет в инвентаре").setStyle(Style.EMPTY.withColor(Formatting.RED)),
                        true
                     );
                  }
               } else {
                  this.intVal2 = InventoryUtils.findMainInventorySlot(Items.EXPERIENCE_BOTTLE);
                  this.intVal3 = InventoryUtils.getSelectedSlot();
                  InventoryActionQueue.getInstance().queueSlotSwap(this.intVal2, this.intVal3, 1);
               }
            } else {
               this.intVal = InventoryUtils.getSelectedSlot();
               InventoryUtils.setSelectedSlot(InventoryUtils.findHotbarSlot(Items.EXPERIENCE_BOTTLE), false);
            }
         } else if (mc.player.getMainHandStack().getItem() != Items.EXPERIENCE_BOTTLE) {
            InventoryUtils.setSelectedSlot(this.intVal, false);
            this.intVal = -1;
         } else if (!mc.player.getItemCooldownManager().isCoolingDown(mc.player.getMainHandStack())) {
            InventoryUtils.useItem(Hand.MAIN_HAND);
         }
      }
   }

   private AutoExpModule() {
      this.keybindSetting = this.addSetting(
         new KeybindSetting("Кидать опыт", "Пока зажат смотрим в пол и кидаем пузырьки опыта из мешка, инвентаря или хотбара, отпустил всё вернулось.")
      );
      this.booleanSetting = this.addSetting(
         new BooleanSetting("Быстрая починка", "Оставляет надетым только самый поломанный элемент брони и меняет его по мере починки.", false)
      );
   }

   private void processData2(int local6763_1) {
      if (this.intVal3 == -1) {
         this.intVal2 = -1;
      } else {
         InventoryActionQueue.getInstance().queueSlotSwap(local6763_1, this.intVal3, 1);
         this.intVal3 = -1;
         this.intVal2 = -1;
      }
   }

   private void updateState4() {
      int local6764_1 = 0;

      for (int index = 9; index <= 35; index++) {
         if (mc.player.getInventory().getStack(index).isEmpty()) {
            local6764_1++;
         }
      }

      boolean[] booleanArray = new boolean[4];

      while (local6764_1 > 0) {
         int local6764_4 = -1;
         float local6764_5 = -1.0F;

         for (int index2 = 0; index2 < 4; index2++) {
            if (!booleanArray[index2] && equipmentSlotArray[index2] != this.equipmentSlot) {
               ItemStack equippedStack = mc.player.getEquippedStack(equipmentSlotArray[index2]);
               if (!equippedStack.isEmpty()) {
                  float local6764_6 = (this.matchesCriteria(equippedStack) ? 10.0F : 0.0F)
                     + (float)(equippedStack.getMaxDamage() - equippedStack.getDamage()) / Math.max(1, equippedStack.getMaxDamage());
                  if (local6764_6 > local6764_5) {
                     local6764_5 = local6764_6;
                     local6764_4 = index2;
                  }
               }
            }
         }

         if (local6764_4 == -1) {
            return;
         }

         booleanArray[local6764_4] = true;
         local6764_1--;
         this.flagArray[local6764_4] = true;
         this.processData3(local6764_4);
      }
   }

   private void updateState5() {
      if (!InventoryActionQueue.getInstance().hasPendingActions() && !InventorySlotManager.getInstance().isLocked()) {
         if (this.equipmentSlot != null) {
            ItemStack equippedStack = mc.player.getEquippedStack(this.equipmentSlot);
            if (!this.matchesCriteria(equippedStack)
               || equippedStack.getMaxDamage() - equippedStack.getDamage() >= Math.max(this.intVal4 * 1.3F, this.intVal4 + equippedStack.getMaxDamage() * 0.1F)
               )
             {
               this.equipmentSlot = null;
            }
         }

         if (this.equipmentSlot == null) {
            int local6766_1 = -1;
            int local6766_2 = -1;
            float local6766_4 = Float.MAX_VALUE;

            for (int index = 0; index < 4; index++) {
               ItemStack equippedStack2 = mc.player.getEquippedStack(equipmentSlotArray[index]);
               int local6766_3 = -1;
               if (!this.matchesCriteria(equippedStack2)) {
                  Item item = this.itemArray[index];
                  if (!this.flagArray[index] || item == null || item == Items.AIR) {
                     continue;
                  }

                  local6766_3 = InventoryUtils.findSlotMatching(itemStack -> itemStack.getItem() == item && this.matchesCriteria(itemStack), 0, 35);
                  if (local6766_3 == -1) {
                     continue;
                  }

                  equippedStack2 = mc.player.getInventory().getStack(local6766_3);
               }

               float local6766_5 = (float)(equippedStack2.getMaxDamage() - equippedStack2.getDamage()) / Math.max(1, equippedStack2.getMaxDamage());
               if (local6766_5 < local6766_4) {
                  local6766_4 = local6766_5;
                  local6766_1 = index;
                  local6766_2 = local6766_3;
               }
            }

            if (local6766_1 == -1) {
               return;
            }

            this.equipmentSlot = equipmentSlotArray[local6766_1];
            if (local6766_2 != -1) {
               this.intVal4 = mc.player.getInventory().getStack(local6766_2).getMaxDamage()
                  - mc.player.getInventory().getStack(local6766_2).getDamage();
               this.flagArray[local6766_1] = true;
               InventoryActionQueue.getInstance().processData8(local6766_2, 5 + local6766_1, 1);
               return;
            }

            this.intVal4 = mc.player.getEquippedStack(this.equipmentSlot).getMaxDamage()
               - mc.player.getEquippedStack(this.equipmentSlot).getDamage();
         }

         this.updateState4();
      }
   }

   @Override
   protected void onDisable() {
      if (this.flag) {
         this.updateState2();
         this.updateState();
      }

      this.flag = false;
   }

   private boolean matchesCriteria(ItemStack itemStack) {
      return !itemStack.isEmpty()
         && itemStack.isDamaged()
         && EnchantmentHelper.hasAnyEnchantmentsWith(itemStack, EnchantmentEffectComponentTypes.REPAIR_WITH_XP);
   }

   private void processData3(int local6769_1) {
      if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler == mc.player.playerScreenHandler
         )
       {
         int local6769_2 = InventoryUtils.findEmptySlot(9, 35);
         if (local6769_2 != -1 && !mc.player.getEquippedStack(equipmentSlotArray[local6769_1]).isEmpty()) {
            InventoryActionQueue.getInstance().processData8(local6769_2, 5 + local6769_1, 1);
         }
      }
   }
}
