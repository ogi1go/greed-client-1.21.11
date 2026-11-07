package greed.client.cosmetic;

import greed.client.cosmetic.CosmeticAnimationLoader;
import greed.client.cosmetic.CosmeticCategory;
import greed.client.cosmetic.CosmeticEntry;
import greed.client.cosmetic.CosmeticFeatureRenderer;
import greed.client.cosmetic.CosmeticStore;
import greed.client.module.misc.GlobalsModule;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.AfterEntities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public final class CosmeticRenderDispatcher {
   private static void renderElement(
      MatrixStack matrixStack,
      VertexConsumerProvider vertexConsumerProvider,
      MinecraftClient minecraftClient,
      AbstractClientPlayerEntity abstractClientPlayerEntity,
      CosmeticEntry cosmeticEntry
   ) {
      if (cosmeticEntry != null && cosmeticEntry.placement() == null) {
         float tickProgress = minecraftClient.getRenderTickCounter().getTickProgress(false);
         Vec3d lerpedPos = abstractClientPlayerEntity.getLerpedPos(tickProgress);
         Vec3d cameraPos = minecraftClient.gameRenderer.getCamera().getCameraPos();
         CosmeticAnimationLoader.AnimationVec3 local4528_8 = CosmeticAnimationLoader.getNested0513(cosmeticEntry);
         if (local4528_8 != null) {
            matrixStack.push();
            matrixStack.translate(lerpedPos.x - cameraPos.x, lerpedPos.y - cameraPos.y, lerpedPos.z - cameraPos.z);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-abstractClientPlayerEntity.getLerpedYaw(tickProgress)));
            matrixStack.translate(local4528_8.x(), abstractClientPlayerEntity.getHeight() - local4528_8.y(), local4528_8.z());
            switch (cosmeticEntry.id()) {
               case 45:
               case 46:
               case 90:
                  matrixStack.translate(0.0, -3.0, 0.0);
                  break;
               case 132:
                  matrixStack.translate(0.0, -2.0, 0.0);
                  break;
               case 133:
                  matrixStack.translate(-1.0, -0.5, 0.0);
                  break;
               case 134:
               case 135:
                  matrixStack.translate(-1.0, 0.0, 0.0);
            }

            BedrockModelRenderer.renderElement7(
               cosmeticEntry,
               matrixStack,
               vertexConsumerProvider,
               WorldRenderer.getLightmapCoordinates(minecraftClient.world, BlockPos.ofFloored(lerpedPos)),
               abstractClientPlayerEntity.isGliding(),
               abstractClientPlayerEntity.isSwimming(),
               abstractClientPlayerEntity.isSneaking(),
               PlayerMovementTracker.isMoving(abstractClientPlayerEntity),
               CosmeticFeatureRenderer.time()
            );
            matrixStack.pop();
         }
      }
   }

   public static void updateState() {
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)minecraftClient -> CosmeticMotionTracker.getCosmeticMotionTracker().processData(minecraftClient));
      WorldRenderEvents.AFTER_ENTITIES
         .register(
            (AfterEntities)worldRenderContext -> {
               MinecraftClient minecraftClient2 = MinecraftClient.getInstance();
               if (minecraftClient2.player != null && minecraftClient2.world != null) {
                  CosmeticMotionTracker.getCosmeticMotionTracker().renderElement(worldRenderContext.matrices(), worldRenderContext.consumers(), minecraftClient2);
                  if (!minecraftClient2.options.getPerspective().isFirstPerson()) {
                     renderElement(
                        worldRenderContext.matrices(), worldRenderContext.consumers(), minecraftClient2, minecraftClient2.player, CosmeticStore.getCosmeticEntry(CosmeticCategory.PET)
                     );
                  }

                  if (GlobalsModule.getInstance().checkState()) {
                     for (AbstractClientPlayerEntity abstractClientPlayerEntity : minecraftClient2.world.getPlayers()) {
                        if (abstractClientPlayerEntity != minecraftClient2.player && !abstractClientPlayerEntity.isSpectator() && !abstractClientPlayerEntity.isInvisible()) {
                           renderElement(
                              worldRenderContext.matrices(),
                              worldRenderContext.consumers(),
                              minecraftClient2,
                              abstractClientPlayerEntity,
                              GlobalsModule.getInstance().getCosmeticEntry(abstractClientPlayerEntity.getGameProfile().name(), CosmeticCategory.PET)
                           );
                        }
                     }
                  }
               }
            }
         );
   }

   private CosmeticRenderDispatcher() {
   }
}
