package greed.mixin;

import greed.client.module.render.AspectRatioModule;
import greed.client.render.BatchedBlurRenderer;
import greed.client.event.CameraFovEvent;
import greed.client.module.render.CustomWorldModule;
import greed.client.util.DepthSnapshot;
import greed.client.event.EventBus;
import greed.client.render.HandOutlineRenderer;
import greed.client.render.JumpCircleRenderer;
import greed.client.module.render.MotionBlurModule;
import greed.client.module.render.NoRenderModule;
import greed.client.render.RainScreenRenderer;
import greed.client.render.WorldProjection;
import greed.client.render.GuiEffectsState;
import greed.client.render.SaturationRenderer;
import ru.dreamix.fabricloader.VMBridge;
import greed.client.render.WetWorldRenderer;
import greed.client.render.WorldRainRenderer;
import greed.client.event.WorldRenderEvent;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
   @Inject(method = "getBasicProjectionMatrix(F)Lorg/joml/Matrix4f;", at = @At("TAIL"), cancellable = true)
   public void getProjectionMatrixHook(float local8087_1, CallbackInfoReturnable<Matrix4f> local8087_2) {
      if (EventBus.getInstance().hasListeners(CameraFovEvent.class)) {
         CameraFovEvent cameraFovEvent = new CameraFovEvent();
         EventBus.getInstance().post(cameraFovEvent);
         if (cameraFovEvent.checkState()) {
            Matrix4f matrix4f = new Matrix4f();
            matrix4f.perspective(local8087_1 * (float) (Math.PI / 180.0), cameraFovEvent.computeFloat(), 0.05F, ((GameRenderer)(Object)this).getFarPlaneDistance());
            local8087_2.setReturnValue(matrix4f);
         }
      }
   }

   @ModifyArg(
      method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/ProjectionMatrix3;set(IIF)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"),
      index = 0
   )
   private int stretchHandProjection(int local8088_1) {
      return AspectRatioModule.getInstance().computeInt(local8088_1);
   }

   @Inject(method = "renderBlur()V", at = @At("HEAD"), cancellable = true)
   private void customStratumBlur(CallbackInfo callbackInfo) {
      if (GuiEffectsState.checkState2()) {
         BatchedBlurRenderer.getBatchedBlurRenderer().processData16(GuiEffectsState.computeFloat2(), GuiEffectsState.computeFloat());
         BatchedBlurRenderer.getBatchedBlurRenderer().updateState3();
         BatchedBlurRenderer.getBatchedBlurRenderer().updateState9();
         callbackInfo.cancel();
      } else {
         if (GuiEffectsState.checkState()) {
            GuiEffectsState.processData3(false);
            BatchedBlurRenderer.getBatchedBlurRenderer().updateState3();
            BatchedBlurRenderer.getBatchedBlurRenderer().updateState9();
            callbackInfo.cancel();
         }
      }
   }

   @Inject(method = "tiltViewWhenHurt(Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At("HEAD"), cancellable = true)
   private void removeHurtShake(MatrixStack matrixStack, float local8090_2, CallbackInfo callbackInfo) {
      if (NoRenderModule.getInstance().matchesCriteria2("Тряска экрана") || NoRenderModule.getInstance().matchesCriteria2("Камера при уроне")) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("TAIL"))
   private void smearFrameAfterHand(RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      MotionBlurModule.updateState();
   }

   @Inject(method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("TAIL"))
   private void drawHandOutlineAfterHand(RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      HandOutlineRenderer.getInstance().updateState();
   }

   @Inject(method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("TAIL"))
   private void applyWorldSaturation(RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      SaturationRenderer.getInstance().updateState();
   }

   @Inject(
      method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/memory/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
         shift = Shift.BEFORE
      )
   )
   private void captureMatricesBeforeWorld(
      RenderTickCounter renderTickCounter, CallbackInfo callbackInfo, @Local(ordinal = 1) Matrix4f matrix4f, @Local(ordinal = 0) Matrix4f matrix4f2
   ) {
      WorldProjection.processData(matrix4f, matrix4f2);
   }

   @Inject(
      method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/memory/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
         shift = Shift.AFTER
      )
   )
   private void hookWorldRender(
      RenderTickCounter renderTickCounter, CallbackInfo callbackInfo, @Local(ordinal = 1) Matrix4f matrix4f, @Local(ordinal = 0) Matrix4f matrix4f2
   ) {
      WorldProjection.processData(matrix4f, matrix4f2);
      DepthSnapshot.updateState4();
      CustomWorldModule.getInstance().processData(matrix4f, matrix4f2);
      CustomWorldModule.getInstance().processData5(matrix4f, matrix4f2);
      WorldRainRenderer.getInstance().processData(matrix4f, matrix4f2);
      WetWorldRenderer.getInstance().processData(matrix4f, matrix4f2);
      RainScreenRenderer.getInstance().processData(matrix4f, matrix4f2);
      JumpCircleRenderer.getInstance().processData(matrix4f, matrix4f2);
      if (EventBus.getInstance().hasListeners(WorldRenderEvent.class)) {
         MatrixStack matrixStack = new MatrixStack();
         matrixStack.multiplyPositionMatrix(matrix4f);
         EventBus.getInstance().post(new WorldRenderEvent(matrixStack, renderTickCounter.getTickProgress(false)));
      }
   }

   @Inject(method = "showFloatingItem(Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
   private void hideTotemActivation(ItemStack itemStack, CallbackInfo callbackInfo) {
      if (NoRenderModule.getInstance().matchesCriteria2("Анимация тотема")) {
         callbackInfo.cancel();
      }
   }

   static {
      VMBridge.identifyClass(GameRendererMixin.class, "1JXtEvNw");
   }
}
