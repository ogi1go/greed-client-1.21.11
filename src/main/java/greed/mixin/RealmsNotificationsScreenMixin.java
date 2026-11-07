package greed.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.realms.gui.screen.RealmsNotificationsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RealmsNotificationsScreen.class)
public abstract class RealmsNotificationsScreenMixin {
   @Inject(method = "init()V", at = @At("HEAD"), cancellable = true)
   private void stopRealmsInit(CallbackInfo callbackInfo) {
      callbackInfo.cancel();
   }

   @Inject(method = "onDisplayed()V", at = @At("HEAD"), cancellable = true)
   private void stopRealmsAdded(CallbackInfo callbackInfo) {
      callbackInfo.cancel();
   }

   @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
   private void stopRealmsPolling(CallbackInfo callbackInfo) {
      callbackInfo.cancel();
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true)
   private void stopRealmsRender(DrawContext drawContext, int local8214_2, int local8214_3, float local8214_4, CallbackInfo callbackInfo) {
      callbackInfo.cancel();
   }
}
