package greed.client.module.misc;

import greed.client.event.EventListener;
import greed.client.setting.LegacyNumberSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;

@ModuleInfo(name = "AutoEat", description = "Сам ест из хотбара, когда голод падает ниже порога.", category = ModuleCategory.MISC)
public final class AutoEatModule extends Module {
   private static AutoEatModule autoEatModule;
   public LegacyNumberSetting legacyNumberSetting;
   private int intVal = -1;
   private boolean flag;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null
         && mc.world != null
         && mc.interactionManager != null
         && !mc.player.isCreative()
         && !mc.player.isSpectator()) {
         boolean local6349_2 = mc.player.getHungerManager().getFoodLevel() <= this.legacyNumberSetting.intValue();
         if (!this.flag) {
            if (!local6349_2) {
               return;
            }

            int local6349_3 = this.computeInt();
            if (local6349_3 == -1) {
               return;
            }

            this.intVal = InventoryUtils.getSelectedSlot();
            if (local6349_3 != this.intVal) {
               InventoryUtils.setSelectedSlot(local6349_3, false);
            }

            this.flag = true;
         }

         if (!mc.player.getInventory().getStack(InventoryUtils.getSelectedSlot()).contains(DataComponentTypes.FOOD)) {
            int local6349_3 = this.computeInt();
            if (local6349_3 == -1) {
               this.updateState();
               return;
            }

            InventoryUtils.setSelectedSlot(local6349_3, false);
         }

         if (!mc.player.isUsingItem()
            && mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND) instanceof Success success
            && success.swingSource() == SwingSource.CLIENT) {
            mc.player.swingHand(Hand.MAIN_HAND);
         }

         if (!local6349_2 && !mc.player.isUsingItem()) {
            this.updateState();
         }
      } else {
         this.updateState();
      }
   };

   private int computeInt() {
      int local6350_1 = -1;
      int local6350_2 = 0;

      for (int index = 0; index < 9; index++) {
         ItemStack stack = mc.player.getInventory().getStack(index);
         FoodComponent foodComponent = stack.get(DataComponentTypes.FOOD);
         if (foodComponent != null
            && !stack.isOf(Items.ROTTEN_FLESH)
            && !stack.isOf(Items.SPIDER_EYE)
            && !stack.isOf(Items.POISONOUS_POTATO)
            && !stack.isOf(Items.PUFFERFISH)
            && !stack.isOf(Items.CHORUS_FRUIT)
            && foodComponent.nutrition() > local6350_2) {
            local6350_2 = foodComponent.nutrition();
            local6350_1 = index;
         }
      }

      return local6350_1;
   }

   @Override
   protected void onDisable() {
      this.updateState();
   }

   private void updateState() {
      if (this.flag) {
         if (mc.player != null && mc.interactionManager != null && mc.player.isUsingItem()) {
            mc.interactionManager.stopUsingItem(mc.player);
         }

         int local6352_1 = this.intVal;
         if (local6352_1 != -1 && local6352_1 != InventoryUtils.getSelectedSlot()) {
            InventoryUtils.setSelectedSlot(local6352_1, false);
         }
      }

      this.flag = false;
      this.intVal = -1;
   }

   private AutoEatModule() {
      this.legacyNumberSetting = this.addSetting(new LegacyNumberSetting("Голод", "Есть, когда уровень голода не выше этого.", 16.0, 1.0, 19.0, 1.0));
   }

   public static AutoEatModule getInstance() {
      if (autoEatModule == null) {
         autoEatModule = new AutoEatModule();
      }

      return autoEatModule;
   }
}
