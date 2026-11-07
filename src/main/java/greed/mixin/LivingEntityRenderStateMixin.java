package greed.mixin;

import greed.client.render.ChamsRenderStateAccess;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements ChamsRenderStateAccess {
   @Unique
   private boolean chams;

   @Override
   public boolean greed$chams() {
      return this.chams;
   }

   @Override
   public void greed$setChams(boolean newChams) {
      this.chams = newChams;
   }
}
