package greed.mixin;

import greed.client.module.render.CameraFeatureModule;
import greed.client.event.EventBus;
import greed.client.render.HandOutlineRenderer;
import greed.client.event.HeldItemRenderEvent;
import greed.client.module.combat.ItemHelperModule;
import greed.client.MinecraftAccess;
import greed.client.RotationManager;
import greed.client.module.render.SwordAnimationsModule;
import ru.dreamix.fabricloader.VMBridge;
import greed.client.module.render.ViewModelModule;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin implements MinecraftAccess {
   @Shadow
   private ItemStack mainHand;
   @Shadow
   private ItemStack offHand;

   @Shadow
   protected abstract void swingArm(float var1, MatrixStack matrixStack, int var3, Arm arm);

   @WrapOperation(
      method = "updateHeldItems()V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;")
   )
   private ItemStack hideHelperItemInHand(ClientPlayerEntity clientPlayerEntity, Operation<ItemStack> local8183_2) {
      return ItemHelperModule.getInstance().computeInt2() != -1
         ? clientPlayerEntity.getInventory().getStack(ItemHelperModule.getInstance().computeInt2())
         : (ItemStack)local8183_2.call(clientPlayerEntity);
   }

   @Inject(
      method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", ordinal = 0, shift = Shift.AFTER)
   )
   private void hookRenderItem(
      AbstractClientPlayerEntity abstractClientPlayerEntity,
      float local8184_2,
      float local8184_3,
      Hand hand,
      float local8184_5,
      ItemStack itemStack,
      float local8184_7,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int local8184_10,
      CallbackInfo callbackInfo
   ) {
      if (EventBus.getInstance().hasListeners(HeldItemRenderEvent.class)) {
         EventBus.getInstance()
            .post(
               new HeldItemRenderEvent(
                  matrixStack,
                  itemStack,
                  hand,
                  hand == Hand.MAIN_HAND ? abstractClientPlayerEntity.getMainArm() : abstractClientPlayerEntity.getMainArm().getOpposite(),
                  local8184_5
               )
            );
      }
   }

   @Redirect(
      method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V",
         ordinal = 2
      )
   )
   private void redirectSwingArm(
      HeldItemRenderer heldItemRenderer,
      float local8185_2,
      MatrixStack matrixStack,
      int local8185_4,
      Arm arm,
      @Local(argsOnly = true, ordinal = 3) float local8185_6
   ) {
      AbstractClientPlayerEntity abstractClientPlayerEntity = mc.player;
      if (abstractClientPlayerEntity instanceof AbstractClientPlayerEntity) {
         if (SwordAnimationsModule.getInstance().matchesCriteria(arm)) {
            matrixStack.translate(-local8185_4 * 0.56F, 0.52F + local8185_6 * 0.6F - CameraFeatureModule.getInstance().computeFloat() * 0.6F, 0.72F);
            SwordAnimationsModule.getInstance().renderElement(matrixStack, local8185_4, local8185_2);
         } else {
            this.swingArm(local8185_2, matrixStack, local8185_4, arm);
         }

         float local8185_8 = SwordAnimationsModule.getInstance().computeFloat(arm);
         matrixStack.scale(local8185_8, local8185_8, local8185_8);
      } else {
         this.swingArm(local8185_2, matrixStack, local8185_4, arm);
      }
   }

   @ModifyVariable(method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V", at = @At("HEAD"), argsOnly = true, ordinal = 3)
   private float lowerHandOnFreeLook(float local8186_1) {
      return Math.max(local8186_1, CameraFeatureModule.getInstance().computeFloat());
   }

   @WrapOperation(
      method = "applyEatOrDrinkTransformation(Lnet/minecraft/client/util/math/MatrixStack;FLnet/minecraft/util/Arm;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/PlayerEntity;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal = 0)
   )
   private void removeUseWobble(MatrixStack matrixStack, float local8187_2, float local8187_3, float local8187_4, Operation<Void> local8187_5) {
      if (!ViewModelModule.getInstance().checkState2()) {
         local8187_5.call(matrixStack, local8187_2, local8187_3, local8187_4);
      }
   }

   @WrapOperation(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionfc;)V"))
   private void freezeHandSway(MatrixStack matrixStack, Quaternionfc quaternionfc, Operation<Void> local8188_3) {
      if (!ViewModelModule.getInstance().checkState3()) {
         local8188_3.call(matrixStack, quaternionfc);
      }
   }

   @Redirect(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch(F)F"))
   private float steerHandPitch(ClientPlayerEntity clientPlayerEntity, float local8189_2) {
      return RotationManager.getRotationManager().isRotating() ? RotationManager.getRotationManager().getTargetPitch() : clientPlayerEntity.getPitch(local8189_2);
   }

   @Redirect(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw(F)F"))
   private float steerHandYaw(ClientPlayerEntity clientPlayerEntity, float local8190_2) {
      return RotationManager.getRotationManager().isRotating() ? RotationManager.getRotationManager().getTargetYaw() : clientPlayerEntity.getYaw(local8190_2);
   }

   @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"))
   private void captureWorldBeforeHand(CallbackInfo callbackInfo) {
      HandOutlineRenderer.getInstance().updateState5();
   }

   @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("TAIL"))
   private void captureHandForOutline(CallbackInfo callbackInfo) {
      HandOutlineRenderer.getInstance().updateState2();
   }

   static {
      VMBridge.identifyClass(HeldItemRendererMixin.class, "MhxZBnDQ");
   }
}
