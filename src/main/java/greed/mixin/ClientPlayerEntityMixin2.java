package greed.mixin;

import greed.client.module.combat.BlockSlotModule;
import greed.client.module.misc.ChestStealerModule;
import greed.client.event.EventBus;
import greed.client.module.combat.NoEntityTraceModule;
import greed.client.module.player.NoPushModule;
import greed.client.event.PlayerSlowdownEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.module.combat.AttackController;
import greed.client.util.GroundTicksAccess;
import greed.client.util.ServerRotation;
import greed.client.RotationManager;
import greed.client.SilentRotation;
import greed.client.event.ScreenCloseEvent;
import greed.client.module.movement.SpiderModule;
import greed.client.module.combat.crystalmodules.utils.explosive.ExplosiveCoordinator;
import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin2 implements GroundTicksAccess {
    @Unique
    private int ticksOnGround;

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void tick(CallbackInfo callbackInfo) {
        AttackController.incrementAttackTicks();
        if (EventBus.getInstance().hasListeners(PlayerTickEvent.class)) {
            EventBus.getInstance().post(new PlayerTickEvent());
        }
    }

    @Inject(method = "sendMovementPackets()V", at = @At("HEAD"))
    private void countGroundTicks(CallbackInfo callbackInfo) {
        if (((ClientPlayerEntity)(Object)this).isOnGround()) {
            this.ticksOnGround++;
        } else {
            this.ticksOnGround = 0;
        }
    }

    @Inject(method = "sendMovementPackets()V", at = @At("HEAD"))
    private void applySilentRotation(CallbackInfo callbackInfo) {
        SilentRotation.begin();
    }

    @Inject(method = "sendMovementPackets()V", at = @At("HEAD"))
    private void spiderPreMotion(CallbackInfo callbackInfo) {
        SpiderModule.getInstance().updateState3();
    }

    @Inject(method = "sendMovementPackets()V", at = @At("HEAD"))
    private void explosiveBeforeMovement(CallbackInfo callbackInfo) {
        ExplosiveCoordinator.beforeMovement();
    }

    @Inject(method = "sendMovementPackets()V", at = @At("RETURN"))
    private void explosiveAfterMovement(CallbackInfo callbackInfo) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        try {
            // Фиксируем ровно ту ротацию, которую только что получил сервер.
            if (MinecraftClient.getInstance().getCameraEntity() == player) {
                if (SilentRotation.isActive()) {
                    ServerRotation.record(SilentRotation.getYaw(), SilentRotation.getPitch());
                } else {
                    ServerRotation.record(player.getYaw(), player.getPitch());
                }
            }
        } finally {
            SilentRotation.end();
        }
        ExplosiveCoordinator.afterMovement();
    }

    @Override
    public int ticksOnGround() {
        return this.ticksOnGround;
    }

    @Inject(method = "closeHandledScreen()V", at = @At("HEAD"), cancellable = true)
    private void closeContainer(CallbackInfo callbackInfo) {
        if (MinecraftClient.getInstance().currentScreen == null) {
            callbackInfo.cancel();
        } else if (EventBus.getInstance().hasListeners(ScreenCloseEvent.class)) {
            ScreenCloseEvent screenCloseEvent = new ScreenCloseEvent(MinecraftClient.getInstance().currentScreen);
            EventBus.getInstance().post(screenCloseEvent);
            if (screenCloseEvent.checkState()) {
                callbackInfo.cancel();
            }
        }
    }

    @Inject(method = "pushOutOfBlocks(DD)V", at = @At("HEAD"), cancellable = true)
    private void removeBlockPush(double local8401_1, double local8401_3, CallbackInfo callbackInfo) {
        if (NoPushModule.getInstance().matchesCriteria("Блоки")) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "getActiveItemSpeedMultiplier()F", at = @At("HEAD"), cancellable = true)
    private void liftItemUseSlow(CallbackInfoReturnable<Float> local8402_1) {
        if (EventBus.getInstance().hasListeners(PlayerSlowdownEvent.class)) {
            PlayerSlowdownEvent playerSlowdownEvent = new PlayerSlowdownEvent();
            EventBus.getInstance().post(playerSlowdownEvent);
            if (playerSlowdownEvent.checkState()) {
                local8402_1.setReturnValue(1.0F);
            }
        }
    }

    @Inject(method = "getCrosshairTarget(FLnet/minecraft/entity/Entity;)Lnet/minecraft/util/hit/HitResult;", at = @At("HEAD"), cancellable = true)
    private void traceThroughEntities(float local8403_1, Entity entity, CallbackInfoReturnable<HitResult> local8403_3) {
        if (NoEntityTraceModule.getInstance().isEnabled()
                || ChestStealerModule.getInstance().isEnabled() && ChestStealerModule.getInstance().disableHitboxes.isChecked()) {
            local8403_3.setReturnValue(entity.raycast(((ClientPlayerEntity)(Object)this).getBlockInteractionRange(), local8403_1, false));
        }
    }

    @Inject(method = "dropSelectedItem(Z)Z", at = @At("HEAD"), cancellable = true)
    private void blockSelectedSlotDrop(boolean local8404_1, CallbackInfoReturnable<Boolean> local8404_2) {
        if (BlockSlotModule.getInstance().isSlotProtected(((ClientPlayerEntity)(Object)this).getInventory().getSelectedSlot())) {
            local8404_2.setReturnValue(false);
        }
    }

    @Redirect(method = "tickMovementInput()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float steerHandBobPitch(ClientPlayerEntity clientPlayerEntity) {
        return RotationManager.getRotationManager().isRotating() ? RotationManager.getRotationManager().getTargetPitch() : clientPlayerEntity.getPitch();
    }

    @Redirect(method = "tickMovementInput()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float steerHandBobYaw(ClientPlayerEntity clientPlayerEntity) {
        return RotationManager.getRotationManager().isRotating() ? RotationManager.getRotationManager().getTargetYaw() : clientPlayerEntity.getYaw();
    }

    static {
        VMBridge.identifyClass(ClientPlayerEntityMixin2.class, "ZdXUcnyB");
    }
}