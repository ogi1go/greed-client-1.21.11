package greed.client.command;

import greed.mixin.BossBarHudMixin;
import greed.client.util.ClientChat;
import greed.client.event.EventBus;
import greed.client.event.EventListener;
import greed.client.MinecraftAccess;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.util.Stopwatch;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public class RctCommand implements ClientCommand, MinecraftAccess {
   private static final Pattern pattern = Pattern.compile("(?:#|№)\\s*(\\d+)(?!\\d)");
   private final Stopwatch stopwatch = new Stopwatch();
   private boolean flag;
   private boolean flag2;
   private boolean flag3;
   private int intVal;
   private long timeMillis;
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (this.intVal != 0 && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket && packetEvent.isIncoming()) {
         String local1024_3 = gameMessageS2CPacket.content().getString().toLowerCase();
         if (!local1024_3.contains("хаб") && local1024_3.contains("не удалось")) {
            ClientChat.processData8("&7На эту анархию нельзя зайти");
            this.updateState();
            EventBus.getInstance().unsubscribe(this);
         }
      }
   };
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (this.timeMillis == 0L) {
         if (this.intVal != 0) {
            if (!this.formatText().equals("HolyWorld")) {
               this.updateState();
               EventBus.getInstance().unsubscribe(this);
            } else if (!this.flag && this.computeInt2("HolyWorld") == this.intVal) {
               this.updateState();
               EventBus.getInstance().unsubscribe(this);
            } else if (this.flag) {
               if (this.computeInt2("HolyWorld") == -1) {
                  this.flag = false;
               } else {
                  mc.player.networkHandler.sendChatCommand("hub");
               }
            } else if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen
               && genericContainerScreen.getTitle().getString().equals("Выбор Лайт анархии:")) {
               if (genericContainerScreen.getScreenHandler().getInventory().size() < 10) {
                  if (!this.flag2) {
                     InventoryUtils.sendSlotClickPacket(computeInt3(this.intVal), 0, SlotActionType.PICKUP, false);
                     this.flag2 = true;
                  }
               } else {
                  if (!this.flag3) {
                     int local1022_3 = computeInt6(genericContainerScreen, this.intVal);
                     if (local1022_3 < 0) {
                        local1022_3 = computeInt5(this.intVal);
                     }

                     InventoryUtils.sendSlotClickPacket(local1022_3, 0, SlotActionType.PICKUP, false);
                     this.flag3 = true;
                  }
               }
            } else {
               if (this.stopwatch.hasExceededMillis(500L)) {
                  this.stopwatch.reset();
                  mc.player.networkHandler.sendChatCommand("lite");
               }
            }
         }
      } else if (mc.player != null && mc.world != null && System.currentTimeMillis() >= this.timeMillis) {
         int local1022_2 = this.computeInt2(this.formatText());
         if (local1022_2 < 1 || local1022_2 > 63) {
            local1022_2 = Math.abs(this.intVal);
         }

         mc.player.networkHandler.sendChatCommand("an" + local1022_2);
         this.updateState();
         EventBus.getInstance().unsubscribe(this);
      }
   };

   private String formatText() {
      if (mc.player != null
         && mc.world != null
         && mc.getNetworkHandler() != null
         && mc.getNetworkHandler().getServerInfo() != null
         && mc.getNetworkHandler().getBrand() != null) {
         String local1019_1 = mc.getNetworkHandler().getServerInfo().address.toLowerCase(Locale.ROOT);
         String local1019_2 = mc.getNetworkHandler().getBrand().toLowerCase(Locale.ROOT);
         if (local1019_2.contains("botfilter")) {
            return "FunTime";
         } else if (local1019_1.contains("funtime")
            || local1019_1.contains("skytime")
            || local1019_1.contains("space-times")
            || local1019_1.contains("funsky")
            || local1019_1.contains("spookytime")) {
            return "CopyTime";
         } else if (local1019_2.contains("holyworld")
            || local1019_2.contains("leaf")
            || local1019_2.contains("vk.com/idwok")
            || local1019_2.contains("(proxy ")) {
            return "HolyWorld";
         } else {
            return local1019_1.contains("reallyworld") ? "ReallyWorld" : "Vanilla";
         }
      } else {
         return "Vanilla";
      }
   }

   @Override
   public String getName() {
      return "rct";
   }

   private static int computeInt(ItemStack itemStack) {
      int local1023_1 = computeInt4(itemStack.getName().getString());
      if (local1023_1 > 0) {
         return local1023_1;
      }

      LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
      if (loreComponent == null) {
         return -1;
      }

      for (Text text : loreComponent.lines()) {
         local1023_1 = computeInt4(text.getString());
         if (local1023_1 > 0) {
            return local1023_1;
         }
      }

      return -1;
   }

   private static boolean matchesCriteria(String local1025_0) {
      return local1025_0.equals("CopyTime") || local1025_0.equals("FunTime");
   }

   private int computeInt2(String local1026_1) {
      Scoreboard scoreboard = mc.world.getScoreboard();
      ScoreboardObjective objectiveForSlot = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      switch (local1026_1) {
         case "FunTime":
         case "CopyTime":
            if (objectiveForSlot != null) {
               String[] stringArray = objectiveForSlot.getDisplayName().getString().split("-");
               if (stringArray.length > 1) {
                  return Integer.parseInt(stringArray[1]);
               }
            }
            break;
         case "HolyWorld":
            if (objectiveForSlot == null) {
               return -1;
            }

            for (ScoreboardEntry scoreboardEntry : scoreboard.getScoreboardEntries(objectiveForSlot)) {
               String string = Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name()).getString();
               int local1026_9 = string.indexOf(35);
               int local1026_10 = string.indexOf(" -◆-", local1026_9 + 1);
               if (local1026_9 != -1 && local1026_10 > local1026_9 + 1) {
                  return Integer.parseInt(string.substring(local1026_9 + 1, local1026_10));
               }
            }
      }

      return -1;
   }

   @Override
   public String getDescription() {
      return "Переподключение к текущей анархии (HolyWorld / CopyTime)";
   }

   private static int computeInt3(int local1028_0) {
      return local1028_0 < 15 ? 0 : (local1028_0 < 33 ? 1 : (local1028_0 < 48 ? 2 : 3));
   }

   @Override
   public void execute(String[] stringArray) {
      String local1029_2 = this.formatText();
      if (!local1029_2.equals("HolyWorld") && !matchesCriteria(local1029_2)) {
         ClientChat.processData8("&7Не работает на этом сервере");
      } else if (local1029_2.equals("HolyWorld")
         && ((BossBarHudMixin)mc.inGameHud.getBossBarHud())
            .getEvents()
            .values()
            .stream()
            .map(clientBossBar -> clientBossBar.getName().getString().toLowerCase())
            .anyMatch(local1030_0 -> local1030_0.contains("pvp") || local1030_0.contains("пвп"))) {
         ClientChat.processData8("&7Вы в режиме PvP");
      } else {
         int local1029_3 = this.computeInt2(local1029_2);
         if (local1029_3 > 0 && (local1029_3 < 64 || matchesCriteria(local1029_2))) {
            EventBus.getInstance().subscribe(this);
            this.flag2 = false;
            this.flag3 = false;
            this.intVal = local1029_3;
            if (matchesCriteria(local1029_2)) {
               this.flag = false;
               this.timeMillis = System.currentTimeMillis() + 900L;
               mc.player.networkHandler.sendChatCommand("hub");
            } else {
               this.flag = true;
               this.timeMillis = 0L;
            }
         } else {
            ClientChat.processData8("&7Не верный лайт");
         }
      }
   }

   private static int computeInt4(String local1031_0) {
      if (local1031_0 != null && !local1031_0.isEmpty()) {
         Matcher matcher = pattern.matcher(local1031_0);
         return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
      } else {
         return -1;
      }
   }

   private static int computeInt5(int local1033_0) {
      int local1033_1 = local1033_0 < 15 ? 1 : (local1033_0 < 33 ? 15 : (local1033_0 < 48 ? 33 : 48));
      return 18 + local1033_0 - local1033_1;
   }

   private void updateState() {
      this.intVal = 0;
      this.flag = false;
      this.timeMillis = 0L;
      this.flag2 = false;
      this.flag3 = false;
   }

   private static int computeInt6(GenericContainerScreen genericContainerScreen, int local1035_1) {
      for (Slot slot : genericContainerScreen.getScreenHandler().slots) {
         if (slot.id >= 18 && !slot.getStack().isEmpty() && computeInt(slot.getStack()) == local1035_1) {
            return slot.id;
         }
      }

      return -1;
   }
}
