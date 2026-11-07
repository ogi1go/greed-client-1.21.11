package greed.mixin;

import greed.client.module.combat.AimAssistModule;
import greed.client.module.combat.AttackAuraModule;
import greed.client.module.render.CameraFeatureModule;
import greed.client.event.EntityMovementEvent;
import greed.client.event.EntityYawEvent;
import greed.client.event.EventBus;
import greed.client.module.render.FreeCameraModule;
import greed.client.MinecraftAccess;
import greed.client.event.MouseLookEvent;
import greed.client.module.player.NoPushModule;
import greed.client.RotationManager;
import greed.client.module.render.SeeInvisibleModule;
import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements MinecraftAccess {
   @Unique
   private static EntityYawEvent sharedHitBoxEvent;
   @Unique
   private static boolean sharedHitBoxEventBusy;

   @Inject(method = "changeLookDirection(DD)V", at = @At("HEAD"), cancellable = true)
   private void steerFreeCamera(double local8229_1, double local8229_3, CallbackInfo callbackInfo) {
      if ((Object)this == mc.player && FreeCameraModule.getInstance().isEnabled()) {
         FreeCameraModule.getInstance().processData(local8229_1, local8229_3);
         callbackInfo.cancel();
      } else if ((Object)this == mc.player && CameraFeatureModule.getInstance().matchesCriteria(local8229_1, local8229_3)) {
         callbackInfo.cancel();
      } else {
         if ((Object)this == mc.player && RotationManager.getRotationManager().handleMouseInput(local8229_1, local8229_3)) {
            callbackInfo.cancel();
         }
      }
   }

   @Inject(method = "changeLookDirection(DD)V", at = @At("RETURN"))
   private void steerAimAssist(double local8230_1, double local8230_3, CallbackInfo callbackInfo) {
      if ((Object)this == mc.player) {
         AimAssistModule.getInstance().applyMouseAimAssist(local8230_1, local8230_3);
      }

      if ((Object)this == mc.player) {
         AttackAuraModule.getInstance().updateLegitAim();
      }
   }

   @ModifyVariable(method = "move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V", at = @At("HEAD"), argsOnly = true)
   private Vec3d applyMoveEvent(Vec3d vec3d) {
      if ((Object)this != mc.player) {
         return vec3d;
      }

      if (EventBus.getInstance().hasListeners(EntityMovementEvent.class)) {
         EntityMovementEvent entityMovementEvent = new EntityMovementEvent(vec3d);
         EventBus.getInstance().post(entityMovementEvent);
         vec3d = entityMovementEvent.getVec3d();
      }

      if (!EventBus.getInstance().hasListeners(MouseLookEvent.class)) {
         return vec3d;
      }

      MouseLookEvent mouseLookEvent = new MouseLookEvent(vec3d.x, vec3d.y, vec3d.z, mc.player.isOnGround());
      EventBus.getInstance().post(mouseLookEvent);
      mc.player.setOnGround(mouseLookEvent.checkState());
      if (mouseLookEvent.checkState()) {
         mc.player.setVelocity(mouseLookEvent.computeDouble2(), mouseLookEvent.computeDouble3(), mouseLookEvent.computeDouble());
      }

      return new Vec3d(mouseLookEvent.computeDouble2(), mouseLookEvent.computeDouble3(), mouseLookEvent.computeDouble());
   }

   @Inject(method = "pushAwayFrom(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
   private void removeEntityPush(Entity entity, CallbackInfo callbackInfo) {
      if (NoPushModule.getInstance().matchesCriteria("Сущности")) {
         if ((Object)this == mc.player || entity == mc.player) {
            callbackInfo.cancel();
         }
      }
   }

   @Inject(method = "adjustMovementForPiston(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;", at = @At("HEAD"), cancellable = true)
   private void removePistonPush(Vec3d vec3d, CallbackInfoReturnable<Vec3d> local8233_2) {
      if ((Object)this == mc.player && NoPushModule.getInstance().matchesCriteria("Блоки")) {
         local8233_2.setReturnValue(Vec3d.ZERO);
      }
   }

   @Redirect(method = "updateMovementInFluid(Lnet/minecraft/registry/tag/TagKey;D)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isPushedByFluids()Z"))
   private boolean removeFluidPush(Entity entity, TagKey<Fluid> local8234_2, double local8234_3) {
      return entity != mc.player
            || (local8234_2 == FluidTags.WATER ? !NoPushModule.getInstance().matchesCriteria("Вода") : !NoPushModule.getInstance().matchesCriteria("Лава"))
         ? entity.isPushedByFluids()
         : false;
   }

   @Inject(method = "isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z", at = @At("HEAD"), cancellable = true)
   private void showInvisibleEntities(PlayerEntity playerEntity, CallbackInfoReturnable<Boolean> local8235_2) {
      if (playerEntity == mc.player && (Object)this instanceof AbstractClientPlayerEntity && SeeInvisibleModule.getInstance().isEnabled()) {
         local8235_2.setReturnValue(false);
      }
   }

   @Inject(method = "getBoundingBox()Lnet/minecraft/util/math/Box;", at = @At("RETURN"), cancellable = true)
   private void applyEntityHitBox(CallbackInfoReturnable<Box> local8236_1) {
      if ((Object)this instanceof LivingEntity && mc.player != null && (Object)this != mc.player && EventBus.getInstance().hasListeners(EntityYawEvent.class)) {
         boolean local8236_2 = !sharedHitBoxEventBusy && mc.isOnThread();
         if (local8236_2 && sharedHitBoxEvent == null) {
            sharedHitBoxEvent = new EntityYawEvent(null, 0.0F);
         }

         EntityYawEvent entityYawEvent = local8236_2 ? sharedHitBoxEvent : new EntityYawEvent((Entity)(Object)this, 0.0F);
         float local8236_4;
         if (local8236_2) {
            sharedHitBoxEventBusy = true;

            try {
               entityYawEvent.setEntity((Entity)(Object)this);
               entityYawEvent.setMargin(0.0F);
               EventBus.getInstance().post(entityYawEvent);
               local8236_4 = entityYawEvent.getMargin();
            } finally {
               entityYawEvent.setEntity(null);
               sharedHitBoxEventBusy = false;
            }
         } else {
            EventBus.getInstance().post(entityYawEvent);
            local8236_4 = entityYawEvent.getMargin();
         }

         if (local8236_4 != 0.0F) {
            Box box = (Box)local8236_1.getReturnValue();
            local8236_1.setReturnValue(
               new Box(
                  box.minX - local8236_4,
                  box.minY,
                  box.minZ - local8236_4,
                  box.maxX + local8236_4,
                  box.maxY,
                  box.maxZ + local8236_4
               )
            );
         }
      }
   }

   static {
      VMBridge.identifyClass(EntityMixin.class, "qY92U9te");
   }
}
