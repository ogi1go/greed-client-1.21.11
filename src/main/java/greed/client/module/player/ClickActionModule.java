package greed.client.module.player;

import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.storage.FriendStore;
import greed.client.util.InputBinding;
import greed.client.setting.KeybindSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.util.InventoryActionQueue;
import greed.client.ui.hud.NotificationHud;
import greed.client.util.InventorySlotManager;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

@ModuleInfo(name = "ClickAction", description = "Бросок привязанных предметов из хотбара или инвентаря по клавише.", category = ModuleCategory.PLAYER)
public final class ClickActionModule extends Module {
   private static ClickActionModule clickActionModule;
   public KeybindSetting enderPearlKey;
   public KeybindSetting expBottleKey;
   public BooleanSetting booleanSetting;
   public KeybindSetting friendKey;
   private boolean flag;
   private boolean flag2;
   private int intVal = -1;
   private int intVal2 = -1;
   private int intVal3 = -1;
   private boolean flag3;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && mc.interactionManager != null) {
         this.updateState3();
         if (mc.currentScreen == null && !mc.player.isUsingItem()) {
            if (this.enderPearlKey.getBinding().isPressed(mc.getWindow().getHandle()) && !this.flag) {
               this.processData(Items.ENDER_PEARL);
            }

            this.flag = this.enderPearlKey.getBinding().isPressed(mc.getWindow().getHandle());
            if (this.expBottleKey.getBinding().isPressed(mc.getWindow().getHandle())) {
               this.updateState2();
            } else {
               this.updateState();
            }
         } else {
            this.flag = this.enderPearlKey.getBinding().isPressed(mc.getWindow().getHandle());
            this.updateState();
         }
      }
   };

   private void processData(Item item) {
      if (!InventorySlotManager.getInstance().matchesCriteria2(item.getDefaultStack())) {
         NotificationHud.notifyWithText(
            this.category.getIcon(),
            item.getDefaultStack().getName().getString(),
            Text.literal("нет в инвентаре").setStyle(Style.EMPTY.withColor(Formatting.RED)),
            true
         );
      } else if (!mc.player.getItemCooldownManager().isCoolingDown(item.getDefaultStack())) {
         InventorySlotManager.getInstance().processData(item.getDefaultStack());
      }
   }

   private ClickActionModule() {
      this.enderPearlKey = this.addSetting(new KeybindSetting("Эндер жемчуг", "Кидает жемчуг по нажатию бинда."));
      this.expBottleKey = new KeybindSetting("Пузырьки опыта", "Кидает пузырьки, пока бинд зажат.");
      this.booleanSetting = this.addSetting(new BooleanSetting("Друг по клику", "Клик по игроку выбранной кнопкой добавляет или убирает его из друзей.", false));
      this.friendKey = this.addSetting(new KeybindSetting("Кнопка друга", "Какой кнопкой отмечать друга.", InputBinding.mouseButton(2)));
      this.friendKey.withVisibility(() -> this.booleanSetting.isChecked());
   }

   private void updateState() {
      this.flag3 = false;
      if (this.intVal != -1) {
         InventoryUtils.setSelectedSlot(this.intVal, false);
         this.intVal = -1;
      }

      if (this.intVal2 != -1) {
         if (this.intVal3 == -1) {
            if (!InventoryActionQueue.getInstance().hasPendingActions() && !InventorySlotManager.getInstance().isLocked()) {
               this.intVal2 = -1;
            }
         } else if (!InventoryActionQueue.getInstance().hasPendingActions()
            && !InventorySlotManager.getInstance().isLocked()
            && mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
            InventoryActionQueue.getInstance().queueSlotSwap(this.intVal2, this.intVal3, 1);
            this.intVal3 = -1;
            this.intVal2 = -1;
         }
      }
   }

   private void updateState2() {
      if (this.intVal2 != -1) {
         if (this.intVal3 == -1) {
            if (!InventoryActionQueue.getInstance().hasPendingActions() && !InventorySlotManager.getInstance().isLocked()) {
               this.intVal2 = -1;
            }
         } else if (InventoryUtils.getSelectedSlot() != this.intVal3) {
            InventoryUtils.setSelectedSlot(this.intVal3, false);
         } else if (mc.player.getMainHandStack().getItem() != Items.EXPERIENCE_BOTTLE) {
            this.updateState();
         } else if (!mc.player.getItemCooldownManager().isCoolingDown(mc.player.getMainHandStack())) {
            InventoryUtils.useItem(Hand.MAIN_HAND);
         }
      } else if (this.intVal != -1) {
         if (mc.player.getMainHandStack().getItem() != Items.EXPERIENCE_BOTTLE) {
            InventoryUtils.setSelectedSlot(this.intVal, false);
            this.intVal = -1;
         } else if (!mc.player.getItemCooldownManager().isCoolingDown(mc.player.getMainHandStack())) {
            InventoryUtils.useItem(Hand.MAIN_HAND);
         }
      } else if (InventoryUtils.findHotbarSlot(Items.EXPERIENCE_BOTTLE) != -1) {
         this.intVal = InventoryUtils.getSelectedSlot();
         InventoryUtils.setSelectedSlot(InventoryUtils.findHotbarSlot(Items.EXPERIENCE_BOTTLE), false);
      } else if (InventoryUtils.findMainInventorySlot(Items.EXPERIENCE_BOTTLE) != -1) {
         this.intVal2 = InventoryUtils.findMainInventorySlot(Items.EXPERIENCE_BOTTLE);
         this.intVal3 = InventoryUtils.getSelectedSlot();
         InventoryActionQueue.getInstance().queueSlotSwap(this.intVal2, this.intVal3, 1);
      } else {
         if (!InventoryUtils.extractItemFromBundle(Items.EXPERIENCE_BOTTLE) && !this.flag3) {
            this.flag3 = true;
            NotificationHud.notifyWithText(
               this.category.getIcon(),
               Items.EXPERIENCE_BOTTLE.getDefaultStack().getName().getString(),
               Text.literal("нет в инвентаре").setStyle(Style.EMPTY.withColor(Formatting.RED)),
               true
            );
         }
      }
   }

   @Override
   protected void onDisable() {
      this.flag = false;
      this.updateState();
   }

   public static ClickActionModule getInstance() {
      if (clickActionModule == null) {
         clickActionModule = new ClickActionModule();
      }

      return clickActionModule;
   }

   private void updateState3() {
      if (this.booleanSetting.isChecked() && mc.currentScreen == null) {
         boolean local1136_1 = this.friendKey.isBound() && this.friendKey.getBinding().isPressed(mc.getWindow().getHandle());
         if (local1136_1 && !this.flag2 && mc.targetedEntity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity && abstractClientPlayerEntity != mc.player
            )
          {
            if (FriendStore.isFriend(abstractClientPlayerEntity.getGameProfile().name())) {
               FriendStore.removeFriend(abstractClientPlayerEntity.getGameProfile().name());
               NotificationHud.processData3(this.category.getIcon(), "ClickAction", abstractClientPlayerEntity.getGameProfile().name() + " больше не друг", true);
            } else {
               FriendStore.addFriend(abstractClientPlayerEntity.getGameProfile().name());
               NotificationHud.processData3(this.category.getIcon(), "ClickAction", abstractClientPlayerEntity.getGameProfile().name() + " теперь друг", true);
            }
         }

         this.flag2 = local1136_1;
      } else {
         this.flag2 = false;
      }
   }
}
