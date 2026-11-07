package greed.client.render;

import greed.client.GreedClient;
import greed.mixin.DrawContextMixin;
import greed.client.util.GuiScale;
import greed.client.render.GuiLayerSerialAccess;
import greed.client.render.ScissorStack;
import greed.client.render.MsdfTextDrawData;
import greed.client.render.MsdfGlyph;
import greed.client.render.RoundedClipStack;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

public final class MsdfTextRenderer implements AutoCloseable {
   private static volatile MsdfTextRenderer msdfTextRenderer;
   private static final VertexFormat vertexFormat = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   private static final VertexFormat vertexFormat2 = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .add("Color", VertexFormatElement.COLOR)
      .add("LineWidth", VertexFormatElement.LINE_WIDTH)
      .build();
   public static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/msdf_text"))
      .withVertexShader(getIdentifier("core/msdf_text"))
      .withFragmentShader(getIdentifier("core/msdf_text"))
      .withVertexFormat(vertexFormat, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withSampler("Sampler0")
      .build();
   public static final RenderPipeline pipeline3 = RenderPipeline.builder()
      .withLocation(getIdentifier("pipeline/msdf_shimmer"))
      .withVertexShader(getIdentifier("core/msdf_shimmer"))
      .withFragmentShader(getIdentifier("core/msdf_shimmer"))
      .withVertexFormat(vertexFormat2, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withSampler("Sampler0")
      .build();
   private final Map<MsdfTextRenderer.DataRecord3, MsdfTextRenderState> msdfStateCache = new LinkedHashMap<>(32);
   private final Map<MsdfTextRenderer.DataRecord4, RasterTextRenderState> rasterStateCache = new LinkedHashMap<>(32);
   private final Map<Identifier, MsdfTextRenderer.DataRecord5> textureSetupCache = new HashMap<>();
   private final SmallTextAtlasBaker smallTextAtlasBaker = new SmallTextAtlasBaker();
   private DrawContext drawContext2;
   private boolean disableSmallText;
   private int warmupIndex;

   public float computeFloat(String fontName, String text, float size) {
      FontAtlas fontAtlas = FontRegistry.getAtlas(fontName);
      return fontAtlas == null ? 0.0F : fontAtlas.computeFloat7(text, size);
   }

   public void updateState() {
      this.smallTextAtlasBaker.updateState3();
      this.drawContext2 = null;
      this.msdfStateCache.clear();
      this.rasterStateCache.clear();
   }

   public boolean checkState() {
      String[] stringArray = FontRegistry.getWarmupNames();
      if (this.warmupIndex >= stringArray.length) {
         return false;
      }

      FontAtlas fontAtlas = FontRegistry.getAtlas(stringArray[this.warmupIndex++]);
      if (fontAtlas != null) {
         this.smallTextAtlasBaker.matchesCriteria5(fontAtlas);
      }

      return true;
   }

   public static void updateState2() {
      MsdfTextRenderer currentMsdfTextRenderer = msdfTextRenderer;
      if (currentMsdfTextRenderer != null) {
         currentMsdfTextRenderer.close();
         msdfTextRenderer = null;
      }
   }

   public void updateState3() {
      this.smallTextAtlasBaker.updateState4();
   }

   private static int toAlphaByte(float colorValue) {
      return Math.round(clamp01(colorValue) * 255.0F);
   }

   private int applyHorizontalFade(int color, float x, MsdfTextDrawData msdfTextDrawData) {
      if (!msdfTextDrawData.hasHorizontalFade()) {
         return color;
      }

      float alpha = 1.0F;
      if (msdfTextDrawData.fadeLeftStrength() > 0.001F) {
         float fadeLeftFactor = smoothstep(clamp01((x - msdfTextDrawData.fadeLeftX()) / msdfTextDrawData.fadeWidth()));
         alpha = Math.min(alpha, 1.0F - msdfTextDrawData.fadeLeftStrength() * (1.0F - fadeLeftFactor));
      }

      if (msdfTextDrawData.fadeRightStrength() > 0.001F) {
         float fadeRightFactor = smoothstep(clamp01((msdfTextDrawData.fadeRightX() - x) / msdfTextDrawData.fadeWidth()));
         alpha = Math.min(alpha, 1.0F - msdfTextDrawData.fadeRightStrength() * (1.0F - fadeRightFactor));
      }

      int alphaByte = color >>> 24 & 0xFF;
      return Math.round(alphaByte * alpha) << 24 | color & 16777215;
   }

   public void processData(MsdfTextDrawData msdfTextDrawData) {
      this.submitText(this.drawContext2, msdfTextDrawData, false, 0.0F, 0.0F, 0.0F, 0.0F);
   }

   private static int applyAlphaToColor(int color2, float alpha2) {
      if (alpha2 >= 0.999F) {
         return color2;
      }

      if (alpha2 <= 0.001F) {
         return color2 & 16777215;
      }

      int outAlpha = Math.round((color2 >>> 24 & 0xFF) * alpha2);
      return outAlpha << 24 | color2 & 16777215;
   }

   public float[] computeFloat2(String fontName2, String text2, float size2) {
      FontAtlas fontAtlas = FontRegistry.getAtlas(fontName2);
      return fontAtlas == null ? new float[]{0.0F, 0.0F} : fontAtlas.computeFloat(text2, size2);
   }

   private boolean tryRenderSmallText(
      GuiRenderState guiRenderState,
      int layerSerial,
      Matrix3x2f matrix3x2f,
      ScreenRect screenRect,
      FontAtlas fontAtlas,
      MsdfTextDrawData msdfTextDrawData
   ) {
      float size3 = msdfTextDrawData.size();
      float atlasSize = SmallTextAtlasBaker.computeFloat5(size3);
      float guiScale = GuiScale.computeFloat();
      int guiScaleInt = GuiScale.computeInt();
      float offsetX = matrix3x2f.m20() * guiScaleInt;
      float offsetY = matrix3x2f.m21() * guiScaleInt;
      float startX = msdfTextDrawData.x();
      float cursorX = startX;
      int prevCodePoint = -1;
      String text3 = msdfTextDrawData.text();
      int index = 0;

      while (index < text3.length()) {
         int codePoint = text3.codePointAt(index);
         index += Character.charCount(codePoint);
         if (codePoint != 10) {
            boolean useSmallAtlas = !fontAtlas.matchesCriteria(codePoint) && SmallTextAtlasBaker.matchesCriteria(codePoint);
            MsdfGlyph msdfGlyph = fontAtlas.getMsdfGlyph(codePoint);
            if (prevCodePoint != -1) {
               cursorX += fontAtlas.computeFloat2(prevCodePoint, codePoint) * size3;
            }

            if (msdfGlyph.drawable() || useSmallAtlas) {
               float screenX = cursorX * guiScale + offsetX;
               int subpixel = Math.round((screenX - (int)Math.floor(screenX)) * 4.0F);
               if (subpixel == 4) {
                  subpixel = 0;
               }

               if (useSmallAtlas
                  ? !this.smallTextAtlasBaker.matchesCriteria4(codePoint, atlasSize, subpixel)
                  : !this.smallTextAtlasBaker.matchesCriteria3(fontAtlas, codePoint, msdfGlyph, atlasSize, subpixel)) {
                  return false;
               }
            }

            cursorX += (useSmallAtlas ? SmallTextAtlasBaker.computeFloat3(codePoint) : msdfGlyph.advance()) * size3;
            prevCodePoint = codePoint;
         } else {
            cursorX = startX;
            prevCodePoint = -1;
         }
      }

      float baselineY = msdfTextDrawData.y() + fontAtlas.computeFloat5(size3);
      float lineHeight = fontAtlas.computeFloat3(size3);
      MsdfTextRenderer.DataRecord dataRecord = MsdfTextRenderer.DataRecord.of(matrix3x2f);
      float cursorXSecond = startX;
      float lineY = Math.round(baselineY * guiScale + offsetY);
      int lineIndex = 0;
      int prevCodePoint2 = -1;
      String textSecond = msdfTextDrawData.text();
      int indexSecond = 0;

      while (indexSecond < textSecond.length()) {
         int codePoint2 = textSecond.codePointAt(indexSecond);
         indexSecond += Character.charCount(codePoint2);
         if (codePoint2 == 10) {
            cursorXSecond = startX;
            lineY = Math.round((baselineY + ++lineIndex * lineHeight) * guiScale + offsetY);
            prevCodePoint2 = -1;
         } else {
            boolean useSmallAtlas2 = !fontAtlas.matchesCriteria(codePoint2) && SmallTextAtlasBaker.matchesCriteria(codePoint2);
            MsdfGlyph msdfGlyph2 = fontAtlas.getMsdfGlyph(codePoint2);
            if (prevCodePoint2 != -1) {
               cursorXSecond += fontAtlas.computeFloat2(prevCodePoint2, codePoint2) * size3;
            }

            if (msdfGlyph2.drawable() || useSmallAtlas2) {
               float screenX2 = cursorXSecond * guiScale + offsetX;
               int floorX = (int)Math.floor(screenX2);
               int subpixel2 = Math.round((screenX2 - floorX) * 4.0F);
               if (subpixel2 == 4) {
                  subpixel2 = 0;
                  floorX++;
               }

               SmallTextAtlasBaker.AtlasGlyphKey glyphKey = useSmallAtlas2
                  ? this.smallTextAtlasBaker.getNested04963(codePoint2, atlasSize, subpixel2)
                  : this.smallTextAtlasBaker.getNested04965(fontAtlas, codePoint2, msdfGlyph2, atlasSize, subpixel2);
               if (glyphKey.drawable()) {
                  float penX = floorX;
                  float left = (penX + glyphKey.offsetX() - offsetX) / guiScale;
                  float top = (lineY - glyphKey.offsetY() - offsetY) / guiScale;
                  float right = left + glyphKey.width() / guiScale;
                  float bottom = top + glyphKey.height() / guiScale;
                  MsdfTextRenderer.DataRecord4 dataRecord4 = new MsdfTextRenderer.DataRecord4(
                     guiRenderState, layerSerial, glyphKey.page(), dataRecord, screenRect
                  );
                  RasterTextRenderState rasterTextRenderState = this.rasterStateCache.get(dataRecord4);
                  if (rasterTextRenderState == null) {
                     rasterTextRenderState = new RasterTextRenderState(matrix3x2f, glyphKey.page().getTextureSetup(), screenRect);
                     this.rasterStateCache.put(dataRecord4, rasterTextRenderState);
                     guiRenderState.addPreparedTextElement(rasterTextRenderState);
                  }

                  RasterTextRenderState rasterTextRenderState2 = rasterTextRenderState;
                  emitClippedQuad(
                     left,
                     top,
                     right,
                     bottom,
                     glyphKey.u0(),
                     glyphKey.v0(),
                     glyphKey.u1(),
                     glyphKey.v1(),
                     (quadLeft, quadTop, quadRight, quadBottom, u0, v0, u1, v1, fadeTop, fadeBottom) -> {
                        int colorTopLeft = applyAlphaToColor(this.applyHorizontalFade(msdfTextDrawData.colorTopLeft(), quadLeft, msdfTextDrawData), fadeTop);
                        int colorTopRight = applyAlphaToColor(this.applyHorizontalFade(msdfTextDrawData.colorTopRight(), quadRight, msdfTextDrawData), fadeTop);
                        int colorBottomRight = applyAlphaToColor(this.applyHorizontalFade(msdfTextDrawData.colorBottomRight(), quadRight, msdfTextDrawData), fadeBottom);
                        int colorBottomLeft = applyAlphaToColor(this.applyHorizontalFade(msdfTextDrawData.colorBottomLeft(), quadLeft, msdfTextDrawData), fadeBottom);
                        if ((colorTopLeft | colorTopRight | colorBottomRight | colorBottomLeft) >>> 24 != 0) {
                           rasterTextRenderState2.add(
                              quadLeft,
                              quadTop,
                              quadRight,
                              quadBottom,
                              u0,
                              v0,
                              u1,
                              v1,
                              colorTopLeft,
                              colorTopRight,
                              colorBottomRight,
                              colorBottomLeft
                           );
                        }
                     }
                  );
               }
            }

            cursorXSecond += (useSmallAtlas2 ? SmallTextAtlasBaker.computeFloat3(codePoint2) : msdfGlyph2.advance()) * size3;
            prevCodePoint2 = codePoint2;
         }
      }

      return true;
   }

   private TextureSetup getTextureSetup(Identifier identifier) {
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
         MsdfTextRenderer.DataRecord5 textureRecord = this.textureSetupCache.get(identifier);
         if (textureRecord != null && textureRecord.texture() == currentTexture) {
            return textureRecord.setup();
         }

         TextureSetup textureSetup = TextureSetup.of(currentTexture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
         this.textureSetupCache.put(identifier, new MsdfTextRenderer.DataRecord5(currentTexture, textureSetup));
         return textureSetup;
      } else {
         return null;
      }
   }

   private static int packArgb(float redValue, float greenValue, float blueValue, float alphaValue) {
      int alphaByte2 = toAlphaByte(alphaValue);
      int redByte = toAlphaByte((redValue + 0.5F) / 2.0F);
      int greenByte = toAlphaByte(greenValue);
      int blueByte = toAlphaByte(blueValue);
      return alphaByte2 << 24 | redByte << 16 | greenByte << 8 | blueByte;
   }

   private void renderGlyphRun(
      MsdfTextRenderState msdfTextRenderState,
      GuiRenderState guiRenderState,
      int layerSerial2,
      Matrix3x2f matrix3x2f,
      ScreenRect screenRect,
      FontAtlas fontAtlas,
      MsdfTextDrawData msdfTextDrawData,
      boolean shimmer,
      int shimmerColor
   ) {
      float size4 = msdfTextDrawData.size();
      float startX2 = msdfTextDrawData.x();
      float baselineY2 = msdfTextDrawData.y() + fontAtlas.computeFloat5(size4);
      float lineHeight2 = fontAtlas.computeFloat3(size4);
      float[] floatArray = this.measureLineWidths(fontAtlas, msdfTextDrawData.text(), size4);
      float cursorX2 = startX2;
      float cursorY = baselineY2;
      int lineIndex2 = 0;
      int prevCodePoint3 = -1;
      String text4 = msdfTextDrawData.text();
      int index2 = 0;

      while (index2 < text4.length()) {
         int codePoint3 = text4.codePointAt(index2);
         index2 += Character.charCount(codePoint3);
         if (codePoint3 == 10) {
            cursorX2 = startX2;
            cursorY += lineHeight2;
            lineIndex2++;
            prevCodePoint3 = -1;
         } else {
            MsdfGlyph msdfGlyph = fontAtlas.getMsdfGlyph(codePoint3);
            if (prevCodePoint3 != -1) {
               cursorX2 += fontAtlas.computeFloat2(prevCodePoint3, codePoint3) * size4;
            }

            if (!fontAtlas.matchesCriteria(codePoint3) && SmallTextAtlasBaker.matchesCriteria(codePoint3)) {
               if (!this.disableSmallText && !shimmer && Math.abs(msdfTextDrawData.rotationDegrees()) < 0.001F) {
                  SmallTextAtlasBaker.AtlasGlyphKey glyphKey2 = this.smallTextAtlasBaker.getNested04963(codePoint3, SmallTextAtlasBaker.computeFloat5(size4), 0);
                  if (glyphKey2.drawable()) {
                     float rightX = cursorX2 + glyphKey2.offsetX() / GuiScale.computeFloat();
                     float topY = cursorY - glyphKey2.offsetY() / GuiScale.computeFloat();
                     MsdfTextRenderer.DataRecord4 dataRecord4b = new MsdfTextRenderer.DataRecord4(
                        guiRenderState, layerSerial2, glyphKey2.page(), MsdfTextRenderer.DataRecord.of(matrix3x2f), screenRect
                     );
                     RasterTextRenderState rasterTextRenderState = this.rasterStateCache.get(dataRecord4b);
                     if (rasterTextRenderState == null) {
                        rasterTextRenderState = new RasterTextRenderState(matrix3x2f, glyphKey2.page().getTextureSetup(), screenRect);
                        this.rasterStateCache.put(dataRecord4b, rasterTextRenderState);
                        guiRenderState.addPreparedTextElement(rasterTextRenderState);
                     }

                     RasterTextRenderState rasterTextRenderState2 = rasterTextRenderState;
                     emitClippedQuad(
                        rightX,
                        topY,
                        rightX + glyphKey2.width() / GuiScale.computeFloat(),
                        topY + glyphKey2.height() / GuiScale.computeFloat(),
                        glyphKey2.u0(),
                        glyphKey2.v0(),
                        glyphKey2.u1(),
                        glyphKey2.v1(),
                        (quadLeft2, quadTop2, quadRight2, quadBottom2, u0Second, v0Second, u1Second, v1Second, fadeTop2, fadeBottom2) -> {
                           int colorTopLeft2 = applyAlphaToColor(this.applyHorizontalFade(msdfTextDrawData.colorTopLeft(), quadLeft2, msdfTextDrawData), fadeTop2);
                           int colorTopRight2 = applyAlphaToColor(this.applyHorizontalFade(msdfTextDrawData.colorTopRight(), quadRight2, msdfTextDrawData), fadeTop2);
                           int colorBottomRight2 = applyAlphaToColor(this.applyHorizontalFade(msdfTextDrawData.colorBottomRight(), quadRight2, msdfTextDrawData), fadeBottom2);
                           int colorBottomLeft2 = applyAlphaToColor(this.applyHorizontalFade(msdfTextDrawData.colorBottomLeft(), quadLeft2, msdfTextDrawData), fadeBottom2);
                           if ((colorTopLeft2 | colorTopRight2 | colorBottomRight2 | colorBottomLeft2) >>> 24 != 0) {
                              rasterTextRenderState2.add(
                                 quadLeft2,
                                 quadTop2,
                                 quadRight2,
                                 quadBottom2,
                                 u0Second,
                                 v0Second,
                                 u1Second,
                                 v1Second,
                                 colorTopLeft2,
                                 colorTopRight2,
                                 colorBottomRight2,
                                 colorBottomLeft2
                              );
                           }
                        }
                     );
                  }
               }

               cursorX2 += SmallTextAtlasBaker.computeFloat3(codePoint3) * size4;
               prevCodePoint3 = codePoint3;
            } else {
               if (msdfGlyph.drawable()) {
                  float leftX = cursorX2 + msdfGlyph.planeLeft() * size4;
                  float rightX = cursorX2 + msdfGlyph.planeRight() * size4;
                  float topY = cursorY - msdfGlyph.planeTop() * size4;
                  float bottomY = cursorY - msdfGlyph.planeBottom() * size4;
                  float lineWidth = lineIndex2 < floatArray.length ? floatArray[lineIndex2] : 1.0F;
                  float fadeLeft = shimmer ? clamp01((leftX - startX2) / lineWidth) : 0.0F;
                  float fadeRight = shimmer ? clamp01((rightX - startX2) / lineWidth) : 0.0F;
                  if (shimmer) {
                     int baseColor = msdfTextDrawData.colorTopLeft();
                     fadeLeft = packSmallColor(fadeLeft, baseColor);
                     fadeRight = packSmallColor(fadeRight, baseColor);
                  }

                  boolean rotated = Math.abs(msdfTextDrawData.rotationDegrees()) >= 0.001F;
                  if (!shimmer && !rotated) {
                     MsdfTextRenderState msdfTextRenderState2 = msdfTextRenderState;
                     float fadeLeftCopy = fadeLeft;
                     float fadeRightCopy = fadeRight;
                     emitClippedQuad(
                        leftX,
                        topY,
                        rightX,
                        bottomY,
                        msdfGlyph.u0(),
                        msdfGlyph.v0(),
                        msdfGlyph.u1(),
                        msdfGlyph.v1(),
                        (quadLeft3, quadTop3, quadRight3, quadBottom3, u0Third, v0Third, u1Third, v1Third, fadeTop3, fadeBottom3) -> {
                           int colorTopLeft3 = applyAlphaToColor(this.applyHorizontalFade(msdfTextDrawData.colorTopLeft(), quadLeft3, msdfTextDrawData), fadeTop3);
                           int colorTopRight3 = applyAlphaToColor(this.applyHorizontalFade(msdfTextDrawData.colorTopRight(), quadRight3, msdfTextDrawData), fadeTop3);
                           int colorBottomRight3 = applyAlphaToColor(this.applyHorizontalFade(msdfTextDrawData.colorBottomRight(), quadRight3, msdfTextDrawData), fadeBottom3);
                           int colorBottomLeft3 = applyAlphaToColor(this.applyHorizontalFade(msdfTextDrawData.colorBottomLeft(), quadLeft3, msdfTextDrawData), fadeBottom3);
                           if ((colorTopLeft3 | colorTopRight3 | colorBottomRight3 | colorBottomLeft3) >>> 24 != 0) {
                              msdfTextRenderState2.addGlyph(
                                 quadLeft3,
                                 quadTop3,
                                 quadRight3,
                                 quadBottom3,
                                 u0Third,
                                 v0Third,
                                 u1Third,
                                 v1Third,
                                 colorTopLeft3,
                                 colorTopRight3,
                                 colorBottomRight3,
                                 colorBottomLeft3,
                                 fadeLeftCopy,
                                 fadeRightCopy,
                                 0.0F,
                                 0.0F,
                                 0.0F
                              );
                           }
                        }
                     );
                  } else {
                     int finalColorTopLeft = shimmer
                        ? shimmerColor
                        : RoundedClipStack.computeInt(this.applyHorizontalFade(msdfTextDrawData.colorTopLeft(), leftX, msdfTextDrawData), leftX, topY);
                     int finalColorTopRight = shimmer
                        ? shimmerColor
                        : RoundedClipStack.computeInt(this.applyHorizontalFade(msdfTextDrawData.colorTopRight(), rightX, msdfTextDrawData), rightX, topY);
                     int finalColorBottomRight = shimmer
                        ? shimmerColor
                        : RoundedClipStack.computeInt(this.applyHorizontalFade(msdfTextDrawData.colorBottomRight(), rightX, msdfTextDrawData), rightX, bottomY);
                     int finalColorBottomLeft = shimmer
                        ? shimmerColor
                        : RoundedClipStack.computeInt(this.applyHorizontalFade(msdfTextDrawData.colorBottomLeft(), leftX, msdfTextDrawData), leftX, bottomY);
                     if (shimmer || (finalColorTopLeft | finalColorTopRight | finalColorBottomRight | finalColorBottomLeft) >>> 24 != 0) {
                        msdfTextRenderState.addGlyph(
                           leftX,
                           topY,
                           rightX,
                           bottomY,
                           msdfGlyph.u0(),
                           msdfGlyph.v0(),
                           msdfGlyph.u1(),
                           msdfGlyph.v1(),
                           finalColorTopLeft,
                           finalColorTopRight,
                           finalColorBottomRight,
                           finalColorBottomLeft,
                           fadeLeft,
                           fadeRight,
                           msdfTextDrawData.rotationDegrees(),
                           msdfTextDrawData.rotationOriginX(),
                           msdfTextDrawData.rotationOriginY()
                        );
                     }
                  }
               }

               cursorX2 += msdfGlyph.advance() * size4;
               prevCodePoint3 = codePoint3;
            }
         }
      }
   }

   private static float packSmallColor(float intensity, int color3) {
      int value = Math.round(clamp01(intensity) * 63.0F);
      int redChannel = Math.round((color3 >>> 16 & 0xFF) / 255.0F * 63.0F);
      int greenChannel = Math.round((color3 >>> 8 & 0xFF) / 255.0F * 63.0F);
      int blueChannel = Math.round((color3 & 0xFF) / 255.0F * 63.0F);
      return value + blueChannel * 64 + greenChannel * 4096 + redChannel * 262144;
   }

   @Override
   public void close() {
      this.resetRenderState();
   }

   private static float clamp01(float value2) {
      return Math.max(0.0F, Math.min(1.0F, value2));
   }

   public static void updateState4() {
      MsdfTextRenderer currentMsdfTextRenderer = msdfTextRenderer;
      if (currentMsdfTextRenderer != null) {
         currentMsdfTextRenderer.resetRenderState();
      } else {
         FontRegistry.clearCache();
      }
   }

   private static void emitClippedQuad(
      float left2,
      float top2,
      float right2,
      float bottom2,
      float u02,
      float v02,
      float u12,
      float v12,
      MsdfTextRenderer.SubInterface2 callback
   ) {
      if (!RoundedClipStack.checkState()) {
         callback.processData6(left2, top2, right2, bottom2, u02, v02, u12, v12, 1.0F, 1.0F);
      } else {
         float clipLeft = RoundedClipStack.computeFloat9();
         float clipTop = RoundedClipStack.computeFloat4();
         float clipRight = clipLeft + RoundedClipStack.computeFloat6();
         float clipBottom = clipTop + RoundedClipStack.computeFloat2();
         if (!(left2 >= clipRight)
            && !(right2 <= clipLeft)
            && !(top2 >= clipBottom)
            && !(bottom2 <= clipTop)
            && !(right2 <= left2)
            && !(bottom2 <= top2)) {
            float rectWidth = right2 - left2;
            float rectHeight = bottom2 - top2;
            float clippedLeft = Math.max(left2, clipLeft);
            float clippedRight = Math.min(right2, clipRight);
            float clippedTop = Math.max(top2, clipTop);
            float clippedBottom = Math.min(bottom2, clipBottom);
            float u0AtLeft = u02 + (u12 - u02) * (clippedLeft - left2) / rectWidth;
            float u1AtRight = u02 + (u12 - u02) * (clippedRight - left2) / rectWidth;
            float topBound = clipTop + 1.0F;
            float bottomBound = clipBottom - 1.0F;
            float[] floatArray = new float[]{
               clippedTop,
               Math.min(Math.max(topBound, clippedTop), clippedBottom),
               Math.min(Math.max(bottomBound, clippedTop), clippedBottom),
               clippedBottom
            };
            Arrays.sort(floatArray);

            for (int index = 0; index < 3; index++) {
               float segmentTop = floatArray[index];
               float segmentBottom = floatArray[index + 1];
               if (!(segmentBottom - segmentTop < 1.0E-4F)) {
                  float v0AtTop = v02 + (v12 - v02) * (segmentTop - top2) / rectHeight;
                  float v1AtBottom = v02 + (v12 - v02) * (segmentBottom - top2) / rectHeight;
                  callback.processData6(
                     clippedLeft,
                     segmentTop,
                     clippedRight,
                     segmentBottom,
                     u0AtLeft,
                     v0AtTop,
                     u1AtRight,
                     v1AtBottom,
                     computeEdgeFade(segmentTop, clipTop, clipBottom),
                     computeEdgeFade(segmentBottom, clipTop, clipBottom)
                  );
               }
            }
         }
      }
   }

   private static float computeEdgeFade(float coordinate, float min, float max) {
      float fade = Math.min((coordinate - min) / 1.0F, (max - coordinate) / 1.0F);
      return Math.max(0.0F, Math.min(1.0F, fade));
   }

   private void resetRenderState() {
      this.warmupIndex = 0;
      this.drawContext2 = null;
      this.msdfStateCache.clear();
      this.rasterStateCache.clear();
      this.textureSetupCache.clear();
      this.smallTextAtlasBaker.updateState();
      FontRegistry.clearCache();
   }

   private float[] measureLineWidths(FontAtlas fontAtlas, String text5, float size5) {
      int lineCount = 1;

      for (int index = 0; index < text5.length(); index++) {
         if (text5.charAt(index) == '\n') {
            lineCount++;
         }
      }

      float[] floatArray = new float[lineCount];
      int lineIndex3 = 0;
      int prevCodePoint4 = -1;
      int textIndex = 0;

      while (textIndex < text5.length()) {
         int codePoint4 = text5.codePointAt(textIndex);
         textIndex += Character.charCount(codePoint4);
         if (codePoint4 == 10) {
            floatArray[lineIndex3] = Math.max(floatArray[lineIndex3], 1.0F);
            lineIndex3++;
            prevCodePoint4 = -1;
         } else {
            MsdfGlyph msdfGlyph = fontAtlas.getMsdfGlyph(codePoint4);
            if (prevCodePoint4 != -1) {
               floatArray[lineIndex3] += fontAtlas.computeFloat2(prevCodePoint4, codePoint4) * size5;
            }

            floatArray[lineIndex3] += (
                  !fontAtlas.matchesCriteria(codePoint4) && SmallTextAtlasBaker.matchesCriteria(codePoint4)
                     ? SmallTextAtlasBaker.computeFloat3(codePoint4)
                     : msdfGlyph.advance()
               )
               * size5;
            prevCodePoint4 = codePoint4;
         }
      }

      for (int lineArrayIndex = 0; lineArrayIndex < floatArray.length; lineArrayIndex++) {
         floatArray[lineArrayIndex] = Math.max(floatArray[lineArrayIndex], 1.0F);
      }

      return floatArray;
   }

   private static Identifier getIdentifier(String path) {
      return Identifier.of("greed", path);
   }

   public void processData4(MsdfTextDrawData msdfTextDrawData, float red, float green, float blue, float alpha3) {
      this.submitText(this.drawContext2, msdfTextDrawData, true, red, green, blue, alpha3);
   }

   private MsdfTextRenderer() {
   }

   public void processData5(boolean disableSmallText) {
      this.disableSmallText = disableSmallText;
   }

   public void renderElement(DrawContext drawContext) {
      if (this.drawContext2 != drawContext) {
         this.msdfStateCache.clear();
         this.rasterStateCache.clear();
      }

      this.drawContext2 = drawContext;
      this.smallTextAtlasBaker.updateState5();
   }

   public void updateState6() {
      this.textureSetupCache.clear();
   }

   public boolean matchesCriteria2(String fontName3, int codePoint5) {
      FontAtlas fontAtlas = FontRegistry.getAtlas(fontName3);
      return fontAtlas != null && fontAtlas.matchesCriteria(codePoint5);
   }

   public static MsdfTextRenderer getMsdfTextRenderer() {
      MsdfTextRenderer currentMsdfTextRenderer = msdfTextRenderer;
      if (currentMsdfTextRenderer == null) {
         synchronized (MsdfTextRenderer.class) {
            currentMsdfTextRenderer = msdfTextRenderer;
            if (currentMsdfTextRenderer == null) {
               currentMsdfTextRenderer = new MsdfTextRenderer();
               msdfTextRenderer = currentMsdfTextRenderer;
            }
         }
      }

      return currentMsdfTextRenderer;
   }

   private void submitText(
      DrawContext drawContext, MsdfTextDrawData msdfTextDrawData, boolean shimmer2, float red2, float green2, float blue2, float alpha4
   ) {
      if (drawContext != null && msdfTextDrawData != null && msdfTextDrawData.visible()) {
         FontAtlas fontAtlas = FontRegistry.getAtlas(msdfTextDrawData.fontName());
         if (fontAtlas != null) {
            try {
               Matrix3x2f matrix3x2f = GuiScale.renderElement(drawContext);
               GuiRenderState guiRenderState = ((DrawContextMixin)drawContext).getGuiRenderState();
               int currentLayerSerial = ((GuiLayerSerialAccess)guiRenderState).getLayerSerial();
               ScreenRect screenRect = ScissorStack.getScreenRect();
               boolean canBatch = !shimmer2 && Math.abs(msdfTextDrawData.rotationDegrees()) < 0.001F && msdfTextDrawData.size() <= 20.0F;
               if (canBatch
                  && !this.disableSmallText
                  && SmallTextAtlasBaker.matchesCriteria2(matrix3x2f)
                  && this.smallTextAtlasBaker.matchesCriteria5(fontAtlas)
                  && this.tryRenderSmallText(guiRenderState, currentLayerSerial, matrix3x2f, screenRect, fontAtlas, msdfTextDrawData)) {
                  return;
               }

               TextureSetup textureSetup = this.getTextureSetup(fontAtlas.getIdentifier());
               if (textureSetup == null) {
                  return;
               }

               int shimmerColor2 = shimmer2 ? packArgb(red2, green2, blue2, alpha4) : 0;
               MsdfTextRenderer.DataRecord3 dataRecord3 = new MsdfTextRenderer.DataRecord3(
                  guiRenderState, currentLayerSerial, fontAtlas.getIdentifier(), MsdfTextRenderer.DataRecord.of(matrix3x2f), screenRect, shimmer2, shimmerColor2
               );
               MsdfTextRenderState msdfTextRenderState = this.msdfStateCache.get(dataRecord3);
               if (msdfTextRenderState == null) {
                  msdfTextRenderState = new MsdfTextRenderState(matrix3x2f, textureSetup, screenRect, shimmer2, fontAtlas.computeFloat8());
                  this.msdfStateCache.put(dataRecord3, msdfTextRenderState);
                  guiRenderState.addPreparedTextElement(msdfTextRenderState);
               }

               this.renderGlyphRun(msdfTextRenderState, guiRenderState, currentLayerSerial, matrix3x2f, screenRect, fontAtlas, msdfTextDrawData, shimmer2, shimmerColor2);
            } catch (RuntimeException exception) {
               GreedClient.logger().warn("[MSDF] Failed to submit text: {}", msdfTextDrawData.text(), exception);
            }
         }
      }
   }

   private static float smoothstep(float t) {
      return t * t * (3.0F - 2.0F * t);
   }

   private record DataRecord(float m00, float m01, float m10, float m11, float m20, float m21) {
      static MsdfTextRenderer.DataRecord of(Matrix3x2f matrix3x2f) {
         return new MsdfTextRenderer.DataRecord(
            matrix3x2f.m00(), matrix3x2f.m01(), matrix3x2f.m10(), matrix3x2f.m11(), matrix3x2f.m20(), matrix3x2f.m21()
         );
      }
   }

   private interface SubInterface2 {
      void processData6(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10);
   }

   private record DataRecord3(
      GuiRenderState state, int layerSerial, Identifier atlas, MsdfTextRenderer.DataRecord pose, ScreenRect scissorArea, boolean shimmer, int shimmerParams
   ) {
   }

   private record DataRecord4(
      GuiRenderState state, int layerSerial, SmallTextAtlasBaker.SubContext page, MsdfTextRenderer.DataRecord pose, ScreenRect scissorArea
   ) {
   }

   private record DataRecord5(AbstractTexture texture, TextureSetup setup) {
   }
}
