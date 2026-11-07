package greed.client.render;

import greed.client.GreedClient;
import greed.mixin.DrawContextMixin;
import greed.mixin.ItemRenderStateMixin;
import greed.mixin.ItemRenderState_LayerRenderStateMixin;
import greed.client.util.GuiScale;
import greed.client.render.GuiLayerSerialAccess;
import greed.client.render.ItemDrawData;
import greed.client.render.CachedItemGeometry;
import greed.client.render.ScissorStack;
import greed.client.render.ItemGeometryQuad;
import greed.client.util.TextureBinding;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.KeyedItemRenderState;
import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.client.render.item.ItemRenderState.LayerRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryUtil;

public final class ItemRenderer implements AutoCloseable {
   private static volatile ItemRenderer itemRenderer;
   private static final VertexFormat vertexFormat = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/item"))
      .withVertexShader(getIdentifier("core/item"))
      .withFragmentShader(getIdentifier("core/item"))
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ItemParamsArray", UniformType.UNIFORM_BUFFER)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withVertexFormat(vertexFormat, DrawMode.QUADS)
      .build();
   private final Map<Identifier, ItemRenderer.DataRecord6> textureBindings = new HashMap<>();
   private final Map<ItemRenderer.DataRecord, CachedItemGeometry> itemGeometryCache = new LinkedHashMap<ItemRenderer.DataRecord, CachedItemGeometry>(
      256, 0.75F, true
   ) {
      @Override
      protected boolean removeEldestEntry(Entry<ItemRenderer.DataRecord, CachedItemGeometry> eldestEntry) {
         return this.size() > 512;
      }
   };
   private final Map<ItemRenderer.CachedItemKey, CachedItemGeometry> modelGeometryCache = new LinkedHashMap<ItemRenderer.CachedItemKey, CachedItemGeometry>(
      256, 0.75F, true
   ) {
      @Override
      protected boolean removeEldestEntry(Entry<ItemRenderer.CachedItemKey, CachedItemGeometry> eldestEntry) {
         return this.size() > 512;
      }
   };
   private ByteBuffer paramsByteBuffer;
   private final Map<ItemRenderer.DataRecord2, GuiItemRenderState> renderStates = new LinkedHashMap<>(32);
   private final Map<ItemGuiElementRenderState, Integer> itemParamIndices = new IdentityHashMap<>();
   private Integer currentTintColor;
   private final float[] paramValues = new float[4096];
   private int paramCount;
   private DrawContext lastDrawContext;
   private GpuBuffer paramsGpuBuffer;
   private boolean paramsDirty;

   private static Identifier getIdentifier(String path) {
      return Identifier.of("greed", path);
   }

   private CachedItemGeometry getCachedItemGeometry(MinecraftClient minecraftClient, ItemDrawData itemDrawData) {
      ItemRenderer.DataRecord dataRecord = ItemRenderer.DataRecord.of(itemDrawData.stack(), itemDrawData.seed());
      CachedItemGeometry cachedItemGeometry = this.itemGeometryCache.get(dataRecord);
      if (cachedItemGeometry != null) {
         return cachedItemGeometry;
      }

      KeyedItemRenderState itemRenderState = new KeyedItemRenderState();
      minecraftClient.getItemModelManager()
         .clearAndUpdate(itemRenderState, itemDrawData.stack(), ItemDisplayContext.GUI, minecraftClient.world, minecraftClient.player, itemDrawData.seed());
      ItemRenderer.CachedItemKey cachedItemKey = new ItemRenderer.CachedItemKey(itemRenderState.getModelKey(), itemDrawData.seed());
      CachedItemGeometry cachedItemGeometry2 = this.modelGeometryCache.get(cachedItemKey);
      if (cachedItemGeometry2 == null) {
         cachedItemGeometry2 = this.getCachedItemGeometry2(itemRenderState);
         this.modelGeometryCache.put(cachedItemKey, cachedItemGeometry2);
      }

      if (!itemRenderState.isAnimated()) {
         this.itemGeometryCache.put(dataRecord, cachedItemGeometry2);
      }

      return cachedItemGeometry2;
   }

