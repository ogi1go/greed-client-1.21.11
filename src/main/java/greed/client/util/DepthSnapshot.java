package greed.client.util;

import greed.client.MinecraftAccess;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gl.Framebuffer;

public final class DepthSnapshot implements MinecraftAccess {
   private static GpuTexture gpuTexture;
   private static GpuTextureView gpuTextureView;
   private static boolean flag;
   private static long timeMillis;

   public static GpuTextureView getGpuTextureView() {
      return flag && gpuTextureView != null && !gpuTextureView.isClosed() ? gpuTextureView : mc.getFramebuffer().getDepthAttachmentView();
   }

   public static void updateState() {
      if (!flag) {
         Framebuffer framebuffer = mc.getFramebuffer();
         if (RenderSystem.getDevice() != null
            && framebuffer != null
            && framebuffer.getDepthAttachment() != null
            && framebuffer.textureWidth > 0
            && framebuffer.textureHeight > 0) {
            GpuTexture depthAttachment = framebuffer.getDepthAttachment();
            if (gpuTexture != null
               && (
                  gpuTexture.isClosed()
                     || gpuTexture.getWidth(0) != framebuffer.textureWidth
                     || gpuTexture.getHeight(0) != framebuffer.textureHeight
                     || gpuTexture.getFormat() != depthAttachment.getFormat()
               )) {
               updateState3();
            }

            if (gpuTexture == null) {
               gpuTexture = RenderSystem.getDevice()
                  .createTexture(() -> "greed_depth_snapshot", 15, depthAttachment.getFormat(), framebuffer.textureWidth, framebuffer.textureHeight, 1, 1);
               gpuTextureView = RenderSystem.getDevice().createTextureView(gpuTexture);
            }

            RenderSystem.getDevice()
               .createCommandEncoder()
               .copyTextureToTexture(depthAttachment, gpuTexture, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
            flag = true;
            timeMillis = System.currentTimeMillis();
         }
      }
   }

   private DepthSnapshot() {
   }

   public static void updateState2() {
      Framebuffer framebuffer = mc.getFramebuffer();
      if (RenderSystem.getDevice() != null && framebuffer != null && framebuffer.getDepthAttachment() != null) {
         updateState();
         RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(framebuffer.getDepthAttachment(), 1.0);
      }
   }

   public static void updateState3() {
      if (gpuTextureView != null) {
         gpuTextureView.close();
         gpuTextureView = null;
      }

      if (gpuTexture != null) {
         gpuTexture.close();
         gpuTexture = null;
      }

      flag = false;
   }

   public static void updateState4() {
      flag = false;
      if (gpuTexture != null && System.currentTimeMillis() - timeMillis > 5000L) {
         updateState3();
      }
   }
}
