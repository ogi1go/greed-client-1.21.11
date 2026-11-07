package greed.client.module.movement;

import greed.mixin.ClientCommonNetworkHandlerMixin;
import greed.mixin.ClientPlayerInteractionManagerMixin2;
import greed.client.event.ClientTickEvent;
import greed.client.event.EventListener;
import greed.mixin.LivingEntityMixin;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.MouseLookEvent;
import greed.client.event.MovementInputEvent;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.util.InventoryActionQueue;
import greed.client.util.EntityUtils;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;
import greed.client.module.movement.SimulatedPlayerInput;
import greed.client.util.Stopwatch;

import java.util.ArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(name = "Spider", description = "Позволяет лазить по стенам.", category = ModuleCategory.MOVEMENT)
public final class SpiderModule extends Module {
   private static SpiderModule spiderModule;
   public ModeSetting modeSetting;
   private final Stopwatch timer = new Stopwatch();
   private final Stopwatch timer2 = new Stopwatch();
   private Integer integer;
   private BlockHitResult blockHitResult2;
   private int intVal = -1;
   private boolean flag;
   private boolean flag2;
   private volatile KeepAliveC2SPacket keepAliveC2SPacket2;
   private volatile ClientConnection clientConnection2;
   private volatile long timeMillis;
   private final EventListener<MouseLookEvent> mouseLookListener = event -> {
      if (mc.player != null && mc.world != null && mc.interactionManager != null && this.modeSetting.isSelected("Ladders")) {
         this.onMouseLook(event);
      }
   };
   private final EventListener<ClientTickEvent> clientTickListener = clientTickEvent -> {
      if (clientTickEvent.isPre()) {
         if (this.keepAliveC2SPacket2 != null && System.currentTimeMillis() - this.timeMillis >= MathHelper.clamp(480L - 3L * this.computeInt2(), 0L, 800L)) {
            this.updateState2();
         }

         if (mc.player != null && mc.world != null && mc.interactionManager != null) {
            if (this.flag2 && (!this.modeSetting.isSelected("FunTime") || this.timer.hasExceededMillis(100L))) {
               this.processData(false);
            }

            if (this.modeSetting.isSelected("FunTime")) {
               if (this.timer.hasExceededMillis(100L)) {
                  int local4466_2 = this.computeInt();
                  if (local4466_2 != -1) {
                     if (!mc.player.getMainHandStack().isOf(Items.PLAYER_HEAD)) {
                        Box box = this.getBox(SimulatedPlayerInput.getSimulatedPlayerInput(1).box2.contract(1.0E-5));
                        if (!this.getArrayList(box).isEmpty()
                           && this.intVal == -1
                           && !InventoryActionQueue.getInstance().hasPendingActions()
                           && this.timer2.hasExceededMillis(400L)) {
                           this.intVal = local4466_2;
                           InventoryActionQueue.getInstance().queueSlotSwap(local4466_2, InventoryUtils.getSelectedSlot(), 1);
                           this.timer2.reset();
                        }
                     } else {
                        ArrayList<BlockHitResult> local4466_3 = this.getArrayList(this.getBox(mc.player.getBoundingBox().contract(1.0E-5)));
                        if (!local4466_3.isEmpty()) {
                           this.blockHitResult2 = local4466_3.getFirst();
                           float[] floatArray = EntityUtils.calculateRotationsToVec(this.blockHitResult2.getPos(), mc.player.getEyePos());
                           RotationManager.getRotationManager()
                              .setRotation(
                                 new RotationAngles(RotationAngles.getCameraYaw(), floatArray[1]),
                                 new RotationSpeed(360.0F, 360.0F, 20.0F, 20.0F),
                                 RotationManager.ReturnMode.Smooth
                              );
                           this.timer.reset();
                           this.processData(true);
                        } else {
                           if (this.intVal != -1 && !InventoryActionQueue.getInstance().hasPendingActions()) {
                              InventoryActionQueue.getInstance().queueSlotSwap(this.intVal, InventoryUtils.getSelectedSlot(), 1);
                              this.intVal = -1;
                              this.timer2.reset();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   };
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (this.blockHitResult2 != null && mc.player != null && mc.world != null && mc.interactionManager != null) {
         BlockHitResult blockHitResult = this.blockHitResult2;
         this.blockHitResult2 = null;
         this.updateState();
         ((ClientPlayerInteractionManagerMixin2)mc.interactionManager)
            .invokeStartPrediction(mc.world, local4461_1 -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHitResult, local4461_1));
         this.flag = true;
      }
   };
   private final EventListener<MovementInputEvent> movementInputListener = movementInputEvent -> {
      if (this.modeSetting.isSelected("FunTime") && !this.timer.hasExceededMillis(100L)) {
         movementInputEvent.setJumping(true);
      }
   };
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (mc.player != null && this.modeSetting.isSelected("FunTime")) {
         if (packetEvent.isOutgoing()) {
            if (packetEvent.getPacket() instanceof KeepAliveC2SPacket keepAliveC2SPacket && this.keepAliveC2SPacket2 == null && mc.getNetworkHandler() != null) {
               this.timeMillis = System.currentTimeMillis();
               this.clientConnection2 = ((ClientCommonNetworkHandlerMixin)mc.getNetworkHandler()).connection();
               this.keepAliveC2SPacket2 = keepAliveC2SPacket;
               packetEvent.cancel();
            }
         } else {
            if (packetEvent.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket screenHandlerSlotUpdateS2CPacket
               && screenHandlerSlotUpdateS2CPacket.getSlot() == mc.player.getInventory().getSelectedSlot() + 36
               && mc.player.getOffHandStack().getItem() == screenHandlerSlotUpdateS2CPacket.getStack().getItem()
               && !this.timer.hasExceededMillis(300L)) {
               packetEvent.cancel();
            }
         }
      }
   };

   public static SpiderModule getInstance() {
      if (spiderModule == null) {
         spiderModule = new SpiderModule();
      }

      return spiderModule;
   }

   @Override
   protected void onEnable() {
      this.blockHitResult2 = null;
      this.flag = false;
      if (this.modeSetting.isSelected("FunTime") && mc.player != null) {
         StatusEffectInstance statusEffect = mc.player.getStatusEffect(StatusEffects.JUMP_BOOST);
         if (statusEffect == null || statusEffect.getDuration() <= 200) {
            int local4460_2 = InventoryUtils.findOffhandSlot(
               itemStack -> {
                  EquippableComponent equippableComponent = itemStack.get(DataComponentTypes.EQUIPPABLE);
                  return equippableComponent != null
                     && equippableComponent.slot() == EquipmentSlot.FEET
                     && itemStack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).toString().contains("Попрыгун");
               }
            );
            if (local4460_2 != -1) {
               InventoryActionQueue.getInstance().processData8(local4460_2, 8, 1);
            }
         }
      }
   }

   private ArrayList<BlockHitResult> getArrayList(Box box) {
      ArrayList<BlockHitResult> local4462_2 = new ArrayList<>();

      for (BlockPos blockPos : BlockPos.iterate(box)) {
         for (Direction direction : Direction.values()) {
            if (direction.getOffsetY() == 0 && this.matchesCriteria(blockPos.offset(direction))) {
               BlockHitResult blockHitResult = FlightModule.getInstance().getBlockHitResult(blockPos.toImmutable(), direction);
               if (blockHitResult != null) {
                  local4462_2.add(blockHitResult);
               }
            }
         }
      }

      return local4462_2;
   }

   private int computeInt() {
      return mc.player.getOffHandStack().isOf(Items.PLAYER_HEAD)
         ? mc.player.playerScreenHandler.slots.getLast().id
         : InventoryUtils.findInventorySlot(Items.PLAYER_HEAD);
   }

   private void onMouseLook(MouseLookEvent event) {
      int local4464_2 = InventoryUtils.findInventorySlot(Items.LADDER);
      if (local4464_2 != -1) {
         if (!mc.player.getMainHandStack().isOf(Items.LADDER)) {
            this.integer = local4464_2;
            InventoryUtils.swapSlotWithHand(local4464_2, Hand.MAIN_HAND);
         } else if (mc.options.jumpKey.isPressed()) {
            Vec3d vec3d = new Vec3d(mc.player.getX(), mc.player.getY() + 1.0, mc.player.getZ());
            BlockHitResult blockHitResult = mc.world
               .raycast(
                  new RaycastContext(
                     vec3d,
                     vec3d.add(Vec3d.of(mc.player.getHorizontalFacing().getVector()).multiply(0.5)),
                     ShapeType.COLLIDER,
                     FluidHandling.NONE,
                     mc.player
                  )
               );
            if (blockHitResult.getType() != Type.BLOCK) {
               return;
            }

            RotationManager.getRotationManager()
               .setRotation(
                  new RotationAngles(EntityUtils.calculateRotationsToVec(blockHitResult.getPos(), mc.player.getEyePos())[0], -59.0F),
                  new RotationSpeed(60.0F, 60.0F, 10.0F, 10.0F),
                  RotationManager.ReturnMode.Empty
               );
            if (!mc.player.horizontalCollision && mc.world.getBlockState(blockHitResult.getBlockPos().offset(blockHitResult.getSide())).isAir()) {
               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);
               mc.player.swingHand(Hand.MAIN_HAND);
               this.timer.reset();
            }

            if (mc.world
               .getBlockState(
                  BlockPos.ofFloored(
                     mc.player.getX(),
                     (mc.player.getBoundingBox().minY + mc.player.getBoundingBox().maxY) / 2.0,
                     mc.player.getZ()
                  )
               )
               .isOf(Blocks.LADDER)) {
               event.processData4(true);
               mc.player.setOnGround(true);
               event.processData3(0.6);
               event.processData(true);
            }
         }
      }
   }

   private void updateState() {
      if (mc.world != null && mc.getNetworkHandler() != null) {
         ScoreboardObjective objectiveForSlot = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
         if (objectiveForSlot != null) {
            String[] stringArray = objectiveForSlot.getDisplayName().getString().split("-");
            if (stringArray.length >= 2 && !stringArray[1].replaceAll("\\D", "").isEmpty() && Integer.parseInt(stringArray[1].replaceAll("\\D", "")) > 100) {
               ((ClientCommonNetworkHandlerMixin)mc.getNetworkHandler())
                  .connection()
                  .send(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN), null);
            }
         }
      }
   }

   private Box getBox(Box box) {
      return box.withMaxY(box.minY + 1.0).withMinY(box.minY + 0.5);
   }

   private SpiderModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Режим", "Под какую проверку подгонять залаз.", "FunTime", "FunTime", "Ladders"));
   }

   private void processData(boolean local4470_1) {
      this.flag2 = local4470_1;
      mc.options.sneakKey.setPressed(local4470_1);
   }

   @Override
   protected void onDisable() {
      if (this.integer != null) {
         InventoryUtils.swapSlotWithHand(this.integer, Hand.MAIN_HAND);
         this.integer = null;
      }

      if (this.intVal != -1) {
         InventoryActionQueue.getInstance().queueSlotSwap(this.intVal, InventoryUtils.getSelectedSlot(), 1);
         this.intVal = -1;
      }

      this.blockHitResult2 = null;
      this.flag = false;
      if (this.flag2) {
         this.processData(false);
      }

      this.updateState2();
   }

   private boolean matchesCriteria(BlockPos blockPos) {
      BlockState blockState = mc.world.getBlockState(blockPos);
      return FlightModule.getInstance().matchesCriteria(blockState) && !blockState.getOutlineShape(mc.world, BlockPos.ORIGIN).isEmpty();
   }

   private void updateState2() {
      KeepAliveC2SPacket keepAliveC2SPacket = this.keepAliveC2SPacket2;
      ClientConnection clientConnection = this.clientConnection2;
      this.keepAliveC2SPacket2 = null;
      this.clientConnection2 = null;
      if (keepAliveC2SPacket != null
         && clientConnection != null
         && mc.getNetworkHandler() != null
         && ((ClientCommonNetworkHandlerMixin)mc.getNetworkHandler()).connection() == clientConnection) {
         clientConnection.send(keepAliveC2SPacket, null);
      }
   }

   public void updateState3() {
      if (this.isEnabled() && this.modeSetting.isSelected("FunTime") && mc.player != null && this.flag) {
         this.flag = false;
         this.updateState();
         mc.player
            .setVelocity(mc.player.getVelocity().x, ((LivingEntityMixin)mc.player).invokeGetJumpPower(), mc.player.getVelocity().z);
         mc.player.setOnGround(true);
      }
   }

   private int computeInt2() {
      return mc.player != null
            && mc.getNetworkHandler() != null
            && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null
         ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency()
         : 0;
   }

   public boolean checkState() {
      return this.isEnabled() && this.modeSetting.isSelected("FunTime") ? !this.timer.hasExceededMillis(100L) : false;
   }
}
