package greed.mixin;

import greed.client.module.render.CustomWorldModule;
import ru.dreamix.fabricloader.VMBridge;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.nio.ByteBuffer;
import net.minecraft.client.render.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
   @ModifyReturnValue(
      method = "getFogColor(Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/world/ClientWorld;IF)Lorg/joml/Vector4f;",
      at = @At("RETURN")
   )
   private Vector4f tintFogColor(Vector4f vector4f) {
      CustomWorldModule customWorldModule = CustomWorldModule.getInstance();
      if (customWorldModule.checkState4()) {
         int value = customWorldModule.fogColor.getValue();
         CustomWorldModule.floatValArray2[0] = (value >> 16 & 0xFF) / 255.0F;
         CustomWorldModule.floatValArray2[1] = (value >> 8 & 0xFF) / 255.0F;
         CustomWorldModule.floatValArray2[2] = (value & 0xFF) / 255.0F;
         return new Vector4f(CustomWorldModule.floatValArray2[0], CustomWorldModule.floatValArray2[1], CustomWorldModule.floatValArray2[2], vector4f.w);
      } else {
         CustomWorldModule.floatValArray2[0] = vector4f.x;
         CustomWorldModule.floatValArray2[1] = vector4f.y;
         CustomWorldModule.floatValArray2[2] = vector4f.z;
         return vector4f;
      }
   }

   @WrapOperation(
      method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V")
   )
   private void scaleFogDistance(
      FogRenderer fogRenderer,
      ByteBuffer byteBuffer,
      int local8342_3,
      Vector4f vector4f,
      float local8342_5,
      float local8342_6,
      float local8342_7,
      float local8342_8,
      float local8342_9,
      float local8342_10,
      Operation<Void> local8342_11
   ) {
      CustomWorldModule customWorldModule = CustomWorldModule.getInstance();
      if (customWorldModule.checkState4() && !customWorldModule.checkState3()) {
         float value = customWorldModule.fogDistance.getValue();
         local8342_11.call(
            new Object[]{
               fogRenderer, byteBuffer, local8342_3, vector4f, local8342_5, local8342_6, value * 0.2F, value, local8342_9, local8342_10
            }
         );
      } else {
         local8342_11.call(
            new Object[]{fogRenderer, byteBuffer, local8342_3, vector4f, local8342_5, local8342_6, local8342_7, local8342_8, local8342_9, local8342_10}
         );
      }
   }

   static {
      VMBridge.identifyClass(FogRendererMixin.class, "cvzKICOH");
   }
}
