package greed.mixin;

import greed.client.ui.GreedCosmeticsScreen;
import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SpecialGuiElementRenderer.class)
public abstract class SpecialGuiElementRendererMixin {
   @ModifyArg(
      method = "renderElement(Lnet/minecraft/client/gui/render/state/special/SpecialGuiElementRenderState;Lnet/minecraft/client/gui/render/state/GuiRenderState;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/render/state/TexturedQuadGuiElementRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/texture/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/ScreenRect;Lnet/minecraft/client/gui/ScreenRect;)V"
      ),
      index = 11
   )
   private int fadeCosmeticModel(int local8239_1) {
      float local8239_2 = GreedCosmeticsScreen.modelAlpha();
      if (local8239_2 >= 0.999F) {
         return local8239_1;
      }

      int local8239_3 = Math.round(Math.max(0.0F, local8239_2) * 255.0F);
      return local8239_3 << 24 | local8239_3 << 16 | local8239_3 << 8 | local8239_3;
   }

   static {
      VMBridge.identifyClass(SpecialGuiElementRendererMixin.class, "JXhSlpVX");
   }
}
