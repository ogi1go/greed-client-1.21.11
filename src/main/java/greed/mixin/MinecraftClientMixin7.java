package greed.mixin;

import greed.client.module.combat.ProjectileHelperModule;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin7 {
   @Inject(method = "doItemUse()V", at = @At("HEAD"), cancellable = true)
   private void deferCrossbowShotUntilAimed(CallbackInfo callbackInfo) {
      if (MinecraftClient.getInstance().player != null
         && MinecraftClient.getInstance().player.getMainHandStack().getItem() instanceof CrossbowItem
         && CrossbowItem.isCharged(MinecraftClient.getInstance().player.getMainHandStack())) {
         if (ProjectileHelperModule.getInstance().checkState3()) {
            callbackInfo.cancel();
         }
      }
   }
}
