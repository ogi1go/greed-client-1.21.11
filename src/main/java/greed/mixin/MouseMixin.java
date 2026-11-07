package greed.mixin;

import greed.client.module.combat.ItemHelperModule;
import greed.client.module.player.ServerHelperModule;

import net.minecraft.client.Mouse;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Mouse.class)
public class MouseMixin {
   @Redirect(method = "onMouseScroll(JDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;setSelectedSlot(I)V"))
   private void skipBlockedScrollSlots(PlayerInventory playerInventory, int local8314_2) {
      playerInventory.setSelectedSlot(
         ItemHelperModule.getInstance().checkState2()
            ? playerInventory.getSelectedSlot()
            : ServerHelperModule.getInstance().computeInt2(playerInventory.getSelectedSlot(), local8314_2)
      );
   }
}
