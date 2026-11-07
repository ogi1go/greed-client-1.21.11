package greed.client.module.render;

import greed.client.setting.BooleanSetting;
import greed.client.GreedClient;
import greed.client.event.EventListener;
import greed.client.storage.FriendStore;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.ui.UiTheme;
import greed.client.setting.ColorSetting;
import greed.client.event.WorldRenderEvent;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.BufferAllocator.CloseableBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@ModuleInfo(name = "SkeletonEsp", description = "Рисует скелет игроков по позе их модели сквозь стены.", category = ModuleCategory.RENDER)
public final class SkeletonEspModule extends Module {
   private static SkeletonEspModule skeletonEspModule;
   private static final Vector4f vector4f = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f vector3f = new Vector3f();
   private static final Matrix4f matrix4f2 = new Matrix4f();
   private static final Matrix4f matrix4f3 = new Matrix4f();
   private static final RenderPipeline pipeline2 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(GreedClient.id("pipeline/skeleton_bones"))
      .withVertexShader(Identifier.ofVanilla("core/position_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_color"))
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   public BooleanSetting booleanSetting;
   public ColorSetting color;
   public ColorSetting friendsColor;
   private GpuBuffer gpuBuffer;
   private BufferAllocator bufferAllocator;
   private final ArrayList<AbstractClientPlayerEntity> list = new ArrayList<>();
   private final Vector3f vector3f2 = new Vector3f();
   private final Vector3f vector3f3 = new Vector3f();
   private final EventListener<WorldRenderEvent> worldRenderListener = worldRenderEvent -> {
      if (mc.player != null && mc.world != null) {
         ArrayList<AbstractClientPlayerEntity> local3893_2 = this.list;
         local3893_2.clear();

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity
               && (mc.options.getPerspective() != Perspective.FIRST_PERSON || abstractClientPlayerEntity != mc.player)) {
               local3893_2.add(abstractClientPlayerEntity);
            }
         }

