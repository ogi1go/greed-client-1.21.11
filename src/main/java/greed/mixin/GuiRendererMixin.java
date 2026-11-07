package greed.mixin;

import greed.client.render.ArcDividerRenderer;
import greed.client.render.BatchedBlurRenderer;
import greed.client.render.BlobRenderer;
import greed.client.render.CircleRenderer;
import greed.client.render.CornerBracketRenderer;
import greed.client.render.FontRegistry;
import greed.client.render.GlassRenderer;
import greed.client.render.GlowCompositeRenderer;
import greed.client.render.ImageRenderer;
import greed.client.render.LineRenderer;
import greed.client.render.MenuGridRenderer;
import greed.client.render.MsdfTextRenderer;
import greed.client.render.Outline360Renderer;
import greed.client.render.OutlineDefaultRenderer;
import greed.client.render.OutlineGlassRenderer;
import greed.client.render.PickerRenderer;
import greed.client.render.ThemePaletteBuffer;
import greed.client.render.ItemRenderCache;
import greed.client.render.PipelineWarmup;
import greed.client.render.GradientSplitBuffer;
import greed.client.render.ShapeBlurManager;
import greed.client.render.ShapePipelines;
import greed.client.render.RectDefaultRenderer;
import greed.client.render.RectHalfIconRenderer;
import greed.client.render.RippleRenderer;
import greed.client.render.ShapeRenderer;
import greed.client.render.ShimmerRenderer;
import greed.client.render.SoftGlowRenderer;
import ru.dreamix.fabricloader.VMBridge;
import greed.client.render.WarpTextRenderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import net.minecraft.client.gui.render.state.TexturedQuadGuiElementRenderState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.texture.TextureSetup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
   @Shadow
   @Final
   private List<?> preparations;
   @Shadow
   @Final
   private List<?> draws;
   @Shadow
   private int blurLayer;
   private final IntArrayList shapeBlurMarkers = new IntArrayList();
   private final IntArrayList shapeBlurMarkerSlots = new IntArrayList();
   private RenderPass currentRenderPass;
   private boolean blurDrawActive;
   private boolean glassDrawActive;
   private boolean shapeDrawActive;
   private boolean glowDrawActive;
   private boolean softGlowDrawActive;
   private boolean glassOutlineDrawActive;
   private boolean circleDrawActive;
   private boolean bracketDrawActive;
   private boolean blobDrawActive;
   private boolean arcDrawActive;
   private boolean pickerDrawActive;
   private boolean rectangleDrawActive;
   private boolean halfIconRectangleDrawActive;
   private boolean outlineDrawActive;
   private boolean outline360DrawActive;
   private boolean imageDrawActive;
   private boolean lineDrawActive;
   private boolean itemDrawActive;
   private boolean rippleDrawActive;
   private boolean shimmerDrawActive;
   private boolean menuGridDrawActive;
   private boolean warpTextDrawActive;

   @Invoker("render")
   abstract void greed$executeDrawRange(
      Supplier<String> var1, Framebuffer framebuffer, GpuBufferSlice gpuBufferSlice, GpuBufferSlice gpuBufferSlice2, GpuBuffer gpuBuffer, IndexType indexType, int var7, int var8
   );

   @Inject(method = "endBuffer(Lnet/minecraft/client/render/BufferBuilder;Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/texture/TextureSetup;Lnet/minecraft/client/gui/ScreenRect;)V", at = @At("HEAD"))
   private void trackShapeBlurMarker(
      BufferBuilder bufferBuilder, RenderPipeline renderPipeline, TextureSetup textureSetup, ScreenRect screenRect, CallbackInfo callbackInfo
   ) {
      BatchedBlurRenderer.getBatchedBlurRenderer().processData12(renderPipeline, textureSetup, this.preparations.size(), this.blurLayer);
      if (ShapePipelines.usesBlurTexture(renderPipeline)) {
         this.shapeBlurMarkers.add(this.preparations.size());
         this.shapeBlurMarkerSlots.add(textureSetup == null ? -1 : ShapeBlurManager.INSTANCE.computeInt4(textureSetup.texure0()));
      }
   }

   @Redirect(
      method = "renderPreparedDraws(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Ljava/util/function/Supplier;Lnet/minecraft/client/gl/Framebuffer;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V"
      )
   )
   private void splitShapeBlurRanges(
      GuiRenderer guiRenderer,
      Supplier<String> local8150_2,
      Framebuffer framebuffer,
      GpuBufferSlice gpuBufferSlice,
      GpuBufferSlice gpuBufferSlice2,
      GpuBuffer gpuBuffer,
      IndexType indexType,
      int local8150_8,
      int local8150_9
   ) {
      if (!this.shapeBlurMarkers.isEmpty() && local8150_8 < local8150_9) {
         int local8150_10 = local8150_8;

         for (int index = 0; index < this.shapeBlurMarkers.size(); index++) {
            int local8150_12 = this.shapeBlurMarkers.getInt(index);
            if (local8150_12 >= local8150_8 && local8150_12 < local8150_9) {
               int local8150_13 = this.shapeBlurMarkerSlots.getInt(index);
               if (local8150_13 >= 0 && ShapeBlurManager.INSTANCE.matchesCriteria(local8150_13)) {
                  if (local8150_12 > local8150_10) {
                     this.greed$executeDrawRange(local8150_2, framebuffer, gpuBufferSlice, gpuBufferSlice2, gpuBuffer, indexType, local8150_10, local8150_12);
                  }

                  ShapeBlurManager.INSTANCE.processData3(local8150_13, framebuffer);
                  local8150_10 = local8150_12;
               }
            }
         }

         if (local8150_10 < local8150_9) {
            this.greed$executeDrawRange(local8150_2, framebuffer, gpuBufferSlice, gpuBufferSlice2, gpuBuffer, indexType, local8150_10, local8150_9);
         }
      } else {
         this.greed$executeDrawRange(local8150_2, framebuffer, gpuBufferSlice, gpuBufferSlice2, gpuBuffer, indexType, local8150_8, local8150_9);
      }
   }

   @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"))
   private void beginBlurFrame(CallbackInfo callbackInfo) {
      int local8151_2 = MinecraftClient.getInstance().getOverlay() == null ? 1 : 8;

      while (local8151_2 > 0 && this.warmNextUnit()) {
         local8151_2--;
      }

      BatchedBlurRenderer.getBatchedBlurRenderer().updateState6();
      GlassRenderer.getGlassRenderer().updateState2();
      ShapeRenderer.getShapeRenderer().updateState2();
      GlowCompositeRenderer.getGlowCompositeRenderer().updateState();
      SoftGlowRenderer.getSoftGlowRenderer().updateState2();
      OutlineGlassRenderer.getOutlineGlassRenderer().updateState4();
      CircleRenderer.getCircleRenderer().updateState5();
      CornerBracketRenderer.getCornerBracketRenderer().updateState4();
      BlobRenderer.getBlobRenderer().updateState4();
      ArcDividerRenderer.getArcDividerRenderer().updateState2();
      PickerRenderer.getPickerRenderer().updateState();
      RectDefaultRenderer.getRectDefaultRenderer().updateState4();
      RectHalfIconRenderer.getRectHalfIconRenderer().updateState4();
      OutlineDefaultRenderer.getOutlineDefaultRenderer().updateState();
      Outline360Renderer.getOutline360Renderer().updateState3();
      ImageRenderer.getImageRenderer().updateState3();
      LineRenderer.getLineRenderer().updateState();
      RippleRenderer.getRippleRenderer().updateState3();
      ShimmerRenderer.getShimmerRenderer().updateState5();
      MenuGridRenderer.getMenuGridRenderer().updateState4();
      WarpTextRenderer.getWarpTextRenderer().updateState2();
      MsdfTextRenderer.getMsdfTextRenderer().updateState3();
      ItemRenderCache.updateState7();
   }

   private boolean warmNextUnit() {
      if (FontRegistry.warmNextFont()) {
         return true;
      } else {
         return MsdfTextRenderer.getMsdfTextRenderer().checkState() ? true : PipelineWarmup.warmNextPipeline();
      }
   }

   @Inject(method = "prepare()V", at = @At("HEAD"))
   private void preparePendingBlurResources(CallbackInfo callbackInfo) {
      this.shapeBlurMarkers.clear();
      this.shapeBlurMarkerSlots.clear();
      GradientSplitBuffer.updateState2();
      BatchedBlurRenderer.getBatchedBlurRenderer().updateState2();
      GlowCompositeRenderer.getGlowCompositeRenderer().updateState3();
   }

   @Inject(method = "prepare()V", at = @At("RETURN"))
   private void prepareRenderUniforms(CallbackInfo callbackInfo) {
      ThemePaletteBuffer.updateState();
      BatchedBlurRenderer.getBatchedBlurRenderer().updateState5();
      GlassRenderer.getGlassRenderer().updateState3();
      ShapeRenderer.getShapeRenderer().updateState();
      GlowCompositeRenderer.getGlowCompositeRenderer().updateState7();
      SoftGlowRenderer.getSoftGlowRenderer().updateState3();
      OutlineGlassRenderer.getOutlineGlassRenderer().updateState3();
      CircleRenderer.getCircleRenderer().updateState4();
      CornerBracketRenderer.getCornerBracketRenderer().updateState5();
      BlobRenderer.getBlobRenderer().updateState();
      ArcDividerRenderer.getArcDividerRenderer().updateState();
      PickerRenderer.getPickerRenderer().updateState4();
      RectDefaultRenderer.getRectDefaultRenderer().updateState3();
      RectHalfIconRenderer.getRectHalfIconRenderer().updateState();
      OutlineDefaultRenderer.getOutlineDefaultRenderer().updateState4();
      Outline360Renderer.getOutline360Renderer().updateState5();
      ImageRenderer.getImageRenderer().updateState2();
      LineRenderer.getLineRenderer().updateState5();
      RippleRenderer.getRippleRenderer().updateState2();
      ShimmerRenderer.getShimmerRenderer().updateState();
      MenuGridRenderer.getMenuGridRenderer().updateState5();
      WarpTextRenderer.getWarpTextRenderer().updateState4();
      ItemRenderCache.updateState8();
   }

   @Inject(method = "renderPreparedDraws(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("HEAD"))
   private void prepareBlurCapture(CallbackInfo callbackInfo) {
      BatchedBlurRenderer.getBatchedBlurRenderer().processData8(this.blurLayer, this.draws.size());
      ShapeBlurManager.INSTANCE.updateState2();
   }

   @Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("RETURN"))
   private void clearSpecialItemTints(CallbackInfo callbackInfo) {
      ItemRenderCache.updateState2();
      ShapeBlurManager.INSTANCE.updateState();
   }

   @Redirect(
      method = "prepareItem(Lnet/minecraft/client/gui/render/state/ItemGuiElementRenderState;FFII)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;addSimpleElementToCurrentLayer(Lnet/minecraft/client/gui/render/state/TexturedQuadGuiElementRenderState;)V"
      )
   )
   private void tintItemAtlasBlit(
      GuiRenderState guiRenderState,
      TexturedQuadGuiElementRenderState texturedQuadGuiElementRenderState,
      ItemGuiElementRenderState itemGuiElementRenderState,
      float local8157_4,
      float local8157_5,
      int local8157_6,
      int local8157_7
   ) {
      guiRenderState.addSimpleElementToCurrentLayer(
         ItemRenderCache.computeInt(itemGuiElementRenderState) == -1
            ? texturedQuadGuiElementRenderState
            : new TexturedQuadGuiElementRenderState(
               texturedQuadGuiElementRenderState.pipeline(),
               texturedQuadGuiElementRenderState.textureSetup(),
               texturedQuadGuiElementRenderState.pose(),
               texturedQuadGuiElementRenderState.x1(),
               texturedQuadGuiElementRenderState.y1(),
               texturedQuadGuiElementRenderState.x2(),
               texturedQuadGuiElementRenderState.y2(),
               texturedQuadGuiElementRenderState.u1(),
               texturedQuadGuiElementRenderState.u2(),
               texturedQuadGuiElementRenderState.v1(),
               texturedQuadGuiElementRenderState.v2(),
               ItemRenderCache.computeInt(itemGuiElementRenderState),
               texturedQuadGuiElementRenderState.scissorArea(),
               texturedQuadGuiElementRenderState.bounds()
            )
      );
   }

   @Redirect(
      method = "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V")
   )
   private void trackPipeline(RenderPass newCurrentRenderPass, RenderPipeline renderPipeline) {
      if (!renderPipeline.getLocation().getNamespace().equals("greed")) {
         this.currentRenderPass = null;
         newCurrentRenderPass.setPipeline(renderPipeline);
      } else {
         this.currentRenderPass = newCurrentRenderPass;
         this.blurDrawActive = BatchedBlurRenderer.getBatchedBlurRenderer().matchesCriteria(renderPipeline);
         this.glassDrawActive = GlassRenderer.getGlassRenderer().matchesCriteria(renderPipeline);
         this.shapeDrawActive = ShapeRenderer.getShapeRenderer().matchesCriteria(renderPipeline);
         this.glowDrawActive = GlowCompositeRenderer.getGlowCompositeRenderer().matchesCriteria(renderPipeline);
         this.softGlowDrawActive = SoftGlowRenderer.getSoftGlowRenderer().matchesCriteria(renderPipeline);
         this.glassOutlineDrawActive = OutlineGlassRenderer.getOutlineGlassRenderer().matchesCriteria(renderPipeline);
         this.circleDrawActive = CircleRenderer.getCircleRenderer().matchesCriteria(renderPipeline);
         this.bracketDrawActive = CornerBracketRenderer.getCornerBracketRenderer().matchesCriteria(renderPipeline);
         this.blobDrawActive = BlobRenderer.getBlobRenderer().matchesCriteria(renderPipeline);
         this.arcDrawActive = ArcDividerRenderer.getArcDividerRenderer().matchesCriteria(renderPipeline);
         this.pickerDrawActive = PickerRenderer.getPickerRenderer().matchesCriteria(renderPipeline);
         this.rectangleDrawActive = RectDefaultRenderer.getRectDefaultRenderer().matchesCriteria(renderPipeline);
         this.halfIconRectangleDrawActive = RectHalfIconRenderer.getRectHalfIconRenderer().matchesCriteria(renderPipeline);
         this.outlineDrawActive = OutlineDefaultRenderer.getOutlineDefaultRenderer().matchesCriteria(renderPipeline);
         this.outline360DrawActive = Outline360Renderer.getOutline360Renderer().matchesCriteria(renderPipeline);
         this.imageDrawActive = ImageRenderer.getImageRenderer().matchesCriteria(renderPipeline);
         this.lineDrawActive = LineRenderer.getLineRenderer().matchesCriteria(renderPipeline);
         this.rippleDrawActive = RippleRenderer.getRippleRenderer().matchesCriteria(renderPipeline);
         this.shimmerDrawActive = ShimmerRenderer.getShimmerRenderer().matchesCriteria(renderPipeline);
         this.menuGridDrawActive = MenuGridRenderer.getMenuGridRenderer().matchesCriteria(renderPipeline);
         this.warpTextDrawActive = WarpTextRenderer.getWarpTextRenderer().matchesCriteria(renderPipeline);
         this.itemDrawActive = ItemRenderCache.matchesCriteria(renderPipeline);
         newCurrentRenderPass.setPipeline(renderPipeline);
      }
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;drawIndexed(IIII)V", shift = Shift.BEFORE))
   private void bindBlurParams(CallbackInfo callbackInfo) {
      if (this.blurDrawActive && this.currentRenderPass != null) {
         BatchedBlurRenderer.getBatchedBlurRenderer().processData5(this.currentRenderPass);
      }

      if (this.glassDrawActive && this.currentRenderPass != null) {
         GlassRenderer.getGlassRenderer().processData(this.currentRenderPass);
      }

      if (this.shapeDrawActive && this.currentRenderPass != null) {
         ShapeRenderer.getShapeRenderer().processData2(this.currentRenderPass);
      }

      if (this.glowDrawActive && this.currentRenderPass != null) {
         GlowCompositeRenderer.getGlowCompositeRenderer().processData11(this.currentRenderPass);
      }

      if (this.softGlowDrawActive && this.currentRenderPass != null) {
         SoftGlowRenderer.getSoftGlowRenderer().processData(this.currentRenderPass);
      }

      if (this.glassOutlineDrawActive && this.currentRenderPass != null) {
         OutlineGlassRenderer.getOutlineGlassRenderer().processData(this.currentRenderPass);
      }

      if (this.circleDrawActive && this.currentRenderPass != null) {
         CircleRenderer.getCircleRenderer().processData(this.currentRenderPass);
      }

      if (this.bracketDrawActive && this.currentRenderPass != null) {
         CornerBracketRenderer.getCornerBracketRenderer().processData2(this.currentRenderPass);
      }

      if (this.blobDrawActive && this.currentRenderPass != null) {
         BlobRenderer.getBlobRenderer().processData3(this.currentRenderPass);
      }

      if (this.arcDrawActive && this.currentRenderPass != null) {
         ArcDividerRenderer.getArcDividerRenderer().processData2(this.currentRenderPass);
      }

      if (this.pickerDrawActive && this.currentRenderPass != null) {
         PickerRenderer.getPickerRenderer().processData2(this.currentRenderPass);
      }

      if (this.rectangleDrawActive && this.currentRenderPass != null) {
         RectDefaultRenderer.getRectDefaultRenderer().processData3(this.currentRenderPass);
      }

      if (this.halfIconRectangleDrawActive && this.currentRenderPass != null) {
         RectHalfIconRenderer.getRectHalfIconRenderer().processData(this.currentRenderPass);
      }

      if (this.outlineDrawActive && this.currentRenderPass != null) {
         OutlineDefaultRenderer.getOutlineDefaultRenderer().processData(this.currentRenderPass);
      }

      if (this.outline360DrawActive && this.currentRenderPass != null) {
         Outline360Renderer.getOutline360Renderer().processData2(this.currentRenderPass);
      }

      if (this.imageDrawActive && this.currentRenderPass != null) {
         ImageRenderer.getImageRenderer().processData3(this.currentRenderPass);
      }

      if (this.lineDrawActive && this.currentRenderPass != null) {
         LineRenderer.getLineRenderer().processData(this.currentRenderPass);
      }

      if (this.rippleDrawActive && this.currentRenderPass != null) {
         RippleRenderer.getRippleRenderer().processData3(this.currentRenderPass);
      }

      if (this.shimmerDrawActive && this.currentRenderPass != null) {
         ShimmerRenderer.getShimmerRenderer().processData2(this.currentRenderPass);
      }

      if (this.menuGridDrawActive && this.currentRenderPass != null) {
         MenuGridRenderer.getMenuGridRenderer().processData(this.currentRenderPass);
      }

      if (this.warpTextDrawActive && this.currentRenderPass != null) {
         WarpTextRenderer.getWarpTextRenderer().processData(this.currentRenderPass);
      }

      if (this.itemDrawActive && this.currentRenderPass != null) {
         ItemRenderCache.processData10(this.currentRenderPass);
      }
   }

   @Inject(method = "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V", at = @At("RETURN"))
   private void clearTrackedPipeline(CallbackInfo callbackInfo) {
      this.currentRenderPass = null;
      this.blurDrawActive = false;
      this.glassDrawActive = false;
      this.shapeDrawActive = false;
      this.glowDrawActive = false;
      this.softGlowDrawActive = false;
      this.glassOutlineDrawActive = false;
      this.circleDrawActive = false;
      this.bracketDrawActive = false;
      this.blobDrawActive = false;
      this.arcDrawActive = false;
      this.pickerDrawActive = false;
      this.rectangleDrawActive = false;
      this.halfIconRectangleDrawActive = false;
      this.outlineDrawActive = false;
      this.outline360DrawActive = false;
      this.imageDrawActive = false;
      this.lineDrawActive = false;
      this.itemDrawActive = false;
      this.rippleDrawActive = false;
      this.shimmerDrawActive = false;
      this.menuGridDrawActive = false;
      this.warpTextDrawActive = false;
   }

   static {
      VMBridge.identifyClass(GuiRendererMixin.class, "ukilqNNd");
   }
}
