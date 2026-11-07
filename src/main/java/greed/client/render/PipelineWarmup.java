package greed.client.render;

import greed.client.module.render.BlockEspModule;
import greed.client.module.render.CustomWorldModule;
import greed.client.module.render.GlassVaporPipeline;
import greed.client.module.render.MotionBlurModule;
import greed.client.module.render.ParticlesModule;
import greed.client.module.render.ProjectilePredictionModule;
import greed.client.module.render.SkeletonEspModule;
import greed.client.module.render.TargetEspModule;
import greed.client.module.render.TorusModule;
import greed.client.module.render.TotemGhostModule;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PipelineWarmup {
   private static final Class<?>[] PIPELINE_OWNERS = new Class[]{
      BatchedBlurRenderer.class,
      GlassRenderer.class,
      ShapeRenderer.class,
      GlowCompositeRenderer.class,
      SoftGlowRenderer.class,
      OutlineGlassRenderer.class,
      CircleRenderer.class,
      CornerBracketRenderer.class,
      BlobRenderer.class,
      ArcDividerRenderer.class,
      PickerRenderer.class,
      RectDefaultRenderer.class,
      RectHalfIconRenderer.class,
      OutlineDefaultRenderer.class,
      Outline360Renderer.class,
      ImageRenderer.class,
      LineRenderer.class,
      RippleRenderer.class,
      ShimmerRenderer.class,
      MenuGridRenderer.class,
      WarpTextRenderer.class,
      DitherRenderer.class,
      EffectIconRenderer.class,
      MsdfTextRenderer.class,
      TextAtlasRenderer.class,
      ItemRenderer.class,
      ShapePipelines.class,
      PlayerOutlineRenderer.class,
      HandOutlineRenderer.class,
      ChamsWorldRenderer.class,
      JumpCircleRenderer.class,
      SaturationRenderer.class,
      MotionBlurModule.class,
      GlassVaporPipeline.class,
      ParticlesModule.class,
      SkeletonEspModule.class,
      BlockEspModule.class,
      TargetEspModule.class,
      TorusModule.class,
      ProjectilePredictionModule.class,
      CustomWorldModule.class,
      TotemGhostModule.class,
      TargetScanFeatureRenderer.class,
      WetWorldRenderer.class,
      RainScreenRenderer.class,
      WorldRainRenderer.class
   };
   private static final Logger LOGGER = LoggerFactory.getLogger("greed-pipelines");
   private static ArrayList<RenderPipeline> pipelines;
   private static int nextPipeline;

   public static boolean warmNextPipeline() {
      // The loading overlay draws before ShaderLoader has applied its new sources.
      if (MinecraftClient.getInstance().getOverlay() != null) {
         return false;
      }
      if (pipelines == null) {
         pipelines = collectPipelines();
      }

      if (nextPipeline >= pipelines.size()) {
         return false;
      }

      RenderPipeline renderPipeline = pipelines.get(nextPipeline++);

      try {
         RenderSystem.getDevice().precompilePipeline(renderPipeline);
      } catch (Throwable throwable) {
         LOGGER.error("pipeline {} failed to compile", renderPipeline.getLocation(), throwable);
      }

      return true;
   }

   public static void resetWarmup() {
      pipelines = null;
      nextPipeline = 0;
   }

   private PipelineWarmup() {
   }

   private static ArrayList<RenderPipeline> collectPipelines() {
      ArrayList<RenderPipeline> local3949_0 = new ArrayList<>(64);

      for (Class<?> local3949_4 : PIPELINE_OWNERS) {
         try {
            for (Field field : local3949_4.getDeclaredFields()) {
               if (Modifier.isStatic(field.getModifiers()) && RenderPipeline.class.isAssignableFrom(field.getType())) {
                  field.setAccessible(true);
                  RenderPipeline renderPipeline = (RenderPipeline)field.get(null);
                  if (renderPipeline != null && !local3949_0.contains(renderPipeline)) {
                     local3949_0.add(renderPipeline);
                  }
               }
            }
         } catch (Throwable throwable) {
         }
      }

      return local3949_0;
   }
}
