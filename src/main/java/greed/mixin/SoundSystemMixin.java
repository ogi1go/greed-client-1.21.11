package greed.mixin;

import greed.client.module.misc.BassBoostModule;
import greed.client.module.render.NoRenderModule;

import java.util.Map;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.sound.Channel.SourceManager;
import net.minecraft.client.sound.SoundSystem.PlayResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
   @Shadow
   @Final
   private Map<SoundInstance, SourceManager> sources;

   @Shadow
   public abstract void stop(SoundInstance soundInstance);

   @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("HEAD"), cancellable = true)
   private void muteSounds(SoundInstance soundInstance, CallbackInfoReturnable<PlayResult> local8125_2) {
      if (NoRenderModule.getInstance().matchesCriteria(soundInstance)) {
         local8125_2.setReturnValue(PlayResult.NOT_STARTED);
      }
   }

   @Inject(method = "tick(Z)V", at = @At("HEAD"))
   private void stopMutedPlayingSounds(boolean local8126_1, CallbackInfo callbackInfo) {
      NoRenderModule noRenderModule = NoRenderModule.getInstance();
      if (noRenderModule.isEnabled()) {
         for (SoundInstance soundInstance : this.sources.keySet()) {
            if (noRenderModule.matchesCriteria(soundInstance)) {
               this.stop(soundInstance);
            }
         }
      }
   }

   @Inject(method = "stop()V", at = @At("HEAD"))
   private void forgetBassChain(CallbackInfo callbackInfo) {
      BassBoostModule.updateState();
   }
}
