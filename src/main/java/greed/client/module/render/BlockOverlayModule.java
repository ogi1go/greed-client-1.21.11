package greed.client.module.render;

import greed.client.setting.BooleanSetting;
import greed.client.util.Colors;
import greed.client.util.DepthSnapshot;
import greed.client.event.EventListener;
import greed.client.setting.LegacyNumberSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.util.RaycastUtils;
import greed.client.setting.ColorSetting;
import greed.client.render.ThemeColors;
import greed.client.RotationAngles;
import greed.client.setting.SectionSetting;
import greed.client.event.WorldRenderEvent;
import greed.client.GreedClient;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ModuleInfo(name = "BlockOverlay", description = "Выделяет грань блока под прицелом, видно сквозь блоки.", category = ModuleCategory.RENDER)
public final class BlockOverlayModule extends Module {
   private static final Logger LOGGER = LoggerFactory.getLogger("greed");
   private static BlockOverlayModule blockOverlayModule;

   public BooleanSetting fill;
   public BooleanSetting shaderFill;
   public LegacyNumberSetting fillOpacity;
   public LegacyNumberSetting lineWidth;
   public BooleanSetting animation;
   public LegacyNumberSetting speed;
   public BooleanSetting customColor;
   public ColorSetting colorSetting;

   private Box box2;
   private float floatVal;
   private long timeMillis;

   private static final RenderPipeline BLOCK_OVERLAY_SHADER_PIPELINE = RenderPipeline.builder()
           .withLocation(GreedClient.id("pipeline/block_overlay_shader"))
           .withVertexShader(GreedClient.id("core/block_overlay"))
           .withFragmentShader(GreedClient.id("core/block_overlay"))
           .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
           .withBlend(BlendFunction.TRANSLUCENT)
           .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
           .withDepthWrite(false)
           .withCull(false)
           .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
           .withUniform("Projection", UniformType.UNIFORM_BUFFER)
           .withUniform("BlockOverlayParams", UniformType.UNIFORM_BUFFER)
           .build();

   private GpuBuffer shaderVertexBuffer;
   private GpuBuffer shaderUniformBuffer;

