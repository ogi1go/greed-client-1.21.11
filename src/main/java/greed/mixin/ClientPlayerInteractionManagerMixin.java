package greed.mixin;

import greed.client.event.AttackEvent;
import greed.client.event.BlockBreakEvent;
import greed.client.event.EventBus;
import greed.client.module.combat.MaceHelperModule;
import greed.client.module.player.ServerHelperModule;
import greed.client.event.SlotClickEvent;

import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
   @Inject(method = "clickSlot(IIILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"), cancellable = true)
   private void clickSlot(int local8078_1, int local8078_2, int local8078_3, SlotActionType slotActionType, PlayerEntity playerEntity, CallbackInfo callbackInfo) {
      if (EventBus.getInstance().hasListeners(SlotClickEvent.class)) {
         SlotClickEvent slotClickEvent = new SlotClickEvent(local8078_1, local8078_2, local8078_3, slotActionType);
         EventBus.getInstance().post(slotClickEvent);
         if (slotClickEvent.checkState()) {
            callbackInfo.cancel();
         }
      }
   }
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void greed$onAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        EventBus.getInstance().post(new AttackEvent(target));
    }
   @Inject(method = "interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;", at = @At("HEAD"), cancellable = true)
   private void failAnchorUse(ClientPlayerEntity clientPlayerEntity, Hand hand, BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> local8079_4) {
      if (MaceHelperModule.getInstance().isEnabled() || ServerHelperModule.getInstance().isEnabled()) {
         if (clientPlayerEntity.getEntityWorld().getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR)) {
            local8079_4.setReturnValue(ActionResult.PASS);
         }
      }
   }

   @Inject(method = "stopUsingItem(Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"), cancellable = true)
   private void stopUseItem(PlayerEntity playerEntity, CallbackInfo callbackInfo) {
      if (EventBus.getInstance().hasListeners(BlockBreakEvent.class)) {
         BlockBreakEvent blockBreakEvent = new BlockBreakEvent();
         EventBus.getInstance().post(blockBreakEvent);
         if (blockBreakEvent.checkState()) {
            callbackInfo.cancel();
         }
      }
   }
}
