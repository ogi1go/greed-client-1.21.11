package greed.client.module.misc;

import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.NumberSetting;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

@ModuleInfo(name = "BassBoost", description = "Усиление низких частот всех игровых звуков через эквалайзер OpenAL.", category = ModuleCategory.MISC)
public final class BassBoostModule extends Module {
   private static BassBoostModule bassBoostModule;
   public NumberSetting numberSetting;
   private volatile int intVal = -1;
   private int intVal2 = -1;
   private volatile boolean flag;
   private volatile int intVal3;

   public static void updateState() {
      if (bassBoostModule != null) {
         bassBoostModule.intVal = -1;
         bassBoostModule.intVal2 = -1;
         bassBoostModule.flag = false;
         bassBoostModule.intVal3 = 0;
      }
   }

   @Override
   protected void onDisable() {
      if (this.intVal != -1) {
         EXTEfx.alAuxiliaryEffectSloti(this.intVal, 1, 0);
      }
   }

   public static void processData(int local7137_0) {
      if (bassBoostModule != null && bassBoostModule.isEnabled() && !bassBoostModule.flag) {
         if (bassBoostModule.intVal == -1) {
            bassBoostModule.updateState2();
         }

         if (bassBoostModule.intVal != -1) {
            bassBoostModule.updateState3();
            AL11.alSource3i(local7137_0, 131078, bassBoostModule.intVal, 0, 0);
         }
      }
   }

   private void updateState2() {
      try {
         AL10.alGetError();
         this.intVal2 = EXTEfx.alGenEffects();
         int local7138_1 = EXTEfx.alGenAuxiliaryEffectSlots();
         EXTEfx.alEffecti(this.intVal2, 32769, 12);
         EXTEfx.alEffectf(this.intVal2, 3, 0.126F);
         EXTEfx.alEffectf(this.intVal2, 6, 0.126F);
         EXTEfx.alEffectf(this.intVal2, 9, 0.126F);
         if (AL10.alGetError() != 0) {
            this.flag = true;
            return;
         }

         this.intVal = local7138_1;
      } catch (Throwable throwable) {
         this.flag = true;
      }
   }

   @Override
   protected void onEnable() {
      this.intVal3 = 0;
   }

   public static BassBoostModule getInstance() {
      if (bassBoostModule == null) {
         bassBoostModule = new BassBoostModule();
      }

      return bassBoostModule;
   }

   private BassBoostModule() {
      this.numberSetting = this.addSetting(
         new NumberSetting("Басы", "Насколько сильнее качают низкие частоты.").withRange(1.0F, 18.0F).withStep(1.0F).setValue(8.0F).withSuffix(" дБ")
      );
   }

   private void updateState3() {
      if (this.numberSetting.intValue() != this.intVal3) {
         this.intVal3 = this.numberSetting.intValue();
         EXTEfx.alEffectf(this.intVal2, 1, Math.max(0.126F, Math.min(7.943F, (float)Math.pow(10.0, this.intVal3 / 20.0) - 1.0F)));
         EXTEfx.alAuxiliaryEffectSloti(this.intVal, 1, this.intVal2);
      }
   }
}
