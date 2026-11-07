package greed.client.module.combat;

import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;

@ModuleInfo(
   name = "NoEntityTrace",
   description = "Убирает хитбокс у всех сущностей в мире позволяя вам взаимодействовать через них.",
   category = ModuleCategory.COMBAT
)
public final class NoEntityTraceModule extends Module {
   private static NoEntityTraceModule noEntityTraceModule;

   private NoEntityTraceModule() {
   }

   public static NoEntityTraceModule getInstance() {
      if (noEntityTraceModule == null) {
         noEntityTraceModule = new NoEntityTraceModule();
      }

      return noEntityTraceModule;
   }
}
