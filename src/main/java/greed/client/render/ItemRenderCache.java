package greed.client.render;

import greed.client.util.Colors;
import greed.client.GreedClient;
import greed.client.render.FontFace;
import greed.client.util.ItemDrawOptions;
import greed.client.render.ItemRenderer;
import greed.client.render.Render2D;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;

public final class ItemRenderCache {
   private static final FontFace fontFace = FontFace.SEMI_BOLD;
   private static boolean flag;

   public static void updateState() {
      updateState5();
      GreedClient.logger().info("[RenderItem] RenderItem initialized");
   }

   public static void processData(ItemStack itemStack, float local3634_1, float local3634_2, float local3634_3, ItemDrawOptions itemDrawOptions) {
      ItemDrawData itemDrawData = new ItemDrawData(itemStack, local3634_1, local3634_2, local3634_3, itemDrawOptions, 0);
      getItemRenderer().processData(itemDrawData);
      processData8(itemDrawData);
   }

   private static ItemStack getItemStack(String local3635_0) {
      if (local3635_0 != null && !local3635_0.isBlank()) {
         Identifier identifier = local3635_0.indexOf(58) >= 0 ? Identifier.tryParse(local3635_0.trim()) : Identifier.ofVanilla(local3635_0.trim());
         if (identifier == null) {
            return ItemStack.EMPTY;
         }

         Item item = Registries.ITEM.getOptionalValue(identifier).orElse(null);
         return item == null ? ItemStack.EMPTY : item.getDefaultStack();
      } else {
         return ItemStack.EMPTY;
      }
   }

   public static void renderElement(DrawContext drawContext) {
      getItemRenderer().renderElement(drawContext);
   }

   public static void updateState2() {
      getItemRenderer().updateState();
   }

   private static void processData2(int local3639_0, float local3639_1, float local3639_2, float local3639_3, float local3639_4) {
      String local3639_5 = Integer.toString(local3639_0);
      float local3639_6 = Math.max(3.0F, Math.round(local3639_3 * 0.6F * 2.0F) / 2.0F);
      float local3639_7 = FontText.computeFloat(fontFace, local3639_5, local3639_6);
      float local3639_8 = Math.round((local3639_1 + local3639_3 - local3639_7) * 2.0F) / 2.0F;
      float local3639_9 = Math.round((local3639_2 + local3639_3 - local3639_6) * 2.0F) / 2.0F;
      int local3639_10 = Math.round(255.0F * local3639_4);
      FontText.processData3(fontFace, local3639_5, local3639_8, local3639_9, local3639_6, Colors.computeInt5(255, 255, 255, local3639_10));
   }

   public static void processData3(ItemStack itemStack, float local3640_1, float local3640_2, float local3640_3) {
      processData(itemStack, local3640_1, local3640_2, local3640_3, ItemDrawOptions.defaults());
   }

   private static void processData4(ItemStack itemStack, float local3641_1, float local3641_2, float local3641_3, float local3641_4) {
      float local3641_5 = Math.max(8.0F, local3641_3 * 0.78F);
      float local3641_6 = Math.max(0.7F, local3641_3 * 0.04F);
      float local3641_7 = local3641_1 + (local3641_3 - local3641_5) * 0.5F;
      float local3641_8 = local3641_2 + local3641_3 - local3641_6 - Math.max(0.35F, local3641_3 * 0.03F);
      float local3641_9 = Math.max(0.0F, Math.min(1.0F, itemStack.getItemBarStep() / 13.0F));
      int itemBarColor = itemStack.getItemBarColor();
      Render2D.drawRect(
         local3641_7, local3641_8, local3641_5, local3641_6, local3641_6 * 0.5F, Colors.computeInt5(0, 0, 0, Math.round(150.0F * local3641_4))
      );
      Render2D.drawRect(
         local3641_7,
         local3641_8,
         Math.max(1.0F, local3641_5 * local3641_9),
         local3641_6,
         local3641_6 * 0.5F,
         Colors.computeInt5(itemBarColor >>> 16 & 0xFF, itemBarColor >>> 8 & 0xFF, itemBarColor & 0xFF, Math.round(240.0F * local3641_4))
      );
   }

   public static void processData5(String local3642_0, float local3642_1, float local3642_2, float local3642_3) {
      processData(getItemStack(local3642_0), local3642_1, local3642_2, local3642_3, ItemDrawOptions.defaults());
   }

   public static void processData6(String local3643_0, float local3643_1, float local3643_2, float local3643_3, ItemDrawOptions itemDrawOptions) {
      processData(getItemStack(local3643_0), local3643_1, local3643_2, local3643_3, itemDrawOptions);
   }

   public static void updateState3() {
      getItemRenderer().updateState6();
   }

   private static ItemRenderer getItemRenderer() {
      return ItemRenderer.getItemRenderer();
   }

   public static void updateState4() {
      getItemRenderer().clearItemCache();
   }

   private ItemRenderCache() {
   }

   private static void updateState5() {
      if (!flag) {
         Identifier identifier = Identifier.of("greed", "render_item_cache");
         ResourceLoader.get(ResourceType.CLIENT_RESOURCES).registerReloader(identifier, (SynchronousResourceReloader)resourceManager -> updateState3());
         flag = true;
      }
   }

   public static void updateState6() {
      ItemRenderer.updateState3();
      GreedClient.logger().info("[RenderItem] RenderItem closed");
   }

   public static void updateState7() {
      getItemRenderer().updateState4();
   }

   public static boolean matchesCriteria(RenderPipeline renderPipeline) {
      return getItemRenderer().matchesCriteria2(renderPipeline);
   }

   public static void processData7(ItemStack itemStack, float local3652_1, float local3652_2, float local3652_3, float local3652_4) {
      processData(itemStack, local3652_1, local3652_2, local3652_3, ItemDrawOptions.defaults().alpha(local3652_4));
   }

   public static int computeInt(ItemGuiElementRenderState itemGuiElementRenderState) {
      return getItemRenderer().computeInt8(itemGuiElementRenderState);
   }

   public static void updateState8() {
      getItemRenderer().updateState5();
   }

   private static void processData8(ItemDrawData itemDrawData) {
      if (itemDrawData != null && itemDrawData.visible()) {
         ItemStack itemStack = itemDrawData.stack();
         ItemDrawOptions itemDrawOptions = itemDrawData.options();
         if (itemDrawOptions.showDurability() && itemStack.isItemBarVisible()) {
            processData4(itemStack, itemDrawData.x(), itemDrawData.y(), itemDrawData.size(), itemDrawOptions.alpha());
         }

         if (itemDrawOptions.showCount() && itemStack.getCount() > 1) {
            processData2(itemStack.getCount(), itemDrawData.x(), itemDrawData.y(), itemDrawData.size(), itemDrawOptions.alpha());
         }
      }
   }

   public static void processData9(ItemGuiElementRenderState itemGuiElementRenderState) {
      getItemRenderer().processData2(itemGuiElementRenderState);
   }

   public static void processData10(RenderPass renderPass) {
      getItemRenderer().processData5(renderPass);
   }
}