   private boolean hasSpecialRenderer(LayerRenderState layerRenderState) {
      return layerRenderState != null && ((ItemRenderState_LayerRenderStateMixin)layerRenderState).getSpecialRenderer() != null;
   }

   public void processData(ItemDrawData itemDrawData) {
      this.submitItem(this.lastDrawContext, itemDrawData);
   }

   public void processData2(ItemGuiElementRenderState itemGuiElementRenderState) {
      if (this.currentTintColor != null) {
         this.itemParamIndices.put(itemGuiElementRenderState, this.currentTintColor);
      }
   }

   private static int scaleColorChannels(int color, float factor) {
      color = ensureOpaqueAlpha(color);
      int alpha = color >>> 24 & 0xFF;
      int red = clamp(Math.round((color >>> 16 & 0xFF) * factor), 0, 255);
      int green = clamp(Math.round((color >>> 8 & 0xFF) * factor), 0, 255);
      int blue = clamp(Math.round((color & 0xFF) * factor), 0, 255);
      return alpha << 24 | red << 16 | green << 8 | blue;
   }

   private ByteBuffer getByteBuffer(boolean fullSize) {
      if (this.paramsByteBuffer == null) {
         this.paramsByteBuffer = MemoryUtil.memCalloc(65536);
      }

      ByteBuffer byteBuffer = this.paramsByteBuffer;
      byteBuffer.clear();
      float timeSeconds = (float)(System.nanoTime() % 30000000000L) / 1.0E9F;

      for (int index = 0; index < this.paramCount; index++) {
         int offset = index * 4 * 4;
         byteBuffer.putFloat(offset, this.paramValues[index]);
         byteBuffer.putFloat(offset + 4, timeSeconds);
         byteBuffer.putFloat(offset + 8, 0.0F);
         byteBuffer.putFloat(offset + 12, 0.0F);
      }

      int byteCount = this.paramCount * 4 * 4;
      if (fullSize) {
         if (byteCount < 65536) {
            MemoryUtil.memSet(MemoryUtil.memAddress(byteBuffer) + byteCount, 0, 65536 - byteCount);
         }

         byteBuffer.position(0).limit(65536);
      } else {
         byteBuffer.position(0).limit(Math.max(16, byteCount));
      }

      return byteBuffer;
   }

