package greed.mixin;

import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.client.render.item.ItemRenderState.LayerRenderState;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.model.json.Transformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LayerRenderState.class)
public interface ItemRenderState_LayerRenderStateMixin {
   @Accessor("glint")
   Glint getFoilType();

   @Accessor("transform")
   Transformation getItemTransform();

   @Accessor("useLight")
   boolean getUsesBlockLight();

   @Accessor("specialModelType")
   SpecialModelRenderer<Object> getSpecialRenderer();

   @Accessor("tints")
   int[] getTintLayers();
}
