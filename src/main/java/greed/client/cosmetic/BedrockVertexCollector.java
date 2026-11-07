package greed.client.cosmetic;

import greed.client.module.render.TagsModule;

import java.util.ArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BedrockVertexCollector {
   public static final int intVal = 5;
   private static float[] floatValArray = new float[7680];
   private static int intVal2;
   private static final ArrayList<Identifier> list = new ArrayList<>(4);
   private static final ArrayList<int[]> list2 = new ArrayList<>(4);
   private static final Vector3f vector3f = new Vector3f();

   public static Identifier getIdentifier(int local3569_0) {
      return list.get(local3569_0);
   }

   public static void updateState() {
      intVal2 = 0;
      list.clear();
      list2.clear();
   }

   public static void processData(BedrockCube bedrockCube, Matrix4f matrix4f) {
      if (!list2.isEmpty()) {
         for (BedrockQuad bedrockQuad : bedrockCube.recoveredClass0278Array) {
            if (bedrockQuad != null) {
               if (intVal2 + 20 > floatValArray.length) {
                  float[] floatArray = new float[Math.max(floatValArray.length * 2, intVal2 + 20)];
                  System.arraycopy(floatValArray, 0, floatArray, 0, intVal2);
                  floatValArray = floatArray;
               }

               for (BedrockVertex bedrockVertex : bedrockQuad.recoveredClass0057Array) {
                  matrix4f.transformPosition(
                     vector3f.set(bedrockVertex.modelVec3.computeFloat(), bedrockVertex.modelVec3.computeFloat3(), bedrockVertex.modelVec3.computeFloat2())
                  );
                  floatValArray[intVal2++] = vector3f.x;
                  floatValArray[intVal2++] = vector3f.y;
                  floatValArray[intVal2++] = vector3f.z;
                  floatValArray[intVal2++] = bedrockVertex.floatVal;
                  floatValArray[intVal2++] = bedrockVertex.floatVal2;
               }

               list2.get(list2.size() - 1)[1]++;
            }
         }
      }
   }

   private BedrockVertexCollector() {
   }

   public static int computeInt(int local3573_0) {
      return list2.get(local3573_0)[0];
   }

   public static float computeFloat(int local3575_0) {
      return floatValArray[local3575_0];
   }

   public static void processData2(Identifier identifier) {
      list.add(identifier);
      list2.add(new int[]{intVal2 / 20, 0});
   }

   public static int computeInt2(int local3577_0) {
      return list2.get(local3577_0)[1];
   }

   public static int computeInt3() {
      return list.size();
   }

   public static boolean checkState() {
      return TagsModule.getInstance().isEnabled()
         && TagsModule.getInstance().applyShader.isChecked()
         && MinecraftClient.getInstance().player != null
         && TagsModule.getInstance().matchesCriteria5(MinecraftClient.getInstance().player);
   }

   public static int computeInt4() {
      return intVal2 / 20;
   }
}
