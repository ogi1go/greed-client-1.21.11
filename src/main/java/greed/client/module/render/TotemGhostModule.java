package greed.client.module.render;

import greed.client.module.combat.AutoTotemModule;
import greed.client.util.Colors;
import greed.client.GreedClient;
import greed.client.event.EventListener;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.module.ModuleManager;
import greed.client.event.PacketEvent;
import greed.client.render.ThemeColors;
import greed.client.event.WorldRenderEvent;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents.AfterClientWorldChange;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

@ModuleInfo(name = "TotemGhost", description = "Призрак модельки на месте лопнувшего тотема.", category = ModuleCategory.RENDER)
public final class TotemGhostModule extends Module {
   private static TotemGhostModule totemGhostModule;
   private static final RenderPipeline pipeline2 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(GreedClient.id("pipeline/totem_ghost_fill"))
      .withVertexShader(Identifier.ofVanilla("core/position_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_color"))
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   private static final RenderPipeline pipeline3 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(GreedClient.id("pipeline/totem_ghost_lines"))
      .withVertexShader(Identifier.ofVanilla("core/position_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_color"))
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.DEBUG_LINES)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   private static final RenderPipeline pipeline4 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(GreedClient.id("pipeline/totem_ghost_glow"))
      .withVertexShader(Identifier.ofVanilla("core/position_tex_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_tex_color"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .build();
   private static final RenderLayer renderLayer = RenderLayer.of("totem_ghost_fill", RenderSetup.builder(pipeline2).expectedBufferSize(16384).build());
   private static final RenderLayer renderLayer2 = RenderLayer.of("totem_ghost_lines", RenderSetup.builder(pipeline3).expectedBufferSize(4096).build());
   private static final RenderLayer renderLayer3 = RenderLayer.of(
      "totem_ghost_glow", RenderSetup.builder(pipeline4).texture("Sampler0", GreedClient.id("textures/particles/glow.png")).expectedBufferSize(1024).build()
   );
   private static final float[][] floatValArray = new float[][]{
      {-0.25F, 1.5F, -0.25F, 0.25F, 2.0F, 0.25F},
      {-0.25F, 0.75F, -0.125F, 0.25F, 1.5F, 0.125F},
      {0.25F, 0.75F, -0.125F, 0.5F, 1.5F, 0.125F},
      {-0.5F, 0.75F, -0.125F, -0.25F, 1.5F, 0.125F},
      {0.0F, 0.0F, -0.125F, 0.25F, 0.75F, 0.125F},
      {-0.25F, 0.0F, -0.125F, 0.0F, 0.75F, 0.125F}
   };
   private static final float[][] floatValArray2 = new float[][]{
      {0.0F, 1.5F, 0.0F}, {0.0F, 0.75F, 0.0F}, {0.375F, 1.4F, 0.0F}, {-0.375F, 1.4F, 0.0F}, {0.125F, 0.75F, 0.0F}, {-0.125F, 0.75F, 0.0F}
   };
   private static final int[][] intValArray = new int[][]{{0, 1, 3, 2}, {4, 5, 7, 6}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}};
   private static final int[][] intValArray2 = new int[][]{{0, 1}, {1, 3}, {3, 2}, {2, 0}, {4, 5}, {5, 7}, {7, 6}, {6, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
   private final ArrayList<TotemGhostModule.SubContext> list = new ArrayList<>();
   private BufferAllocator bufferAllocator = new BufferAllocator(131072);
   private Immediate immediate2 = VertexConsumerProvider.immediate(this.bufferAllocator);
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (AutoTotemModule.getInstance().displayTotemPop.isChecked()
         && packetEvent.isIncoming()
         && packetEvent.getPacket() instanceof EntityStatusS2CPacket entityStatusS2CPacket
         && entityStatusS2CPacket.getStatus() == 35) {
         mc.execute(
            () -> {
               if (mc.world != null && entityStatusS2CPacket.getEntity(mc.world) != null && entityStatusS2CPacket.getEntity(mc.world) != mc.player) {
                  TotemGhostModule.SubContext local4062_2 = new TotemGhostModule.SubContext(
                     entityStatusS2CPacket.getEntity(mc.world).getEntityPos().x,
                     entityStatusS2CPacket.getEntity(mc.world).getEntityPos().y,
                     entityStatusS2CPacket.getEntity(mc.world).getEntityPos().z,
                     entityStatusS2CPacket.getEntity(mc.world).getYaw()
                  );
                  if (entityStatusS2CPacket.getEntity(mc.world) instanceof LivingEntity livingEntity) {
                     local4062_2.floatVal = livingEntity.bodyYaw;
                     local4062_2.floatVal2 = MathHelper.clamp(MathHelper.wrapDegrees(livingEntity.headYaw - livingEntity.bodyYaw), -75.0F, 75.0F);
                     local4062_2.floatValArray3[0] = livingEntity.getPitch();
                     local4062_2.floatValArray3[2] = (float)Math.toDegrees(
                        MathHelper.cos(livingEntity.limbAnimator.getAnimationProgress() * 0.6662F + (float) Math.PI)
                           * 2.0F
                           * Math.min(livingEntity.limbAnimator.getSpeed(), 1.0F)
                           * 0.5F
                     );
                     local4062_2.floatValArray3[3] = (float)Math.toDegrees(
                        MathHelper.cos(livingEntity.limbAnimator.getAnimationProgress() * 0.6662F)
                           * 2.0F
                           * Math.min(livingEntity.limbAnimator.getSpeed(), 1.0F)
                           * 0.5F
                     );
                     local4062_2.floatValArray3[4] = (float)Math.toDegrees(
                        MathHelper.cos(livingEntity.limbAnimator.getAnimationProgress() * 0.6662F) * 1.4F * Math.min(livingEntity.limbAnimator.getSpeed(), 1.0F)
                     );
                     local4062_2.floatValArray3[5] = (float)Math.toDegrees(
                        MathHelper.cos(livingEntity.limbAnimator.getAnimationProgress() * 0.6662F + (float) Math.PI)
                           * 1.4F
                           * Math.min(livingEntity.limbAnimator.getSpeed(), 1.0F)
                     );
                  }

                  this.list.add(local4062_2);
               }
            }
         );
      }
   };
   private final EventListener<WorldRenderEvent> worldRenderListener = worldRenderEvent -> {
      if (AutoTotemModule.getInstance().displayTotemPop.isChecked() && mc.player != null && mc.world != null && mc.gameRenderer != null) {
         this.list.removeIf(local4054_0 -> (float)(System.currentTimeMillis() - local4054_0.timeMillis) >= 2000.0F);
         if (!this.list.isEmpty()) {
            for (TotemGhostModule.SubContext local4064_3 : this.list) {
               local4064_3.floatVal3 = (float)(System.currentTimeMillis() - local4064_3.timeMillis) / 2000.0F;
               local4064_3.floatVal4 = Math.min(local4064_3.floatVal3 * 8.0F, 1.0F)
                  * (local4064_3.floatVal3 > 0.55F ? 1.0F - (local4064_3.floatVal3 - 0.55F) / 0.45F : 1.0F);
               worldRenderEvent.getMatrixStack().push();
               worldRenderEvent.getMatrixStack()
                  .translate(
                     local4064_3.doubleVal - mc.gameRenderer.getCamera().getCameraPos().x,
                     local4064_3.doubleVal2
                        - mc.gameRenderer.getCamera().getCameraPos().y
                        + (1.0F - (1.0F - local4064_3.floatVal3) * (1.0F - local4064_3.floatVal3) * (1.0F - local4064_3.floatVal3)) * 2.0F,
                     local4064_3.doubleVal3 - mc.gameRenderer.getCamera().getCameraPos().z
                  );
               worldRenderEvent.getMatrixStack().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-local4064_3.floatVal + local4064_3.floatVal3 * 40.0F));
               worldRenderEvent.getMatrixStack().push();
               worldRenderEvent.getMatrixStack().translate(0.0F, 2.16F, 0.0F);
               worldRenderEvent.getMatrixStack().multiply(RotationAxis.POSITIVE_X.rotationDegrees(11.0F));
               worldRenderEvent.getMatrixStack().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-4.0F));
               local4064_3.matrix4f2.set(worldRenderEvent.getMatrixStack().peek().getPositionMatrix());
               worldRenderEvent.getMatrixStack().pop();

               for (int index = 0; index < floatValArray.length; index++) {
                  if (local4064_3.floatValArray3[index] != 0.0F || index == 0 && local4064_3.floatVal2 != 0.0F) {
                     worldRenderEvent.getMatrixStack().push();
                     worldRenderEvent.getMatrixStack().translate(floatValArray2[index][0], floatValArray2[index][1], floatValArray2[index][2]);
                     if (index == 0 && local4064_3.floatVal2 != 0.0F) {
                        worldRenderEvent.getMatrixStack().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-local4064_3.floatVal2));
                     }

                     if (local4064_3.floatValArray3[index] != 0.0F) {
                        worldRenderEvent.getMatrixStack().multiply(RotationAxis.POSITIVE_X.rotationDegrees(local4064_3.floatValArray3[index]));
                     }

                     worldRenderEvent.getMatrixStack().translate(-floatValArray2[index][0], -floatValArray2[index][1], -floatValArray2[index][2]);
                     local4064_3.matrix4fArray[index].set(worldRenderEvent.getMatrixStack().peek().getPositionMatrix());
                     worldRenderEvent.getMatrixStack().pop();
                  } else {
                     local4064_3.matrix4fArray[index].set(worldRenderEvent.getMatrixStack().peek().getPositionMatrix());
                  }
               }

               worldRenderEvent.getMatrixStack().pop();
               worldRenderEvent.getMatrixStack().push();
               worldRenderEvent.getMatrixStack()
                  .translate(
                     local4064_3.doubleVal - mc.gameRenderer.getCamera().getCameraPos().x,
                     local4064_3.doubleVal2
                        - mc.gameRenderer.getCamera().getCameraPos().y
                        + (1.0F - (1.0F - local4064_3.floatVal3) * (1.0F - local4064_3.floatVal3) * (1.0F - local4064_3.floatVal3)) * 2.0F
                        + 1.1F,
                     local4064_3.doubleVal3 - mc.gameRenderer.getCamera().getCameraPos().z
                  );
               worldRenderEvent.getMatrixStack().multiply(mc.gameRenderer.getCamera().getRotation());
               local4064_3.matrix4f3.set(worldRenderEvent.getMatrixStack().peek().getPositionMatrix());
               worldRenderEvent.getMatrixStack().pop();
            }

            for (TotemGhostModule.SubContext local4064_3 : this.list) {
               if (!(local4064_3.floatVal4 <= 0.0F)) {
                  for (int index2 = 0; index2 < floatValArray.length; index2++) {
                     for (int[] intArray : intValArray) {
                        for (int local4064_12 : intArray) {
                           this.immediate2
                              .getBuffer(renderLayer)
                              .vertex(
                                 local4064_3.matrix4fArray[index2],
                                 this.computeFloat(floatValArray[index2], local4064_12, 0),
                                 this.computeFloat(floatValArray[index2], local4064_12, 1),
                                 this.computeFloat(floatValArray[index2], local4064_12, 2)
                              )
                              .color(Colors.computeInt6(this.computeInt(), local4064_3.floatVal4 * 0.25F));
                        }
                     }
                  }
               }
            }

            for (TotemGhostModule.SubContext local4064_3 : this.list) {
               this.processData2(
                  this.immediate2.getBuffer(renderLayer),
                  local4064_3.matrix4f2,
                  Colors.computeInt6(this.computeInt(), local4064_3.floatVal4 * 0.2F),
                  0,
                  0.3F,
                  0.395F,
                  0.0F
               );
               this.processData2(
                  this.immediate2.getBuffer(renderLayer),
                  local4064_3.matrix4f2,
                  0,
                  Colors.computeInt6(this.computeInt(), local4064_3.floatVal4 * 0.2F),
                  0.405F,
                  0.5F,
                  0.0F
               );
            }

            for (TotemGhostModule.SubContext local4064_3 : this.list) {
               this.processData2(
                  this.immediate2.getBuffer(renderLayer),
                  local4064_3.matrix4f2,
                  Colors.computeInt6(this.computeInt(), local4064_3.floatVal4 * 0.92F),
                  Colors.computeInt6(this.computeInt(), local4064_3.floatVal4 * 0.92F),
                  0.39F,
                  0.41F,
                  0.006F
               );
               this.processData2(
                  this.immediate2.getBuffer(renderLayer),
                  local4064_3.matrix4f2,
                  Colors.computeInt6(this.computeInt(), local4064_3.floatVal4 * 0.92F),
                  Colors.computeInt6(this.computeInt(), local4064_3.floatVal4 * 0.92F),
                  0.39F,
                  0.41F,
                  -0.006F
               );

               for (int index3 = 0; index3 < 96; index3++) {
                  this.immediate2
                     .getBuffer(renderLayer)
                     .vertex(
                        local4064_3.matrix4f2,
                        MathHelper.cos((float) (Math.PI * 2) * index3 / 96.0F) * 0.41F,
                        -0.006F,
                        MathHelper.sin((float) (Math.PI * 2) * index3 / 96.0F) * 0.41F
                     )
                     .color(Colors.computeInt6(this.computeInt(), local4064_3.floatVal4 * 0.92F));
                  this.immediate2
                     .getBuffer(renderLayer)
                     .vertex(
                        local4064_3.matrix4f2,
                        MathHelper.cos((float) (Math.PI * 2) * (index3 + 1) / 96.0F) * 0.41F,
                        -0.006F,
                        MathHelper.sin((float) (Math.PI * 2) * (index3 + 1) / 96.0F) * 0.41F
                     )
                     .color(Colors.computeInt6(this.computeInt(), local4064_3.floatVal4 * 0.92F));
                  this.immediate2
                     .getBuffer(renderLayer)
                     .vertex(
                        local4064_3.matrix4f2,
                        MathHelper.cos((float) (Math.PI * 2) * (index3 + 1) / 96.0F) * 0.41F,
                        0.006F,
                        MathHelper.sin((float) (Math.PI * 2) * (index3 + 1) / 96.0F) * 0.41F
                     )
                     .color(Colors.computeInt6(this.computeInt(), local4064_3.floatVal4 * 0.92F));
                  this.immediate2
                     .getBuffer(renderLayer)
                     .vertex(
                        local4064_3.matrix4f2,
                        MathHelper.cos((float) (Math.PI * 2) * index3 / 96.0F) * 0.41F,
                        0.006F,
                        MathHelper.sin((float) (Math.PI * 2) * index3 / 96.0F) * 0.41F
                     )
                     .color(Colors.computeInt6(this.computeInt(), local4064_3.floatVal4 * 0.92F));
               }
            }

            for (TotemGhostModule.SubContext local4064_3 : this.list) {
               if (!(local4064_3.floatVal4 <= 0.0F)) {
                  for (int index4 = 0; index4 < floatValArray.length; index4++) {
                     for (int[] intArray2 : intValArray2) {
                        this.immediate2
                           .getBuffer(renderLayer2)
                           .vertex(
                              local4064_3.matrix4fArray[index4],
                              this.computeFloat(floatValArray[index4], intArray2[0], 0),
                              this.computeFloat(floatValArray[index4], intArray2[0], 1),
                              this.computeFloat(floatValArray[index4], intArray2[0], 2)
                           )
                           .color(Colors.computeInt6(this.computeInt(), local4064_3.floatVal4 * 0.45F));
                        this.immediate2
                           .getBuffer(renderLayer2)
                           .vertex(
                              local4064_3.matrix4fArray[index4],
                              this.computeFloat(floatValArray[index4], intArray2[1], 0),
                              this.computeFloat(floatValArray[index4], intArray2[1], 1),
                              this.computeFloat(floatValArray[index4], intArray2[1], 2)
                           )
                           .color(Colors.computeInt6(this.computeInt(), local4064_3.floatVal4 * 0.45F));
                     }
                  }
               }
            }

            for (TotemGhostModule.SubContext local4064_3 : this.list) {
               this.processData(
                  this.immediate2.getBuffer(renderLayer3),
                  local4064_3.matrix4f3,
                  Colors.computeInt6(this.computeInt(), local4064_3.floatVal4 * 0.43F),
                  1.5F
               );
               this.processData(
                  this.immediate2.getBuffer(renderLayer3),
                  local4064_3.matrix4f3,
                  Colors.computeInt6(this.computeInt(), local4064_3.floatVal4 * 0.63F),
                  0.7F
               );
            }

            this.immediate2.draw();
         }
      } else {
         this.list.clear();
      }
   };

   private void processData(VertexConsumer vertexConsumer, Matrix4f matrix4f, int local4055_3, float local4055_4) {
      vertexConsumer.vertex(matrix4f, -local4055_4, -local4055_4, 0.0F).texture(0.0F, 1.0F).color(local4055_3);
      vertexConsumer.vertex(matrix4f, local4055_4, -local4055_4, 0.0F).texture(1.0F, 1.0F).color(local4055_3);
      vertexConsumer.vertex(matrix4f, local4055_4, local4055_4, 0.0F).texture(1.0F, 0.0F).color(local4055_3);
      vertexConsumer.vertex(matrix4f, -local4055_4, local4055_4, 0.0F).texture(0.0F, 0.0F).color(local4055_3);
   }

   private float computeFloat(float[] floatArray, int local4056_2, int local4056_3) {
      return floatArray[(local4056_2 >> local4056_3 & 1) * 3 + local4056_3];
   }

   private int computeInt() {
      return ThemeColors.intVal9;
   }

   public static TotemGhostModule getInstance() {
      if (totemGhostModule == null) {
         totemGhostModule = new TotemGhostModule();
      }

      return totemGhostModule;
   }

   private TotemGhostModule() {
      ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((AfterClientWorldChange)(minecraftClient, clientWorld) -> this.list.clear());
      ModuleManager.unregisterModule(this);
      this.setEnabledWithoutCallbacks(true);
   }

   @Override
   protected void onDisable() {
      this.list.clear();
   }

   private void processData2(
      VertexConsumer vertexConsumer, Matrix4f matrix4f, int local4063_3, int local4063_4, float local4063_5, float local4063_6, float local4063_7
   ) {
      if (local4063_3 >>> 24 != 0 || local4063_4 >>> 24 != 0) {
         for (int index = 0; index < 96; index++) {
            vertexConsumer.vertex(
                  matrix4f,
                  MathHelper.cos((float) (Math.PI * 2) * index / 96.0F) * local4063_5,
                  local4063_7,
                  MathHelper.sin((float) (Math.PI * 2) * index / 96.0F) * local4063_5
               )
               .color(local4063_3);
            vertexConsumer.vertex(
                  matrix4f,
                  MathHelper.cos((float) (Math.PI * 2) * index / 96.0F) * local4063_6,
                  local4063_7,
                  MathHelper.sin((float) (Math.PI * 2) * index / 96.0F) * local4063_6
               )
               .color(local4063_4);
            vertexConsumer.vertex(
                  matrix4f,
                  MathHelper.cos((float) (Math.PI * 2) * (index + 1) / 96.0F) * local4063_6,
                  local4063_7,
                  MathHelper.sin((float) (Math.PI * 2) * (index + 1) / 96.0F) * local4063_6
               )
               .color(local4063_4);
            vertexConsumer.vertex(
                  matrix4f,
                  MathHelper.cos((float) (Math.PI * 2) * (index + 1) / 96.0F) * local4063_5,
                  local4063_7,
                  MathHelper.sin((float) (Math.PI * 2) * (index + 1) / 96.0F) * local4063_5
               )
               .color(local4063_3);
         }
      }
   }

   private static final class SubContext {
      private final double doubleVal;
      private final double doubleVal2;
      private final double doubleVal3;
      private final long timeMillis;
      private float floatVal;
      private float floatVal2;
      private final float[] floatValArray3 = new float[TotemGhostModule.floatValArray.length];
      private float floatVal3;
      private float floatVal4;
      private final Matrix4f[] matrix4fArray = new Matrix4f[TotemGhostModule.floatValArray.length];
      private final Matrix4f matrix4f2 = new Matrix4f();
      private final Matrix4f matrix4f3 = new Matrix4f();

      private SubContext(double local4065_1, double local4065_3, double local4065_5, float local4065_7) {
         this.doubleVal = local4065_1;
         this.doubleVal2 = local4065_3;
         this.doubleVal3 = local4065_5;
         this.floatVal = local4065_7;
         this.timeMillis = System.currentTimeMillis();

         for (int index = 0; index < TotemGhostModule.floatValArray.length; index++) {
            this.matrix4fArray[index] = new Matrix4f();
         }
      }
   }
}
