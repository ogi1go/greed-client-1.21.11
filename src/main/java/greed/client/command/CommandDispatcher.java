package greed.client.command;

import greed.client.util.ClientChat;

import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.util.Formatting;

public class CommandDispatcher {
   private final ArrayList<ClientCommand> list;
   private final String strVal;

   public ArrayList<String> getArrayList(String local2969_1) {
      ArrayList<String> local2969_2 = new ArrayList<>();
      if (!local2969_1.startsWith(this.strVal)) {
         return local2969_2;
      }

      String[] stringArray = local2969_1.substring(this.strVal.length()).split(" ", -1);
      if (stringArray.length == 1) {
         for (ClientCommand clientCommand : this.list) {
            if (clientCommand.getName().startsWith(stringArray[0].toLowerCase())) {
               local2969_2.add(this.strVal + clientCommand.getName());
            }
         }

         return local2969_2;
      } else {
         for (ClientCommand clientCommand2 : this.list) {
            if (clientCommand2.getName().equalsIgnoreCase(stringArray[0])) {
               for (String local2969_7 : clientCommand2.suggestArguments(Arrays.copyOfRange(stringArray, 1, stringArray.length))) {
                  if (local2969_7.toLowerCase().startsWith(stringArray[stringArray.length - 1].toLowerCase())) {
                     local2969_2.add(local2969_7);
                  }
               }
            }
         }

         return local2969_2;
      }
   }

   public CommandDispatcher(ArrayList<ClientCommand> local2970_1, String local2970_2) {
      this.list = local2970_1;
      this.strVal = local2970_2;
   }

   public ArrayList<ClientCommand> getArrayList2(String local2971_1) {
      ArrayList<ClientCommand> local2971_2 = new ArrayList<>();

      for (ClientCommand clientCommand : this.list) {
         if ((this.strVal + clientCommand.getName()).startsWith(local2971_1) || local2971_1.startsWith(this.strVal + clientCommand.getName() + " ")) {
            local2971_2.add(clientCommand);
         }
      }

      return local2971_2;
   }

   public ArrayList<ClientCommand> getArrayList3() {
      return this.list;
   }

   public boolean matchesCriteria(String local2973_1) {
      if (!local2973_1.startsWith(this.strVal)) {
         return false;
      }

      String[] stringArray = local2973_1.substring(this.strVal.length()).trim().split(" +");

      for (ClientCommand clientCommand : this.list) {
         if (clientCommand.getName().equalsIgnoreCase(stringArray[0])) {
            clientCommand.execute(Arrays.copyOfRange(stringArray, 1, stringArray.length));
            return true;
         }
      }

      ClientChat.processData8("Неизвестная команда, введи " + Formatting.GRAY + this.strVal + "help");
      return true;
   }

   public String formatText() {
      return this.strVal;
   }

   public String formatText2(String local2975_1) {
      return this.getArrayList(local2975_1).isEmpty()
         ? local2975_1
         : local2975_1.substring(0, local2975_1.length() - local2975_1.substring(local2975_1.lastIndexOf(32) + 1).length())
            + this.getArrayList(local2975_1).get(0)
            + " ";
   }
}
