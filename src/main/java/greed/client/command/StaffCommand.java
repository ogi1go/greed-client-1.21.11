package greed.client.command;

import greed.client.util.ClientChat;
import greed.client.storage.StaffStore;

import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;

public class StaffCommand implements ClientCommand {
   @Override
   public String getName() {
      return "staff";
   }

   @Override
   public ArrayList<String> suggestArguments(String[] stringArray) {
      if (stringArray.length <= 1) {
         return new ArrayList<>(Arrays.asList("name", "prefix", "del", "list", "clear"));
      }

      if (stringArray.length != 2) {
         return new ArrayList<>();
      }

      if (!stringArray[0].equalsIgnoreCase("del") && !stringArray[0].equalsIgnoreCase("remove") && !stringArray[0].equalsIgnoreCase("delete")) {
         ArrayList<String> local7032_2 = new ArrayList<>();
         if (MinecraftClient.getInstance().getNetworkHandler() != null && MinecraftClient.getInstance().world != null) {
            for (PlayerListEntry playerListEntry : MinecraftClient.getInstance().getNetworkHandler().getPlayerList()) {
               if (stringArray[0].equalsIgnoreCase("name") && !StaffStore.matchesCriteria3(playerListEntry.getProfile().name())) {
                  local7032_2.add(playerListEntry.getProfile().name());
               }

               if (stringArray[0].equalsIgnoreCase("prefix")) {
                  Team team = MinecraftClient.getInstance().world.getScoreboard().getScoreHolderTeam(playerListEntry.getProfile().name());
                  if (team != null
                     && !team.getPrefix().getString().trim().isEmpty()
                     && !local7032_2.contains(team.getPrefix().getString().trim())) {
                     local7032_2.add(team.getPrefix().getString().trim());
                  }
               }
            }

            return local7032_2;
         } else {
            return local7032_2;
         }
      } else {
         ArrayList<String> local7032_2 = StaffStore.getArrayList2();
         local7032_2.addAll(StaffStore.getArrayList());
         return local7032_2;
      }
   }

   @Override
   public void execute(String[] stringArray) {
      if (stringArray.length != 0 && !stringArray[0].equalsIgnoreCase("list")) {
         if (stringArray[0].equalsIgnoreCase("clear")) {
            StaffStore.updateState2();
            ClientChat.processData8("Список своих стаффов очищен.");
         } else if (stringArray.length < 2) {
            ClientChat.processData8(
               "Использование: &7.staff name <ник>&r, &7.staff prefix <префикс>&r, &7.staff del <ник или префикс>&r, &7.staff list&r, &7.staff clear"
            );
         } else {
            switch (stringArray[0].toLowerCase()) {
               case "name":
               case "nick":
                  ClientChat.processData8(
                     StaffStore.matchesCriteria(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)))
                        ? "Ник &7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r теперь всегда в стафф-листе."
                        : "Ник &7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r уже добавлен."
                  );
                  break;
               case "prefix":
               case "tag":
                  ClientChat.processData8(
                     StaffStore.matchesCriteria5(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)))
                        ? "Префикс &7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r теперь считается стаффским."
                        : "Префикс &7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r уже добавлен."
                  );
                  break;
               case "del":
               case "remove":
               case "delete":
                  ClientChat.processData8(
                     StaffStore.matchesCriteria4(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)))
                        ? "&7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r убран из своих стаффов."
                        : "&7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r не найден."
                  );
                  break;
               default:
                  ClientChat.processData8(
                     "Использование: &7.staff name <ник>&r, &7.staff prefix <префикс>&r, &7.staff del <ник или префикс>&r, &7.staff list&r, &7.staff clear"
                  );
            }
         }
      } else if (StaffStore.getArrayList2().isEmpty() && StaffStore.getArrayList().isEmpty()) {
         ClientChat.processData8("Своих стаффов пока нет. Пример: &7.staff name Spa1k1&r, &7.staff prefix ГМ");
      } else {
         ClientChat.processData9(
            "&fСвои стаффы:",
            "Ники: &7" + (StaffStore.getArrayList2().isEmpty() ? "нет" : String.join(", ", StaffStore.getArrayList2())),
            "Префиксы: &7" + (StaffStore.getArrayList().isEmpty() ? "нет" : String.join(", ", StaffStore.getArrayList()))
         );
      }
   }

   @Override
   public String getDescription() {
      return "Свои стаффы (.staff name <ник> / prefix <префикс> / del <ник или префикс> / list / clear)";
   }
}
