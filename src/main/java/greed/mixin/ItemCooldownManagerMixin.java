package greed.mixin;

import greed.client.util.ItemCooldownAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemCooldownManager.class)
public abstract class ItemCooldownManagerMixin implements ItemCooldownAccess {
   @Shadow
   private int tick;
   @Shadow
   @Final
   private Map<Identifier, ?> entries;
   @Unique
   private final HashMap<Identifier, Integer> cooldownEndTickByGroup = new HashMap<>();

   @Shadow
   public abstract Identifier getGroup(ItemStack itemStack);

   @Shadow
   public abstract void remove(Identifier identifier);

   @Inject(method = "set(Lnet/minecraft/util/Identifier;I)V", at = @At("HEAD"))
   private void trackCooldownEndTick(Identifier identifier, int local8143_2, CallbackInfo callbackInfo) {
      this.cooldownEndTickByGroup.put(identifier, this.tick + local8143_2);
   }

   @Inject(method = "remove(Lnet/minecraft/util/Identifier;)V", at = @At("HEAD"))
   private void dropCooldownEndTick(Identifier identifier, CallbackInfo callbackInfo) {
      this.cooldownEndTickByGroup.remove(identifier);
   }

   @Override
   public int remainingCooldownTicks(ItemStack itemStack) {
      Integer local8145_2 = this.cooldownEndTickByGroup.get(this.getGroup(itemStack));
      return local8145_2 == null ? 0 : Math.max(0, local8145_2 - this.tick);
   }

   @Override
   public void wipeAllCooldowns() {
      for (Identifier identifier : new ArrayList<>(this.entries.keySet())) {
         this.remove(identifier);
      }
   }
}
