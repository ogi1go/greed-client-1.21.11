package greed.client.command;

import greed.client.util.WaypointManager;

import greed.client.util.ClientChat;
import greed.client.command.ClientCommand;
import greed.client.MinecraftAccess;

import java.util.ArrayList;
import java.util.Arrays;

public class WaypointCommand implements MinecraftAccess, ClientCommand {
   @Override
   public ArrayList<String> suggestArguments(String[] stringArray) {
      if (stringArray.length <= 1) {
         return new ArrayList<>(Arrays.asList("add", "del", "clear"));
      } else if (stringArray.length != 2 || !stringArray[0].equalsIgnoreCase("del") && !stringArray[0].equalsIgnoreCase("remove")) {
         return stringArray.length == 2 && stringArray[0].equalsIgnoreCase("add") && mc.player != null
            ? new ArrayList<>(Arrays.asList(mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ()))
            : new ArrayList<>();
      } else {
         return WaypointManager.getInstance().getArrayList();
      }
   }

   @Override
   public String getName() {
      return "way";
   }

   @Override
   public void execute(String[] stringArray) {
      if (stringArray.length == 0) {
         ClientChat.processData8("Использование: &7.way add [название] <x y z>&r, &7.way del <название>&r, &7.way clear");
      } else if (stringArray[0].equalsIgnoreCase("clear")) {
         ClientChat.processData8(
            WaypointManager.getInstance().computeInt() == 0 ? "Меток нет." : "Удалено меток: &7" + WaypointManager.getInstance().computeInt()
         );
         WaypointManager.getInstance().updateState();
      } else if (!stringArray[0].equalsIgnoreCase("del") && !stringArray[0].equalsIgnoreCase("remove")) {
         if (!stringArray[0].equalsIgnoreCase("add")) {
            ClientChat.processData8("Использование: &7.way add [название] <x y z>&r, &7.way del <название>&r, &7.way clear");
         } else if (stringArray.length == 1 && mc.player != null) {
            WaypointManager.getInstance().processData("Метка", mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
            ClientChat.processData8(
               "Метка &7Метка&r поставлена на &7" + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ()
            );
         } else if (stringArray.length != 4 && stringArray.length != 5) {
            ClientChat.processData8("Использование: &7.way add [название] <x y z>");
         } else {
            try {
               WaypointManager.getInstance()
                  .processData(
                     stringArray.length == 5 ? stringArray[1] : "Метка",
                     Integer.parseInt(stringArray[stringArray.length - 3]),
                     Integer.parseInt(stringArray[stringArray.length - 2]),
                     Integer.parseInt(stringArray[stringArray.length - 1])
                  );
               ClientChat.processData8(
                  "Метка &7"
                     + (stringArray.length == 5 ? stringArray[1] : "Метка")
                     + "&r поставлена на &7"
                     + stringArray[stringArray.length - 3]
                     + " "
                     + stringArray[stringArray.length - 2]
                     + " "
                     + stringArray[stringArray.length - 1]
               );
            } catch (NumberFormatException exception) {
               ClientChat.processData8("Координаты должны быть целыми числами.");
            }
         }
      } else if (stringArray.length < 2) {
         ClientChat.processData8("Использование: &7.way del <название>");
      } else {
         ClientChat.processData8(
            WaypointManager.getInstance().matchesCriteria(stringArray[1])
               ? "Метка &7" + stringArray[1] + "&r удалена."
               : "Метка &7" + stringArray[1] + "&r не найдена."
         );
      }
   }

   @Override
   public String getDescription() {
      return "Метки (.way add [название] <x y z> / del <название> / clear)";
   }
}
