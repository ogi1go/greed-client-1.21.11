package greed.client.module.misc;

import greed.client.GreedClient;
import com.greed.lib.discord.DiscordNative$Handlers;
import com.greed.lib.discord.DiscordNative$Presence;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import ru.dreamix.protection.api.impl.profile.ProfileAPI;
import greed.client.util.DiscordRpcLibrary;

import java.util.Locale;

@ModuleInfo(name = "DiscordRPC", description = "Показывает клиент в статусе Discord.", category = ModuleCategory.MISC)
public final class DiscordRPCModule extends Module {
   private static DiscordRPCModule discordRPCModule;
   private final DiscordNative$Handlers discordHandlers = new DiscordNative$Handlers();
   private volatile Thread thread2;
   private volatile String strVal = "";
   private long timeMillis;

   private void updateState() {
      try {
         DiscordRpcLibrary.discordRpcLibrary.Discord_Initialize("1527791397556588664", this.discordHandlers, true, "");
         this.updateState2();

         while (this.thread2 == Thread.currentThread()) {
            DiscordRpcLibrary.discordRpcLibrary.Discord_RunCallbacks();
            Thread.sleep(2000L);
         }
      } catch (InterruptedException exception) {
      } catch (Throwable throwable) {
         GreedClient.logger().warn("Discord RPC недоступен: {}", throwable.toString());
      } finally {
         try {
            DiscordRpcLibrary.discordRpcLibrary.Discord_ClearPresence();
            DiscordRpcLibrary.discordRpcLibrary.Discord_Shutdown();
         } catch (Throwable throwable2) {
         }
      }
   }

   public static DiscordRPCModule getInstance() {
      if (discordRPCModule == null) {
         discordRPCModule = new DiscordRPCModule();
      }

      return discordRPCModule;
   }

   private void updateState2() {
      DiscordNative$Presence presence = new DiscordNative$Presence();
      ProfileAPI.get().getModel().getName();
      presence.details = " by ogi";
      presence.state = "feel greed";
      presence.startTimestamp = this.timeMillis;
      presence.largeImageKey = "https://i.ibb.co/hxsyxR5h/ASD1-ASD0000-0200-2.gif";
      presence.largeImageText = "t.me/greedlc";
      if (!this.strVal.isEmpty()) {
         presence.smallImageKey = this.strVal;
         presence.smallImageText = "feel greed";
      }

      presence.button_label_1 = "Телеграм";
      presence.button_url_1 = "https://t.me/greedlc";
      presence.button_label_2 = "Дискорд";
      presence.button_url_2 = "https://discord.gg/greedclient";
      DiscordRpcLibrary.discordRpcLibrary.Discord_UpdatePresence(presence);
   }

   @Override
   protected void onEnable() {
      if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux") && this.thread2 == null) {
         this.timeMillis = System.currentTimeMillis() / 1000L;
         Thread thread = new Thread(this::updateState, "greed-discord-rpc");
         thread.setDaemon(true);
         this.thread2 = thread;
         thread.start();
      }
   }

   private DiscordRPCModule() {
      this.discordHandlers.ready = user -> {
         if (user.avatar != null && !user.avatar.isEmpty()) {
            this.strVal = "https://cdn.discordapp.com/avatars/" + user.userId + "/" + user.avatar + ".png";
         }

         this.updateState2();
      };
   }

   @Override
   protected void onDisable() {
      Thread thread = this.thread2;
      this.thread2 = null;
      if (thread != null) {
         thread.interrupt();
      }
   }
}
