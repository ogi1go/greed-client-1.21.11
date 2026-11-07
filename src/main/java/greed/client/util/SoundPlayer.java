package greed.client.util;

import greed.mixin.ClientCommonNetworkHandlerMixin;
import greed.client.module.render.InterfaceModule;
import greed.client.MinecraftAccess;

import java.io.BufferedInputStream;
import java.util.HashMap;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;

public class SoundPlayer implements MinecraftAccess {
   private static final HashMap<String, Clip[]> map = new HashMap<>();

   public static void processData(String local5083_0) {
      if (mc.world != null
         && mc.player != null
         && !(mc.currentScreen instanceof LevelLoadingScreen)
         && ((ClientCommonNetworkHandlerMixin)mc.player.networkHandler).connection().isOpen()
         && InterfaceModule.getInstance().moduleSounds.isChecked()
         && !(InterfaceModule.getInstance().soundVolume.getValue() <= 0.0F)) {
         if (!map.containsKey(local5083_0)) {
            Clip[] clipArray = new Clip[2];
            map.put(local5083_0, clipArray);
            Thread thread = new Thread(
               () -> {
                  for (int index = 0; index < clipArray.length; index++) {
                     try {
                        Clip clip = AudioSystem.getClip();
                        clip.open(
                           AudioSystem.getAudioInputStream(
                              new BufferedInputStream(SoundPlayer.class.getResourceAsStream("/assets/greed/sound/" + local5083_0 + ".wav"))
                           )
                        );
                        clipArray[index] = clip;
                     } catch (Exception var4x) {
                     }
                  }
               }
            );
            thread.setDaemon(true);
            thread.start();
         } else {
            for (int index2 = 0; index2 < ((Clip[])map.get(local5083_0)).length; index2++) {
               if (map.get(local5083_0)[index2] != null
                  && (!map.get(local5083_0)[index2].isRunning() || index2 >= map.get(local5083_0).length - 1)) {
                  try {
                     ((FloatControl)map.get(local5083_0)[index2].getControl(Type.MASTER_GAIN))
                        .setValue(
                           Math.max(
                              20.0F * (float)Math.log10(InterfaceModule.getInstance().soundVolume.getValue() / 100.0F),
                              ((FloatControl)map.get(local5083_0)[index2].getControl(Type.MASTER_GAIN)).getMinimum()
                           )
                        );
                     map.get(local5083_0)[index2].stop();
                     map.get(local5083_0)[index2].setFramePosition(0);
                     map.get(local5083_0)[index2].start();
                  } catch (Exception exception) {
                  }

                  return;
               }
            }
         }
      }
   }
}
