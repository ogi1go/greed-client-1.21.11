package greed.client.module.render;

import greed.client.setting.BooleanSetting;
import greed.client.GreedClient;
import greed.client.event.EventListener;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.NumberSetting;
import greed.client.event.PacketEvent;
import greed.client.render.WorldProjection;
import greed.client.event.WorldRenderEvent;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.BufferAllocator.CloseableBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

@ModuleInfo(name = "Torus", description = "Хромированное кольцо ударной волны из повреждённых сущностей.", category = ModuleCategory.RENDER)
public final class TorusModule extends Module {
   private static TorusModule torusModule;
   private static final RenderPipeline pipeline2 = RenderPipeline.builder()
      .withLocation(GreedClient.id("pipeline/torus"))
      .withVertexShader(GreedClient.id("core/torus"))
      .withFragmentShader(GreedClient.id("core/torus"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("TorusParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline pipeline3 = RenderPipeline.builder()
      .withLocation(GreedClient.id("pipeline/torus_through_walls"))
      .withVertexShader(GreedClient.id("core/torus"))
      .withFragmentShader(GreedClient.id("core/torus"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("TorusParams", UniformType.UNIFORM_BUFFER)
      .build();
   public BooleanSetting booleanSetting;
   public NumberSetting noiseFrequency;
   public NumberSetting lifetime;
   public NumberSetting size;
   private final ConcurrentLinkedQueue<Integer> concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
   private final ArrayList<TorusModule.DataRecord> list = new ArrayList<>();
   private SimpleFramebuffer framebuffer;
   private GpuBuffer gpuBuffer;
   private GpuBuffer gpuBuffer2;
   private BufferAllocator bufferAllocator;
   private final Vector3f vector3f3 = new Vector3f();
   private final Vector3f vector3f4 = new Vector3f();
   private static final int[][] intValArray = new int[][]{{0, 0}, {1, 0}, {1, 1}, {0, 1}};
   private static final double[] doubleValArray = new double[11];
   private static final double[] doubleValArray2 = new double[11];
   private static final double[] doubleValArray3 = new double[31];
   private static final double[] doubleValArray4 = new double[31];
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (packetEvent.isIncoming()
         && mc.player != null
         && packetEvent.getPacket() instanceof EntityDamageS2CPacket entityDamageS2CPacket
         && entityDamageS2CPacket.entityId() != mc.player.getId()) {
         if (entityDamageS2CPacket.sourceCauseId() == mc.player.getId() || entityDamageS2CPacket.sourceDirectId() == mc.player.getId()) {
            this.concurrentLinkedQueue.add(entityDamageS2CPacket.entityId());
         }
      }
   };
   private final EventListener<WorldRenderEvent> worldRenderListener = worldRenderEvent -> {
      if (mc.player != null && mc.world != null && mc.gameRenderer != null) {
         this.list.removeIf(local6963_0 -> (float)(System.currentTimeMillis() - local6963_0.spawnAtMs()) > local6963_0.lifetimeMs());

         Integer local6970_2;
         while ((local6970_2 = this.concurrentLinkedQueue.poll()) != null) {
            Entity entityById = mc.world.getEntityById(local6970_2);
            if (entityById != null) {
               Vec3d vec3d = entityById == mc.player
                  ? mc.player.getEyePos().add(mc.player.getRotationVector().multiply(1.2))
                  : (
                     mc.crosshairTarget instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() == entityById
                        ? entityHitResult.getPos()
                        : entityById.getEyePos()
                  );
               this.list
                  .add(
                     new TorusModule.DataRecord(
                        vec3d,
                        RotationAxis.POSITIVE_Y
                           .rotationDegrees(
                              (float)(-(Math.toDegrees(Math.atan2(vec3d.z - mc.player.getZ(), vec3d.x - mc.player.getX())) - 90.0))
                           )
                           .mul(
                              RotationAxis.POSITIVE_X
                                 .rotationDegrees(
                                    (float)(
                                       -Math.toDegrees(
                                          Math.atan2(
                                             vec3d.y - mc.player.getEyePos().y,
                                             Math.hypot(vec3d.x - mc.player.getX(), vec3d.z - mc.player.getZ())
                                          )
                                       )
                                    )
                                 )
                           ),
                        System.currentTimeMillis(),
                        this.lifetime.floatValue(),
                        this.size.floatValue()
                     )
                  );
            }
         }

         if (!this.list.isEmpty()
            && mc.getFramebuffer() != null
            && mc.getFramebuffer().getColorAttachmentView() != null
            && mc.getFramebuffer().textureWidth > 0
            && mc.getFramebuffer().textureHeight > 0) {
            boolean local6970_2x = false;
            int local6970_3 = 0;

            for (int index = this.list.size(); local6970_3 < index; local6970_3++) {
               if (this.matchesCriteria(this.list.get(local6970_3))) {
                  local6970_2x = true;
                  break;
               }
            }

            if (local6970_2x) {
               if (this.framebuffer == null) {
                  this.framebuffer = new SimpleFramebuffer(
                     "greed_torus_scene", mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight, false
                  );
               }

               if (this.framebuffer.textureWidth != mc.getFramebuffer().textureWidth
                  || this.framebuffer.textureHeight != mc.getFramebuffer().textureHeight) {
                  this.framebuffer.resize(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
               }

               RenderSystem.getDevice()
                  .createCommandEncoder()
                  .copyTextureToTexture(
                     mc.getFramebuffer().getColorAttachment(),
                     this.framebuffer.getColorAttachment(),
                     0,
                     0,
                     0,
                     0,
                     0,
                     mc.getFramebuffer().textureWidth,
                     mc.getFramebuffer().textureHeight
                  );
               local6970_3 = 0;
               if (this.bufferAllocator == null) {
                  this.bufferAllocator = new BufferAllocator(19200 * VertexFormats.POSITION_COLOR_NORMAL.getVertexSize());
               }

               CloseableBuffer allocated = this.bufferAllocator.getAllocated();
               if (allocated != null) {
                  allocated.close();
               }

               BufferBuilder bufferBuilder = new BufferBuilder(this.bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR_NORMAL);
               Vector3f vector3f = this.vector3f3;
               Vector3f vector3f2 = this.vector3f4;
               int[][] intArrayArray = intValArray;
               double local6970_9 = mc.gameRenderer.getCamera().getCameraPos().x;
               double local6970_11 = mc.gameRenderer.getCamera().getCameraPos().y;
               double local6970_13 = mc.gameRenderer.getCamera().getCameraPos().z;

               for (TorusModule.DataRecord local6970_16 : this.list) {
                  if (this.matchesCriteria(local6970_16)) {
                     float local6970_17 = Math.min(1.0F, (float)(System.currentTimeMillis() - local6970_16.spawnAtMs()) / local6970_16.lifetimeMs());
                     float local6970_18 = 0.6F * local6970_16.sizeMul()
                        + 2.0F * local6970_16.sizeMul() * (1.0F - (1.0F - local6970_17) * (1.0F - local6970_17) * (1.0F - local6970_17));
                     float local6970_19 = 0.4F * local6970_16.sizeMul() * (1.0F - local6970_17 * local6970_17 * local6970_17);
                     int local6970_20 = (int)(255.0F * (local6970_17 < 0.7F ? 1.0F : 1.0F - (local6970_17 - 0.7F) / 0.3F));

                     for (int index2 = 0; index2 < 10; index2++) {
                        for (int index3 = 0; index3 < 30; index3++) {
                           for (int[] intArray : intArrayArray) {
                              double local6970_27 = doubleValArray[index2 + intArray[0]];
                              double local6970_29 = doubleValArray2[index2 + intArray[0]];
                              double local6970_31 = doubleValArray3[index3 + intArray[1]];
                              double local6970_33 = doubleValArray4[index3 + intArray[1]];
                              vector3f2.set((float)(local6970_27 * local6970_31), (float)(local6970_27 * local6970_33), (float)local6970_29);
                              vector3f.set(
                                 (float)((local6970_18 + local6970_19 * local6970_27) * local6970_31),
                                 (float)((local6970_18 + local6970_19 * local6970_27) * local6970_33),
                                 (float)(local6970_19 * local6970_29)
                              );
                              local6970_16.facing().transform(vector3f2);
                              local6970_16.facing().transform(vector3f);
                              bufferBuilder.vertex(
                                    (float)(local6970_16.pos().x - local6970_9 + vector3f.x),
                                    (float)(local6970_16.pos().y - local6970_11 + vector3f.y),
                                    (float)(local6970_16.pos().z - local6970_13 + vector3f.z)
                                 )
                                 .color(255, 255, 255, local6970_20)
                                 .normal(vector3f2.x, vector3f2.y, vector3f2.z);
                           }

                           local6970_3++;
                        }
                     }
                  }
               }

               if (this.gpuBuffer2 == null || this.gpuBuffer2.isClosed()) {
                  this.gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "greed_torus_params", 136, 160L);
               }

               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = memoryStack.calloc(160);
                  worldRenderEvent.getMatrixStack().peek().getPositionMatrix().get(0, byteBuffer);
                  WorldProjection.getMatrix4f().get(64, byteBuffer);
                  byteBuffer.putFloat(128, (float)mc.gameRenderer.getCamera().getCameraPos().x);
                  byteBuffer.putFloat(132, (float)mc.gameRenderer.getCamera().getCameraPos().y);
                  byteBuffer.putFloat(136, (float)mc.gameRenderer.getCamera().getCameraPos().z);
                  byteBuffer.putFloat(140, this.noiseFrequency.floatValue() / 100.0F);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.gpuBuffer2.slice(0L, 160L), byteBuffer);
               } catch (Throwable throwable) {
                  if (memoryStack != null) {
                     try {
                        memoryStack.close();
                     } catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                     }
                  }

                  throw throwable;
               }

               if (memoryStack != null) {
                  memoryStack.close();
               }

               try (BuiltBuffer local6970_15x = bufferBuilder.end()) {
                  if (this.gpuBuffer != null && !this.gpuBuffer.isClosed() && this.gpuBuffer.size() >= local6970_15x.getBuffer().remaining()) {
                     RenderSystem.getDevice()
                        .createCommandEncoder()
                        .writeToBuffer(this.gpuBuffer.slice(0L, local6970_15x.getBuffer().remaining()), local6970_15x.getBuffer());
                  } else {
                     if (this.gpuBuffer != null && !this.gpuBuffer.isClosed()) {
                        this.gpuBuffer.close();
                     }

                     this.gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "greed_torus_vertices", 40, local6970_15x.getBuffer());
                  }

                  try (RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                           () -> "greed_torus",
                           mc.getFramebuffer().getColorAttachmentView(),
                           OptionalInt.empty(),
                           this.booleanSetting.isChecked() ? mc.getFramebuffer().getDepthAttachmentView() : null,
                           OptionalDouble.empty()
                        )) {
                     renderPass.setPipeline(this.booleanSetting.isChecked() ? pipeline2 : pipeline3);
                     renderPass.bindTexture("Sampler0", this.framebuffer.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                     renderPass.setUniform("TorusParams", this.gpuBuffer2);
                     renderPass.setVertexBuffer(0, this.gpuBuffer);
                     renderPass.setIndexBuffer(
                        RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(local6970_3 * 6),
                        RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                     );
                     renderPass.drawIndexed(0, 0, local6970_3 * 6, 1);
                  }
               }
            }
         }
      }
   };

