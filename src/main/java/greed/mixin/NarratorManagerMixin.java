package greed.mixin;

import com.mojang.text2speech.Narrator;
import net.minecraft.client.option.NarratorMode;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NarratorManager.class)
public abstract class NarratorManagerMixin {
   @Redirect(method = "<init>(Lnet/minecraft/client/MinecraftClient;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/text2speech/Narrator;getNarrator()Lcom/mojang/text2speech/Narrator;"))
   private Narrator skipNarratorLibrary() {
      return Narrator.EMPTY;
   }

   @Inject(method = "say(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
   private void muteMessage(String local8203_1, boolean local8203_2, CallbackInfo callbackInfo) {
      callbackInfo.cancel();
   }

   @Inject(method = "narrateText(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
   private void muteQueuedMessage(Text text, CallbackInfo callbackInfo) {
      callbackInfo.cancel();
   }

   @Inject(method = "onModeChange(Lnet/minecraft/client/option/NarratorMode;)V", at = @At("HEAD"), cancellable = true)
   private void ignoreStatusChange(NarratorMode narratorMode, CallbackInfo callbackInfo) {
      callbackInfo.cancel();
   }

   @Inject(method = "checkNarratorLibrary(Z)V", at = @At("HEAD"), cancellable = true)
   private void ignoreStatusCheck(boolean local8206_1, CallbackInfo callbackInfo) {
      callbackInfo.cancel();
   }

   @Inject(method = "isActive()Z", at = @At("HEAD"), cancellable = true)
   private void neverActive(CallbackInfoReturnable<Boolean> local8207_1) {
      local8207_1.setReturnValue(false);
   }
}
