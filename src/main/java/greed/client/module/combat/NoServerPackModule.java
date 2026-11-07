package greed.client.module.combat;

import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;

@ModuleInfo(
   name = "NoServerPack",
   description = "Блокирует подкачку и включение серверного ресурспака, отвечая серверу что пак загружен.",
   category = ModuleCategory.COMBAT
)
public final class NoServerPackModule extends Module {
   private static NoServerPackModule noServerPackModule;

   @Override
   protected void onEnable() {
      mc.getServerResourcePackProvider().removeAll();
   }

   public static NoServerPackModule getInstance() {
      if (noServerPackModule == null) {
         noServerPackModule = new NoServerPackModule();
      }

      return noServerPackModule;
   }

   private NoServerPackModule() {
   }
}
