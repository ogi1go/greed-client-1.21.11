package greed.client.render;

import greed.client.GreedClient;
import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.GuiLayerSerialAccess;
import greed.client.render.IconDrawData;
import greed.client.render.TextureReference;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

public final class EffectIconRenderer implements AutoCloseable {
   private static volatile EffectIconRenderer effectIconRenderer;
   private static final VertexFormat vertexFormat = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .build();
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/effect_icon"))
      .withVertexShader(getIdentifier("core/effect_icon"))
      .withFragmentShader(getIdentifier("core/effect_icon"))
      .withVertexFormat(vertexFormat, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .build();
   private final Map<Identifier, EffectIconRenderer.DataRecord> map = new HashMap<>();
   private final Map<EffectIconRenderer.DataRecord3, IconRenderState> map2 = new LinkedHashMap<>(16);
   private DrawContext drawContext2;

   public void renderElement(DrawContext drawContext) {
      if (this.drawContext2 != drawContext) {
         this.map2.clear();
      }

      this.drawContext2 = drawContext;
   }

   public void updateState() {
      this.map.clear();
   }

   public static EffectIconRenderer getEffectIconRenderer() {
      EffectIconRenderer currentEffectIconRenderer = effectIconRenderer;
      if (currentEffectIconRenderer == null) {
         synchronized (EffectIconRenderer.class) {
            currentEffectIconRenderer = effectIconRenderer;
            if (currentEffectIconRenderer == null) {
               currentEffectIconRenderer = new EffectIconRenderer();
               effectIconRenderer = currentEffectIconRenderer;
            }
         }
      }

      return currentEffectIconRenderer;
   }

   @Override
   public void close() {
      this.map2.clear();
      this.map.clear();
      this.drawContext2 = null;
   }

   public void updateState2() {
      this.drawContext2 = null;
      this.map2.clear();
   }

   private EffectIconRenderer() {
   }

   public void processData(EffectIconDraw effectIconDraw) {
      this.renderElement2(this.drawContext2, effectIconDraw);
   }

   private TextureReference getTextureReference(Identifier identifier) {
      if (identifier == null) {
         return null;
      }

      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null && minecraftClient.getTextureManager() != null) {
         AbstractTexture currentTexture = minecraftClient.getTextureManager().getTexture(identifier);
         if (currentTexture != null && currentTexture.getGlTextureView() != null) {
            EffectIconRenderer.DataRecord local1964_4 = this.map.get(identifier);
            if (local1964_4 != null && local1964_4.texture() == currentTexture) {
               return local1964_4.value();
            }

            TextureSetup textureSetup = TextureSetup.of(currentTexture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            TextureReference textureReference = new TextureReference(identifier, textureSetup);
            this.map.put(identifier, new EffectIconRenderer.DataRecord(currentTexture, textureReference));
            return textureReference;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public void updateState3() {
      this.map2.clear();
   }

   private static int computeInt(int local1966_0) {
      return local1966_0;
   }

   public static void updateState4() {
      EffectIconRenderer currentEffectIconRenderer = effectIconRenderer;
      if (currentEffectIconRenderer != null) {
         currentEffectIconRenderer.close();
         effectIconRenderer = null;
      }
   }

   private Sprite getSprite2(EffectIconDraw effectIconDraw) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null && effectIconDraw.effect() != null) {
         SpriteAtlasTexture atlasTexture = minecraftClient.getAtlasManager().getAtlasTexture(Atlases.GUI);
         Identifier identifier = InGameHud.getEffectTexture(effectIconDraw.effect());
         return atlasTexture.getSprite(identifier);
      } else {
         return null;
      }
   }

   private void renderElement2(DrawContext drawContext, EffectIconDraw effectIconDraw) {
      if (drawContext != null && effectIconDraw != null && effectIconDraw.visible()) {
         Sprite sprite = this.getSprite2(effectIconDraw);
         if (sprite != null) {
            TextureReference textureReference = this.getTextureReference(sprite.getAtlasId());
            if (textureReference != null) {
               IconDrawData iconDrawData = new IconDrawData(
                  effectIconDraw.x(),
                  effectIconDraw.y(),
                  effectIconDraw.size(),
                  sprite.getMinU(),
                  sprite.getMinV(),
                  sprite.getMaxU(),
                  sprite.getMaxV(),
                  computeInt(effectIconDraw.color())
               );

               try {
                  GuiRenderState guiRenderState = ((DrawContextMixin)drawContext).getGuiRenderState();
                  int currentLayerSerial = ((GuiLayerSerialAccess)guiRenderState).getLayerSerial();
                  Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
                  IconRenderState iconRenderState = new IconRenderState(matrix3x2f, textureReference);
                  iconRenderState.add(iconDrawData);
                  guiRenderState.addSimpleElement(iconRenderState);
               } catch (RuntimeException exception) {
                  GreedClient.logger().warn("[EffectIconRenderer] Failed to submit effect icon", exception);
               }
            }
         }
      }
   }

   private static Identifier getIdentifier(String local1971_0) {
      return Identifier.of("greed", local1971_0);
   }

   private record DataRecord(AbstractTexture texture, TextureReference value) {
   }

   private record DataRecord2(float m00, float m01, float m10, float m11, float m20, float m21) {
      static EffectIconRenderer.DataRecord2 of(Matrix3x2f matrix3x2f) {
         return new EffectIconRenderer.DataRecord2(
            matrix3x2f.m00(), matrix3x2f.m01(), matrix3x2f.m10(), matrix3x2f.m11(), matrix3x2f.m20(), matrix3x2f.m21()
         );
      }
   }

   private record DataRecord3(GuiRenderState state, int layerSerial, Identifier texture, EffectIconRenderer.DataRecord2 pose) {
   }
}
