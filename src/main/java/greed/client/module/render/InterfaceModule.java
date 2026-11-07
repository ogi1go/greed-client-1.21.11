package greed.client.module.render;

import greed.client.ui.hud.TargetHud;
import greed.client.util.TargetHudStyle;

import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.setting.LegacyNumberSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.MultiSelectSetting;
import greed.client.event.PacketEvent;
import greed.client.ui.ClickGuiStyle;
import greed.client.setting.ColorSetting;
import greed.client.util.TargetHudStyle;
import greed.client.setting.TextSetting;
import greed.client.module.player.UseTrackerModule;

import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;

@ModuleInfo(name = "Interface", description = "Какие элементы худа показывать.", category = ModuleCategory.RENDER)
public final class InterfaceModule extends Module {
   private static InterfaceModule interfaceModule;
   public ColorSetting colorSetting;
   public MultiSelectSetting elements;
   public LegacyNumberSetting cooldownSlotDarkening;
   public BooleanSetting centerAlignment;
   public MultiSelectSetting components;
   public TextSetting textSetting;
   public BooleanSetting showDonationPotions;
   public BooleanSetting headParticlesOnHit;
   public BooleanSetting moduleSounds;
   public LegacyNumberSetting soundVolume;
   private int intVal;
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof PlaySoundS2CPacket playSoundS2CPacket) {
         mc.execute(
            () -> {
               if (mc.player != null && mc.world != null && this.isEnabled()) {
                  TargetHudStyle.processData7(
                     mc.player, playSoundS2CPacket.getX(), playSoundS2CPacket.getY(), playSoundS2CPacket.getZ(), playSoundS2CPacket.getSound().value().id().toString()
                  );
               }
            }
         );
      }
   };

   private InterfaceModule() {
      this.colorSetting = this.addSetting(new ColorSetting("Тема", "Акцентный цвет всего клиента.").setValue(-12092417));
      this.elements = this.addSetting(
         new MultiSelectSetting(
            "Элементы",
            "Какие элементы худа рисовать.",
            new String[]{"watermark", "keybinds", "serverbinds", "potions", "cooldowns", "staffs", "target", "inventory", "Target", "toasts"},
            "watermark",
            "keybinds",
            "serverbinds",
            "potions",
            "cooldowns",
            "staffs",
            "target",
            "Target",
            "toasts"
         )
      );
      this.cooldownSlotDarkening = this.addSetting(
         new LegacyNumberSetting("Затемнение слота кд", "Насколько затемнять слот хотбара под цифрой кулдауна.", 25.0, 0.0, 100.0, 1.0)
      );
      this.cooldownSlotDarkening.withVisibility(() -> this.elements.matchesOption("cooldowns"));
      this.centerAlignment = this.addSetting(new BooleanSetting("Отцентрировать", "Держать ватермарку по центру сверху и уступать боссбару.", false));
      this.centerAlignment.withVisibility(() -> false);
      this.components = this.addSetting(
         new MultiSelectSetting(
            "Компоненты",
            "Что показывать в ватермарке.",
            new String[]{"Аватарка", "Логин", "ФПС", "Пинг", "Координаты", "Время"},
            "Аватарка",
            "Логин",
            "ФПС",
            "Время"
         )
      );
//      this.components.withVisibility(() -> false);
      this.textSetting = this.addSetting(new TextSetting("Порядок ватермарки", "Порядок компонентов ватермарки."));
//      this.textSetting.withVisibility(() -> false);
      this.showDonationPotions = this.addSetting(
         new BooleanSetting("Показывать донатные зелья", "Показывать в зельях донатные зелья FunTime и SpookyTime вместо их эффектов.", true)
      );
      this.showDonationPotions.withVisibility(() -> false);
      this.headParticlesOnHit = this.addSetting(
         new BooleanSetting("Партиклы из головы при ударе", "При ударе по цели из её головы в TargetHud вылетают светящиеся частицы.", true)
      );
      this.headParticlesOnHit.withVisibility(() -> false);
      this.moduleSounds = this.addSetting(new BooleanSetting("Звуки модулей", "Проигрывать звук при включении и выключении модуля.", true));
      this.soundVolume = this.addSetting(new LegacyNumberSetting("Громкость звуков", "Громкость звуков модулей в процентах.", 12.0, 0.0, 100.0, 1.0));
      this.soundVolume.withVisibility(() -> this.moduleSounds.isChecked());
   }

   public boolean matchesCriteria(String local3516_1) {
      return local3516_1.equals("arrows")
         ? UseTrackerModule.getInstance().isEnabled() && UseTrackerModule.getInstance().arrowCounter.isChecked()
         : this.isEnabled() && this.elements.matchesOption(local3516_1);
   }

   public static InterfaceModule getInstance() {
      if (interfaceModule == null) {
         interfaceModule = new InterfaceModule();
      }

      return interfaceModule;
   }

   public void updateState() {
      int local3523_1 = 0xFF000000 | this.colorSetting.getValue() & 16777215;
      if (local3523_1 != this.intVal) {
         this.intVal = local3523_1;
         ClickGuiStyle.processData(local3523_1);
      }
   }

   public boolean checkState() {
      return this.isEnabled() && this.centerAlignment.isChecked();
   }
}
