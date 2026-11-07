package greed.mixin;

import greed.client.module.render.PhysicSounds;
import greed.mixin.accessor.SourceAccessor;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(SoundSystem.class)
public abstract class SoundEngineMixin {

    @Shadow
    @Final
    private Map<SoundInstance, Channel.SourceManager> sources;

    @Inject(method = "stop()V", at = @At("HEAD"))
    private void onDestroy(CallbackInfo ci) {
        PhysicSounds module = PhysicSounds.INSTANCE;
        if (module != null) {
            module.invalidate();
        }
    }

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("RETURN"))
    private void onPlay(SoundInstance instance, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        SoundSystem.PlayResult result = cir.getReturnValue();
        if (result != SoundSystem.PlayResult.STARTED && result != SoundSystem.PlayResult.STARTED_SILENTLY) {
            return;
        }

        Channel.SourceManager handle = this.sources.get(instance);
        if (handle == null) {
            return;
        }

        handle.run(source -> {
            int sourceId = ((SourceAccessor) source).getPointer();

            PhysicSounds module = PhysicSounds.INSTANCE;
            if (module != null && module.isEnabled()) {
                module.processAcoustics(sourceId, instance);
            } else if (module != null) {
                module.reset(sourceId);
            }
        });
    }
}
