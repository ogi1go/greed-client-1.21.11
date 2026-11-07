package greed.client.util;

import com.greed.lib.discord.DiscordNative$Handlers;
import com.greed.lib.discord.DiscordNative$Presence;
import com.greed.lib.discord.DiscordNative$User;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;

public interface DiscordRpcLibrary extends Library {
   DiscordRpcLibrary discordRpcLibrary = (DiscordRpcLibrary)Native.load("discord-rpc", DiscordRpcLibrary.class);

   void Discord_ClearPresence();

   void Discord_Initialize(String var1, DiscordNative$Handlers handlers, boolean var3, String var4);

   void Discord_RunCallbacks();

   void Discord_UpdatePresence(DiscordNative$Presence presence);

   void Discord_Shutdown();

   interface SubInterface extends Callback {
      void apply(DiscordNative$User user);
   }

   interface SubInterface2 extends Callback {
      void apply(DiscordNative$User user);
   }

   interface SubInterface3 extends Callback {
      void apply(int var1, String var2);
   }

   interface SubInterface4 extends Callback {
      void apply(String var1);
   }
}
