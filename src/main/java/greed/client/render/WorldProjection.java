package greed.client.render;

import greed.client.MinecraftAccess;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class WorldProjection implements MinecraftAccess {
   private static final Matrix4f matrix4f3 = new Matrix4f();
   private static final Matrix4f matrix4f4 = new Matrix4f();
   private static final Matrix4f matrix4f5 = new Matrix4f();
   private static final FrustumIntersection frustumIntersection = new FrustumIntersection();

   public static Vector4f getVector4f(Box box, float local1540_1, float local1540_2) {
      if (mc.gameRenderer == null) {
         return null;
      }

      float local1540_3 = Float.MAX_VALUE;
      float local1540_4 = Float.MAX_VALUE;
      float local1540_5 = -Float.MAX_VALUE;
      float local1540_6 = -Float.MAX_VALUE;
      boolean local1540_7 = false;
      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      Vector4f vector4f = new Vector4f();

      for (int index = 0; index < 8; index++) {
         vector4f.set(
            (float)((index & 1) == 0 ? box.minX : box.maxX) - (float)cameraPos.x,
            (float)((index & 2) == 0 ? box.minY : box.maxY) - (float)cameraPos.y,
            (float)((index & 4) == 0 ? box.minZ : box.maxZ) - (float)cameraPos.z,
            1.0F
         );
         matrix4f5.transform(vector4f);
         if (!(vector4f.w <= 0.05F)) {
            local1540_7 = true;
            local1540_3 = Math.min(local1540_3, (vector4f.x / vector4f.w * 0.5F + 0.5F) * local1540_1);
            local1540_5 = Math.max(local1540_5, (vector4f.x / vector4f.w * 0.5F + 0.5F) * local1540_1);
            local1540_4 = Math.min(local1540_4, (0.5F - vector4f.y / vector4f.w * 0.5F) * local1540_2);
            local1540_6 = Math.max(local1540_6, (0.5F - vector4f.y / vector4f.w * 0.5F) * local1540_2);
         }
      }

      return local1540_7 ? new Vector4f(local1540_3, local1540_4, local1540_5 - local1540_3, local1540_6 - local1540_4) : null;
   }

   public static Matrix4f getMatrix4f() {
      return matrix4f4;
   }

   public static boolean matchesCriteria(Box box) {
      if (mc.gameRenderer == null) {
         return true;
      }

      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      return frustumIntersection.testAab(
         (float)(box.minX - cameraPos.x),
         (float)(box.minY - cameraPos.y),
         (float)(box.minZ - cameraPos.z),
         (float)(box.maxX - cameraPos.x),
         (float)(box.maxY - cameraPos.y),
         (float)(box.maxZ - cameraPos.z)
      );
   }

   public static Vec3d getVec3d(Vec3d vec3d, float local1543_1, float local1543_2) {
      if (mc.gameRenderer == null) {
         return null;
      }

      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      Vector4f vector4f = matrix4f5.transform(
         new Vector4f((float)(vec3d.x - cameraPos.x), (float)(vec3d.y - cameraPos.y), (float)(vec3d.z - cameraPos.z), 1.0F)
      );
      return vector4f.w <= 0.05F
         ? null
         : new Vec3d((vector4f.x / vector4f.w * 0.5F + 0.5F) * local1543_1, (0.5F - vector4f.y / vector4f.w * 0.5F) * local1543_2, vector4f.w);
   }

   public static void processData(Matrix4f matrix4f, Matrix4f matrix4f2) {
      matrix4f3.set(matrix4f);
      matrix4f4.set(matrix4f2);
      matrix4f5.set(matrix4f2).mul(matrix4f);
      frustumIntersection.set(matrix4f5);
   }

   public static boolean matchesCriteria2(double local1545_0, double local1545_2, double local1545_4, float local1545_6) {
      if (mc.gameRenderer == null) {
         return true;
      }

      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      return frustumIntersection.testSphere(
         (float)(local1545_0 - cameraPos.x), (float)(local1545_2 - cameraPos.y), (float)(local1545_4 - cameraPos.z), local1545_6
      );
   }

   public static Vec3d getVec3d2(Vec3d vec3d, float local1546_1, float local1546_2) {
      Vec3d vec3d2 = getVec3d(vec3d, local1546_1, local1546_2);
      return vec3d2 != null
            && !(vec3d2.x < -local1546_1 * 0.25F)
            && !(vec3d2.x > local1546_1 * 1.25F)
            && !(vec3d2.y < -local1546_2 * 0.25F)
            && !(vec3d2.y > local1546_2 * 1.25F)
         ? vec3d2
         : null;
   }

   public static Matrix4f getMatrix4f2() {
      return matrix4f3;
   }
}
