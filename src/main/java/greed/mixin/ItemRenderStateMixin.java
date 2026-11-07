package greed.mixin;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderState.LayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.class)
public interface ItemRenderStateMixin {
   @Accessor("layerCount")
   int getActiveLayerCount();

   @Accessor("layers")
   LayerRenderState[] getLayers();
}