         if (!local3893_2.isEmpty()) {
            if (this.bufferAllocator == null) {
               this.bufferAllocator = new BufferAllocator(520 * VertexFormats.POSITION_COLOR.getVertexSize());
            }

            CloseableBuffer allocated = this.bufferAllocator.getAllocated();
            if (allocated != null) {
               allocated.close();
            }

            BufferBuilder bufferBuilder = new BufferBuilder(this.bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int local3893_5 = 0;

            for (AbstractClientPlayerEntity abstractClientPlayerEntity2 : local3893_2) {
               local3893_5 += this.renderElement2(
                  worldRenderEvent.getMatrixStack(),
                  bufferBuilder,
                  abstractClientPlayerEntity2,
                  worldRenderEvent.computeFloat(),
                  FriendStore.isFriend(abstractClientPlayerEntity2.getGameProfile().name())
                     ? this.friendsColor.getValue()
                     : (this.booleanSetting.isChecked() ? this.color.getValue() : UiTheme.intVal7)
               );
            }

            this.processData(bufferBuilder, local3893_5);
            local3893_2.clear();
         }
      }
   };

   @Override
   protected void onDisable() {
      if (this.gpuBuffer != null && !this.gpuBuffer.isClosed()) {
         this.gpuBuffer.close();
      }

      this.gpuBuffer = null;
      if (this.bufferAllocator != null) {
         this.bufferAllocator.close();
         this.bufferAllocator = null;
      }

      this.list.clear();
   }

   public static SkeletonEspModule getInstance() {
      if (skeletonEspModule == null) {
         skeletonEspModule = new SkeletonEspModule();
      }

      return skeletonEspModule;
   }

   private int renderElement(
      MatrixStack matrixStack,
      BufferBuilder bufferBuilder,
      ModelPart modelPart,
      float local3891_4,
      float local3891_5,
      float local3891_6,
      float local3891_7,
      int local3891_8
   ) {
      matrixStack.push();
      matrixStack.translate(local3891_4, local3891_5, local3891_6);
      if (modelPart.roll != 0.0F) {
         matrixStack.multiply(RotationAxis.POSITIVE_Z.rotation(modelPart.roll));
      }

      if (modelPart.yaw != 0.0F) {
         matrixStack.multiply(RotationAxis.NEGATIVE_Y.rotation(modelPart.yaw));
      }

      if (modelPart.pitch != 0.0F) {
         matrixStack.multiply(RotationAxis.NEGATIVE_X.rotation(modelPart.pitch));
      }

      int local3891_9 = this.computeInt(bufferBuilder, matrixStack.peek().getPositionMatrix(), 0.0F, 0.0F, 0.0F, 0.0F, local3891_7, 0.0F, local3891_8);
      matrixStack.pop();
      return local3891_9;
   }

   private int computeInt(
      BufferBuilder bufferBuilder,
      Matrix4f matrix4f,
      float local3892_3,
      float local3892_4,
      float local3892_5,
      float local3892_6,
      float local3892_7,
      float local3892_8,
      int local3892_9
   ) {
      matrix4f.transformPosition(this.vector3f2.set(local3892_3, local3892_4, local3892_5));
      matrix4f.transformPosition(this.vector3f3.set(local3892_6, local3892_7, local3892_8));
      float local3892_10 = this.vector3f3.x - this.vector3f2.x;
      float local3892_11 = this.vector3f3.y - this.vector3f2.y;
      float local3892_12 = this.vector3f3.z - this.vector3f2.z;
      float local3892_13 = (this.vector3f2.x + this.vector3f3.x) * 0.5F;
      float local3892_14 = (this.vector3f2.y + this.vector3f3.y) * 0.5F;
      float local3892_15 = (this.vector3f2.z + this.vector3f3.z) * 0.5F;
      float local3892_16 = local3892_11 * local3892_15 - local3892_12 * local3892_14;
      float local3892_17 = local3892_12 * local3892_13 - local3892_10 * local3892_15;
      float local3892_18 = local3892_10 * local3892_14 - local3892_11 * local3892_13;
      float local3892_19 = MathHelper.sqrt(local3892_16 * local3892_16 + local3892_17 * local3892_17 + local3892_18 * local3892_18);
      if (local3892_19 < 1.0E-6F) {
         return 0;
      }

      float local3892_20 = MathHelper.sqrt(local3892_13 * local3892_13 + local3892_14 * local3892_14 + local3892_15 * local3892_15);
      float local3892_21 = Math.max(0.008F, local3892_20 * 0.0022F * MathHelper.clamp(1.35F - local3892_20 / 80.0F, 0.5F, 1.0F)) / local3892_19;
      this.processData2(
         bufferBuilder, local3892_16, local3892_17, local3892_18, local3892_21 * 1.9F, local3892_9 & 16777215 | (int)((local3892_9 >>> 24) * 0.3F) << 24
      );
      this.processData2(bufferBuilder, local3892_16, local3892_17, local3892_18, local3892_21, local3892_9);
      return 2;
   }

   private void processData(BufferBuilder bufferBuilder, int local3894_2) {
      BuiltBuffer builtBuffer = bufferBuilder.endNullable();
      if (builtBuffer != null) {
         try (builtBuffer) {
            if (local3894_2 != 0 && mc.getFramebuffer() != null && mc.getFramebuffer().getColorAttachmentView() != null) {
               if (this.gpuBuffer != null && !this.gpuBuffer.isClosed() && this.gpuBuffer.size() >= builtBuffer.getBuffer().remaining()) {
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .writeToBuffer(this.gpuBuffer.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
               } else {
                  if (this.gpuBuffer != null && !this.gpuBuffer.isClosed()) {
                     this.gpuBuffer.close();
                  }

                  this.gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_skeleton_bones", 40, builtBuffer.getBuffer());
               }

               GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().write(matrix4f3, vector4f, vector3f, matrix4f2);

               try (RenderPass renderPass = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(
                        () -> "greed_skeleton", mc.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                     )) {
                  renderPass.setPipeline(pipeline2);
                  renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                  renderPass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                  renderPass.setVertexBuffer(0, this.gpuBuffer);
                  renderPass.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(local3894_2 * 6),
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  renderPass.drawIndexed(0, 0, local3894_2 * 6, 1);
                  return;
               }
            }
         }
      }
   }

   private SkeletonEspModule() {
      this.booleanSetting = this.addSetting(new BooleanSetting("Свой цвет", "Красить кости своим цветом вместо ACCENT.", false));
      this.color = this.addSetting(new ColorSetting("Цвет", "Цвет костей скелета."));
      this.friendsColor = this.addSetting(new ColorSetting("Цвет друзей", "Цвет костей друзей.", new Color(64, 255, 128, 255)));
      this.color.withVisibility(() -> this.booleanSetting.isChecked());
   }

   private void processData2(BufferBuilder bufferBuilder, float local3896_2, float local3896_3, float local3896_4, float local3896_5, int local3896_6) {
      bufferBuilder.vertex(
            this.vector3f2.x - local3896_2 * local3896_5, this.vector3f2.y - local3896_3 * local3896_5, this.vector3f2.z - local3896_4 * local3896_5
         )
         .color(local3896_6);
      bufferBuilder.vertex(
            this.vector3f2.x + local3896_2 * local3896_5, this.vector3f2.y + local3896_3 * local3896_5, this.vector3f2.z + local3896_4 * local3896_5
         )
         .color(local3896_6);
      bufferBuilder.vertex(
            this.vector3f3.x + local3896_2 * local3896_5, this.vector3f3.y + local3896_3 * local3896_5, this.vector3f3.z + local3896_4 * local3896_5
         )
         .color(local3896_6);
      bufferBuilder.vertex(
            this.vector3f3.x - local3896_2 * local3896_5, this.vector3f3.y - local3896_3 * local3896_5, this.vector3f3.z - local3896_4 * local3896_5
         )
         .color(local3896_6);
   }

   private int renderElement2(MatrixStack matrixStack, BufferBuilder bufferBuilder, AbstractClientPlayerEntity abstractClientPlayerEntity, float local3897_4, int local3897_5) {
      PlayerEntityRenderer<AbstractClientPlayerEntity> local3897_6 = (PlayerEntityRenderer<AbstractClientPlayerEntity>)mc.getEntityRenderDispatcher()
         .<AbstractClientPlayerEntity>getRenderer(abstractClientPlayerEntity);
      PlayerEntityRenderState playerEntityRenderState = local3897_6.createRenderState();
      local3897_6.updateRenderState(abstractClientPlayerEntity, playerEntityRenderState, local3897_4);
      PlayerEntityModel model = local3897_6.getModel();
      model.setAngles(playerEntityRenderState);
      boolean isInSwimmingPose = abstractClientPlayerEntity.isInSwimmingPose();
      boolean isGliding = abstractClientPlayerEntity.isGliding();
      boolean local3897_11 = abstractClientPlayerEntity.isSneaking() && !abstractClientPlayerEntity.getAbilities().flying;
      float local3897_12 = local3897_11 ? 0.6F : 0.7F;
      float local3897_13 = local3897_11 ? 0.23F : 0.0F;
      float local3897_14 = local3897_11 ? 1.05F : 1.4F;
      float local3897_15 = local3897_11 ? 1.05F : 1.35F;
      matrixStack.push();
      Vec3d lerpedPos = abstractClientPlayerEntity.getLerpedPos(local3897_4);
      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      matrixStack.translate(lerpedPos.x - cameraPos.x, lerpedPos.y - cameraPos.y, lerpedPos.z - cameraPos.z);
      if (isInSwimmingPose) {
         matrixStack.translate(0.0F, 0.35F, 0.0F);
      }

      matrixStack.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(MathHelper.lerp(local3897_4, abstractClientPlayerEntity.lastBodyYaw, abstractClientPlayerEntity.bodyYaw) + 180.0F));
      if (isInSwimmingPose || isGliding) {
         matrixStack.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(90.0F + abstractClientPlayerEntity.getPitch(local3897_4)));
      }

      if (isInSwimmingPose) {
         matrixStack.translate(0.0F, -0.95F, 0.0F);
      }

      int local3897_18 = 0;
      local3897_18 += this.computeInt(
         bufferBuilder, matrixStack.peek().getPositionMatrix(), 0.0F, local3897_12, local3897_13, 0.0F, local3897_14, 0.0F, local3897_5
      );
      local3897_18 += this.computeInt(bufferBuilder, matrixStack.peek().getPositionMatrix(), -0.37F, local3897_15, 0.0F, 0.37F, local3897_15, 0.0F, local3897_5);
      local3897_18 += this.computeInt(
         bufferBuilder, matrixStack.peek().getPositionMatrix(), -0.15F, local3897_12, local3897_13, 0.15F, local3897_12, local3897_13, local3897_5
      );
      local3897_18 += this.renderElement(matrixStack, bufferBuilder, model.head, 0.0F, local3897_14, 0.0F, 0.15F, local3897_5);
      local3897_18 += this.renderElement(matrixStack, bufferBuilder, model.rightLeg, 0.15F, local3897_12, local3897_13, -0.6F, local3897_5);
      local3897_18 += this.renderElement(matrixStack, bufferBuilder, model.leftLeg, -0.15F, local3897_12, local3897_13, -0.6F, local3897_5);
      local3897_18 += this.renderElement(matrixStack, bufferBuilder, model.rightArm, 0.37F, local3897_15, 0.0F, -0.55F, local3897_5);
      local3897_18 += this.renderElement(matrixStack, bufferBuilder, model.leftArm, -0.37F, local3897_15, 0.0F, -0.55F, local3897_5);
      matrixStack.pop();
      return local3897_18;
   }
}
