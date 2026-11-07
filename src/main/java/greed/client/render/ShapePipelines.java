package greed.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;

public final class ShapePipelines {
   public static final RenderPipeline ROUNDED = createShapePipeline("rounded_gui", "ui/shape2/rounded", "ui/shape2/rounded", 0);
   public static final RenderPipeline TEXTURED = createShapePipeline("shape_texture_gui", "ui/shape2/shape", "ui/shape2/shape", 1);
   public static final RenderPipeline TEXTURED_BLUR = createShapePipeline("shape_texture_gui_2d_blur", "ui/shape2/shape", "ui/shape2/shape", 1);
   public static final RenderPipeline GLASS = createShapePipeline("glass2", "ui/shape2/glass", "ui/shape2/glass", 2);
   public static final RenderPipeline GLASS_BLUR = createShapePipeline("glass2_2d_blur", "ui/shape2/glass", "ui/shape2/glass", 2);
   public static final RenderPipeline PIE = createShapePipeline("pie_gui", "ui/shape2/pie", "ui/shape2/pie", 0);
   public static final RenderPipeline PIE_BLUR = createShapePipeline("pie_blur_gui", "ui/shape2/pie", "ui/shape2/pie_blur", 1);
   public static final RenderPipeline BLUR_DOWN = createBlurPipeline("shape_blur_down", "ui/shape2/blur_down", false);
   public static final RenderPipeline BLUR_UP = createBlurPipeline("shape_blur_up", "ui/shape2/blur_up", false);
   public static final RenderPipeline CLAMP_COPY = createBlurPipeline("shape_clamp_copy", "ui/shape2/clamp_copy", true);

   public static boolean usesBlurTexture(RenderPipeline renderPipeline) {
      return renderPipeline == TEXTURED_BLUR || renderPipeline == GLASS_BLUR;
   }

   private static RenderPipeline createShapePipeline(String local6945_0, String local6945_1, String local6945_2, int local6945_3) {
      Builder builder = RenderPipeline.builder()
         .withLocation(shaderId("pipeline/" + local6945_0))
         .withVertexShader(shaderId(local6945_1))
         .withFragmentShader(shaderId(local6945_2))
         .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
         .withUniform("Projection", UniformType.UNIFORM_BUFFER)
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(GreedVertexFormats.vertexFormat, DrawMode.QUADS);
      if (local6945_3 >= 1) {
         builder.withSampler("Sampler0");
      }

      if (local6945_3 >= 2) {
         builder.withSampler("Sampler1");
      }

      return builder.build();
   }

   private ShapePipelines() {
   }

   private static RenderPipeline createBlurPipeline(String local6947_0, String local6947_1, boolean local6947_2) {
      Builder builder = RenderPipeline.builder()
         .withLocation(shaderId("pipeline/" + local6947_0))
         .withVertexShader(shaderId("ui/shape2/pass"))
         .withFragmentShader(shaderId(local6947_1))
         .withSampler("Sampler0")
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withVertexFormat(GreedVertexFormats.vertexFormat2, DrawMode.QUADS);
      if (local6947_2) {
         builder.withUniform("GreedBlurRect", UniformType.UNIFORM_BUFFER);
      }

      return builder.build();
   }

   private static Identifier shaderId(String local6949_0) {
      return Identifier.of("greed", local6949_0);
   }
}
