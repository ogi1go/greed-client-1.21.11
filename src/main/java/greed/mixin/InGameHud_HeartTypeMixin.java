package greed.mixin;

import greed.client.module.render.NoRenderModule;
import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net/minecraft/client/gui/hud/InGameHud$HeartType")
public abstract class InGameHud_HeartTypeMixin {
   @Inject(method = "fromPlayerState(Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/client/gui/hud/InGameHud$HeartType;", at = @At("RETURN"), cancellable = true)
   private static void removeWitherHearts(PlayerEntity playerEntity, CallbackInfoReturnable<Object> local8179_1) {
      if (NoRenderModule.getInstance().matchesCriteria2("Иссушающие сердца")
         && local8179_1.getReturnValue() instanceof Enum<?> local8179_2
         && local8179_2.name().equals("WITHERED")) {
         local8179_1.setReturnValue(Enum.valueOf(local8179_2.getClass(), "NORMAL"));
      }
   }

   static {
      VMBridge.identifyClass(InGameHud_HeartTypeMixin.class, "NPAHNzJf");
   }
}
