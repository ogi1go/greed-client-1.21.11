package greed.client.module.render;

import greed.client.event.EventListener;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.MultiSelectSetting;
import greed.client.event.PlayerTickEvent;
import greed.client.setting.SectionSetting;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundCategory;

@ModuleInfo(name = "NoRender", description = "Отключение назойливых визуальных эффектов и звуков.", category = ModuleCategory.RENDER)
public final class NoRenderModule extends Module {
   private static NoRenderModule noRenderModule;
   public MultiSelectSetting overlays;
   public MultiSelectSetting sounds;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && this.matchesCriteria2("Эффекты вардена")) {
         mc.player.removeStatusEffect(StatusEffects.DARKNESS);
      }
   };

   public boolean matchesCriteria(SoundInstance soundInstance) {
      if (!this.isEnabled()) {
         return false;
      } else {
         String path = soundInstance.getId().getPath();
         if (this.sounds.matchesOption("Пластинки") && soundInstance.getCategory() == SoundCategory.RECORDS) {
            return true;
         } else if (this.sounds.matchesOption("Портал") && path.startsWith("block.portal.")) {
            return true;
         } else if (this.sounds.matchesOption("Иссушитель") && path.startsWith("entity.wither.")) {
            return true;
         } else if (this.sounds.matchesOption("Варден") && path.startsWith("entity.warden.")) {
            return true;
         } else if (this.sounds.matchesOption("Шипы") && path.equals("enchant.thorns.hit")) {
            return true;
         } else {
            return !this.sounds.matchesOption("Опыт")
                  || !path.equals("entity.experience_orb.pickup")
                  || mc.player != null
                     && !(
                        (soundInstance.getX() - mc.player.getX()) * (soundInstance.getX() - mc.player.getX())
                              + (soundInstance.getY() - mc.player.getY()) * (soundInstance.getY() - mc.player.getY())
                              + (soundInstance.getZ() - mc.player.getZ()) * (soundInstance.getZ() - mc.player.getZ())
                           > 4.0
                     )
               ? this.sounds.matchesOption("Копание") && path.startsWith("block.") && (path.endsWith(".hit") || path.endsWith(".break"))
               : true;
         }
      }
   }

   public static NoRenderModule getInstance() {
      if (noRenderModule == null) {
         noRenderModule = new NoRenderModule();
      }

      return noRenderModule;
   }

   private NoRenderModule() {
      this.addSetting(new SectionSetting("Отключение оверлея"));
      this.overlays = this.addSetting(
         new MultiSelectSetting(
            "Оверлеи",
            "Какие эффекты отключать.",
            new String[]{
               "Темнота",
               "Туман слепоты",
               "Тряска экрана",
               "Камера при уроне",
               "Эффекты FOV",
               "Оверлей лавы",
               "Оверлей огня",
               "Оверлей воды",
               "Тыква на голове",
               "Затемнение блоков",
               "Иссушающие сердца",
               "Эффекты вардена",
               "Индикатор атаки",
               "Скорборд",
               "Анимация тотема"
            },
            "Темнота",
            "Тряска экрана"
         )
      );
      this.addSetting(new SectionSetting("Отключение звуков"));
      this.sounds = this.addSetting(
         new MultiSelectSetting(
            "Звуки", "Какие звуки заглушать.", new String[]{"Опыт", "Пластинки", "Портал", "Иссушитель", "Шипы", "Копание", "Варден"}, "Опыт", "Портал"
         )
      );
   }

   public boolean matchesCriteria2(String local5588_1) {
      return this.isEnabled() && this.overlays.matchesOption(local5588_1);
   }
}
