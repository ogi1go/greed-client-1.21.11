package greed.mixin;

import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin2 {
   @Unique
   private static final Identifier greed$foodEmpty = Identifier.ofVanilla("hud/food_empty");
   @Unique
   private static final Identifier greed$foodHalf = Identifier.ofVanilla("hud/food_half");
   @Unique
   private static final Identifier greed$foodFull = Identifier.ofVanilla("hud/food_full");

   @Inject(method = "renderFood(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;II)V", at = @At("TAIL"))
   private void renderSaturationBar(DrawContext drawContext, PlayerEntity playerEntity, int local8281_3, int local8281_4, CallbackInfo callbackInfo) {
      int local8281_6 = Math.round(playerEntity.getHungerManager().getSaturationLevel());
      if (local8281_6 > 0) {
         for (int index = 0; index < 10; index++) {
            drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, greed$foodEmpty, local8281_4 - index * 8 - 9, local8281_3 - 10, 9, 9);
            if (index * 2 + 1 < local8281_6) {
               drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, greed$foodFull, local8281_4 - index * 8 - 9, local8281_3 - 10, 9, 9);
            } else if (index * 2 + 1 == local8281_6) {
               drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, greed$foodHalf, local8281_4 - index * 8 - 9, local8281_3 - 10, 9, 9);
            }
         }
      }
   }

   static {
      VMBridge.identifyClass(InGameHudMixin2.class, "n61THj3q");
   }
}
