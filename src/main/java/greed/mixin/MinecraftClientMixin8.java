package greed.mixin;

import greed.client.event.EventBus;
import greed.client.event.SubTickEvent;
import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin8 {
   @Unique
   private long lastHookTime;

   @Inject(method = "render(Z)V", at = @At("HEAD"))
   private void postAccumulatedRender(boolean local8368_1, CallbackInfo callbackInfo) {
      long currentLastHookTime = Util.getMeasuringTimeNano();
      if (this.lastHookTime == 0L) {
         this.lastHookTime = currentLastHookTime;
      }

      long local8368_5 = (currentLastHookTime - this.lastHookTime) / 4166666L;
      this.lastHookTime += local8368_5 * 4166666L;
      int local8368_7 = (int)Math.min(local8368_5, 240L);
      if (local8368_7 != 0 && EventBus.getInstance().hasListeners(SubTickEvent.class)) {
         float tickProgress = ((MinecraftClient)(Object)this).getRenderTickCounter().getTickProgress(true);

         for (int index = 0; index < local8368_7; index++) {
            EventBus.getInstance().post(new SubTickEvent(index, local8368_7, Math.min(1.0F, tickProgress + (index + 1) / 12.0F)));
         }
      }
   }

   static {
      VMBridge.identifyClass(MinecraftClientMixin8.class, "r2WM85Zf");
   }
}
