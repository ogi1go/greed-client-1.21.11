package greed.client.module.misc;

import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.storage.FriendStore;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.MultiChoiceSetting;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.ui.hud.NotificationHud;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(name = "AutoAccept", description = "Сам принимает телепорты, приглашения в клан и командные дуэли.", category = ModuleCategory.MISC)
public final class AutoAcceptModule extends Module {
   private static AutoAcceptModule autoAcceptModule;
   public MultiChoiceSetting multiChoiceSetting;
   public BooleanSetting booleanSetting;
   private static final String[] strValArray = new String[]{
      "просит телепортироваться",
      "хочет телепортироваться",
      "Заявка буудет автоматически отменена через",
      "просит телепортироваться к Вам.",
      "120 секунд",
      "has requested to teleport",
      "teleport to you",
      "This request will timeout after",
      "120 seconds"
   };
   private static final Pattern pattern = Pattern.compile("\\[⚔] (\\w+) приглашает Вас в клан");
   private static final Pattern pattern2 = Pattern.compile("\\[⚔] Игрок (.+?) приглашает вас в свою дуэльную команду!");
   private long timeMillis;
   private boolean flag;
   public final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (this.flag && mc.player != null && mc.currentScreen != null) {
         if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
            if (mc.currentScreen.getTitle().getString().contains("Приглашение в команду")) {
               Inventory inventory = genericContainerScreenHandler.getInventory();

               for (int index = 0; index < inventory.size(); index++) {
                  if (inventory.getStack(index).getItem() == Items.GREEN_STAINED_GLASS_PANE) {
                     InventoryUtils.sendSlotClickPacket(index, 0, SlotActionType.QUICK_MOVE, true);
                     mc.player.closeHandledScreen();
                     break;
                  }
               }
            }

            this.flag = false;
         }
      }
   };

   private boolean matchesCriteria(String local6140_1) {
      for (String local6140_3 : FriendStore.getFriendList()) {
         if (local6140_1.toLowerCase(Locale.ROOT).contains(local6140_3.toLowerCase(Locale.ROOT))) {
            return true;
         }
      }

      return false;
   }

   private AutoAcceptModule() {
      this.multiChoiceSetting = this.addSetting(
         new MultiChoiceSetting("Принимать что/куда?", "Какие приглашения принимать самому.")
            .withOptions("Телепортации", "В клан", "В командные дуэли")
            .withSelection("Телепортации", "В клан", "В командные дуэли")
      );
      this.booleanSetting = this.addSetting(new BooleanSetting("Только друзья", "Принимать, только если в сообщении есть ник друга.", true));
   }

   public static AutoAcceptModule getInstance() {
      if (autoAcceptModule == null) {
         autoAcceptModule = new AutoAcceptModule();
      }

      return autoAcceptModule;
   }

   public void processData(String local6144_1) {
      if (this.isEnabled() && mc.player != null && local6144_1 != null) {
         if (!this.booleanSetting.isChecked() || this.matchesCriteria(local6144_1)) {
            if (this.multiChoiceSetting.matchesOption("Телепортации")) {
               for (String local6144_5 : strValArray) {
                  if (local6144_1.contains(local6144_5)) {
                     this.timeMillis = System.currentTimeMillis();
                     mc.player.networkHandler.sendChatCommand("tpaccept");
                     NotificationHud.processData3(this.category.getIcon(), "AutoAccept", "Принял телепорт", true);
                     return;
                  }
               }
            }

            if (this.multiChoiceSetting.matchesOption("В клан")) {
               Matcher matcher = pattern.matcher(local6144_1);
               if (matcher.find()) {
                  this.timeMillis = System.currentTimeMillis();
                  mc.player.networkHandler.sendChatCommand("clan accept " + matcher.group(1));
                  NotificationHud.processData3(this.category.getIcon(), "AutoAccept", "Принял в клан от " + matcher.group(1), true);
                  return;
               }
            }

            if (this.multiChoiceSetting.matchesOption("В командные дуэли")) {
               Matcher matcher2 = pattern2.matcher(local6144_1);
               if (matcher2.find()) {
                  this.timeMillis = System.currentTimeMillis();
                  mc.player.networkHandler.sendChatCommand("duel team accept " + matcher2.group(1));
                  this.flag = true;
                  NotificationHud.processData3(this.category.getIcon(), "AutoAccept", "Принял дуэль от " + matcher2.group(1), true);
               }
            }
         }
      }
   }
}
