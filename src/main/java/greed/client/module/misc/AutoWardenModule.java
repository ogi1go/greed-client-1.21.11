package greed.client.module.misc;

import greed.mixin.BossBarHudMixin;
import greed.client.util.ClientChat;
import greed.client.util.Colors;
import greed.client.event.EventListener;
import greed.client.render.FontFace;
import greed.client.event.HudRenderEvent;
import greed.client.module.render.InterfaceModule;
import greed.client.setting.LegacyNumberSetting;
import greed.client.util.MathUtils;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.render.FontText;
import greed.client.ui.hud.WatermarkHud;
import greed.client.util.InventoryActionQueue;
import greed.client.module.movement.MovementPauseController;
import greed.client.render.ThemeColors;
import greed.client.util.InventorySlotManager;
import greed.client.util.AimPointUtils;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;
import greed.client.util.Stopwatch;
import greed.client.event.WorldRenderEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(
   name = "AutoWarden",
   description = "Летит на арену по клан-хому, лутает сундуки с таймером под инвизом и сдаёт лут в двойной сундук на базе.",
   category = ModuleCategory.MISC
)
public final class AutoWardenModule extends Module {
   private static AutoWardenModule autoWardenModule;
   private static final Pattern pattern = Pattern.compile("(?:(\\d{1,2}):(\\d{2})|(\\d+)\\s*(?:сек|сек\\.|с|s)|(\\d+)\\s*(?:мин|мин\\.|m))");
   private static final Pattern pattern2 = Pattern.compile("\\d+");
   private static final Pattern pattern3 = Pattern.compile(".*Телепортация через[:\\s]*(\\d+(?:[.,]\\d+)?)\\s*сек.*");
   private static final String[] strValArray = new String[]{
      "#set allowBreak false",
      "#set allowPlace false",
      "#set allowSprint true",
      "#set sprintInWater true",
      "#set allowParkour true",
      "#set allowDiagonalDescend true",
      "#set allowDownward true",
      "#set avoidance true",
      "#set allowVines false",
      "#set blocksToAvoid twisting_vines,twisting_vines_plant,weeping_vines,weeping_vines_plant,sculk_sensor,sculk_shrieker",
      "#set costHeuristic 3.5",
      "#set primaryTimeoutMS 100",
      "#set movementTimeoutTicks 40",
      "#set freeLook true",
      "#set renderPath true",
      "#set renderPathAsLine true",
      "#set renderGoal false"
   };
   private static final String[] strValArray2 = new String[]{
      "#set allowBreak true",
      "#set allowPlace true",
      "#set renderPath true",
      "#set renderGoal true",
      "#set renderPathAsLine false",
      "#set colorCurrentPath 255,0,0",
      "#set colorNextPath 255,0,255"
   };
   public LegacyNumberSetting clanHome;
   public LegacyNumberSetting openDistance;
   private final Stopwatch timer = new Stopwatch();
   private final Stopwatch timer2 = new Stopwatch();
   private final ArrayList<BlockPos> list = new ArrayList<>();
   private final ArrayList<BlockPos> list2 = new ArrayList<>();
   public final ArrayList<Integer> list3 = new ArrayList<>();
   private AutoWardenModule.TypeOption nested0388;
   private AutoWardenModule.TypeOption nested03882;
   private String strVal;
   private int intVal;
   private boolean flag;
   private boolean flag2;
   private BlockPos blockPos5;
   private BlockPos blockPos6;
   private BlockPos blockPos7;
   private final Stopwatch timer3 = new Stopwatch();
   private final ArrayList<BlockPos> list4 = new ArrayList<>();
   private final HashMap<BlockPos, Long> map = new HashMap<>();
   private boolean flag3;
   private boolean flag4;
   private boolean flag5;
   private boolean flag6;
   private boolean flag7;
   private int intVal2;
   private int intVal3;
   private int intVal4;
   private int intVal5;
   private int intVal6;
   private int intVal7;
   private int intVal8 = -1;
   private Vec3d vec3d4 = Vec3d.ZERO;
   private long timeMillis;
   private long timeMillis2;
   private long timeMillis3;
   private long timeMillis4;
   private long timeMillis5;
   private long timeMillis6;
   private long timeMillis7;
   private float floatVal;
   private float floatVal2;
   private float floatVal3;
   private int intVal9;
   private int intVal10;
   private int intVal11;
   private int intVal12;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> this.updateState();
   private final EventListener<PacketEvent> packetListener = event -> this.onPacket(event);
   private final EventListener<HudRenderEvent> hudRenderListener = event2 -> this.onHudRender(event2);

   private boolean matchesCriteria(BlockPos blockPos) {
      return mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty()
         && mc.world.getBlockState(blockPos.up()).getCollisionShape(mc.world, blockPos.up()).isEmpty()
         && !mc.world.getBlockState(blockPos.down()).getCollisionShape(mc.world, blockPos.down()).isEmpty()
         && !this.matchesCriteria8(blockPos)
         && !this.matchesCriteria8(blockPos.up())
         && !this.matchesCriteria8(blockPos.down());
   }

   public static boolean checkState() {
      return autoWardenModule != null && autoWardenModule.isEnabled();
   }

