package greed.client.render;

import greed.client.MinecraftAccess;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap.Type;

public final class RoofHeightMap implements MinecraftAccess {
   public static final int intVal = 64;
   public static final int intVal2 = 128;
   public static int intVal3 = Integer.MIN_VALUE;
   public static int intVal4;
   public static int intVal5;
   private static NativeImage nativeImage;
   private static GpuTexture gpuTexture;
   private static GpuTextureView gpuTextureView;
   private static long timeMillis;

   private RoofHeightMap() {
   }

   public static void updateState() {
      if (mc.world != null) {
         if (gpuTexture == null || gpuTexture.isClosed()) {
            nativeImage = new NativeImage(64, 64, false);
            gpuTexture = RenderSystem.getDevice().createTexture(() -> "greed_roof_map", 5, TextureFormat.RGBA8, 64, 64, 1, 1);
            gpuTextureView = RenderSystem.getDevice().createTextureView(gpuTexture);
            intVal3 = Integer.MIN_VALUE;
         }

         int local5406_0 = (MathHelper.floor(mc.gameRenderer.getCamera().getCameraPos().x) & -8) - 32;
         int local5406_1 = (MathHelper.floor(mc.gameRenderer.getCamera().getCameraPos().z) & -8) - 32;
         int local5406_2 = MathHelper.floor(mc.gameRenderer.getCamera().getCameraPos().y) & -8;
         if (local5406_0 != intVal3 || local5406_1 != intVal4 || local5406_2 != intVal5 || System.currentTimeMillis() - timeMillis > 500L) {
            for (int index = 0; index < 64; index++) {
               for (int index2 = 0; index2 < 64; index2++) {
                  int local5406_5 = MathHelper.clamp(
                     mc.world.getTopY(Type.MOTION_BLOCKING, local5406_0 + index2, local5406_1 + index) - local5406_2 + 128, 0, 255
                  );
                  nativeImage.setColor(index2, index, 0xFF000000 | local5406_5 << 16 | local5406_5 << 8 | local5406_5);
               }
            }

            RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpuTexture, nativeImage);
            intVal3 = local5406_0;
            intVal4 = local5406_1;
            intVal5 = local5406_2;
            timeMillis = System.currentTimeMillis();
         }
      }
   }

   public static GpuTextureView getGpuTextureView() {
      return gpuTextureView;
   }
}
