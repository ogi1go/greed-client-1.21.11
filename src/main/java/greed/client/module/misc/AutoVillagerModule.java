package greed.client.module.misc;

import greed.client.setting.BooleanSetting;
import greed.mixin.BossBarHudMixin;
import greed.client.ui.ClickGuiScreen;
import greed.client.util.ClientChat;
import greed.client.util.Easings;
import greed.client.event.EventBus;
import greed.client.event.EventListener;
import greed.client.render.FontFace;
import greed.client.event.HudRenderEvent;
import greed.client.util.MathUtils;
import greed.mixin.MinecraftClientMixin3;
import greed.mixin.MinecraftClientMixin6;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.render.FontText;
import greed.client.util.EntityUtils;
import greed.client.storage.VillagerTaskQueue;
import greed.client.util.RaycastUtils;
import greed.client.render.ThemeColors;
import greed.client.util.AimPointUtils;
import greed.client.util.MerchantScreenAccess;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.chunk.ChunkSection;

@ModuleInfo(
   name = "AutoVillager",
   description = "Обходит жителей, скупает товар за изумруды, крафтит блоки и сдаёт их в сундук.",
   category = ModuleCategory.MISC
)
public final class AutoVillagerModule extends Module {
   private static AutoVillagerModule autoVillagerModule;
   private static final Pattern pattern = Pattern.compile("\\d+");
   private static final String[] strValArray = new String[]{
      "#set allowBreak false",
      "#set allowPlace false",
      "#set allowSprint true",
      "#set sprintInWater true",
      "#set allowParkour true",
      "#set allowDiagonalDescend true",
      "#set allowDownward true",
      "#set costHeuristic 3.5",
      "#set primaryTimeoutMS 100",
      "#set movementTimeoutTicks 40",
      "#set freeLook true"
   };
   private static final String[] strValArray2 = new String[]{"#set allowBreak true", "#set allowPlace true"};
   public ModeSetting modeSetting;
   private int intVal;
   public BooleanSetting booleanSetting;
   private VillagerTaskQueue villagerTaskQueue = new VillagerTaskQueue();
   private BlockPos blockPos3;
   private BlockPos blockPos4;
   private BlockPos blockPos5;
   private BlockPos blockPos6;
   private LivingEntity livingEntity2;
   private boolean flag;
   private boolean flag2;
   private boolean flag3;
   private boolean flag4;
   private int intVal2;
   private int intVal3;
   private int intVal4;
   private int intVal5;
   private int intVal6;
   private int intVal7;
   private int intVal8 = Integer.MAX_VALUE;
   private int intVal9;
   private boolean flag5;
   private boolean flag6;
   private boolean flag7;
   private boolean flag8;
   private boolean flag9;
   private BlockPos blockPos7;
   private long timeMillis;
   private long timeMillis2;
   private int intVal10;
   private int intVal11;
   private int intVal12;
   private int intVal13;
   private int intVal14;
   private int intVal15;
   private int intVal16;
   private int intVal17;
   private int intVal18;
   private int intVal19;
   private int intVal20;
   private int intVal21;
   private int intVal22;
   private int intVal23;
   private int intVal24;
   private int intVal25;
   private int intVal26;
   private int intVal27;
   private int intVal28;
   private int intVal29;
   private boolean flag10;
   private int intVal30;
   private int intVal31;
   private Vec3d vec3d3 = Vec3d.ZERO;
   private float floatVal;
   private float floatVal2;
   private float floatVal3;
   private float floatVal4;
   private float floatVal5;
   private float floatVal6;
   private float floatVal7;
   private long timeMillis3;
   private long timeMillis4;
   private long timeMillis5;
   private boolean flag11;
   private long timeMillis6;
   private long timeMillis7;
   private Set<UUID> set2 = new HashSet<>();
   private Set<UUID> set3 = new HashSet<>();
   private HashMap<UUID, Integer> map = new HashMap<>();
   private boolean flag12;
   private boolean flag13;
   private BlockPos blockPos8;
   private int intVal32;
   private int intVal33;
   private final EventListener<PlayerTickEvent> playerTickListener = event -> this.onPlayerTick(event);
   private final EventListener<HudRenderEvent> hudRenderListener = event2 -> this.onHudRender(event2);
   private final EventListener<PacketEvent> packetListener = event3 -> this.onPacket(event3);

   private void updateState() {
      mc.player
         .networkHandler
         .sendPacket(
            new LookAndOnGround(mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision)
         );
   }

   private VillagerTaskQueue.SubContext getNested0471(VillagerTaskQueue.SubContext local3209_1) {
      local3209_1.getNested04712(PlayerTickEvent.class, playerTickEvent -> {
         for (String local3331_4 : strValArray) {
            ClientChat.sendBaritoneMessage(local3331_4);
         }

         return true;
      });
      this.processData6(local3209_1);
      this.processData5(local3209_1, () -> !this.flag4);
      this.processData2(local3209_1);
      this.processData4(local3209_1, true);
      int local3209_2 = this.computeInt9() + 2;

      for (int index = 0; index < local3209_2; index++) {
         this.processData8(local3209_1, true);
         this.processData18(local3209_1, true);
         this.processData16(local3209_1);
      }

      this.processData8(local3209_1, false);
      this.processData18(local3209_1, false);
      this.processData5(local3209_1, () -> this.flag4);
      return local3209_1;
   }

   private void processData(BlockPos blockPos) {
      RotationManager.getRotationManager().resetRotation();
      this.flag10 = true;
      ClientChat.sendBaritoneMessage("#go " + blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ());
   }

