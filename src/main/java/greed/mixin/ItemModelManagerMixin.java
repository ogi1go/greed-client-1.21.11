package greed.mixin;

import greed.client.cosmetic.CosmeticItemRenderer;

import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelManager.class)
public abstract class ItemModelManagerMixin {
   @Inject(method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/world/World;Lnet/minecraft/util/HeldItemContext;I)V", at = @At("HEAD"), cancellable = true)
   private void appendWeaponSkin(
      ItemRenderState itemRenderState,
      ItemStack itemStack,
      ItemDisplayContext itemDisplayContext,
      World world,
      HeldItemContext heldItemContext,
      int local8113_6,
      CallbackInfo callbackInfo
   ) {
      if (CosmeticItemRenderer.matchesCriteria(itemRenderState, itemStack, itemDisplayContext, heldItemContext == null ? null : heldItemContext.getEntity())) {
         callbackInfo.cancel();
      }
   }
}
