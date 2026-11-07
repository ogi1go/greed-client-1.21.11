package greed.client.module.combat;

import greed.client.util.AuctionItemCatalog;
import greed.client.setting.BooleanSetting;
import greed.client.util.ClientChat;
import greed.client.event.EventListener;
import greed.client.util.InputBinding;
import greed.client.setting.KeybindSetting;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.util.InventoryActionQueue;
import greed.client.ui.hud.NotificationHud;
import greed.client.util.EntityUtils;
import greed.client.util.InventorySlotManager;
import greed.client.storage.AutoBuyItem;
import greed.client.event.SlotClickEvent;
import greed.client.ui.SwapWheelScreen;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "AutoSwap", description = "Свапает выбранные предметы в левую руку по клавише.", category = ModuleCategory.COMBAT)
public final class AutoSwapModule extends Module {
   private static AutoSwapModule autoSwapModule;
   public ModeSetting swapMode;
   public ModeSetting firstItem;
   public ModeSetting secondItem;
   public KeybindSetting cerberusKey;
   public KeybindSetting vortexKey;
   public KeybindSetting wheelKey;
   public KeybindSetting swapKey;
   public BooleanSetting booleanSetting;
   private boolean swapKeyPressed;
   private boolean wheelKeyPressed;
   private Item activeSwapItem;
   private ItemStack savedOffhandItem = ItemStack.EMPTY;
   private boolean cerberusKeyPressed;
   private boolean vortexKeyPressed;
   private boolean isSwapActive;
   private int previousSlotIndex = -1;
   private final ItemStack[] configuredSwapItems = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
   private final EventListener<PlayerTickEvent> swapTickListener = playerTickEvent -> {
      if (mc.player != null) {
         boolean local4049_2 = this.swapMode.isSelected("Двойной свап")
            && this.swapKey.isBound()
            && this.swapKey.getBinding().isPressed(mc.getWindow().getHandle());
         if (local4049_2 && !this.swapKeyPressed) {
            this.performDoubleSwap();
         }

         this.swapKeyPressed = local4049_2;
         boolean local4049_3 = this.cerberusKey.isBound() && this.cerberusKey.getBinding().isPressed(mc.getWindow().getHandle());
         if (local4049_3 && !this.cerberusKeyPressed) {
            this.processData("цербер");
         }

         this.cerberusKeyPressed = local4049_3;
         boolean local4049_4 = this.vortexKey.isBound() && this.vortexKey.getBinding().isPressed(mc.getWindow().getHandle());
         if (local4049_4 && !this.vortexKeyPressed) {
            this.processData("вихр");
         }

         this.vortexKeyPressed = local4049_4;
         boolean local4049_5 = this.swapMode.isSelected("Тройной свап")
            && this.wheelKey.isBound()
            && this.checkState()
            && (mc.currentScreen == null || mc.currentScreen instanceof SwapWheelScreen);
         if (local4049_5
            && !this.isSwapActive
            && (mc.currentScreen == null || mc.currentScreen instanceof SwapWheelScreen swapWheelScreen && swapWheelScreen.closing())) {
            mc.setScreen(new SwapWheelScreen(this.configuredSwapItems, local4021_1 -> this.processData3(local4021_1)));
         }

         if (!local4049_5 && this.isSwapActive && mc.currentScreen instanceof SwapWheelScreen swapWheelScreen2) {
            if (!swapWheelScreen2.pickerOpen() && swapWheelScreen2.hoveredSlot() != -1 && !this.configuredSwapItems[swapWheelScreen2.hoveredSlot()].isEmpty()) {
               this.processData(Formatting.strip(this.configuredSwapItems[swapWheelScreen2.hoveredSlot()].getName().getString()).toLowerCase());
            }

            if (!swapWheelScreen2.pickerOpen()) {
               swapWheelScreen2.startClosing();
            }
         }

         this.isSwapActive = local4049_5;
      }
   };
   private final EventListener<SlotClickEvent> swapSlotClickListener = slotClickEvent -> {
      if (this.previousSlotIndex != -1
         && mc.player != null
         && slotClickEvent.getSlotId() >= 0
         && slotClickEvent.getSlotId() < mc.player.currentScreenHandler.slots.size()) {
         slotClickEvent.processData(true);
         if (!mc.player.currentScreenHandler.getSlot(slotClickEvent.getSlotId()).getStack().isEmpty()) {
            this.configuredSwapItems[this.previousSlotIndex] = mc.player.currentScreenHandler.getSlot(slotClickEvent.getSlotId()).getStack().copy();
         }

         this.previousSlotIndex = -1;
         mc.execute(() -> mc.setScreen(null));
      }
   };

