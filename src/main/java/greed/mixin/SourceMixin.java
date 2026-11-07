package greed.mixin;

import greed.client.module.misc.BassBoostModule;

import net.minecraft.client.sound.Source;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Source.class)
public class SourceMixin {
   @Shadow
   @Final
   private int pointer;

   @Inject(method = "play()V", at = @At("HEAD"))
   private void attachBassBoost(CallbackInfo callbackInfo) {
      BassBoostModule.processData(this.pointer);
   }
}
