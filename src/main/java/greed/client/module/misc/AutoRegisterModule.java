package greed.client.module.misc;

import greed.client.event.EventListener;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PacketEvent;
import greed.client.setting.NumericTextSetting;

import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@ModuleInfo(name = "AutoRegister", description = "Автоматически вводит пароль при запросе регистрации или входа.", category = ModuleCategory.MISC)
public final class AutoRegisterModule extends Module {
   private static AutoRegisterModule autoRegisterModule;
   public NumericTextSetting password;
   public NumericTextSetting registerCommand;
   public NumericTextSetting loginCommand;
   private long timeMillis;
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (mc.player != null
         && packetEvent.isIncoming()
         && !this.password.getText().isEmpty()
         && System.currentTimeMillis() - this.timeMillis >= 2000L
         && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
         String local4176_3 = gameMessageS2CPacket.content().getString().toLowerCase();
         if (local4176_3.contains("войд") || local4176_3.contains("логин") || local4176_3.contains("login") || local4176_3.contains("авториз")) {
            this.timeMillis = System.currentTimeMillis();
            this.processData(this.loginCommand.getText());
         } else if (local4176_3.contains("регист") || local4176_3.contains("register")) {
            this.timeMillis = System.currentTimeMillis();
            this.processData(this.registerCommand.getText());
         }
      }
   };

   private AutoRegisterModule() {
      this.password = this.addSetting(new NumericTextSetting("Пароль", "Твой пароль сервера для регистрации и входа.", "", 64));
      this.registerCommand = new NumericTextSetting("Команда регистрации", "%pass% заменяется на пароль.", "reg %pass% %pass%", 128);
      this.loginCommand = new NumericTextSetting("Команда входа", "%pass% заменяется на пароль.", "l %pass%", 128);
   }

   public static AutoRegisterModule getInstance() {
      if (autoRegisterModule == null) {
         autoRegisterModule = new AutoRegisterModule();
      }

      return autoRegisterModule;
   }

   private void processData(String local4178_1) {
      mc.player
         .networkHandler
         .sendChatCommand((local4178_1.startsWith("/") ? local4178_1.substring(1) : local4178_1).replace("%pass%", this.password.getText()));
   }
}
