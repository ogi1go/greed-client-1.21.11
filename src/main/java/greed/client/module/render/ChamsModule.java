package greed.client.module.render;

import greed.client.event.EventListener;
import greed.client.event.WorldRenderEvent;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.BooleanSetting;
import greed.client.setting.ColorSetting;
import greed.client.setting.LegacyNumberSetting;
import greed.client.setting.ModeSetting;
import greed.client.setting.MultiSelectSetting;
import greed.client.render.ChamsWorldRenderer;
import greed.client.ui.UiTheme;

import java.awt.Color;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@ModuleInfo(name = "Chams", description = "Показывает выбранных существ сквозь стены вместе с экипировкой.", category = ModuleCategory.RENDER)
public final class ChamsModule extends Module {
   private static ChamsModule chamsModule;
   private final long startMillis = System.currentTimeMillis();
   public ModeSetting mode;
   public MultiSelectSetting multiSelectSetting;
   public BooleanSetting customColor;
   public ColorSetting color;
   public ColorSetting friendsColor;
   public LegacyNumberSetting fill;
   public LegacyNumberSetting outline;
   public BooleanSetting waves;
   public LegacyNumberSetting waveSpeedX;
   public LegacyNumberSetting waveSpeedY;
   public LegacyNumberSetting waveScale;
   public LegacyNumberSetting waveDensity;
   public LegacyNumberSetting waveGlow;
   public BooleanSetting glow;
   public LegacyNumberSetting glowIntensity;
   public BooleanSetting pulse;
   public LegacyNumberSetting pulseSpeed;

   private final EventListener<WorldRenderEvent> worldRenderListener = worldRenderEvent -> {
      if (this.isShaderMode()) {
         ChamsWorldRenderer.getInstance().render(worldRenderEvent);
      }
   };

