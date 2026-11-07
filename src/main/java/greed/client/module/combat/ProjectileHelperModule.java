package greed.client.module.combat;

import greed.client.event.BlockBreakEvent;
import greed.client.setting.BooleanSetting;
import greed.client.event.EventListener;
import greed.client.storage.FriendStore;
import greed.client.event.HudRenderEvent;
import greed.mixin.MinecraftClientMixin6;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.MultiSelectSetting;
import greed.client.event.PlayerTickEvent;
import greed.client.module.render.ProjectilePredictionModule;
import greed.client.util.AimPointUtils;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;
import greed.client.module.player.ServerHelperModule;

import java.util.HashMap;
import java.util.LinkedList;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(name = "ProjectileHelper", description = "Доводит прицел по цели при стрельбе из лука и трезубца.", category = ModuleCategory.COMBAT)
public final class ProjectileHelperModule extends Module {
   private static ProjectileHelperModule projectileHelperModule;
   public BooleanSetting aimAssist;
   public MultiSelectSetting multiSelectSetting;
   public ModeSetting modeSetting;
   public BooleanSetting clientRotation;
   public BooleanSetting displayTooltip;
   private final HashMap<LivingEntity, LinkedList<Vec3d>> map = new HashMap<>();
   private LivingEntity livingEntity4;
   private int intVal;
   private boolean flag = true;
   private boolean flag2;
   private float floatVal;
   private float floatVal2;
   private boolean flag3;
   private int intVal2;
   private float floatVal3;
   private float floatVal4;
   private RotationAngles rotationAngles;
   private boolean flag4;
   private boolean flag5;
   private int intVal3;
   private boolean flag6;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && mc.world != null) {
         boolean isPressed = mc.options.attackKey.isPressed();
         if (this.checkState2() && isPressed && !this.flag2) {
            this.flag = !this.flag;
         }

         this.flag2 = isPressed;
         if (!this.checkState2()) {
            this.flag = true;
            this.livingEntity4 = null;
            this.intVal = 0;
            this.intVal3 = 0;
            if (this.flag3) {
               if (!this.flag4) {
                  mc.player.setYaw(mc.player.headYaw = this.floatVal3);
                  mc.player.setPitch(this.floatVal4);
               }

               this.flag3 = false;
            }
         } else if (!this.flag) {
            this.livingEntity4 = null;
            this.intVal = 0;
            this.intVal3 = 0;
         } else if (mc.player.getMainHandStack().getItem() instanceof CrossbowItem) {
            this.updateState();
            if (this.livingEntity4 == null) {
               this.intVal3 = 0;
            } else {
               this.getVec3d3(this.livingEntity4);
               if (this.intVal3 != 0) {
                  this.intVal3++;
                  RotationManager.getRotationManager()
                     .setRotationWithTimeout(AimPointUtils.getRotationAngles(this.getVec3d()), new RotationSpeed(360.0F, 360.0F, 30.0F, 30.0F), 2L);
                  mc.player.setYaw(mc.player.headYaw = AimPointUtils.getRotationAngles(this.getVec3d()).getYaw());
                  mc.player.setPitch(MathHelper.clamp(AimPointUtils.getRotationAngles(this.getVec3d()).getPitch(), -90.0F, 90.0F));
                  if (mc.interactionManager != null) {
                     this.flag6 = true;
                     ((MinecraftClientMixin6)mc).invokeStartUseItem();
                     this.flag6 = false;
                     if (this.checkState()) {
                        RotationManager.getRotationManager().syncCameraAngles();
                     }

                     this.intVal3 = 0;
                  }
               }
            }
         } else if (!this.modeSetting.isSelected("Снап")) {
            this.updateState();
            if (this.livingEntity4 != null) {
               this.updateState2();
            }
         } else if (!this.flag3) {
            this.updateState();
            if (this.livingEntity4 != null) {
               this.getVec3d3(this.livingEntity4);
            }
         } else if (!this.flag4) {
            if (++this.intVal2 >= 2 && mc.interactionManager != null) {
               this.flag5 = true;
               mc.interactionManager.stopUsingItem(mc.player);
               this.flag5 = false;
               mc.player.setYaw(mc.player.headYaw = this.floatVal3);
               mc.player.setPitch(this.floatVal4);
               this.flag3 = false;
               this.livingEntity4 = null;
            }
         } else {
            RotationManager.getRotationManager().setRotationWithTimeout(this.rotationAngles, new RotationSpeed(360.0F, 360.0F, 30.0F, 30.0F), 2L);
            this.intVal2++;
            boolean local966_3 = Math.abs(MathHelper.wrapDegrees(this.rotationAngles.getYaw() - mc.player.getYaw())) <= 1.0F
               && Math.abs(this.rotationAngles.getPitch() - mc.player.getPitch()) <= 1.0F;
            if ((local966_3 && this.intVal2 >= 2 || this.intVal2 >= 10) && mc.interactionManager != null) {
               this.flag5 = true;
               mc.interactionManager.stopUsingItem(mc.player);
               this.flag5 = false;
               if (this.checkState()) {
                  RotationManager.getRotationManager().syncCameraAngles();
               }

               this.flag3 = false;
               this.livingEntity4 = null;
            }
         }
      }
   };
   private final EventListener<BlockBreakEvent> blockBreakListener = blockBreakEvent -> {
      if (this.isEnabled()
         && !this.flag5
         && mc.player != null
         && mc.world != null
         && this.aimAssist.isChecked()
         && this.checkState()
         && this.checkState2()
         && this.flag) {
         RotationManager.getRotationManager().syncCameraAngles();
      }

      if (this.isEnabled() && !this.flag5 && this.modeSetting.isSelected("Снап") && mc.player != null && mc.world != null) {
         if (!(mc.player.getMainHandStack().getItem() instanceof CrossbowItem)) {
            if (this.checkState2() && this.flag) {
               if (!this.flag3) {
                  this.updateState();
                  if (this.livingEntity4 == null) {
                     return;
                  }

                  this.rotationAngles = AimPointUtils.getRotationAngles(this.getVec3d());
                  this.flag4 = this.clientRotation.isChecked();
                  if (this.flag4) {
                     RotationManager.getRotationManager().setRotationWithTimeout(this.rotationAngles, new RotationSpeed(360.0F, 360.0F, 30.0F, 30.0F), 2L);
                  } else {
                     this.floatVal3 = mc.player.getYaw();
                     this.floatVal4 = mc.player.getPitch();
                     mc.player.setYaw(mc.player.headYaw = this.rotationAngles.getYaw());
                     mc.player.setPitch(this.rotationAngles.getPitch());
                  }

                  this.flag3 = true;
                  this.intVal2 = 0;
               }

               blockBreakEvent.processData(true);
            }
         }
      }
   };
   private final EventListener<HudRenderEvent> hudRenderListener = hudRenderEvent -> {
      if (this.displayTooltip.isChecked()) {
         this.floatVal2 = this.floatVal2 + ((this.checkState2() && this.flag && this.livingEntity4 != null ? 1.0F : 0.0F) - this.floatVal2) * 0.2F;
         if (!(this.floatVal2 < 0.01F)) {
            ;
         }
      }
   };

   private Vec3d getVec3d() {
      ItemStack itemStack = mc.player.getMainHandStack();
      double local965_2 = this.computeDouble(itemStack);
      double local965_4 = this.computeDouble3(itemStack);
      if (!(local965_2 <= 0.0) && !(local965_4 <= 0.0)) {
         int local965_6 = mc.player.isUsingItem() ? 1 : 0;
         Vec3d velocity = mc.player.getVelocity();
         Vec3d vec3d = mc.player.getCameraPosVec(1.0F).add(velocity.multiply(local965_6));
         Vec3d vec3d2 = itemStack.getItem() instanceof CrossbowItem ? Vec3d.ZERO : this.getVec3d3(this.livingEntity4);
         Box box = this.livingEntity4.getBoundingBox().offset(vec3d2.multiply(local965_6));
         Vec3d vec3d3 = this.getVec3d2(vec3d, box, this.livingEntity4);
         ProjectileHelperModule.TrajectoryData local965_12 = null;

         for (int index = 0; index < 4; index++) {
            Vec3d vec3d4 = vec3d3.subtract(vec3d);
            if (local965_12 != null) {
               vec3d4 = vec3d4.subtract(velocity.multiply(local965_12.flightTicks()));
            }

            local965_12 = this.getNested0361(vec3d4, local965_2, local965_4);
            if (local965_12 == null) {
               break;
            }

            Vec3d vec3d5 = this.getVec3d2(vec3d, box.offset(vec3d2.multiply(local965_12.flightTicks())), this.livingEntity4);
            if (vec3d5.squaredDistanceTo(vec3d3) <= 1.0E-4) {
               vec3d3 = vec3d5;
               break;
            }

            vec3d3 = vec3d5;
         }

         if (local965_12 != null) {
            ProjectileHelperModule.TrajectoryData local965_13 = this.getNested0361(vec3d3.subtract(vec3d), local965_2, local965_4);
            if (local965_13 != null) {
               local965_12 = local965_13;
            }
         }

         if (local965_12 == null) {
            Vec3d vec3d6 = vec3d3.subtract(vec3d);
            return vec3d6.lengthSquared() > 1.0E-6 ? vec3d6.normalize() : new Vec3d(0.0, -1.0, 0.0);
         } else {
            return local965_12.launchDir();
         }
      } else {
         return this.livingEntity4.getBoundingBox().getCenter().subtract(mc.player.getCameraPosVec(1.0F)).normalize();
      }
   }

   private ProjectileHelperModule.TrajectoryData getNested0361(Vec3d vec3d, double local967_2, double local967_4) {
      double local967_6 = Math.hypot(vec3d.x, vec3d.z);
      double local967_8 = vec3d.y;
      double local967_10 = local967_2 * local967_2;
      double local967_12 = local967_10 * local967_10 - local967_4 * (local967_4 * local967_6 * local967_6 + 2.0 * local967_8 * local967_10);
      if (local967_12 < 0.0) {
         return null;
      }

      double local967_14 = Math.sqrt(local967_12);
      double local967_16 = (local967_10 - local967_14) / (local967_4 * local967_6);
      double local967_18 = (local967_10 + local967_14) / (local967_4 * local967_6);
      double local967_20 = !Double.isNaN(local967_16) && !Double.isInfinite(local967_16) ? local967_16 : local967_18;
      if (!Double.isNaN(local967_20) && !Double.isInfinite(local967_20)) {
         double local967_22 = Math.atan(local967_20);
         if (local967_22 < -Math.PI / 2 || local967_22 > Math.PI / 2) {
            local967_22 = Math.atan(local967_18);
         }

         double local967_24 = local967_6 / (local967_2 * Math.cos(local967_22));
         return !Double.isNaN(local967_24) && !Double.isInfinite(local967_24) && !(local967_24 <= 0.0)
            ? new ProjectileHelperModule.TrajectoryData(new Vec3d(vec3d.x, local967_6 * Math.tan(local967_22), vec3d.z).normalize(), local967_24)
            : null;
      } else {
         return null;
      }
   }

   private void updateState() {
      LivingEntity livingEntity = null;
      double local968_2 = Double.MAX_VALUE;

      for (AbstractClientPlayerEntity livingEntity2 : mc.world.getPlayers()) {
         if (livingEntity2 != mc.player && !FriendStore.isFriend(livingEntity2.getNameForScoreboard())) {
            double local968_6 = this.computeDouble2(livingEntity2) * mc.player.distanceTo(livingEntity2);
            if (local968_6 < local968_2) {
               local968_2 = local968_6;
               livingEntity = livingEntity2;
            }
         }
      }

      if (this.livingEntity4 == null) {
         this.livingEntity4 = livingEntity;
         this.intVal = 0;
      } else if (livingEntity != null && livingEntity != this.livingEntity4 && ++this.intVal >= 3) {
         this.livingEntity4 = livingEntity;
         this.intVal = 0;
      } else if (livingEntity == this.livingEntity4) {
         this.intVal = 0;
      }
   }

   private double computeDouble(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return 0.0;
      } else if (itemStack.getItem() == Items.BOW && mc.player.isUsingItem()) {
         return 3.0F * BowItem.getPullProgress(mc.player.getItemUseTime());
      } else if (itemStack.getItem() instanceof CrossbowItem) {
         return CrossbowItem.isCharged(itemStack) ? 3.15 : 0.0;
      } else {
         return itemStack.getItem() instanceof TridentItem && mc.player.isUsingItem() ? 2.5 : 0.0;
      }
   }

   private Vec3d getVec3d2(Vec3d vec3d, Box box, LivingEntity livingEntity) {
      Vec3d center = box.getCenter();
      Vec3d vec3d2 = livingEntity.getRotationVec(mc.getRenderTickCounter().getTickProgress(true));
      double local971_6 = Math.min(box.getLengthX(), box.getLengthZ()) * 0.5;
      Vec3d vec3d3 = new Vec3d(
         MathHelper.clamp(center.x + vec3d2.x * local971_6, box.minX, box.maxX),
         MathHelper.clamp(center.y + vec3d2.y * local971_6, box.minY, box.maxY),
         MathHelper.clamp(center.z + vec3d2.z * local971_6, box.minZ, box.maxZ)
      );
      if (!this.matchesCriteria(vec3d, vec3d3)) {
         return vec3d3;
      }

      if (!this.matchesCriteria(vec3d, center)) {
         return center;
      }

      double local971_9 = local971_6 * 0.6;
      Vec3d[] vec3dArray = new Vec3d[]{vec3d2.multiply(local971_9), vec3d2.multiply(local971_9 * 0.5), vec3d2.multiply(-local971_9 * 0.5)};

      for (Vec3d vec3d4 : vec3dArray) {
         Vec3d vec3d5 = center.add(vec3d4.x, vec3d4.y, vec3d4.z);
         if (box.contains(vec3d5) && !this.matchesCriteria(vec3d, vec3d5)) {
            return vec3d5;
         }
      }

      Vec3d[] vec3dArray2 = new Vec3d[]{
         new Vec3d(0.0, 0.25, 0.0),
         new Vec3d(0.0, -0.25, 0.0),
         new Vec3d(0.25, 0.0, 0.0),
         new Vec3d(-0.25, 0.0, 0.0),
         new Vec3d(0.0, 0.0, 0.25),
         new Vec3d(0.0, 0.0, -0.25),
         new Vec3d(0.18, 0.2, 0.18),
         new Vec3d(-0.18, 0.2, 0.18),
         new Vec3d(0.18, 0.2, -0.18),
         new Vec3d(-0.18, 0.2, -0.18),
         new Vec3d(0.18, -0.1, 0.18),
         new Vec3d(-0.18, -0.1, 0.18),
         new Vec3d(0.18, -0.1, -0.18),
         new Vec3d(-0.18, -0.1, -0.18)
      };

      for (Vec3d vec3d6 : vec3dArray2) {
         Vec3d vec3d7 = center.add(
            box.getLengthX() * vec3d6.x, box.getLengthY() * vec3d6.y, box.getLengthZ() * vec3d6.z
         );
         if (box.contains(vec3d7) && !this.matchesCriteria(vec3d, vec3d7)) {
            return vec3d7;
         }
      }

      return vec3d3;
   }

   private Vec3d getVec3d3(LivingEntity livingEntity) {
      this.map.keySet().removeIf(livingEntity2 -> livingEntity2.isRemoved() || livingEntity2 != livingEntity && livingEntity2.squaredDistanceTo(livingEntity) > 4096.0);
      LinkedList<Vec3d> local972_2 = this.map.computeIfAbsent(livingEntity, livingEntity3 -> new LinkedList<>());
      if (local972_2.size() >= 5) {
         local972_2.pollFirst();
      }

      local972_2.add(livingEntity.getEntityPos());
      if (local972_2.size() < 2) {
         return Vec3d.ZERO;
      }

      Vec3d vec3d = Vec3d.ZERO;

      for (int index = 1; index < local972_2.size(); index++) {
         Vec3d vec3d2 = local972_2.get(index).subtract(local972_2.get(index - 1));
         vec3d = vec3d.add(vec3d2.x, 0.0, vec3d2.z);
      }

      return vec3d.multiply(1.0 / (local972_2.size() - 1));
   }

   public boolean checkState() {
      return ServerHelperModule.getInstance().isEnabled() && ServerHelperModule.getInstance().modeSetting.isSelected("SpookyTime");
   }

   private ProjectileHelperModule() {
      this.aimAssist = this.addSetting(new BooleanSetting("Доводка прицела", "Выключи, чтобы модуль только рисовал предикт и не трогал прицел.", true));
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting(
            "Помогать с", "Из какого оружия доводить прицел.", new String[]{"Трезубцем", "Луком", "Арбалетом"}, "Трезубцем", "Луком", "Арбалетом"
         )
      );
      this.multiSelectSetting.withVisibility(() -> this.aimAssist.isChecked());
      this.modeSetting = this.addSetting(
         new ModeSetting("Стиль наводки", "Снап доворачивает мгновенно в точку, плавная ведёт с разбросом.", "Плавная", "Плавная", "Снап")
      );
      this.modeSetting.withVisibility(() -> this.aimAssist.isChecked());
      this.clientRotation = this.addSetting(
         new BooleanSetting("Клиентская ротация", "Снап ведёт камеру ротатором, выкл тихо разворачивает на один тик.", true)
            .withVisibility(() -> this.aimAssist.isChecked() && this.modeSetting.isSelected("Снап"))
      );
      this.displayTooltip = this.addSetting(new BooleanSetting("Отображение подсказки", "Пишет по центру, как отключить доводку.", false));
   }

   private boolean checkState2() {
      return false;
   }

   public boolean checkState3() {
      return false;
   }

   @Override
   protected void onEnable() {
      ProjectilePredictionModule.getInstance().setEnabledWithoutCallbacks(true);
   }

   private double computeDouble2(LivingEntity livingEntity) {
      Vec3d vec3d = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getStandingEyeHeight(), mc.player.getZ());
      Box boundingBox = livingEntity.getBoundingBox();
      Vec3d vec3d2 = new Vec3d(
         Math.max(boundingBox.minX, Math.min(vec3d.x, boundingBox.maxX)),
         Math.max(boundingBox.minY, Math.min(vec3d.y, boundingBox.maxY)),
         Math.max(boundingBox.minZ, Math.min(vec3d.z, boundingBox.maxZ))
      );
      Vec3d vec3d3 = new RotationAngles(mc.player.getYaw(), mc.player.getPitch()).getDirectionVector();
      return Math.toDegrees(Math.acos(vec3d3.dotProduct(vec3d2.subtract(vec3d).normalize())));
   }

   private void updateState2() {
      Vec3d vec3d = this.getVec3d();
      RotationAngles currentRotationAngles = AimPointUtils.getRotationAngles(vec3d);
      float local982_3 = currentRotationAngles.getYaw();
      float local982_4 = currentRotationAngles.getPitch();
      if (this.checkState()) {
         RotationManager.getRotationManager()
            .setRotationWithTimeout(
               new RotationAngles(local982_3, MathHelper.clamp(local982_4, -90.0F, 90.0F)), new RotationSpeed(160.0F, 160.0F, 30.0F, 30.0F), 2L
            );
      } else {
         float local982_5 = mc.player.age + mc.getRenderTickCounter().getTickProgress(true);
         float local982_6 = Math.abs(MathHelper.wrapDegrees(local982_3 - mc.player.getYaw()));
         float local982_7 = Math.abs(MathHelper.wrapDegrees(local982_4 - mc.player.getPitch()));
         float local982_8 = (float)Math.hypot(local982_6, local982_7);
         float local982_9 = MathHelper.clamp(local982_8 <= 8.0F ? 0.0F : (local982_8 - 8.0F) / 26.0F, 0.0F, 1.0F);
         this.floatVal = MathHelper.clamp(MathHelper.lerp(0.2F, this.floatVal, local982_9), 0.05F, 22.0F);
         float local982_10 = MathHelper.clamp(mc.player.getItemUseTimeLeft(), 1, 6);
         float local982_11 = (float)(
               Math.sin(local982_5 * 0.8F) * 15.0
                  + Math.sin(local982_5 * 0.04 + 17.2) * 1.5
                  + Math.sin(local982_5 * 0.11 + 5.8) * 3.0
                  + Math.sin(local982_5 * 0.07 + 12.3) * 1.0
            )
            / local982_10
            * this.floatVal;
         float local982_12 = (float)(Math.sin(local982_5 * 0.4) * 15.0 + Math.sin(local982_5 * 0.03 + 54.1) * 0.5)
            / MathHelper.clamp(mc.player.getItemUseTimeLeft(), 1, 10)
            * this.floatVal;
         RotationManager.getRotationManager()
            .setRotationWithTimeout(
               new RotationAngles(local982_3 + local982_11, MathHelper.clamp(local982_4 + local982_12, -90.0F, 90.0F)),
               new RotationSpeed(160.0F, 160.0F, 30.0F, 30.0F),
               2L
            );
      }
   }

   public LivingEntity getLivingEntity() {
      return this.livingEntity4;
   }

   private double computeDouble3(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return 0.0;
      } else if (itemStack.getItem() == Items.BOW) {
         return 0.05;
      } else if (itemStack.getItem() instanceof CrossbowItem) {
         return 0.05;
      } else {
         return itemStack.getItem() instanceof TridentItem ? 0.05 : 0.0;
      }
   }

   @Override
   protected void onDisable() {
      ProjectilePredictionModule.getInstance().setEnabledWithoutCallbacks(false);
      if (this.flag3 && !this.flag4 && mc.player != null) {
         mc.player.setYaw(mc.player.headYaw = this.floatVal3);
         mc.player.setPitch(this.floatVal4);
      }

      this.livingEntity4 = null;
      this.intVal = 0;
      this.flag = true;
      this.intVal3 = 0;
      this.flag3 = false;
      this.intVal2 = 0;
      this.flag5 = false;
      this.floatVal = 0.0F;
      this.floatVal2 = 0.0F;
      this.map.clear();
   }

   public static ProjectileHelperModule getInstance() {
      if (projectileHelperModule == null) {
         projectileHelperModule = new ProjectileHelperModule();
      }

      return projectileHelperModule;
   }

   private boolean matchesCriteria(Vec3d vec3d, Vec3d vec3d2) {
      return mc.world == null
         ? false
         : mc.world.raycast(new RaycastContext(vec3d, vec3d2, ShapeType.COLLIDER, FluidHandling.NONE, mc.player)).getType()
            == Type.BLOCK;
   }

   private record TrajectoryData(Vec3d launchDir, double flightTicks) {
   }
}
