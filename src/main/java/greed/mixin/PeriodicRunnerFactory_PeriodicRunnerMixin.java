package greed.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/client/realms/util/PeriodicRunnerFactory$PeriodicRunner")
public abstract class PeriodicRunnerFactory_PeriodicRunnerMixin {
   @Inject(method = "run(J)V", at = @At("HEAD"), cancellable = true)
   private void stopRealmsUpdate(long local8301_1, CallbackInfo callbackInfo) {
      callbackInfo.cancel();
   }
}
