package greed.client.command;

import greed.client.util.ClientChat;
import greed.client.command.ClientCommand;
import greed.client.MinecraftAccess;

import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.util.Util;

public class ResourcePackCommand implements MinecraftAccess, ClientCommand {
   @Override
   public String getName() {
      return "rp";
   }

   @Override
   public String getDescription() {
      return "Открывает папку с ресурспаками (.rp dir)";
   }

   @Override
   public void execute(String[] stringArray) {
      if (stringArray.length > 0 && stringArray[0].equalsIgnoreCase("dir")) {
         Util.getOperatingSystem().open(mc.getResourcePackDir());
         ClientChat.processData8("Папка ресурспаков открыта.");
      } else {
         ClientChat.processData8("Использование: &7.rp dir");
      }
   }

   @Override
   public ArrayList<String> suggestArguments(String[] stringArray) {
      return stringArray.length <= 1 ? new ArrayList<>(Arrays.asList("dir")) : new ArrayList<>();
   }
}
