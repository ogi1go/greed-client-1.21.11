package greed.client.module.render;

import greed.client.setting.BooleanSetting;
import greed.client.util.Colors;
import greed.client.event.EventListener;
import greed.client.storage.FriendStore;
import greed.client.event.HudRenderEvent;
import greed.client.setting.LegacyNumberSetting;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.MultiSelectSetting;
import greed.client.util.GuiScale;
import greed.client.setting.ColorSetting;
import greed.client.render.ThemeColors;
import greed.client.ui.hud.HudRenderContext;
import greed.client.render.Render2D;
import greed.client.RotationAngles;

import java.awt.Color;
import java.util.HashMap;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

@ModuleInfo(name = "Arrows", description = "Кольцо стрелок вокруг прицела показывает, куда повернуться до игрока.", category = ModuleCategory.RENDER)
public final class ArrowsModule extends Module {
   private static ArrowsModule arrowsModule;
   public MultiSelectSetting multiSelectSetting;
   public ColorSetting colorSetting;
   public ModeSetting modeSetting;
   public BooleanSetting renderIn3d;
   public LegacyNumberSetting size;
   public LegacyNumberSetting range;
   public BooleanSetting changeOnSprint;
   public LegacyNumberSetting radiusOnSprint;
   public BooleanSetting changeInMenu;
   public LegacyNumberSetting radiusInMenu;
   private float floatVal = 50.0F;
   private final HashMap<Integer, float[]> map = new HashMap<>();
   private final EventListener<HudRenderEvent> hudRenderListener = hudRenderEvent -> {
      if (mc.player != null && mc.world != null) {
         float local4978_2 = MathHelper.clamp(52.0F / Math.max(1, mc.getCurrentFps()), 0.0F, 1.0F);
         float local4978_3 = hudRenderEvent.computeInt2() / 2.0F * GuiScale.computeInt() / GuiScale.computeFloat();
         float local4978_4 = hudRenderEvent.computeInt() / 2.0F * GuiScale.computeInt() / GuiScale.computeFloat();
         this.floatVal = this.floatVal
            + (
                  (
                        this.changeInMenu.isChecked() && mc.currentScreen instanceof HandledScreen
                           ? this.radiusInMenu.getValue()
                           : (this.changeOnSprint.isChecked() && mc.player.isSprinting() ? this.radiusOnSprint.getValue() : this.range.getValue())
                     )
                     - this.floatVal
               )
               * local4978_2;
         float local4978_5 = (float)Math.toRadians(RotationAngles.getCameraYaw());
         HudRenderContext.renderElement(hudRenderEvent.getDrawContext());
         if (this.renderIn3d.isChecked()) {
            HudRenderContext.processData2(
               local4978_3, local4978_4, 1.0F, MathHelper.cos(Math.toRadians(MathHelper.clamp(-RotationAngles.getCameraPitch() + 90.0F, -60.0F, 60.0F)))
            );
         }

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity livingEntity && this.matchesCriteria(livingEntity)) {
               double local4978_9 = livingEntity.getLerpedPos(hudRenderEvent.computeFloat()).x - mc.gameRenderer.getCamera().getCameraPos().x;
               double local4978_11 = livingEntity.getLerpedPos(hudRenderEvent.computeFloat()).z - mc.gameRenderer.getCamera().getCameraPos().z;
               float local4978_13 = (float)Math.toDegrees(
                  Math.atan2(
                     -(local4978_11 * MathHelper.cos(local4978_5) - local4978_9 * MathHelper.sin(local4978_5)),
                     -(local4978_9 * MathHelper.cos(local4978_5) + local4978_11 * MathHelper.sin(local4978_5))
                  )
               );
               float local4978_14 = livingEntity instanceof PlayerEntity playerEntity && FriendStore.isFriend(playerEntity.getGameProfile().name())
                  ? 1.0F
                  : 0.0F;
               float[] floatArray = this.map.computeIfAbsent(livingEntity.getId(), local4973_2 -> new float[]{local4978_13, 0.0F, local4978_14});
               floatArray[0] = MathHelper.lerpAngleDegrees(local4978_2, floatArray[0], local4978_13);
               floatArray[1] += (1.0F - floatArray[1]) * local4978_2;
               floatArray[2] += (local4978_14 - floatArray[2]) * local4978_2;
               this.processData(floatArray, local4978_3, local4978_4);
            }
         }

