package greed.client.render;

import greed.client.GreedClient;
import greed.client.event.WorldRenderEvent;
import greed.client.module.render.ChamsModule;
import greed.client.storage.FriendStore;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
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
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public final class ChamsWorldRenderer {
   private static ChamsWorldRenderer chamsWorldRenderer;
   private static final Vector4f white = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f zero = new Vector3f();
   private static final Matrix4f identity = new Matrix4f();
   private static final Matrix4f identity2 = new Matrix4f();
   private static final int[][] faces = new int[][]{{0, 2, 6, 4}, {1, 5, 7, 3}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 6, 7, 5}};
   private static final RenderPipeline fillPipeline = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(GreedClient.id("pipeline/chams_fill"))
      .withVertexShader(GreedClient.id("core/chams/chams_fill"))
      .withFragmentShader(GreedClient.id("core/chams/chams_fill"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withUniform("ChamsFillParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline linePipeline = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(GreedClient.id("pipeline/chams_lines"))
      .withVertexShader(Identifier.ofVanilla("core/position_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_color"))
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   private final long startMillis = System.currentTimeMillis();
   private final Vector3f[] corners = new Vector3f[8];
   private final float[] localX = new float[8];
   private final float[] localY = new float[8];
   private final float[] localZ = new float[8];
   private GpuBuffer fillBuffer;
   private GpuBuffer lineBuffer;
   private GpuBuffer paramsBuffer;
   private BufferAllocator fillAllocator;
   private BufferAllocator lineAllocator;
   private BufferBuilder fillBuilder;
   private BufferBuilder lineBuilder;
   private int fillQuads;
   private int lineQuads;
   private int fillColor;
   private int lineColor;
   private boolean glowEnabled;
   private float glowWidth;
   private float lineWidth;

   private ChamsWorldRenderer() {
      for (int index = 0; index < 8; index++) {
         this.corners[index] = new Vector3f();
      }
   }

   public static ChamsWorldRenderer getInstance() {
      if (chamsWorldRenderer == null) {
         chamsWorldRenderer = new ChamsWorldRenderer();
      }

      return chamsWorldRenderer;
   }

   public void release() {
      if (this.fillBuffer != null && !this.fillBuffer.isClosed()) {
         this.fillBuffer.close();
      }

      this.fillBuffer = null;
      if (this.lineBuffer != null && !this.lineBuffer.isClosed()) {
         this.lineBuffer.close();
      }

      this.lineBuffer = null;
      if (this.paramsBuffer != null && !this.paramsBuffer.isClosed()) {
         this.paramsBuffer.close();
      }

      this.paramsBuffer = null;
      if (this.fillAllocator != null) {
         this.fillAllocator.close();
         this.fillAllocator = null;
      }

      if (this.lineAllocator != null) {
         this.lineAllocator.close();
         this.lineAllocator = null;
      }
   }

   public void render(WorldRenderEvent worldRenderEvent) {
      MinecraftClient mc = MinecraftClient.getInstance();
      ChamsModule module = ChamsModule.getInstance();
      if (mc.player == null
         || mc.world == null
         || mc.gameRenderer == null
         || mc.getFramebuffer() == null
         || mc.getFramebuffer().getColorAttachmentView() == null) {
         return;
      }

      if (this.fillAllocator == null) {
         this.fillAllocator = new BufferAllocator(2048 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
      }

      if (this.lineAllocator == null) {
         this.lineAllocator = new BufferAllocator(4096 * VertexFormats.POSITION_COLOR.getVertexSize());
      }

      CloseableBuffer fillAllocated = this.fillAllocator.getAllocated();
      if (fillAllocated != null) {
         fillAllocated.close();
      }

      CloseableBuffer lineAllocated = this.lineAllocator.getAllocated();
      if (lineAllocated != null) {
         lineAllocated.close();
      }

      this.fillBuilder = new BufferBuilder(this.fillAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      this.lineBuilder = new BufferBuilder(this.lineAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      this.fillQuads = 0;
      this.lineQuads = 0;
      this.glowEnabled = module.glow.isChecked();
      this.glowWidth = 1.0F + module.glowIntensity.getValue() * 0.45F;

      float tickDelta = worldRenderEvent.computeFloat();
      MatrixStack matrixStack = worldRenderEvent.getMatrixStack();
      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      float pulse = module.pulseMultiplier();
      int fillAlpha = MathHelper.clamp(Math.round(module.fill.getValue() * 255.0F * pulse), 0, 255);
      int lineAlpha = MathHelper.clamp(Math.round(module.outline.getValue() * 255.0F * pulse), 0, 255);
      int baseRgb = module.computeColor() & 0xFFFFFF;
      int friendRgb = module.friendsColor.getValue() & 0xFFFFFF;
      this.lineWidth = lineAlpha > 0 ? 1.0F : 0.0F;

      for (Entity entity : mc.world.getEntities()) {
         if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isAlive() || !module.matchesCriteria(livingEntity)) {
            continue;
         }

         boolean friend = livingEntity instanceof AbstractClientPlayerEntity friendCheck
            && FriendStore.isFriend(friendCheck.getGameProfile().name());
         int rgb = friend ? friendRgb : baseRgb;
         this.fillColor = vivid(rgb, 1.18F, 1.12F, fillAlpha);
         this.lineColor = vivid(rgb, 1.12F, 1.08F, lineAlpha);

         if (livingEntity instanceof AbstractClientPlayerEntity player) {
            this.renderPlayer(mc, matrixStack, player, tickDelta, cameraPos);
         } else {
            this.renderBounds(matrixStack, livingEntity, tickDelta, cameraPos);
         }
      }

      BuiltBuffer fillBuilt = this.fillBuilder.endNullable();
      BuiltBuffer lineBuilt = this.lineBuilder.endNullable();
      this.fillBuilder = null;
      this.lineBuilder = null;

      try {
         if (fillBuilt != null && this.fillQuads > 0) {
            this.fillBuffer = this.upload(this.fillBuffer, fillBuilt, "greed_chams_fill");
            this.writeParams(module);
            this.draw(mc, fillPipeline, this.fillBuffer, this.fillQuads, true);
         }

         if (lineBuilt != null && this.lineQuads > 0) {
            this.lineBuffer = this.upload(this.lineBuffer, lineBuilt, "greed_chams_lines");
            this.draw(mc, linePipeline, this.lineBuffer, this.lineQuads, false);
         }
      } finally {
         if (fillBuilt != null) {
            fillBuilt.close();
         }

         if (lineBuilt != null) {
            lineBuilt.close();
         }
      }
   }

   private GpuBuffer upload(GpuBuffer existing, BuiltBuffer builtBuffer, String name) {
      if (existing != null && !existing.isClosed() && existing.size() >= builtBuffer.getBuffer().remaining()) {
         RenderSystem.getDevice()
            .createCommandEncoder()
            .writeToBuffer(existing.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
         return existing;
      }

      if (existing != null && !existing.isClosed()) {
         existing.close();
      }

      return RenderSystem.getDevice().createBuffer(() -> name, 40, builtBuffer.getBuffer());
   }

   private void writeParams(ChamsModule module) {
      if (this.paramsBuffer == null || this.paramsBuffer.isClosed()) {
         this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_chams_fill_params", 136, 32L);
      }

      float time = (float)((System.currentTimeMillis() - this.startMillis) % 1000000L) / 1000.0F;

      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         ByteBuffer byteBuffer = memoryStack.calloc(32);
         byteBuffer.putFloat(0, time);
         byteBuffer.putFloat(4, module.waveSpeedX.getValue());
         byteBuffer.putFloat(8, module.waveSpeedY.getValue());
         byteBuffer.putFloat(12, module.waveScale.getValue());
         byteBuffer.putFloat(16, module.waveDensity.getValue());
         byteBuffer.putFloat(20, module.waveGlow.getValue());
         byteBuffer.putFloat(24, module.waves.isChecked() ? 1.0F : 0.0F);
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.paramsBuffer.slice(0L, 32L), byteBuffer);
      }
   }

   private void draw(MinecraftClient mc, RenderPipeline pipeline, GpuBuffer buffer, int quads, boolean withParams) {
      GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().write(identity, white, zero, identity2);

      try (RenderPass renderPass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> "greed_chams", mc.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
         renderPass.setPipeline(pipeline);
         renderPass.setUniform("DynamicTransforms", dynamicTransforms);
         renderPass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
         if (withParams) {
            renderPass.setUniform("ChamsFillParams", this.paramsBuffer);
         }

         renderPass.setVertexBuffer(0, buffer);
         renderPass.setIndexBuffer(
            RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(quads * 6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
         );
         renderPass.drawIndexed(0, 0, quads * 6, 1);
      }
   }

   private void renderPlayer(MinecraftClient mc, MatrixStack matrixStack, AbstractClientPlayerEntity player, float tickDelta, Vec3d cameraPos) {
      PlayerEntityRenderer<AbstractClientPlayerEntity> renderer = (PlayerEntityRenderer<AbstractClientPlayerEntity>)mc.getEntityRenderDispatcher()
         .<AbstractClientPlayerEntity>getRenderer(player);
      PlayerEntityRenderState renderState = renderer.createRenderState();
      renderer.updateRenderState(player, renderState, tickDelta);
      PlayerEntityModel model = renderer.getModel();
      model.setAngles(renderState);

      Vec3d lerpedPos = player.getLerpedPos(tickDelta);
      float scale = player.getScale();
      float pitch = player.getPitch(tickDelta);
      float bodyYaw = MathHelper.lerp(tickDelta, player.lastBodyYaw, player.bodyYaw);

      matrixStack.push();
      matrixStack.translate(lerpedPos.x - cameraPos.x, lerpedPos.y - cameraPos.y, lerpedPos.z - cameraPos.z);
      matrixStack.scale(scale, scale, scale);
      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
      if (player.isGliding()) {
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F - pitch));
      } else if (player.isInSwimmingPose()) {
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(player.isTouchingWater() ? -90.0F - pitch : -90.0F));
         matrixStack.translate(0.0F, -1.0F, 0.3F);
      }

      matrixStack.scale(-1.0F, -1.0F, 1.0F);
      matrixStack.scale(0.9375F, 0.9375F, 0.9375F);
      matrixStack.translate(0.0F, -1.501F, 0.0F);

      ModelPart root = model.getRootPart();
      this.renderPart(matrixStack, root, model.head, -4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F);
      this.renderPart(matrixStack, root, model.body, -4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F);
      this.renderPart(matrixStack, root, model.rightArm, -3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F);
      this.renderPart(matrixStack, root, model.leftArm, -1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F);
      this.renderPart(matrixStack, root, model.rightLeg, -2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F);
      this.renderPart(matrixStack, root, model.leftLeg, -2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F);
      matrixStack.pop();
   }

   private void renderPart(
      MatrixStack matrixStack, ModelPart root, ModelPart part, float offX, float offY, float offZ, float width, float height, float depth
   ) {
      matrixStack.push();
       root.applyTransform(matrixStack);
       part.applyTransform(matrixStack);
      float unit = 1.0F / 16.0F;
      this.emitBox(
         matrixStack.peek().getPositionMatrix(),
         offX * unit,
         offY * unit,
         offZ * unit,
         (offX + width) * unit,
         (offY + height) * unit,
         (offZ + depth) * unit
      );
      matrixStack.pop();
   }

   private void renderBounds(MatrixStack matrixStack, LivingEntity entity, float tickDelta, Vec3d cameraPos) {
      Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
      Box box = entity.getBoundingBox();
      matrixStack.push();
      matrixStack.translate(lerpedPos.x - cameraPos.x, lerpedPos.y - cameraPos.y, lerpedPos.z - cameraPos.z);
      this.emitBox(
         matrixStack.peek().getPositionMatrix(),
         (float)(box.minX - entity.getX()),
         (float)(box.minY - entity.getY()),
         (float)(box.minZ - entity.getZ()),
         (float)(box.maxX - entity.getX()),
         (float)(box.maxY - entity.getY()),
         (float)(box.maxZ - entity.getZ())
      );
      matrixStack.pop();
   }

   private void emitBox(Matrix4f matrix, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
      for (int index = 0; index < 8; index++) {
         this.localX[index] = (index & 1) == 0 ? minX : maxX;
         this.localY[index] = (index & 2) == 0 ? minY : maxY;
         this.localZ[index] = (index & 4) == 0 ? minZ : maxZ;
         this.corners[index].set(this.localX[index], this.localY[index], this.localZ[index]);
         matrix.transformPosition(this.corners[index]);
      }

      if (((this.fillColor >>> 24) & 0xFF) > 0) {
         for (int[] face : faces) {
            for (int vertex : face) {
               Vector3f position = this.corners[vertex];
               this.fillBuilder
                  .vertex(position.x, position.y, position.z)
                  .texture(waveU(this.localX[vertex], this.localY[vertex], this.localZ[vertex]), waveV(this.localX[vertex], this.localY[vertex], this.localZ[vertex]))
                  .color(this.fillColor);
            }

            this.fillQuads++;
         }
      }

      if (this.lineWidth > 0.0F) {
         for (int bit = 1; bit <= 4; bit <<= 1) {
            for (int index = 0; index < 8; index++) {
               if ((index & bit) == 0) {
                  this.emitLine(this.corners[index], this.corners[index | bit]);
               }
            }
         }
      }
   }

   private void emitLine(Vector3f from, Vector3f to) {
      float dx = to.x - from.x;
      float dy = to.y - from.y;
      float dz = to.z - from.z;
      float midX = (from.x + to.x) * 0.5F;
      float midY = (from.y + to.y) * 0.5F;
      float midZ = (from.z + to.z) * 0.5F;
      float crossX = dy * midZ - dz * midY;
      float crossY = dz * midX - dx * midZ;
      float crossZ = dx * midY - dy * midX;
      float crossLength = MathHelper.sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ);
      if (crossLength < 1.0E-6F) {
         return;
      }

      float distance = MathHelper.sqrt(midX * midX + midY * midY + midZ * midZ);
      float half = Math.max(0.004F, distance * 0.0011F * MathHelper.clamp(1.35F - distance / 80.0F, 0.5F, 1.0F)) / crossLength;
      if (this.glowEnabled) {
         int glowColor = this.lineColor & 0xFFFFFF | (int)(((this.lineColor >>> 24) & 0xFF) * 0.3F) << 24;
         this.addLineQuad(from, to, crossX, crossY, crossZ, half * this.glowWidth, glowColor);
      }

      this.addLineQuad(from, to, crossX, crossY, crossZ, half, this.lineColor);
   }

   private void addLineQuad(Vector3f from, Vector3f to, float crossX, float crossY, float crossZ, float half, int color) {
      this.lineBuilder.vertex(from.x - crossX * half, from.y - crossY * half, from.z - crossZ * half).color(color);
      this.lineBuilder.vertex(from.x + crossX * half, from.y + crossY * half, from.z + crossZ * half).color(color);
      this.lineBuilder.vertex(to.x + crossX * half, to.y + crossY * half, to.z + crossZ * half).color(color);
      this.lineBuilder.vertex(to.x - crossX * half, to.y - crossY * half, to.z - crossZ * half).color(color);
      this.lineQuads++;
   }

   private static float waveU(float x, float y, float z) {
      return x * 1.15F + z * 0.72F;
   }

   private static float waveV(float x, float y, float z) {
      return y * 1.05F - z * 0.38F + x * 0.18F;
   }

   private static int vivid(int rgb, float saturationBoost, float brightnessBoost, int alpha) {
      float[] hsb = Color.RGBtoHSB(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, null);
      float saturation = MathHelper.clamp(hsb[1] * saturationBoost, 0.0F, 1.0F);
      float brightness = MathHelper.clamp(Math.max(hsb[2], 0.8F) * brightnessBoost, 0.0F, 1.0F);
      return alpha << 24 | Color.HSBtoRGB(hsb[0], saturation, brightness) & 0xFFFFFF;
   }
}
