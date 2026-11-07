package greed.mixin;

import greed.client.render.GuiLayerSerialAccess;
import greed.client.render.ItemRenderCache;
import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin implements GuiLayerSerialAccess {
   @Unique
   private int layerSerial;
   @Shadow
   private int blurLayer;

   @Override
   public int getLayerSerial() {
      return this.layerSerial;
   }

   @Override
   public void bumpLayerSerial() {
      this.layerSerial++;
   }

   @Inject(method = "createNewRootLayer()V", at = @At("RETURN"))
   private void trackNextStratum(CallbackInfo callbackInfo) {
      this.layerSerial++;
   }

   @Inject(method = "goUpLayer()V", at = @At("RETURN"))
   private void trackUpLayer(CallbackInfo callbackInfo) {
      this.layerSerial++;
   }

   @Inject(method = "clear()V", at = @At("HEAD"))
   private void resetLayerSerial(CallbackInfo callbackInfo) {
      this.layerSerial = 0;
   }

   @Inject(method = "applyBlur()V", at = @At("HEAD"), cancellable = true)
   private void skipSecondBlur(CallbackInfo callbackInfo) {
      if (this.blurLayer != Integer.MAX_VALUE) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "addItem(Lnet/minecraft/client/gui/render/state/ItemGuiElementRenderState;)V", at = @At("HEAD"))
   private void trackSpecialItemTint(ItemGuiElementRenderState itemGuiElementRenderState, CallbackInfo callbackInfo) {
      ItemRenderCache.processData9(itemGuiElementRenderState);
   }

   static {
      VMBridge.identifyClass(GuiRenderStateMixin.class, "dxbGAceV");
   }
}
