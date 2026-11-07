package greed.mixin;

import greed.client.module.render.NoRenderModule;
import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.BlindnessEffectFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlindnessEffectFogModifier.class)
public abstract class BlindnessEffectFogModifierMixin {
   @Inject(method = "applyStartEndModifier(Lnet/minecraft/client/render/fog/FogData;Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/RenderTickCounter;)V", at = @At("TAIL"))
   private void removeBlindnessFog(
      FogData fogData, Camera camera, ClientWorld clientWorld, float local8284_4, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo
   ) {
      if (NoRenderModule.getInstance().matchesCriteria2("Туман слепоты")) {
         fogData.environmentalStart = local8284_4;
         fogData.environmentalEnd = local8284_4;
         fogData.skyEnd = 1.0F;
         fogData.cloudEnd = 1.0F;
      }
   }

   static {
      VMBridge.identifyClass(BlindnessEffectFogModifierMixin.class, "AqV0KMJ4");
   }
}
