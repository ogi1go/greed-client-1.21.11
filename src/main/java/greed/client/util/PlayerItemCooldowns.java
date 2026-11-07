package greed.client.util;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class PlayerItemCooldowns {
   private static final Map<UUID, PlayerItemCooldowns.DataRecord> map = new HashMap<>();
   private static ClientWorld clientWorld2;

   public static Map<ServerItemType, Long> getMap(AbstractClientPlayerEntity abstractClientPlayerEntity) {
      PlayerItemCooldowns.DataRecord local4179_1 = map.get(abstractClientPlayerEntity.getUuid());
      if (local4179_1 != null && local4179_1.entityId() == abstractClientPlayerEntity.getId()) {
         if (abstractClientPlayerEntity != MinecraftClient.getInstance().player) {
            return local4179_1.expiresAtMs();
         }

         EnumMap<ServerItemType, Long> local4179_2 = new EnumMap<>(ServerItemType.class);
         long local4179_3 = System.currentTimeMillis();

         for (ServerItemType serverItemType : local4179_1.expiresAtMs().keySet()) {
            int local4179_7 = 0;

            for (ServerItemType.DataRecord local4179_9 : serverItemType.getList()) {
               StatusEffectInstance statusEffectInstance = abstractClientPlayerEntity.getStatusEffect(local4179_9.effect());
               if (statusEffectInstance == null || !ServerItemType.matchesCriteria(statusEffectInstance.getAmplifier(), local4179_9.amplifier())) {
                  local4179_7 = 0;
                  break;
               }

               local4179_7 = Math.max(local4179_7, statusEffectInstance.getDuration());
            }

            if (local4179_7 > 0) {
               local4179_2.put(serverItemType, local4179_3 + local4179_7 * 50L);
            }
         }

         return local4179_2;
      } else {
         return Map.of();
      }
   }

   private PlayerItemCooldowns() {
   }

   public static void processData(ServerItemType serverItemType, BlockPos blockPos) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient.world != null && ServerItemType.checkState()) {
         processData2(minecraftClient.world);
         Vec3d vec3d = blockPos.toCenterPos();
         long local4184_4 = System.currentTimeMillis();

         for (AbstractClientPlayerEntity abstractClientPlayerEntity : minecraftClient.world.getPlayers()) {
            if (abstractClientPlayerEntity.isAlive()) {
               int local4184_8 = serverItemType.computeInt2(ServerItemType.computeDouble(abstractClientPlayerEntity.getBoundingBox(), vec3d));
               if (local4184_8 > 0) {
                  PlayerItemCooldowns.DataRecord local4184_9 = map.get(abstractClientPlayerEntity.getUuid());
                  if (local4184_9 == null || local4184_9.entityId() != abstractClientPlayerEntity.getId()) {
                     local4184_9 = new PlayerItemCooldowns.DataRecord(abstractClientPlayerEntity.getId(), new EnumMap<>(ServerItemType.class));
                     map.put(abstractClientPlayerEntity.getUuid(), local4184_9);
                  }

                  local4184_9.expiresAtMs().merge(serverItemType, local4184_4 + local4184_8 * 50L, Math::max);
               }
            }
         }
      }
   }

   public static void processData2(ClientWorld clientWorld) {
      if (clientWorld != clientWorld2) {
         map.clear();
         clientWorld2 = clientWorld;
      }

      if (!map.isEmpty()) {
         long local4185_1 = System.currentTimeMillis();
         map.entrySet()
            .removeIf(
               local4183_3 -> {
                  PlayerEntity playerEntity = clientWorld.getPlayerByUuid(local4183_3.getKey());
                  if (playerEntity != null
                     && !playerEntity.isRemoved()
                     && !(playerEntity.getHealth() <= 0.0F)
                     && playerEntity.getId() == local4183_3.getValue().entityId()) {
                     local4183_3.getValue().expiresAtMs().values().removeIf(local4182_2 -> local4182_2 <= local4185_1);
                     return local4183_3.getValue().expiresAtMs().isEmpty();
                  } else {
                     return true;
                  }
               }
            );
      }
   }

   private record DataRecord(int entityId, EnumMap<ServerItemType, Long> expiresAtMs) {
   }
}
