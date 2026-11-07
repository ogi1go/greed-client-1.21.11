package greed.client.module.misc;

import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.util.Stopwatch;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(name = "ChestStealer", description = "Автоматически забирает все предметы из сундуков.", category = ModuleCategory.MISC)
public final class ChestStealerModule extends Module {
   private static ChestStealerModule chestStealerModule;
   public ModeSetting modeSetting;
   public BooleanSetting disableHitboxes;
   public BooleanSetting autoClose;
   private final Stopwatch stopwatch = new Stopwatch();
   private boolean flag;
   public final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && mc.currentScreen != null) {
         if (!this.modeSetting.isSelected("FunTime") || !mc.currentScreen.getTitle().getString().contains("☃ Аукцион ")) {
            if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
               Inventory inventory = genericContainerScreenHandler.getInventory();
               ArrayList arrayList = new ArrayList();

               for (int index = 0; index < inventory.size(); index++) {
                  if (!inventory.getStack(index).isEmpty()) {
                     arrayList.add(index);
                  }
               }

               if (!arrayList.isEmpty() && this.stopwatch.hasExceededMillis(this.modeSetting.isSelected("FunTime") ? 400L : 10L)) {
                  InventoryUtils.sendSlotClickPacket(
                     (Integer)arrayList.get(ThreadLocalRandom.current().nextInt(arrayList.size())), 0, SlotActionType.QUICK_MOVE, false
                  );
                  this.flag = true;
                  this.stopwatch.reset();
               }

               if (this.autoClose.isChecked() && this.flag) {
                  boolean local1244_5 = false;

                  for (int index2 = 0; index2 < inventory.size(); index2++) {
                     if (!inventory.getStack(index2).isEmpty()) {
                        local1244_5 = true;
                        break;
                     }
                  }

                  if (!local1244_5) {
                     mc.player.closeHandledScreen();
                     this.flag = false;
                  }
               }
            }
         }
      }
   };

   private ChestStealerModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Режим использования", "Пауза между кликами под сервер.", "FunTime", "Vanilla", "FunTime"));
      this.disableHitboxes = this.addSetting(new BooleanSetting("Отключить хитбоксы", "Не целиться в сущности, пока модуль включён.", true));
      this.autoClose = this.addSetting(new BooleanSetting("Авто-закрытие", "Закрывает сундук, когда он опустел.", true));
   }

   public static ChestStealerModule getInstance() {
      if (chestStealerModule == null) {
         chestStealerModule = new ChestStealerModule();
      }

      return chestStealerModule;
   }
}
