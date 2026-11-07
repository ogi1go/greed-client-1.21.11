package greed.client.scripting;

import greed.client.MinecraftAccess;
import greed.client.util.Stopwatch;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;

public final class LuaClientApi {
   private LuaClientApi() {
   }

   public static final class SubContext {
      public static final LuaClientApi.SubContext nested0436 = new LuaClientApi.SubContext(-1);
      public static final LuaClientApi.SubContext nested04362 = new LuaClientApi.SubContext(-4144960);
      public static final LuaClientApi.SubContext nested04363 = new LuaClientApi.SubContext(-8355712);
      public static final LuaClientApi.SubContext nested04364 = new LuaClientApi.SubContext(-12566464);
      public static final LuaClientApi.SubContext nested04365 = new LuaClientApi.SubContext(-16777216);
      public static final LuaClientApi.SubContext nested04366 = new LuaClientApi.SubContext(-65536);
      public static final LuaClientApi.SubContext nested04367 = new LuaClientApi.SubContext(-20561);
      public static final LuaClientApi.SubContext nested04368 = new LuaClientApi.SubContext(-14336);
      public static final LuaClientApi.SubContext nested04369 = new LuaClientApi.SubContext(-256);
      public static final LuaClientApi.SubContext nested043610 = new LuaClientApi.SubContext(-16711936);
      public static final LuaClientApi.SubContext nested043611 = new LuaClientApi.SubContext(-65281);
      public static final LuaClientApi.SubContext nested043612 = new LuaClientApi.SubContext(-16711681);
      public static final LuaClientApi.SubContext nested043613 = new LuaClientApi.SubContext(-16776961);
      private int intVal;
      private int intVal2;
      private int intVal3;
      private int intVal4;

      public SubContext(double local3951_1, double local3951_3, double local3951_5, double local3951_7) {
         this.intVal = (int)(local3951_1 * 255.0);
         this.intVal2 = (int)(local3951_3 * 255.0);
         this.intVal3 = (int)(local3951_5 * 255.0);
         this.intVal4 = (int)(local3951_7 * 255.0);
      }

      public float computeFloat() {
         return this.intVal3;
      }

      public float computeFloat2() {
         return this.intVal2;
      }

      public int computeInt() {
         return (this.intVal4 & 0xFF) << 24 | (this.intVal & 0xFF) << 16 | (this.intVal2 & 0xFF) << 8 | this.intVal3 & 0xFF;
      }

      public SubContext(double local3956_1, double local3956_3, double local3956_5) {
         this(local3956_1, local3956_3, local3956_5, 1.0);
      }

      public float computeFloat3() {
         return this.intVal;
      }

      public SubContext(int local3958_1) {
         this.intVal4 = local3958_1 >> 24 & 0xFF;
         this.intVal = local3958_1 >> 16 & 0xFF;
         this.intVal2 = local3958_1 >> 8 & 0xFF;
         this.intVal3 = local3958_1 & 0xFF;
      }

      public float computeFloat4() {
         return this.intVal4;
      }
   }

   public static final class SubContext2 {
      private final Stopwatch stopwatch = new Stopwatch();

      public boolean matchesCriteria(long local3960_1) {
         return this.stopwatch.hasExceededMillis(local3960_1);
      }

      public void updateState() {
         this.stopwatch.reset();
      }

      public long computeLong() {
         return this.stopwatch.elapsedMillis();
      }
   }

   public static final class SubContext3 {
      private final MatrixStack matrixStack2;

      public void processData(double local3964_1, double local3964_3, double local3964_5) {
         this.matrixStack2.translate(local3964_1, local3964_3, local3964_5);
      }

      public void processData2(float local3965_1, float local3965_2, float local3965_3) {
         this.processData6(local3965_1);
         this.processData3(local3965_2);
         this.processData5(local3965_3);
      }

      public MatrixStack getMatrixStack() {
         return this.matrixStack2;
      }

      public SubContext3(MatrixStack matrixStack) {
         this.matrixStack2 = matrixStack;
      }