         this.map.entrySet().removeIf(local4972_4 -> {
            if (mc.world.getEntityById(local4972_4.getKey()) instanceof LivingEntity livingEntity2 && this.matchesCriteria(livingEntity2)) {
               return false;
            } else {
               local4972_4.getValue()[1] -= ((float[])local4972_4.getValue())[1] * local4978_2;
               this.processData(local4972_4.getValue(), local4978_3, local4978_4);
               return local4972_4.getValue()[1] < 0.01F;
            }
         });
         if (this.renderIn3d.isChecked()) {
            HudRenderContext.updateState();
         }
      }
   };

   private ArrowsModule() {
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting("Кого показывать", "На кого вести стрелки.", new String[]{"Игроки", "Друзья", "Мобы"}, "Игроки", "Друзья")
      );
      this.colorSetting = this.addSetting(new ColorSetting("Цвет друзей", "Каким цветом вести стрелку на друга.", new Color(60, 232, 107)));
      this.renderIn3d = this.addSetting(new BooleanSetting("Рендерить в 3д", "Кладёт кольцо стрелок на плоскость по наклону взгляда.", false));
      this.size = this.addSetting(new LegacyNumberSetting("Размер", "Размер стрелки.", 15.0, 6.0, 40.0, 0.5));
      this.range = this.addSetting(new LegacyNumberSetting("Радиус", "Радиус кольца стрелок.", 50.0, 25.0, 150.0, 0.1));
      this.changeOnSprint = this.addSetting(new BooleanSetting("Менять при спринте", "Разводить кольцо шире, пока бежишь.", true));
      this.radiusOnSprint = this.addSetting(new LegacyNumberSetting("Радиус при спринте", "Радиус кольца во время спринта.", 100.0, 25.0, 150.0, 0.1));
      this.radiusOnSprint.withVisibility(() -> this.changeOnSprint.isChecked());
      this.changeInMenu = this.addSetting(new BooleanSetting("Менять в меню", "Разводить кольцо шире с открытым инвентарём или контейнером.", true));
      this.radiusInMenu = this.addSetting(
         new LegacyNumberSetting("Радиус в меню", "Радиус кольца с открытым инвентарём или контейнером.", 130.0, 25.0, 150.0, 0.1)
      );
      this.radiusInMenu.withVisibility(() -> this.changeInMenu.isChecked());
   }

   @Override
   protected void onDisable() {
      this.map.clear();
   }

   public static ArrowsModule getInstance() {
      if (arrowsModule == null) {
         arrowsModule = new ArrowsModule();
      }

      return arrowsModule;
   }

   private void processData(float[] floatArray, float local4980_2, float local4980_3) {
      float local4980_4 = local4980_2 + this.floatVal * MathHelper.cos(Math.toRadians(floatArray[0])) - this.size.getValue() / 2.0F;
      float local4980_5 = local4980_3 + this.floatVal * MathHelper.sin(Math.toRadians(floatArray[0])) - this.size.getValue() / 2.0F;
      Render2D.drawImageRotatedGlow(
         "assets/greed/textures/images/arrow_nav_hollow.png",
         local4980_4,
         local4980_5,
         this.size.getValue(),
         0.0F,
         floatArray[0] + 90.0F,
         local4980_4 + this.size.getValue() / 2.0F,
         local4980_5 + this.size.getValue() / 2.0F,
         Colors.computeInt6(
            Colors.computeInt2(ThemeColors.intVal9, this.colorSetting.getValue(), floatArray[2]), floatArray[1]
         )
      );
   }

   private boolean matchesCriteria(LivingEntity livingEntity) {
      if (livingEntity == mc.player || livingEntity instanceof ArmorStandEntity || !livingEntity.isAlive()) {
         return false;
      } else if (livingEntity instanceof PlayerEntity playerEntity) {
         return FriendStore.isFriend(playerEntity.getGameProfile().name())
            ? this.multiSelectSetting.matchesOption("Друзья")
            : this.multiSelectSetting.matchesOption("Игроки");
      } else {
         return this.multiSelectSetting.matchesOption("Мобы");
      }
   }
}
