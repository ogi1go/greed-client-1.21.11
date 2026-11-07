//package greed.client.command;
//
//import greed.client.util.ClientChat;
//import greed.client.module.misc.PartyModule;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//
//public class PartyCommand implements ClientCommand {
//   @Override
//   public ArrayList<String> suggestArguments(String[] stringArray) {
//      return stringArray.length <= 1 ? new ArrayList<>(Arrays.asList("create", "join", "leave", "lock", "unlock", "kick", "list")) : new ArrayList<>();
//   }
//
//   @Override
//   public String getName() {
//      return "party";
//   }
//
//   @Override
//   public String getDescription() {
//      return "Пати (.party create/join <код>/leave/lock/unlock/kick <ник>/list)";
//   }
//
//   @Override
//   public void execute(String[] stringArray) {
//      PartyModule partyModule = PartyModule.getInstance();
//      switch (stringArray.length == 0 ? "" : stringArray[0].toLowerCase()) {
//         case "create":
//            partyModule.updateState6();
//            break;
//         case "join":
//            if (stringArray.length < 2) {
//               ClientChat.processData8("Использование: &7.party join <код>");
//            } else {
//               partyModule.processData4(stringArray[1]);
//            }
//            break;
//         case "leave":
//            partyModule.updateState2();
//            break;
//         case "lock":
//            partyModule.processData6(true);
//            break;
//         case "unlock":
//            partyModule.processData6(false);
//            break;
//         case "kick":
//            if (stringArray.length < 2) {
//               ClientChat.processData8("Использование: &7.party kick <ник>");
//            } else {
//               partyModule.processData2(stringArray[1]);
//            }
//            break;
//         default:
//            partyModule.updateState4();
//      }
//   }
//}
