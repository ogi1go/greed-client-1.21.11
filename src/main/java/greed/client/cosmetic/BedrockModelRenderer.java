package greed.client.cosmetic;

import greed.client.cosmetic.CosmeticAnimationLoader;
import greed.client.cosmetic.CosmeticAnimationState;
import greed.client.cosmetic.CosmeticEntry;

import java.util.Map.Entry;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BedrockModelRenderer {
   private static final float[] floatValArray = new float[]{0.0F, 0.0F, 0.0F};
   private static final float[] floatValArray2 = new float[]{1.0F, 1.0F, 1.0F};
   private static final float[] floatValArray3 = new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F};
   private static final float[] floatValArray4 = new float[3];
   private static final MatrixStack matrixStack2 = new MatrixStack();
   private static final Vector3f vector3f2 = new Vector3f();
   private static final Matrix4f matrix4f2 = new Matrix4f();
   private static final Matrix4f matrix4f3 = new Matrix4f();
   private static final Matrix3f matrix3f2 = new Matrix3f();
   private static final Matrix3f matrix3f3 = new Matrix3f();
   private static int intVal;

   private static void renderElement(CosmeticAnimationState cosmeticAnimationState, MatrixStack matrixStack, boolean local6204_2) {
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
      if (local6204_2) {
         matrixStack.translate(cosmeticAnimationState.floatVal2, cosmeticAnimationState.floatVal3, cosmeticAnimationState.floatVal4);
      }

      if (cosmeticAnimationState.floatVal5 != 0.0F) {
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cosmeticAnimationState.floatVal5));
      }

      if (cosmeticAnimationState.floatVal6 != 0.0F) {
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cosmeticAnimationState.floatVal6));
      }

      if (cosmeticAnimationState.floatVal7 != 0.0F) {
         matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(cosmeticAnimationState.floatVal7));
      }

      matrixStack.scale(cosmeticAnimationState.floatVal, cosmeticAnimationState.floatVal, cosmeticAnimationState.floatVal);
   }

   private static void processData(
      BedrockCube[] recoveredClass0169Array,
      float[] floatArray,
      int local6205_2,
      Matrix4f matrix4f,
      Matrix3f matrix3f,
      VertexConsumer vertexConsumer,
      int local6205_6
   ) {
      for (int index = 0; index < local6205_2; index++) {
         int local6205_8 = index * 25;
         matrix4f2.set(matrix4f).mul(matrix4f3.set(floatArray, local6205_8));
         matrix3f2.set(matrix3f).mul(matrix3f3.set(floatArray, local6205_8 + 16));
         processData2(recoveredClass0169Array[index], matrix4f2, matrix3f2, vertexConsumer, local6205_6);
      }
   }

   private static CosmeticAnimationState getCosmeticAnimationState(
      CosmeticEntry cosmeticEntry, boolean local6206_1, boolean local6206_2, boolean local6206_3, boolean local6206_4, float local6206_5
   ) {
      CosmeticAnimationState cosmeticAnimationState = CosmeticAnimationLoader.getCosmeticAnimationState2(cosmeticEntry);
      if (cosmeticAnimationState != null && cosmeticEntry.textureId() != null) {
         cosmeticAnimationState.updateState();
         String local6206_7 = cosmeticAnimationState.formatText5(local6206_1, local6206_2, local6206_3, local6206_4);
         if (local6206_7 != null) {
            CosmeticAnimationState.ChannelState local6206_8 = cosmeticAnimationState.map.get(local6206_7);
            if (local6206_8 != null) {
               processData3(cosmeticAnimationState, local6206_8, local6206_5);
            }
         }

         return cosmeticAnimationState;
      } else {
         return null;
      }
   }

   private BedrockModelRenderer() {
   }

   private static void renderElement2(BedrockBone bedrockBone, MatrixStack matrixStack, VertexConsumer vertexConsumer, int local6208_3) {
      if (!bedrockBone.flag) {
         matrixStack.push();
         renderElement4(bedrockBone, matrixStack);

         for (BedrockCube bedrockCube : bedrockBone.list2) {
            matrixStack.push();
            renderElement5(bedrockCube, matrixStack);
            processData2(bedrockCube, matrixStack.peek().getPositionMatrix(), matrixStack.peek().getNormalMatrix(), vertexConsumer, local6208_3);
            matrixStack.pop();
         }

         for (BedrockBone bedrockBone2 : bedrockBone.list) {
            renderElement2(bedrockBone2, matrixStack, vertexConsumer, local6208_3);
         }

         matrixStack.pop();
      }
   }

   private static void processData2(BedrockCube bedrockCube, Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer vertexConsumer, int local6210_4) {
      for (BedrockQuad bedrockQuad : bedrockCube.recoveredClass0278Array) {
         if (bedrockQuad != null) {
            Vector3f vector3f = vector3f2.set(bedrockQuad.modelVec3.computeFloat(), bedrockQuad.modelVec3.computeFloat3(), bedrockQuad.modelVec3.computeFloat2());
            matrix3f.transform(vector3f);
            float local6210_10 = vector3f.x();
            float local6210_11 = vector3f.y();
            float local6210_12 = vector3f.z();
            if ((bedrockCube.modelVec3.computeFloat3() == 0.0F || bedrockCube.modelVec3.computeFloat2() == 0.0F) && local6210_10 < 0.0F) {
               local6210_10 = -local6210_10;
            }

            if ((bedrockCube.modelVec3.computeFloat() == 0.0F || bedrockCube.modelVec3.computeFloat2() == 0.0F) && local6210_11 < 0.0F) {
               local6210_11 = -local6210_11;
            }

            if ((bedrockCube.modelVec3.computeFloat() == 0.0F || bedrockCube.modelVec3.computeFloat3() == 0.0F) && local6210_12 < 0.0F) {
               local6210_12 = -local6210_12;
            }

            for (BedrockVertex bedrockVertex : bedrockQuad.recoveredClass0057Array) {
               vertexConsumer.vertex(matrix4f, bedrockVertex.modelVec3.computeFloat(), bedrockVertex.modelVec3.computeFloat3(), bedrockVertex.modelVec3.computeFloat2())
                  .color(255, 255, 255, 255)
                  .texture(bedrockVertex.floatVal, bedrockVertex.floatVal2)
                  .overlay(OverlayTexture.DEFAULT_UV)
                  .light(local6210_4)
                  .normal(local6210_10, local6210_11, local6210_12);
            }
         }
      }
   }

   private static void renderElement3(
      BedrockBone bedrockBone, MatrixStack matrixStack, BedrockCube[] recoveredClass0169Array, float[] floatArray, Matrix4f matrix4f
   ) {
      if (!bedrockBone.flag) {
         matrixStack.push();
         renderElement4(bedrockBone, matrixStack);

         for (BedrockCube bedrockCube : bedrockBone.list2) {
            matrixStack.push();
            renderElement5(bedrockCube, matrixStack);
            int local6212_7 = intVal++;
            recoveredClass0169Array[local6212_7] = bedrockCube;
            matrixStack.peek().getPositionMatrix().get(floatArray, local6212_7 * 25);
            matrixStack.peek().getNormalMatrix().get(floatArray, local6212_7 * 25 + 16);
            if (matrix4f != null) {
               BedrockVertexCollector.processData(bedrockCube, matrix4f2.set(matrix4f).mul(matrixStack.peek().getPositionMatrix()));
            }

            matrixStack.pop();
         }

         for (BedrockBone bedrockBone2 : bedrockBone.list) {
            renderElement3(bedrockBone2, matrixStack, recoveredClass0169Array, floatArray, matrix4f);
         }

         matrixStack.pop();
      }
   }

   private static void renderElement4(BedrockBone bedrockBone, MatrixStack matrixStack) {
      BedrockTransforms.renderElement(bedrockBone, matrixStack);
      BedrockTransforms.renderElement8(bedrockBone, matrixStack);
      BedrockTransforms.renderElement6(bedrockBone, matrixStack);
      BedrockTransforms.renderElement4(bedrockBone, matrixStack);
      BedrockTransforms.renderElement2(bedrockBone, matrixStack);
   }

   static void processData3(CosmeticAnimationState cosmeticAnimationState, CosmeticAnimationState.ChannelState local6214_1, float local6214_2) {
      float local6214_3 = local6214_1.flag ? local6214_2 % local6214_1.floatVal8 : Math.min(local6214_2, local6214_1.floatVal8);

      for (Entry<String, CosmeticAnimationState.KeyframeState> local6214_5 : local6214_1.map5.entrySet()) {
         BedrockBone bedrockBone = cosmeticAnimationState.getBedrockBone(local6214_5.getKey());
         if (bedrockBone != null) {
            float[] floatArray = cosmeticAnimationState.map2.get(local6214_5.getKey());
            if (floatArray == null) {
               floatArray = floatValArray3;
            }

            CosmeticAnimationState.KeyframeState value = local6214_5.getValue();
            if (!value.nested0533.checkState()) {
               float[] floatArray2 = value.nested0533.computeFloat2(local6214_3, floatValArray, floatValArray4);
               bedrockBone.processData5(floatArray[0] + (float)Math.toRadians(-floatArray2[0]));
               bedrockBone.processData7(floatArray[1] + (float)Math.toRadians(-floatArray2[1]));
               bedrockBone.processData8(floatArray[2] + (float)Math.toRadians(floatArray2[2]));
            }

            if (!value.nested05332.checkState()) {
               float[] floatArray3 = value.nested05332.computeFloat2(local6214_3, floatValArray, floatValArray4);
               bedrockBone.processData2(floatArray[3] + floatArray3[0]);
               bedrockBone.processData13(floatArray[4] + floatArray3[1]);
               bedrockBone.processData4(floatArray[5] + floatArray3[2]);
            }

            if (!value.nested05333.checkState()) {
               float[] floatArray4 = value.nested05333.computeFloat2(local6214_3, floatValArray2, floatValArray4);
               bedrockBone.processData9(floatArray[6] * floatArray4[0]);
               bedrockBone.processData12(floatArray[7] * floatArray4[1]);
               bedrockBone.processData6(floatArray[8] * floatArray4[2]);
            }
         }
      }
   }

   private static void renderElement5(BedrockCube bedrockCube, MatrixStack matrixStack) {
      BedrockTransforms.renderElement7(bedrockCube, matrixStack);
      BedrockTransforms.renderElement3(bedrockCube, matrixStack);
      BedrockTransforms.renderElement5(bedrockCube, matrixStack);
   }

   public static boolean renderElement6(
      CosmeticEntry cosmeticEntry,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int local6216_3,
      boolean local6216_4,
      boolean local6216_5,
      boolean local6216_6,
      boolean local6216_7,
      float local6216_8
   ) {
      CosmeticAnimationState cosmeticAnimationState = getCosmeticAnimationState(cosmeticEntry, local6216_4, local6216_5, local6216_6, local6216_7, local6216_8);
      if (cosmeticAnimationState == null) {
         return false;
      }

      matrixStack.push();
      renderElement(cosmeticAnimationState, matrixStack, true);
      RenderLayer renderLayer = RenderLayers.entityCutoutNoCull(cosmeticEntry.textureId());
      boolean local6216_11 = BedrockVertexCollector.checkState();
      if (local6216_11) {
         BedrockVertexCollector.processData2(cosmeticEntry.textureId());
      }

      BedrockCube[] recoveredClass0169Array = new BedrockCube[cosmeticAnimationState.intVal3];
      float[] floatArray = new float[cosmeticAnimationState.intVal3 * 25];
      matrixStack2.peek().getPositionMatrix().identity();
      matrixStack2.peek().getNormalMatrix().identity();
      intVal = 0;
      Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();

      for (BedrockBone bedrockBone : cosmeticAnimationState.bedrockModel.list) {
         renderElement3(bedrockBone, matrixStack2, recoveredClass0169Array, floatArray, local6216_11 ? positionMatrix : null);
      }

      int local6216_15 = intVal;
      if (local6216_15 > 0) {
         orderedRenderCommandQueue.submitCustom(
            matrixStack,
            renderLayer,
            (entry, vertexConsumer) -> processData(
               recoveredClass0169Array, floatArray, local6216_15, entry.getPositionMatrix(), entry.getNormalMatrix(), vertexConsumer, local6216_3
            )
         );
      }

      matrixStack.pop();
      return true;
   }

   public static boolean renderElement7(
      CosmeticEntry cosmeticEntry,
      MatrixStack matrixStack,
      VertexConsumerProvider vertexConsumerProvider,
      int local6217_3,
      boolean local6217_4,
      boolean local6217_5,
      boolean local6217_6,
      boolean local6217_7,
      float local6217_8
   ) {
      CosmeticAnimationState cosmeticAnimationState = getCosmeticAnimationState(cosmeticEntry, local6217_4, local6217_5, local6217_6, local6217_7, local6217_8);
      if (cosmeticAnimationState == null) {
         return false;
      }

      matrixStack.push();
      renderElement(cosmeticAnimationState, matrixStack, false);
      if (!cosmeticAnimationState.bedrockModel.list.isEmpty()) {
         BedrockBone bedrockBone = cosmeticAnimationState.bedrockModel.list.get(0);
         float local6217_11 = bedrockBone.computeFloat2() / 16.0F;
         float local6217_12 = bedrockBone.computeFloat6() / 16.0F;
         float local6217_13 = bedrockBone.computeFloat12() / 16.0F;
         matrixStack.translate(local6217_11, local6217_12, local6217_13);
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
         matrixStack.scale(1.0F, -1.0F, 1.0F);
         matrixStack.translate(-local6217_11, -local6217_12, -local6217_13);
      } else {
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
         matrixStack.scale(1.0F, -1.0F, 1.0F);
      }

      VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayers.entityCutoutNoCull(cosmeticEntry.textureId()));

      try {
         for (BedrockBone bedrockBone2 : cosmeticAnimationState.bedrockModel.list) {
            renderElement2(bedrockBone2, matrixStack, vertexConsumer, local6217_3);
         }
      } finally {
         matrixStack.pop();
      }

      return true;
   }
}
