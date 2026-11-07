package com.greed.lib.discord;

import ru.dreamix.fabricloader.VMBridge;

import com.sun.jna.Structure;
import java.util.List;

public class DiscordNative$User extends Structure {
   public String userId;
   public String username;
   public String discriminator;
   public String avatar;

   public DiscordNative$User() {
      this.setStringEncoding("UTF-8");
   }

   static {
      VMBridge.identifyClass(DiscordNative$User.class, "U8zSuouZ");
   }

   protected List<String> getFieldOrder() {
      return List.of("userId", "username", "discriminator", "avatar");
   }
}
