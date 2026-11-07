package greed.mixin;

import greed.client.module.render.ShulkerPreviewModule;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
   @Inject(method = "renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"))
   private void renderShulkerToolip(DrawContext drawContext, int local8068_2, int local8068_3, float local8068_4, CallbackInfo callbackInfo) {
      if ((Object)this instanceof HandledScreen<?> local8068_6) {
         if (((HandledScreenMixin)local8068_6).hoveredSlot() != null && ((HandledScreenMixin)local8068_6).hoveredSlot().hasStack()) {
            ShulkerPreviewModule.getInstance().renderElement2(drawContext, ((HandledScreenMixin)local8068_6).hoveredSlot().getStack(), local8068_2, local8068_3);
         }
      }
   }
}