   private MutableText getMutableText(ItemStack itemStack) {
      MutableText mutableText = Text.empty();
      mutableText.append(
         itemStack.getName()
            .copy()
            .styled(style -> style.getColor() == null ? style.withColor(itemStack.getRarity().getFormatting()) : style)
      );
      if (itemStack.getCount() > 1) {
         mutableText.append(Text.literal(" [").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
         mutableText.append(Text.literal("x" + itemStack.getCount()).setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
         mutableText.append(Text.literal("]").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
      }

      return mutableText;
   }

   public static AutoSwapModule getInstance() {
      if (autoSwapModule == null) {
         autoSwapModule = new AutoSwapModule();
      }

      return autoSwapModule;
   }

   private int computeInt() {
      int local4024_1 = InventoryUtils.findSlotMatching(
         itemStack -> EntityUtils.isDonationTotem(itemStack) && !this.isConfiguredSwapItem(itemStack) && !this.matchesCriteria(itemStack), 0, 35
      );
      if (local4024_1 != -1) {
         return local4024_1;
      }

      int local4024_2 = InventoryUtils.findSlotMatching(
         itemStack2 -> itemStack2.getItem() == Items.TOTEM_OF_UNDYING
            && itemStack2.hasEnchantments()
            && !this.isConfiguredSwapItem(itemStack2)
            && !this.matchesCriteria(itemStack2),
         0,
         35
      );
      return local4024_2 != -1
         ? local4024_2
         : InventoryUtils.findSlotMatching(
            itemStack3 -> itemStack3.getItem() == Items.TOTEM_OF_UNDYING && !this.isConfiguredSwapItem(itemStack3) && !this.matchesCriteria(itemStack3), 0, 35
         );
   }

   public boolean isConfiguredSwapItem(ItemStack itemStack) {
      AuctionItemCatalog.updateState();
      return Formatting.strip(itemStack.getName().getString()).toLowerCase().contains("цербер")
         || itemStack.getItem() == Items.TOTEM_OF_UNDYING
            && AuctionItemCatalog.getAutoBuyItem("Талисман Вихря") != null
            && AutoBuyItem.CHECKER.test(itemStack, AuctionItemCatalog.getAutoBuyItem("Талисман Вихря"));
   }

   private void processData(String local4027_1) {
      if (!this.checkState2()) {
         int local4027_2 = InventoryUtils.findSlotMatching(
            itemStack -> Formatting.strip(itemStack.getName().getString()).toLowerCase().contains(local4027_1), 0, 35
         );
         if (local4027_2 != -1) {
            this.activeSwapItem = mc.player.getInventory().getStack(local4027_2).getItem();
            if (this.booleanSetting.isChecked()) {
               NotificationHud.notifyWithText(
                  this.category.getIcon(), "Свап", this.getMutableText(mc.player.getInventory().getStack(local4027_2)), true
               );
               ClientChat.info(
                  Text.literal("Свап ")
                     .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                     .append(this.getMutableText(mc.player.getInventory().getStack(local4027_2)))
               );
            }

            if (!this.isConfiguredSwapItem(mc.player.getOffHandStack())) {
               this.savedOffhandItem = mc.player.getOffHandStack().copy();
            }

            InventoryActionQueue.getInstance().processData5(local4027_2, 40, 1);
         }
      }
   }

   private boolean checkState() {
      return this.wheelKey.getBinding().getDevice() == InputBinding.DeviceType.MOUSE
         ? GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), this.wheelKey.getBinding().getCode()) == 1
         : GLFW.glfwGetKey(mc.getWindow().getHandle(), this.wheelKey.getBinding().getCode()) == 1;
   }

   private boolean checkState2() {
      return mc.player == null
         || AutoTotemModule.getInstance().isTotemActive()
         || InventoryActionQueue.getInstance().hasPendingActions()
         || InventorySlotManager.getInstance().isLocked();
   }

   private int computeInt2(String local4030_1) {
      return switch (local4030_1) {
         case "Золотое яблоко" -> InventoryUtils.findSlotMatching(
            itemStack -> itemStack.getItem() == Items.GOLDEN_APPLE && !this.isConfiguredSwapItem(itemStack), 0, 35
         );
         case "Зачарованное золотое яблоко" -> InventoryUtils.findSlotMatching(
            itemStack2 -> itemStack2.getItem() == Items.ENCHANTED_GOLDEN_APPLE && !this.isConfiguredSwapItem(itemStack2), 0, 35
         );
         case "Тотем" -> this.computeInt();
         case "Щит" -> InventoryUtils.findSlotMatching(itemStack3 -> itemStack3.getItem() == Items.SHIELD && !this.isConfiguredSwapItem(itemStack3), 0, 35);
         case "Шар" -> this.computeInt3();
         default -> -1;
      };
   }

   public void processData2(int local4033_1, ItemStack itemStack) {
      this.configuredSwapItems[local4033_1] = itemStack;
   }

   public ItemStack getItemStack(int local4035_1) {
      return this.configuredSwapItems[local4035_1];
   }

   private boolean matchesCriteria(ItemStack itemStack) {
      return AutoTotemModule.getInstance().takeEnchantedWithSave.isChecked()
         && !AutoTotemModule.getInstance().numericTextSetting.getText().isEmpty()
         && AutoTotemModule.getInstance().isCustomTotem(itemStack);
   }

   private AutoSwapModule() {
      this.swapMode = this.addSetting(
         new ModeSetting("Режим свапа", "Двойной цикл из двух предметов, тройной колесо на три слота.", "Двойной свап", "Двойной свап", "Тройной свап")
      );
      this.firstItem = this.addSetting(
         new ModeSetting(
            "Первый предмет", "Первый предмет в цикле свапа.", "Золотое яблоко", "Золотое яблоко", "Зачарованное золотое яблоко", "Тотем", "Щит", "Шар"
         )
      );
      this.secondItem = this.addSetting(
         new ModeSetting(
            "Второй предмет", "Второй предмет в цикле свапа.", "Тотем", "Золотое яблоко", "Зачарованное золотое яблоко", "Тотем", "Щит", "Шар"
         )
      );
      this.swapKey = this.addSetting(new KeybindSetting("Свап", "Клавиша свапа предмета."));
      this.cerberusKey = this.addSetting(new KeybindSetting("Церберка", "Свапает голову цербера в левую руку."));
      this.vortexKey = this.addSetting(new KeybindSetting("Вихрь", "Свапает вихрь в левую руку."));
      this.wheelKey = this.addSetting(new KeybindSetting("Колесо свапа", "Удерживай, наводи на слот и отпускай, чтобы свапнуть выбранный предмет."));
      this.firstItem.withVisibility(() -> this.swapMode.isSelected("Двойной свап"));
      this.secondItem.withVisibility(() -> this.swapMode.isSelected("Двойной свап"));
      this.swapKey.withVisibility(() -> this.swapMode.isSelected("Двойной свап"));
      this.wheelKey.withVisibility(() -> this.swapMode.isSelected("Тройной свап"));
      this.booleanSetting = this.addSetting(new BooleanSetting("Уведомлять о свапе", "Показывать нотификацию с именем предмета, на который свапнулся.", false));
   }

   private int computeInt3() {
      int local4040_1 = InventoryUtils.findSlotMatching(
         itemStack -> itemStack.getItem() == Items.PLAYER_HEAD
            && itemStack.contains(DataComponentTypes.ATTRIBUTE_MODIFIERS)
            && !itemStack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS).modifiers().isEmpty()
            && !this.isConfiguredSwapItem(itemStack)
            && !Formatting.strip(itemStack.getName().getString()).toLowerCase().contains("голова"),
         0,
         35
      );
      return local4040_1 != -1
         ? local4040_1
         : InventoryUtils.findSlotMatching(
            itemStack2 -> itemStack2.getItem() == Items.PLAYER_HEAD
               && itemStack2.contains(DataComponentTypes.ATTRIBUTE_MODIFIERS)
               && !itemStack2.get(DataComponentTypes.ATTRIBUTE_MODIFIERS).modifiers().isEmpty()
               && !this.isConfiguredSwapItem(itemStack2),
            0,
            35
         );
   }

