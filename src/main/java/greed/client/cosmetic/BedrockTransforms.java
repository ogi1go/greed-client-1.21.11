package greed.client.cosmetic;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BedrockTransforms {
   private static final Vector3f vector3f2 = new Vector3f(1.0F, 0.0F, 0.0F);
   private static final Vector3f vector3f3 = new Vector3f(0.0F, 1.0F, 0.0F);
   private static final Vector3f vector3f4 = new Vector3f(0.0F, 0.0F, 1.0F);

   public static void renderElement(BedrockBone bedrockBone, MatrixStack matrixStack) {
      matrixStack.translate(-bedrockBone.computeFloat5() / 16.0F, bedrockBone.computeFloat() / 16.0F, bedrockBone.computeFloat10() / 16.0F);
   }

   public static void renderElement2(BedrockBone bedrockBone, MatrixStack matrixStack) {
      matrixStack.translate(-bedrockBone.computeFloat2() / 16.0F, -bedrockBone.computeFloat6() / 16.0F, -bedrockBone.computeFloat12() / 16.0F);
   }

   public static void renderElement3(BedrockCube bedrockCube, MatrixStack matrixStack) {
      if (bedrockCube.modelVec33.computeFloat2() != 0.0F) {
         matrixStack.multiply(getQuaternionf(vector3f4, bedrockCube.modelVec33.computeFloat2()));
      }

      if (bedrockCube.modelVec33.computeFloat3() != 0.0F) {
         matrixStack.multiply(getQuaternionf(vector3f3, bedrockCube.modelVec33.computeFloat3()));
      }

      if (bedrockCube.modelVec33.computeFloat() != 0.0F) {
         matrixStack.multiply(getQuaternionf(vector3f2, bedrockCube.modelVec33.computeFloat()));
      }
   }

   public static void renderElement4(BedrockBone bedrockBone, MatrixStack matrixStack) {
      matrixStack.scale(bedrockBone.computeFloat11(), bedrockBone.computeFloat3(), bedrockBone.computeFloat9());
   }

   public static void renderElement5(BedrockCube bedrockCube, MatrixStack matrixStack) {
      matrixStack.translate(
         -bedrockCube.modelVec32.computeFloat() / 16.0F, -bedrockCube.modelVec32.computeFloat3() / 16.0F, -bedrockCube.modelVec32.computeFloat2() / 16.0F
      );
   }

   public static void renderElement6(BedrockBone bedrockBone, MatrixStack matrixStack) {
      if (bedrockBone.computeFloat4() != 0.0F) {
         matrixStack.multiply(getQuaternionf(vector3f4, bedrockBone.computeFloat4()));
      }

      if (bedrockBone.computeFloat8() != 0.0F) {
         matrixStack.multiply(getQuaternionf(vector3f3, bedrockBone.computeFloat8()));
      }

      if (bedrockBone.computeFloat7() != 0.0F) {
         matrixStack.multiply(getQuaternionf(vector3f2, bedrockBone.computeFloat7()));
      }
   }

   public static void renderElement7(BedrockCube bedrockCube, MatrixStack matrixStack) {
      matrixStack.translate(
         bedrockCube.modelVec32.computeFloat() / 16.0F, bedrockCube.modelVec32.computeFloat3() / 16.0F, bedrockCube.modelVec32.computeFloat2() / 16.0F
      );
   }

   public static void renderElement8(BedrockBone bedrockBone, MatrixStack matrixStack) {
      matrixStack.translate(bedrockBone.computeFloat2() / 16.0F, bedrockBone.computeFloat6() / 16.0F, bedrockBone.computeFloat12() / 16.0F);
   }

   public static Quaternionf getQuaternionf(Vector3f vector3f, float local7452_1) {
      float local7452_2 = (float)Math.sin(local7452_1 / 2.0F);
      float local7452_3 = vector3f.x() * local7452_2;
      float local7452_4 = vector3f.y() * local7452_2;
      float local7452_5 = vector3f.z() * local7452_2;
      float local7452_6 = (float)Math.cos(local7452_1 / 2.0F);
      return new Quaternionf(local7452_3, local7452_4, local7452_5, local7452_6);
   }
}