   private void updateState() {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (this.intVal9 <= 0) {
            this.intVal9 = this.computeInt5();
         }

         if (mc.player.getEntityPos().squaredDistanceTo(this.vec3d4) < 0.0025) {
            this.intVal4++;
         } else {
            this.intVal4 = 0;
         }

         this.vec3d4 = mc.player.getEntityPos();
         if (!this.flag6 && !(mc.currentScreen instanceof DeathScreen)) {
            this.updateState4();
            this.updateState3();
            this.updateState8();
            if (this.checkState8()) {
               this.updateState6();
            } else if (this.checkState15() || this.checkState13()) {
               return;
            }

            if (!this.checkState3() && this.nested0388 != AutoWardenModule.TypeOption.WARP) {
               if (this.intVal9 > 0) {
                  ClientChat.processData8(
                     "Измерение «" + mc.world.getRegistryKey().getValue().getPath() + "» вместо оверворлда — ухожу домой сдаваться."
                  );
                  this.processData3("home", this.intVal9, AutoWardenModule.TypeOption.DEPOSIT);
               }
            } else {
               switch (this.nested0388) {
                  case SUPPLY:
                     if (this.checkState4()) {
                        if (this.checkState2() || this.timer.hasExceededMillis(15000L) && mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                           this.updateState2();
                           if (this.blockPos5 != null) {
                              this.list2.add(this.blockPos5);
                           }

                           this.processData(
                              InventoryUtils.findOffhandSlot(this::matchesCriteria6) == -1
                                    && this.getBlockPos4(blockPos -> this.matchesCriteria3(blockPos, ChestType.SINGLE) && !this.list2.contains(blockPos))
                                       != null
                                 ? AutoWardenModule.TypeOption.SUPPLY
                                 : AutoWardenModule.TypeOption.BUFF
                           );
                           return;
                        }

                        return;
                     }

                     if (mc.player.age % 20 == 0
                        && this.checkState14()
                        && this.getBlockPos4(
                              blockPos2 -> this.matchesCriteria3(blockPos2, ChestType.LEFT)
                                 && !this.list2.contains(blockPos2)
                                 && mc.player.squaredDistanceTo(Vec3d.ofCenter(blockPos2)) < 1024.0
                           )
                           != null) {
                        this.processData(AutoWardenModule.TypeOption.DEPOSIT);
                        return;
                     }

                     if (!this.checkState10()) {
                        this.processData(AutoWardenModule.TypeOption.BUFF);
                        return;
                     }

                     this.processData5(ChestType.SINGLE, AutoWardenModule.TypeOption.BUFF);
                     break;
                  case BUFF:
                     if (mc.player.getHealth() < 16.0F && mc.player.getHungerManager().getFoodLevel() >= 18) {
                        return;
                     }

                     if (this.computeInt7() > 0 && this.computeInt5() > 0 && this.computeInt5() != this.computeInt7()) {
                        this.processData3("clan home " + this.clanHome.intValue(), this.computeInt7(), AutoWardenModule.TypeOption.HUNT);
                        return;
                     }

                     this.processData3("clan home " + this.clanHome.intValue(), 0, AutoWardenModule.TypeOption.HUNT);
                     break;
                  case WARP:
                     if (this.intVal > 0) {
                        if (this.computeInt5() != this.intVal) {
                           if (this.flag && !this.timer.hasExceededMillis(20000L)) {
                              return;
                           }

                           this.updateState2();
                           this.updateState7();
                           mc.player.networkHandler.sendChatCommand("an" + this.intVal);
                           this.flag = true;
                           this.timer.reset();
                           return;
                        }

                        if (this.flag && !this.timer.hasExceededMillis(4000L)) {
                           return;
                        }

                        this.intVal = 0;
                        this.flag = false;
                        this.flag2 = false;
                        this.timer.reset();
                        return;
                     }

                     if (this.strVal == null) {
                        this.processData(this.nested03882);
                        return;
                     }

                     if (!this.flag) {
                        this.updateState2();
                        this.updateState7();
                        mc.player.networkHandler.sendChatCommand(this.strVal);
                        this.flag = true;
                        this.timer.reset();
                        return;
                     }

                     if (!this.flag2) {
                        if (this.checkState12()) {
                           this.timer.reset();
                        }

                        if (this.timer.hasExceededMillis(8000L)) {
                           this.flag = false;
                        }

                        return;
                     }

                     if (this.checkState12()) {
                        return;
                     }

                     if (this.checkState5()) {
                        return;
                     }

                     if (this.nested03882 == AutoWardenModule.TypeOption.HUNT) {
                        if (this.computeInt5() > 0 && this.computeInt7() > 0 && this.computeInt5() != this.computeInt7()) {
                           ClientChat.processData8(
                              "Клан-хом утащил на анархию " + this.computeInt5() + " вместо " + this.computeInt7() + " — убираю её из списка выездных."
                           );
                           this.list3.remove(Integer.valueOf(this.computeInt7()));
                        }

                        this.list.clear();
                        this.intVal12 = 0;
                        this.map.clear();
                     }

                     this.processData(this.nested03882);
                     break;
                  case HUNT:
                     if (!this.matchesCriteria12(mc.player.getBlockPos())) {
                        if (mc.player.age % 20 == 0
                           && this.checkState14()
                           && this.getBlockPos4(
                                 blockPos3 -> this.matchesCriteria3(blockPos3, ChestType.LEFT)
                                    && !this.list2.contains(blockPos3)
                                    && mc.player.squaredDistanceTo(Vec3d.ofCenter(blockPos3)) < 1024.0
                              )
                              != null) {
                           this.updateState7();
                           this.processData(AutoWardenModule.TypeOption.DEPOSIT);
                           return;
                        }

                        if (!this.timer2.hasExceededMillis(3000L)) {
                           return;
                        }

                        this.timer2.reset();
                        this.processData4(this.getBlockPos5(mc.player.getBlockPos()));
                        return;
                     }

                     if (this.checkState4()) {
                        if (!this.matchesCriteria19(true, slot -> !this.matchesCriteria14(slot.getStack()))) {
                           return;
                        }

                        this.updateState2();
                        if (this.flag7) {
                           this.intVal12++;
                        }

                        this.flag7 = false;
                        return;
                     }

                     if (this.intVal12 < 2
                        && !this.checkState6()
                        && !(mc.player.getHealth() < 7.0F)
                        && mc.player.getHungerManager().getFoodLevel() > 6) {
                        if (mc.player.age % 5 == 0) {
                           BlockPos blockPos4 = this.getBlockPos2();
                           if (blockPos4 == null) {
                              blockPos4 = this.getBlockPos3();
                           }

                           if (blockPos4 != null) {
                              this.flag4 = this.computeInt(blockPos4) < 0;
                              this.blockPos5 = blockPos4;
                              this.blockPos6 = this.getBlockPos6(blockPos4);
                              this.processData4(this.blockPos6);
                              this.processData(AutoWardenModule.TypeOption.CHEST);
                              return;
                           }
                        }

                        this.updateState5();
                        break;
                     }

                     if (!((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().isEmpty()) {
                        this.updateState5();
                        return;
                     }

                     this.updateState7();
                     this.processData(AutoWardenModule.TypeOption.ARENA);
                     return;
                  case CHEST:
                     if (this.blockPos5 == null || !(mc.world.getBlockEntity(this.blockPos5) instanceof ChestBlockEntity)) {
                        this.updateState7();
                        this.processData(AutoWardenModule.TypeOption.HUNT);
                        return;
                     }

                     if (this.checkState4()) {
                        if (!this.matchesCriteria19(true, slot2 -> !this.matchesCriteria14(slot2.getStack())) && !this.timer.hasExceededMillis(10000L)) {
                           return;
                        }

                        this.updateState2();
                        this.list.add(this.blockPos5);
                        if (this.flag7) {
                           this.intVal12++;
                        }

                        this.processData(AutoWardenModule.TypeOption.HUNT);
                        return;
                     }

                     if (this.flag4 && this.computeInt(this.blockPos5) >= 0) {
                        this.list.add(this.blockPos5);
                        this.updateState7();
                        this.processData(AutoWardenModule.TypeOption.HUNT);
                        return;
                     }

                     if (!this.matchesCriteria5(this.blockPos5, this.blockPos6)) {
                        return;
                     }

                     if (this.computeInt(this.blockPos5) > 0 && !this.timer.hasExceededMillis(360000L)) {
                        this.updateState7();
                        return;
                     }

                     if (this.intVal3++ > 200) {
                        this.list.add(this.blockPos5);
                        this.processData(AutoWardenModule.TypeOption.HUNT);
                        return;
                     }

                     if (!this.matchesCriteria17(this.blockPos5)) {
                        if (this.intVal3 % 20 == 1) {
                           this.processData4(this.blockPos6 != null ? this.blockPos6 : this.blockPos5);
                        }

                        return;
                     }

                     this.updateState7();
                     this.processData2(this.blockPos5);
                     break;
                  case ARENA:
                     if (this.checkState5()) {
                        return;
                     }

                     if (this.computeInt3() != -1) {
                        if (mc.player.age % 2 != 0) {
                           return;
                        }

                        InventoryUtils.sendSlotClickPacket(this.computeInt3(), 0, SlotActionType.PICKUP, false);
                        this.timer.reset();
                        return;
                     }

                     if (this.flag2) {
                        if (!this.timer.hasExceededMillis(1500L)) {
                           return;
                        }

                        this.processData3("home home", this.intVal9, AutoWardenModule.TypeOption.DEPOSIT);
                        return;
                     }

                     if (!this.flag) {
                        mc.player.networkHandler.sendChatCommand("darena");
                        this.flag = true;
                        this.timer.reset();
                        return;
                     }

                     if (!this.timer.hasExceededMillis(4000L)) {
                        return;
                     }

                     this.updateState2();
                     this.flag = false;
                     break;
                  case DEPOSIT:
                     if (this.checkState4()) {
                        if (!this.matchesCriteria19(false, this::matchesCriteria10)) {
                           if (this.computeInt6((GenericContainerScreenHandler)mc.player.currentScreenHandler) != -1) {
                              return;
                           }

                           ClientChat.processData8("Двойной сундук заполнен, ищу следующий.");
                           this.list2.add(this.blockPos5);
                           this.updateState2();
                           this.processData(AutoWardenModule.TypeOption.DEPOSIT);
                           return;
                        }

                        if (!this.checkState7()) {
                           return;
                        }

                        this.updateState2();
                        if (++this.intVal11 > 2) {
                           this.intVal10++;
                        }

                        this.processData(AutoWardenModule.TypeOption.SUPPLY);
                        return;
                     }

                     this.processData5(ChestType.LEFT, AutoWardenModule.TypeOption.DEPOSIT);
               }
            }
         } else {
            this.updateState9();
         }
      }
   }

   private boolean matchesCriteria2(ItemStack itemStack, RegistryEntry<StatusEffect> local2260_2) {
      if (itemStack.isOf(Items.POTION) && itemStack.get(DataComponentTypes.POTION_CONTENTS) != null) {
         for (StatusEffectInstance statusEffectInstance : itemStack.get(DataComponentTypes.POTION_CONTENTS).getEffects()) {
            if (statusEffectInstance.getEffectType().value() == local2260_2.value()) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private double computeDouble(BlockPos blockPos) {
      double local2261_2 = Double.MAX_VALUE;

      for (BlockPos blockPos2 : this.list4) {
         local2261_2 = Math.min(local2261_2, Math.hypot(blockPos2.getX() - blockPos.getX(), blockPos2.getZ() - blockPos.getZ()));
      }

      return local2261_2;
   }

   private boolean matchesCriteria3(BlockPos blockPos, ChestType chestType) {
      if (!mc.world.getBlockState(blockPos).contains(ChestBlock.CHEST_TYPE)) {
         return false;
      } else {
         return chestType == ChestType.SINGLE
            ? mc.world.getBlockState(blockPos).get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE
            : mc.world.getBlockState(blockPos).get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE;
      }
   }

   private AutoWardenModule() {
      this.clanHome = this.addSetting(
         new LegacyNumberSetting("Дом клана", "Номер клан-хома, на который телепортироваться в начале круга.", 1.0, 1.0, 9.0, 1.0)
      );
      this.openDistance = this.addSetting(
         new LegacyNumberSetting("Открывать от", "Идти к сундуку, если на его таймере осталось не больше стольких секунд.", 45.0, 5.0, 300.0, 1.0)
      );
   }

   private boolean matchesCriteria4(ItemStack itemStack) {
      return this.matchesCriteria2(itemStack, StatusEffects.INVISIBILITY);
   }

   private boolean matchesCriteria5(BlockPos blockPos, BlockPos blockPos2) {
      if ((blockPos2 == null || !(mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(blockPos2)) < 0.6))
         && !(mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos)) <= 2.5)
         && (!this.matchesCriteria17(blockPos) || blockPos2 != null && this.intVal2 <= 80)) {
         this.intVal2++;
         if (this.intVal2 % 20 == 0) {
            this.processData4(blockPos2 != null ? blockPos2 : blockPos);
         }

         if (this.intVal2 > 300 || this.intVal2 > 12 && this.intVal4 > 20) {
            this.updateState7();
            return true;
         } else {
            return false;
         }
      } else {
         this.intVal2 = 0;
         return true;
      }
   }

   private void processData(AutoWardenModule.TypeOption local2268_1) {
      this.nested0388 = local2268_1;
      this.timer.reset();
      this.intVal2 = 0;
      this.intVal3 = 0;
      this.intVal5 = 0;
      this.intVal6 = 0;
      this.intVal8 = -1;
      this.flag7 = false;
      this.flag = false;
      this.flag2 = false;
      this.updateState6();
      if (local2268_1 != AutoWardenModule.TypeOption.CHEST) {
         this.blockPos5 = null;
         this.blockPos6 = null;
      }

      if (local2268_1 == AutoWardenModule.TypeOption.BUFF) {
         this.list2.clear();
      }
   }

   private boolean checkState2() {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
         MovementPauseController.getInstance().pauseForTicks(1);
         if (mc.player.age % 2 != 0) {
            return false;
         }

         if (!genericContainerScreenHandler.getCursorStack().isEmpty()) {
            if (this.matchesCriteria11(genericContainerScreenHandler.getCursorStack()) && InventoryUtils.findEmptySlot(0, 35) != -1) {
               InventoryUtils.sendSlotClickPacket(
                  InventoryUtils.getScreenSlotId(InventoryUtils.findEmptySlot(0, 35)), this.intVal6++ < 4 ? 1 : 0, SlotActionType.PICKUP, false
               );
               return false;
            } else {
               InventoryUtils.sendSlotClickPacket(this.intVal8 != -1 ? this.intVal8 : this.computeInt4(genericContainerScreenHandler), 0, SlotActionType.PICKUP, false);
               this.intVal8 = -1;
               return false;
            }
         } else {
            this.intVal8 = -1;
            this.intVal6 = 0;
            if (InventoryUtils.findEmptySlot(0, 35) == -1) {
               return true;
            }

            for (Slot slot : genericContainerScreenHandler.slots) {
               if (slot.id < genericContainerScreenHandler.getRows() * 9 && slot.hasStack() && this.matchesCriteria11(slot.getStack())) {
                  this.intVal5 = 0;
                  if (slot.getStack().getCount() == 1) {
                     InventoryUtils.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, false);
                     return false;
                  }

                  this.intVal8 = slot.id;
                  InventoryUtils.clickSlot(slot, 0, SlotActionType.PICKUP, false);
                  return false;
               }
            }

            return ++this.intVal5 >= 3;
         }
      } else {
         return true;
      }
   }

   private BlockPos getBlockPos2() {
      BlockPos blockPos = null;
      double local2270_2 = Double.MAX_VALUE;

      for (int index = mc.player.getChunkPos().x - 9; index <= mc.player.getChunkPos().x + 9; index++) {
         for (int index2 = mc.player.getChunkPos().z - 9; index2 <= mc.player.getChunkPos().z + 9; index2++) {
            if (mc.world.isChunkLoaded(index, index2)) {
               for (BlockEntity blockEntity : mc.world.getChunk(index, index2).getBlockEntities().values()) {
                  if (blockEntity instanceof ChestBlockEntity && !(blockEntity instanceof TrappedChestBlockEntity)) {
                     BlockPos blockPos2 = blockEntity.getPos().toImmutable();
                     if (this.matchesCriteria12(blockPos2) && this.matchesCriteria3(blockPos2, ChestType.SINGLE)) {
                        int local2270_9 = this.computeInt(blockPos2);
                        if (local2270_9 >= 0) {
                           this.map.put(blockPos2, System.currentTimeMillis() + local2270_9 * 1000L);
                        } else {
                           this.map.remove(blockPos2);
                        }

                        if (!this.list.contains(blockPos2)
                           && local2270_9 <= this.openDistance.intValue()
                           && !this.matchesCriteria9(blockPos2)
                           && (local2270_9 >= 0 || !(mc.player.squaredDistanceTo(Vec3d.ofCenter(blockPos2)) > 225.0))
                           && !(mc.player.squaredDistanceTo(Vec3d.ofCenter(blockPos2)) >= local2270_2)) {
                           local2270_2 = mc.player.squaredDistanceTo(Vec3d.ofCenter(blockPos2));
                           blockPos = blockPos2;
                        }
                     }
                  }
               }
            }
         }
      }

      return blockPos;
   }

   private boolean matchesCriteria6(ItemStack itemStack) {
      return itemStack.isOf(Items.GOLDEN_CARROT);
   }

   private int computeInt(BlockPos blockPos) {
      if (blockPos != null && mc.world != null) {
         for (Entity entity : mc.world
            .getOtherEntities(
               mc.player,
               new Box(blockPos).expand(3.0, 4.0, 3.0),
               entity2 -> entity2 instanceof ArmorStandEntity armorStandEntity && armorStandEntity.hasCustomName() || entity2 instanceof TextDisplayEntity
            )) {
            Matcher matcher = pattern.matcher(
               (entity instanceof TextDisplayEntity textDisplayEntity ? textDisplayEntity.getText() : entity.getName())
                  .getString()
                  .replaceAll("(?i)§[0-9a-fk-or]", "")
                  .trim()
            );
            if (matcher.find()) {
               if (matcher.group(1) != null) {
                  return Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2));
               }

               if (matcher.group(3) != null) {
                  return Integer.parseInt(matcher.group(3));
               }

               return Integer.parseInt(matcher.group(4)) * 60;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private boolean checkState3() {
      return mc.world != null && mc.world.getRegistryKey().getValue().getPath().equals("overworld");
   }

   private boolean checkState4() {
      return mc.currentScreen instanceof HandledScreen && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler;
   }

   @Override
   protected void onDisable() {
      this.updateState6();
      MovementPauseController.getInstance().pauseForTicks(-1);
      this.timeMillis3 = 0L;
      WardenHelperModule.getInstance().setEnabledWithoutCallbacks(false);
      if (mc.player != null) {
         this.updateState7();

         for (String local2279_4 : strValArray2) {
            ClientChat.sendBaritoneMessage(local2279_4);
         }

         RotationManager.getRotationManager().resetRotation();
         mc.player.setSneaking(false);
      }
   }

   private boolean checkState5() {
      if (mc.player.currentScreenHandler != mc.player.playerScreenHandler || InventoryUtils.findInventorySlot(Items.GLASS_BOTTLE) == -1) {
         this.intVal7 = 0;
         return false;
      }

      if (this.intVal7 >= 5) {
         return false;
      }

      if (mc.player.age % 5 != 0) {
         return true;
      }

      this.intVal7++;
      InventoryUtils.sendSlotClickPacket(InventoryUtils.getScreenSlotId(InventoryUtils.findInventorySlot(Items.GLASS_BOTTLE)), 1, SlotActionType.THROW, false);
      return true;
   }

   private int computeInt2() {
      for (int index = 8; index >= 0; index--) {
         if (!mc.player.getInventory().getStack(index).isEmpty() && !this.matchesCriteria16(mc.player.getInventory().getStack(index))) {
            return index;
         }
      }

      return -1;
   }

   private int computeInt3() {
      if (mc.currentScreen instanceof HandledScreen<?> local2282_1) {
         if (local2282_1.getScreenHandler().slots.size() > 24
            && local2282_1.getScreenHandler().getSlot(24).hasStack()
            && local2282_1.getScreenHandler().getSlot(24).getStack().isOf(Items.PUFFERFISH)) {
            return 24;
         }

         for (Slot slot : local2282_1.getScreenHandler().slots) {
            if (slot.hasStack() && slot.getStack().getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").contains("На Смотровую")) {
               return slot.id;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private long computeLong() {
      return mc.player.getStatusEffect(StatusEffects.INVISIBILITY) == null
         ? 0L
         : mc.player.getStatusEffect(StatusEffects.INVISIBILITY).getDuration() * 50L;
   }

   private int computeInt4(GenericContainerScreenHandler genericContainerScreenHandler) {
      return this.computeInt6(genericContainerScreenHandler) != -1 ? this.computeInt6(genericContainerScreenHandler) : InventoryUtils.getScreenSlotId(InventoryUtils.findEmptySlot(0, 35));
   }

   private boolean matchesCriteria7(double local2286_1) {
      for (PlayerEntity playerEntity : mc.world.getPlayers()) {
         if (playerEntity != mc.player && mc.player.squaredDistanceTo(playerEntity) <= local2286_1 * local2286_1) {
            return true;
         }
      }

      return false;
   }

   private void updateState2() {
      if (mc.currentScreen != null) {
         mc.player.closeHandledScreen();
      }

      InventoryUtils.pickupAll();
      MovementPauseController.getInstance().pauseForTicks(-1);
   }

   private boolean matchesCriteria8(BlockPos blockPos) {
      return mc.world.getBlockState(blockPos).isOf(Blocks.TWISTING_VINES)
         || mc.world.getBlockState(blockPos).isOf(Blocks.TWISTING_VINES_PLANT)
         || mc.world.getBlockState(blockPos).isOf(Blocks.WEEPING_VINES)
         || mc.world.getBlockState(blockPos).isOf(Blocks.WEEPING_VINES_PLANT)
         || mc.world.getBlockState(blockPos).isOf(Blocks.SCULK_SENSOR)
         || mc.world.getBlockState(blockPos).isOf(Blocks.SCULK_SHRIEKER);
   }

   private boolean matchesCriteria9(BlockPos blockPos) {
      if (!mc.world.getEntitiesByClass(WardenEntity.class, new Box(blockPos).expand(6.0), wardenEntity -> wardenEntity.isAlive()).isEmpty()) {
         return true;
      }

      for (PlayerEntity playerEntity : mc.world.getPlayers()) {
         if (playerEntity != mc.player
            && playerEntity.getEntityPos().squaredDistanceTo(Vec3d.ofCenter(blockPos)) <= 25.0
            && this.matchesCriteria21(playerEntity)) {
            return true;
         }
      }

      return false;
   }

   private void processData2(BlockPos blockPos) {
      if (mc.player.isSneaking()) {
         mc.player.setSneaking(false);
      }

      Vec3d vec3d = Vec3d.ofCenter(blockPos);
      RotationAngles rotationAngles = AimPointUtils.getRotationAngles(vec3d.subtract(mc.player.getEyePos()));
      float local2290_4 = (float)(
         (
               Math.sin(mc.player.age * 0.31F) * 0.5
                  + Math.sin(mc.player.age * 0.73F + 1.1F) * 0.3
                  + Math.sin(mc.player.age * 1.7F + 2.6F) * 0.2
            )
            * 8.0
      );
      RotationManager.getRotationManager()
         .setRotationWithTimeout(
            new RotationAngles(rotationAngles.getYaw() + local2290_4, MathHelper.clamp(rotationAngles.getPitch() + local2290_4 / 4.0F, -90.0F, 90.0F)),
            new RotationSpeed(40.0F, 40.0F, 30.0F, 30.0F),
            300L
         );
      if (mc.player.age % 2 == 0
         && (
            this.intVal3 > 60
               || !(Math.abs(MathHelper.wrapDegrees(rotationAngles.getYaw() + local2290_4 - mc.player.getYaw())) > 5.0F)
                  && !(Math.abs(rotationAngles.getPitch() + local2290_4 / 4.0F - mc.player.getPitch()) > 5.0F)
         )) {
         BlockHitResult blockHitResult = mc.world
            .raycast(new RaycastContext(mc.player.getEyePos(), vec3d, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
         if (!this.matchesCriteria13(blockHitResult.getBlockPos(), blockPos)) {
            blockHitResult = new BlockHitResult(vec3d, Direction.UP, blockPos, false);
         }

         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);
         mc.player.swingHand(Hand.MAIN_HAND);
         this.timer.reset();
      }
   }

   private boolean matchesCriteria10(Slot slot) {
      if (this.matchesCriteria4(slot.getStack())) {
         return slot.getIndex() != InventoryUtils.findOffhandSlot(this::matchesCriteria4);
      } else if (this.matchesCriteria20(slot.getStack())) {
         return slot.getIndex() != InventoryUtils.findOffhandSlot(this::matchesCriteria20);
      } else {
         return this.matchesCriteria6(slot.getStack()) ? slot.getIndex() != InventoryUtils.findOffhandSlot(this::matchesCriteria6) : true;
      }
   }

   private void processData3(String local2292_1, int local2292_2, AutoWardenModule.TypeOption local2292_3) {
      this.strVal = local2292_1;
      this.intVal = local2292_2;
      this.nested03882 = local2292_3;
      this.processData(AutoWardenModule.TypeOption.WARP);
   }

   @Override
   protected void onEnable() {
      this.list.clear();
      this.list2.clear();
      this.list4.clear();
      this.map.clear();
      this.blockPos5 = null;
      this.blockPos6 = null;
      this.intVal = 0;
      this.strVal = null;
      this.flag3 = true;
      this.flag4 = false;
      this.flag5 = false;
      this.flag6 = false;
      this.vec3d4 = Vec3d.ZERO;
      this.timeMillis = 0L;
      this.timeMillis6 = 0L;
      this.timeMillis7 = 0L;
      this.timeMillis2 = 0L;
      this.intVal9 = 0;
      this.intVal10 = 0;
      this.intVal11 = 0;
      this.intVal12 = 0;
      this.intVal4 = 0;
      this.timeMillis3 = System.currentTimeMillis();
      this.timer2.reset();
      WardenHelperModule.getInstance().setEnabledWithoutCallbacks(true);
      this.processData3("home", 0, AutoWardenModule.TypeOption.DEPOSIT);
      if (mc.player != null && mc.world != null) {
         for (String local2293_4 : strValArray) {
            ClientChat.sendBaritoneMessage(local2293_4);
         }

         ClientChat.sendBaritoneMessage(
            "#set colorCurrentPath "
               + (ThemeColors.intVal9 >> 16 & 0xFF)
               + ","
               + (ThemeColors.intVal9 >> 8 & 0xFF)
               + ","
               + (ThemeColors.intVal9 & 0xFF)
         );
         ClientChat.sendBaritoneMessage(
            "#set colorNextPath "
               + (ThemeColors.intVal9 >> 16 & 0xFF)
               + ","
               + (ThemeColors.intVal9 >> 8 & 0xFF)
               + ","
               + (ThemeColors.intVal9 & 0xFF)
         );
      } else {
         this.setEnabled(false);
      }
   }

   private Vec3d getVec3d(BlockPos blockPos) {
      Vec3d vec3d = Vec3d.ofCenter(blockPos);
      Vec3d vec3d2 = null;
      double local2295_4 = Double.MAX_VALUE;

      for (double local2295_6 = -0.4; local2295_6 <= 0.41; local2295_6 += 0.4) {
         for (double local2295_8 = -0.4; local2295_8 <= 0.41; local2295_8 += 0.4) {
            for (double local2295_10 = -0.4; local2295_10 <= 0.41; local2295_10 += 0.4) {
               Vec3d vec3d3 = vec3d.add(local2295_6, local2295_8, local2295_10);
               if (!(vec3d3.squaredDistanceTo(vec3d) >= local2295_4)
                  && this.matchesCriteria13(
                     mc.world
                        .raycast(new RaycastContext(mc.player.getEyePos(), vec3d3, ShapeType.COLLIDER, FluidHandling.NONE, mc.player))
                        .getBlockPos(),
                     blockPos
                  )) {
                  local2295_4 = vec3d3.squaredDistanceTo(vec3d);
                  vec3d2 = vec3d3;
               }
            }
         }
      }

      return vec3d2;
   }

   private void updateState3() {
      if (!(mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock() instanceof SlabBlock)
         && !(mc.world.getBlockState(mc.player.getBlockPos().down()).getBlock() instanceof StairsBlock)) {
         if (mc.player.isSneaking()) {
            mc.player.setSneaking(false);
         }
      } else {
         mc.player.setSneaking(true);
      }
   }

   private void processData4(BlockPos blockPos) {
      if (blockPos != null) {
         if (!this.flag3 || !blockPos.equals(this.blockPos7) || this.timer3.hasExceededMillis(3000L)) {
            RotationManager.getRotationManager().resetRotation();
            this.flag3 = true;
            this.blockPos7 = blockPos;
            this.timer3.reset();
            ClientChat.sendBaritoneMessage("#go " + blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ());
         }
      }
   }

   private BlockPos getBlockPos3() {
      BlockPos blockPos = null;
      long local2298_2 = System.currentTimeMillis() + this.openDistance.intValue() * 1000L;

      for (BlockPos blockPos2 : this.map.keySet()) {
         if (mc.world.getBlockEntity(blockPos2) instanceof ChestBlockEntity
            && !this.list.contains(blockPos2)
            && this.map.get(blockPos2) < local2298_2) {
            local2298_2 = this.map.get(blockPos2);
            blockPos = blockPos2;
         }
      }

      return blockPos;
   }

   private void onPacket(PacketEvent event) {
      if (event.isIncoming()) {
         if (event.getPacket() instanceof DeathMessageS2CPacket) {
            this.flag6 = true;
         }

         if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.flag2 = true;
         }
      }
   }

   private void updateState4() {
      for (PlayerEntity playerEntity : mc.world.getPlayers()) {
         if (playerEntity != mc.player
            && !(mc.player.squaredDistanceTo(playerEntity) > 900.0)
            && !(
               Math.abs(
                     MathHelper.wrapDegrees(
                        playerEntity.getHeadYaw()
                           - (float)Math.toDegrees(Math.atan2(playerEntity.getX() - mc.player.getX(), mc.player.getZ() - playerEntity.getZ()))
                     )
                  )
                  > 25.0F
            )
            && !(
               (mc.player.getX() - playerEntity.getX()) * (playerEntity.getX() - playerEntity.lastX)
                     + (mc.player.getZ() - playerEntity.getZ()) * (playerEntity.getZ() - playerEntity.lastZ)
                  <= 0.01
            )) {
            this.timeMillis7 = System.currentTimeMillis();
            if (this.timeMillis6 == 0L) {
               this.timeMillis6 = System.currentTimeMillis();
            }

            return;
         }
      }

      if (System.currentTimeMillis() - this.timeMillis7 > 1500L) {
         this.timeMillis6 = 0L;
      }
   }

   private boolean matchesCriteria11(ItemStack itemStack) {
      return this.matchesCriteria4(itemStack) && InventoryUtils.findOffhandSlot(this::matchesCriteria4) == -1
         || this.matchesCriteria20(itemStack) && InventoryUtils.findOffhandSlot(this::matchesCriteria20) == -1
         || this.matchesCriteria6(itemStack) && InventoryUtils.findOffhandSlot(this::matchesCriteria6) == -1;
   }

   public static AutoWardenModule getInstance() {
      if (autoWardenModule == null) {
         autoWardenModule = new AutoWardenModule();
      }

      return autoWardenModule;
   }

   private boolean checkState6() {
      int local2303_1 = 0;

      for (int index = 0; index < 36; index++) {
         if (!mc.player.getInventory().getStack(index).isEmpty()) {
            if (++local2303_1 >= 24) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean matchesCriteria12(BlockPos blockPos) {
      return this.checkState3() && blockPos.getX() >= -2060 && blockPos.getX() <= -1928 && blockPos.getZ() >= -2069 && blockPos.getZ() <= -1936;
   }

   private long computeLong2() {
      return mc.player.getStatusEffect(StatusEffects.SPEED) == null ? 0L : mc.player.getStatusEffect(StatusEffects.SPEED).getDuration() * 50L;
   }

   private int computeInt5() {
      if (mc.world == null) {
         return -1;
      }

      ScoreboardObjective objectiveForSlot = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      if (objectiveForSlot == null) {
         return -1;
      }

      Matcher matcher = pattern2.matcher(objectiveForSlot.getDisplayName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""));
      int local2306_3 = -1;

      while (matcher.find()) {
         local2306_3 = Integer.parseInt(matcher.group());
      }

      return local2306_3;
   }

   private BlockPos getBlockPos4(Predicate<BlockPos> local2308_1) {
      BlockPos blockPos = null;
      double local2308_3 = Double.MAX_VALUE;

      for (int index = mc.player.getChunkPos().x - 9; index <= mc.player.getChunkPos().x + 9; index++) {
         for (int index2 = mc.player.getChunkPos().z - 9; index2 <= mc.player.getChunkPos().z + 9; index2++) {
            if (mc.world.isChunkLoaded(index, index2)) {
               for (BlockEntity blockEntity : mc.world.getChunk(index, index2).getBlockEntities().values()) {
                  if (blockEntity instanceof ChestBlockEntity
                     && !(blockEntity instanceof TrappedChestBlockEntity)
                     && !(mc.player.squaredDistanceTo(Vec3d.ofCenter(blockEntity.getPos())) > 16384.0)
                     && !(mc.player.squaredDistanceTo(Vec3d.ofCenter(blockEntity.getPos())) >= local2308_3)
                     && local2308_1.test(blockEntity.getPos())) {
                     local2308_3 = mc.player.squaredDistanceTo(Vec3d.ofCenter(blockEntity.getPos()));
                     blockPos = blockEntity.getPos().toImmutable();
                  }
               }
            }
         }
      }

      return blockPos;
   }

   private boolean matchesCriteria13(BlockPos blockPos, BlockPos blockPos2) {
      return blockPos.equals(blockPos2)
         || blockPos.getSquaredDistance(blockPos2) <= 1.0
            && mc.world.getBlockEntity(blockPos) instanceof ChestBlockEntity
            && !this.matchesCriteria3(blockPos2, ChestType.SINGLE);
   }

   private void onWorldRender(WorldRenderEvent event) {
      if (mc.player != null && mc.world != null && this.flag3 && this.blockPos7 != null) {
         Vec3d vec3d = mc.player.getEntityPos().add(0.0, 0.9, 0.0);
         Vec3d vec3d2 = Vec3d.ofCenter(this.blockPos7);
         Vec3d vec3d3 = vec3d2.subtract(vec3d).normalize();
         event.getMatrixStack().push();
         event.getMatrixStack()
            .translate(
               vec3d.x - mc.gameRenderer.getCamera().getCameraPos().x,
               vec3d.y - mc.gameRenderer.getCamera().getCameraPos().y,
               vec3d.z - mc.gameRenderer.getCamera().getCameraPos().z
            );
         VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines());
         vertexConsumer.vertex(event.getMatrixStack().peek(), 0.0F, 0.0F, 0.0F)
            .color(Colors.computeInt6(ThemeColors.intVal9, 0.35F))
            .normal(event.getMatrixStack().peek(), (float)vec3d3.x, (float)vec3d3.y, (float)vec3d3.z)
            .lineWidth(2.5F);
         vertexConsumer.vertex(
               event.getMatrixStack().peek(),
               (float)(vec3d2.x - vec3d.x),
               (float)(vec3d2.y - vec3d.y),
               (float)(vec3d2.z - vec3d.z)
            )
            .color(ThemeColors.intVal9)
            .normal(event.getMatrixStack().peek(), (float)vec3d3.x, (float)vec3d3.y, (float)vec3d3.z)
            .lineWidth(2.5F);
         mc.getBufferBuilders().getEntityVertexConsumers().draw();
         event.getMatrixStack().pop();
      }
   }

   private boolean matchesCriteria14(ItemStack itemStack) {
      return itemStack.isOf(Items.PIGLIN_HEAD)
         || itemStack.isOf(Items.ZOMBIE_HEAD)
         || itemStack.isOf(Items.DISC_FRAGMENT_5)
         || itemStack.isOf(Items.ENDER_CHEST)
         || itemStack.isOf(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE)
         || itemStack.isOf(Items.PUFFERFISH)
         || itemStack.isOf(Items.ECHO_SHARD)
         || itemStack.isOf(Items.SCULK)
         || itemStack.isOf(Items.CREEPER_HEAD)
         || itemStack.isOf(Items.SKULL_BANNER_PATTERN)
         || itemStack.isOf(Items.NAUTILUS_SHELL)
         || itemStack.isOf(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE)
         || itemStack.isOf(Items.COOKED_MUTTON)
         || itemStack.isOf(Items.SCULK_SENSOR)
         || itemStack.isOf(Items.GHAST_TEAR)
         || itemStack.isOf(Items.BLAZE_SPAWN_EGG)
         || itemStack.isOf(Items.ENCHANTING_TABLE)
         || itemStack.contains(DataComponentTypes.JUKEBOX_PLAYABLE);
   }

   private boolean checkState7() {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
         if (mc.player.age % 2 != 0) {
            return false;
         }

         if (!genericContainerScreenHandler.getCursorStack().isEmpty()) {
            if (!this.matchesCriteria6(genericContainerScreenHandler.getCursorStack())) {
               return true;
            } else if (this.intVal8 != -1 && !genericContainerScreenHandler.slots.get(this.intVal8).hasStack()) {
               InventoryUtils.sendSlotClickPacket(this.intVal8, 1, SlotActionType.PICKUP, false);
               return false;
            } else if (this.computeInt6(genericContainerScreenHandler) == -1) {
               InventoryUtils.sendSlotClickPacket(this.intVal8, 0, SlotActionType.PICKUP, false);
               this.intVal8 = -1;
               ClientChat.processData8("Двойной сундук полон — излишек морковки остаётся с собой.");
               return true;
            } else {
               InventoryUtils.sendSlotClickPacket(this.computeInt6(genericContainerScreenHandler), 0, SlotActionType.PICKUP, false);
               return false;
            }
         } else {
            int local2313_2 = 0;

            for (Slot slot : genericContainerScreenHandler.slots) {
               if (slot.id >= genericContainerScreenHandler.getRows() * 9 && slot.hasStack() && this.matchesCriteria6(slot.getStack())) {
                  local2313_2 += slot.getStack().getCount();
               }
            }

            if (local2313_2 <= 1) {
               return true;
            }

            for (Slot slot2 : genericContainerScreenHandler.slots) {
               if (slot2.id >= genericContainerScreenHandler.getRows() * 9 && slot2.hasStack() && this.matchesCriteria6(slot2.getStack())) {
                  this.intVal8 = slot2.id;
                  InventoryUtils.clickSlot(slot2, 0, SlotActionType.PICKUP, false);
                  return false;
               }
            }

            return true;
         }
      } else {
         return true;
      }
   }

   private boolean checkState8() {
      return this.timeMillis6 != 0L && System.currentTimeMillis() - this.timeMillis6 > 2500L;
   }

   private void updateState5() {
      if (this.timer2.hasExceededMillis(this.intVal4 >= 8 ? 1500L : 7000L)) {
         this.intVal4 = 0;
         this.timer2.reset();
         BlockPos blockPos = null;
         double local2315_2 = -1.0;

         for (int index = 0; index < 24; index++) {
            BlockPos blockPos2 = new BlockPos(
               ThreadLocalRandom.current().nextInt(-2051, -1937), mc.player.getBlockPos().getY(), ThreadLocalRandom.current().nextInt(-2060, -1945)
            );
            if (!(Math.min(this.computeDouble(blockPos2), this.computeDouble2(blockPos2)) <= local2315_2)) {
               local2315_2 = Math.min(this.computeDouble(blockPos2), this.computeDouble2(blockPos2));
               blockPos = blockPos2;
               if (local2315_2 >= 50.0) {
                  break;
               }
            }
         }

         this.list4.add(blockPos);
         if (this.list4.size() > 5) {
            this.list4.remove(0);
         }

         this.processData4(blockPos);
      }
   }

   private boolean checkState9() {
      if (mc.currentScreen instanceof HandledScreen) {
         return false;
      }

      if (this.checkState8()) {
         this.timeMillis = System.currentTimeMillis() + 6000L;
      }

      if (System.currentTimeMillis() > this.timeMillis) {
         return false;
      }

      if (!this.timer2.hasExceededMillis(this.checkState8() ? 1000L : 2500L)) {
         return this.nested0388 == AutoWardenModule.TypeOption.HUNT || this.nested0388 == AutoWardenModule.TypeOption.CHEST;
      }

      this.timer2.reset();
      this.updateState6();
      BlockPos blockPos = null;
      double local2316_2 = -1.0;

      for (int index = 0; index < 360; index += 30) {
         BlockPos blockPos2 = mc.player
            .getBlockPos()
            .add(
               (int)(Math.cos(Math.toRadians(index)) * (this.checkState8() ? 32.0 : 25.0)),
               0,
               (int)(Math.sin(Math.toRadians(index)) * (this.checkState8() ? 32.0 : 25.0))
            );
         if (this.nested0388 == AutoWardenModule.TypeOption.HUNT || this.nested0388 == AutoWardenModule.TypeOption.CHEST) {
            blockPos2 = this.getBlockPos5(blockPos2);
         }

         if (!(this.computeDouble2(blockPos2) <= local2316_2)) {
            local2316_2 = this.computeDouble2(blockPos2);
            blockPos = blockPos2;
         }
      }

      this.processData4(blockPos);
      return this.nested0388 == AutoWardenModule.TypeOption.HUNT || this.nested0388 == AutoWardenModule.TypeOption.CHEST;
   }

   private boolean checkState10() {
      return InventoryUtils.findOffhandSlot(this::matchesCriteria4) == -1 || InventoryUtils.findOffhandSlot(this::matchesCriteria6) == -1;
   }

   private boolean checkState11() {
      for (PlayerEntity playerEntity : mc.world.getPlayers()) {
         if (playerEntity != mc.player
            && !(mc.player.squaredDistanceTo(playerEntity) > 625.0)
            && (
               mc.player.squaredDistanceTo(playerEntity) < 100.0
                  || this.matchesCriteria21(playerEntity)
                  || Math.abs(
                        MathHelper.wrapDegrees(
                           playerEntity.getHeadYaw()
                              - (float)Math.toDegrees(Math.atan2(playerEntity.getX() - mc.player.getX(), mc.player.getZ() - playerEntity.getZ()))
                        )
                     )
                     < 20.0F
            )) {
            return true;
         }
      }

      return false;
   }

   private boolean matchesCriteria15(Predicate<ItemStack> local2319_1, float local2319_2) {
      this.flag5 = true;
      if (mc.player.getVelocity().horizontalLengthSquared() > 0.001) {
         this.updateState7();
         return true;
      }

      RotationManager.getRotationManager()
         .setRotationWithTimeout(new RotationAngles(mc.player.getYaw(), local2319_2), new RotationSpeed(35.0F, 35.0F, 30.0F, 30.0F), 500L);
      if (Math.abs(mc.player.getPitch() - local2319_2) > 15.0F) {
         return true;
      }

      if (InventoryUtils.findSlotMatching(local2319_1, 0, 8) == -1) {
         if (!InventoryActionQueue.getInstance().hasPendingActions()) {
            InventoryActionQueue.getInstance().queueSlotSwap(InventoryUtils.findOffhandSlot(local2319_1), 8, 1);
         }

         return true;
      } else {
         if (InventoryUtils.getSelectedSlot() != InventoryUtils.findSlotMatching(local2319_1, 0, 8)) {
            InventoryUtils.setSelectedSlot(InventoryUtils.findSlotMatching(local2319_1, 0, 8), false);
            return true;
         }

         if (mc.currentScreen == null) {
            mc.options.useKey.setPressed(true);
            return true;
         }

         if (!mc.player.isUsingItem()) {
            InventoryUtils.useItem(Hand.MAIN_HAND);
         }

         return true;
      }
   }

   private void processData5(ChestType chestType, AutoWardenModule.TypeOption local2320_2) {
      if (this.blockPos5 == null) {
         if (mc.player.age % 5 == 0) {
            this.blockPos5 = this.getBlockPos4(blockPos -> this.matchesCriteria3(blockPos, chestType) && !this.list2.contains(blockPos));
            if (this.blockPos5 == null) {
               if (this.timer.hasExceededMillis(20000L)) {
                  if (!this.list2.isEmpty()) {
                     this.list2.clear();
                     this.timer.reset();
                  } else {
                     if (chestType == ChestType.SINGLE) {
                        ClientChat.processData8("Рядом нет сундука с ресурсами.");
                        ClientChat.processData8(
                           "Поставь маленький сундук рядом с собой и заполни его: &7Зелье невидимости&r, &7золотые морковки&r, по желанию &7Зелье скорости&r."
                        );
                        ClientChat.processData8(
                           "Ещё на Warden оставь &7/clan home&r с любым названием, а дома — большой сундук, куда бот будет складывать ресурсы."
                        );
                     } else if (local2320_2 == this.nested0388) {
                        ClientChat.processData8("Не вижу двойного сундука для сдачи — он должен стоять там, куда ведёт /home home. Жду.");
                        this.timer.reset();
                        return;
                     }

                     this.processData(local2320_2);
                  }
               }
            } else {
               this.blockPos6 = this.getBlockPos6(this.blockPos5);
               if (this.nested0388 == AutoWardenModule.TypeOption.DEPOSIT) {
                  ClientChat.processData8("Сдаю лут: двойной сундук " + this.blockPos5.toShortString() + ".");
               }

               this.processData4(this.blockPos6 != null ? this.blockPos6 : this.blockPos5);
            }
         }
      } else if (this.matchesCriteria5(this.blockPos5, this.blockPos6)) {
         if (this.intVal3++ > 200) {
            ClientChat.processData8(
               "Не смог открыть сундук "
                  + this.blockPos5.toShortString()
                  + " за 10с"
                  + (this.matchesCriteria17(this.blockPos5) ? " (стою рядом, но клик не проходит)" : " (не могу подойти)")
                  + "."
            );
            this.list2.add(this.blockPos5);
            this.processData(local2320_2);
         } else if (!this.matchesCriteria17(this.blockPos5)) {
            if (this.intVal3 % 20 == 1) {
               this.processData4(this.blockPos6 != null ? this.blockPos6 : this.blockPos5);
            }
         } else {
            this.updateState7();
            this.processData2(this.blockPos5);
         }
      }
   }

   private void updateState6() {
      if (mc.options != null) {
         mc.options.useKey.setPressed(false);
      }
   }

   private void updateState7() {
      if (this.flag3) {
         this.flag3 = false;
         this.blockPos7 = null;
         ClientChat.sendBaritoneMessage("#stop");
      }
   }

   private int computeInt6(GenericContainerScreenHandler genericContainerScreenHandler) {
      for (Slot slot : genericContainerScreenHandler.slots) {
         if (slot.id < genericContainerScreenHandler.getRows() * 9 && !slot.hasStack()) {
            return slot.id;
         }
      }

      return -1;
   }

   private double computeDouble2(BlockPos blockPos) {
      double local2324_2 = Double.MAX_VALUE;

      for (Entity entity : mc.world
         .getOtherEntities(
            mc.player, new Box(blockPos).expand(64.0), entity2 -> entity2 instanceof PlayerEntity || entity2 instanceof WardenEntity
         )) {
         local2324_2 = Math.min(local2324_2, Math.hypot(entity.getX() - blockPos.getX(), entity.getZ() - blockPos.getZ()));
      }

      return local2324_2;
   }

   private boolean checkState12() {
      for (ClientBossBar clientBossBar : ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().values()) {
         if (pattern3.matcher(clientBossBar.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim()).matches()) {
            return true;
         }
      }

      return false;
   }

   private boolean matchesCriteria16(ItemStack itemStack) {
      return this.matchesCriteria6(itemStack) || this.matchesCriteria4(itemStack) || this.matchesCriteria20(itemStack);
   }

   private boolean matchesCriteria17(BlockPos blockPos) {
      return blockPos != null && mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos)) <= mc.player.getBlockInteractionRange() - 1.0;
   }

   private void onHudRender(HudRenderEvent event) {
      if (mc.player != null && mc.world != null && this.isEnabled()) {
         float local2328_2 = InterfaceModule.getInstance().matchesCriteria("watermark") && WatermarkHud.computeFloat2() > 0.0F
            ? WatermarkHud.computeFloat2() * event.computeInt() + 6.0F
            : 2.0F + ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().size() * 19.0F;
         long local2328_3 = this.timeMillis3 > 0L ? System.currentTimeMillis() - this.timeMillis3 : 0L;
         FontText.processData9(
            FontFace.SEMI, "AutoWarden — Информация", event.computeInt2() / 2.0F, local2328_2, 7.0F, ThemeColors.intVal5
         );
         FontText.processData9(
            FontFace.SEMI,
            System.currentTimeMillis() <= this.timeMillis ? "Стадия: УБЕГАЮ ОТ ИГРОКА" : "Стадия: " + this.nested0388.name(),
            event.computeInt2() / 2.0F,
            local2328_2 + 10.0F,
            7.0F,
            ThemeColors.intVal5
         );
         FontText.processData9(
            FontFace.SEMI,
            "Время работы: " + String.format("%02d:%02d:%02d", local2328_3 / 3600000L, local2328_3 / 60000L % 60L, local2328_3 / 1000L % 60L),
            event.computeInt2() / 2.0F,
            local2328_2 + 20.0F,
            7.0F,
            ThemeColors.intVal5
         );
         if (this.nested0388 == AutoWardenModule.TypeOption.CHEST && this.blockPos5 != null && this.computeInt(this.blockPos5) >= 0) {
            FontText.processData9(
               FontFace.SEMI,
               "Жду сундук: " + this.computeInt(this.blockPos5) + "с",
               event.computeInt2() / 2.0F,
               local2328_2 + 31.0F,
               7.0F,
               ThemeColors.intVal5
            );
         }

         if (this.nested0388 == AutoWardenModule.TypeOption.HUNT) {
            FontText.processData9(
               FontFace.SEMI,
               "Слутано сундуков: " + this.intVal12,
               event.computeInt2() / 2.0F,
               local2328_2 + 31.0F,
               7.0F,
               ThemeColors.intVal5
            );
         }
      }
   }

   private boolean matchesCriteria18(ItemStack itemStack) {
      return !itemStack.isOf(Items.PHANTOM_MEMBRANE) && !itemStack.isOf(Items.ELYTRA)
         ? itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("снотворное")
            || itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("ассасина")
            || itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("святая")
            || itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("радиации")
            || itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("драконья")
            || itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("неизбежная")
            || itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("обледенения")
            || itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("световая")
            || itemStack.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase().contains("палладина")
         : true;
   }

   private boolean matchesCriteria19(boolean local2330_1, Predicate<Slot> local2330_2) {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
         MovementPauseController.getInstance().pauseForTicks(1);

         for (Slot slot : genericContainerScreenHandler.slots) {
            if (slot.id >= genericContainerScreenHandler.getRows() * 9 != local2330_1 && slot.hasStack() && local2330_2.test(slot)) {
               this.intVal5 = 0;
               this.flag7 = true;
               if (mc.player.age % 2 == 0) {
                  InventoryUtils.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, false);
               }

               return false;
            }
         }

         return ++this.intVal5 >= 3;
      } else {
         return true;
      }
   }

   private BlockPos getBlockPos5(BlockPos blockPos) {
      return new BlockPos(MathHelper.clamp(blockPos.getX(), -2051, -1937), blockPos.getY(), MathHelper.clamp(blockPos.getZ(), -2060, -1945));
   }

   private int computeInt7() {
      return !this.list3.isEmpty() && this.intVal11 >= 2 ? this.list3.get(this.intVal10 % this.list3.size()) : this.intVal9;
   }

   private void updateState8() {
      if (this.nested0388 == AutoWardenModule.TypeOption.HUNT || this.nested0388 == AutoWardenModule.TypeOption.CHEST) {
         if (!(mc.currentScreen instanceof HandledScreen) && !(mc.player.getVelocity().horizontalLengthSquared() < 0.004)) {
            if (System.currentTimeMillis() - this.timeMillis4 > this.timeMillis5) {
               this.timeMillis4 = System.currentTimeMillis();
               this.timeMillis5 = (long)MathUtils.randomFloat(420.0F, 950.0F);
               this.floatVal3 = MathUtils.randomFloat(9.0F, 17.0F);
               this.floatVal = MathUtils.randomFloat(-14.0F, 14.0F);
               this.floatVal2 = MathUtils.randomFloat(-8.0F, 16.0F);
            }

            float local2333_1 = (float)Math.toDegrees(Math.atan2(-mc.player.getVelocity().x, mc.player.getVelocity().z)) + this.floatVal;
            float local2333_2 = (float)((Math.sin(mc.player.age * 0.11F) * 0.6 + Math.sin(mc.player.age * 0.27F + 1.4F) * 0.4) * 3.0);
            RotationManager.getRotationManager()
               .setRotationWithMode(
                  new RotationAngles(local2333_1 + local2333_2, this.floatVal2 + local2333_2 / 3.0F),
                  new RotationSpeed(this.floatVal3, this.floatVal3 * 0.6F, 30.0F, 30.0F),
                  0L,
                  RotationManager.ReturnMode.Empty
               );
         }
      }
   }

   private boolean checkState13() {
      if (!(mc.currentScreen instanceof HandledScreen)
         && !this.flag5
         && !mc.player.isUsingItem()
         && !InventoryActionQueue.getInstance().hasPendingActions()
         && !InventorySlotManager.getInstance().isLocked()
         && mc.player.age % 5 == 0) {
         if ((this.nested0388 == AutoWardenModule.TypeOption.HUNT || this.nested0388 == AutoWardenModule.TypeOption.CHEST)
            && InventoryUtils.findHotbarSlot(Items.GLASS_BOTTLE) != -1) {
            if (InventoryUtils.getSelectedSlot() != InventoryUtils.findHotbarSlot(Items.GLASS_BOTTLE)) {
               InventoryUtils.setSelectedSlot(InventoryUtils.findHotbarSlot(Items.GLASS_BOTTLE), false);
               return false;
            } else {
               mc.player.dropSelectedItem(true);
               return false;
            }
         } else {
            if ((this.nested0388 == AutoWardenModule.TypeOption.HUNT || this.nested0388 == AutoWardenModule.TypeOption.CHEST)
               && InventoryUtils.findInventorySlot(Items.GLASS_BOTTLE) != -1
               && InventoryUtils.findEmptySlot(0, 8) != -1) {
               InventoryActionQueue.getInstance().queueSlotSwap(InventoryUtils.findInventorySlot(Items.GLASS_BOTTLE), InventoryUtils.findEmptySlot(0, 8), 1);
               return false;
            }

            if (mc.player.getMainHandStack().isEmpty()) {
               return false;
            }

            if (InventoryUtils.findEmptySlot(0, 8) != -1) {
               InventoryUtils.setSelectedSlot(InventoryUtils.findEmptySlot(0, 8), false);
               return false;
            }

            if (this.computeInt2() != -1 && InventoryUtils.findEmptySlot(9, 35) != -1) {
               this.updateState7();
               if (mc.player.getVelocity().horizontalLengthSquared() > 0.001) {
                  return true;
               }

               InventoryActionQueue.getInstance().queueSlotSwap(this.computeInt2(), InventoryUtils.findEmptySlot(9, 35), 1);
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private void updateState9() {
      this.flag6 = false;
      this.updateState6();
      MovementPauseController.getInstance().pauseForTicks(-1);
      this.updateState7();
      this.list.clear();
      if (mc.currentScreen instanceof DeathScreen) {
         mc.player.requestRespawn();
         mc.setScreen(null);
      }

      this.processData3("home", this.intVal9, AutoWardenModule.TypeOption.DEPOSIT);
   }

   private boolean matchesCriteria20(ItemStack itemStack) {
      return this.matchesCriteria2(itemStack, StatusEffects.SPEED);
   }

   private BlockPos getBlockPos6(BlockPos blockPos) {
      BlockPos blockPos2 = null;
      double local2337_3 = Double.MAX_VALUE;

      for (int index = -1; index <= 1; index++) {
         for (int index2 = -1; index2 <= 1; index2++) {
            for (int index3 = -1; index3 <= 1; index3++) {
               if ((index != 0 || index2 != 0)
                  && this.matchesCriteria(blockPos.add(index, index3, index2))
                  && !(mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(blockPos.add(index, index3, index2))) >= local2337_3)) {
                  local2337_3 = mc.player.squaredDistanceTo(Vec3d.ofBottomCenter(blockPos.add(index, index3, index2)));
                  blockPos2 = blockPos.add(index, index3, index2);
               }
            }
         }
      }

      return blockPos2;
   }

   private boolean checkState14() {
      int local2338_1 = 0;

      for (int index = 0; index < 36; index++) {
         if (!mc.player.getInventory().getStack(index).isEmpty()) {
            if (this.matchesCriteria6(mc.player.getInventory().getStack(index))) {
               local2338_1 += mc.player.getInventory().getStack(index).getCount();
            } else if (!this.matchesCriteria16(mc.player.getInventory().getStack(index))) {
               return true;
            }
         }
      }

      return local2338_1 > 1;
   }

   private boolean matchesCriteria21(PlayerEntity playerEntity) {
      for (EquipmentSlot equipmentSlot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         if (!playerEntity.getEquippedStack(equipmentSlot).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private boolean checkState15() {
      this.flag5 = false;
      if (mc.currentScreen instanceof HandledScreen) {
         this.updateState6();
         return false;
      }

      boolean local2340_1 = this.nested0388 == AutoWardenModule.TypeOption.HUNT
         || this.nested0388 == AutoWardenModule.TypeOption.BUFF && this.computeInt5() == this.computeInt7() && !this.timer.hasExceededMillis(20000L);
      if (local2340_1 && this.computeLong() <= 240000L && InventoryUtils.findOffhandSlot(this::matchesCriteria4) != -1) {
         return this.matchesCriteria15(this::matchesCriteria4, 90.0F);
      }

      if (local2340_1 && this.computeLong2() <= 50000L && InventoryUtils.findOffhandSlot(this::matchesCriteria20) != -1) {
         return this.matchesCriteria15(this::matchesCriteria20, 90.0F);
      }

      if (this.nested0388 != AutoWardenModule.TypeOption.HUNT
         && this.nested0388 != AutoWardenModule.TypeOption.CHEST
         && mc.player.getHungerManager().getFoodLevel() < 19
         && InventoryUtils.findOffhandSlot(this::matchesCriteria6) != -1) {
         return this.matchesCriteria15(this::matchesCriteria6, -90.0F);
      }

      this.updateState6();
      return false;
   }

   private enum TypeOption {
      SUPPLY,
      BUFF,
      WARP,
      HUNT,
      CHEST,
      ARENA,
      DEPOSIT;
   }
}