   private ChamsModule() {
      this.mode = this.addSetting(
         new ModeSetting("Режим", "Классический — модель, подтянутая к камере. Шейдер — анимированная заливка, обводка и свечение сквозь стены.", "Шейдер", "Классический", "Шейдер")
      );
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting("Кого показывать", "Кого видно сквозь стены.", new String[]{"Игроков", "Мобов", "Голых игроков", "Игроков в незерите"}, "Игроков")
      );
      this.customColor = this.addSetting(new BooleanSetting("Свой цвет", "Использовать свой цвет вместо цвета темы.", false));
      this.customColor.withVisibility(this::isShaderMode);
      this.color = this.addSetting(new ColorSetting("Цвет", "Цвет эффекта.").setValue(-12092417));
      this.color.withVisibility(() -> this.isShaderMode() && this.customColor.isChecked());
      this.friendsColor = this.addSetting(new ColorSetting("Цвет друзей", "Цвет эффекта на друзьях.", new Color(85, 255, 85)));
      this.friendsColor.withVisibility(this::isShaderMode);
      this.fill = this.addSetting(new LegacyNumberSetting("Заливка", "Яркость заливки внутри силуэта.", 0.5, 0.0, 1.0, 0.05));
      this.fill.withVisibility(this::isShaderMode);
      this.outline = this.addSetting(new LegacyNumberSetting("Обводка", "Яркость обводки по краю силуэта.", 0.9, 0.0, 1.0, 0.05));
      this.outline.withVisibility(this::isShaderMode);
      this.waves = this.addSetting(new BooleanSetting("Волны", "Анимированные волны внутри заливки.", true));
      this.waves.withVisibility(this::isShaderMode);
      this.waveSpeedX = this.addSetting(new LegacyNumberSetting("Скорость волн X", "Скорость дрейфа волн по горизонтали.", 0.22, 0.0, 1.5, 0.01));
      this.waveSpeedX.withVisibility(() -> this.isShaderMode() && this.waves.isChecked());
      this.waveSpeedY = this.addSetting(new LegacyNumberSetting("Скорость волн Y", "Скорость дрейфа волн по вертикали.", 0.15, 0.0, 1.5, 0.01));
      this.waveSpeedY.withVisibility(() -> this.isShaderMode() && this.waves.isChecked());
      this.waveScale = this.addSetting(new LegacyNumberSetting("Масштаб волн", "Размер узора волн.", 1.35, 0.2, 4.0, 0.05));
      this.waveScale.withVisibility(() -> this.isShaderMode() && this.waves.isChecked());
      this.waveDensity = this.addSetting(new LegacyNumberSetting("Плотность волн", "Частота полос в узоре.", 1.15, 0.5, 3.0, 0.05));
      this.waveDensity.withVisibility(() -> this.isShaderMode() && this.waves.isChecked());
      this.waveGlow = this.addSetting(new LegacyNumberSetting("Яркость волн", "Яркость светлых полос в узоре.", 1.0, 0.2, 3.0, 0.05));
      this.waveGlow.withVisibility(() -> this.isShaderMode() && this.waves.isChecked());
      this.glow = this.addSetting(new BooleanSetting("Свечение", "Мягкое свечение вокруг силуэта.", true));
      this.glow.withVisibility(this::isShaderMode);
      this.glowIntensity = this.addSetting(new LegacyNumberSetting("Сила свечения", "Радиус и яркость свечения.", 2.0, 1.0, 5.0, 0.1));
      this.glowIntensity.withVisibility(() -> this.isShaderMode() && this.glow.isChecked());
      this.pulse = this.addSetting(new BooleanSetting("Пульсация", "Плавно менять прозрачность эффекта.", false));
      this.pulse.withVisibility(this::isShaderMode);
      this.pulseSpeed = this.addSetting(new LegacyNumberSetting("Скорость пульсации", "Скорость пульсации прозрачности.", 2.0, 0.5, 5.0, 0.1));
      this.pulseSpeed.withVisibility(() -> this.isShaderMode() && this.pulse.isChecked());
   }

   @Override
   protected void onDisable() {
      ChamsWorldRenderer.getInstance().release();
   }

   public static ChamsModule getInstance() {
      if (chamsModule == null) {
         chamsModule = new ChamsModule();
      }

      return chamsModule;
   }

   public boolean isShaderMode() {
      return this.mode.matchesOption("Шейдер");
   }

   public boolean isClassicMode() {
      return !this.isShaderMode();
   }

   public int computeColor() {
      return this.customColor.isChecked() ? this.color.getValue() : UiTheme.intVal7;
   }

   public float pulseMultiplier() {
      if (!this.pulse.isChecked()) {
         return 1.0F;
      }

      float seconds = (System.currentTimeMillis() - this.startMillis) / 1000.0F;
      float wave = (float)((Math.sin(seconds * this.pulseSpeed.getValue() * Math.PI) + 1.0) * 0.5);
      return 0.65F + 0.35F * wave;
   }

   public boolean matchesCriteria(LivingEntity livingEntity) {
      if (this.isEnabled() && livingEntity != mc.player && !(livingEntity instanceof ArmorStandEntity)) {
         if (!(livingEntity instanceof PlayerEntity playerEntity)) {
            return this.multiSelectSetting.matchesOption("Мобов");
         } else {
            if (this.multiSelectSetting.matchesOption("Игроков")) {
               return true;
            }

            int equippedCount = 0;
            int netheriteCount = 0;

            for (EquipmentSlot equipmentSlot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
               ItemStack equippedStack = playerEntity.getEquippedStack(equipmentSlot);
               if (!equippedStack.isEmpty()) {
                  equippedCount++;
                  if (equippedStack.isOf(Items.NETHERITE_HELMET)
                     || equippedStack.isOf(Items.NETHERITE_CHESTPLATE)
                     || equippedStack.isOf(Items.NETHERITE_LEGGINGS)
                     || equippedStack.isOf(Items.NETHERITE_BOOTS)) {
                     netheriteCount++;
                  }
               }
            }

            return this.multiSelectSetting.matchesOption("Голых игроков") && equippedCount == 0
               || this.multiSelectSetting.matchesOption("Игроков в незерите") && netheriteCount > 0;
         }
      } else {
         return false;
      }
   }
}