   private TorusModule() {
      this.booleanSetting = this.addSetting(new BooleanSetting("Глубина", "Не позволяет показывать Torus сквозь блоки.", false));
      this.noiseFrequency = this.addSetting(
         new NumberSetting("Частота шума", "Сила эффекта шума на поверхности.").withRange(0.0F, 100.0F).withStep(1.0F).setValue(50.0F)
      );
      this.lifetime = this.addSetting(
         new NumberSetting("Время жизни", "Как долго тор остаётся видимым.").withRange(500.0F, 5000.0F).withStep(100.0F).setValue(1000.0F)
      );
      this.size = this.addSetting(
         new NumberSetting("Размер", "Общий множитель размера тора.").withRange(0.2F, 2.0F).withStep(0.1F).setValue(0.5F)
      );
   }

   static {
      for (int index4 = 0; index4 <= 10; index4++) {
         doubleValArray[index4] = Math.cos(index4 * (Math.PI / 5));
         doubleValArray2[index4] = Math.sin(index4 * (Math.PI / 5));
      }

      for (int index5 = 0; index5 <= 30; index5++) {
         doubleValArray3[index5] = Math.cos(index5 * (Math.PI / 15));
         doubleValArray4[index5] = Math.sin(index5 * (Math.PI / 15));
      }
   }

   private boolean matchesCriteria(TorusModule.DataRecord local6965_1) {
      return WorldProjection.matchesCriteria2(local6965_1.pos().x, local6965_1.pos().y, local6965_1.pos().z, 3.0F * local6965_1.sizeMul());
   }

   @Override
   protected void onDisable() {
      this.concurrentLinkedQueue.clear();
      this.list.clear();
      if (this.framebuffer != null) {
         this.framebuffer.delete();
         this.framebuffer = null;
      }

      if (this.gpuBuffer != null && !this.gpuBuffer.isClosed()) {
         this.gpuBuffer.close();
      }

      this.gpuBuffer = null;
      if (this.gpuBuffer2 != null && !this.gpuBuffer2.isClosed()) {
         this.gpuBuffer2.close();
      }

      this.gpuBuffer2 = null;
      if (this.bufferAllocator != null) {
         this.bufferAllocator.close();
         this.bufferAllocator = null;
      }
   }

   public static TorusModule getInstance() {
      if (torusModule == null) {
         torusModule = new TorusModule();
      }

      return torusModule;
   }

   private record DataRecord(Vec3d pos, Quaternionf facing, long spawnAtMs, float lifetimeMs, float sizeMul) {
   }
}
