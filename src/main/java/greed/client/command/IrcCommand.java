//package greed.client.command;
//
//import greed.client.util.ClientChat;
//import ru.dreamix.protection.api.impl.profile.ProfileAPI;
//import greed.client.util.IrcClient;
//import ru.dreamix.protection.api.impl.profile.objects.RoleObject;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//
//public class IrcCommand implements ClientCommand {
//   @Override
//   public ArrayList<String> suggestArguments(String[] stringArray) {
//      if (stringArray.length > 1) {
//         return new ArrayList<>();
//      }
//
//      ArrayList<String> local7467_2 = new ArrayList<>(Arrays.asList("on", "off"));
//      if (this.checkState()) {
//         local7467_2.add("online");
//      }
//
//      return local7467_2;
//   }
//
//   private boolean checkState() {
//      return ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.ADMIN);
//   }
//
//   @Override
//   public String getName() {
//      return "irc";
//   }
//
//   @Override
//   public String getDescription() {
//      return "Чат клиента (.irc on/off, писать через @сообщение)";
//   }
//
//   @Override
//   public void execute(String[] stringArray) {
//      switch (stringArray.length == 0 ? "" : stringArray[0].toLowerCase()) {
//         case "on":
//            IrcClient.getInstance().processData2(true);
//            ClientChat.processData8("IRC включён, пиши в чат через &7@сообщение");
//            break;
//         case "off":
//            IrcClient.getInstance().processData2(false);
//            ClientChat.processData8("IRC выключен.");
//            break;
//         case "online":
//            if (!this.checkState()) {
//               ClientChat.processData8("&cКоманда доступна только администраторам.");
//            } else if (!IrcClient.getInstance().checkState()) {
//               ClientChat.processData8("IRC выключен, включи через &7.irc on");
//            } else {
//               IrcClient.getInstance().updateState();
//            }
//            break;
//         default:
//            ClientChat.processData8(
//               "IRC " + (IrcClient.getInstance().checkState() ? "&aвключён" : "&cвыключен") + "&r. Использование: &7.irc on/off&r, сообщение: &7@текст"
//            );
//      }
//   }
//}
