package greed.client.render;

import greed.client.GreedClient;
import greed.mixin.DrawContextMixin;
import greed.client.render.RasterTextDrawData;
import greed.client.util.GuiScale;
import greed.client.render.RasterGlyphCache;
import greed.client.render.GuiLayerSerialAccess;
import greed.client.render.RasterGlyphQuad;
import greed.client.render.ScissorStack;
import greed.client.render.RasterTextLayout;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

public final class TextAtlasRenderer implements AutoCloseable {
   private static volatile TextAtlasRenderer textAtlasRenderer;
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/text_atlas"))
      .withVertexShader(getIdentifier("core/text_atlas"))
      .withFragmentShader(getIdentifier("core/text_atlas"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .build();
   public static final RenderPipeline pipeline3 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/text_atlas_fade"))
      .withVertexShader(getIdentifier("core/text_atlas_fade"))
      .withFragmentShader(getIdentifier("core/text_atlas_fade"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .build();
   private final RasterFontAtlas rasterFontAtlas = new RasterFontAtlas();
   private final Map<TextAtlasRenderer.DataRecord5, RasterGlyphRenderState> map = new LinkedHashMap<>(64);
   private DrawContext drawContext2;

   private RasterTextDrawData getRasterTextDrawData(RasterTextDrawData rasterTextDrawData) {
      return new RasterTextDrawData(
         rasterTextDrawData.fontName(),
         rasterTextDrawData.text(),
         rasterTextDrawData.x(),
         rasterTextDrawData.y(),
         rasterTextDrawData.size(),
         computeInt(rasterTextDrawData.colorTopLeft()),
         computeInt(rasterTextDrawData.colorTopRight()),
         computeInt(rasterTextDrawData.colorBottomRight()),
         computeInt(rasterTextDrawData.colorBottomLeft()),
         rasterTextDrawData.rotationDegrees(),
         rasterTextDrawData.rotationOriginX(),
         rasterTextDrawData.rotationOriginY(),
         rasterTextDrawData.fadeLeft(),
         rasterTextDrawData.fadeRight(),
         rasterTextDrawData.fadeLeftX(),
         rasterTextDrawData.fadeRightX(),
         rasterTextDrawData.fadeWidth(),
         rasterTextDrawData.fadeLeftStrength(),
         rasterTextDrawData.fadeRightStrength(),
         rasterTextDrawData.snapOrigin()
      );
   }

   private static boolean matchesCriteria(int local4775_0) {
      return false;
   }

   public float computeFloat(String local4776_1, String local4776_2, float local4776_3) {
      return local4776_2 != null && !local4776_2.isEmpty() ? this.rasterFontAtlas.getRasterGlyphCache(local4776_1, local4776_3).computeFloat2(local4776_2) : 0.0F;
   }

   private static int computeInt(int local4777_0) {
      return matchesCriteria(local4777_0) ? local4777_0 | 0xFF000000 : local4777_0;
   }

   private boolean shouldWarpText(RasterTextDrawData rasterTextDrawData) {
      return matchesCriteria(rasterTextDrawData.colorTopLeft())
         || matchesCriteria(rasterTextDrawData.colorTopRight())
         || matchesCriteria(rasterTextDrawData.colorBottomRight())
         || matchesCriteria(rasterTextDrawData.colorBottomLeft());
   }

   private static Identifier getIdentifier(String local4779_0) {
      return Identifier.of("greed", local4779_0);
   }

   public void renderElement(DrawContext drawContext, RasterTextDrawData rasterTextDrawData) {
      this.renderElement3(drawContext);
      this.processData(rasterTextDrawData);
      this.updateState();
   }

   public void updateState() {
      this.drawContext2 = null;
      this.map.clear();
   }

   public void processData(RasterTextDrawData rasterTextDrawData) {
      this.renderElement2(this.drawContext2, rasterTextDrawData);
   }

   private void renderElement2(DrawContext drawContext, RasterTextDrawData rasterTextDrawData) {
      if (drawContext != null && rasterTextDrawData != null) {
         RasterTextDrawData rasterTextDrawData2 = this.shouldWarpText(rasterTextDrawData) ? this.getRasterTextDrawData(rasterTextDrawData) : rasterTextDrawData;
         if (rasterTextDrawData2.visible()) {
            try {
               RasterGlyphCache rasterGlyphCache = this.rasterFontAtlas.getRasterGlyphCache(rasterTextDrawData2.fontName(), rasterTextDrawData2.size());
               Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
               RasterTextLayout rasterTextLayout = rasterGlyphCache.getRasterTextLayout2(rasterTextDrawData2.text());
               if (rasterTextLayout.checkState()) {
                  return;
               }

               rasterGlyphCache.updateState();
               GuiRenderState guiRenderState = ((DrawContextMixin)drawContext).getGuiRenderState();
               int currentLayerSerial = ((GuiLayerSerialAccess)guiRenderState).getLayerSerial();
               TextAtlasRenderer.DataRecord local4783_9 = TextAtlasRenderer.DataRecord.of(matrix3x2f);
               ScreenRect screenRect = ScissorStack.getScreenRect();
               RenderPipeline renderPipeline = rasterTextDrawData2.hasHorizontalFade() ? pipeline3 : pipeline2;

               for (RasterTextLayout.DataRecord local4783_13 : rasterTextLayout.getList()) {
                  if (!local4783_13.glyphs().isEmpty()) {
                     TextAtlasRenderer.DataRecord5 local4783_14 = new TextAtlasRenderer.DataRecord5(
                        guiRenderState, currentLayerSerial, local4783_13.page(), local4783_9, screenRect, renderPipeline
                     );
                     RasterGlyphRenderState rasterGlyphRenderState = this.map.get(local4783_14);
                     if (rasterGlyphRenderState == null) {
                        rasterGlyphRenderState = new RasterGlyphRenderState(matrix3x2f, local4783_13.page(), screenRect, renderPipeline);
                        this.map.put(local4783_14, rasterGlyphRenderState);
                        guiRenderState.addPreparedTextElement(rasterGlyphRenderState);
                     }

                     rasterGlyphRenderState.add(
                        local4783_13.glyphs(),
                        rasterTextDrawData2.x(),
                        rasterTextDrawData2.y(),
                        rasterTextDrawData2.colorTopLeft(),
                        rasterTextDrawData2.colorTopRight(),
                        rasterTextDrawData2.colorBottomRight(),
                        rasterTextDrawData2.colorBottomLeft(),
                        rasterTextDrawData2.rotationDegrees(),
                        rasterTextDrawData2.rotationOriginX(),
                        rasterTextDrawData2.rotationOriginY(),
                        rasterTextDrawData2.fadeLeft(),
                        rasterTextDrawData2.fadeRight(),
                        rasterTextDrawData2.fadeLeftX(),
                        rasterTextDrawData2.fadeRightX(),
                        rasterTextDrawData2.fadeWidth(),
                        rasterTextDrawData2.fadeLeftStrength(),
                        rasterTextDrawData2.fadeRightStrength(),
                        rasterTextDrawData2.snapOrigin()
                     );
                  }
               }
            } catch (RuntimeException exception) {
               GreedClient.logger().warn("[FontRenderer] Failed to submit text: {}", rasterTextDrawData2.text(), exception);
            }
         }
      }
   }

   public void renderElement3(DrawContext drawContext) {
      if (this.drawContext2 != drawContext) {
         this.map.clear();
         this.rasterFontAtlas.updateState();
      }

      this.drawContext2 = drawContext;
   }

   private TextAtlasRenderer() {
   }

   public TextAtlasRenderer.DataRecord2 getNested0465(String local4786_1, String local4786_2, float local4786_3) {
      if (local4786_2 != null && !local4786_2.isEmpty() && !(local4786_3 <= 0.0F)) {
         try {
            RasterGlyphCache rasterGlyphCache = this.rasterFontAtlas.getRasterGlyphCache(local4786_1, local4786_3);
            RasterTextLayout rasterTextLayout = rasterGlyphCache.getRasterTextLayout2(local4786_2);
            if (rasterTextLayout.checkState()) {
               return TextAtlasRenderer.DataRecord2.EMPTY;
            }

            rasterGlyphCache.updateState();
            List<TextAtlasRenderer.DataRecord4> local4786_6 = new ArrayList<>(rasterTextLayout.getList().size());

            for (RasterTextLayout.DataRecord local4786_8 : rasterTextLayout.getList()) {
               List<TextAtlasRenderer.DataRecord3> local4786_9 = new ArrayList<>(local4786_8.glyphs().size());

               for (RasterGlyphQuad rasterGlyphQuad : local4786_8.glyphs()) {
                  local4786_9.add(
                     new TextAtlasRenderer.DataRecord3(
                        rasterGlyphQuad.x0(),
                        rasterGlyphQuad.y0(),
                        rasterGlyphQuad.x1(),
                        rasterGlyphQuad.y1(),
                        rasterGlyphQuad.u0(),
                        rasterGlyphQuad.v0(),
                        rasterGlyphQuad.u1(),
                        rasterGlyphQuad.v1()
                     )
                  );
               }

               local4786_6.add(new TextAtlasRenderer.DataRecord4(local4786_8.page().getTextureSetup(), local4786_9));
            }

            return new TextAtlasRenderer.DataRecord2(local4786_6, rasterTextLayout.computeFloat2(), rasterTextLayout.computeFloat());
         } catch (RuntimeException exception) {
            GreedClient.logger().warn("[FontRenderer] Failed to layout glyph pattern: {}", local4786_2, exception);
            return TextAtlasRenderer.DataRecord2.EMPTY;
         }
      } else {
         return TextAtlasRenderer.DataRecord2.EMPTY;
      }
   }

   public static TextAtlasRenderer getTextAtlasRenderer() {
      TextAtlasRenderer currentTextAtlasRenderer = textAtlasRenderer;
      if (currentTextAtlasRenderer == null) {
         synchronized (TextAtlasRenderer.class) {
            currentTextAtlasRenderer = textAtlasRenderer;
            if (currentTextAtlasRenderer == null) {
               currentTextAtlasRenderer = new TextAtlasRenderer();
               textAtlasRenderer = currentTextAtlasRenderer;
            }
         }
      }

      return currentTextAtlasRenderer;
   }

   public static void updateState2() {
      TextAtlasRenderer currentTextAtlasRenderer = textAtlasRenderer;
      if (currentTextAtlasRenderer != null) {
         currentTextAtlasRenderer.close();
         textAtlasRenderer = null;
      }
   }

   @Override
   public void close() {
      this.drawContext2 = null;
      this.map.clear();
      this.rasterFontAtlas.close();
   }

   private record DataRecord(float m00, float m01, float m10, float m11, float m20, float m21) {
      static TextAtlasRenderer.DataRecord of(Matrix3x2f matrix3x2f) {
         return new TextAtlasRenderer.DataRecord(
            matrix3x2f.m00(), matrix3x2f.m01(), matrix3x2f.m10(), matrix3x2f.m11(), matrix3x2f.m20(), matrix3x2f.m21()
         );
      }
   }

   public record DataRecord2(List<TextAtlasRenderer.DataRecord4> pages, float width, float height) {
      public static final TextAtlasRenderer.DataRecord2 EMPTY = new TextAtlasRenderer.DataRecord2(List.of(), 0.0F, 0.0F);

      public boolean empty() {
         return this.pages.isEmpty();
      }

      public DataRecord2 {
         pages = List.copyOf(pages);
      }
   }

   public record DataRecord3(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1) {
   }

   public record DataRecord4(TextureSetup textureSetup, List<TextAtlasRenderer.DataRecord3> glyphs) {
      public DataRecord4 {
         glyphs = List.copyOf(glyphs);
      }
   }

   private record DataRecord5(
      GuiRenderState state, int layerSerial, FontTexture page, TextAtlasRenderer.DataRecord pose, ScreenRect scissorArea, RenderPipeline pipeline
   ) {
   }
}
