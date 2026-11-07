package greed.client.render;

import greed.client.GreedClient;
import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.GuiLayerSerialAccess;
import greed.client.render.CachedTexture;
import greed.client.render.ImageDrawData;
import greed.client.render.TexturedQuadDrawData;
import greed.client.render.ScissorStack;

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
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

public final class ImageRenderer implements AutoCloseable {
   private static volatile ImageRenderer imageRenderer;
   private static final VertexFormat vertexFormat = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier2("pipeline/image"))
      .withVertexShader(getIdentifier2("core/image"))
      .withFragmentShader(getIdentifier2("core/image"))
      .withVertexFormat(vertexFormat, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ImageParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline pipeline3 = RenderPipeline.builder()
      .withLocation(getIdentifier2("pipeline/image_glow"))
      .withVertexShader(getIdentifier2("core/image"))
      .withFragmentShader(getIdentifier2("core/image"))
      .withVertexFormat(vertexFormat, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ImageParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final Map<Identifier, ImageRenderer.DataRecord> map = new HashMap<>();
   private int intVal = 4;
   private final Map<ImageRenderer.DataRecord2, TexturedQuadRenderState> map2 = new LinkedHashMap<>(32);
   private final List<TexturedQuadDrawData> list = new ArrayList<>(128);
   private final ByteBuffer byteBuffer2 = ByteBuffer.allocateDirect(49152).order(ByteOrder.nativeOrder());
   private DrawContext drawContext2;
   private GpuBuffer gpuBuffer2;
   private boolean flag;

   private static int computeInt(int local5087_0) {
      return local5087_0;
   }

   private Identifier getIdentifier(String local5088_1) {
      if (local5088_1 != null && !local5088_1.isBlank()) {
         String local5088_2 = local5088_1.trim().replace('\\', '/');
         if (local5088_2.startsWith("/")) {
            local5088_2 = local5088_2.substring(1);
         }

         if (local5088_2.startsWith("assets/")) {
            local5088_2 = local5088_2.substring("assets/".length());
            int local5088_3 = local5088_2.indexOf(47);
            if (local5088_3 >= 0) {
               String local5088_4 = local5088_2.substring(0, local5088_3);
               String local5088_5 = local5088_2.substring(local5088_3 + 1);
               return Identifier.of(local5088_4, local5088_5);
            }
         }

         if (local5088_2.indexOf(58) >= 0) {
            return Identifier.tryParse(local5088_2);
         }

         if (!local5088_2.startsWith("images/")) {
            local5088_2 = "images/" + local5088_2;
         }

         if (!local5088_2.contains(".")) {
            local5088_2 = local5088_2 + ".png";
         }

         return Identifier.of("greed", local5088_2);
      } else {
         return null;
      }
   }

   private int computeInt2() {
      return this.list.size() * 6 * 4 * 4;
   }

   public boolean matchesCriteria(RenderPipeline renderPipeline) {
      return renderPipeline == pipeline2 || renderPipeline == pipeline3;
   }

   private GpuBuffer getGpuBuffer() {
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed() && this.gpuBuffer2.size() >= 49152L) {
         return this.gpuBuffer2;
      }

      this.updateState4();

      try {
         this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_image_params", 136, 49152L);
         return this.gpuBuffer2;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private static float computeFloat(float local5092_0, float local5092_1, float local5092_2) {
      return Math.max(local5092_1, Math.min(local5092_2, local5092_0));
   }

   private static float computeFloat2(float local5093_0) {
      return !Float.isFinite(local5093_0) ? 0.0F : Math.max(0.001F, local5093_0);
   }

   private static void processData(ByteBuffer byteBuffer, int local5094_1, int local5094_2) {
      byteBuffer.putFloat(local5094_1, (local5094_2 >>> 16 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local5094_1 + 4, (local5094_2 >>> 8 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local5094_1 + 8, (local5094_2 & 0xFF) / 255.0F);
      byteBuffer.putFloat(local5094_1 + 12, (local5094_2 >>> 24 & 0xFF) / 255.0F);
   }

   public static void updateState() {
      ImageRenderer currentImageRenderer = imageRenderer;
      if (currentImageRenderer != null) {
         currentImageRenderer.close();
         imageRenderer = null;
      }
   }

   public void updateState2() {
      if (!this.list.isEmpty() && this.flag) {
         GpuBuffer gpuBuffer = this.getGpuBuffer();
         if (gpuBuffer != null) {
            try {
               ByteBuffer byteBuffer = this.getByteBuffer();
               RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, byteBuffer.remaining()), byteBuffer);
               this.flag = false;
            } catch (RuntimeException exception) {
               this.flag = true;
            }
         }
      }
   }

   private ImageRenderer() {
   }

   public boolean matchesCriteria2(String local5098_1) {
      return this.getCachedTexture(local5098_1) != null;
   }

   @Override
   public void close() {
      this.map2.clear();
      this.list.clear();
      this.map.clear();
      this.drawContext2 = null;
      this.updateState4();
   }

   public void updateState3() {
      this.intVal = 4;
      this.list.clear();
      this.flag = false;
   }

   private GpuBuffer getGpuBuffer2() {
      if (!this.flag && this.gpuBuffer2 != null) {
         return this.gpuBuffer2;
      }

      this.updateState2();
      return !this.flag && this.gpuBuffer2 != null ? this.gpuBuffer2 : null;
   }

   private ByteBuffer getByteBuffer() {
      ByteBuffer byteBuffer = this.byteBuffer2;
      byteBuffer.clear();

      for (int index = 0; index < this.list.size(); index++) {
         TexturedQuadDrawData texturedQuadDrawData = this.list.get(index);
         int local5102_4 = index * 6 * 4 * 4;
         byteBuffer.putFloat(local5102_4, texturedQuadDrawData.radiusTL());
         byteBuffer.putFloat(local5102_4 + 4, texturedQuadDrawData.radiusTR());
         byteBuffer.putFloat(local5102_4 + 8, texturedQuadDrawData.radiusBR());
         byteBuffer.putFloat(local5102_4 + 12, texturedQuadDrawData.radiusBL());
         byteBuffer.putFloat(local5102_4 + 16, texturedQuadDrawData.width());
         byteBuffer.putFloat(local5102_4 + 20, texturedQuadDrawData.height());
         byteBuffer.putFloat(local5102_4 + 24, texturedQuadDrawData.smoothness());
         byteBuffer.putFloat(local5102_4 + 28, 0.0F);
         processData(byteBuffer, local5102_4 + 32, texturedQuadDrawData.colorTopLeft());
         processData(byteBuffer, local5102_4 + 48, texturedQuadDrawData.colorTopRight());
         processData(byteBuffer, local5102_4 + 64, texturedQuadDrawData.colorBottomRight());
         processData(byteBuffer, local5102_4 + 80, texturedQuadDrawData.colorBottomLeft());
      }

      byteBuffer.limit(this.computeInt2());
      byteBuffer.position(0);
      return byteBuffer;
   }

   public static ImageRenderer getImageRenderer() {
      ImageRenderer currentImageRenderer = imageRenderer;
      if (currentImageRenderer == null) {
         synchronized (ImageRenderer.class) {
            currentImageRenderer = imageRenderer;
            if (currentImageRenderer == null) {
               currentImageRenderer = new ImageRenderer();
               imageRenderer = currentImageRenderer;
            }
         }
      }

      return currentImageRenderer;
   }

   int computeInt3(TexturedQuadDrawData texturedQuadDrawData) {
      int local5105_2 = this.list.size();
      if (local5105_2 == 512) {
         return -1;
      }

      this.list.add(texturedQuadDrawData);
      this.flag = true;
      return local5105_2;
   }

   private void renderElement(DrawContext drawContext, ImageDrawData imageDrawData) {
      if (drawContext != null && imageDrawData != null && imageDrawData.visible()) {
         CachedTexture cachedTexture = this.getCachedTexture(imageDrawData.texture());
         if (cachedTexture != null) {
            float local5106_4 = imageDrawData.explicitWidth() > 0.0F ? imageDrawData.explicitWidth() : cachedTexture.drawWidth(imageDrawData.size());
            float local5106_5 = imageDrawData.explicitHeight() > 0.0F ? imageDrawData.explicitHeight() : cachedTexture.drawHeight(imageDrawData.size());
            if (!(local5106_4 <= 0.0F) && !(local5106_5 <= 0.0F)) {
               float local5106_6 = Math.max(0.0F, Math.min(local5106_4, local5106_5) * 0.5F);
               TexturedQuadDrawData texturedQuadDrawData = new TexturedQuadDrawData(
                  imageDrawData.x(),
                  imageDrawData.y(),
                  local5106_4,
                  local5106_5,
                  computeFloat(imageDrawData.radiusTL(), 0.0F, local5106_6),
                  computeFloat(imageDrawData.radiusTR(), 0.0F, local5106_6),
                  computeFloat(imageDrawData.radiusBR(), 0.0F, local5106_6),
                  computeFloat(imageDrawData.radiusBL(), 0.0F, local5106_6),
                  computeFloat2(imageDrawData.smoothness()),
                  computeInt(imageDrawData.colorTopLeft()),
                  computeInt(imageDrawData.colorTopRight()),
                  computeInt(imageDrawData.colorBottomRight()),
                  computeInt(imageDrawData.colorBottomLeft()),
                  computeFloat(imageDrawData.u0(), 0.0F, 1.0F),
                  computeFloat(imageDrawData.v0(), 0.0F, 1.0F),
                  computeFloat(imageDrawData.u1(), 0.0F, 1.0F),
                  computeFloat(imageDrawData.v1(), 0.0F, 1.0F),
                  Float.isFinite(imageDrawData.rotationDegrees()) ? imageDrawData.rotationDegrees() : 0.0F,
                  Float.isFinite(imageDrawData.rotationOriginX()) ? imageDrawData.rotationOriginX() : 0.0F,
                  Float.isFinite(imageDrawData.rotationOriginY()) ? imageDrawData.rotationOriginY() : 0.0F
               );

               try {
                  GuiRenderState guiRenderState = ((DrawContextMixin)drawContext).getGuiRenderState();
                  int currentLayerSerial = ((GuiLayerSerialAccess)guiRenderState).getLayerSerial();
                  Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
                  ScreenRect screenRect = ScissorStack.getScreenRect();
                  ImageRenderer.DataRecord2 local5106_12 = new ImageRenderer.DataRecord2(
                     guiRenderState,
                     currentLayerSerial,
                     cachedTexture.id(),
                     imageDrawData.nearest()
                        && Math.abs(texturedQuadDrawData.u1() - texturedQuadDrawData.u0()) * cachedTexture.width()
                           <= local5106_4 * GuiScale.computeFloat() * matrix3x2f.m00()
                        && Math.abs(texturedQuadDrawData.v1() - texturedQuadDrawData.v0()) * cachedTexture.height()
                           <= local5106_5 * GuiScale.computeFloat() * matrix3x2f.m00(),
                     ImageRenderer.DataRecord3.of(matrix3x2f),
                     screenRect,
                     imageDrawData.glow()
                  );
                  TexturedQuadRenderState texturedQuadRenderState = this.map2.get(local5106_12);
                  if (texturedQuadRenderState == null) {
                     texturedQuadRenderState = new TexturedQuadRenderState(matrix3x2f, cachedTexture, screenRect, local5106_12.nearest(), imageDrawData.glow());
                     texturedQuadRenderState.add(texturedQuadDrawData);
                     this.map2.put(local5106_12, texturedQuadRenderState);
                     guiRenderState.addSimpleElement(texturedQuadRenderState);
                  } else {
                     texturedQuadRenderState.add(texturedQuadDrawData);
                  }
               } catch (RuntimeException exception) {
                  GreedClient.logger().warn("[ImageRenderer] Failed to submit image: {}", imageDrawData.texture(), exception);
               }
            }
         }
      }
   }

   private static Identifier getIdentifier2(String local5107_0) {
      return Identifier.of("greed", local5107_0);
   }

   private void updateState4() {
      if (this.gpuBuffer2 != null) {
         this.gpuBuffer2.close();
         this.gpuBuffer2 = null;
      }
   }

   public void updateState5() {
      this.map.clear();
   }

   private CachedTexture getCachedTexture(String local5110_1) {
      Identifier identifier = this.getIdentifier(local5110_1);
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

      if (this.intVal <= 0 && !this.map.containsKey(identifier)) {
         return null;
      }

      AbstractTexture currentTexture = textureManager.getTexture(identifier);
      if (currentTexture != null && currentTexture.getGlTextureView() != null && currentTexture.getGlTexture() != null) {
         ImageRenderer.DataRecord local5110_6 = this.map.get(identifier);
         if (local5110_6 != null && local5110_6.texture() == currentTexture) {
            return local5110_6.value();
         }

         this.intVal--;
         int local5110_7 = Math.max(1, currentTexture.getGlTexture().getWidth(0));
         int local5110_8 = Math.max(1, currentTexture.getGlTexture().getHeight(0));
         TextureSetup textureSetup = TextureSetup.of(currentTexture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
         TextureSetup textureSetup2 = TextureSetup.of(currentTexture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
         CachedTexture cachedTexture = new CachedTexture(identifier, textureSetup, textureSetup2, local5110_7, local5110_8);
         this.map.put(identifier, new ImageRenderer.DataRecord(currentTexture, cachedTexture));
         return cachedTexture;
      } else {
         return null;
      }
   }

   public void updateState6() {
      this.map2.clear();
   }

   public void renderElement2(DrawContext drawContext) {
      if (this.drawContext2 != drawContext) {
         this.map2.clear();
      }

      this.drawContext2 = drawContext;
   }

   public void updateState7() {
      this.drawContext2 = null;
      this.map2.clear();
   }

   public void processData2(ImageDrawData imageDrawData) {
      this.renderElement(this.drawContext2, imageDrawData);
   }

   public void processData3(RenderPass renderPass) {
      if (renderPass != null && !this.list.isEmpty()) {
         GpuBuffer gpuBuffer = this.getGpuBuffer2();
         if (gpuBuffer != null) {
            renderPass.setUniform("ImageParamsArray", gpuBuffer);
         }
      }
   }

   private record DataRecord(AbstractTexture texture, CachedTexture value) {
   }

   private record DataRecord2(
      GuiRenderState state, int layerSerial, Identifier texture, boolean nearest, ImageRenderer.DataRecord3 pose, ScreenRect scissorArea, boolean glow
   ) {
   }

   private record DataRecord3(float m00, float m01, float m10, float m11, float m20, float m21) {
      static ImageRenderer.DataRecord3 of(Matrix3x2f matrix3x2f) {
         return new ImageRenderer.DataRecord3(
            matrix3x2f.m00(), matrix3x2f.m01(), matrix3x2f.m10(), matrix3x2f.m11(), matrix3x2f.m20(), matrix3x2f.m21()
         );
      }
   }
}
