package greed.client.module.misc;

import greed.client.ui.panel.PotionPresetPanel;

import greed.client.util.ClientChat;
import greed.client.ui.GreedPotionCombinerScreen;
import greed.client.event.EventListener;
import greed.client.setting.KeybindSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PlayerTickEvent;
import greed.client.util.PotionPreset;
import greed.client.ui.panel.PotionPresetPanel;
import greed.client.util.InventoryActionQueue;
import greed.client.util.InventorySlotManager;
import greed.client.module.player.ServerHelperModule;

import java.util.ArrayDeque;
import java.util.Random;
import net.minecraft.client.gui.screen.ChatScreen;

@ModuleInfo(name = "PotionCombiner", description = "Комбинирует бафы SpookyTime: по бинду кидает зелья пресета по очереди.", category = ModuleCategory.MISC)
public final class PotionCombinerModule extends Module {
   private static PotionCombinerModule potionCombinerModule;
   public KeybindSetting keybindSetting;
   private final Random random = new Random();
   private final ArrayDeque<String> arrayDeque = new ArrayDeque<>();
   private boolean flag;
   private long timeMillis;
   private long timeMillis2;
   private String strVal = "";
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && mc.getWindow() != null) {
         if (this.keybindSetting.isBound()
            && this.keybindSetting.getBinding().isPressed(mc.getWindow().getHandle())
            && !this.flag
            && !(mc.currentScreen instanceof ChatScreen)
            && !(mc.currentScreen instanceof GreedPotionCombinerScreen)) {
            PotionPresetPanel.getInstance().updateState2();
            mc.setScreen(GreedPotionCombinerScreen.instance());
         }

         this.flag = this.keybindSetting.isBound() && this.keybindSetting.getBinding().isPressed(mc.getWindow().getHandle());
         if (mc.world != null && mc.interactionManager != null) {
            this.updateState2();
            this.updateState();
         }
      }
   };

   private void updateState() {
      if (!this.arrayDeque.isEmpty() && mc.currentScreen == null && System.currentTimeMillis() >= this.timeMillis) {
         if (!InventorySlotManager.getInstance().isLocked() && !InventoryActionQueue.getInstance().hasPendingActions()) {
            String local955_1 = this.arrayDeque.poll();

            for (ServerHelperModule.SubContext local955_3 : ServerHelperModule.getInstance().getArrayList()) {
               if (local955_3.getItem2() == null && local955_3.getKeybindSetting().name.equals(local955_1)) {
                  ServerHelperModule.getInstance().updateState2();
                  ServerHelperModule.getInstance().processData9(local955_3);
                  break;
               }
            }

            this.timeMillis = System.currentTimeMillis() + 80L + this.random.nextInt(40);
         }
      }
   }

   private void processData(String local956_1) {
      this.processData2(local956_1);
      if (this.isEnabled()) {
         this.toggle();
      }
   }

   @Override
   protected void onDisable() {
      this.arrayDeque.clear();
   }

   @Override
   protected void onEnable() {
      this.strVal = "";
      this.timeMillis2 = 0L;
   }

   private PotionCombinerModule() {
      this.keybindSetting = this.addSetting(new KeybindSetting("Открыть меню", "Клавиша, которая открывает окно Potion Combiner."));
   }

   private void processData2(String local960_1) {
      if (local960_1 != null && !local960_1.isBlank()) {
         if (!local960_1.equals(this.strVal) || System.currentTimeMillis() >= this.timeMillis2) {
            ClientChat.processData8("&8[&dPotionCombiner&8] &f" + local960_1);
            this.strVal = local960_1;
            this.timeMillis2 = System.currentTimeMillis() + 2500L;
         }
      }
   }

   private void updateState2() {
      for (PotionPreset.SubContext local961_2 : PotionPreset.getInstance().list) {
         boolean local961_3 = local961_2.keybindSetting.isBound()
            && local961_2.keybindSetting.getBinding().isPressed(mc.getWindow().getHandle())
            && mc.currentScreen == null;
         if (local961_2.flag && local961_3 && !local961_2.flag2 && this.arrayDeque.isEmpty()) {
            for (PotionPreset.SubContext2 local961_5 : local961_2.list2) {
               this.arrayDeque.add(local961_5.strVal3);
            }

            this.timeMillis = System.currentTimeMillis();
            this.processData2("Комбо &d" + local961_2.strVal + "&f: кидаю " + local961_2.list2.size() + " зелья по очереди.");
         }

         local961_2.flag2 = local961_3;
      }
   }

   public static PotionCombinerModule getInstance() {
      if (potionCombinerModule == null) {
         potionCombinerModule = new PotionCombinerModule();
      }

      return potionCombinerModule;
   }
}
