package greed.mixin;

import greed.client.module.render.CustomWorldModule;
import ru.dreamix.fabricloader.VMBridge;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(World.class)
public abstract class WorldMixin {
   @ModifyReturnValue(method = "getRainGradient(F)F", at = @At("RETURN"))
   private float forceRainLevel(float local8082_1) {
      if (!((Object)this instanceof ClientWorld)) {
         return local8082_1;
      } else {
         CustomWorldModule customWorldModule = CustomWorldModule.getInstance();
         if (!customWorldModule.isEnabled() || customWorldModule.weather.isSelected("Выкл")) {
            return local8082_1;
         } else {
            return customWorldModule.weather.isSelected("Ясно") ? 0.0F : 1.0F;
         }
      }
   }

   @ModifyReturnValue(method = "getThunderGradient(F)F", at = @At("RETURN"))
   private float forceThunderLevel(float local8083_1) {
      if (!((Object)this instanceof ClientWorld)) {
         return local8083_1;
      } else {
         CustomWorldModule customWorldModule = CustomWorldModule.getInstance();
         if (!customWorldModule.isEnabled() || customWorldModule.weather.isSelected("Выкл")) {
            return local8083_1;
         } else {
            return customWorldModule.weather.isSelected("Гроза") ? 1.0F : 0.0F;
         }
      }
   }

   static {
      VMBridge.identifyClass(WorldMixin.class, "9l5yDGd0");
   }
}
