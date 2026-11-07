package greed.client.command;

import greed.client.util.ClientChat;
import greed.client.storage.FriendStore;

import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

public class FriendCommand implements ClientCommand {
   private static String formatText(String local6703_0) {
      if (MinecraftClient.getInstance().getNetworkHandler() == null) {
         return local6703_0;
      }

      for (PlayerListEntry playerListEntry : MinecraftClient.getInstance().getNetworkHandler().getPlayerList()) {
         if (playerListEntry.getProfile().name().equalsIgnoreCase(local6703_0)) {
            return playerListEntry.getProfile().name();
         }
      }

      return local6703_0;
   }

   @Override
   public ArrayList<String> suggestArguments(String[] stringArray) {
      if (stringArray.length <= 1) {
         return new ArrayList<>(Arrays.asList("add", "remove", "list", "clear"));
      }

      if (stringArray.length != 2) {
         return new ArrayList<>();
      }

      if (!stringArray[0].equalsIgnoreCase("remove") && !stringArray[0].equalsIgnoreCase("delete")) {
         ArrayList<String> local6704_2 = new ArrayList<>();
         if (stringArray[0].equalsIgnoreCase("add") && MinecraftClient.getInstance().getNetworkHandler() != null) {
            for (PlayerListEntry playerListEntry : MinecraftClient.getInstance().getNetworkHandler().getPlayerList()) {
               if (!FriendStore.isFriend(playerListEntry.getProfile().name())) {
                  local6704_2.add(playerListEntry.getProfile().name());
               }
            }
         }

         return local6704_2;
      } else {
         return FriendStore.getFriendList();
      }
   }

   @Override
   public void execute(String[] stringArray) {
      if (stringArray.length == 0 || stringArray[0].equalsIgnoreCase("list")) {
         ClientChat.processData8(
            FriendStore.getFriendList().isEmpty() ? "Друзей пока нет." : "Друзья: &7" + String.join(", ", FriendStore.getFriendList())
         );
      } else if (stringArray[0].equalsIgnoreCase("clear")) {
         FriendStore.clearFriends();
         ClientChat.processData8("Список друзей очищен.");
      } else if (stringArray.length < 2) {
         ClientChat.processData8("Использование: &7.friend add/remove/list/clear <ник>");
      } else {
         switch (stringArray[0].toLowerCase()) {
            case "add":
               ClientChat.processData8(
                  FriendStore.addFriend(formatText(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length))))
                     ? "&7" + formatText(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length))) + "&r добавлен в друзья."
                     : "&7" + formatText(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length))) + "&r уже в друзьях."
               );
               break;
            case "remove":
            case "delete":
               ClientChat.processData8(
                  FriendStore.removeFriend(formatText(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length))))
                     ? "&7" + formatText(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length))) + "&r удалён из друзей."
                     : "&7" + formatText(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length))) + "&r не в друзьях."
               );
               break;
            default:
               ClientChat.processData8("Использование: &7.friend add/remove/list/clear <ник>");
         }
      }
   }

   @Override
   public String getName() {
      return "friend";
   }

   @Override
   public String getDescription() {
      return "Друзья (.friend add/remove/list/clear <ник>)";
   }
}
