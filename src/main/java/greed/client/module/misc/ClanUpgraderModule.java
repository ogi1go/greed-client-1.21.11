package greed.client.module.misc;

import greed.client.util.ClientChat;
import greed.client.event.EventListener;
import greed.client.util.MathUtils;
import greed.mixin.MinecraftClientMixin6;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;

import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

@ModuleInfo(name = "ClanUpgrader", description = "Быстро прокачивает клан с помощью редстоуна и факела.", category = ModuleCategory.MISC)
public final class ClanUpgraderModule extends Module {
   private static ClanUpgraderModule clanUpgraderModule;
   private int intVal = -1;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         int local1997_2 = InventoryUtils.findHotbarSlot(Items.REDSTONE);
         int local1997_3 = InventoryUtils.findHotbarSlot(Items.TORCH);
         int local1997_4 = local1997_2 != -1 ? local1997_2 : local1997_3;
         if (local1997_4 == -1) {
            ClientChat.processData8("Вам необходимо иметь факел или редстоун в хотбаре");
            this.toggle();
         } else {
            float local1997_5 = (float)(Math.sin(System.currentTimeMillis() / 1220.0) * (Math.abs(90.0F - mc.player.getPitch()) / 8.0F))
               + MathUtils.randomFloat(-0.1F, 0.1F);
            RotationAngles rotationAngles = new RotationAngles(
               mc.player.getYaw() + MathUtils.randomFloat(-1.0F, 1.0F), MathHelper.clamp(88.0F + local1997_5, -90.0F, 90.0F)
            );
            RotationManager.getRotationManager().setRotationSimple(rotationAngles, new RotationSpeed(90.0F, 90.0F, 90.0F, 90.0F));
            if (this.intVal == -1) {
               this.intVal = InventoryUtils.getSelectedSlot();
            }

            if (InventoryUtils.getSelectedSlot() != local1997_4) {
               mc.player.getInventory().setSelectedSlot(local1997_4);
            }

            if (!(
               Math.hypot(
                     Math.abs(MathHelper.wrapDegrees(rotationAngles.getYaw() - mc.player.getYaw())),
                     Math.abs(MathHelper.wrapDegrees(rotationAngles.getPitch() - mc.player.getPitch()))
                  )
                  > 1.0
            )) {
               BlockPos blockPos = mc.player.getBlockPos();
               if (!mc.world.getBlockState(blockPos).isOf(Blocks.REDSTONE_WIRE) && !mc.world.getBlockState(blockPos).isOf(Blocks.TORCH)) {
                  ((MinecraftClientMixin6)mc).invokeStartUseItem();
               } else {
                  mc.interactionManager.attackBlock(blockPos, Direction.UP);
                  mc.player.swingHand(Hand.MAIN_HAND);
               }
            }
         }
      }
   };

   public static ClanUpgraderModule getInstance() {
      if (clanUpgraderModule == null) {
         clanUpgraderModule = new ClanUpgraderModule();
      }

      return clanUpgraderModule;
   }

   private ClanUpgraderModule() {
   }

   @Override
   protected void onDisable() {
      if (mc.player != null && this.intVal != -1) {
         mc.player.getInventory().setSelectedSlot(this.intVal);
         this.intVal = -1;
      }
   }
}
