package greed.mixin;

import greed.client.module.render.CustomWorldModule;
import ru.dreamix.fabricloader.VMBridge;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WeatherRendering.class)
public abstract class WeatherRenderingMixin {
   @WrapOperation(
      method = "renderPrecipitation(Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/client/render/state/WeatherRenderState;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WeatherRendering;renderPieces(Lnet/minecraft/client/render/VertexConsumer;Ljava/util/List;Lnet/minecraft/util/math/Vec3d;FIF)V",
         ordinal = 0
      )
   )
   private void skipVanillaRain(
      WeatherRendering weatherRendering,
      VertexConsumer vertexConsumer,
      List<?> local8115_3,
      Vec3d vec3d,
      float local8115_5,
      int local8115_6,
      float local8115_7,
      Operation<Void> local8115_8
   ) {
      if (!CustomWorldModule.getInstance().isEnabled()
         || !CustomWorldModule.getInstance().shaderRain.isChecked()
            && (
               !CustomWorldModule.getInstance().worldRain.isChecked()
                  || !CustomWorldModule.getInstance().hideVanillaRain.isChecked()
                  || !(CustomWorldModule.getInstance().rainStreams.getValue() > 0.002F)
            )) {
         local8115_8.call(new Object[]{weatherRendering, vertexConsumer, local8115_3, vec3d, local8115_5, local8115_6, local8115_7});
      }
   }

   @ModifyArg(
      method = "renderPieces(Lnet/minecraft/client/render/VertexConsumer;Ljava/util/List;Lnet/minecraft/util/math/Vec3d;FIF)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumer;color(I)Lnet/minecraft/client/render/VertexConsumer;")
   )
   private int tintPrecipitationColor(int local8116_1) {
      CustomWorldModule customWorldModule = CustomWorldModule.getInstance();
      if (customWorldModule.isEnabled() && customWorldModule.customPrecipitationColor.isChecked()) {
         int value = customWorldModule.precipitationColor.getValue();
         int local8116_4 = local8116_1 >>> 24;
         return local8116_4 << 24 | value & 16777215;
      } else {
         return local8116_1;
      }
   }

   static {
      VMBridge.identifyClass(WeatherRenderingMixin.class, "xV8TSUVH");
   }
}
