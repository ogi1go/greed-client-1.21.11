package com.greed.lib.discord;

import greed.client.util.DiscordRpcLibrary;
import ru.dreamix.fabricloader.VMBridge;

import com.sun.jna.Structure;
import java.util.List;

public class DiscordNative$Handlers extends Structure {
   public DiscordRpcLibrary.SubInterface2 ready;
   public DiscordRpcLibrary.SubInterface3 disconnected;
   public DiscordRpcLibrary.SubInterface3 errored;
   public DiscordRpcLibrary.SubInterface4 joinGame;
   public DiscordRpcLibrary.SubInterface4 spectateGame;
   public DiscordRpcLibrary.SubInterface joinRequest;

   static {
      VMBridge.identifyClass(DiscordNative$Handlers.class, "xtUTdsvN");
   }

   protected List<String> getFieldOrder() {
      return List.of("ready", "disconnected", "errored", "joinGame", "spectateGame", "joinRequest");
   }
}
