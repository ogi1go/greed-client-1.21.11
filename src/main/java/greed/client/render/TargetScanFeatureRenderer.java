package greed.client.render;

import greed.client.GreedClient;
import greed.client.module.render.TargetEspModule;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.function.Function;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public final class TargetScanFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   private static final RenderPipeline pipeline2 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(GreedClient.id("pipeline/target_scan_surface"))
      .withVertexShader(GreedClient.id("core/target_scan_surface"))
      .withFragmentShader(GreedClient.id("core/target_scan_surface"))
      .withSampler("Sampler0")
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(true)
      .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS)
      .build();
   private static final Function<Identifier, RenderLayer> function2 = Util.memoize(
      identifier -> RenderLayer.of("target_scan_surface", RenderSetup.builder(pipeline2).texture("Sampler0", identifier).expectedBufferSize(32768).build())
   );

   private void emitModelMesh(MatrixStack matrixStack, VertexConsumer vertexConsumer) {
      this.getContextModel().head.render(matrixStack, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV, -1);
      this.getContextModel().body.render(matrixStack, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV, -1);
      this.getContextModel().rightArm.render(matrixStack, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV, -1);
      this.getContextModel().leftArm.render(matrixStack, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV, -1);
      this.getContextModel().rightLeg.render(matrixStack, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV, -1);
      this.getContextModel().leftLeg.render(matrixStack, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV, -1);
   }

   public void render(
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int local4258_3,
      PlayerEntityRenderState playerEntityRenderState,
      float local4258_5,
      float local4258_6
   ) {
      if (MinecraftClient.getInstance().world != null && playerEntityRenderState.skinTextures != null) {
         if (MinecraftClient.getInstance().world.getEntityById(playerEntityRenderState.id) instanceof PlayerEntity playerEntity) {
            TargetEspModule.DataRecord local4258_8 = TargetEspModule.getInstance().getNested0418(playerEntity);
            if (local4258_8 != null && !(local4258_8.alpha() <= 0.001F)) {
               orderedRenderCommandQueue.getBatchingQueue(20)
                  .submitCustom(matrixStack, function2.apply(playerEntityRenderState.skinTextures.body().texturePath()), (entry, vertexConsumer) -> {
                     MatrixStack matrixStack2 = new MatrixStack();
                     matrixStack2.peek().copy(entry);
                     matrixStack2.scale(1.008F - local4258_8.hit() * 0.018F, 1.008F - local4258_8.hit() * 0.018F, 1.008F - local4258_8.hit() * 0.018F);
                     this.getContextModel().setAngles(playerEntityRenderState);
                     TargetScanFeatureRenderer.SubContext2 vertexConsumer2 = new TargetScanFeatureRenderer.SubContext2();
                     this.emitModelMesh(matrixStack2, vertexConsumer2);
                     if (vertexConsumer2.validSpan()) {
                        this.emitModelMesh(matrixStack2, new TargetScanFeatureRenderer.SubContext(vertexConsumer, vertexConsumer2, local4258_8));
                     }
                  });
            }
         }
      }
   }

   public TargetScanFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> local4260_1) {
      super(local4260_1);
   }

   private static final class SubContext implements VertexConsumer {
      private VertexConsumer vertexConsumer3;
      private TargetScanFeatureRenderer.SubContext2 nested0451;
      private int intVal;
      private int intVal2;
      private int intVal3;
      private int intVal4;
      private int intVal5;
      private int intVal6;
      private int intVal7;
      private float floatVal;
      private float floatVal2;

      private static float spanPC(float local4264_0, float local4264_1, float local4264_2) {
         return MathHelper.clamp((local4264_0 - local4264_1) / Math.max(1.0E-4F, local4264_2 - local4264_1), 0.0F, 1.0F);
      }

      private SubContext(VertexConsumer vertexConsumer, TargetScanFeatureRenderer.SubContext2 local4265_2, TargetEspModule.DataRecord local4265_3) {
         this.vertexConsumer3 = vertexConsumer;
         this.nested0451 = local4265_2;
         this.intVal = local4265_3.primaryColor() >>> 16 & 0xFF;
         this.intVal2 = local4265_3.primaryColor() >>> 8 & 0xFF;
         this.intVal3 = local4265_3.primaryColor() & 0xFF;
         this.intVal4 = (local4265_3.secondaryColor() >>> 16 & 0xFF) << 8 | Math.round(MathHelper.clamp(local4265_3.phase(), 0.0F, 1.0F) * 255.0F);
         this.intVal5 = (local4265_3.secondaryColor() >>> 8 & 0xFF) << 8 | Math.round(MathHelper.clamp(local4265_3.alpha(), 0.0F, 1.0F) * 255.0F);
         this.intVal6 = (local4265_3.secondaryColor() & 0xFF) << 8 | Math.round(MathHelper.clamp(local4265_3.glow(), 0.0F, 2.0F) * 127.5F);
         this.intVal7 = Math.round(MathHelper.clamp(local4265_3.hit(), 0.0F, 1.0F) * 255.0F) << 8;
      }

      @Override
      public VertexConsumer color(int local4266_1, int local4266_2, int local4266_3, int local4266_4) {
         return this.setScanColor();
      }

      @Override
      public VertexConsumer normal(float local4267_1, float local4267_2, float local4267_3) {
         this.vertexConsumer3.normal(local4267_1, local4267_2, local4267_3);
         return this;
      }

      @Override
      public VertexConsumer color(int local4268_1) {
         return this.setScanColor();
      }

      @Override
      public VertexConsumer light(int local4269_1, int local4269_2) {
         this.vertexConsumer3.light(this.intVal6, this.intVal7 | Math.round(spanPC(this.floatVal, this.nested0451.floatVal3, this.nested0451.__) * 255.0F));
         return this;
      }

      @Override
      public VertexConsumer overlay(int local4270_1, int local4270_2) {
         this.vertexConsumer3.overlay(this.intVal4, this.intVal5);
         return this;
      }

      @Override
      public VertexConsumer vertex(float local4271_1, float local4271_2, float local4271_3) {
         this.floatVal = local4271_1;
         this.floatVal2 = local4271_2;
         this.vertexConsumer3.vertex(local4271_1, local4271_2, local4271_3);
         return this;
      }

      @Override
      public VertexConsumer lineWidth(float local4272_1) {
         this.vertexConsumer3.lineWidth(local4272_1);
         return this;
      }

      @Override
      public VertexConsumer texture(float local4273_1, float local4273_2) {
         this.vertexConsumer3.texture(local4273_1, local4273_2);
         return this;
      }

      private VertexConsumer setScanColor() {
         this.vertexConsumer3
            .color(
               this.intVal,
               this.intVal2,
               this.intVal3,
               Math.round(spanPC(this.floatVal2, this.nested0451.floatVal4, this.nested0451.floatVal5) * 255.0F)
            );
         return this;
      }
   }

   private static final class SubContext2 implements VertexConsumer {
      private float floatVal3 = Float.POSITIVE_INFINITY;
      private float floatVal4 = Float.POSITIVE_INFINITY;
      private float __ = Float.NEGATIVE_INFINITY;
      private float floatVal5 = Float.NEGATIVE_INFINITY;

      @Override
      public VertexConsumer overlay(int local4275_1, int local4275_2) {
         return this;
      }

      @Override
      public VertexConsumer normal(float local4276_1, float local4276_2, float local4276_3) {
         return this;
      }

      @Override
      public VertexConsumer vertex(float local4277_1, float local4277_2, float local4277_3) {
         this.floatVal3 = Math.min(this.floatVal3, local4277_1);
         this.floatVal4 = Math.min(this.floatVal4, local4277_2);
         this.__ = Math.max(this.__, local4277_1);
         this.floatVal5 = Math.max(this.floatVal5, local4277_2);
         return this;
      }

      @Override
      public VertexConsumer texture(float local4278_1, float local4278_2) {
         return this;
      }

      @Override
      public VertexConsumer light(int local4279_1, int local4279_2) {
         return this;
      }

      @Override
      public VertexConsumer color(int local4280_1, int local4280_2, int local4280_3, int local4280_4) {
         return this;
      }

      private boolean validSpan() {
         return Float.isFinite(this.floatVal3)
            && Float.isFinite(this.floatVal4)
            && this.__ - this.floatVal3 > 1.0E-4F
            && this.floatVal5 - this.floatVal4 > 1.0E-4F;
      }

      @Override
      public VertexConsumer lineWidth(float local4283_1) {
         return this;
      }

      @Override
      public VertexConsumer color(int local4284_1) {
         return this;
      }
   }
}
