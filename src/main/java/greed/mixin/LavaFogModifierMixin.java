package greed.mixin;

import greed.client.module.render.NoRenderModule;
import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.render.fog.LavaFogModifier;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LavaFogModifier.class)
public abstract class LavaFogModifierMixin {
   @Inject(method = "applyStartEndModifier(Lnet/minecraft/client/render/fog/FogData;Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/RenderTickCounter;)V", at = @At("TAIL"))
   private void removeLavaFog(
      FogData fogData, Camera camera, ClientWorld clientWorld, float local8409_4, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo
   ) {
      if (NoRenderModule.getInstance().matchesCriteria2("Оверлей лавы")) {
         fogData.environmentalStart = -8.0F;
         fogData.environmentalEnd = 64.0F;
         fogData.renderDistanceStart = -8.0F;
         fogData.renderDistanceEnd = 64.0F;
      }
   }

   static {
      VMBridge.identifyClass(LavaFogModifierMixin.class, "ID5njDBO");
   }
}
