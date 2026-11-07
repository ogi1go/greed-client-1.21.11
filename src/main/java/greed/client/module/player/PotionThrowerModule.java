package greed.client.module.player;

import greed.client.util.AuctionItemCatalog;
import greed.client.event.EventListener;
import greed.client.util.InputBinding;
import greed.client.setting.KeybindSetting;
import greed.client.setting.LegacyNumberSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PlayerTickEvent;
import greed.client.ui.PotionWheelScreen;
import greed.client.util.InventoryUtils;
import greed.client.setting.NumericTextSetting;
import greed.client.util.InventoryActionQueue;
import greed.client.util.InventorySlotManager;
import greed.client.storage.AutoBuyItem;

import java.util.ArrayList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "PotionThrower", description = "Быстрое метание бафов через колесо.", category = ModuleCategory.PLAYER)
public final class PotionThrowerModule extends Module {
   private static PotionThrowerModule potionThrowerModule;
   public KeybindSetting keybindSetting;
   public LegacyNumberSetting legacyNumberSetting;
   public NumericTextSetting numericTextSetting;
   private final ArrayList<String> list = new ArrayList<>();
   private final PotionWheelScreen potionWheelScreen = new PotionWheelScreen();
   private boolean flag;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null) {
         this.updateState2();
      }
   };

   public boolean checkState() {
      return this.keybindSetting.isBound() && this.checkState2();
   }

   public void updateState() {
      String local4101_1 = String.join(";", this.list);
      ArrayList<String> local4101_2 = new ArrayList<>(this.list);
      this.numericTextSetting.setValue(local4101_1);
      this.list.clear();
      this.list.addAll(local4101_2);
   }

   public void processData(AutoBuyItem autoBuyItem) {
      if (autoBuyItem != null
         && mc.player != null
         && mc.interactionManager != null
         && !InventorySlotManager.getInstance().isLocked()
         && !InventoryActionQueue.getInstance().hasPendingActions()) {
         if (InventoryUtils.findOffhandSlot(itemStack -> AutoBuyItem.CHECKER.test(itemStack, autoBuyItem)) != -1) {
            ServerHelperModule.getInstance().processData3(autoBuyItem.name());
            InventorySlotManager.getInstance()
               .processData3(
                  mc.player
                     .getInventory()
                     .getStack(InventoryUtils.findOffhandSlot(itemStack2 -> AutoBuyItem.CHECKER.test(itemStack2, autoBuyItem)))
                     .copy(),
                  true
               );
         } else if (InventoryUtils.findOffhandSlot(itemStack3 -> this.computeInt(itemStack3, autoBuyItem) != -1) != -1
            && InventoryUtils.findEmptySlot(0, 35) != -1) {
            ServerHelperModule.getInstance().processData3(autoBuyItem.name());
            InventorySlotManager.getInstance()
               .processData3(
                  mc.player
                     .getInventory()
                     .getStack(InventoryUtils.findOffhandSlot(itemStack4 -> this.computeInt(itemStack4, autoBuyItem) != -1))
                     .get(DataComponentTypes.BUNDLE_CONTENTS)
                     .get(
                        this.computeInt(
                           mc.player
                              .getInventory()
                              .getStack(InventoryUtils.findOffhandSlot(itemStack5 -> this.computeInt(itemStack5, autoBuyItem) != -1)),
                           autoBuyItem
                        )
                     )
                     .copy(),
                  true
               );
         }
      }
   }

   public AutoBuyItem getAutoBuyItem(int local4104_1) {
      if (local4104_1 >= 0 && local4104_1 < this.getArrayList().size() && !this.getArrayList().get(local4104_1).isEmpty()) {
         for (AutoBuyItem autoBuyItem : AuctionItemCatalog.getArrayList2()) {
            if (autoBuyItem.name().equals(this.getArrayList().get(local4104_1))) {
               return autoBuyItem;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public ArrayList<String> getArrayList() {
      if (this.list.isEmpty()) {
         for (String local4106_4 : this.numericTextSetting.getValue().split(";", -1)) {
            this.list.add(local4106_4);
         }
      }

      int local4106_1 = Math.max(2, Math.min(7, this.legacyNumberSetting.intValue()));

      while (this.list.size() < local4106_1) {
         this.list.add("");
      }

      while (this.list.size() > local4106_1) {
         this.list.remove(this.list.size() - 1);
      }

      return this.list;
   }

   private int computeInt(ItemStack itemStack, AutoBuyItem autoBuyItem) {
      if (itemStack.get(DataComponentTypes.BUNDLE_CONTENTS) == null) {
         return -1;
      }

      for (int index = 0; index < itemStack.get(DataComponentTypes.BUNDLE_CONTENTS).size(); index++) {
         if (AutoBuyItem.CHECKER.test(itemStack.get(DataComponentTypes.BUNDLE_CONTENTS).get(index), autoBuyItem)) {
            return index;
         }
      }

      return -1;
   }

   public ArrayList<AutoBuyItem> getArrayList2() {
      ArrayList<AutoBuyItem> local4109_1 = new ArrayList<>();
      AuctionItemCatalog.updateState();

      for (AutoBuyItem autoBuyItem : AuctionItemCatalog.getArrayList2()) {
         if (autoBuyItem.item() == Items.SPLASH_POTION) {
            local4109_1.add(autoBuyItem);
         }
      }

      return local4109_1;
   }

   private boolean checkState2() {
      return this.keybindSetting.getBinding().getDevice() == InputBinding.DeviceType.MOUSE
         ? GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), this.keybindSetting.getBinding().getCode()) == 1
         : GLFW.glfwGetKey(mc.getWindow().getHandle(), this.keybindSetting.getBinding().getCode()) == 1;
   }

   public static PotionThrowerModule getInstance() {
      if (potionThrowerModule == null) {
         potionThrowerModule = new PotionThrowerModule();
      }

      return potionThrowerModule;
   }

   private PotionThrowerModule() {
      this.keybindSetting = this.addSetting(
         new KeybindSetting("Открыть колесо", "Удерживай клавишу, наводи на зелье и отпускай, чтобы кинуть.", InputBinding.keyboardKey(86))
      );
      this.legacyNumberSetting = this.addSetting(new LegacyNumberSetting("Количество зелий", "Сколько секторов в колесе.", 3.0, 2.0, 7.0, 1.0));
      this.numericTextSetting = this.addSetting(new NumericTextSetting("Слоты колеса", "", "", 512));
      this.numericTextSetting.withVisibility(() -> false);
      this.numericTextSetting.onChange(this.list::clear);
   }

   private void updateState2() {
      if (this.keybindSetting.isBound()) {
         boolean local4114_1 = this.checkState2() && (mc.currentScreen == null || mc.currentScreen == this.potionWheelScreen);
         if (local4114_1 && !this.flag && (mc.currentScreen == null || this.potionWheelScreen.closing())) {
            mc.setScreen(this.potionWheelScreen);
         }

         if (!local4114_1 && this.flag && mc.currentScreen == this.potionWheelScreen && !this.potionWheelScreen.pickerOpen() && !this.potionWheelScreen.closing()) {
            if (this.potionWheelScreen.draggedSlot() != -1) {
               this.potionWheelScreen.releaseHeldSlot(this.potionWheelScreen.hoveredSlot());
            } else if (this.potionWheelScreen.hoveredSlot() != -1) {
               this.processData(this.getAutoBuyItem(this.potionWheelScreen.hoveredSlot()));
            }

            this.potionWheelScreen.startClosing();
         }

         this.flag = local4114_1;
      }
   }
}