   private void performDoubleSwap() {
      if (!this.checkState2()) {
         if (this.isConfiguredSwapItem(mc.player.getOffHandStack()) && !this.savedOffhandItem.isEmpty()) {
            int local4042_1 = InventoryUtils.findSlotMatching(itemStack -> ItemStack.areItemsAndComponentsEqual(itemStack, this.savedOffhandItem), 0, 35);
            if (local4042_1 != -1) {
               this.savedOffhandItem = ItemStack.EMPTY;
               this.activeSwapItem = mc.player.getInventory().getStack(local4042_1).getItem();
               if (this.booleanSetting.isChecked()) {
                  NotificationHud.notifyWithText(
                     this.category.getIcon(), "Свап", this.getMutableText(mc.player.getInventory().getStack(local4042_1)), true
                  );
                  ClientChat.info(
                     Text.literal("Свап ")
                        .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                        .append(this.getMutableText(mc.player.getInventory().getStack(local4042_1)))
                  );
               }

               InventoryActionQueue.getInstance().processData5(local4042_1, 40, 1);
               return;
            }
         }

         if (mc.player.getOffHandStack().getItem() != this.activeSwapItem) {
            this.wheelKeyPressed = this.computeInt2(this.firstItem.getSelectedOption()) == -1;
         }

         int local4042_1 = this.computeInt2(this.wheelKeyPressed ? this.secondItem.getSelectedOption() : this.firstItem.getSelectedOption());
         if (local4042_1 != -1) {
            this.wheelKeyPressed = !this.wheelKeyPressed;
            this.activeSwapItem = mc.player.getInventory().getStack(local4042_1).getItem();
            if (this.booleanSetting.isChecked()) {
               NotificationHud.notifyWithText(
                  this.category.getIcon(), "Свап", this.getMutableText(mc.player.getInventory().getStack(local4042_1)), true
               );
               ClientChat.info(
                  Text.literal("Свап ")
                     .setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                     .append(this.getMutableText(mc.player.getInventory().getStack(local4042_1)))
               );
            }

            InventoryActionQueue.getInstance().processData5(local4042_1, 40, 1);
         }
      }
   }

   public void processData3(int local4044_1) {
      this.previousSlotIndex = local4044_1;
      mc.setScreen(new InventoryScreen(mc.player));
   }
}
