package greed.mixin;

import greed.client.MinecraftAccess;
import greed.client.module.player.NoPushModule;
import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldBorder.class)
public abstract class WorldBorderMixin implements MinecraftAccess {
   @Inject(method = "canCollide(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Z", at = @At("HEAD"), cancellable = true)
   private void removeBorderPush(Entity entity, Box box, CallbackInfoReturnable<Boolean> local8250_3) {
      if (entity == mc.player && NoPushModule.getInstance().matchesCriteria("Граница мира")) {
         local8250_3.setReturnValue(false);
      }
   }

   static {
      VMBridge.identifyClass(WorldBorderMixin.class, "RMgutFys");
   }
}
