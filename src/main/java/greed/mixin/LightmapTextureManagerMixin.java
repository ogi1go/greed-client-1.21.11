package greed.mixin;

import greed.client.module.render.BrightnessModule;
import greed.client.module.render.CustomWorldModule;
import greed.client.module.render.NoRenderModule;
import ru.dreamix.fabricloader.VMBridge;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
   @Inject(method = "getDarkness(Lnet/minecraft/entity/LivingEntity;FF)F", at = @At("HEAD"), cancellable = true)
   private void removeDarkness(LivingEntity livingEntity, float local8198_2, float local8198_3, CallbackInfoReturnable<Float> local8198_4) {
      if (NoRenderModule.getInstance().matchesCriteria2("Темнота")) {
         local8198_4.setReturnValue(0.0F);
      }
   }

   @ModifyExpressionValue(method = "update(F)V", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
   private float boostWorldBrightness(float local8199_1) {
      CustomWorldModule customWorldModule = CustomWorldModule.getInstance();
      return local8199_1
         * (
            BrightnessModule.getInstance().isEnabled() && BrightnessModule.getInstance().modeSetting.isSelected("Гамма")
               ? BrightnessModule.getInstance().legacyNumberSetting.getValue()
               : 1.0F
         )
         * (customWorldModule.isEnabled() ? customWorldModule.brightness.getValue() : 1.0F);
   }

   static {
      VMBridge.identifyClass(LightmapTextureManagerMixin.class, "4fxSAVJ0");
   }
}
