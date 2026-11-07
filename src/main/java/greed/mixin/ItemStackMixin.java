package greed.mixin;

import greed.client.module.misc.YTMODEModule;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
   @Inject(method = "hasGlint()Z", at = @At("HEAD"), cancellable = true)
   private void stripFoilOnYtMode(CallbackInfoReturnable<Boolean> local8303_1) {
      if (YTMODEModule.getInstance().isEnabled()) {
         local8303_1.setReturnValue(false);
      }
   }
}
