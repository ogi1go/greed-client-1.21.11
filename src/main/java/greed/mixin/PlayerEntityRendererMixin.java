package greed.mixin;

import greed.client.module.render.TagsModule;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
   @Inject(method = "hasLabel(Lnet/minecraft/entity/PlayerLikeEntity;D)Z", at = @At("HEAD"), cancellable = true)
   private void hideVanillaNameTag(PlayerLikeEntity playerLikeEntity, double local8196_2, CallbackInfoReturnable<Boolean> local8196_4) {
      if (TagsModule.getInstance().isEnabled() && playerLikeEntity instanceof AbstractClientPlayerEntity) {
         local8196_4.setReturnValue(false);
      }
   }
}
