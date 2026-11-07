package greed.client.command;

import java.util.ArrayList;

public interface ClientCommand {
   void execute(String[] stringArray);

   String getName();

   default ArrayList<String> suggestArguments(String[] stringArray) {
      return new ArrayList<>();
   }

   String getDescription();
}