   private final EventListener<WorldRenderEvent> worldRenderListener = worldRenderEvent -> {
      if (mc.player != null && mc.world != null) {
         float local5973_2 = Math.max(1.0F, Math.min((float)(System.currentTimeMillis() - this.timeMillis), 100.0F)) / 16.666666F;
         this.timeMillis = System.currentTimeMillis();
         BlockHitResult blockHitResult = mc.world
                 .raycast(
                         new RaycastContext(
                                 mc.player.getCameraPosVec(worldRenderEvent.computeFloat()),
                                 mc.player
                                         .getCameraPosVec(worldRenderEvent.computeFloat())
                                         .add(
                                                 new RotationAngles(RotationAngles.getCameraYaw(), RotationAngles.getCameraPitch())
                                                         .getDirectionVector()
                                                         .multiply(mc.player.getBlockInteractionRange())
                                         ),
                                 ShapeType.OUTLINE,
                                 FluidHandling.NONE,
                                 mc.player
                         )
                 );
         Box box = blockHitResult.getType() == Type.BLOCK && !mc.world.getBlockState(blockHitResult.getBlockPos()).isAir()
                 ? (
                 mc.world.getBlockState(blockHitResult.getBlockPos()).getOutlineShape(mc.world, blockHitResult.getBlockPos()).isEmpty()
                         ? new Box(blockHitResult.getBlockPos()).expand(0.002)
                         : mc.world
                         .getBlockState(blockHitResult.getBlockPos())
                         .getOutlineShape(mc.world, blockHitResult.getBlockPos())
                         .getBoundingBox()
                         .offset(blockHitResult.getBlockPos())
                         .expand(0.002)
         )
                 : null;
         if (box != null && this.matchesCriteria(mc.player.getCameraPosVec(worldRenderEvent.computeFloat()), blockHitResult.getPos())) {
            box = null;
         }

         if (box != null) {
            box = this.getBox(box, blockHitResult.getSide());
         }

         if (box != null) {
            this.box2 = this.box2 != null && !(this.floatVal < 0.003921569F) && this.animation.isChecked()
                    ? new Box(
                    MathHelper.lerp(Math.min(1.0F, this.speed.getValue() * local5973_2), this.box2.minX, box.minX),
                    MathHelper.lerp(Math.min(1.0F, this.speed.getValue() * local5973_2), this.box2.minY, box.minY),
                    MathHelper.lerp(Math.min(1.0F, this.speed.getValue() * local5973_2), this.box2.minZ, box.minZ),
                    MathHelper.lerp(Math.min(1.0F, this.speed.getValue() * local5973_2), this.box2.maxX, box.maxX),
                    MathHelper.lerp(Math.min(1.0F, this.speed.getValue() * local5973_2), this.box2.maxY, box.maxY),
                    MathHelper.lerp(Math.min(1.0F, this.speed.getValue() * local5973_2), this.box2.maxZ, box.maxZ)
            )
                    : box;
         }

         this.floatVal = MathHelper.clamp(this.floatVal + (box == null ? -0.22F : 0.22F) * local5973_2, 0.0F, 1.0F);
         if (this.box2 != null && !(this.floatVal < 0.003921569F)) {
            DepthSnapshot.updateState2();
            worldRenderEvent.getMatrixStack().push();
            worldRenderEvent.getMatrixStack()
                    .translate(
                            this.box2.minX - mc.gameRenderer.getCamera().getCameraPos().x,
                            this.box2.minY - mc.gameRenderer.getCamera().getCameraPos().y,
                            this.box2.minZ - mc.gameRenderer.getCamera().getCameraPos().z
                    );
            Matrix4f positionMatrix = worldRenderEvent.getMatrixStack().peek().getPositionMatrix();

            if (this.fill.isChecked() && this.fillOpacity.getValue() > 0.001F) {
               if (this.shaderFill.isChecked()) {
                  try {
                     renderShaderFill(positionMatrix);
                  } catch (Throwable throwable) {
                     LOGGER.error("BlockOverlay shader fill failed", throwable);
                  }
               } else {
                  VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.debugQuads());
                  float[] floatArray = new float[]{0.0F, (float)(this.box2.maxX - this.box2.minX)};
                  float[] floatArray2 = new float[]{0.0F, (float)(this.box2.maxY - this.box2.minY)};
                  float[] floatArray3 = new float[]{0.0F, (float)(this.box2.maxZ - this.box2.minZ)};
                  int[][] intArrayArray = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
                  int local5973_11 = floatArray[1] < 0.01F ? 0 : (floatArray2[1] < 0.01F ? 1 : (floatArray3[1] < 0.01F ? 2 : -1));

                  for (int index = 0; index < intArrayArray.length; index++) {
                     if (local5973_11 < 0 || index == local5973_11 * 2) {
                        for (int local5973_16 : intArrayArray[index]) {
                           vertexConsumer.vertex(positionMatrix, floatArray[local5973_16 >> 2], floatArray2[local5973_16 >> 1 & 1], floatArray3[local5973_16 & 1])
                                   .color(
                                           Colors.computeInt6(
                                                   this.customColor.isChecked() ? this.colorSetting.getValue() : ThemeColors.intVal9,
                                                   this.fillOpacity.getValue() * this.floatVal
                                           )
                                   );
                        }
                     }
                  }
               }
            }

            VertexRendering.drawOutline(
                    worldRenderEvent.getMatrixStack(),
                    mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines()),
                    VoxelShapes.cuboid(
                            new Box(
                                    0.0,
                                    0.0,
                                    0.0,
                                    this.box2.maxX - this.box2.minX,
                                    this.box2.maxY - this.box2.minY,
                                    this.box2.maxZ - this.box2.minZ
                            )
                    ),
                    0.0,
                    0.0,
                    0.0,
                    Colors.computeInt6(this.customColor.isChecked() ? this.colorSetting.getValue() : ThemeColors.intVal9, this.floatVal),
                    this.lineWidth.getValue()
            );
            mc.getBufferBuilders().getEntityVertexConsumers().draw();
            worldRenderEvent.getMatrixStack().pop();
         }
      }
   };

   private void renderShaderFill(Matrix4f positionMatrix) {
      Framebuffer framebuffer = mc.getFramebuffer();
      if (framebuffer == null || framebuffer.getColorAttachmentView() == null) return;

      float dX = (float)(this.box2.maxX - this.box2.minX);
      float dY = (float)(this.box2.maxY - this.box2.minY);
      float dZ = (float)(this.box2.maxZ - this.box2.minZ);

      float[] fX = new float[]{0.0F, dX};
      float[] fY = new float[]{0.0F, dY};
      float[] fZ = new float[]{0.0F, dZ};
      int[][] intArrayArray = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
      int targetFace = fX[1] < 0.01F ? 0 : (fY[1] < 0.01F ? 2 : (fZ[1] < 0.01F ? 4 : 0));

      int[] indices = intArrayArray[targetFace];
      float[] uvs = {0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f};
      int colorInt = Colors.computeInt6(this.customColor.isChecked() ? this.colorSetting.getValue() : ThemeColors.intVal9, this.fillOpacity.getValue() * this.floatVal);

      try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize())) {
         BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         for (int i = 0; i < 4; i++) {
            int idx = indices[i];
            float vx = fX[idx >> 2];
            float vy = fY[idx >> 1 & 1];
            float vz = fZ[idx & 1];

            bufferBuilder.vertex(positionMatrix, vx, vy, vz)
                    .texture(uvs[i * 2], uvs[i * 2 + 1])
                    .color(colorInt);
         }

         try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
            if (this.shaderVertexBuffer != null && !this.shaderVertexBuffer.isClosed()) {
               this.shaderVertexBuffer.close();
            }
            this.shaderVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_block_overlay_shader_vb", GpuBuffer.USAGE_VERTEX, builtBuffer.getBuffer());
         }
      }

      int rgbVal = this.customColor.isChecked() ? this.colorSetting.getValue() : ThemeColors.intVal9;
      updateShaderUniformBuffer(framebuffer, rgbVal, this.fillOpacity.getValue() * this.floatVal);

      GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().write(
              RenderSystem.getModelViewMatrix(),
              new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
              new Vector3f(),
              new Matrix4f()
      );

      try (RenderPass renderPass = RenderSystem.getDevice()
              .createCommandEncoder()
              .createRenderPass(
                      () -> "greed_block_overlay_shader",
                      framebuffer.getColorAttachmentView(),
                      OptionalInt.empty(),
                      framebuffer.getDepthAttachmentView(),
                      OptionalDouble.empty()
              )) {
         renderPass.setPipeline(BLOCK_OVERLAY_SHADER_PIPELINE);
         RenderSystem.bindDefaultUniforms(renderPass);
         renderPass.setUniform("DynamicTransforms", dynamicTransforms);
         renderPass.setUniform("BlockOverlayParams", this.shaderUniformBuffer);
         renderPass.setVertexBuffer(0, this.shaderVertexBuffer);
         renderPass.setIndexBuffer(
                 RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6),
                 RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
         );
         renderPass.drawIndexed(0, 0, 6, 1);
      }
   }

   private void updateShaderUniformBuffer(Framebuffer framebuffer, int colorRgb, float fillStrength) {
      if (this.shaderUniformBuffer == null || this.shaderUniformBuffer.isClosed()) {
         this.shaderUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_block_overlay_shader_params", 136, 128L);
      }

      try (MemoryStack memoryStack = MemoryStack.stackPush()) {
         ByteBuffer byteBuffer = memoryStack.calloc(128);

         float r = (colorRgb >> 16 & 0xFF) / 255.0F;
         float g = (colorRgb >> 8 & 0xFF) / 255.0F;
         float b = (colorRgb & 0xFF) / 255.0F;

         byteBuffer.putFloat(0, r);
         byteBuffer.putFloat(4, g);
         byteBuffer.putFloat(8, b);

         byteBuffer.putFloat(16, 1.0f - r);
         byteBuffer.putFloat(20, 1.0f - g);
         byteBuffer.putFloat(24, 1.0f - b);

         byteBuffer.putFloat(32, (float)(System.currentTimeMillis() % 100000L) / 1000.0F);
         byteBuffer.putFloat(36, 1.2f);
         byteBuffer.putFloat(40, 1.0f);
         byteBuffer.putFloat(44, 1.1f);
         byteBuffer.putFloat(48, 1.0f);
         byteBuffer.putFloat(52, fillStrength);
         byteBuffer.putFloat(56, 1.0f);
         byteBuffer.putFloat(60, 0.0f);

         float texWidth = framebuffer != null && framebuffer.textureWidth > 0 ? framebuffer.textureWidth : 1.0f;
         float texHeight = framebuffer != null && framebuffer.textureHeight > 0 ? framebuffer.textureHeight : 1.0f;
         byteBuffer.putFloat(64, 1.0f / texWidth);
         byteBuffer.putFloat(68, 1.0f / texHeight);

         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.shaderUniformBuffer.slice(0L, 128L), byteBuffer);
      }
   }

   private void cleanupShaderBuffers() {
      if (this.shaderVertexBuffer != null && !this.shaderVertexBuffer.isClosed()) {
         this.shaderVertexBuffer.close();
         this.shaderVertexBuffer = null;
      }
      if (this.shaderUniformBuffer != null && !this.shaderUniformBuffer.isClosed()) {
         this.shaderUniformBuffer.close();
         this.shaderUniformBuffer = null;
      }
   }

   private Box getBox(Box box, Direction direction) {
      return switch (direction) {
         case DOWN -> new Box(box.minX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ);
         case UP -> new Box(box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ);
         case NORTH -> new Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ);
         case SOUTH -> new Box(box.minX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ);
         case WEST -> new Box(box.minX, box.minY, box.minZ, box.minX, box.maxY, box.maxZ);
         case EAST -> new Box(box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
      };
   }

   private boolean matchesCriteria(Vec3d vec3d, Vec3d vec3d2) {
      return RaycastUtils.getEntityHitResult2(
              mc.player,
              vec3d,
              vec3d2,
              mc.player.getBoundingBox().stretch(vec3d2.subtract(vec3d)).expand(1.0, 1.0, 1.0),
              entity -> !entity.isSpectator() && entity.canHit(),
              vec3d.distanceTo(vec3d2)
      )
              != null;
   }

   private BlockOverlayModule() {
      this.addSetting(new SectionSetting("Бокс"));

      this.fill = this.addSetting(new BooleanSetting("Заливка", "Заливать грань цветом.", true));
      this.shaderFill = this.addSetting(new BooleanSetting("Заливка шейдером", "Заливать грань шейдером", true));
      this.fillOpacity = this.addSetting(new LegacyNumberSetting("Прозрачность заливки", "Насколько плотно залиты грани.", 0.25, 0.0, 1.0, 0.05));
      this.lineWidth = this.addSetting(new LegacyNumberSetting("Толщина линии", "Толщина линий оутлайна.", 2.0, 0.5, 6.0, 0.1));
      this.addSetting(new SectionSetting("Анимация"));
      this.animation = this.addSetting(new BooleanSetting("Анимация", "Бокс плавно переезжает по x, y, z на новый блок вместо мгновенного прыжка.", true));
      this.speed = this.addSetting(new LegacyNumberSetting("Скорость", "Как быстро бокс догоняет новый блок.", 0.35, 0.05, 1.0, 0.05));
      this.addSetting(new SectionSetting("Цвет"));
      this.customColor = this.addSetting(new BooleanSetting("Свой цвет", "Использовать свой цвет вместо ACCENT.", false));
      this.colorSetting = this.addSetting(new ColorSetting("Цвет", "Цвет бокса."));

      this.fillOpacity.withVisibility(() -> this.fill.isChecked());
      this.shaderFill.withVisibility(() -> this.fill.isChecked());
      this.speed.withVisibility(() -> this.animation.isChecked());
      this.colorSetting.withVisibility(() -> this.customColor.isChecked());
   }

   @Override
   protected void onDisable() {
      this.box2 = null;
      this.floatVal = 0.0F;
      this.cleanupShaderBuffers();
   }

   public static BlockOverlayModule getInstance() {
      if (blockOverlayModule == null) {
         blockOverlayModule = new BlockOverlayModule();
      }

      return blockOverlayModule;
   }
}