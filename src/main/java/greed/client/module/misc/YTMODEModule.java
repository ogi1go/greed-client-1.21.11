package greed.client.module.misc;

import greed.client.util.ClientChat;
import greed.client.event.EventListener;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PlayerTickEvent;

import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

@ModuleInfo(name = "YTMODE", description = "Прячет фортуну, маскирует боссбар под дуэль и рисует невидимкам 1000 хп.", category = ModuleCategory.MISC)
public final class YTMODEModule extends Module {
   private static YTMODEModule yTMODEModule;
   public ModeSetting modeSetting;
   private long timeMillis;
   private ServerInfo serverInfo2;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (this.timeMillis != 0L && System.currentTimeMillis() >= this.timeMillis && this.serverInfo2 != null && mc.world != null) {
         this.timeMillis = 0L;
         ServerInfo serverInfo = this.serverInfo2;
         mc.send(() -> {
            if (mc.world != null) {
               mc.world.disconnect(Text.literal("YTMODE: смена скина"));
               mc.disconnectWithProgressScreen();
               ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), mc, ServerAddress.parse(serverInfo.address), serverInfo, false, null);
            }
         });
      }
   };

   public Text getText() {
      return this.modeSetting.isSelected("2x2")
         ? ClientChat.getMutableText("Командная дуэль", 47084, 3047398)
         : ClientChat.getMutableText("Обычная дуэль", 47084, 3047398);
   }

   public Color getColor() {
      return Color.BLUE;
   }

   public boolean checkState() {
      return this.isEnabled() && !this.modeSetting.isSelected("Rtp");
   }

   private YTMODEModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Режим", "Подо что маскируется картинка.", "1x1", "1x1", "Rtp", "2x2"));
   }

   public static YTMODEModule getInstance() {
      if (yTMODEModule == null) {
         yTMODEModule = new YTMODEModule();
      }

      return yTMODEModule;
   }

   public boolean matchesCriteria(ScoreboardEntry scoreboardEntry) {
      return mc.world == null
         ? false
         : Team.decorateName(mc.world.getScoreboard().getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name())
            .getString()
            .toLowerCase()
            .contains("фортун");
   }

   @Override
   protected void onEnable() {
      if (mc.player != null && mc.getNetworkHandler() != null && !mc.isConnectedToLocalServer()) {
         mc.player.networkHandler.sendChatCommand("skin s_FunTime_1");
         this.serverInfo2 = mc.getCurrentServerEntry();
         this.timeMillis = System.currentTimeMillis() + 1000L;
      }
   }

   public boolean matchesCriteria2(String local2102_1) {
      return !local2102_1.contains("\n")
            || !local2102_1.toLowerCase().contains("spookytime") && !local2102_1.replaceAll("(?i)§[0-9a-fk-or]", "").strip().startsWith("**")
         ? !local2102_1.replaceAll("(?i)§[0-9a-fk-or]", "").strip().isEmpty()
            && "╔╠╚".indexOf(local2102_1.replaceAll("(?i)§[0-9a-fk-or]", "").strip().charAt(0)) >= 0
         : true;
   }

   @Override
   protected void onDisable() {
      this.timeMillis = 0L;
      this.serverInfo2 = null;
   }
}
