package greed.client.cosmetic;

import greed.client.module.misc.GlobalsModule;
import greed.client.module.misc.UnHookModule;
import greed.client.cosmetic.BedrockModelRenderer;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer.HeadTransformation;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public final class CosmeticFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   private static final long timeMillis = System.nanoTime();
   private static final CosmeticCategory[] recoveredClass0304Array = new CosmeticCategory[]{
      CosmeticCategory.BODYWEAR, CosmeticCategory.WINGS, CosmeticCategory.HAT, CosmeticCategory.ACCESSORY
   };
   private static final Int2ObjectOpenHashMap<CosmeticEntry[]> map = new Int2ObjectOpenHashMap();
   private static long timeMillis2 = Long.MIN_VALUE;
   private static Object object;

   private static boolean isLocal(PlayerEntityRenderState playerEntityRenderState) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      return minecraftClient.player != null && minecraftClient.player.getId() == playerEntityRenderState.id;
   }

   public CosmeticFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> local4576_1) {
      super(local4576_1);
   }

   private static int layerSlot(CosmeticCategory cosmeticCategory) {
      for (int index = 0; index < recoveredClass0304Array.length; index++) {
         if (recoveredClass0304Array[index] == cosmeticCategory) {
            return index;
         }
      }

      return 0;
   }

   public void render(
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int local4579_3,
      PlayerEntityRenderState playerEntityRenderState,
      float local4579_5,
      float local4579_6
   ) {
      if (UnHookModule.isUnhooked()) {
         return;
      }

      if (!playerEntityRenderState.spectator && (isLocal(playerEntityRenderState) || GlobalsModule.getInstance().checkState())) {
         float local4579_7 = time();
         boolean local4579_8 = Math.abs(playerEntityRenderState.limbSwingAmplitude) > 0.01F;
         this.submitGeometry(CosmeticCategory.BODYWEAR, matrixStack, orderedRenderCommandQueue, local4579_3, playerEntityRenderState, local4579_8, local4579_7);
         this.submitGeometry(CosmeticCategory.WINGS, matrixStack, orderedRenderCommandQueue, local4579_3, playerEntityRenderState, local4579_8, local4579_7);
         this.submitGeometry(CosmeticCategory.HAT, matrixStack, orderedRenderCommandQueue, local4579_3, playerEntityRenderState, local4579_8, local4579_7);
         this.submitGeometry(CosmeticCategory.ACCESSORY, matrixStack, orderedRenderCommandQueue, local4579_3, playerEntityRenderState, local4579_8, local4579_7);
      }
   }

   private static CosmeticEntry equippedEntry(CosmeticCategory cosmeticCategory, PlayerEntityRenderState playerEntityRenderState) {
      if (isLocal(playerEntityRenderState)) {
         return CosmeticStore.getCosmeticEntry(cosmeticCategory);
      }

      CosmeticEntry[] recoveredClass0148Array = syncedEntries(playerEntityRenderState.id);
      return recoveredClass0148Array == null ? null : recoveredClass0148Array[layerSlot(cosmeticCategory)];
   }

   private static CosmeticEntry[] syncedEntries(int local4581_0) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient.world == null) {
         return null;
      }

      long local4581_2 = System.currentTimeMillis() / 50L;
      if (local4581_2 != timeMillis2 || minecraftClient.world != object) {
         map.clear();
         timeMillis2 = local4581_2;
         object = minecraftClient.world;
      }

      CosmeticEntry[] recoveredClass0148Array = (CosmeticEntry[])map.get(local4581_0);
      if (recoveredClass0148Array == null) {
         recoveredClass0148Array = new CosmeticEntry[recoveredClass0304Array.length];
         if (minecraftClient.world.getEntityById(local4581_0) instanceof AbstractClientPlayerEntity abstractClientPlayerEntity) {
            String local4581_6 = abstractClientPlayerEntity.getGameProfile().name();

            for (int index = 0; index < recoveredClass0304Array.length; index++) {
               recoveredClass0148Array[index] = GlobalsModule.getInstance().getCosmeticEntry(local4581_6, recoveredClass0304Array[index]);
            }
         }

         map.put(local4581_0, recoveredClass0148Array);
      }

      return recoveredClass0148Array;
   }

   private void submitGeometry(
      CosmeticCategory cosmeticCategory,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int local4582_4,
      PlayerEntityRenderState playerEntityRenderState,
      boolean local4582_6,
      float local4582_7
   ) {
      CosmeticEntry cosmeticEntry = equippedEntry(cosmeticCategory, playerEntityRenderState);
      if (cosmeticEntry != null) {
         if (cosmeticEntry.placement() != null) {
            this.submitHostedModel(cosmeticEntry, cosmeticEntry.placement(), matrixStack, orderedRenderCommandQueue, local4582_4, playerEntityRenderState);
         } else {
            matrixStack.push();
            if (cosmeticCategory == CosmeticCategory.HAT) {
               this.getContextModel().head.applyTransform(matrixStack);
               matrixStack.scale(1.08F, 1.08F, 1.08F);
               switch (cosmeticEntry.id()) {
                  case 48:
                  case 49:
                  case 50:
                  case 53:
                  case 57:
                     matrixStack.translate(0.0, -0.365, 0.0);
                     break;
                  case 80:
                     matrixStack.translate(0.0, 0.06, 0.0);
                  case 85:
                  case 98:
                     break;
                  case 88:
                  case 97:
                  case 125:
                     matrixStack.translate(0.0, 0.0625, 0.0);
                     break;
                  case 92:
                     matrixStack.translate(0.0, 0.0, -0.00625);
                     break;
                  case 96:
                     matrixStack.translate(0.0, 0.0, -0.06875);
                     break;
                  default:
                     matrixStack.translate(0.0, -0.15, 0.0);
               }
            } else if (cosmeticCategory == CosmeticCategory.BODYWEAR) {
               this.getContextModel().body.applyTransform(matrixStack);
               matrixStack.translate(0.0, 0.25, 0.0);
            } else if (cosmeticCategory == CosmeticCategory.WINGS) {
               this.getContextModel().body.applyTransform(matrixStack);
               matrixStack.translate(0.0, 0.27, 0.0);
            }

            BedrockModelRenderer.renderElement6(
               cosmeticEntry,
               matrixStack,
               orderedRenderCommandQueue,
               local4582_4,
               playerEntityRenderState.isGliding,
               playerEntityRenderState.isSwimming,
               playerEntityRenderState.isInSneakingPose,
               local4582_6,
               local4582_7
            );
            matrixStack.pop();
         }
      }
   }

   private void submitHostedModel(
      CosmeticEntry cosmeticEntry,
      CosmeticPlacement cosmeticPlacement,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int local4583_5,
      PlayerEntityRenderState playerEntityRenderState
   ) {
      if (!cosmeticPlacement.isPet()) {
         CosmeticModel cosmeticModel = CosmeticModel.getCosmeticModel(cosmeticEntry);
         if (cosmeticModel != null) {
            matrixStack.push();
            this.getContextModel().getRootPart().applyTransform(matrixStack);
            if (cosmeticPlacement.onBody()) {
               this.getContextModel().body.applyTransform(matrixStack);
            } else {
               this.getContextModel().head.applyTransform(matrixStack);
            }

            float local4583_8 = cosmeticPlacement.anchor().equals("shoulder") ? Math.max(0.3F, Math.abs(cosmeticPlacement.x())) : cosmeticPlacement.x();
            matrixStack.translate(local4583_8, cosmeticPlacement.y(), cosmeticPlacement.z());
            if (!cosmeticPlacement.isAnimatedHead()) {
               HeadFeatureRenderer.translate(matrixStack, HeadTransformation.DEFAULT);
            }

            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cosmeticPlacement.rotationX()));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cosmeticPlacement.rotationY()));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(cosmeticPlacement.rotationZ()));
            if (cosmeticPlacement.isAnimatedHead()) {
               float local4583_9 = cosmeticPlacement.alignScale() * Math.max(0.05F, cosmeticPlacement.scale());
               matrixStack.translate(0.0F, -0.25F, 0.0F);
               matrixStack.scale(local4583_9, -local4583_9, local4583_9);
               matrixStack.translate(-cosmeticPlacement.alignX() / 16.0F, -cosmeticPlacement.alignY() / 16.0F, 0.0F);
            } else {
               float local4583_9 = Math.max(0.05F, cosmeticPlacement.scale());
               matrixStack.scale(local4583_9, local4583_9, local4583_9);
               cosmeticModel.renderElement3(matrixStack);
            }

            cosmeticModel.renderElement5(matrixStack, orderedRenderCommandQueue, local4583_5, playerEntityRenderState.age / 20.0F, 0.0F);
            matrixStack.pop();
         }
      }
   }

   public static float time() {
      return (float)((System.nanoTime() - timeMillis) / 1.0E9);
   }
}
