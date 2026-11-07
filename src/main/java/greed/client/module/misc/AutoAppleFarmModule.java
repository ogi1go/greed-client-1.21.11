package greed.client.module.misc;

import greed.mixin.BossBarHudMixin;
import greed.client.util.ClientChat;
import greed.client.event.EventListener;
import greed.client.render.FontFace;
import greed.client.event.HudRenderEvent;
import greed.client.setting.LegacyNumberSetting;
import greed.mixin.MinecraftClientMixin3;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.render.FontText;
import greed.client.util.InventoryActionQueue;
import greed.client.render.ThemeColors;
import greed.client.util.AimPointUtils;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;
import greed.client.util.Stopwatch;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(
   name = "AutoAppleFarm",
   description = "Сажает дуб, растит костной мукой, ломает крону мотыгой и ствол топором, чинит мотыгу опытом с аукциона.",
   category = ModuleCategory.MISC
)
public final class AutoAppleFarmModule extends Module {
   private static AutoAppleFarmModule autoAppleFarmModule;
   public ModeSetting modeSetting;
   public LegacyNumberSetting legacyNumberSetting;
   private AutoAppleFarmModule.FarmState nested0377 = AutoAppleFarmModule.FarmState.PLACE_SAPLING;
   private BlockPos blockPos3;
   private BlockPos blockPos4;
   private BlockPos blockPos5;
   private int intVal;
   private int intVal2;
   private int intVal3;
   private int intVal4;
   private Boolean valBoolean;
   private long timeMillis;
   private BlockPos blockPos6;
   private int intVal5;
   private int intVal6;
   private int intVal7;
   private Set<BlockPos> set = new HashSet<>();
   private BlockPos blockPos7;
   private int intVal8;
   private int intVal9;
   private int intVal10;
   private boolean flag;
   private final Stopwatch timer = new Stopwatch();
   private final Stopwatch timer2 = new Stopwatch();
   private boolean flag2;
   private long timeMillis2;
   private long timeMillis3;
   private long timeMillis4;
   private int intVal11;
   private int intVal12;
   private Set<Integer> set2 = new HashSet<>();
   private long timeMillis5;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (this.isEnabled()) {
         if (mc.player != null && mc.world != null) {
            if (mc.currentScreen instanceof GameMenuScreen) {
               this.updateState11();
            } else {
               this.updateState8();
               this.updateState4();
               if (!this.checkState8()) {
                  if (this.nested0377 != AutoAppleFarmModule.FarmState.PLACE_SAPLING || !this.checkState10()) {
                     if (this.checkState5()) {
                        switch (this.nested0377) {
                           case PLACE_SAPLING:
                              this.updateState6();
                              break;
                           case GROW:
                              this.updateState();
                              break;
                           case BREAK_LEAVES:
                              this.updateState9();
                              break;
                           case BREAK_LOG:
                              this.updateState3();
                        }
                     }
                  }
               }
            }
         }
      }
   };
   private final EventListener<HudRenderEvent> hudRenderListener = event -> this.onHudRender(event);

   private boolean matchesCriteria(BlockPos blockPos) {
      return mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos)) <= mc.player.getBlockInteractionRange() - 0.1;
   }

   private BlockPos getBlockPos2() {
      BlockPos blockPos = null;
      double local1634_2 = Double.MAX_VALUE;

      for (int index = -1; index <= 1; index++) {
         for (int index2 = -1; index2 <= 1; index2++) {
            BlockPos blockPos2 = new BlockPos(
               (int)Math.floor(mc.player.getX()) + index,
               (int)Math.floor(mc.player.getY()) - 1,
               (int)Math.floor(mc.player.getZ()) + index2
            );
            if (this.matchesCriteria4(blockPos2)
               && (mc.world.getBlockState(blockPos2.up()).isAir() || mc.world.getBlockState(blockPos2.up()).isOf(Blocks.OAK_SAPLING))
               && mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos2)) <= mc.player.getBlockInteractionRange()
               && mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos2)) < local1634_2) {
               local1634_2 = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos2));
               blockPos = blockPos2.up().toImmutable();
            }
         }
      }

      return blockPos;
   }

   private float computeFloat() {
      return 2.0F + ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().size() * 19.0F;
   }

   private static String formatText(long local1638_0) {
      return String.format("%02d:%02d:%02d", local1638_0 / 1000L / 3600L, local1638_0 / 1000L % 3600L / 60L, local1638_0 / 1000L % 60L);
   }

   private void processData(Vec3d vec3d, float local1639_2) {
      this.processData3(AimPointUtils.getRotationAngles(vec3d.subtract(mc.player.getEyePos())), local1639_2);
   }

   private boolean matchesCriteria2(Block block) {
      for (int index = -5; index <= 5; index++) {
         for (int index2 = -5; index2 <= 5; index2++) {
            for (int index3 = -5; index3 <= 5; index3++) {
               if (mc.world
                  .getBlockState(
                     new BlockPos(
                        (int)Math.floor(mc.player.getX()) + index,
                        (int)Math.floor(mc.player.getY()) + index2,
                        (int)Math.floor(mc.player.getZ()) + index3
                     )
                  )
                  .isOf(block)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public static AutoAppleFarmModule getInstance() {
      if (autoAppleFarmModule == null) {
         autoAppleFarmModule = new AutoAppleFarmModule();
      }

      return autoAppleFarmModule;
   }

   private void updateState() {
      if (this.blockPos3 == null || !mc.world.getBlockState(this.blockPos3).isOf(Blocks.OAK_SAPLING)) {
         this.nested0377 = AutoAppleFarmModule.FarmState.BREAK_LEAVES;
      } else if (this.intVal2 > 0) {
         this.intVal2--;
      } else if (this.matchesCriteria(this.blockPos3)) {
         if (this.computeInt3() != -1) {
            boolean local1643_1 = this.matchesCriteria9(this.computeInt3());
            this.processData5(Vec3d.ofCenter(this.blockPos3));
            if (local1643_1 && this.matchesCriteria6(Vec3d.ofCenter(this.blockPos3))) {
               this.matchesCriteria3(this.getBlockHitResult(this.blockPos3, Direction.UP));
               this.intVal2 = 1;
            }
         }
      }
   }

   private BlockPos getBlockPos3(Block block) {
      BlockPos blockPos = null;
      double local1644_3 = Double.MAX_VALUE;

      for (int index = -5; index <= 5; index++) {
         for (int index2 = -5; index2 <= 5; index2++) {
            for (int index3 = -5; index3 <= 5; index3++) {
               BlockPos blockPos2 = new BlockPos(
                  (int)Math.floor(mc.player.getX()) + index,
                  (int)Math.floor(mc.player.getY()) + index2,
                  (int)Math.floor(mc.player.getZ()) + index3
               );
               if (mc.world.getBlockState(blockPos2).isOf(block)
                  && mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos2)) <= mc.player.getBlockInteractionRange() - 0.1
                  && mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos2)) < local1644_3) {
                  local1644_3 = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos2));
                  blockPos = blockPos2;
               }
            }
         }
      }

      return blockPos;
   }

   private void updateState2() {
      this.nested0377 = AutoAppleFarmModule.FarmState.PLACE_SAPLING;
      this.blockPos3 = null;
      this.blockPos4 = null;
      this.blockPos5 = null;
      this.intVal = 0;
      this.intVal2 = 0;
      this.intVal3 = 0;
      this.intVal4 = 0;
   }

   private void updateState3() {
      if (this.getBlockPos3(Blocks.OAK_LOG) == null) {
         this.updateState11();
         this.updateState2();
         this.intVal = 16;
      } else {
         this.processData4(this.getBlockPos3(Blocks.OAK_LOG), 16.0F);
      }
   }

   private int computeInt() {
      int local1647_1 = -1;

      for (int index = 0; index < 9; index++) {
         if (mc.player.getInventory().getStack(index).isOf(Items.OAK_SAPLING)
            && (
               local1647_1 == -1
                  || mc.player.getInventory().getStack(index).getCount() > mc.player.getInventory().getStack(local1647_1).getCount()
            )) {
            local1647_1 = index;
         }
      }

      return local1647_1;
   }

   @Override
   protected void onDisable() {
      this.updateState11();
      this.intVal10 = 0;
      this.flag2 = false;
      this.intVal5 = 0;
      this.intVal6 = 0;
      this.blockPos6 = null;
      this.intVal8 = 0;
      this.intVal9 = 0;
      this.blockPos7 = null;
      this.timeMillis4 = 0L;
      this.updateState10();
      if (this.valBoolean != null && mc.options != null) {
         mc.options.pauseOnLostFocus = this.valBoolean;
         this.valBoolean = null;
      }
   }

   private void updateState4() {
      if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
         if (System.currentTimeMillis() >= this.timeMillis5) {
            if (InventoryUtils.findEmptySlot(9, 35) != -1) {
               for (int index = 0; index < 9; index++) {
                  if (!this.matchesCriteria5(index)) {
                     InventoryUtils.sendSlotClickPacket(InventoryUtils.getScreenSlotId(index), 0, SlotActionType.QUICK_MOVE, false);
                     this.timeMillis5 = System.currentTimeMillis() + 250L;
                     return;
                  }
               }
            }
         }
      }
   }

   private boolean checkState() {
      return mc.player.currentScreenHandler instanceof GenericContainerScreenHandler;
   }

   private String formatText2() {
      if (this.intVal10 == 1) {
         return "BUYING XP";
      } else if (this.intVal10 == 2) {
         return "REPAIRING";
      } else if (this.intVal5 != 0) {
         return "RESTOCKING";
      } else if (this.intVal8 != 0) {
         return "UNLOADING";
      } else {
         return !this.checkState9() ? "NO ITEMS" : "WORKING";
      }
   }

   private void updateState5() {
      if (mc.options != null) {
         mc.options.attackKey.setPressed(false);
      }
   }

   private boolean matchesCriteria3(BlockHitResult blockHitResult) {
      if (!this.matchesCriteria(blockHitResult.getBlockPos())) {
         return false;
      }

      mc.crosshairTarget = blockHitResult;
      if (!mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult).isAccepted()) {
         return false;
      }

      mc.player.swingHand(Hand.MAIN_HAND);
      return true;
   }

   private void processData2(BlockPos blockPos) {
      mc.crosshairTarget = this.getBlockHitResult(blockPos, this.getDirection(blockPos));
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, this.getBlockHitResult(blockPos, this.getDirection(blockPos)));
      mc.player.swingHand(Hand.MAIN_HAND);
   }

   private void updateState6() {
      this.updateState11();
      if (this.intVal > 0) {
         this.intVal--;
      } else if (this.blockPos5 != null) {
         if (this.intVal3 > 0) {
            this.intVal3--;
         } else if (mc.world.getBlockState(this.blockPos5).isOf(Blocks.OAK_SAPLING)) {
            this.blockPos3 = this.blockPos5.toImmutable();
            this.blockPos5 = null;
            this.intVal4 = 0;
            this.nested0377 = AutoAppleFarmModule.FarmState.GROW;
         } else {
            this.blockPos5 = null;
            this.intVal4++;
            this.intVal = 2;
            if (this.intVal4 >= 6) {
               this.intVal4 = 0;
               this.intVal = 20;
            }
         }
      } else {
         BlockPos blockPos = this.getBlockPos2();
         if (blockPos != null) {
            if (mc.world.getBlockState(blockPos).isOf(Blocks.OAK_LOG) || mc.world.getBlockState(blockPos).isOf(Blocks.OAK_LEAVES)) {
               this.nested0377 = AutoAppleFarmModule.FarmState.BREAK_LEAVES;
            } else if (mc.world.getBlockState(blockPos).isOf(Blocks.OAK_SAPLING)) {
               this.blockPos3 = blockPos.toImmutable();
               this.nested0377 = AutoAppleFarmModule.FarmState.GROW;
            } else if (mc.world.getBlockState(blockPos).isAir()) {
               if (this.matchesCriteria4(blockPos.down()) && this.matchesCriteria(blockPos.down())) {
                  if (InventoryUtils.findSlotMatching(itemStack -> itemStack.isOf(Items.OAK_SAPLING), 0, 8) != -1) {
                     boolean local1658_2 = this.matchesCriteria9(InventoryUtils.findSlotMatching(itemStack2 -> itemStack2.isOf(Items.OAK_SAPLING), 0, 8));
                     this.processData5(Vec3d.ofCenter(blockPos.down()).add(0.0, 0.5, 0.0));
                     if (local1658_2 && this.matchesCriteria6(Vec3d.ofCenter(blockPos.down()).add(0.0, 0.5, 0.0))) {
                        if (!this.matchesCriteria3(this.getBlockHitResult(blockPos.down(), Direction.UP))) {
                           this.intVal4++;
                           this.intVal = 2;
                           if (this.intVal4 >= 6) {
                              this.intVal4 = 0;
                              this.intVal = 20;
                           }
                        } else {
                           this.blockPos5 = blockPos.toImmutable();
                           this.intVal3 = 2;
                           this.intVal = 1;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean matchesCriteria4(BlockPos blockPos) {
      return mc.world.getBlockState(blockPos).isOf(Blocks.GRASS_BLOCK)
         || mc.world.getBlockState(blockPos).isOf(Blocks.DIRT)
         || mc.world.getBlockState(blockPos).isOf(Blocks.COARSE_DIRT)
         || mc.world.getBlockState(blockPos).isOf(Blocks.ROOTED_DIRT);
   }

   private void processData3(RotationAngles rotationAngles, float local1660_2) {
      RotationManager.getRotationManager().setRotationWithTimeout(rotationAngles, new RotationSpeed(local1660_2, local1660_2, 45.0F, 45.0F), 300L);
   }

   private void updateState7() {
      this.intVal10 = 0;
      this.flag2 = false;
      if (mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE) && InventoryUtils.findEmptySlot(0, 35) != -1) {
         InventoryActionQueue.getInstance().queueSlotSwap(InventoryUtils.findEmptySlot(0, 35), 40, 1);
      }

      this.updateState10();
   }

   private int computeInt2() {
      return this.flag ? this.computeInt5() : this.computeInt6();
   }

   private int computeInt3() {
      return this.modeSetting.isSelected("Озеленитель")
         ? this.computeInt6()
         : InventoryUtils.findSlotMatching(itemStack -> itemStack.isOf(Items.BONE_MEAL), 0, 8);
   }

   private int computeInt4() {
      int local1665_1 = 0;

      for (int index = 0; index < 36; index++) {
         if (mc.player.getInventory().getStack(index).isEmpty()) {
            local1665_1++;
         }
      }

      return local1665_1;
   }

   private void updateState8() {
      for (ItemEntity itemEntity : mc.world
         .getEntitiesByClass(ItemEntity.class, mc.player.getBoundingBox().expand(10.0), itemEntity2 -> itemEntity2.getStack().isOf(Items.APPLE))) {
         if (this.set2.add(itemEntity.getId())) {
            this.intVal11 = this.intVal11 + itemEntity.getStack().getCount();
         }
      }
   }

   private boolean matchesCriteria5(int local1669_1) {
      if (mc.player.getInventory().getStack(local1669_1).isEmpty()) {
         return true;
      } else if (mc.player.getInventory().getStack(local1669_1).isIn(ItemTags.HOES)
         || mc.player.getInventory().getStack(local1669_1).isIn(ItemTags.AXES)) {
         return true;
      } else {
         return mc.player.getInventory().getStack(local1669_1).isOf(Items.BONE_MEAL)
            ? !this.modeSetting.isSelected("Озеленитель")
            : mc.player.getInventory().getStack(local1669_1).isOf(Items.OAK_SAPLING) && local1669_1 == this.computeInt();
      }
   }

   private boolean checkState2() {
      if (this.intVal8 == 0) {
         if (InventoryUtils.findEmptySlot(0, 35) != -1 && this.computeInt4() > 4) {
            return false;
         }

         if (!this.checkState6()) {
            return false;
         }

         this.blockPos7 = this.getBlockPos4();
         if (this.blockPos7 == null) {
            return false;
         }

         this.updateState11();
         this.intVal8 = 1;
         this.intVal9 = 0;
         return true;
      } else if (this.intVal8 == 1) {
         if (this.checkState()) {
            this.intVal8 = 2;
            this.intVal9 = 0;
            return true;
         }

         this.processData5(Vec3d.ofCenter(this.blockPos7));
         if (this.intVal9 % 5 == 0) {
            this.processData2(this.blockPos7);
         }

         if (++this.intVal9 > 60) {
            this.intVal8 = 0;
         }

         return true;
      } else if (this.intVal8 == 2) {
         if (!this.checkState()) {
            this.intVal8 = 0;
            return true;
         }

         if (this.checkState6() && this.intVal9 <= 200) {
            if (this.intVal9++ % 2 == 0 && this.getSlot3() != null) {
               InventoryUtils.clickSlot(this.getSlot3(), 0, SlotActionType.QUICK_MOVE, false);
            }

            return true;
         } else {
            this.intVal8 = 3;
            return true;
         }
      } else {
         InventoryUtils.closeHandledScreen(false);
         this.blockPos7 = null;
         this.intVal8 = 0;
         return true;
      }
   }

   private boolean checkState3() {
      if (this.intVal5 == 0) {
         if (this.modeSetting.isSelected("Озеленитель")) {
            return false;
         }

         if (InventoryUtils.getItemCount(Items.BONE_MEAL) > 64) {
            return false;
         }

         this.blockPos6 = this.getBlockPos5(this.set, null);
         if (this.blockPos6 == null) {
            return false;
         }

         this.updateState11();
         this.intVal7 = InventoryUtils.getItemCount(Items.BONE_MEAL) + 64 * ThreadLocalRandom.current().nextInt(3, 6);
         this.intVal5 = 1;
         this.intVal6 = 0;
         return true;
      } else if (this.intVal5 == 1) {
         if (this.checkState()) {
            this.intVal5 = 2;
            this.intVal6 = 0;
            return true;
         }

         this.processData5(Vec3d.ofCenter(this.blockPos6));
         if (this.intVal6 % 5 == 0) {
            this.processData2(this.blockPos6);
         }

         if (++this.intVal6 > 60) {
            this.set.add(this.blockPos6);
            this.intVal5 = 0;
         }

         return true;
      } else if (this.intVal5 == 2) {
         if (!this.checkState()) {
            this.intVal5 = 0;
            return true;
         }

         if (InventoryUtils.getItemCount(Items.BONE_MEAL) >= this.intVal7 || this.intVal6 > 200) {
            this.intVal5 = 3;
            return true;
         }

         if (this.getSlot2(Items.BONE_MEAL) == null) {
            if (InventoryUtils.getItemCount(Items.BONE_MEAL) <= 64) {
               this.set.add(this.blockPos6);
            }

            this.intVal5 = 3;
            return true;
         } else {
            if (this.intVal6++ % 2 == 0) {
               InventoryUtils.clickSlot(this.getSlot2(Items.BONE_MEAL), 0, SlotActionType.QUICK_MOVE, false);
            }

            return true;
         }
      } else {
         InventoryUtils.closeHandledScreen(false);
         this.blockPos6 = null;
         this.intVal5 = 0;
         return true;
      }
   }

   private boolean matchesCriteria6(Vec3d vec3d) {
      return this.matchesCriteria7(AimPointUtils.getRotationAngles(vec3d.subtract(mc.player.getEyePos())));
   }

   private void processData4(BlockPos blockPos, float local1673_2) {
      if (!this.matchesCriteria(blockPos)) {
         this.updateState5();
      } else {
         this.processData(Vec3d.ofCenter(blockPos), local1673_2);
         BlockHitResult blockHitResult = this.getBlockHitResult2();
         if (blockHitResult == null) {
            this.updateState5();
         } else {
            int local1673_4;
            if (mc.world.getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.OAK_LEAVES)) {
               local1673_4 = this.computeInt6();
            } else {
               if (!mc.world.getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.OAK_LOG)) {
                  this.updateState5();
                  return;
               }

               local1673_4 = this.computeInt5();
            }

            if (local1673_4 == -1) {
               this.updateState5();
            } else if (!this.matchesCriteria9(local1673_4)) {
               this.updateState5();
            } else {
               this.blockPos4 = blockHitResult.getBlockPos().toImmutable();
               if (mc.currentScreen == null && mc.isWindowFocused()) {
                  mc.options.attackKey.setPressed(true);
               } else {
                  mc.options.attackKey.setPressed(false);
                  mc.interactionManager.updateBlockBreakingProgress(blockHitResult.getBlockPos(), blockHitResult.getSide());
                  mc.player.swingHand(Hand.MAIN_HAND);
               }
            }
         }
      }
   }

   private String formatText3() {
      String local1675_1 = "";
      if (InventoryUtils.findSlotMatching(itemStack -> itemStack.isIn(ItemTags.HOES), 0, 8) == -1) {
         local1675_1 = local1675_1 + "мотыга, ";
      }

      if (InventoryUtils.findSlotMatching(itemStack2 -> itemStack2.isIn(ItemTags.AXES), 0, 8) == -1) {
         local1675_1 = local1675_1 + "топор, ";
      }

      if (InventoryUtils.findSlotMatching(itemStack3 -> itemStack3.isOf(Items.OAK_SAPLING), 0, 8) == -1) {
         local1675_1 = local1675_1 + "саженец дуба, ";
      }

      if (!this.modeSetting.isSelected("Озеленитель") && InventoryUtils.findSlotMatching(itemStack4 -> itemStack4.isOf(Items.BONE_MEAL), 0, 8) == -1) {
         local1675_1 = local1675_1 + "костная мука, ";
      }

      return local1675_1.isEmpty() ? "-" : local1675_1.substring(0, local1675_1.length() - 2);
   }

   private boolean matchesCriteria7(RotationAngles rotationAngles) {
      return Math.abs(MathHelper.wrapDegrees(rotationAngles.getYaw() - mc.player.getYaw())) <= 12.0F
         && Math.abs(rotationAngles.getPitch() - mc.player.getPitch()) <= 12.0F;
   }

   private long computeLong() {
      return this.timeMillis4 > 0L ? System.currentTimeMillis() - this.timeMillis4 : 0L;
   }

   private boolean checkState4() {
      if (InventoryUtils.findInventorySlot(Items.EXPERIENCE_BOTTLE) == -1 && !mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)) {
         if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen) {
            if (!genericContainerScreen.getTitle().getString().contains("Подозрительная цена") && !genericContainerScreen.getTitle().getString().contains("Подтверждение покупки")) {
               if (!AuctionHelperModule.getInstance().matchesCriteria3(genericContainerScreen.getTitle().getString())) {
                  return true;
               }

               if (this.flag2) {
                  if (!this.timer2.hasExceededMillis(3000L)) {
                     return true;
                  }

                  this.flag2 = false;
               }

               int local1681_2 = -1;
               long local1681_3 = Long.MAX_VALUE;

               for (int index = 0; index < mc.player.currentScreenHandler.slots.size() - 36 - 9; index++) {
                  if (mc.player.currentScreenHandler.getSlot(index).hasStack()
                     && mc.player.currentScreenHandler.getSlot(index).getStack().isOf(Items.EXPERIENCE_BOTTLE)
                     && !AuctionHelperModule.getInstance()
                        .formatText2(
                           mc.player.currentScreenHandler.getSlot(index).getStack().getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "")
                        )
                        .contains("не актуален")
                     && !AuctionHelperModule.getInstance()
                        .formatText2(
                           mc.player.currentScreenHandler.getSlot(index).getStack().getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "")
                        )
                        .matches(".*\\[\\s*\\d+\\s*ур.*")
                     && !mc.player
                        .currentScreenHandler
                        .getSlot(index)
                        .getStack()
                        .getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT)
                        .lines()
                        .stream()
                        .anyMatch(
                           text -> AuctionHelperModule.getInstance()
                              .formatText2(text.getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                              .matches(".*содержит.*опыт.*")
                        )
                     && AutoMineModule.getInstance().matchesCriteria6(mc.player.currentScreenHandler.getSlot(index).getStack())
                     && AuctionHelperModule.getInstance().computeInt(mc.player.currentScreenHandler.getSlot(index).getStack()) >= 0
                     && !(
                        AuctionHelperModule.getInstance().computeInt(mc.player.currentScreenHandler.getSlot(index).getStack())
                           > this.legacyNumberSetting.getValue()
                     )
                     && AuctionHelperModule.getInstance().computeInt(mc.player.currentScreenHandler.getSlot(index).getStack()) < local1681_3) {
                     local1681_2 = index;
                     local1681_3 = AuctionHelperModule.getInstance().computeInt(mc.player.currentScreenHandler.getSlot(index).getStack());
                  }
               }

               if (local1681_2 != -1) {
                  InventoryUtils.sendSlotClickPacket(local1681_2, 0, SlotActionType.PICKUP, false);
                  this.flag2 = true;
                  this.timer2.reset();
                  this.timer.reset();
                  return true;
               }

               if (this.timer.hasExceededMillis(400L)) {
                  InventoryUtils.sendSlotClickPacket(mc.player.currentScreenHandler.slots.size() - 36 - 5, 0, SlotActionType.PICKUP, false);
                  this.timer.reset();
               }

               return true;
            } else if (mc.player
               .currentScreenHandler
               .slots
               .stream()
               .anyMatch(
                  slot -> slot.hasStack()
                     && AuctionHelperModule.getInstance()
                        .formatText2(slot.getStack().getName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                        .contains("не актуален")
               )) {
               mc.player.closeHandledScreen();
               mc.player.networkHandler.sendChatCommand("ah search опыт");
               this.flag2 = false;
               this.timer.reset();
               return true;
            } else {
               InventoryUtils.sendSlotClickPacket(0, 0, SlotActionType.QUICK_MOVE, false);
               this.timer.reset();
               return true;
            }
         } else {
            if (this.timer.hasExceededMillis(3000L)) {
               mc.player.networkHandler.sendChatCommand("ah search опыт");
               this.timer.reset();
            }

            return true;
         }
      } else {
         if (mc.currentScreen != null) {
            mc.player.closeHandledScreen();
         }

         this.flag2 = false;
         this.intVal10 = 2;
         this.timer.reset();
         return true;
      }
   }

   private boolean checkState5() {
      if (this.checkState9()) {
         return true;
      }

      this.updateState11();
      if (System.currentTimeMillis() < this.timeMillis) {
         return false;
      }

      this.timeMillis = System.currentTimeMillis() + 8000L;
      ClientChat.send("&8[&bGreed Client&8] &fВ хотбаре не хватает: &c" + this.formatText3());
      return false;
   }

   private Direction getDirection(BlockPos blockPos) {
      if (Math.abs(mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).y)
            > Math.abs(mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).x)
         && Math.abs(mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).y)
            > Math.abs(mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).z)) {
         return mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).y >= 0.0 ? Direction.UP : Direction.DOWN;
      } else if (Math.abs(mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).x)
         >= Math.abs(mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).z)) {
         return mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).x >= 0.0 ? Direction.EAST : Direction.WEST;
      } else {
         return mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).z >= 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   private void onHudRender(HudRenderEvent event) {
      if (mc.player != null && mc.world != null && this.isEnabled()) {
         FontText.processData9(
            FontFace.SEMI, "AutoAppleFarm — Информация", event.computeInt2() / 2.0F, this.computeFloat(), 7.0F, ThemeColors.intVal5
         );
         FontText.processData9(
            FontFace.SEMI,
            "Текущее состояние: " + this.formatText2(),
            event.computeInt2() / 2.0F,
            this.computeFloat() + 10.0F,
            7.0F,
            ThemeColors.intVal5
         );
         FontText.processData9(
            FontFace.SEMI,
            "Налутано яблок: " + this.intVal11,
            event.computeInt2() / 2.0F,
            this.computeFloat() + 20.0F,
            7.0F,
            ThemeColors.intVal5
         );
         FontText.processData9(
            FontFace.SEMI,
            "Время работы: " + formatText(this.computeLong()),
            event.computeInt2() / 2.0F,
            this.computeFloat() + 30.0F,
            7.0F,
            ThemeColors.intVal5
         );
      }
   }

   private boolean checkState6() {
      return InventoryUtils.getItemCount(Items.OAK_LOG) > 0
         || InventoryUtils.getItemCount(Items.OAK_LEAVES) > 0
         || InventoryUtils.getItemCount(Items.STICK) > 0
         || InventoryUtils.getItemCount(Items.APPLE) > 0
         || InventoryUtils.getItemCount(Items.OAK_SAPLING) > 64;
   }

   private BlockPos getBlockPos4() {
      return this.getBlockPos5(null, Blocks.HOPPER);
   }

   @Override
   protected void onEnable() {
      if (mc.options != null) {
         this.valBoolean = mc.options.pauseOnLostFocus;
         mc.options.pauseOnLostFocus = false;
      }

      this.timeMillis4 = System.currentTimeMillis();
      this.intVal11 = 0;
      this.intVal12 = mc.player != null ? InventoryUtils.getItemCount(Items.APPLE) : 0;
      this.timeMillis = 0L;
      this.timeMillis2 = 0L;
      this.timeMillis3 = 0L;
      this.timeMillis5 = 0L;
      this.set2.clear();
      this.set.clear();
      this.updateState2();
   }

   private void updateState9() {
      if (this.getBlockPos3(Blocks.OAK_LEAVES) != null) {
         this.processData4(this.getBlockPos3(Blocks.OAK_LEAVES), 32.0F);
      } else if (this.matchesCriteria2(Blocks.OAK_LEAVES) && this.getBlockPos3(Blocks.OAK_LOG) != null) {
         this.processData4(this.getBlockPos3(Blocks.OAK_LOG), 16.0F);
      } else {
         this.updateState11();
         this.nested0377 = AutoAppleFarmModule.FarmState.BREAK_LOG;
      }
   }

   private int computeInt5() {
      return InventoryUtils.findSlotMatching(itemStack -> itemStack.isIn(ItemTags.AXES), 0, 8);
   }

   private void processData5(Vec3d vec3d) {
      this.processData(vec3d, 32.0F);
   }

   private boolean matchesCriteria8(ItemStack itemStack) {
      return itemStack.isOf(Items.OAK_LEAVES) || itemStack.isOf(Items.OAK_LOG) || itemStack.isOf(Items.STICK) || itemStack.isOf(Items.APPLE);
   }

   private boolean matchesCriteria9(int local1693_1) {
      if (local1693_1 < 0) {
         return false;
      }

      if (mc.player.getInventory().getSelectedSlot() != local1693_1) {
         if (System.currentTimeMillis() < this.timeMillis2) {
            return false;
         }

         this.updateState11();
         InventoryUtils.setSelectedSlot(local1693_1, false);
         this.timeMillis2 = System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(300L, 501L);
         this.timeMillis3 = System.currentTimeMillis() + 150L;
         return false;
      } else {
         return System.currentTimeMillis() >= this.timeMillis3;
      }
   }

   private Slot getSlot2(Item item) {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
         for (Slot slot : genericContainerScreenHandler.slots) {
            if (slot.id < genericContainerScreenHandler.getRows() * 9 && slot.hasStack() && slot.getStack().isOf(item)) {
               return slot;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private void updateState10() {
      RotationManager.getRotationManager().resetRotation();
      if (mc.player != null) {
         mc.player.setYaw(RotationAngles.getCameraYaw());
         mc.player.setPitch(RotationAngles.getCameraPitch());
         mc.player.headYaw = RotationAngles.getCameraYaw();
         mc.player.bodyYaw = RotationAngles.getCameraYaw();
         mc.player.lastYaw = RotationAngles.getCameraYaw();
         mc.player.lastPitch = RotationAngles.getCameraPitch();
      }
   }

   private BlockHitResult getBlockHitResult(BlockPos blockPos, Direction direction) {
      return new BlockHitResult(
         Vec3d.ofCenter(blockPos).add(direction.getOffsetX() * 0.5, direction.getOffsetY() * 0.5, direction.getOffsetZ() * 0.5),
         direction,
         blockPos,
         false
      );
   }

   private float computeFloat2(ItemStack itemStack) {
      return !itemStack.isEmpty() && itemStack.isDamageable() && itemStack.getMaxDamage() > 0
         ? 1.0F - (float)itemStack.getDamage() / itemStack.getMaxDamage()
         : 1.0F;
   }

   private BlockHitResult getBlockHitResult2() {
      BlockHitResult blockHitResult = mc.world
         .raycast(
            new RaycastContext(
               mc.player.getEyePos(),
               mc.player
                  .getEyePos()
                  .add(
                     new RotationAngles(mc.player.getYaw(), mc.player.getPitch())
                        .getDirectionVector()
                        .multiply(mc.player.getBlockInteractionRange())
                  ),
               ShapeType.OUTLINE,
               FluidHandling.NONE,
               mc.player
            )
         );
      if (blockHitResult.getType() != Type.BLOCK) {
         return null;
      } else {
         return !mc.world.getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.OAK_LEAVES)
               && !mc.world.getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.OAK_LOG)
            ? null
            : blockHitResult;
      }
   }

   private boolean checkState7() {
      if (mc.currentScreen != null) {
         mc.player.closeHandledScreen();
      }

      if (!mc.player.currentScreenHandler.getCursorStack().isEmpty() && InventoryUtils.findEmptySlot(0, 35) != -1) {
         InventoryUtils.sendSlotClickPacket(InventoryUtils.getScreenSlotId(InventoryUtils.findEmptySlot(0, 35)), 0, SlotActionType.PICKUP, false);
         return true;
      }

      if (this.computeInt2() == -1 || this.computeFloat2(mc.player.getInventory().getStack(this.computeInt2())) >= 0.9F) {
         this.updateState7();
         return false;
      }

      if (!this.matchesCriteria9(this.computeInt2())) {
         return true;
      }

      if (mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)) {
         this.processData6(new RotationAngles(mc.player.getYaw(), 90.0F));
         if (mc.player.getPitch() < 75.0F) {
            return true;
         }

         ((MinecraftClientMixin3)mc).setRightClickDelay(0);
         InventoryUtils.useItem(Hand.OFF_HAND);
         mc.player.swingHand(Hand.OFF_HAND);
         return true;
      } else if (InventoryUtils.findInventorySlot(Items.EXPERIENCE_BOTTLE) != -1) {
         if (!InventoryActionQueue.getInstance().hasPendingActions()) {
            InventoryActionQueue.getInstance().queueSlotSwap(InventoryUtils.findInventorySlot(Items.EXPERIENCE_BOTTLE), 40, 1);
         }

         return true;
      } else {
         this.intVal10 = 1;
         this.flag2 = false;
         this.timer.reset();
         return true;
      }
   }

   private boolean checkState8() {
      if (this.intVal10 != 0) {
         this.updateState11();
         return this.intVal10 == 1 ? this.checkState4() : this.checkState7();
      }

      if (this.computeInt6() != -1 && this.computeFloat2(mc.player.getInventory().getStack(this.computeInt6())) < 0.15F) {
         this.flag = false;
      } else {
         if (this.computeInt5() == -1 || !(this.computeFloat2(mc.player.getInventory().getStack(this.computeInt5())) < 0.15F)) {
            return false;
         }

         this.flag = true;
      }

      this.intVal10 = InventoryUtils.findInventorySlot(Items.EXPERIENCE_BOTTLE) == -1 && !mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)
         ? 1
         : 2;
      this.flag2 = false;
      this.timer.reset();
      return true;
   }

   private AutoAppleFarmModule() {
      this.modeSetting = this.addSetting(
         new ModeSetting(
            "Рост", "Чем растить саженец: костной мукой или зачарованием Озеленитель на мотыге.", "Костная мука", "Костная мука", "Озеленитель"
         )
      );
      this.legacyNumberSetting = this.addSetting(
         new LegacyNumberSetting("Цена стака опыта", "Дороже этого стак опыта на аукционе не покупать.", 200000.0, 10000.0, 1000000.0, 10000.0)
      );
   }

   private int computeInt6() {
      return InventoryUtils.findSlotMatching(itemStack -> itemStack.isIn(ItemTags.HOES), 0, 8);
   }

   private void processData6(RotationAngles rotationAngles) {
      this.processData3(rotationAngles, 32.0F);
   }

   private Slot getSlot3() {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
         for (Slot slot : genericContainerScreenHandler.slots) {
            if (slot.id >= genericContainerScreenHandler.getRows() * 9 && slot.hasStack()) {
               if (this.matchesCriteria8(slot.getStack())) {
                  return slot;
               }

               if (slot.getStack().isOf(Items.OAK_SAPLING) && InventoryUtils.getItemCount(Items.OAK_SAPLING) - slot.getStack().getCount() >= 64
                  )
                {
                  return slot;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private void updateState11() {
      if (mc.interactionManager != null && mc.interactionManager.isBreakingBlock()) {
         mc.interactionManager.cancelBlockBreaking();
      }

      if (mc.options != null) {
         mc.options.attackKey.setPressed(false);
      }

      this.blockPos4 = null;
   }

   private boolean checkState9() {
      return InventoryUtils.findSlotMatching(itemStack -> itemStack.isIn(ItemTags.HOES), 0, 8) != -1
         && InventoryUtils.findSlotMatching(itemStack2 -> itemStack2.isIn(ItemTags.AXES), 0, 8) != -1
         && InventoryUtils.findSlotMatching(itemStack3 -> itemStack3.isOf(Items.OAK_SAPLING), 0, 8) != -1
         && (this.modeSetting.isSelected("Озеленитель") || InventoryUtils.findSlotMatching(itemStack4 -> itemStack4.isOf(Items.BONE_MEAL), 0, 8) != -1);
   }

   private BlockPos getBlockPos5(Set<BlockPos> local1710_1, Block block) {
      BlockPos blockPos = null;
      double local1710_4 = Double.MAX_VALUE;

      for (int index = -5; index <= 5; index++) {
         for (int index2 = -5; index2 <= 5; index2++) {
            for (int index3 = -5; index3 <= 5; index3++) {
               BlockPos blockPos2 = mc.player.getBlockPos().add(index, index2, index3);
               if ((mc.world.getBlockState(blockPos2).isOf(Blocks.CHEST) || mc.world.getBlockState(blockPos2).isOf(Blocks.TRAPPED_CHEST))
                  && (local1710_1 == null || !local1710_1.contains(blockPos2))
                  && (block == null || mc.world.getBlockState(blockPos2.down()).isOf(block))
                  && this.matchesCriteria(blockPos2)
                  && mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(blockPos2)) < local1710_4) {
                  local1710_4 = mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(blockPos2));
                  blockPos = blockPos2.toImmutable();
               }
            }
         }
      }

      return blockPos;
   }

   private boolean checkState10() {
      return this.checkState2() ? true : this.checkState3();
   }

   private enum FarmState {
      PLACE_SAPLING,
      GROW,
      BREAK_LEAVES,
      BREAK_LOG;
   }
}
