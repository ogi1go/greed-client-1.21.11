package greed.client.module.misc;

import greed.client.util.ClientChat;
import greed.client.event.EventListener;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.ui.UiTheme;

import java.util.ArrayDeque;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@ModuleInfo(name = "DuelHelper", description = "Сам ищет дуэли: встаёт в очередь, перезапускает поиск и выходит после боя.", category = ModuleCategory.MISC)
public final class DuelHelperModule extends Module {
   private static DuelHelperModule duelHelperModule;
   public ModeSetting modeSetting;
   private final ArrayDeque<DuelHelperModule.DataRecord2> arrayDeque = new ArrayDeque<>();
   private DuelHelperModule.TypeOption nested0375 = DuelHelperModule.TypeOption.IDLE;
   private long timeMillis;
   private long timeMillis2;
   private long timeMillis3;
   private String strVal;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         long local1616_2 = System.currentTimeMillis();
         if (!this.modeSetting.getSelectedOption().equals(this.strVal)) {
            this.strVal = this.modeSetting.getSelectedOption();
            this.processData2("Режим: " + this.modeSetting.getSelectedOption().toLowerCase());
            this.processData3(0L);
         }

         while (!this.arrayDeque.isEmpty() && this.arrayDeque.peek().runAt() <= local1616_2) {
            String local1616_4 = this.arrayDeque.poll().command();
            if (local1616_4 == null) {
               this.updateState();
            } else {
               mc.getNetworkHandler().sendChatCommand(local1616_4);
            }
         }

         if (this.nested0375 == DuelHelperModule.TypeOption.WAITING_SEARCH_CONFIRM && local1616_2 - this.timeMillis >= 2000L) {
            this.updateState();
         } else if (this.nested0375 == DuelHelperModule.TypeOption.IN_DUEL && this.arrayDeque.isEmpty() && local1616_2 - this.timeMillis3 >= 2500L) {
            this.processData2("Дуэль началась, модуль выключен");
            this.setEnabled(false);
         } else if (this.nested0375 == DuelHelperModule.TypeOption.IN_QUEUE && this.arrayDeque.isEmpty() && local1616_2 - this.timeMillis2 >= 65000L) {
            this.processData2("Прошло 1:05 без соперника, ищем заново");
            this.updateState();
         }
      }
   };
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket && !gameMessageS2CPacket.overlay()) {
         String string = gameMessageS2CPacket.content().getString();
         mc.execute(() -> this.processData(string));
      }
   };

   public static DuelHelperModule getInstance() {
      if (duelHelperModule == null) {
         duelHelperModule = new DuelHelperModule();
      }

      return duelHelperModule;
   }

   private void updateState() {
      this.nested0375 = DuelHelperModule.TypeOption.WAITING_SEARCH_CONFIRM;
      this.timeMillis = System.currentTimeMillis();
      if (mc.player != null && mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendChatCommand(this.modeSetting.isSelected("Командный") ? "duel team search" : "duel search");
      }
   }

   private void processData(String local1614_1) {
      if (this.isEnabled()) {
         if (local1614_1.contains("Вы начали поиск") || local1614_1.contains("уже был начат")) {
            this.nested0375 = DuelHelperModule.TypeOption.IN_QUEUE;
            this.timeMillis2 = System.currentTimeMillis();
            this.processData2("В очереди, ищем соперника");
         }

         if (local1614_1.contains("был отменен") || local1614_1.contains("был отменён") || local1614_1.contains("не было найдено противников")) {
            this.processData2("Поиск отменён, перезапускаем");
            this.nested0375 = DuelHelperModule.TypeOption.IDLE;
            this.arrayDeque.clear();
            this.arrayDeque.add(new DuelHelperModule.DataRecord2(null, System.currentTimeMillis() + 300L));
         }

         if (local1614_1.contains("Дуэль начнётся") || local1614_1.contains("Дуэль начнется")) {
            this.timeMillis3 = System.currentTimeMillis();
            if (this.nested0375 != DuelHelperModule.TypeOption.IN_DUEL) {
               this.processData2("Соперник найден, дуэль начинается");
            }

            this.nested0375 = DuelHelperModule.TypeOption.IN_DUEL;
         }

         if (local1614_1.contains("покинул дуэль")
            && this.nested0375 == DuelHelperModule.TypeOption.IN_DUEL
            && (mc.player == null || !local1614_1.contains(mc.player.getName().getString()))) {
            this.processData2("Соперник ливнул, ищем заново");
            this.processData3(300L);
         }

         if (local1614_1.contains("одержала Победу") || local1614_1.contains("одержал Победу")) {
            this.processData2("Победа, ищем следующую дуэль");
            this.processData3(300L);
         }

         if (local1614_1.contains("одержала Поражение") || local1614_1.contains("одержал Поражение") || local1614_1.contains("потерпела Поражение")) {
            this.processData2("Поражение, ищем снова");
            this.processData3(300L);
         }

         if (local1614_1.contains("Дуэль завершена") && this.nested0375 == DuelHelperModule.TypeOption.IN_DUEL) {
            this.processData3(300L);
         }
      }
   }

   private void processData2(String local1615_1) {
      ClientChat.processData4(
         ClientChat.getMutableText("DuelHelper", UiTheme.intVal7, UiTheme.intVal9, true)
            .append(Text.literal(Formatting.DARK_GRAY + " » " + Formatting.RESET))
            .append(Text.literal(local1615_1))
      );
   }

   private DuelHelperModule() {
      this.modeSetting = this.addSetting(
         new ModeSetting("Режим", "Одиночный /duel search, Командный /duel team search.", "Одиночный", "Одиночный", "Командный")
      );
   }

   @Override
   protected void onEnable() {
      this.arrayDeque.clear();
      this.strVal = this.modeSetting.getSelectedOption();
      this.processData2("Включён, " + this.modeSetting.getSelectedOption().toLowerCase() + " режим");
      this.updateState();
   }

   @Override
   protected void onDisable() {
      this.arrayDeque.clear();
      this.nested0375 = DuelHelperModule.TypeOption.IDLE;
   }

   private void processData3(long local1621_1) {
      this.nested0375 = DuelHelperModule.TypeOption.IDLE;
      this.arrayDeque.clear();
      long local1621_3 = System.currentTimeMillis();
      this.arrayDeque.add(new DuelHelperModule.DataRecord2("duel leave", local1621_3 + local1621_1));
      this.arrayDeque.add(new DuelHelperModule.DataRecord2(null, local1621_3 + local1621_1 + 300L));
   }

   private enum TypeOption {
      IDLE,
      WAITING_SEARCH_CONFIRM,
      IN_QUEUE,
      IN_DUEL;
   }

   private record DataRecord2(String command, long runAt) {
   }
}
