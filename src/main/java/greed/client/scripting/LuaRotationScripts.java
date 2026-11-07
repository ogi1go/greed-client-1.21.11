package greed.client.scripting;

import greed.client.module.combat.AttackAuraModule;
import greed.client.MinecraftAccess;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import net.minecraft.entity.LivingEntity;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public final class LuaRotationScripts implements MinecraftAccess {
   private static final LinkedHashMap<String, LuaFunction> map = new LinkedHashMap<>();
   private final ArrayList<String> list = new ArrayList<>();

   public static boolean matchesCriteria(String local1037_0) {
      return map.get(local1037_0) != null;
   }

   public boolean matchesCriteria2(LuaClientApi.LuaTargetEntity local1039_1, float local1039_2) {
      return local1039_1 != null && mc.player != null
         ? local1039_1.getEntity()
               .getEntityPos()
               .add(0.0, local1039_1.getEntity().getStandingEyeHeight() - 0.3F, 0.0)
               .subtract(mc.player.getEyePos())
               .horizontalLengthSquared()
            < local1039_2
         : false;
   }

   public void updateState() {
      for (String local1040_2 : new ArrayList<>(this.list)) {
         this.processData(local1040_2);
      }
   }

   public boolean matchesCriteria3(LuaClientApi.LuaTargetEntity local1041_1) {
      return local1041_1 != null && mc.player != null
         ? local1041_1.getEntity()
               .getEntityPos()
               .add(0.0, local1041_1.getEntity().getStandingEyeHeight() - 0.3F, 0.0)
               .subtract(mc.player.getEyePos())
               .horizontalLengthSquared()
            < 0.0925
         : false;
   }

   public static void executeRotationScript(String local1042_0, LivingEntity livingEntity, boolean local1042_2, float local1042_3) {
      if (map.get(local1042_0) != null && livingEntity != null) {
         map.get(local1042_0)
            .call(CoerceJavaToLua.coerce(new LuaClientApi.LuaTargetEntity(livingEntity)), LuaValue.valueOf(local1042_2), LuaValue.valueOf(local1042_3));
      }
   }

   public void processData(String local1043_1) {
      map.remove(local1043_1);
      this.list.remove(local1043_1);
      if (AttackAuraModule.getInstance().rotationType.isSelected(local1043_1)) {
         AttackAuraModule.getInstance().rotationType.withSelectedOption("FunTime");
      }

      AttackAuraModule.getInstance().rotationType.getOptions().remove(local1043_1);
   }

   public void processData2(String local1044_1, LuaFunction luaFunction) {
      map.put(local1044_1, luaFunction);
      if (!this.list.contains(local1044_1)) {
         this.list.add(local1044_1);
      }

      if (!AttackAuraModule.getInstance().rotationType.getOptions().contains(local1044_1)) {
         AttackAuraModule.getInstance().rotationType.getOptions().add(local1044_1);
      }
   }
}