      public void processData3(float local3968_1) {
         this.matrixStack2.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local3968_1));
      }

      public void processData4(float local3969_1, float local3969_2, float local3969_3) {
         this.matrixStack2.scale(local3969_1, local3969_2, local3969_3);
      }

      public void processData5(float local3970_1) {
         this.matrixStack2.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local3970_1));
      }

      public void processData6(float local3971_1) {
         this.matrixStack2.multiply(RotationAxis.POSITIVE_X.rotationDegrees(local3971_1));
      }
   }

   public static final class SubContext4 {
      private final StatusEffectInstance statusEffectInstance2;
      private final String strVal;
      private final String strVal2;

      public SubContext4(StatusEffectInstance statusEffectInstance, String local3972_2, String local3972_3) {
         this.statusEffectInstance2 = statusEffectInstance;
         this.strVal2 = local3972_3;
         this.strVal = local3972_2;
      }

      public StatusEffectInstance getStatusEffectInstance() {
         return this.statusEffectInstance2;
      }

      public String formatText() {
         return this.strVal2;
      }

      public String formatText2() {
         return this.strVal;
      }
   }

   public static final class SubContext5 {
      public double doubleVal;
      public double doubleVal2;
      public double doubleVal3;

      public void processData7(double local3976_1, double local3976_3, double local3976_5) {
         this.doubleVal = local3976_1;
         this.doubleVal2 = local3976_3;
         this.doubleVal3 = local3976_5;
      }

      public SubContext5(double local3977_1, double local3977_3, double local3977_5) {
         this.doubleVal = local3977_1;
         this.doubleVal2 = local3977_3;
         this.doubleVal3 = local3977_5;
      }
   }

   public static final class LuaTargetEntity extends LuaClientApi.SubContext6 {
      private final LivingEntity livingEntity;

      public LivingEntity getEntity() {
         return this.livingEntity;
      }

      public boolean checkState() {
         return this.livingEntity.isBlocking();
      }

      public int computeInt2() {
         return this.livingEntity.hurtTime;
      }

      public float computeFloat5() {
         return this.livingEntity.getHealth();
      }

      public float computeFloat6() {
         return this.livingEntity.getMaxHealth();
      }

      public float computeFloat7() {
         return this.livingEntity.getAbsorptionAmount();
      }

      public float computeFloat8() {
         return this.livingEntity.bodyYaw;
      }

      public float computeFloat9() {
         return this.livingEntity.getArmor();
      }

      public String formatText3() {
         return this.livingEntity.getOffHandStack().getItem().getTranslationKey();
      }

      public LuaTargetEntity(LivingEntity entity) {
         super(entity);
         this.livingEntity = entity;
      }

      public String formatText4() {
         return this.livingEntity.getMainHandStack().getItem().getTranslationKey();
      }

      public int computeInt3() {
         return this.livingEntity.hurtTime;
      }

      public boolean checkState2() {
         return this.livingEntity.isUsingItem();
      }
   }

   public static class SubContext6 implements MinecraftAccess {
      private final Entity targetEntity;

      public boolean checkState3() {
         return this.targetEntity.isTouchingWater();
      }

      public boolean checkState4() {
         return this.targetEntity.isGlowing();
      }

      public LuaClientApi.SubContext5 getNested0440() {
         return new LuaClientApi.SubContext5(this.targetEntity.getX(), this.targetEntity.getY(), this.targetEntity.getZ());
      }

      public float computeFloat10() {
         return (float)this.targetEntity.fallDistance;
      }

      public SubContext6(Entity entity) {
         this.targetEntity = entity;
      }

      public double computeDouble(LuaClientApi.SubContext6 local3997_1) {
         return local3997_1 == null ? -1.0 : this.targetEntity.distanceTo(local3997_1.getEntity());
      }

      public boolean checkState5() {
         return this.targetEntity.isSneaking();
      }

      public LuaClientApi.SubContext5 getNested04402() {
         return new LuaClientApi.SubContext5(this.targetEntity.getVelocity().x, this.targetEntity.getVelocity().y, this.targetEntity.getVelocity().z);
      }

      public boolean checkState6() {
         return this.targetEntity instanceof PlayerEntity;
      }

      public boolean checkState7() {
         return this.targetEntity.isSprinting();
      }

      public Entity getEntity() {
         return this.targetEntity;
      }

      public boolean checkState8() {
         return this.targetEntity.isAlive();
      }

      public int computeInt4() {
         return this.targetEntity.getId();
      }

      public boolean checkState9() {
         return this.targetEntity.isInLava();
      }

      public String formatText5() {
         return this.targetEntity.getName().getString();
      }

      public float computeFloat11() {
         return this.targetEntity.getPitch();
      }

      public float computeFloat12() {
         return this.targetEntity.getYaw();
      }

      public boolean checkState10() {
         return this.targetEntity.isOnGround();
      }

      public boolean checkState11() {
         return this.targetEntity.isInvisible();
      }

      public String formatText6() {
         return this.targetEntity.getType().getTranslationKey();
      }
   }

   public static final class SubContext7 {
      private float floatVal;
      private float floatVal2;
      private long timeMillis;
      private long timeMillis2 = 1L;

      public void processData8(float local4012_1, int local4012_2) {
         if (this.floatVal2 != local4012_1) {
            this.floatVal = this.computeFloat13();
            this.floatVal2 = local4012_1;
            this.timeMillis = System.currentTimeMillis();
            this.timeMillis2 = Math.max(1, local4012_2);
         }
      }

      public boolean checkState12() {
         return System.currentTimeMillis() - this.timeMillis >= this.timeMillis2;
      }

      public boolean checkState13() {
         return this.checkState12();
      }

      public float computeFloat13() {
         float local4016_1 = Math.min(1.0F, (float)(System.currentTimeMillis() - this.timeMillis) / (float)this.timeMillis2);
         return this.floatVal + (this.floatVal2 - this.floatVal) * (1.0F - (1.0F - local4016_1) * (1.0F - local4016_1));
      }

      public float computeFloat14() {
         return this.computeFloat13();
      }
   }
}
