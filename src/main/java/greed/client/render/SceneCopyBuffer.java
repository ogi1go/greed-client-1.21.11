package greed.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;

public final class SceneCopyBuffer {
   private static SimpleFramebuffer framebuffer2;
   private static long timeMillis;

   public static GpuTextureView getGpuTextureView(Framebuffer framebuffer) {
      if (framebuffer != null && framebuffer.getColorAttachment() != null && framebuffer.textureWidth > 0 && framebuffer.textureHeight > 0) {
         if (framebuffer2 == null) {
            framebuffer2 = new SimpleFramebuffer("greed_scene_copy", framebuffer.textureWidth, framebuffer.textureHeight, false);
         }

         if (framebuffer2.textureWidth != framebuffer.textureWidth || framebuffer2.textureHeight != framebuffer.textureHeight) {
            framebuffer2.resize(framebuffer.textureWidth, framebuffer.textureHeight);
         }

         if (framebuffer2.getColorAttachment() == null) {
            return null;
         }

         RenderSystem.getDevice()
            .createCommandEncoder()
            .copyTextureToTexture(
               framebuffer.getColorAttachment(), framebuffer2.getColorAttachment(), 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight
            );
         timeMillis = System.currentTimeMillis();
         return framebuffer2.getColorAttachmentView();
      } else {
         return null;
      }
   }

   public static void updateState() {
      if (framebuffer2 != null) {
         framebuffer2.delete();
         framebuffer2 = null;
      }
   }

   private SceneCopyBuffer() {
   }

   public static void updateState2() {
      if (framebuffer2 != null && System.currentTimeMillis() - timeMillis > 5000L) {
         updateState();
      }
   }
}