   private GpuBuffer getGpuBuffer() {
      if (this.paramsGpuBuffer != null && !this.paramsGpuBuffer.isClosed() && this.paramsGpuBuffer.size() >= 65536L) {
         return this.paramsGpuBuffer;
      }

      this.closeGpuBuffer();

      try {
         this.paramsGpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_item_params", 136, 65536L);
         return this.paramsGpuBuffer;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   public boolean matchesCriteria2(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2;
   }

   private void addGeometryToState(
      GuiRenderState guiRenderState,
      int layerSerial,
      Matrix3x2f matrix3x2f,
      ItemRenderer.DataRecord3 pose,
      TextureBinding textureBinding,
      ItemGeometryQuad itemGeometryQuad,
      float x,
      float y,
      float size,
      int tint,
      float glint
   ) {
      ItemRenderer.DataRecord2 stateKey = new ItemRenderer.DataRecord2(
         guiRenderState, layerSerial, textureBinding.id(), pose, ScissorStack.getScreenRect()
      );
      GuiItemRenderState itemRenderState = this.renderStates.get(stateKey);
      if (itemRenderState == null) {
         itemRenderState = new GuiItemRenderState(matrix3x2f, textureBinding, ScissorStack.getScreenRect());
         itemRenderState.add(itemGeometryQuad, x, y, size, tint, glint);
         this.renderStates.put(stateKey, itemRenderState);
         guiRenderState.addSimpleElement(itemRenderState);
      } else {
         itemRenderState.add(itemGeometryQuad, x, y, size, tint, glint);
      }
   }

   private static int resolveTintColor(BakedQuad bakedQuad, int[] intArray) {
      int tintColor = -1;
      if (bakedQuad.hasTint()) {
         int tintIndex = bakedQuad.tintIndex();
         if (tintIndex >= 0 && tintIndex < intArray.length) {
            tintColor = ensureOpaqueAlpha(intArray[tintIndex]);
         }
      }

      return tintColor;
   }

   private static float computeQuadArea(
      float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3
   ) {
      return Math.abs(
            cross2d(x0, y0, x1, y1)
               + cross2d(x1, y1, x2, y2)
               + cross2d(x2, y2, x3, y3)
               + cross2d(x3, y3, x0, y0)
         )
         * 0.5F;
   }

   private void renderItemGeometry(DrawContext drawContext, ItemDrawData itemDrawData) {
      float scale = itemDrawData.size() / 16.0F;
      if (!(scale <= 0.0F)) {
         drawContext.getMatrices().pushMatrix();

         try {
            GuiScale.processData(drawContext.getMatrices());
            drawContext.getMatrices().translate(itemDrawData.x(), itemDrawData.y());
            drawContext.getMatrices().scale(scale);
            this.currentTintColor = applyAlpha(itemDrawData.options().color(), itemDrawData.options().alpha());
            drawContext.drawItem(itemDrawData.stack(), 0, 0, itemDrawData.seed());
         } finally {
            this.currentTintColor = null;
            drawContext.getMatrices().popMatrix();
         }
      }
   }

   public void renderElement(DrawContext drawContext) {
      if (this.lastDrawContext != drawContext) {
         this.renderStates.clear();
      }

      this.lastDrawContext = drawContext;
   }

   private void collectLayerQuads(List<ItemRenderer.DataRecord4> quads, ItemRenderer.SubContext5 subContext, LayerRenderState layerRenderState) {
      if (layerRenderState != null) {
         List<BakedQuad> currentQuads = layerRenderState.getQuads();
         if (!currentQuads.isEmpty()) {
            ItemRenderState_LayerRenderStateMixin itemRenderState_LayerRenderStateMixin = (ItemRenderState_LayerRenderStateMixin)layerRenderState;
            Glint foilType = itemRenderState_LayerRenderStateMixin.getFoilType();
            boolean hasFoil = foilType != Glint.NONE;
            int[] tintLayers = itemRenderState_LayerRenderStateMixin.getTintLayers();
            Matrix4f matrix4f = this.getMatrix4f(itemRenderState_LayerRenderStateMixin);
            boolean usesBlockLight = itemRenderState_LayerRenderStateMixin.getUsesBlockLight();
            if (usesBlockLight) {
               subContext.markExpanded();
            }

            for (BakedQuad bakedQuad : currentQuads) {
               ItemRenderer.DataRecord4 quadData = this.createQuadData(
                  bakedQuad, matrix4f, resolveTintColor(bakedQuad, tintLayers), hasFoil, usesBlockLight
               );
               if (!(quadData.area() < 2.0E-6F)) {
                  subContext.expandBounds(quadData.x0(), quadData.y0(), quadData.z0());
                  subContext.expandBounds(quadData.x1(), quadData.y1(), quadData.z1());
                  subContext.expandBounds(quadData.x2(), quadData.y2(), quadData.z2());
                  subContext.expandBounds(quadData.x3(), quadData.y3(), quadData.z3());
                  quads.add(quadData);
               }
            }
         }
      }
   }

   @Override
   public void close() {
      this.renderStates.clear();
      this.itemParamIndices.clear();
      this.paramCount = 0;
      this.itemGeometryCache.clear();
      this.modelGeometryCache.clear();
      this.textureBindings.clear();
      this.lastDrawContext = null;
      this.closeGpuBuffer();
      if (this.paramsByteBuffer != null) {
         MemoryUtil.memFree(this.paramsByteBuffer);
         this.paramsByteBuffer = null;
      }
   }

   private Matrix4f getMatrix4f(ItemRenderState_LayerRenderStateMixin itemRenderState_LayerRenderStateMixin) {
      net.minecraft.client.util.math.MatrixStack.Entry entry = new net.minecraft.client.util.math.MatrixStack.Entry();
      Transformation itemTransform = itemRenderState_LayerRenderStateMixin.getItemTransform();
      if (itemTransform == null) {
         itemTransform = Transformation.IDENTITY;
      }

      itemTransform.apply(false, entry);
      return new Matrix4f(entry.getPositionMatrix());
   }

   private ItemGeometryQuad getItemGeometryQuad(ItemRenderer.DataRecord4 quad, ItemRenderer.DataRecord7 transform) {
      return new ItemGeometryQuad(
         quad.atlas(),
         transform.x(quad.x0()),
         transform.y(quad.y0()),
         quad.u0(),
         quad.v0(),
         transform.x(quad.x1()),
         transform.y(quad.y1()),
         quad.u1(),
         quad.v1(),
         transform.x(quad.x2()),
         transform.y(quad.y2()),
         quad.u2(),
         quad.v2(),
         transform.x(quad.x3()),
         transform.y(quad.y3()),
         quad.u3(),
         quad.v3(),
         quad.tint(),
         quad.foil()
      );
   }

   private ItemRenderer.DataRecord4 createQuadData(BakedQuad bakedQuad, Matrix4f matrix4f, int tintColor, boolean foil, boolean usesBlockLight) {
      Vector3fc vector3fc = bakedQuad.position0();
      Vector3fc vector3fc2 = bakedQuad.position1();
      Vector3fc vector3fc3 = bakedQuad.position2();
      Vector3fc vector3fc4 = bakedQuad.position3();
      Vector3f vector3f = getVector3f(vector3fc, matrix4f);
      Vector3f vector3f2 = getVector3f(vector3fc2, matrix4f);
      Vector3f vector3f3 = getVector3f(vector3fc3, matrix4f);
      Vector3f vector3f4 = getVector3f(vector3fc4, matrix4f);
      long packedUv0 = bakedQuad.packedUV0();
      long packedUv1 = bakedQuad.packedUV1();
      long packedUv2 = bakedQuad.packedUV2();
      long packedUv3 = bakedQuad.packedUV3();
      float averageDepth = (vector3f.z + vector3f2.z + vector3f3.z + vector3f4.z) * 0.25F;
      int finalTint = usesBlockLight ? scaleColorChannels(tintColor, getFaceBrightness(bakedQuad.face())) : tintColor;
      return new ItemRenderer.DataRecord4(
         bakedQuad.sprite().getAtlasId(),
         vector3f.x,
         vector3f.y,
         Vector2f.getX(packedUv0),
         Vector2f.getY(packedUv0),
         vector3f2.x,
         vector3f2.y,
         Vector2f.getX(packedUv1),
         Vector2f.getY(packedUv1),
         vector3f3.x,
         vector3f3.y,
         Vector2f.getX(packedUv2),
         Vector2f.getY(packedUv2),
         vector3f4.x,
         vector3f4.y,
         Vector2f.getX(packedUv3),
         Vector2f.getY(packedUv3),
         finalTint,
         foil,
         averageDepth,
         computeQuadArea(vector3f.x, vector3f.y, vector3f2.x, vector3f2.y, vector3f3.x, vector3f3.y, vector3f4.x, vector3f4.y),
         vector3f.z,
         vector3f2.z,
         vector3f3.z,
         vector3f4.z
      );
   }

   public void updateState() {
      this.itemParamIndices.clear();
   }

   public static ItemRenderer getItemRenderer() {
      ItemRenderer currentItemRenderer = itemRenderer;
      if (currentItemRenderer == null) {
         synchronized (ItemRenderer.class) {
            currentItemRenderer = itemRenderer;
            if (currentItemRenderer == null) {
               currentItemRenderer = new ItemRenderer();
               itemRenderer = currentItemRenderer;
            }
         }
      }

      return currentItemRenderer;
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   private void closeGpuBuffer() {
      if (this.paramsGpuBuffer != null) {
         this.paramsGpuBuffer.close();
         this.paramsGpuBuffer = null;
      }
   }

   public static void updateState3() {
      ItemRenderer currentItemRenderer = itemRenderer;
      if (currentItemRenderer != null) {
         currentItemRenderer.close();
         itemRenderer = null;
      }
   }

   public void updateState4() {
      this.paramCount = 0;
      this.paramsDirty = false;
   }

   private static int multiplyColors(int colorA, int colorB, float alphaFactor) {
      colorA = ensureOpaqueAlpha(colorA);
      colorB = ensureOpaqueAlpha(colorB);
      int alpha = Math.round((colorA >>> 24 & 0xFF) * ((colorB >>> 24 & 0xFF) / 255.0F) * alphaFactor);
      int red = (colorA >>> 16 & 0xFF) * (colorB >>> 16 & 0xFF) / 255;
      int green = (colorA >>> 8 & 0xFF) * (colorB >>> 8 & 0xFF) / 255;
      int blue = (colorA & 0xFF) * (colorB & 0xFF) / 255;
      return clamp(alpha, 0, 255) << 24 | red << 16 | green << 8 | blue;
   }

   public void clearItemCache() {
      this.lastDrawContext = null;
      this.renderStates.clear();
   }

   private static float getFaceBrightness(Direction direction) {
      if (direction == null) {
         return 0.84F;
      }

      return switch (direction) {
         case UP -> 0.98F;
         case DOWN -> 0.72F;
         case NORTH -> 0.9F;
         case SOUTH -> 0.84F;
         case EAST, WEST -> 0.78F;
      };
   }

   private CachedItemGeometry getCachedItemGeometry2(ItemRenderState itemRenderState) {
      ItemRenderStateMixin itemRenderStateMixin = (ItemRenderStateMixin)itemRenderState;
      LayerRenderState[] layers = itemRenderStateMixin.getLayers();
      int layerCount = Math.min(itemRenderStateMixin.getActiveLayerCount(), layers.length);

      for (int index = 0; index < layerCount; index++) {
         if (this.hasSpecialRenderer(layers[index])) {
            return new CachedItemGeometry(List.of(), itemRenderState.isAnimated(), true);
         }
      }

      List<ItemRenderer.DataRecord4> quadList = new ArrayList<>(Math.max(8, layerCount * 8));
      ItemRenderer.SubContext5 bounds = new ItemRenderer.SubContext5();

      for (int index2 = 0; index2 < layerCount; index2++) {
         this.collectLayerQuads(quadList, bounds, layers[index2]);
      }

      quadList.sort(Comparator.comparingDouble(ItemRenderer.DataRecord4::depth));
      ItemRenderer.DataRecord7 normalizer = ItemRenderer.DataRecord7.of(bounds);
      List<ItemGeometryQuad> geometryQuads = new ArrayList<>(quadList.size());

      for (ItemRenderer.DataRecord4 quadData : quadList) {
         geometryQuads.add(this.getItemGeometryQuad(quadData, normalizer));
      }

      return new CachedItemGeometry(List.copyOf(geometryQuads), itemRenderState.isAnimated(), false);
   }

   private GpuBuffer getGpuBuffer2() {
      if (!this.paramsDirty && this.paramsGpuBuffer != null) {
         return this.paramsGpuBuffer;
      }

      this.updateState5();
      if (!this.paramsDirty && this.paramsGpuBuffer != null) {
         return this.paramsGpuBuffer;
      }

      this.closeGpuBuffer();
      ByteBuffer byteBuffer = this.getByteBuffer(true);
      this.paramsGpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_item_params", 128, byteBuffer);
      this.paramsDirty = false;
      return this.paramsGpuBuffer;
   }

   private static int applyAlpha(int color, float alpha) {
      color = ensureOpaqueAlpha(color);
      float alphaFraction = (color >>> 24 & 0xFF) / 255.0F * alpha;
      return Math.round(255.0F * alphaFraction) << 24
         | Math.round((color >>> 16 & 0xFF) * alphaFraction) << 16
         | Math.round((color >>> 8 & 0xFF) * alphaFraction) << 8
         | Math.round((color & 0xFF) * alphaFraction);
   }

   private static int ensureOpaqueAlpha(int color) {
      return (color & 0xFF000000) == 0 && (color & 16777215) != 0 ? color | 0xFF000000 : color;
   }

   int computeInt7(float value) {
      int index = this.paramCount;
      if (index == 4096) {
         return -1;
      }

      this.paramValues[index] = value;
      this.paramCount++;
      this.paramsDirty = true;
      return index;
   }

   public void updateState5() {
      if (this.paramCount != 0 && this.paramsDirty) {
         GpuBuffer gpuBuffer = this.getGpuBuffer();
         if (gpuBuffer != null) {
            try {
               ByteBuffer byteBuffer = this.getByteBuffer(false);
               RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
               this.paramsDirty = false;
            } catch (RuntimeException exception) {
               this.paramsDirty = true;
            }
         }
      }
   }

   private static float cross2d(float x0, float y0, float x1, float y1) {
      return x0 * y1 - x1 * y0;
   }

   private void submitItem(DrawContext drawContext, ItemDrawData itemDrawData) {
      if (drawContext != null && itemDrawData != null && itemDrawData.visible()) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (minecraftClient != null && minecraftClient.getItemModelManager() != null) {
            try {
               GuiRenderState guiRenderState = ((DrawContextMixin)drawContext).getGuiRenderState();
               int currentLayerSerial = ((GuiLayerSerialAccess)guiRenderState).getLayerSerial();
               Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
               ItemRenderer.DataRecord3 pose = ItemRenderer.DataRecord3.of(matrix3x2f);
               CachedItemGeometry cachedItemGeometry = this.getCachedItemGeometry(minecraftClient, itemDrawData);
               if (cachedItemGeometry.specialRenderer()) {
                  this.renderItemGeometry(drawContext, itemDrawData);
                  return;
               }

               for (ItemGeometryQuad itemGeometryQuad : cachedItemGeometry.quads()) {
                  TextureBinding textureBinding = this.getTextureBinding(itemGeometryQuad.atlas());
                  if (textureBinding != null) {
                     float glintStrength = itemDrawData.options().glintMode().shouldRender(itemDrawData.stack(), itemGeometryQuad.foil())
                        ? itemDrawData.options().glintStrength()
                        : 0.0F;
                     int tintColor = multiplyColors(itemDrawData.options().color(), itemGeometryQuad.tint(), itemDrawData.options().alpha());
                     this.addGeometryToState(
                        guiRenderState,
                        currentLayerSerial,
                        matrix3x2f,
                        pose,
                        textureBinding,
                        itemGeometryQuad,
                        itemDrawData.x(),
                        itemDrawData.y(),
                        itemDrawData.size(),
                        tintColor,
                        glintStrength
                     );
                  }
               }
            } catch (RuntimeException exception) {
               GreedClient.logger().warn("[RenderItem] Failed to submit item: {}", itemDrawData.stack().getName().getString(), exception);
            }
         }
      }
   }

   private static Vector3f getVector3f(Vector3fc vector3fc, Matrix4f matrix4f) {
      return new Vector3f(vector3fc).mulPosition(matrix4f);
   }

   private ItemRenderer() {
   }

   public int computeInt8(ItemGuiElementRenderState itemGuiElementRenderState) {
      return this.itemParamIndices.getOrDefault(itemGuiElementRenderState, -1);
   }

   public void processData5(RenderPass renderPass) {
      if (renderPass != null && this.paramCount != 0) {
         GpuBuffer gpuBuffer = this.getGpuBuffer2();
         if (gpuBuffer != null) {
            renderPass.setUniform("ItemParamsArray", gpuBuffer);
         }
      }
   }

   private TextureBinding getTextureBinding(Identifier identifier) {
      if (identifier == null) {
         return null;
      }

      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient == null) {
         return null;
      }

      TextureManager textureManager = minecraftClient.getTextureManager();
      if (textureManager == null) {
         return null;
      }

      AbstractTexture currentTexture = textureManager.getTexture(identifier);
      if (currentTexture != null && currentTexture.getGlTextureView() != null) {
         ItemRenderer.DataRecord6 cachedBinding = this.textureBindings.get(identifier);
         if (cachedBinding != null && cachedBinding.texture() == currentTexture) {
            return cachedBinding.value();
         }

         TextureSetup textureSetup = TextureSetup.of(currentTexture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
         TextureBinding textureBinding = new TextureBinding(
            identifier, textureSetup, TextureSetup.of(currentTexture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR))
         );
         this.textureBindings.put(identifier, new ItemRenderer.DataRecord6(currentTexture, textureBinding));
         return textureBinding;
      } else {
         return null;
      }
   }

   public void updateState6() {
      this.itemGeometryCache.clear();
      this.modelGeometryCache.clear();
      this.textureBindings.clear();
   }

   private record DataRecord(Item item, ComponentMap components, int count, int seed) {
      static ItemRenderer.DataRecord of(ItemStack itemStack, int seed) {
         return new ItemRenderer.DataRecord(itemStack.getItem(), itemStack.getImmutableComponents(), itemStack.getCount(), seed);
      }
   }

   private record DataRecord2(GuiRenderState state, int layerSerial, Identifier texture, ItemRenderer.DataRecord3 pose, ScreenRect scissor) {
   }

   private record DataRecord3(float m00, float m01, float m10, float m11, float m20, float m21) {
      static ItemRenderer.DataRecord3 of(Matrix3x2f matrix3x2f) {
         return new ItemRenderer.DataRecord3(
            matrix3x2f.m00(), matrix3x2f.m01(), matrix3x2f.m10(), matrix3x2f.m11(), matrix3x2f.m20(), matrix3x2f.m21()
         );
      }
   }

   private record DataRecord4(
      Identifier atlas,
      float x0,
      float y0,
      float u0,
      float v0,
      float x1,
      float y1,
      float u1,
      float v1,
      float x2,
      float y2,
      float u2,
      float v2,
      float x3,
      float y3,
      float u3,
      float v3,
      int tint,
      boolean foil,
      float depth,
      float area,
      float z0,
      float z1,
      float z2,
      float z3
   ) {
   }

   private static final class SubContext5 {
      private float minX = Float.MAX_VALUE;
      private float minY = Float.MAX_VALUE;
      private float minZ = Float.MAX_VALUE;
      private float maxX = -Float.MAX_VALUE;
      private float maxY = -Float.MAX_VALUE;
      private float maxZ = -Float.MAX_VALUE;
      private boolean forceExpand;

      private void markExpanded() {
         this.forceExpand = true;
      }

      private boolean needsExpansion() {
         return this.forceExpand || this.maxZ - this.minZ > 0.02F;
      }

      private void expandBounds(float x, float y, float z) {
         this.minX = Math.min(this.minX, x);
         this.minY = Math.min(this.minY, y);
         this.minZ = Math.min(this.minZ, z);
         this.maxX = Math.max(this.maxX, x);
         this.maxY = Math.max(this.maxY, y);
         this.maxZ = Math.max(this.maxZ, z);
      }

      private boolean isValid() {
         return this.minX <= this.maxX && this.minY <= this.maxY;
      }
   }

   private record DataRecord6(AbstractTexture texture, TextureBinding value) {
   }

   private record DataRecord7(float minX, float maxY, float scale, float offsetX, float offsetY) {
      private float x(float coordinate) {
         return this.offsetX + (coordinate - this.minX) * this.scale;
      }

      private float y(float coordinate) {
         return this.offsetY + (this.maxY - coordinate) * this.scale;
      }

      private static ItemRenderer.DataRecord7 of(ItemRenderer.SubContext5 bounds) {
         if (!bounds.isValid()) {
            return new ItemRenderer.DataRecord7(0.0F, 1.0F, 1.0F, 0.0F, 0.0F);
         } else {
            float width = Math.max(1.0E-4F, bounds.maxX - bounds.minX);
            float height = Math.max(1.0E-4F, bounds.maxY - bounds.minY);
            float maxDimension = Math.max(width, height);
            if (bounds.needsExpansion()) {
               float scale = Math.min(1.0F / maxDimension, 1.08F);
               return new ItemRenderer.DataRecord7(
                  (bounds.minX + bounds.maxX) * 0.5F, (bounds.minY + bounds.maxY) * 0.5F, scale, 0.5F, 0.5F
               );
            } else {
               float scale = 1.0F / maxDimension;
               return new ItemRenderer.DataRecord7(
                  bounds.minX,
                  bounds.maxY,
                  scale,
                  (1.0F - width * scale) * 0.5F,
                  (1.0F - height * scale) * 0.5F
               );
            }
         }
      }
   }

   private record CachedItemKey(Object identity, int seed) {
   }
}
