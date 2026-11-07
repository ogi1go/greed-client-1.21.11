package greed.client.module.misc;

import greed.client.setting.BooleanSetting;
import greed.mixin.ClientCommonNetworkHandlerMixin;
import greed.client.event.EventListener;
import greed.client.storage.FriendStore;
import greed.client.setting.LegacyNumberSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.MultiSelectSetting;
import greed.client.event.PlayerTickEvent;
import greed.client.setting.NumericTextSetting;
import greed.client.ui.hud.StaffHud;
import greed.client.ui.hud.NotificationHud;

import java.util.Locale;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

@ModuleInfo(name = "AutoLeave", description = "Сам уходит с сервера: мало ХП, модератор в табе или чужой игрок рядом.", category = ModuleCategory.MISC)
public final class AutoLeaveModule extends Module {
   private static AutoLeaveModule autoLeaveModule;
   public MultiSelectSetting multiSelectSetting;
   public BooleanSetting booleanSetting;
   public NumericTextSetting numericTextSetting;
   public LegacyNumberSetting minHealth;
   public LegacyNumberSetting distance;
   private boolean flag;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (!this.flag
         && mc.player != null
         && mc.world != null
         && mc.getNetworkHandler() != null
         && !mc.isConnectedToLocalServer()) {
         if (this.multiSelectSetting.matchesOption("Мало ХП") && mc.player.getHealth() <= this.minHealth.getValue()) {
            this.processData("Здоровье " + String.format(Locale.US, "%.1f", mc.player.getHealth()) + " хп");
         } else {
            if (this.multiSelectSetting.matchesOption("Модератор")) {
               for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getPlayerList()) {
                  if (StaffHud.pattern.matcher(playerListEntry.getProfile().name()).matches()) {
                     Team team = mc.world.getScoreboard().getScoreHolderTeam(playerListEntry.getProfile().name());
                     String local7421_5 = team != null && !team.getPrefix().getString().trim().isEmpty()
                        ? team.getPrefix().getString().trim()
                        : (
                           playerListEntry.getDisplayName() == null
                              ? ""
                              : playerListEntry.getDisplayName().getString().replace(playerListEntry.getProfile().name(), "").trim()
                        );
                     if (!local7421_5.isEmpty() && StaffHud.pattern2.matcher(local7421_5.toLowerCase(Locale.ROOT)).matches()) {
                        this.processData("Модератор " + playerListEntry.getProfile().name() + " в табе");
                        return;
                     }
                  }
               }
            }

            if (this.multiSelectSetting.matchesOption("Игрок рядом")) {
               for (AbstractClientPlayerEntity abstractClientPlayerEntity : mc.world.getPlayers()) {
                  if (abstractClientPlayerEntity != mc.player
                     && !FriendStore.isFriend(abstractClientPlayerEntity.getGameProfile().name())
                     && !(abstractClientPlayerEntity.distanceTo(mc.player) > this.distance.getValue())) {
                     this.processData(
                        "Игрок "
                           + abstractClientPlayerEntity.getGameProfile().name()
                           + " в "
                           + String.format(Locale.US, "%.1f", abstractClientPlayerEntity.distanceTo(mc.player))
                           + " бл"
                     );
                     return;
                  }
               }
            }
         }
      }
   };

   private void processData(String local7417_1) {
      this.flag = true;
      if (this.booleanSetting.isChecked()) {
         ((ClientCommonNetworkHandlerMixin)mc.player.networkHandler).connection().disconnect(Text.literal("[AutoLeave] " + local7417_1));
      } else if (!this.numericTextSetting.getText().isBlank()) {
         mc.player
            .networkHandler
            .sendChatCommand(this.numericTextSetting.getText().startsWith("/") ? this.numericTextSetting.getText().substring(1) : this.numericTextSetting.getText());
         NotificationHud.processData3(this.category.getIcon(), "AutoLeave", local7417_1, true);
      }

      this.setEnabled(false);
   }

   @Override
   protected void onEnable() {
      this.flag = false;
   }

   private AutoLeaveModule() {
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting(
            "Условия", "Когда уходить: мало ХП, модератор появился в табе, не-друг подошёл близко.", new String[]{"Мало ХП", "Модератор", "Игрок рядом"}
         )
      );
      this.booleanSetting = this.addSetting(new BooleanSetting("Разрыв соединения", "Полностью выйти с сервера вместо команды.", false));
      this.numericTextSetting = this.addSetting(new NumericTextSetting("Команда", "Команда ухода, когда разрыв выключен.", "/hub", 32));
      this.numericTextSetting.withVisibility(() -> !this.booleanSetting.isChecked());
      this.minHealth = this.addSetting(new LegacyNumberSetting("Мин. ХП", "Уходить, когда здоровье не выше этого.", 5.0, 1.0, 20.0, 0.5));
      this.minHealth.withVisibility(() -> this.multiSelectSetting.matchesOption("Мало ХП"));
      this.distance = this.addSetting(new LegacyNumberSetting("Дистанция", "Уходить, когда не-друг ближе этого расстояния.", 10.0, 1.0, 100.0, 1.0));
      this.distance.withVisibility(() -> this.multiSelectSetting.matchesOption("Игрок рядом"));
   }

   public static AutoLeaveModule getInstance() {
      if (autoLeaveModule == null) {
         autoLeaveModule = new AutoLeaveModule();
      }

      return autoLeaveModule;
   }
}
