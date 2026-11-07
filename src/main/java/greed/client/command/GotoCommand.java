package greed.client.command;

import greed.client.MinecraftAccess;
import greed.client.util.ClientChat;
import greed.client.util.greedtone.Goal;
import greed.client.util.greedtone.PathExecutor;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;

public class GotoCommand implements MinecraftAccess, ClientCommand {
    @Override
    public void execute(String[] stringArray) {
        if (stringArray.length == 0) {
            if (PathExecutor.isActive()) {
                PathExecutor.stop();
                ClientChat.processData8("Навигация остановлена");
            } else {
                ClientChat.processData8("Использование: &7.goto <x> <y> <z> &rили &7.goto stop");
            }
            return;
        }

        if (stringArray.length == 1 && stringArray[0].equalsIgnoreCase("stop")) {
            if (PathExecutor.isActive()) {
                PathExecutor.stop();
                ClientChat.processData8("Навигация остановлена");
            } else {
                ClientChat.processData8("Навигация не активна");
            }
            return;
        }

        if (stringArray.length != 3) {
            ClientChat.processData8("Использование: &7.goto <x> <y> <z> &rили &7.goto stop");
            return;
        }

        if (mc.player == null) {
            return;
        }

        try {
            int x = parseCoord(stringArray[0], mc.player.getBlockX());
            int y = parseCoord(stringArray[1], mc.player.getBlockY());
            int z = parseCoord(stringArray[2], mc.player.getBlockZ());
            PathExecutor.goTo(new Goal.AtBlock(new BlockPos(x, y, z)));
            ClientChat.processData8("Иду к &7" + x + " " + y + " " + z);
        } catch (NumberFormatException e) {
            ClientChat.processData8("Координаты должны быть числами");
        }
    }

    private int parseCoord(String s, int base) {
        if (s.equals("~")) {
            return base;
        }
        if (s.startsWith("~")) {
            String rest = s.substring(1);
            if (rest.isEmpty()) {
                return base;
            }
            return base + Integer.parseInt(rest);
        }
        // try as number (int/float)
        if (s.matches("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?")) {
            double d = Double.parseDouble(s);
            return (int) Math.floor(d);
        }
        return Integer.parseInt(s);
    }

    @Override
    public String getName() {
        return "goto";
    }

    @Override
    public String getDescription() {
        return "Идёт по гридтону к координатам (.goto <x> <y> <z> / stop)";
    }

    @Override
    public ArrayList<String> suggestArguments(String[] stringArray) {
        ArrayList<String> list = new ArrayList<>();
        if (stringArray.length == 0) {
            list.add("stop");
            if (mc.player != null) {
                list.add(mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ());
            }
            return list;
        }
        if (stringArray.length == 1) {
            list.add("stop");
            return list;
        }
        return list;
    }
}
