package greed.client.module.render;

import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.setting.KeybindSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.NumberSetting;
import greed.client.event.PlayerTickEvent;
import greed.client.ui.AnimatedValue;
import greed.client.RotationManager;

import net.minecraft.client.option.Perspective;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(name = "CameraFeature", description = "Фрилук, зум и плавное переключение камеры на F5.", category = ModuleCategory.RENDER)
public final class CameraFeatureModule extends Module {
   private static CameraFeatureModule cameraFeatureModule;
   public KeybindSetting overviewKey;
   public KeybindSetting zoomKey;
   public NumberSetting numberSetting;
   public BooleanSetting booleanSetting;
   private boolean flag;
   private boolean flag2;
   private float floatVal;
   private float floatVal2;
   private final AnimatedValue animation = new AnimatedValue(280L);
   private final AnimatedValue animation2 = new AnimatedValue(240L);
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && mc.options != null) {
         boolean local1061_2 = this.overviewKey.isBound() && this.overviewKey.getBinding().isPressed(mc.getWindow().getHandle());
         if (local1061_2 && !this.flag) {
            this.floatVal = mc.player.getYaw();
            this.floatVal2 = mc.player.getPitch();
         }

         this.flag = local1061_2;
         this.flag2 = this.zoomKey.isBound() && this.zoomKey.getBinding().isPressed(mc.getWindow().getHandle());
         this.animation.processData(this.flag ? 1.0 : 0.0);
         this.animation2.processData(this.booleanSetting.isChecked() && mc.options.getPerspective() == Perspective.THIRD_PERSON_BACK ? 1.0 : 0.0);
      }
   };

   public void processData(double local1046_1, double local1046_3) {
      this.floatVal += (float)local1046_1 * 0.15F;
      this.floatVal2 = Math.max(-90.0F, Math.min(90.0F, this.floatVal2 + (float)local1046_3 * 0.15F));
   }

   @Override
   protected void onEnable() {
      this.flag = false;
      this.flag2 = false;
      this.animation.getAnimatedValue(0.0);
      this.animation2.getAnimatedValue(mc.options != null && mc.options.getPerspective() == Perspective.THIRD_PERSON_BACK ? 1.0 : 0.0);
   }

   public float computeFloat() {
      return this.isEnabled() ? (float)this.animation.computeDouble() : 0.0F;
   }

   public boolean checkState() {
      return this.isEnabled()
         && mc.player != null
         && (
            this.checkState2()
               || this.booleanSetting.isChecked()
                  && mc.options != null
                  && mc.options.getPerspective() == Perspective.THIRD_PERSON_BACK
                  && this.animation2.computeDouble() > 0.001
                  && this.animation2.computeDouble() < 0.999
         );
   }

   public static CameraFeatureModule getInstance() {
      if (cameraFeatureModule == null) {
         cameraFeatureModule = new CameraFeatureModule();
      }

      return cameraFeatureModule;
   }

   public float computeFloat2() {
      return this.checkState2()
         ? this.floatVal2
            + ((RotationManager.getRotationManager().isRotating() ? RotationManager.getRotationManager().getTargetPitch() : mc.player.getPitch()) - this.floatVal2)
               * (1.0F - (float)this.animation.computeDouble())
         : (RotationManager.getRotationManager().isRotating() ? RotationManager.getRotationManager().getTargetPitch() : mc.player.getPitch());
   }

   private float computeFloat3() {
      return this.checkState2() ? (float)this.animation.computeDouble() : (float)this.animation2.computeDouble();
   }

   public float computeFloat4() {
      return this.isEnabled() && this.flag2 ? 1.0F / this.numberSetting.floatValue() : -1.0F;
   }

   private double computeDouble(Vec3d vec3d, Vec3d vec3d2, double local1054_3) {
      if (mc.world == null) {
         return local1054_3;
      }

      double local1054_5 = local1054_3;

      for (int index = 0; index < 8; index++) {
         Vec3d vec3d3 = vec3d.add(
            (index & 1) == 0 ? -0.1 : 0.1, (index >> 1 & 1) == 0 ? -0.1 : 0.1, (index >> 2 & 1) == 0 ? -0.1 : 0.1
         );
         HitResult hitResult = mc.world
            .raycast(
               new RaycastContext(vec3d3, vec3d3.add(vec3d2.multiply(local1054_3)), ShapeType.VISUAL, FluidHandling.NONE, mc.player)
            );
         if (hitResult.getType() != Type.MISS) {
            local1054_5 = Math.min(local1054_5, hitResult.getPos().distanceTo(vec3d));
         }
      }

      return local1054_5;
   }

   private CameraFeatureModule() {
      this.overviewKey = this.addSetting(new KeybindSetting("Обзор", "Свободный обзор не позволяющий сменить траекторию вашего игрока."));
      this.zoomKey = this.addSetting(new KeybindSetting("Зум", "Зум который был вырезан в 1.16."));
      this.numberSetting = this.addSetting(
         new NumberSetting("Сила зума", "Во сколько раз приближать.").withRange(2.0F, 10.0F).withStep(0.5F).setValue(4.0F)
      );
      this.booleanSetting = this.addSetting(new BooleanSetting("Плавная смена вида", "Анимация переключения камеры по F5.", true));
   }

   @Override
   protected void onDisable() {
      this.flag = false;
      this.flag2 = false;
   }

   public float computeFloat5() {
      return this.checkState2()
         ? MathHelper.lerpAngleDegrees(
            1.0F - (float)this.animation.computeDouble(),
            this.floatVal,
            RotationManager.getRotationManager().isRotating() ? RotationManager.getRotationManager().getTargetYaw() : mc.player.getYaw()
         )
         : (RotationManager.getRotationManager().isRotating() ? RotationManager.getRotationManager().getTargetYaw() : mc.player.getYaw());
   }

   private boolean checkState2() {
      return this.flag || this.animation.computeDouble() > 0.001;
   }

   public boolean matchesCriteria(double local1059_1, double local1059_3) {
      if (this.isEnabled()
         && mc.player != null
         && this.overviewKey.isBound()
         && this.overviewKey.getBinding().isPressed(mc.getWindow().getHandle())) {
         if (!this.flag) {
            this.floatVal = mc.player.getYaw();
            this.floatVal2 = mc.player.getPitch();
            this.flag = true;
            this.animation.processData(1.0);
         }

         this.processData(local1059_1, local1059_3);
         return true;
      } else {
         return false;
      }
   }

   public Vec3d getVec3d(float local1060_1) {
      Vec3d vec3d = Vec3d.fromPolar(this.computeFloat2(), this.computeFloat5()).multiply(-1.0);
      return mc.player
         .getCameraPosVec(local1060_1)
         .add(
            vec3d.multiply(
               this.computeDouble(
                  mc.player.getCameraPosVec(local1060_1),
                  vec3d,
                  4.0
                     * (
                        this.checkState2()
                           ? Math.max(
                              this.animation.computeDouble(),
                              mc.options != null && mc.options.getPerspective() != Perspective.FIRST_PERSON ? 1.0 : 0.0
                           )
                           : this.animation2.computeDouble()
                     )
               )
            )
         );
   }

   public boolean checkState3() {
      return this.checkState() && this.computeFloat3() > 0.15F;
   }

   public boolean checkState4() {
      return this.isEnabled() && this.flag;
   }
}