   private boolean matchesCriteria(BlockPos blockPos, BlockPos blockPos2) {
      if (!this.matchesCriteria6(blockPos) && (blockPos2 == null || !(mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(blockPos2)) < 6.0))) {
         this.intVal26++;
         if (mc.player.getEntityPos().squaredDistanceTo(this.vec3d3) < 0.0025) {
            this.intVal30++;
         } else {
            this.intVal30 = 0;
         }

         this.vec3d3 = mc.player.getEntityPos();
         if (this.intVal26 % 20 == 0 && this.intVal27 < 6) {
            this.intVal27++;
            this.processData(blockPos2 != null ? blockPos2 : blockPos);
         }

         if (this.intVal26 > 12) {
            this.matchesCriteria11(blockPos2 != null ? blockPos2 : blockPos);
         }

         if (this.intVal26 > 12 && this.intVal30 > 12 && this.intVal31 >= 3) {
            this.updateState4();
            return true;
         } else if (this.intVal26 > 600) {
            this.updateState4();
            return true;
         } else {
            return false;
         }
      } else {
         this.intVal26 = 0;
         return true;
      }
   }

   private void processData2(VillagerTaskQueue.SubContext local3219_1) {
      local3219_1.getNested04712(PlayerTickEvent.class, playerTickEvent -> this.checkState11());
   }

   private void updateState2() {
      if (this.flag9) {
         mc.options.useKey.setPressed(InputUtil.isKeyPressed(mc.getWindow(), mc.options.useKey.getDefaultKey().getCode()));
         this.flag9 = false;
      }
   }

   private boolean checkState() {
      return InventoryUtils.getItemCount(Items.GOLD_BLOCK) > 0;
   }

   private boolean checkState2() {
      return this.computeInt5() > this.intVal11 || InventoryUtils.getItemCount(Items.EMERALD) < this.intVal10;
   }

   private boolean matchesCriteria2(MerchantScreen merchantScreen, int local3224_2) {
      return ((MerchantScreenAccess)merchantScreen).greed$getSelectedIndex() != local3224_2
         ? false
         : merchantScreen.getScreenHandler().getSlot(2) != null
            && merchantScreen.getScreenHandler().getSlot(2).hasStack()
            && merchantScreen.getScreenHandler().getSlot(2).getStack().isOf(this.getItem3());
   }

   private void updateState3() {
      this.intVal21 = 1;
      this.intVal22 = 0;
      this.intVal23 = 0;
      this.intVal24 = 0;
   }

   private void updateState4() {
      if (this.flag10) {
         this.flag10 = false;
         ClientChat.sendBaritoneMessage("#stop");
      }
   }

   private LivingEntity getLivingEntity(boolean local3230_1) {
      return mc.world
         .getEntitiesByClass(
            MerchantEntity.class,
            mc.player.getBoundingBox().expand(128.0),
            merchantEntity -> merchantEntity.isAlive() && (!local3230_1 || !this.set2.contains(merchantEntity.getUuid()))
         )
         .stream()
         .min(Comparator.comparingDouble(merchantEntity2 -> mc.player.squaredDistanceTo(merchantEntity2)))
         .orElse(null);
   }

   private void processData3(LivingEntity livingEntity) {
      if (livingEntity != null && livingEntity.isAlive()) {
         if (this.matchesCriteria10(livingEntity)) {
            ((MinecraftClientMixin3)mc).setRightClickDelay(0);
            this.updateState8();
            this.processData15(livingEntity);
            if (EntityUtils.isLookingAtEntity(
               livingEntity, mc.player.getEntityInteractionRange() + 1.0, mc.player.getYaw(), mc.player.getPitch()
            )) {
               EntityHitResult entityHitResult = RaycastUtils.getEntityHitResult(
                  mc.player.getEntityInteractionRange() + 1.0,
                  new RotationAngles(mc.player.getYaw(), mc.player.getPitch()),
                  entity -> entity.getId() == livingEntity.getId()
               );
               if (entityHitResult == null) {
                  entityHitResult = new EntityHitResult(livingEntity, livingEntity.getBoundingBox().getCenter());
               }

               mc.crosshairTarget = entityHitResult;
               mc.targetedEntity = livingEntity;
               mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interactAt(livingEntity, false, Hand.MAIN_HAND, entityHitResult.getPos()));
               mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(livingEntity, false, Hand.MAIN_HAND));
               mc.interactionManager.interactEntity(mc.player, livingEntity, Hand.MAIN_HAND);
               ((MinecraftClientMixin6)mc).invokeStartUseItem();
            }
         }
      }
   }

   private String formatText() {
      if (this.flag5) {
         return String.format(
            "Перезаход через: %d:%02d",
            (int)((Math.max(0L, this.timeMillis - System.currentTimeMillis()) + 999L) / 1000L) / 60,
            (int)((Math.max(0L, this.timeMillis - System.currentTimeMillis()) + 999L) / 1000L) % 60
         );
      } else {
         return this.flag6
            ? String.format(
               "Перезапуск через: %d:%02d",
               (int)((Math.max(0L, this.timeMillis2 - System.currentTimeMillis()) + 999L) / 1000L) / 60,
               (int)((Math.max(0L, this.timeMillis2 - System.currentTimeMillis()) + 999L) / 1000L) % 60
            )
            : null;
      }
   }

   private void updateState5() {
      if (mc.currentScreen instanceof ClickGuiScreen) {
         mc.setScreen(null);
      }

      this.updateState6();
   }

   private void updateState6() {
      if (checkState6()) {
         if (mc.currentScreen instanceof HandledScreen) {
            InventoryUtils.closeHandledScreen(true);
            mc.player.closeHandledScreen();
         } else if (mc.currentScreen != null) {
            mc.setScreen(null);
         }
      }
   }

   private Slot getSlot2(ScreenHandler screenHandler) {
      Slot slot = null;

      for (Slot slot2 : screenHandler.slots) {
         if (slot2.id >= 10
            && slot2.hasStack()
            && slot2.getStack().isOf(Items.GOLD_INGOT)
            && (slot == null || slot2.getStack().getCount() > slot.getStack().getCount())) {
            slot = slot2;
         }
      }

      return slot;
   }

   private void processData4(VillagerTaskQueue.SubContext local3237_1, boolean local3237_2) {
      local3237_1.getNested04712(PlayerTickEvent.class, playerTickEvent -> {
         if (this.intVal21 == 0 && !this.flag) {
            this.intVal25 = 0;
         }

         return this.matchesCriteria7(local3237_2);
      });
   }

   public static boolean checkState3() {
      return autoVillagerModule != null && (autoVillagerModule.timeMillis6 > 0L || autoVillagerModule.isEnabled() || autoVillagerModule.flag7);
   }

   private LivingEntity getLivingEntity2() {
      return mc.world
         .getEntitiesByClass(
            MerchantEntity.class,
            mc.player.getBoundingBox().expand(128.0),
            merchantEntity -> merchantEntity.isAlive()
               && !this.set2.contains(merchantEntity.getUuid())
               && this.map.getOrDefault(merchantEntity.getUuid(), -1) > 0
         )
         .stream()
         .max(Comparator.comparingInt(merchantEntity2 -> this.map.getOrDefault(merchantEntity2.getUuid(), -1)))
         .orElse(null);
   }

   private void processData5(VillagerTaskQueue.SubContext local3240_1, BooleanSupplier booleanSupplier) {
      BooleanSupplier booleanSupplier2 = () -> booleanSupplier.getAsBoolean() && this.computeInt2() > 0;
      local3240_1.getNested04712(PlayerTickEvent.class, playerTickEvent -> {
         if (!booleanSupplier2.getAsBoolean()) {
            return true;
         }

         if (!this.isEnabled()) {
            return true;
         }

         this.updateState14();
         this.updateState4();
         this.intVal9 = 0;
         return true;
      });
      local3240_1.getNested04712(PlayerTickEvent.class, playerTickEvent2 -> {
         if (!booleanSupplier2.getAsBoolean()) {
            return true;
         } else {
            return this.isEnabled() && this.flag4 ? this.computeInt5() <= 0 || ++this.intVal9 >= 200 : true;
         }
      });
      local3240_1.getNested04712(PlayerTickEvent.class, playerTickEvent3 -> {
         if (!booleanSupplier2.getAsBoolean()) {
            return true;
         }

         if (!this.isEnabled()) {
            return true;
         }

         this.updateState5();
         mc.player.networkHandler.sendChatCommand("hub");
         this.flag5 = true;
         this.timeMillis = System.currentTimeMillis() + 90000L;
         return true;
      });
      local3240_1.getNested04712(PlayerTickEvent.class, playerTickEvent4 -> {
         if (!booleanSupplier2.getAsBoolean()) {
            return true;
         } else {
            return !this.isEnabled() ? true : System.currentTimeMillis() >= this.timeMillis;
         }
      });
      local3240_1.getNested04712(PlayerTickEvent.class, playerTickEvent5 -> {
         if (!booleanSupplier2.getAsBoolean()) {
            return true;
         }

         if (!this.isEnabled()) {
            return true;
         }

         this.flag5 = false;
         this.updateState5();
         this.updateState15();
         this.flag8 = true;
         this.intVal29 = 0;
         mc.player.networkHandler.sendChatCommand("an" + this.computeInt2());
         return true;
      });
      local3240_1.getNested04712(PlayerTickEvent.class, playerTickEvent6 -> {
         if (!booleanSupplier2.getAsBoolean()) {
            return true;
         }

         if (!this.isEnabled()) {
            return true;
         }

         if (this.computeInt3() == this.computeInt2()) {
            return true;
         }

         if (++this.intVal29 % 400 == 0) {
            mc.player.networkHandler.sendChatCommand("an" + this.computeInt2());
         }

         return this.intVal29 >= 1200;
      });
      this.processData13(local3240_1, 3);
      local3240_1.getNested04712(PlayerTickEvent.class, playerTickEvent7 -> {
         if (!booleanSupplier2.getAsBoolean()) {
            return true;
         }

         this.flag5 = false;
         this.flag7 = true;
         this.flag6 = true;
         this.timeMillis2 = System.currentTimeMillis() + 5000L;
         this.updateState15();
         this.setEnabled(false);
         return true;
      });
   }

   private BlockPos getBlockPos2(Predicate<BlockState> local3241_1) {
      BlockPos blockPos = null;
      double local3241_3 = Double.MAX_VALUE;

      for (int index = mc.player.getChunkPos().x - 9; index <= mc.player.getChunkPos().x + 9; index++) {
         for (int index2 = mc.player.getChunkPos().z - 9; index2 <= mc.player.getChunkPos().z + 9; index2++) {
            if (mc.world.isChunkLoaded(index, index2)) {
               for (int index3 = 0; index3 < mc.world.getChunk(index, index2).getSectionArray().length; index3++) {
                  ChunkSection chunkSection = mc.world.getChunk(index, index2).getSectionArray()[index3];
                  if (!chunkSection.isEmpty() && chunkSection.hasAny(local3241_1)) {
                     for (int index4 = 0; index4 < 16; index4++) {
                        for (int index5 = 0; index5 < 16; index5++) {
                           for (int index6 = 0; index6 < 16; index6++) {
                              if (local3241_1.test(chunkSection.getBlockState(index4, index5, index6))
                                 && !(
                                    mc.player
                                          .squaredDistanceTo(
                                             Vec3d.ofCenter(
                                                new BlockPos(
                                                   index * 16 + index4,
                                                   mc.world.sectionIndexToCoord(index3) * 16 + index5,
                                                   index2 * 16 + index6
                                                )
                                             )
                                          )
                                       > 16384.0
                                 )
                                 && !(
                                    mc.player
                                          .squaredDistanceTo(
                                             Vec3d.ofCenter(
                                                new BlockPos(
                                                   index * 16 + index4,
                                                   mc.world.sectionIndexToCoord(index3) * 16 + index5,
                                                   index2 * 16 + index6
                                                )
                                             )
                                          )
                                       >= local3241_3
                                 )) {
                                 local3241_3 = mc.player
                                    .squaredDistanceTo(
                                       Vec3d.ofCenter(
                                          new BlockPos(
                                             index * 16 + index4,
                                             mc.world.sectionIndexToCoord(index3) * 16 + index5,
                                             index2 * 16 + index6
                                          )
                                       )
                                    );
                                 blockPos = new BlockPos(
                                    index * 16 + index4,
                                    mc.world.sectionIndexToCoord(index3) * 16 + index5,
                                    index2 * 16 + index6
                                 );
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return blockPos;
   }

   private void onPlayerTick(PlayerTickEvent event) {
      if (this.computeInt3() > 0) {
         this.intVal = this.computeInt3();
      }

      if (!this.booleanSetting.isChecked() || !this.flag8 && (!this.flag7 || System.currentTimeMillis() >= this.timeMillis2) && !this.isEnabled()) {
         if (this.flag9) {
            this.updateState2();
         }
      } else {
         this.updateState12();
      }

      if (this.flag7) {
         if (System.currentTimeMillis() >= this.timeMillis2) {
            if (mc.player != null && mc.world != null) {
               if (this.computeInt3() == this.computeInt2()) {
                  if (this.computeInt9() != 0 || ++this.intVal32 >= 200) {
                     this.updateState11();
                  }
               }
            }
         }
      }
   }

   private boolean matchesCriteria3(TradeOffer tradeOffer, Item item) {
      return !tradeOffer.getSellItem().isEmpty() && tradeOffer.getSellItem().isOf(item);
   }

   private boolean matchesCriteria4(Item item) {
      return item == Items.GOLD_INGOT || item == Items.GOLD_BLOCK || item == this.getItem3();
   }

   private boolean matchesCriteria5(ItemStack itemStack) {
      if (itemStack.get(DataComponentTypes.FOOD) == null) {
         return false;
      } else {
         return itemStack.get(DataComponentTypes.FOOD).canAlwaysEat()
            ? mc.player.getHungerManager().isNotFull()
            : mc.player.getHungerManager().getFoodLevel() <= 17;
      }
   }

   @Override
   protected void onDisable() {
      this.updateState2();
      this.villagerTaskQueue.updateState();
      this.updateState15();
      if (!this.flag7 && !this.flag6 && !this.flag8) {
         this.flag7 = false;
         this.flag8 = false;
         this.timeMillis6 = 0L;
         this.timeMillis7 = 0L;
         this.intVal9 = 0;
         this.intVal = 0;
         this.blockPos7 = null;
         this.flag5 = false;
         this.flag6 = false;
         this.timeMillis = 0L;
         this.timeMillis2 = 0L;
         this.blockPos3 = null;
         this.blockPos4 = null;
         this.blockPos5 = null;
         this.blockPos6 = null;
         this.livingEntity2 = null;
         this.flag = false;
         this.flag2 = false;
         this.flag3 = false;
         this.flag4 = true;
         this.intVal2 = 0;
         this.intVal26 = 0;
         this.intVal27 = 0;
         this.updateState9();
         this.intVal3 = 0;
         this.intVal4 = 0;
         this.intVal10 = 0;
         this.intVal11 = 0;
         this.intVal12 = 0;
         this.intVal13 = 0;
         this.intVal14 = 0;
         this.intVal15 = 0;
         this.intVal16 = 0;
         this.intVal17 = 0;
         this.intVal18 = 0;
         this.intVal19 = 0;
         this.intVal20 = 0;
         this.updateState10();
         this.flag11 = false;
         this.set2.clear();
         this.set3.clear();
         this.map.clear();
         this.flag12 = false;
      } else {
         EventBus.getInstance().subscribe(this);
      }
   }

   private void processData6(VillagerTaskQueue.SubContext local3250_1) {
      local3250_1.getNested04712(PlayerTickEvent.class, playerTickEvent -> {
         if (!this.isEnabled()) {
            return true;
         }

         this.flag4 = true;
         this.intVal18 = 0;
         this.intVal13 = 0;
         this.intVal14 = 0;
         this.intVal15 = 0;
         this.intVal16 = 0;
         this.intVal17 = 0;
         this.intVal19 = 0;
         this.updateState14();
         this.livingEntity2 = null;
         return mc.currentScreen == null || !(mc.currentScreen instanceof HandledScreen);
      });
      local3250_1.getNested04712(PlayerTickEvent.class, playerTickEvent2 -> {
         if (!this.isEnabled()) {
            return true;
         } else {
            this.livingEntity2 = this.getLivingEntity(false);
            if (this.livingEntity2 != null) {
               this.updateState9();
               this.intVal18 = 0;
               return true;
            } else {
               return ++this.intVal18 >= 5;
            }
         }
      });
      this.processData14(local3250_1);
      this.processData7(local3250_1);
      local3250_1.getNested04712(PlayerTickEvent.class, playerTickEvent3 -> {
         if (mc.currentScreen instanceof MerchantScreen merchantScreen) {
            if (merchantScreen.getScreenHandler().getRecipes() != null && !merchantScreen.getScreenHandler().getRecipes().isEmpty()) {
               this.intVal19 = 0;
               return true;
            } else {
               return ++this.intVal19 >= 10;
            }
         } else {
            return true;
         }
      });
      local3250_1.getNested04712(
         PlayerTickEvent.class,
         playerTickEvent4 -> {
            if (mc.currentScreen instanceof MerchantScreen merchantScreen2
               && merchantScreen2.getScreenHandler().getRecipes() != null
               && !merchantScreen2.getScreenHandler().getRecipes().isEmpty()) {
               this.flag4 = this.computeInt7(merchantScreen2.getScreenHandler()) >= 0;
               if (!this.flag4) {
                  ClientChat.processData8("У проверочного жителя нет нужного трейда — цикл уйдёт в хаб на перезаход.");
               }
            }

            this.updateState6();
            return true;
         }
      );
   }

   private void updateState7() {
      this.updateState4();
      mc.player.networkHandler.sendChatCommand("shop");
   }

   private Item getItem2() {
      return this.modeSetting.isSelected("Слеза Гаста") ? null : this.getItem3();
   }

   private Item getItem3() {
      return this.modeSetting.isSelected("Слеза Гаста") ? Items.GHAST_TEAR : Items.GOLD_INGOT;
   }

   private boolean checkState4() {
      int local3255_1 = 0;

      for (int index = 0; index < 36; index++) {
         if (mc.player.getInventory().getStack(index).isEmpty()) {
            if (++local3255_1 > 2) {
               return false;
            }
         }
      }

      return local3255_1 <= 2;
   }

   private void updateState8() {
      if (mc.player.isSneaking()) {
         mc.player.setSneaking(false);
      }
   }

   private void processData7(VillagerTaskQueue.SubContext local3257_1) {
      local3257_1.getNested04712(PlayerTickEvent.class, playerTickEvent -> {
         if (!this.isEnabled() || this.livingEntity2 == null) {
            return true;
         }

         if (!this.flag13) {
            return true;
         }

         if (mc.currentScreen instanceof MerchantScreen) {
            this.intVal15 = 0;
            return true;
         }

         if (this.getLivingEntity3() == null) {
            return true;
         }

         this.updateState8();
         if (this.intVal15 % 3 == 0) {
            this.processData3(this.getLivingEntity3());
         }

         return mc.currentScreen instanceof MerchantScreen || ++this.intVal15 >= 30;
      });
      local3257_1.getNested04712(PlayerTickEvent.class, playerTickEvent2 -> {
         if (!this.isEnabled() || this.livingEntity2 == null) {
            return true;
         } else if (mc.currentScreen instanceof MerchantScreen) {
            RotationManager.getRotationManager().resetRotation();
            this.intVal19 = 0;
            return true;
         } else {
            return ++this.intVal19 >= 10;
         }
      });
   }

   private Slot getSlot3(Item item) {
      if (mc.currentScreen instanceof HandledScreen<?> local3258_2) {
         for (int index = 0; index < this.computeInt(local3258_2.getScreenHandler()); index++) {
            if (local3258_2.getScreenHandler().getSlot(index) != null
               && local3258_2.getScreenHandler().getSlot(index).hasStack()
               && local3258_2.getScreenHandler().getSlot(index).getStack().isOf(item)) {
               return local3258_2.getScreenHandler().getSlot(index);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private void processData8(VillagerTaskQueue.SubContext local3260_1, boolean local3260_2) {
      local3260_1.getNested04712(PlayerTickEvent.class, playerTickEvent -> {
         if (!this.isEnabled()) {
            return true;
         }

         this.updateState14();
         this.intVal3 = 0;
         this.intVal4 = 0;
         this.intVal2 = 0;
         this.flag2 = this.computeInt10() >= 9 && (!local3260_2 || this.checkState5());
         if (!this.flag2) {
            this.blockPos3 = null;
            this.blockPos4 = null;
            return mc.currentScreen == null || !(mc.currentScreen instanceof HandledScreen);
         }

         this.blockPos3 = this.getBlockPos2(blockState -> blockState.isOf(Blocks.CRAFTING_TABLE));
         this.blockPos4 = this.blockPos3 != null ? this.getBlockPos4(this.blockPos3) : null;
         this.flag2 = this.blockPos3 != null;
         if (this.blockPos3 == null && !local3260_2) {
            ClientChat.processData8("Верстак не найден в радиусе 128 блоков — крафт блоков пропускаю.");
         }

         return mc.currentScreen == null || !(mc.currentScreen instanceof HandledScreen);
      });
      local3260_1.getNested04712(PlayerTickEvent.class, playerTickEvent2 -> {
         if (this.isEnabled() && this.flag2) {
            this.intVal26 = 0;
            this.intVal27 = 0;
            this.intVal30 = 0;
            this.intVal31 = 0;
            this.vec3d3 = mc.player.getEntityPos();
            this.processData(this.blockPos4 != null ? this.blockPos4 : this.blockPos3);
            return true;
         } else {
            return true;
         }
      });
      local3260_1.getNested04712(
         PlayerTickEvent.class,
         playerTickEvent3 -> this.isEnabled() && this.flag2 && this.blockPos3 != null ? this.matchesCriteria(this.blockPos3, this.blockPos4) : true
      );
      local3260_1.getNested04712(PlayerTickEvent.class, playerTickEvent4 -> {
         if (this.flag2) {
            this.updateState4();
         }

         return true;
      });
      local3260_1.getNested04712(PlayerTickEvent.class, playerTickEvent5 -> {
         if (!this.isEnabled() || !this.flag2 || this.blockPos3 == null) {
            return true;
         }

         if (mc.currentScreen instanceof CraftingScreen) {
            this.intVal2 = 0;
            return true;
         }

         if (!this.matchesCriteria6(this.blockPos3)) {
            if (this.intVal2 % 20 == 1) {
               this.processData(this.blockPos4 != null ? this.blockPos4 : this.blockPos3);
            }

            return ++this.intVal2 >= 30;
         } else {
            if (this.intVal2 % 5 == 0) {
               this.matchesCriteria15(this.blockPos3, this.blockPos4);
            }

            return ++this.intVal2 >= 30;
         }
      });
      local3260_1.getNested04712(PlayerTickEvent.class, playerTickEvent6 -> this.checkState8());
      local3260_1.getNested04712(PlayerTickEvent.class, playerTickEvent7 -> {
         this.updateState6();
         this.flag2 = false;
         InventoryUtils.pickupAll();
         return true;
      });
   }

   private void processData9(VillagerTaskQueue.SubContext local3261_1) {
      local3261_1.getNested04712(PlayerTickEvent.class, playerTickEvent -> {
         this.intVal13 = 0;
         this.intVal14 = 0;
         this.intVal15 = 0;
         this.intVal18 = 0;
         this.intVal19 = 0;
         this.updateState14();
         this.livingEntity2 = null;
         return mc.currentScreen == null || !(mc.currentScreen instanceof HandledScreen);
      });
      local3261_1.getNested04712(PlayerTickEvent.class, playerTickEvent2 -> {
         if (!this.isEnabled()) {
            return true;
         }

         if (this.flag4 && !this.checkState12()) {
            this.livingEntity2 = this.getLivingEntity4();
            if (this.livingEntity2 != null) {
               this.updateState9();
               this.intVal18 = 0;
               return true;
            } else {
               return !this.set3.isEmpty() ? true : ++this.intVal18 >= 5;
            }
         } else {
            this.livingEntity2 = null;
            return true;
         }
      });
      this.processData14(local3261_1);
      this.processData7(local3261_1);
      local3261_1.getNested04712(PlayerTickEvent.class, playerTickEvent3 -> {
         if (mc.currentScreen instanceof MerchantScreen merchantScreen) {
            if (merchantScreen.getScreenHandler().getRecipes() != null && !merchantScreen.getScreenHandler().getRecipes().isEmpty()) {
               this.intVal19 = 0;
               return true;
            } else {
               return ++this.intVal19 >= 10;
            }
         } else {
            return true;
         }
      });
      local3261_1.getNested04712(PlayerTickEvent.class, playerTickEvent4 -> {
         if (this.livingEntity2 != null) {
            if (mc.currentScreen instanceof MerchantScreen merchantScreen2) {
               this.map.put(this.livingEntity2.getUuid(), this.computeInt4(merchantScreen2.getScreenHandler()));
            }

            this.set3.add(this.livingEntity2.getUuid());
         }

         this.updateState6();
         return true;
      });
   }

   private boolean matchesCriteria6(BlockPos blockPos) {
      return blockPos != null && mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos)) <= mc.player.getBlockInteractionRange() - 0.1;
   }

   private int computeInt(ScreenHandler screenHandler) {
      return screenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler ? genericContainerScreenHandler.getRows() * 9 : Math.max(0, screenHandler.slots.size() - 36);
   }

   private boolean matchesCriteria7(boolean local3268_1) {
      if (!this.isEnabled() || !this.flag4 || this.checkState4() || this.checkState12()) {
         this.updateState10();
         this.flag = false;
         return true;
      }

      if (local3268_1 && this.intVal21 == 0 && this.checkState9()) {
         this.updateState10();
         this.flag = false;
         return true;
      }

      this.flag = true;
      switch (this.intVal21) {
         case 0:
            if (this.intVal25 >= 8) {
               this.updateState10();
               this.flag = false;
               return true;
            }

            this.updateState4();
            this.updateState14();
            this.updateState3();
            return false;
         case 1:
            if (mc.currentScreen instanceof HandledScreen) {
               this.intVal22 = 0;
               this.intVal21 = 2;
               return false;
            } else {
               this.intVal22++;
               if (this.intVal22 <= 3) {
                  return false;
               } else {
                  if (this.intVal22 - 3 == 1 || (this.intVal22 - 3) % 80 == 0) {
                     this.updateState7();
                  }

                  if (this.intVal22 - 3 >= 640) {
                     this.intVal25++;
                     this.intVal21 = 0;
                     this.intVal22 = 0;
                  }

                  return false;
               }
            }
         case 2:
            if (!(mc.currentScreen instanceof HandledScreen)) {
               this.intVal21 = 0;
               return false;
            } else {
               this.intVal22++;
               if (this.intVal22 <= 2) {
                  return false;
               } else if (this.getItem2() == null) {
                  this.intVal21 = 4;
                  this.intVal22 = 0;
                  return false;
               } else {
                  if (this.getSlot3(this.getItem2()) == null) {
                     if (this.intVal22 >= 62) {
                        this.intVal25++;
                        this.intVal21 = 0;
                        this.intVal22 = 0;
                     }

                     return false;
                  }

                  InventoryUtils.clickSlot(this.getSlot3(this.getItem2()), 0, SlotActionType.PICKUP, false);
                  this.intVal22 = 0;
                  this.intVal21 = 4;
                  return false;
               }
            }
         case 3:
         default:
            this.intVal21 = 0;
            return false;
         case 4:
            if (this.intVal23 >= 4) {
               this.intVal21 = 5;
               this.intVal22 = 0;
               return false;
            } else if (!(mc.currentScreen instanceof HandledScreen)) {
               this.intVal21 = 0;
               this.intVal22 = 0;
               return false;
            } else if (this.intVal22 < 10) {
               this.intVal22++;
               return false;
            } else {
               if (this.getSlot3(Items.EMERALD) == null) {
                  this.intVal24++;
                  if (this.intVal24 >= 30) {
                     this.intVal24 = 0;
                     this.intVal25++;
                     this.intVal21 = 0;
                     this.intVal22 = 0;
                  }

                  return false;
               }

               this.intVal24 = 0;
               this.intVal22 = 0;
               InventoryUtils.clickSlot(this.getSlot3(Items.EMERALD), 1, SlotActionType.QUICK_MOVE, false);
               this.intVal23++;
               return false;
            }
         case 5:
            if (mc.currentScreen != null) {
               this.updateState6();
               return false;
            } else {
               InventoryUtils.pickupAll();
               if (this.checkState9()) {
                  this.updateState10();
                  this.flag = false;
                  return true;
               } else {
                  this.intVal25++;
                  this.intVal23 = 0;
                  this.intVal21 = 0;
                  this.intVal22 = 0;
                  return false;
               }
            }
      }
   }

   private boolean checkState5() {
      int local3271_1 = 0;

      for (int index = 0; index < 36; index++) {
         if (mc.player.getInventory().getStack(index).isEmpty()) {
            if (++local3271_1 > 10) {
               return false;
            }
         }
      }

      return local3271_1 <= 10;
   }

   private int computeInt2() {
      return this.intVal;
   }

   private static boolean checkState6() {
      return mc.player != null && mc.world != null && mc.interactionManager != null;
   }

   private boolean matchesCriteria8(MerchantScreen merchantScreen) {
      return merchantScreen.getScreenHandler().getSlot(2) == null
         || !merchantScreen.getScreenHandler().getSlot(2).hasStack()
         || !merchantScreen.getScreenHandler().getSlot(2).getStack().isOf(this.getItem3());
   }

   private void updateState9() {
      this.floatVal = this.floatVal2 = 0.0F;
      this.timeMillis3 = this.timeMillis4 = 0L;
      this.timeMillis5 = 0L;
   }

   private boolean matchesCriteria9(MerchantScreen merchantScreen) {
      if (this.computeInt7(merchantScreen.getScreenHandler()) < 0) {
         return false;
      }

      if (this.matchesCriteria2(merchantScreen, this.computeInt7(merchantScreen.getScreenHandler()))) {
         return true;
      }

      ((MerchantScreenAccess)merchantScreen).greed$selectTrade(this.computeInt7(merchantScreen.getScreenHandler()));
      return false;
   }

   private int computeInt3() {
      if (mc.world == null) {
         return -1;
      }

      ScoreboardObjective objectiveForSlot = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      if (objectiveForSlot == null) {
         return -1;
      }

      Matcher matcher = pattern.matcher(objectiveForSlot.getDisplayName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""));
      int local3280_3 = -1;

      while (matcher.find()) {
         local3280_3 = Integer.parseInt(matcher.group());
      }

      return local3280_3;
   }

   private boolean matchesCriteria10(LivingEntity livingEntity) {
      return this.matchesCriteria16(mc.player.getEyePos(), livingEntity);
   }

   private AutoVillagerModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Обмен", "Что выкупать у жителей.", "Золото", "Золото", "Слеза Гаста"));
      this.booleanSetting = this.addSetting(new BooleanSetting("Использовать еду", "Есть из левой руки во время работы и ожидания перезахода.", true));
   }

   private BlockPos getBlockPos3(LivingEntity livingEntity) {
      BlockPos blockPos = null;
      double local3285_3 = Double.MAX_VALUE;
      BlockPos blockPos2 = null;
      double local3285_6 = Double.MAX_VALUE;

      for (int index = -2; index <= 2; index++) {
         for (int index2 = -2; index2 <= 2; index2++) {
            if (index != 0 || index2 != 0) {
               for (int index3 = -1; index3 <= 1; index3++) {
                  if (this.matchesCriteria14(livingEntity.getBlockPos().add(index, index3, index2))) {
                     if (this.matchesCriteria16(
                        Vec3d.ofBottomCenter(livingEntity.getBlockPos().add(index, index3, index2))
                           .add(0.0, mc.player.getStandingEyeHeight(), 0.0),
                        livingEntity
                     )) {
                        if (livingEntity.getBlockPos().add(index, index3, index2).getSquaredDistance(livingEntity.getBlockPos()) * 100.0
                              + mc.player
                                 .squaredDistanceTo(Vec3d.ofBottomCenter(livingEntity.getBlockPos().add(index, index3, index2)))
                           < local3285_3) {
                           local3285_3 = livingEntity.getBlockPos().add(index, index3, index2).getSquaredDistance(livingEntity.getBlockPos())
                                 * 100.0
                              + mc.player
                                 .squaredDistanceTo(Vec3d.ofBottomCenter(livingEntity.getBlockPos().add(index, index3, index2)));
                           blockPos = livingEntity.getBlockPos().add(index, index3, index2);
                        }
                     } else if (livingEntity.getBlockPos().add(index, index3, index2).getSquaredDistance(livingEntity.getBlockPos()) * 100.0
                           + mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(livingEntity.getBlockPos().add(index, index3, index2)))
                        < local3285_6) {
                        local3285_6 = livingEntity.getBlockPos().add(index, index3, index2).getSquaredDistance(livingEntity.getBlockPos())
                              * 100.0
                           + mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(livingEntity.getBlockPos().add(index, index3, index2)));
                        blockPos2 = livingEntity.getBlockPos().add(index, index3, index2);
                     }
                  }
               }
            }
         }
      }

      if (blockPos != null) {
         return blockPos;
      } else {
         return blockPos2 != null ? blockPos2 : livingEntity.getBlockPos();
      }
   }

   private float computeFloat() {
      return 2.0F + ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().size() * 19.0F;
   }

   private void updateState10() {
      this.intVal21 = 0;
      this.intVal22 = 0;
      this.intVal23 = 0;
      this.intVal24 = 0;
      this.intVal25 = 0;
   }

   private void onPacket(PacketEvent event) {
      if (event.isIncoming() && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
         if (this.flag8 || this.flag7) {
            if (mc.player != null && mc.world != null) {
               if (this.computeInt3() == this.computeInt2()) {
                  if (this.computeInt9() != 0 || ++this.intVal32 >= 200) {
                     this.updateState11();
                  }
               }
            }
         }
      }
   }

   private LivingEntity getLivingEntity3() {
      if (this.livingEntity2 == null) {
         return null;
      } else if (mc.world.getEntityById(this.livingEntity2.getId()) instanceof LivingEntity livingEntity && livingEntity.isAlive()) {
         this.livingEntity2 = livingEntity;
         return livingEntity;
      } else {
         this.livingEntity2 = mc.world
            .getEntitiesByClass(
               MerchantEntity.class,
               mc.player.getBoundingBox().expand(128.0),
               merchantEntity -> merchantEntity.isAlive() && merchantEntity.getUuid().equals(this.livingEntity2.getUuid())
            )
            .stream()
            .findFirst()
            .orElse(null);
         return this.livingEntity2;
      }
   }

   private void updateState11() {
      if (this.flag8 || this.flag7) {
         if (this.isEnabled()) {
            this.setEnabled(false);
         }

         this.flag7 = false;
         this.flag6 = false;
         this.timeMillis2 = 0L;
         this.flag8 = false;
         this.flag5 = false;
         this.updateState2();
         this.villagerTaskQueue.updateState();
         this.updateState15();
         EventBus.getInstance().unsubscribe(this);
         this.flag12 = true;
         this.intVal32 = 0;
         this.setEnabled(true);
      }
   }

   private static String formatText2(long local3295_0) {
      return String.format("%02d:%02d:%02d", local3295_0 / 1000L / 3600L, local3295_0 / 1000L % 3600L / 60L, local3295_0 / 1000L % 60L);
   }

   private int computeInt4(MerchantScreenHandler merchantScreenHandler) {
      if (merchantScreenHandler.getRecipes() == null) {
         return -1;
      }

      int local3296_2 = -1;

      for (TradeOffer tradeOffer : merchantScreenHandler.getRecipes()) {
         if (tradeOffer != null
            && !tradeOffer.isDisabled()
            && this.matchesCriteria3(tradeOffer, this.getItem3())
            && this.matchesCriteria17(tradeOffer)
            && tradeOffer.getSellItem().getCount() * 100 / Math.max(1, tradeOffer.getDisplayedFirstBuyItem().getCount()) > local3296_2) {
            local3296_2 = tradeOffer.getSellItem().getCount() * 100 / Math.max(1, tradeOffer.getDisplayedFirstBuyItem().getCount());
         }
      }

      return local3296_2;
   }

   private boolean matchesCriteria11(BlockPos blockPos) {
      if (blockPos != null && this.intVal30 > 12 && this.intVal31 < 3) {
         this.intVal31++;
         this.intVal30 = 0;
         this.processData(blockPos);
         return true;
      } else {
         return false;
      }
   }

   private void updateState12() {
      if (mc.player != null && mc.world != null && mc.currentScreen == null) {
         if (this.matchesCriteria18(mc.player.getOffHandStack()) && this.matchesCriteria5(mc.player.getOffHandStack())
            || mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.OFF_HAND) {
            if (mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.OFF_HAND) {
               mc.options.useKey.setPressed(true);
               this.flag9 = true;
            } else {
               InventoryUtils.useItem(Hand.OFF_HAND);
               mc.options.useKey.setPressed(true);
               this.flag9 = true;
            }
         } else {
            this.updateState2();
         }
      }
   }

   private boolean matchesCriteria12(MerchantScreen merchantScreen) {
      return this.computeInt7(merchantScreen.getScreenHandler()) >= 0 && this.matchesCriteria2(merchantScreen, this.computeInt7(merchantScreen.getScreenHandler()));
   }

   private void updateState13() {
      if (this.livingEntity2 != null) {
         this.set2.add(this.livingEntity2.getUuid());
      }

      this.updateState6();
      InventoryUtils.pickupAll();
   }

   private boolean matchesCriteria13(BlockPos blockPos) {
      return mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty();
   }

   private boolean matchesCriteria14(BlockPos blockPos) {
      return this.matchesCriteria13(blockPos)
         && this.matchesCriteria13(blockPos.up())
         && !mc.world.getBlockState(blockPos.down()).getCollisionShape(mc.world, blockPos.down()).isEmpty();
   }

   private void processData10(ScreenHandler screenHandler) {
      if (this.computeInt8() > 0) {
         for (Slot slot : screenHandler.slots) {
            if (slot.id >= 10
               && slot.hasStack()
               && slot.getStack().isOf(Items.GOLD_INGOT)
               && slot.getStack().getCount() < slot.getStack().getMaxCount()) {
               InventoryUtils.clickSlot(slot, 0, SlotActionType.PICKUP, false);
               if (this.computeInt8() <= 0) {
                  return;
               }
            }
         }

         for (Slot slot2 : screenHandler.slots) {
            if (slot2.id >= 10 && !slot2.hasStack()) {
               InventoryUtils.clickSlot(slot2, 0, SlotActionType.PICKUP, false);
               return;
            }
         }
      }
   }

   private boolean matchesCriteria15(BlockPos blockPos, BlockPos blockPos2) {
      this.updateState8();
      RotationManager.getRotationManager().resetRotation();
      Direction direction = this.getDirection(blockPos, blockPos2);
      this.processData12(
         AimPointUtils.getRotationAngles(
            Vec3d.ofCenter(blockPos)
               .add(direction.getOffsetX() * 0.5, direction.getOffsetY() * 0.5, direction.getOffsetZ() * 0.5)
               .subtract(mc.player.getEyePos())
         )
      );
      this.updateState();
      mc.crosshairTarget = new BlockHitResult(
         Vec3d.ofCenter(blockPos).add(direction.getOffsetX() * 0.5, direction.getOffsetY() * 0.5, direction.getOffsetZ() * 0.5),
         direction,
         blockPos,
         false
      );
      ActionResult actionResult = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, (BlockHitResult)mc.crosshairTarget);
      mc.player.swingHand(Hand.MAIN_HAND);
      return actionResult.isAccepted();
   }

   private int computeInt5() {
      return InventoryUtils.getItemCount(this.getItem3());
   }

   private LivingEntity getLivingEntity4() {
      return mc.world
         .getEntitiesByClass(
            MerchantEntity.class,
            mc.player.getBoundingBox().expand(128.0),
            merchantEntity -> merchantEntity.isAlive() && !this.set3.contains(merchantEntity.getUuid())
         )
         .stream()
         .min(Comparator.comparingDouble(merchantEntity2 -> mc.player.squaredDistanceTo(merchantEntity2)))
         .orElse(null);
   }

   private long computeLong() {
      return this.timeMillis6 > 0L ? System.currentTimeMillis() - this.timeMillis6 : 0L;
   }

   private void processData11(ScreenHandler screenHandler) {
      for (int index = 0;
         index < 9 && this.getSlot2(screenHandler) != null && this.getSlot2(screenHandler).getStack().getCount() >= 9;
         index++
      ) {
         InventoryUtils.clickSlot(this.getSlot2(screenHandler), 0, SlotActionType.PICKUP, false);
         InventoryUtils.sendSlotClickPacket(-999, 0, SlotActionType.QUICK_CRAFT, false);

         for (int index2 = 1; index2 <= 9; index2++) {
            InventoryUtils.sendSlotClickPacket(index2, 1, SlotActionType.QUICK_CRAFT, false);
         }

         InventoryUtils.sendSlotClickPacket(-999, 2, SlotActionType.QUICK_CRAFT, false);
         this.processData10(screenHandler);
      }

      InventoryUtils.sendSlotClickPacket(0, 0, SlotActionType.QUICK_MOVE, false);

      for (int index3 = 1; index3 <= 9; index3++) {
         if (screenHandler.getSlot(index3) != null && screenHandler.getSlot(index3).hasStack()) {
            InventoryUtils.sendSlotClickPacket(index3, 0, SlotActionType.QUICK_MOVE, false);
         }
      }
   }

   private boolean checkState7() {
      if (!this.isEnabled()) {
         this.flag3 = false;
         return true;
      }

      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
         if (++this.intVal6 > 200) {
            return true;
         }

         if (InventoryUtils.getItemCount(Items.GOLD_INGOT) + InventoryUtils.getItemCount(Items.GOLD_BLOCK) + this.computeInt5() < this.intVal8) {
            this.intVal8 = InventoryUtils.getItemCount(Items.GOLD_INGOT) + InventoryUtils.getItemCount(Items.GOLD_BLOCK) + this.computeInt5();
            this.intVal7 = 0;
         }

         if (++this.intVal7 > 20) {
            return true;
         }

         for (Slot slot : genericContainerScreenHandler.slots) {
            if (slot.id >= genericContainerScreenHandler.getRows() * 9 && slot.hasStack() && this.matchesCriteria4(slot.getStack().getItem())) {
               this.intVal5 = 0;
               if (this.intVal6 % 2 == 0) {
                  InventoryUtils.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, false);
               }

               return false;
            }
         }

         if (this.checkState13()) {
            this.intVal5 = 0;
            return false;
         } else {
            return ++this.intVal5 >= 3;
         }
      } else {
         if (this.flag3 && this.intVal6 == 0) {
            ClientChat.processData8(
               "Сундук нашёлся, но не открылся ("
                  + (this.matchesCriteria6(this.blockPos5) ? "стою рядом, клик не открывает" : "не смог подойти")
                  + ") — сдача пропущена."
            );
         }

         return true;
      }
   }

   private boolean checkState8() {
      if (!this.isEnabled()) {
         this.flag2 = false;
         return true;
      }

      if (!(mc.currentScreen instanceof CraftingScreen)) {
         if (this.flag2) {
            ClientChat.processData8(
               "Верстак нашёлся, но не открылся ("
                  + (this.matchesCriteria6(this.blockPos3) ? "стою рядом, клик не открывает" : "не смог подойти")
                  + ") — крафт пропущен."
            );
         }

         return true;
      } else {
         if (!(mc.player.currentScreenHandler instanceof CraftingScreenHandler)) {
            return true;
         }

         if (this.computeInt10() < 9) {
            return true;
         }

         if (this.intVal3 >= 256) {
            return true;
         }

         if (this.intVal4 > 0) {
            this.intVal4--;
            return false;
         }

         if (this.computeInt6(mc.player.currentScreenHandler) >= 9) {
            this.processData17(mc.player.currentScreenHandler);
         } else {
            this.processData11(mc.player.currentScreenHandler);
         }

         this.intVal3++;
         this.intVal4 = 4;
         return false;
      }
   }

   private void processData12(RotationAngles rotationAngles) {
      mc.player.setYaw(rotationAngles.getYaw());
      mc.player.setPitch(MathHelper.clamp(rotationAngles.getPitch(), -90.0F, 90.0F));
      mc.player.headYaw = rotationAngles.getYaw();
      mc.player.bodyYaw = rotationAngles.getYaw();
      mc.player.lastYaw = rotationAngles.getYaw();
      mc.player.lastPitch = MathHelper.clamp(rotationAngles.getPitch(), -90.0F, 90.0F);
   }

   private boolean checkState9() {
      return InventoryUtils.getItemCount(Items.EMERALD) >= 16;
   }

   private int computeInt6(ScreenHandler screenHandler) {
      int local3320_2 = 0;

      for (Slot slot : screenHandler.slots) {
         if (slot.id >= 10 && slot.hasStack() && slot.getStack().isOf(Items.GOLD_INGOT)) {
            local3320_2++;
         }
      }

      return local3320_2;
   }

   private int computeInt7(MerchantScreenHandler merchantScreenHandler) {
      if (merchantScreenHandler.getRecipes() == null) {
         return -1;
      }

      int local3322_2 = -1;
      int local3322_3 = -1;

      for (int index = 0; index < merchantScreenHandler.getRecipes().size(); index++) {
         TradeOffer tradeOffer = merchantScreenHandler.getRecipes().get(index);
         if (tradeOffer != null
            && !tradeOffer.isDisabled()
            && this.matchesCriteria3(tradeOffer, this.getItem3())
            && (
               tradeOffer.getSellItem().getCount() + (this.matchesCriteria17(tradeOffer) ? 10000 : 0) > local3322_3
                  || tradeOffer.getSellItem().getCount() + (this.matchesCriteria17(tradeOffer) ? 10000 : 0) == local3322_3 && index < local3322_2
            )) {
            local3322_3 = tradeOffer.getSellItem().getCount() + (this.matchesCriteria17(tradeOffer) ? 10000 : 0);
            local3322_2 = index;
         }
      }

      return local3322_2;
   }

   private void processData13(VillagerTaskQueue.SubContext local3325_1, int local3325_2) {
      if (local3325_2 > 0) {
         local3325_1.getNested04712(PlayerTickEvent.class, playerTickEvent -> {
            this.intVal33 = 0;
            return true;
         });
         local3325_1.getNested04712(PlayerTickEvent.class, playerTickEvent2 -> ++this.intVal33 >= local3325_2);
      }
   }

   private int computeInt8() {
      return mc.player.currentScreenHandler.getCursorStack().isOf(Items.GOLD_INGOT)
         ? mc.player.currentScreenHandler.getCursorStack().getCount()
         : 0;
   }

   private boolean matchesCriteria16(Vec3d vec3d, LivingEntity livingEntity) {
      for (Vec3d vec3d2 : new Vec3d[]{livingEntity.getEyePos(), livingEntity.getBoundingBox().getCenter()}) {
         if (mc.world.raycast(new RaycastContext(vec3d, vec3d2, ShapeType.COLLIDER, FluidHandling.NONE, mc.player)).getType()
            == Type.MISS) {
            return true;
         }
      }

      return false;
   }

   private void updateState14() {
      if (checkState6()) {
         if (mc.currentScreen instanceof HandledScreen) {
            InventoryUtils.closeHandledScreen(true);
            mc.player.closeHandledScreen();
         } else if (mc.currentScreen != null) {
            mc.setScreen(null);
         }
      }
   }

   private void processData14(VillagerTaskQueue.SubContext local3333_1) {
      local3333_1.getNested04712(PlayerTickEvent.class, playerTickEvent -> {
         if (!this.isEnabled() || this.livingEntity2 == null) {
            return true;
         }

         if (this.getLivingEntity3() == null) {
            return true;
         }

         this.intVal16 = 0;
         this.intVal17 = 0;
         this.intVal30 = 0;
         this.intVal31 = 0;
         this.flag13 = false;
         this.vec3d3 = mc.player.getEntityPos();
         this.blockPos8 = this.getBlockPos3(this.getLivingEntity3());
         this.processData(this.blockPos8);
         return true;
      });
      local3333_1.getNested04712(
         PlayerTickEvent.class,
         playerTickEvent2 -> {
            if (!this.isEnabled() || this.livingEntity2 == null) {
               return true;
            }

            if (this.getLivingEntity3() == null) {
               return true;
            }

            if (mc.player.squaredDistanceTo(this.getLivingEntity3()) < 4.0) {
               this.intVal16 = 0;
               this.flag13 = true;
               this.updateState4();
               return true;
            }

            if (mc.player.squaredDistanceTo(this.getLivingEntity3()) < 9.0 && this.matchesCriteria10(this.getLivingEntity3())) {
               this.intVal16 = 0;
               this.flag13 = true;
               this.updateState4();
               return true;
            }

            this.intVal16++;
            if (mc.player.getEntityPos().squaredDistanceTo(this.vec3d3) < 0.0025) {
               this.intVal30++;
            } else {
               this.intVal30 = 0;
            }

            this.vec3d3 = mc.player.getEntityPos();
            if (this.intVal16 % 20 == 0 && !this.getBlockPos3(this.getLivingEntity3()).equals(this.blockPos8)) {
               this.intVal17++;
               this.blockPos8 = this.getBlockPos3(this.getLivingEntity3());
               this.processData(this.blockPos8);
            }

            if (this.intVal16 > 12) {
               this.matchesCriteria11(this.blockPos8);
            }

            if (mc.player.distanceTo(this.getLivingEntity3()) < mc.player.getEntityInteractionRange() + 1.0
               && this.matchesCriteria10(this.getLivingEntity3())
               && this.intVal16 > 100) {
               this.flag13 = true;
               this.updateState4();
               return true;
            }

            if (this.intVal16 <= 600 && (this.intVal16 <= 12 || this.intVal30 <= 12 || this.intVal31 < 3)) {
               return false;
            }

            this.updateState4();
            return true;
         }
      );
      local3333_1.getNested04712(
         PlayerTickEvent.class,
         playerTickEvent3 -> {
            if (!this.isEnabled() || this.livingEntity2 == null) {
               return true;
            } else if (!this.flag13) {
               return true;
            } else if (mc.currentScreen instanceof MerchantScreen) {
               return true;
            } else if (this.getLivingEntity3() == null) {
               return true;
            } else {
               this.updateState8();
               this.processData15(this.getLivingEntity3());
               if (mc.currentScreen instanceof MerchantScreen) {
                  return true;
               } else if (EntityUtils.isLookingAtEntity(
                  this.getLivingEntity3(), mc.player.getEntityInteractionRange() + 1.0, mc.player.getYaw(), mc.player.getPitch()
               )) {
                  this.intVal13 = 0;
                  return true;
               } else if (mc.player.distanceTo(this.getLivingEntity3()) < 3.0F && this.matchesCriteria10(this.getLivingEntity3())) {
                  this.intVal13++;
                  return this.intVal13 >= 6;
               } else {
                  this.intVal13 = 0;
                  return ++this.intVal14 >= 30;
               }
            }
         }
      );
   }

   private void processData15(LivingEntity livingEntity) {
      if (this.timeMillis5 == 0L) {
         this.timeMillis5 = System.currentTimeMillis();
      }

      if (System.currentTimeMillis() - this.timeMillis3 > this.timeMillis4) {
         this.timeMillis3 = System.currentTimeMillis();
         this.timeMillis4 = (long)MathUtils.randomFloat(260.0F, 620.0F);
         this.floatVal3 = MathUtils.randomFloat(252.0F, 274.0F);
         this.floatVal4 = MathUtils.randomFloat(14.0F, 24.0F);
         this.floatVal5 = MathUtils.randomFloat(13.0F, 21.0F);
         this.floatVal6 = MathUtils.randomFloat(0.04F, 0.12F) * (ThreadLocalRandom.current().nextBoolean() ? 1.0F : -1.0F);
         this.floatVal7 = MathUtils.randomFloat(0.4F, 0.9F);
      }

      RotationAngles rotationAngles = AimPointUtils.getRotationAngles(
         livingEntity.getEntityPos()
            .add(0.0, MathHelper.clamp(livingEntity.getHeight() * 0.62, 0.1, Math.max(0.1, livingEntity.getHeight() - 0.1)), 0.0)
            .subtract(mc.player.getEyePos())
      );
      float local3334_3 = MathHelper.wrapDegrees(rotationAngles.getYaw() - mc.player.getYaw());
      float local3334_4 = MathHelper.clamp(rotationAngles.getPitch(), -90.0F, 90.0F) - mc.player.getPitch();
      float local3334_5 = (float)Math.hypot(local3334_3, local3334_4);
      float local3334_6 = this.floatVal3
         * (float)Easings.EASE_OUT_CIRC.apply(MathUtils.clampUnit(local3334_5 / this.floatVal4))
         * MathHelper.lerp(MathHelper.clamp((float)(System.currentTimeMillis() - this.timeMillis5) / 220.0F, 0.0F, 1.0F), 0.78F, 1.0F);
      this.floatVal = this.floatVal
         + MathHelper.clamp(
            local3334_6 * (local3334_5 < 0.001F ? 0.0F : local3334_3 / local3334_5) * 1.25F - this.floatVal, -this.floatVal5 * 1.2F, this.floatVal5 * 1.2F
         );
      this.floatVal2 = this.floatVal2
         + MathHelper.clamp(
            local3334_6 * (local3334_5 < 0.001F ? 0.0F : local3334_4 / local3334_5) + this.floatVal * this.floatVal6 - this.floatVal2,
            -this.floatVal5 * 0.68F,
            this.floatVal5 * 0.68F
         );
      this.floatVal = MathHelper.clamp(this.floatVal, -Math.abs(local3334_3), Math.abs(local3334_3));
      this.floatVal2 = MathHelper.clamp(
         this.floatVal2,
         -Math.abs(local3334_4) - Math.abs(this.floatVal * this.floatVal6),
         Math.abs(local3334_4) + Math.abs(this.floatVal * this.floatVal6)
      );
      if (local3334_5 < this.floatVal7) {
         this.floatVal = this.floatVal2 = 0.0F;
      }

      RotationManager.getRotationManager()
         .setRotationWithTimeout(
            rotationAngles,
            new RotationSpeed(
               Math.min(Math.abs(this.floatVal), EntityUtils.calculateYawSpeed(rotationAngles, 1.0F, 1.5F)),
               Math.min(Math.abs(this.floatVal2), EntityUtils.calculatePitchSpeed(rotationAngles, 2.5F, 4.0F)),
               30.0F,
               30.0F
            ),
            1000L
         );
   }

   private int computeInt9() {
      return mc.world.getEntitiesByClass(MerchantEntity.class, mc.player.getBoundingBox().expand(128.0), Entity::isAlive).size();
   }

   private boolean matchesCriteria17(TradeOffer tradeOffer) {
      return tradeOffer.getDisplayedFirstBuyItem().isOf(Items.EMERALD) ? true : tradeOffer.getDisplayedSecondBuyItem().isOf(Items.EMERALD);
   }

   private void processData16(VillagerTaskQueue.SubContext local3340_1) {
      this.processData4(local3340_1, true);
      local3340_1.getNested04712(PlayerTickEvent.class, playerTickEvent -> {
         this.intVal13 = 0;
         this.intVal14 = 0;
         this.intVal15 = 0;
         this.intVal18 = 0;
         this.intVal19 = 0;
         this.intVal20 = 0;
         this.updateState14();
         this.livingEntity2 = null;
         return mc.currentScreen == null || !(mc.currentScreen instanceof HandledScreen);
      });
      local3340_1.getNested04712(PlayerTickEvent.class, playerTickEvent2 -> {
         if (!this.isEnabled()) {
            return true;
         }

         if (this.flag4 && !this.checkState4() && !this.checkState12()) {
            this.livingEntity2 = this.getLivingEntity(true);
            if (this.livingEntity2 != null) {
               this.updateState9();
               this.intVal18 = 0;
               return true;
            } else {
               return true;
            }
         } else {
            this.livingEntity2 = null;
            return true;
         }
      });
      this.processData14(local3340_1);
      this.processData7(local3340_1);
      local3340_1.getNested04712(PlayerTickEvent.class, playerTickEvent3 -> {
         if (mc.currentScreen instanceof MerchantScreen merchantScreen) {
            if (this.computeInt7(merchantScreen.getScreenHandler()) < 0) {
               return true;
            } else if (this.matchesCriteria9(merchantScreen)) {
               this.intVal20 = 0;
               return true;
            } else {
               return ++this.intVal20 >= 6;
            }
         } else {
            return true;
         }
      });
      local3340_1.getNested04712(PlayerTickEvent.class, playerTickEvent4 -> {
         if (mc.currentScreen instanceof MerchantScreen merchantScreen2) {
            if (this.computeInt7(merchantScreen2.getScreenHandler()) < 0) {
               return true;
            } else if (this.matchesCriteria9(merchantScreen2) && this.matchesCriteria12(merchantScreen2)) {
               this.intVal10 = InventoryUtils.getItemCount(Items.EMERALD);
               this.intVal11 = this.computeInt5();
               this.intVal12 = 0;
               InventoryUtils.clickSlot(merchantScreen2.getScreenHandler().getSlot(2), 0, SlotActionType.QUICK_MOVE, false);
               return true;
            } else {
               return false;
            }
         } else {
            return true;
         }
      });
      local3340_1.getNested04712(PlayerTickEvent.class, playerTickEvent5 -> {
         if (this.checkState2()) {
            return true;
         }

         if (mc.currentScreen instanceof MerchantScreen merchantScreen3) {
            if (this.computeInt7(merchantScreen3.getScreenHandler()) < 0) {
               return true;
            }

            if (this.matchesCriteria8(merchantScreen3)) {
               return true;
            }

            if (this.intVal12 < 3) {
               this.intVal12++;
               if (this.matchesCriteria9(merchantScreen3) && this.matchesCriteria12(merchantScreen3)) {
                  InventoryUtils.clickSlot(merchantScreen3.getScreenHandler().getSlot(2), 0, SlotActionType.QUICK_MOVE, false);
                  return false;
               } else {
                  return this.intVal12 >= 3;
               }
            } else {
               return true;
            }
         } else {
            return true;
         }
      });
      local3340_1.getNested04712(PlayerTickEvent.class, playerTickEvent6 -> {
         this.updateState13();
         return true;
      });
   }

   private BlockPos getBlockPos4(BlockPos blockPos) {
      BlockPos blockPos2 = null;
      double local3342_3 = Double.MAX_VALUE;

      for (int index = -1; index <= 1; index++) {
         for (int index2 = -1; index2 <= 1; index2++) {
            for (int index3 = -1; index3 <= 1; index3++) {
               if ((index != 0 || index2 != 0)
                  && this.matchesCriteria14(blockPos.add(index, index3, index2))
                  && !(mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(blockPos.add(index, index3, index2))) >= local3342_3)) {
                  local3342_3 = mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(blockPos.add(index, index3, index2)));
                  blockPos2 = blockPos.add(index, index3, index2);
               }
            }
         }
      }

      return blockPos2;
   }

   private boolean checkState10() {
      return mc.currentScreen instanceof HandledScreen && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler;
   }

   private boolean checkState11() {
      if (this.isEnabled() && this.flag4 && this.blockPos7 != null) {
         if (mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(this.blockPos7)) < 2.5) {
            if (this.flag11) {
               this.updateState4();
            }

            this.flag11 = false;
            return true;
         } else {
            if (!this.flag11) {
               this.intVal28 = 0;
               this.intVal30 = 0;
               this.intVal31 = 0;
               this.vec3d3 = mc.player.getEntityPos();
               this.processData(this.blockPos7);
               this.flag11 = true;
               return false;
            }

            this.intVal28++;
            if (mc.player.getEntityPos().squaredDistanceTo(this.vec3d3) < 0.0025) {
               this.intVal30++;
            } else {
               this.intVal30 = 0;
            }

            this.vec3d3 = mc.player.getEntityPos();
            if (this.intVal28 > 12) {
               this.matchesCriteria11(this.blockPos7);
            }

            if ((this.intVal28 <= 12 || this.intVal30 <= 12 || this.intVal31 < 3) && this.intVal28 <= 600) {
               return false;
            }

            this.updateState4();
            this.flag11 = false;
            return true;
         }
      } else {
         this.flag11 = false;
         return true;
      }
   }

   private void updateState15() {
      if (mc.player != null) {
         this.updateState4();

         for (String local3346_4 : strValArray2) {
            ClientChat.sendBaritoneMessage(local3346_4);
         }

         mc.player.setSneaking(false);
      }
   }

   private void processData17(ScreenHandler screenHandler) {
      for (int index = 1; index <= 9 && this.getSlot2(screenHandler) != null; index++) {
         InventoryUtils.clickSlot(this.getSlot2(screenHandler), 0, SlotActionType.PICKUP, false);
         InventoryUtils.sendSlotClickPacket(index, 0, SlotActionType.PICKUP, false);
      }

      this.processData10(screenHandler);
      InventoryUtils.sendSlotClickPacket(0, 0, SlotActionType.QUICK_MOVE, false);

      for (int index2 = 1; index2 <= 9; index2++) {
         if (screenHandler.getSlot(index2) != null && screenHandler.getSlot(index2).hasStack()) {
            InventoryUtils.sendSlotClickPacket(index2, 0, SlotActionType.QUICK_MOVE, false);
         }
      }
   }

   private BlockPos getBlockPos5() {
      BlockPos blockPos = null;
      double local3349_2 = Double.MAX_VALUE;

      for (int index = mc.player.getChunkPos().x - 9; index <= mc.player.getChunkPos().x + 9; index++) {
         for (int index2 = mc.player.getChunkPos().z - 9; index2 <= mc.player.getChunkPos().z + 9; index2++) {
            if (mc.world.isChunkLoaded(index, index2)) {
               for (BlockEntity blockEntity : mc.world.getChunk(index, index2).getBlockEntities().values()) {
                  if (blockEntity instanceof ChestBlockEntity
                     && !(blockEntity instanceof TrappedChestBlockEntity)
                     && !(mc.player.squaredDistanceTo(Vec3d.ofCenter(blockEntity.getPos())) > 16384.0)
                     && !(mc.player.squaredDistanceTo(Vec3d.ofCenter(blockEntity.getPos())) >= local3349_2)) {
                     local3349_2 = mc.player.squaredDistanceTo(Vec3d.ofCenter(blockEntity.getPos()));
                     blockPos = blockEntity.getPos().toImmutable();
                  }
               }
            }
         }
      }

      return blockPos;
   }

   public static AutoVillagerModule getInstance() {
      if (autoVillagerModule == null) {
         autoVillagerModule = new AutoVillagerModule();
      }

      return autoVillagerModule;
   }

   @Override
   protected void onEnable() {
      this.blockPos3 = null;
      this.blockPos4 = null;
      this.blockPos5 = null;
      this.blockPos6 = null;
      this.livingEntity2 = null;
      this.flag = false;
      this.flag2 = false;
      this.flag3 = false;
      this.flag4 = true;
      this.intVal2 = 0;
      this.intVal26 = 0;
      this.intVal27 = 0;
      this.intVal28 = 0;
      this.flag10 = true;
      this.updateState9();
      this.intVal3 = 0;
      this.intVal4 = 0;
      this.flag5 = false;
      this.flag6 = false;
      this.flag7 = false;
      this.flag8 = false;
      this.updateState2();
      this.timeMillis = 0L;
      this.timeMillis2 = 0L;
      this.intVal10 = 0;
      this.intVal11 = 0;
      this.intVal12 = 0;
      this.intVal13 = 0;
      this.intVal14 = 0;
      this.intVal15 = 0;
      this.intVal16 = 0;
      this.intVal17 = 0;
      this.intVal18 = 0;
      this.intVal19 = 0;
      this.intVal20 = 0;
      this.updateState10();
      this.flag11 = false;
      this.set2.clear();
      if (!this.flag12) {
         this.set3.clear();
         this.map.clear();
      }

      this.flag12 = false;
      if (mc.player != null && mc.world != null) {
         if (this.blockPos7 == null) {
            this.blockPos7 = mc.player.getBlockPos().toImmutable();
         }

         if (this.timeMillis6 == 0L) {
            this.timeMillis6 = System.currentTimeMillis();
         }

         this.timeMillis7 = System.currentTimeMillis();
         this.villagerTaskQueue.processData(this.getNested0471(new VillagerTaskQueue.SubContext().getNested0471(12000)));
      } else {
         this.setEnabled(false);
      }
   }

   private int computeInt10() {
      return InventoryUtils.getItemCount(Items.GOLD_INGOT);
   }

   private void onHudRender(HudRenderEvent event) {
      if (mc.player != null && mc.world != null) {
         if (this.isEnabled() || this.flag7 || this.timeMillis6 != 0L) {
            FontText.processData9(
               FontFace.SEMI, "AutoVillager — Информация", event.computeInt2() / 2.0F, this.computeFloat(), 7.0F, ThemeColors.intVal5
            );
            FontText.processData9(
               FontFace.SEMI,
               "Текущее состояние: WORKING",
               event.computeInt2() / 2.0F,
               this.computeFloat() + 10.0F,
               7.0F,
               ThemeColors.intVal5
            );
            FontText.processData9(
               FontFace.SEMI,
               "Время работы: " + formatText2(this.computeLong()),
               event.computeInt2() / 2.0F,
               this.computeFloat() + 20.0F,
               7.0F,
               ThemeColors.intVal5
            );
            if (this.formatText() != null) {
               FontText.processData9(
                  FontFace.SEMI, this.formatText(), event.computeInt2() / 2.0F, this.computeFloat() + 31.0F, 7.0F, ThemeColors.intVal5
               );
            }
         }
      }
   }

   private boolean checkState12() {
      return this.timeMillis7 > 0L && System.currentTimeMillis() - this.timeMillis7 >= 300000L;
   }

   private boolean matchesCriteria18(ItemStack itemStack) {
      return !itemStack.isEmpty() && itemStack.contains(DataComponentTypes.FOOD);
   }

   private boolean checkState13() {
      return InventoryUtils.getItemCount(Items.GOLD_INGOT) > 0 || InventoryUtils.getItemCount(Items.GOLD_BLOCK) > 0 || this.computeInt5() > 0;
   }

   private void processData18(VillagerTaskQueue.SubContext local3363_1, boolean local3363_2) {
      local3363_1.getNested04712(PlayerTickEvent.class, playerTickEvent -> {
         if (!this.isEnabled()) {
            return true;
         }

         this.updateState14();
         this.intVal2 = 0;
         this.intVal5 = 0;
         this.intVal6 = 0;
         this.intVal7 = 0;
         this.intVal8 = Integer.MAX_VALUE;
         this.flag3 = this.checkState13() && (!local3363_2 || this.checkState5() || this.checkState());
         if (!this.flag3) {
            this.blockPos5 = null;
            this.blockPos6 = null;
            return mc.currentScreen == null || !(mc.currentScreen instanceof HandledScreen);
         }

         this.blockPos5 = this.getBlockPos5();
         this.blockPos6 = this.blockPos5 != null ? this.getBlockPos4(this.blockPos5) : null;
         this.flag3 = this.blockPos5 != null;
         if (this.blockPos5 == null && !local3363_2) {
            ClientChat.processData8("Сундук не найден в радиусе 128 блоков — сдача пропускается.");
         }

         return mc.currentScreen == null || !(mc.currentScreen instanceof HandledScreen);
      });
      local3363_1.getNested04712(PlayerTickEvent.class, playerTickEvent2 -> {
         if (this.isEnabled() && this.flag3) {
            this.intVal26 = 0;
            this.intVal27 = 0;
            this.intVal30 = 0;
            this.intVal31 = 0;
            this.vec3d3 = mc.player.getEntityPos();
            this.processData(this.blockPos6 != null ? this.blockPos6 : this.blockPos5);
            return true;
         } else {
            return true;
         }
      });
      local3363_1.getNested04712(
         PlayerTickEvent.class,
         playerTickEvent3 -> this.isEnabled() && this.flag3 && this.blockPos5 != null ? this.matchesCriteria(this.blockPos5, this.blockPos6) : true
      );
      local3363_1.getNested04712(PlayerTickEvent.class, playerTickEvent4 -> {
         if (this.flag3) {
            this.updateState4();
         }

         return true;
      });
      local3363_1.getNested04712(PlayerTickEvent.class, playerTickEvent5 -> {
         if (!this.isEnabled() || !this.flag3 || this.blockPos5 == null) {
            return true;
         }

         if (this.checkState10()) {
            this.intVal2 = 0;
            return true;
         }

         if (!this.matchesCriteria6(this.blockPos5)) {
            if (this.intVal2 % 20 == 1) {
               this.processData(this.blockPos6 != null ? this.blockPos6 : this.blockPos5);
            }

            return ++this.intVal2 >= 30;
         } else {
            if (this.intVal2 % 5 == 0) {
               this.matchesCriteria15(this.blockPos5, this.blockPos6);
            }

            return ++this.intVal2 >= 30;
         }
      });
      local3363_1.getNested04712(PlayerTickEvent.class, playerTickEvent6 -> this.checkState7());
      local3363_1.getNested04712(PlayerTickEvent.class, playerTickEvent7 -> {
         this.updateState6();
         this.flag3 = false;
         InventoryUtils.pickupAll();
         return true;
      });
   }

   private Direction getDirection(BlockPos blockPos, BlockPos blockPos2) {
      if (blockPos2 == null
         || blockPos2.getX() - blockPos.getX() == 0 && blockPos2.getY() - blockPos.getY() == 0 && blockPos2.getZ() - blockPos.getZ() == 0) {
         if (Math.abs(mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).x)
            >= Math.abs(mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).z)) {
            return mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).x >= 0.0 ? Direction.EAST : Direction.WEST;
         } else {
            return mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).z >= 0.0 ? Direction.SOUTH : Direction.NORTH;
         }
      } else if (Math.abs(blockPos2.getX() - blockPos.getX()) >= Math.abs(blockPos2.getY() - blockPos.getY())
         && Math.abs(blockPos2.getX() - blockPos.getX()) >= Math.abs(blockPos2.getZ() - blockPos.getZ())) {
         return blockPos2.getX() - blockPos.getX() >= 0 ? Direction.EAST : Direction.WEST;
      } else if (Math.abs(blockPos2.getZ() - blockPos.getZ()) >= Math.abs(blockPos2.getY() - blockPos.getY())) {
         return blockPos2.getZ() - blockPos.getZ() >= 0 ? Direction.SOUTH : Direction.NORTH;
      } else {
         return blockPos2.getY() - blockPos.getY() >= 0 ? Direction.UP : Direction.DOWN;
      }
   }
}
