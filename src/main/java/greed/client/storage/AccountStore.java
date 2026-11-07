package greed.client.storage;

import greed.client.module.misc.AutoRegisterModule;
import greed.client.event.EventListener;
import greed.mixin.MinecraftClientMixin5;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;

public final class AccountStore {
   private static final AccountStore accountStore = new AccountStore();
   private ArrayList<AccountEntry> list = new ArrayList<>();
   private AccountEntry accountEntry;
   private long timeMillis;
   private Path path;
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null) {
         if (packetEvent.isOutgoing() && packetEvent.getPacket() instanceof CommandExecutionC2SPacket commandExecutionC2SPacket) {
            String[] stringArray = commandExecutionC2SPacket.command().trim().split("\\s+");
            if (stringArray.length >= 2
               && (
                  stringArray[0].equalsIgnoreCase("reg")
                     || stringArray[0].equalsIgnoreCase("register")
                     || stringArray[0].equalsIgnoreCase("l")
                     || stringArray[0].equalsIgnoreCase("login")
               )) {
               minecraftClient.execute(() -> AutoRegisterModule.getInstance().password.setValue(stringArray[1]));
            }
         }

         if (this.accountEntry != null) {
            AccountEntry currentAccountEntry = this.accountEntry;
            if (packetEvent.isOutgoing() && packetEvent.getPacket() instanceof ChatMessageC2SPacket chatMessageC2SPacket) {
               minecraftClient.execute(() -> currentAccountEntry.processData5(chatMessageC2SPacket.chatMessage()));
            } else if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof GameJoinS2CPacket) {
               minecraftClient.execute(() -> {
                  if (minecraftClient.getCurrentServerEntry() != null) {
                     currentAccountEntry.processData6(formatText2(minecraftClient.getCurrentServerEntry().address));
                     this.updateState5();
                  }
               });
            }
         }
      }
   };
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      MinecraftClient minecraftClient2 = MinecraftClient.getInstance();
      if (minecraftClient2 != null && this.accountEntry != null && minecraftClient2.player != null && minecraftClient2.world != null) {
         Team team = minecraftClient2.world.getScoreboard().getScoreHolderTeam(minecraftClient2.player.getNameForScoreboard());
         String local6917_4 = team != null && team.getPrefix() != null
            ? Formatting.strip(team.getPrefix().getString()).replaceAll("[^\\p{L}\\p{N}]", "")
            : "";
         if (!local6917_4.isEmpty() && !local6917_4.equals(this.accountEntry.formatText5())) {
            this.accountEntry.processData3(local6917_4);
            this.updateState5();
         }
      }
   };

   private static AccountEntry getAccountEntry2(String local6907_0) {
      if (local6907_0 != null && !local6907_0.isBlank()) {
         String[] stringArray = local6907_0.split("\t", -1);
         if (stringArray.length < 9) {
            return null;
         }

         try {
            return new AccountEntry(
               stringArray[0],
               UUID.fromString(stringArray[1]),
               "MICROSOFT".equals(stringArray[2]) ? AccountEntry.TypeOption.MICROSOFT : AccountEntry.TypeOption.CRACKED,
               stringArray[3],
               stringArray[4],
               Long.parseLong(stringArray[5]),
               Long.parseLong(stringArray[6]),
               Boolean.parseBoolean(stringArray[7])
            );
         } catch (RuntimeException exception) {
            return null;
         }
      } else {
         return null;
      }
   }

   public void updateState() {
      if (this.accountEntry != null && this.timeMillis > 0L) {
         this.accountEntry.processData(System.currentTimeMillis() - this.timeMillis);
      }

      this.timeMillis = System.currentTimeMillis();
   }

   private static long computeLong(int local6909_0, int local6909_1, int local6909_2) {
      return LocalDate.of(local6909_0, local6909_1, local6909_2).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
   }

   public long computeLong2(AccountEntry newAccountEntry) {
      return newAccountEntry.computeLong2()
         + (newAccountEntry == this.accountEntry && this.timeMillis > 0L ? Math.max(0L, System.currentTimeMillis() - this.timeMillis) : 0L);
   }

   public void processData(AccountEntry newAccountEntry, String local6913_2) {
      String local6913_3 = local6913_2 == null ? "" : local6913_2.trim();
      if (newAccountEntry != null && !local6913_3.isEmpty() && !local6913_3.equals(newAccountEntry.formatText4())) {
         for (AccountEntry currentAccountEntry : this.list) {
            if (currentAccountEntry != newAccountEntry && currentAccountEntry.formatText4().equalsIgnoreCase(local6913_3)) {
               return;
            }
         }

         newAccountEntry.processData7(local6913_3);
         if (newAccountEntry.getNested0488() == AccountEntry.TypeOption.CRACKED) {
            newAccountEntry.processData4(AccountEntry.getUUID(local6913_3));
         }

         if (newAccountEntry == this.accountEntry) {
            this.processData5(newAccountEntry);
         }

         this.updateState5();
      }
   }

   private void updateState2() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      Session session = minecraftClient != null ? minecraftClient.getSession() : null;
      if (session != null) {
         for (AccountEntry currentAccountEntry : this.list) {
            if (currentAccountEntry.formatText4().equalsIgnoreCase(session.getUsername())) {
               this.accountEntry = currentAccountEntry;
               this.timeMillis = System.currentTimeMillis();
               return;
            }
         }
      }
   }

   public void processData2(AccountEntry newAccountEntry) {
      newAccountEntry.processData2(!newAccountEntry.checkState());
      this.updateState5();
   }

   public void processData3(AccountEntry newAccountEntry) {
      if (newAccountEntry == this.accountEntry) {
         this.updateState();
         this.accountEntry = null;
      }

      this.list.remove(newAccountEntry);
      this.updateState5();
   }

   private static long computeLong3(long local6919_0, long local6919_2, long local6919_4) {
      return ((local6919_0 * 24L + local6919_2) * 60L + local6919_4) * 60000L;
   }

   public AccountEntry getAccountEntry3(String local6921_1) {
      String local6921_2 = local6921_1.trim();
      if (local6921_2.isEmpty()) {
         return null;
      }

      for (AccountEntry currentAccountEntry : this.list) {
         if (currentAccountEntry.formatText4().equalsIgnoreCase(local6921_2)) {
            return currentAccountEntry;
         }
      }

      AccountEntry currentAccountEntry2 = AccountEntry.getAccountEntry(local6921_2);
      this.list.add(currentAccountEntry2);
      this.updateState5();
      return currentAccountEntry2;
   }

   public ArrayList<AccountEntry> getArrayList() {
      return this.list;
   }

   private void updateState3() {
   }

   public AccountEntry getAccountEntry() {
      return this.accountEntry;
   }

   private void updateState4() {
      this.list.clear();

      try {
         if (Files.exists(this.path)) {
            for (String local6925_2 : Files.readAllLines(this.path, StandardCharsets.UTF_8)) {
               AccountEntry currentAccountEntry = getAccountEntry2(local6925_2);
               if (currentAccountEntry != null) {
                  this.list.add(currentAccountEntry);
               }
            }
         }
      } catch (IOException exception) {
      }

      if (this.list.isEmpty()) {
         this.updateState3();
         this.updateState5();
      }
   }

   public boolean matchesCriteria(AccountEntry newAccountEntry) {
      return newAccountEntry != null && newAccountEntry == this.accountEntry;
   }

   private static String formatText(String local6927_0) {
      return local6927_0 == null ? "" : local6927_0.replace('\t', ' ').replace('\n', ' ');
   }

   private void processData4(String local6928_1, String local6928_2, String local6928_3, long local6928_4, long local6928_6, boolean local6928_8) {
      this.list
         .add(
            new AccountEntry(
               local6928_1,
               AccountEntry.getUUID(local6928_1),
               AccountEntry.TypeOption.CRACKED,
               local6928_3,
               local6928_2,
               local6928_4,
               local6928_6,
               local6928_8
            )
         );
   }

   public static AccountStore getAccountStore() {
      return accountStore;
   }

   private AccountStore() {
      this.path = FabricLoader.getInstance().getConfigDir().resolve("greed").resolve("altmanager.txt");
      this.updateState4();
      this.updateState2();
   }

   public static String formatText2(String local6931_0) {
      if (local6931_0 != null && !local6931_0.isBlank()) {
         String local6931_1 = local6931_0.trim().toLowerCase(Locale.ROOT).split(":", 2)[0];
         String[] stringArray = local6931_1.split("\\.");
         String local6931_3 = stringArray.length >= 2 ? stringArray[stringArray.length - 2] : stringArray[0];
         return local6931_3.isEmpty() ? "—" : Character.toUpperCase(local6931_3.charAt(0)) + local6931_3.substring(1);
      } else {
         return "—";
      }
   }

   public ArrayList<AccountEntry> getArrayList2(String local6932_1) {
      String local6932_2 = local6932_1 == null ? "" : local6932_1.trim().toLowerCase(Locale.ROOT);
      ArrayList<AccountEntry> local6932_3 = new ArrayList<>();

      for (AccountEntry currentAccountEntry : this.list) {
         if (local6932_2.isEmpty() || currentAccountEntry.formatText4().toLowerCase(Locale.ROOT).contains(local6932_2)) {
            local6932_3.add(currentAccountEntry);
         }
      }

      local6932_3.sort(
         (newAccountEntry, newAccountEntry2) -> newAccountEntry.checkState() != newAccountEntry2.checkState()
            ? (newAccountEntry.checkState() ? -1 : 1)
            : Long.compare(newAccountEntry2.computeLong(), newAccountEntry.computeLong())
      );
      return local6932_3;
   }

   public void processData5(AccountEntry newAccountEntry) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null && newAccountEntry != null) {
         ((MinecraftClientMixin5)minecraftClient).setUser(new Session(newAccountEntry.formatText4(), newAccountEntry.getUUID2(), "", Optional.empty(), Optional.empty()));
         this.updateState();
         this.accountEntry = newAccountEntry;
         this.timeMillis = System.currentTimeMillis();
         this.updateState5();
      }
   }

   public void updateState5() {
      try {
         Files.createDirectories(this.path.getParent());
         StringBuilder stringBuilder = new StringBuilder();

         for (AccountEntry currentAccountEntry : this.list) {
            stringBuilder.append(formatText(currentAccountEntry.formatText4()))
               .append('\t')
               .append(currentAccountEntry.getUUID2())
               .append('\t')
               .append(currentAccountEntry.getNested0488())
               .append('\t')
               .append(formatText(currentAccountEntry.formatText2()))
               .append('\t')
               .append(formatText(currentAccountEntry.formatText5()))
               .append('\t')
               .append(currentAccountEntry.computeLong())
               .append('\t')
               .append(currentAccountEntry == this.accountEntry ? this.computeLong2(currentAccountEntry) : currentAccountEntry.computeLong2())
               .append('\t')
               .append(currentAccountEntry.checkState())
               .append('\t')
               .append('1')
               .append('\n');
         }

         Files.writeString(this.path, stringBuilder.toString(), StandardCharsets.UTF_8);
      } catch (IOException exception) {
      }
   }

   public int computeInt(AccountEntry.TypeOption local6936_1) {
      int local6936_2 = 0;

      for (AccountEntry currentAccountEntry : this.list) {
         if (currentAccountEntry.getNested0488() == local6936_1) {
            local6936_2++;
         }
      }

      return local6936_2;
   }
}
