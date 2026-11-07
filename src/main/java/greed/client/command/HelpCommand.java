package greed.client.command;

import greed.client.util.ClientChat;

import java.util.ArrayList;

public class HelpCommand implements ClientCommand {
   private final ArrayList<ClientCommand> list;

   public HelpCommand(ArrayList<ClientCommand> local1784_1) {
      this.list = local1784_1;
   }

   @Override
   public void execute(String[] stringArray) {
      for (ClientCommand clientCommand : this.list) {
         if (stringArray.length > 0 && clientCommand.getName().equalsIgnoreCase(stringArray[0])) {
            ClientChat.processData9("&f." + clientCommand.getName() + ":", "&f" + clientCommand.getDescription());
            return;
         }
      }

      String[] stringArray2 = new String[this.list.size()];

      for (int index = 0; index < this.list.size(); index++) {
         stringArray2[index] = "&7." + this.list.get(index).getName() + " &8— &f" + this.list.get(index).getDescription();
      }

      ClientChat.processData9("&fКоманды клиента:", stringArray2);
   }

   @Override
   public String getDescription() {
      return "Показывает список всех команд";
   }

   @Override
   public ArrayList<String> suggestArguments(String[] stringArray) {
      ArrayList<String> local1787_2 = new ArrayList<>();
      if (stringArray.length <= 1) {
         for (ClientCommand clientCommand : this.list) {
            local1787_2.add(clientCommand.getName());
         }
      }

      return local1787_2;
   }

   @Override
   public String getName() {
      return "help";
   }
}
