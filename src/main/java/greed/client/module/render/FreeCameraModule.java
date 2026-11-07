package greed.client.module.render;

import greed.client.event.EventListener;
import greed.client.event.EventPriority;
import greed.client.render.FontFace;
import greed.client.event.HudRenderEvent;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.NumberSetting;
import greed.client.event.PlayerTickEvent;
import greed.client.util.GuiScale;
import greed.client.render.FontText;

import java.util.Locale;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "FreeCamera", description = "Отвязанная камера для полёта и обзора сквозь блоки.", category = ModuleCategory.RENDER)
public final class FreeCameraModule extends Module {
   private static FreeCameraModule freeCameraModule;
   public NumberSetting numberSetting;
   private Vec3d vec3d2;
   private Vec3d vec3d3;
   private float floatVal;
   private float floatVal2;
   private long timeMillis;
   private Vec3d vec3d4;
   private float floatVal3;
   private float floatVal4;
   private ClientPlayerEntity clientPlayerEntity;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null) {
         if (this.vec3d2 == null || this.clientPlayerEntity != mc.player) {
            this.clientPlayerEntity = mc.player;
            this.vec3d2 = mc.player.getEyePos();
            this.vec3d3 = this.vec3d2;
            this.floatVal = mc.player.getYaw();
            this.floatVal2 = mc.player.getPitch();
         }

         this.vec3d3 = this.vec3d2;
         Vec3d vec3d = Vec3d.ZERO;
         if (mc.options.forwardKey.isPressed()) {
            vec3d = vec3d.add(Vec3d.fromPolar(0.0F, this.floatVal));
         }

         if (mc.options.backKey.isPressed()) {
            vec3d = vec3d.subtract(Vec3d.fromPolar(0.0F, this.floatVal));
         }

         if (mc.options.rightKey.isPressed()) {
            vec3d = vec3d.add(Vec3d.fromPolar(0.0F, this.floatVal + 90.0F));
         }

         if (mc.options.leftKey.isPressed()) {
            vec3d = vec3d.subtract(Vec3d.fromPolar(0.0F, this.floatVal + 90.0F));
         }

         if (mc.options.jumpKey.isPressed()) {
            vec3d = vec3d.add(0.0, 1.0, 0.0);
         }

         if (mc.options.sneakKey.isPressed()) {
            vec3d = vec3d.subtract(0.0, 1.0, 0.0);
         }

         if (vec3d.lengthSquared() > 0.0) {
            this.vec3d2 = this.vec3d2
               .add(vec3d.normalize().multiply(this.numberSetting.floatValue() * (mc.options.sprintKey.isPressed() ? 2.0F : 1.0F)));
         }
      }
   };
   @EventPriority(100)
   private final EventListener<HudRenderEvent> hudRenderListener = hudRenderEvent -> {
      if (mc.player != null && this.vec3d2 != null) {
         FontText.processData3(
            FontFace.SEMI,
            String.format(Locale.US, "%.1f, %.1f, %.1f", this.vec3d2.x, this.vec3d2.y, this.vec3d2.z),
            hudRenderEvent.computeInt2() * GuiScale.computeInt() / GuiScale.computeFloat() / 2.0F
               - FontText.computeFloat(
                     FontFace.SEMI, String.format(Locale.US, "%.1f, %.1f, %.1f", this.vec3d2.x, this.vec3d2.y, this.vec3d2.z), 5.5F
                  )
                  / 2.0F,
            hudRenderEvent.computeInt() * GuiScale.computeInt() / GuiScale.computeFloat() / 2.0F + 8.0F,
            5.5F,
            -419430401
         );
      }
   };

   @Override
   protected void onDisable() {
      if (this.vec3d2 != null) {
         this.timeMillis = System.currentTimeMillis();
         this.vec3d4 = this.vec3d2;
         this.floatVal3 = this.floatVal;
         this.floatVal4 = this.floatVal2;
      }
   }

   public Vec3d getVec3d(float local1210_1) {
      return this.isEnabled()
         ? this.vec3d3.lerp(this.vec3d2, local1210_1)
         : this.vec3d4.lerp(mc.player.getCameraPosVec(local1210_1), this.computeFloat2());
   }

   public void processData(double local1211_1, double local1211_3) {
      this.floatVal += (float)local1211_1 * 0.15F;
      this.floatVal2 = Math.max(-90.0F, Math.min(90.0F, this.floatVal2 + (float)local1211_3 * 0.15F));
   }

   @Override
   protected void onEnable() {
      this.timeMillis = 0L;
      this.clientPlayerEntity = mc.player;
      this.vec3d2 = mc.player == null ? null : mc.gameRenderer.getCamera().getCameraPos();
      this.vec3d3 = this.vec3d2;
      if (mc.player != null) {
         this.floatVal = mc.player.getYaw();
         this.floatVal2 = mc.player.getPitch();
      }
   }

   public boolean checkState() {
      return this.checkState2() && this.getVec3d(1.0F).squaredDistanceTo(mc.player.getEyePos()) > 0.5625;
   }

   public static FreeCameraModule getInstance() {
      if (freeCameraModule == null) {
         freeCameraModule = new FreeCameraModule();
      }

      return freeCameraModule;
   }

   private FreeCameraModule() {
      this.numberSetting = this.addSetting(
         new NumberSetting("Скорость", "Блоков за тик полёта камеры.").withRange(0.1F, 2.0F).withStep(0.1F).setValue(0.5F)
      );
   }

   public float computeFloat() {
      return this.isEnabled() ? this.floatVal2 : this.floatVal4 + (mc.player.getPitch() - this.floatVal4) * this.computeFloat2();
   }

   public boolean checkState2() {
      return mc.player != null && this.vec3d2 != null && (this.isEnabled() || System.currentTimeMillis() - this.timeMillis < 350L);
   }

   private float computeFloat2() {
      float local1219_1 = Math.min(1.0F, (float)(System.currentTimeMillis() - this.timeMillis) / 350.0F);
      return local1219_1 * local1219_1 * (3.0F - 2.0F * local1219_1);
   }

   public float computeFloat3() {
      return this.isEnabled() ? this.floatVal : this.floatVal3 + MathHelper.wrapDegrees(mc.player.getYaw() - this.floatVal3) * this.computeFloat2();
   }
}
