package greed.client.module.misc;

import greed.mixin.BossBarHudMixin;
import greed.client.util.ClientChat;
import greed.client.event.ClientTickEvent;
import greed.client.util.Colors;
import greed.client.event.EventListener;
import greed.client.setting.LegacyNumberSetting;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.event.MovementInputEvent;
import greed.client.event.PacketEvent;
import greed.client.util.InventoryUtils;
import greed.client.setting.NumericTextSetting;
import greed.client.util.InventoryActionQueue;
import greed.client.RotationAngles;
import greed.client.RotationManager;
import greed.client.RotationSpeed;
import greed.client.util.Stopwatch;
import greed.client.event.WorldRenderEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;

@ModuleInfo(
   name = "AutoMine",
   description = "Перебирает анархии, находит автошахту с таймером меньше минуты, релогается на точке и копает алмазы.",
   category = ModuleCategory.MISC
)
public final class AutoMineModule extends Module {
   private static AutoMineModule autoMineModule;
   private static final Pattern pattern2 = Pattern.compile("^(\\d{1,2}):(\\d{2})$");
   private static final Pattern pattern3 = Pattern.compile(".*Телепортация через[:\\s]*(\\d+(?:[.,]\\d+)?)\\s*сек.*");
   private static final Pattern pattern4 = Pattern.compile(".*успешно купили.*");
   private static final String[] strValArray = new String[]{
      "an101",
      "an102",
      "an103",
      "an104",
      "an105",
      "an106",
      "an107",
      "an108",
      "an201",
      "an202",
      "an203",
      "an204",
      "an205",
      "an206",
      "an207",
      "an208",
      "an209",
      "an210",
      "an301",
      "an302",
      "an303",
      "an304",
      "an305",
      "an501",
      "an502",
      "an503",
      "an601",
      "an602",
      "an603"
   };
   private static final ArrayList<String> list = new ArrayList<>();
   private static final int[][] intValArray;
   private static final BlockPos blockPos7;
   private static final BlockPos blockPos8;
   private static final BlockPos blockPos9;
   private static final BlockPos blockPos10;
   private static final String[] strValArray2;
   public ModeSetting modeSetting;
   public NumericTextSetting numericTextSetting;
   public LegacyNumberSetting legacyNumberSetting;
   private AutoMineModule.TypeOption nested0489;
   private final Stopwatch timer = new Stopwatch();
   private final Stopwatch timer2 = new Stopwatch();
   private final Stopwatch timer3 = new Stopwatch();
   private final Stopwatch timer4 = new Stopwatch();
   private ServerInfo serverInfo;
   private boolean flag;
   private boolean flag2;
   private String strVal;
   private boolean flag3;
   private boolean flag4;
   private int[] intValArray2;
   private BlockPos blockPos11;
   private boolean flag5;
   private int intVal;
   private long timeMillis;
   private final ArrayList<BlockPos> list2 = new ArrayList<>();
   private long timeMillis2;
   private long timeMillis3;
   private long timeMillis4;
   private final ArrayDeque<BlockPos> arrayDeque = new ArrayDeque<>();
   private Runnable runnable2;
   private long timeMillis5;
   private ItemStack itemStack12 = ItemStack.EMPTY;
   private boolean flag6;
   private boolean flag7;
   private final EventListener<ClientTickEvent> clientTickListener = clientTickEvent -> {
      if (clientTickEvent.isPre()) {
         if (mc.player != null && mc.world != null && mc.interactionManager != null) {
            if (mc.getCurrentServerEntry() != null) {
               this.serverInfo = mc.getCurrentServerEntry();
            }

            this.timer4.reset();
            if (this.modeSetting.isSelected("FunTime")) {
               mc.options.attackKey.setPressed(false);
            }

            if (!this.flag4) {
               for (String local5649_5 : strValArray2) {
                  ClientChat.sendBaritoneMessage(local5649_5);
               }

               this.flag4 = true;
            }

            if (this.blockPos11 == null) {
               this.updateState4();
               this.list2.clear();
            }

            this.blockPos11 = null;
            if (!this.modeSetting.isSelected("FunTime") || !this.checkState2()) {
               if (this.runnable2 != null) {
                  if (System.currentTimeMillis() >= this.timeMillis5) {
                     Runnable runnable = this.runnable2;
                     this.runnable2 = null;
                     runnable.run();
                  }
               } else if (this.nested0489 != AutoMineModule.TypeOption.SCAN_TITLE
                     && this.nested0489 != AutoMineModule.TypeOption.STAND_READY
                     && this.nested0489 != AutoMineModule.TypeOption.DIG_MINE
                  || !mc.player.getMainHandStack().isIn(ItemTags.PICKAXES)
                  || !mc.player.getMainHandStack().isDamageable()
                  || mc.player.getMainHandStack().getMaxDamage() - mc.player.getMainHandStack().getDamage() > 1
                     && !(
                        (mc.player.getMainHandStack().getMaxDamage() - mc.player.getMainHandStack().getDamage())
                              * 100.0
                              / mc.player.getMainHandStack().getMaxDamage()
                           < 6.0
                     )) {
                  switch (this.nested0489) {
                     case SCAN_TITLE:
                        {
ScoreboardObjective objectiveForSlot = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
                        if (objectiveForSlot != null) {
                           String[] stringArray = objectiveForSlot.getDisplayName().getString().split("-");
                           if (stringArray.length > 1 && !stringArray[1].replaceAll("\\D", "").isEmpty()) {
                              this.strVal = "an" + stringArray[1].replaceAll("\\D", "");
                           }
                        }

                        Matcher matcher = pattern2.matcher(this.getOptional("Автошахт", pattern2, 150.0).orElse(""));
                        if (!matcher.matches()) {
                           if (this.timeMillis != 0L) {
                              if (!this.timer2.hasExceededMillis(this.timeMillis)) {
                                 this.checkState();
                                 return;
                              }

                              this.timeMillis = 0L;
                           }

                           this.timer2.reset();
                           if (!this.timer.hasExceededMillis(3000L)) {
                              return;
                           }

                           if (this.flag3) {
                              this.updateState3();
                              return;
                           }

                           mc.player.networkHandler.sendChatCommand("warp mine");
                           this.flag3 = true;
                           this.nested0489 = AutoMineModule.TypeOption.WARP_TELEPORT;
                           this.timer.reset();
                           return;
                        }

                        int local5649_4 = Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2));
                        if (this.strVal == null) {
                           this.updateState3();
                           return;
                        }

                        if (this.timeMillis == 0L || !this.timer2.hasExceededMillis(this.timeMillis)) {
                           if (this.checkState()) {
                              if (this.timeMillis == 0L) {
                                 this.timeMillis = 17000L;
                                 this.timer2.reset();
                              }

                              return;
                           }

                           if (this.timeMillis == 0L && !this.timer2.hasExceededMillis(600L)) {
                              return;
                           }
                        }

                        if (local5649_4 >= 60) {
                           this.updateState3();
                           return;
                        }

                        this.intValArray2 = intValArray[ThreadLocalRandom.current().nextInt(intValArray.length)];
                        mc.interactionManager.cancelBlockBreaking();
                        RotationManager.getRotationManager().resetRotation();
                        this.flag5 = false;
                        ClientChat.sendBaritoneMessage("#go " + this.intValArray2[0] + " " + this.intValArray2[1] + " " + this.intValArray2[2]);
                        this.nested0489 = AutoMineModule.TypeOption.BARITONE_RIDE;
                        break;
}
                     case WARP_TELEPORT:
                        {
for (ClientBossBar clientBossBar : ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().values()) {
                           Matcher matcher2 = pattern3.matcher(clientBossBar.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim());
                           if (matcher2.matches()) {
                              this.processData((long)(Double.parseDouble(matcher2.group(1).replace(',', '.')) * 1000.0) + 200L, () -> {
                                 this.nested0489 = AutoMineModule.TypeOption.SCAN_TITLE;
                                 this.timer.reset();
                                 this.timer2.reset();
                                 this.timeMillis = 0L;
                              });
                              return;
                           }
                        }

                        if (this.timer.hasExceededMillis(3000L)) {
                           this.nested0489 = AutoMineModule.TypeOption.SCAN_TITLE;
                           this.timer.reset();
                           this.timer2.reset();
                           this.timeMillis = 0L;
                        }
                        break;
}
                     case BARITONE_RIDE:
                        {
if (mc.interactionManager.isBreakingBlock()) {
                           mc.interactionManager.cancelBlockBreaking();
                        }

                        if (mc.player.squaredDistanceTo(this.intValArray2[0] + 0.5, this.intValArray2[1], this.intValArray2[2] + 0.5) > 2.25) {
                           return;
                        }

                        ClientChat.sendBaritoneMessage("#stop");
                        this.nested0489 = AutoMineModule.TypeOption.STAND_READY;
                        break;
}
                     case STAND_READY:
                        {
Matcher matcher3 = pattern2.matcher(this.getOptional("Автошахт", pattern2, 150.0).orElse(""));
                        if (!matcher3.matches()) {
                           return;
                        }

                        if (Integer.parseInt(matcher3.group(1)) * 60 + Integer.parseInt(matcher3.group(2))
                           <= (this.modeSetting.isSelected("FunTime") ? 3 : 2)) {
                           mc.interactionManager.cancelBlockBreaking();
                           mc.player.networkHandler.sendChatCommand("hub");
                           this.processData(this.modeSetting.isSelected("FunTime") ? 2500L : 600L, () -> {
                              mc.player.networkHandler.sendChatCommand(this.strVal);
                              this.processData(1500L, () -> {
                                 this.nested0489 = AutoMineModule.TypeOption.DIG_MINE;
                                 this.timer.reset();
                                 this.timer2.reset();
                              });
                           });
                           return;
                        }

                        if (Integer.parseInt(matcher3.group(1)) * 60 + Integer.parseInt(matcher3.group(2)) >= 20) {
                           this.checkState();
                           return;
                        }

                        if (mc.player.squaredDistanceTo(this.intValArray2[0] + 0.5, this.intValArray2[1], this.intValArray2[2] + 0.5) > 2.25) {
                           mc.interactionManager.cancelBlockBreaking();
                           RotationManager.getRotationManager().resetRotation();
                           this.flag5 = false;
                           ClientChat.sendBaritoneMessage("#go " + this.intValArray2[0] + " " + this.intValArray2[1] + " " + this.intValArray2[2]);
                           this.nested0489 = AutoMineModule.TypeOption.BARITONE_RIDE;
                        }
                        break;
}
                     case DIG_MINE:
                        {
if (this.timer2.hasExceededMillis(17000L)) {
                           mc.interactionManager.cancelBlockBreaking();
                           this.updateState3();
                           return;
                        }

                        if (this.checkState()) {
                           this.timer.reset();
                           return;
                        }

                        if (this.timer.hasExceededMillis(5000L)) {
                           this.updateState3();
                        }
                        break;
}
                     case HOME_TELEPORT:
                        {
for (ClientBossBar clientBossBar2 : ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().values()) {
                           Matcher matcher4 = pattern3.matcher(clientBossBar2.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim());
                           if (matcher4.matches()) {
                              this.processData((long)(Double.parseDouble(matcher4.group(1).replace(',', '.')) * 1000.0) + 500L, () -> {
                                 this.nested0489 = AutoMineModule.TypeOption.DUMP_DIAMONDS;
                                 this.timer.reset();
                              });
                              return;
                           }
                        }

                        if (this.timer.hasExceededMillis(3000L)) {
                           this.nested0489 = AutoMineModule.TypeOption.DUMP_DIAMONDS;
                           this.timer.reset();
                        }
                        break;
}
                     case DUMP_DIAMONDS:
                        {
if (!this.timer.hasExceededMillis(150L)) {
                           return;
                        }

                        int local5649_2 = -1;
                        int local5649_3 = 0;

                        for (; local5649_3 <= 35; local5649_3++) {
                           if (mc.player.getInventory().getStack(local5649_3).isOf(Items.DIAMOND)
                              && (
                                 local5649_2 == -1
                                    || mc.player.getInventory().getStack(local5649_3).getCount()
                                       > mc.player.getInventory().getStack(local5649_2).getCount()
                              )) {
                              local5649_2 = local5649_3;
                           }
                        }

                        if (local5649_2 == -1
                           || InventoryUtils.findSlotMatching(itemStack -> itemStack.isOf(Items.DIAMOND), 0, local5649_2 - 1) == -1
                              && InventoryUtils.findSlotMatching(itemStack2 -> itemStack2.isOf(Items.DIAMOND), local5649_2 + 1, 35) == -1) {
                           mc.player.networkHandler.sendChatCommand("ah search опыт");
                           this.nested0489 = AutoMineModule.TypeOption.AH_BUY;
                           this.timer.reset();
                           return;
                        }

                        InventoryUtils.sendSlotClickPacket(
                           InventoryUtils.getScreenSlotId(
                              InventoryUtils.findSlotMatching(itemStack3 -> itemStack3.isOf(Items.DIAMOND), 0, local5649_2 - 1) != -1
                                 ? InventoryUtils.findSlotMatching(itemStack4 -> itemStack4.isOf(Items.DIAMOND), 0, local5649_2 - 1)
                                 : InventoryUtils.findSlotMatching(itemStack5 -> itemStack5.isOf(Items.DIAMOND), local5649_2 + 1, 35)
                           ),
                           1,
                           SlotActionType.THROW,
                           false
                        );
                        this.timer.reset();
                        break;
}
                     case AH_BUY:
                        {
if (mc.player.getMainHandStack().isIn(ItemTags.PICKAXES)
                           && mc.player.getMainHandStack().getMaxDamage() - mc.player.getMainHandStack().getDamage()
                              >= mc.player.getMainHandStack().getMaxDamage() * 0.9F
                           && !mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)
                           && InventoryUtils.findInventorySlot(Items.EXPERIENCE_BOTTLE) == -1) {
                           if (mc.currentScreen != null) {
                              mc.player.closeHandledScreen();
                           }

                           this.flag = false;
                           this.nested0489 = AutoMineModule.TypeOption.SPREAD_DIAMONDS;
                           this.timer.reset();
                           return;
                        }

                        if (!this.flag2 && InventoryUtils.findInventorySlot(Items.EXPERIENCE_BOTTLE) == -1) {
                           if (!(mc.currentScreen instanceof GenericContainerScreen genericContainerScreen)) {
                              if (this.timer.hasExceededMillis(3000L)) {
                                 mc.player.networkHandler.sendChatCommand("ah search опыт");
                                 this.timer.reset();
                              }

                              return;
                           }

                           if (!genericContainerScreen.getTitle().getString().contains("Подозрительная цена")
                              && !genericContainerScreen.getTitle().getString().contains("Подтверждение покупки")) {
                              if (!AuctionHelperModule.getInstance().matchesCriteria3(genericContainerScreen.getTitle().getString())) {
                                 return;
                              }

                              if (this.flag) {
                                 if (!this.timer3.hasExceededMillis(3000L)) {
                                    return;
                                 }

                                 this.flag = false;
                              }

                              int local5649_3 = -1;
                              long local5649_4 = Long.MAX_VALUE;

                              for (int index = 0; index < mc.player.currentScreenHandler.slots.size() - 36 - 9; index++) {
                                 if (mc.player.currentScreenHandler.getSlot(index).hasStack()
                                    && mc.player.currentScreenHandler.getSlot(index).getStack().isOf(Items.EXPERIENCE_BOTTLE)) {
                                    String local5649_7 = mc.player
                                       .currentScreenHandler
                                       .getSlot(index)
                                       .getStack()
                                       .getName()
                                       .getString()
                                       .replaceAll("(?i)§[0-9a-fk-or]", "");
                                    if (!AuctionHelperModule.getInstance().formatText2(local5649_7).contains("не актуален")
                                       && !AuctionHelperModule.getInstance().formatText2(local5649_7).matches(".*\\[\\s*\\d+\\s*ур.*")
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
                                       && this.matchesCriteria6(mc.player.currentScreenHandler.getSlot(index).getStack())
                                       && AuctionHelperModule.getInstance().computeInt(mc.player.currentScreenHandler.getSlot(index).getStack())
                                          >= 0
                                       && !(
                                          AuctionHelperModule.getInstance().computeInt(mc.player.currentScreenHandler.getSlot(index).getStack())
                                             > this.legacyNumberSetting.getValue()
                                       )
                                       && AuctionHelperModule.getInstance().computeInt(mc.player.currentScreenHandler.getSlot(index).getStack())
                                          < local5649_4) {
                                       local5649_3 = index;
                                       local5649_4 = AuctionHelperModule.getInstance()
                                          .computeInt(mc.player.currentScreenHandler.getSlot(index).getStack());
                                    }
                                 }
                              }

                              if (local5649_3 != -1) {
                                 InventoryUtils.sendSlotClickPacket(local5649_3, 0, SlotActionType.PICKUP, false);
                                 this.flag = true;
                                 this.timer3.reset();
                                 this.timer.reset();
                                 return;
                              }

                              if (this.timer.hasExceededMillis(400L)) {
                                 InventoryUtils.sendSlotClickPacket(mc.player.currentScreenHandler.slots.size() - 36 - 5, 0, SlotActionType.PICKUP, false);
                                 this.timer.reset();
                              }
                              break;
                           }

                           if (mc.player
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
                              this.flag = false;
                              this.timer.reset();
                              return;
                           }

                           InventoryUtils.sendSlotClickPacket(0, 0, SlotActionType.QUICK_MOVE, false);
                           this.timer.reset();
                           return;
                        }

                        if (mc.currentScreen != null) {
                           mc.player.closeHandledScreen();
                        }

                        this.flag2 = false;
                        this.flag = false;
                        this.nested0489 = AutoMineModule.TypeOption.XP_THROW;
                        this.timer.reset();
                        return;
}
                     case XP_THROW:
                        {
mc.player.closeHandledScreen();
                        this.updateState2();
                        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                           InventoryUtils.sendSlotClickPacket(InventoryUtils.getScreenSlotId(InventoryUtils.findEmptySlot(0, 35)), 0, SlotActionType.PICKUP, false);
                           return;
                        }

                        if (mc.player.getMainHandStack().isIn(ItemTags.PICKAXES)
                           && mc.player.getMainHandStack().getMaxDamage() - mc.player.getMainHandStack().getDamage()
                              >= mc.player.getMainHandStack().getMaxDamage() * 0.9F
                           && !mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)
                           && InventoryUtils.findInventorySlot(Items.EXPERIENCE_BOTTLE) == -1) {
                           if (!this.itemStack12.isEmpty()
                              && mc.player.getOffHandStack().isEmpty()
                              && InventoryUtils.findOffhandSlot(itemStack6 -> ItemStack.areItemsAndComponentsEqual(itemStack6, this.itemStack12)) != -1) {
                              InventoryUtils.sendSlotClickPacket(
                                 InventoryUtils.getScreenSlotId(
                                    InventoryUtils.findOffhandSlot(itemStack7 -> ItemStack.areItemsAndComponentsEqual(itemStack7, this.itemStack12))
                                 ),
                                 0,
                                 SlotActionType.PICKUP,
                                 false
                              );
                              InventoryUtils.sendSlotClickPacket(InventoryUtils.getScreenSlotId(40), 0, SlotActionType.PICKUP, false);
                              this.itemStack12 = ItemStack.EMPTY;
                              return;
                           }

                           this.itemStack12 = ItemStack.EMPTY;
                           this.nested0489 = AutoMineModule.TypeOption.SPREAD_DIAMONDS;
                           this.timer.reset();
                           return;
                        }

                        if (mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)) {
                           RotationManager.getRotationManager()
                              .setRotation(
                                 new RotationAngles(mc.player.getYaw(), 90.0F),
                                 new RotationSpeed(0.0F, Math.abs(90.0F - mc.player.getPitch()) * 0.3F, 30.0F, 30.0F),
                                 RotationManager.ReturnMode.Empty
                              );
                           if (mc.player.getPitch() < 75.0F) {
                              return;
                           }

                           InventoryUtils.useItem(Hand.OFF_HAND);
                           return;
                        }

                        if (InventoryUtils.findInventorySlot(Items.EXPERIENCE_BOTTLE) != -1) {
                           InventoryUtils.sendSlotClickPacket(
                              InventoryUtils.getScreenSlotId(InventoryUtils.findInventorySlot(Items.EXPERIENCE_BOTTLE)), 0, SlotActionType.PICKUP, false
                           );
                           InventoryUtils.sendSlotClickPacket(InventoryUtils.getScreenSlotId(40), 0, SlotActionType.PICKUP, false);
                           if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                              InventoryUtils.sendSlotClickPacket(
                                 InventoryUtils.getScreenSlotId(InventoryUtils.findEmptySlot(0, 35)), 0, SlotActionType.PICKUP, false
                              );
                           }

                           return;
                        }

                        if (!this.timer.hasExceededMillis(1500L)) {
                           return;
                        }

                        if (mc.player.getMainHandStack().getMaxDamage() - mc.player.getMainHandStack().getDamage()
                           < mc.player.getMainHandStack().getMaxDamage() * 0.93F) {
                           mc.player.networkHandler.sendChatCommand("ah search опыт");
                           this.nested0489 = AutoMineModule.TypeOption.AH_BUY;
                           this.timer.reset();
                           return;
                        }

                        this.nested0489 = AutoMineModule.TypeOption.SPREAD_DIAMONDS;
                        this.timer.reset();
                        break;
}
                     case SPREAD_DIAMONDS:
                        {
if (!this.timer.hasExceededMillis(150L)) {
                           return;
                        }

                        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                           if (!mc.player.currentScreenHandler.getCursorStack().isOf(Items.DIAMOND)) {
                              InventoryUtils.sendSlotClickPacket(
                                 InventoryUtils.getScreenSlotId(InventoryUtils.findEmptySlot(0, 35)), 0, SlotActionType.PICKUP, false
                              );
                              this.timer.reset();
                              return;
                           }

                           if (InventoryUtils.findEmptySlot(0, 35) != -1) {
                              InventoryUtils.sendSlotClickPacket(
                                 InventoryUtils.getScreenSlotId(InventoryUtils.findEmptySlot(0, 35)), 1, SlotActionType.PICKUP, false
                              );
                           } else {
                              InventoryUtils.sendSlotClickPacket(-999, 0, SlotActionType.PICKUP, false);
                           }

                           this.timer.reset();
                           return;
                        }

                        if (InventoryUtils.findOffhandSlot(itemStack8 -> itemStack8.isOf(Items.DIAMOND) && itemStack8.getCount() > 1) != -1
                           && InventoryUtils.findEmptySlot(0, 35) != -1) {
                           InventoryUtils.sendSlotClickPacket(
                              InventoryUtils.getScreenSlotId(
                                 InventoryUtils.findOffhandSlot(itemStack9 -> itemStack9.isOf(Items.DIAMOND) && itemStack9.getCount() > 1)
                              ),
                              0,
                              SlotActionType.PICKUP,
                              false
                           );
                           this.timer.reset();
                           return;
                        }

                        if (InventoryUtils.findOffhandSlot(itemStack10 -> itemStack10.isOf(Items.DIAMOND) && itemStack10.getCount() > 1) != -1) {
                           InventoryUtils.sendSlotClickPacket(
                              InventoryUtils.getScreenSlotId(
                                 InventoryUtils.findOffhandSlot(itemStack11 -> itemStack11.isOf(Items.DIAMOND) && itemStack11.getCount() > 1)
                              ),
                              1,
                              SlotActionType.THROW,
                              false
                           );
                           return;
                        }

                        this.updateState3();
}
                  }
               } else {
                  mc.interactionManager.cancelBlockBreaking();
                  if (!mc.player.getOffHandStack().isEmpty()
                     && !mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)
                     && InventoryUtils.findEmptySlot(0, 35) != -1) {
                     this.itemStack12 = mc.player.getOffHandStack().copy();
                     InventoryUtils.sendSlotClickPacket(InventoryUtils.getScreenSlotId(40), 0, SlotActionType.PICKUP, false);
                     InventoryUtils.sendSlotClickPacket(InventoryUtils.getScreenSlotId(InventoryUtils.findEmptySlot(0, 35)), 0, SlotActionType.PICKUP, false);
                  }

                  mc.player.networkHandler.sendChatCommand("an" + this.numericTextSetting.getText().replaceAll("\\D", ""));
                  this.strVal = "an" + this.numericTextSetting.getText().replaceAll("\\D", "");
                  this.processData(1500L, () -> {
                     mc.player.networkHandler.sendChatCommand("home");
                     this.nested0489 = AutoMineModule.TypeOption.HOME_TELEPORT;
                     this.timer.reset();
                  });
               }
            }
         } else {
            this.updateState();
         }
      }
   };
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (packetEvent.isIncoming()
         && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket
         && pattern4.matcher(AuctionHelperModule.getInstance().formatText2(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))).matches()) {
         this.flag2 = true;
      }
   };
   private final EventListener<PacketEvent> packetListener2 = packetEvent2 -> {
      if (packetEvent2.isIncoming() && packetEvent2.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket2) {
         if (Formatting.strip(gameMessageS2CPacket2.content().getString()).toLowerCase().contains("delnmt")) {
            packetEvent2.processData(true);
         }
      }
   };
   private final EventListener<MovementInputEvent> movementInputListener = movementInputEvent -> {
      if (mc.player != null && this.blockPos11 != null && this.nested0489 != AutoMineModule.TypeOption.BARITONE_RIDE) {
         movementInputEvent.setForward(1.0F);
         RotationManager.correctMovement(
            movementInputEvent,
            mc.player.getYaw(),
            (float)(
                  Math.atan2(this.blockPos11.getZ() + 0.5 - mc.player.getZ(), this.blockPos11.getX() + 0.5 - mc.player.getX())
                     * 180.0
                     / Math.PI
               )
               - 90.0F
         );
         if (!mc.player.horizontalCollision) {
            this.timeMillis3 = 0L;
         } else {
            if (this.timeMillis3 == 0L) {
               this.timeMillis3 = System.currentTimeMillis();
            }
         }
      }
   };
   private final EventListener<WorldRenderEvent> worldRenderListener = worldRenderEvent -> {
      if (mc.player != null && mc.world != null && !this.list2.isEmpty()) {
         for (BlockPos blockPos : this.list2) {
            worldRenderEvent.getMatrixStack().push();
            worldRenderEvent.getMatrixStack()
               .translate(
                  blockPos.getX() - mc.gameRenderer.getCamera().getCameraPos().x,
                  blockPos.getY() - mc.gameRenderer.getCamera().getCameraPos().y,
                  blockPos.getZ() - mc.gameRenderer.getCamera().getCameraPos().z
               );
            Matrix4f positionMatrix = worldRenderEvent.getMatrixStack().peek().getPositionMatrix();
            VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.debugQuads());
            float[] floatArray = new float[]{0.0F, 1.0F};
            float[] floatArray2 = new float[]{0.0F, 1.0F};
            float[] floatArray3 = new float[]{0.0F, 1.0F};
            int[][] intArrayArray = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};

            for (int[] intArray : intArrayArray) {
               for (int local5645_17 : intArray) {
                  vertexConsumer.vertex(positionMatrix, floatArray[local5645_17 >> 2], floatArray2[local5645_17 >> 1 & 1], floatArray3[local5645_17 & 1])
                     .color(Colors.computeInt6(blockPos.equals(this.blockPos11) ? -16711826 : -16718337, 0.27450982F));
               }
            }

            VertexRendering.drawOutline(
               worldRenderEvent.getMatrixStack(),
               mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines()),
               VoxelShapes.cuboid(new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
               0.0,
               0.0,
               0.0,
               Colors.computeInt6(blockPos.equals(this.blockPos11) ? -16711826 : -16718337, 0.8F),
               1.0F
            );
            mc.getBufferBuilders().getEntityVertexConsumers().draw();
            worldRenderEvent.getMatrixStack().pop();
         }
      }
   };

   private void updateState() {
      if (this.serverInfo != null
         && (
            mc.currentScreen instanceof DisconnectedScreen
               || mc.currentScreen instanceof TitleScreen
               || mc.currentScreen instanceof MultiplayerScreen
         )
         && this.timer4.hasExceededMillis(5000L)) {
         this.timer4.reset();
         this.nested0489 = AutoMineModule.TypeOption.SCAN_TITLE;
         this.strVal = null;
         this.flag3 = false;
         this.runnable2 = null;
         this.flag = false;
         this.flag2 = false;
         this.timeMillis = 0L;
         this.timer.reset();
         this.timer2.reset();
         ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), mc, ServerAddress.parse(this.serverInfo.address), this.serverInfo, false, null);
      }
   }

   public static AutoMineModule getInstance() {
      if (autoMineModule == null) {
         autoMineModule = new AutoMineModule();
      }

      return autoMineModule;
   }

   private boolean matchesCriteria(BlockPos blockPos) {
      return !this.matchesCriteria3(blockPos) && (!this.matchesCriteria4(blockPos) || !this.matchesCriteria4(blockPos.up()));
   }

   private boolean matchesCriteria2(BlockPos blockPos) {
      Vec3d vec3d = new Vec3d(blockPos.getX() + 0.5 - mc.player.getX(), 0.0, blockPos.getZ() + 0.5 - mc.player.getZ());
      if (vec3d.lengthSquared() < 0.01) {
         return false;
      }

      Vec3d vec3d2 = new Vec3d(-vec3d.z, 0.0, vec3d.x).normalize().multiply(0.35);

      for (double local5650_4 = 0.0; local5650_4 <= Math.min(vec3d.length(), 48.0); local5650_4 += 0.25) {
         Vec3d vec3d3 = mc.player.getEntityPos().add(vec3d.normalize().multiply(local5650_4));
         if (this.matchesCriteria(BlockPos.ofFloored(vec3d3))
            || this.matchesCriteria(BlockPos.ofFloored(vec3d3.add(vec3d2)))
            || this.matchesCriteria(BlockPos.ofFloored(vec3d3.subtract(vec3d2)))) {
            return true;
         }
      }

      return false;
   }

   private void updateState2() {
      if (!mc.player.getMainHandStack().isIn(ItemTags.PICKAXES)
         && InventoryUtils.findOffhandSlot(itemStack -> itemStack.isIn(ItemTags.PICKAXES)) != -1) {
         if (InventoryUtils.findOffhandSlot(itemStack2 -> itemStack2.isIn(ItemTags.PICKAXES)) <= 8) {
            InventoryUtils.setSelectedSlot(InventoryUtils.findOffhandSlot(itemStack3 -> itemStack3.isIn(ItemTags.PICKAXES)), false);
         } else if (!InventoryActionQueue.getInstance().hasPendingActions()) {
            InventoryActionQueue.getInstance()
               .queueSlotSwap(InventoryUtils.findOffhandSlot(itemStack4 -> itemStack4.isIn(ItemTags.PICKAXES)), InventoryUtils.getSelectedSlot(), 1);
         }
      }
   }

   private Optional<String> getOptional(String local5657_1, Pattern pattern, double local5657_3) {
      if (mc.player != null && mc.world != null && local5657_1 != null && pattern != null) {
         List<ArmorStandEntity> local5657_5 = mc.world
            .getOtherEntities(
               mc.player,
               mc.player.getBoundingBox().expand(local5657_3),
               entity -> entity instanceof ArmorStandEntity armorStandEntity && armorStandEntity.hasCustomName()
            )
            .stream()
            .map(ArmorStandEntity.class::cast)
            .sorted(Comparator.comparingDouble(armorStandEntity2 -> armorStandEntity2.squaredDistanceTo(mc.player)))
            .toList();

         for (ArmorStandEntity armorStandEntity3 : local5657_5) {
            if (armorStandEntity3.getName()
               .getString()
               .replaceAll("(?i)§[0-9a-fk-or]", "")
               .trim()
               .toLowerCase()
               .replace("-", "")
               .replace(" ", "")
               .contains(local5657_1.toLowerCase())) {
               ArmorStandEntity armorStandEntity4 = local5657_5.stream()
                  .filter(armorStandEntity5 -> pattern.matcher(armorStandEntity5.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim()).matches())
                  .min(Comparator.comparingDouble(armorStandEntity6 -> Math.abs(armorStandEntity6.getY() - armorStandEntity3.getY())))
                  .orElse(null);
               if (armorStandEntity4 != null) {
                  return Optional.of(armorStandEntity4.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim());
               }
            }
         }

         return Optional.empty();
      } else {
         return Optional.empty();
      }
   }

   static {
      for (int[] intArray2 : new int[][]{{101, 114}, {201, 228}, {301, 325}, {501, 516}, {901, 904}}) {
         for (int index2 = intArray2[0]; index2 <= intArray2[1]; index2++) {
            list.add("an" + index2);
         }
      }

      intValArray = new int[][]{
         {-53, 69, 33},
         {-55, 69, 39},
         {-55, 69, 30},
         {-50, 69, 33},
         {-67, 69, 25},
         {-55, 69, 25},
         {-51, 69, 30},
         {-52, 69, 43},
         {-62, 69, 40},
         {-67, 69, 40},
         {-67, 69, 26},
         {-63, 69, 27},
         {-61, 69, 30},
         {-56, 69, 32}
      };
      blockPos7 = new BlockPos(-47, 78, 31);
      blockPos8 = new BlockPos(-47, 78, 37);
      blockPos9 = new BlockPos(-69, 69, 24);
      blockPos10 = new BlockPos(-49, 78, 44);
      strValArray2 = new String[]{
         "#set renderPath false",
         "#set renderGoal false",
         "#set allowPlace true",
         "#set allowSprint true",
         "#set costHeuristic 1.0",
         "#set allowParkour true",
         "#set allowDiagonalDescend true",
         "#set allowDownward true",
         "#set allowBreak true",
         "#set allowDiagonalAscend true",
         "#set allowParkourAscend true",
         "#set allowParkourPlace true",
         "#set maxFallHeightNoWater 255",
         "#set maxFallHeightBucket 255",
         "#set allowWaterBucketFall false",
         "#set assumeWalkOnLava true",
         "#set assumeWalkOnWater true",
         "#set sprintInWater true",
         "#set blockBreakAdditionalPenalty 0.0",
         "#set chunkCaching false",
         "#set pruneRegionsFromRAM true",
         "#set renderCachedChunks false",
         "#set blocksToDisallowBreaking minecraft:andesite,minecraft:infested_stone_bricks,minecraft:stone_bricks,minecraft:infested_chiseled_stone_bricks,minecraft:chiseled_stone_bricks,minecraft:andesite_wall,minecraft:stone_brick_stairs,minecraft:gray_concrete_powder,minecraft:light_gray_concrete,minecraft:ladder,minecraft:gray_wool,minecraft:red_wool,minecraft:stone_stairs,minecraft:red_terracotta,minecraft:red_sand,minecraft:red_concrete,minecraft:andesite_stairs,minecraft:brick_slab,minecraft:stone_slab,minecraft:stone_brick_slab,minecraft:red_glazed_terracotta,minecraft:red_concrete_powder,minecraft:crimson_stairs,minecraft:snow_block,minecraft:snow,minecraft:coarse_dirt,minecraft:dirt,minecraft:brown_wool,minecraft:oak_leaves,minecraft:brown_concrete,minecraft:white_concrete,minecraft:brown_concrete_powder,minecraft:white_concrete_powder,minecraft:oak_slab,minecraft:stripped_spruce_wood,minecraft:spruce_stairs,minecraft:spruce_fence,minecraft:prismarine,minecraft:clay,minecraft:light_gray_concrete_powder,minecraft:light_blue_stained_glass,minecraft:dark_oak_slab,minecraft:stripped_spruce_log,minecraft:short_grass,minecraft:podzol,minecraft:grass_block,minecraft:ice,minecraft:packed_ice,minecraft:blue_ice,minecraft:blue_terracotta,minecraft:brown_terracotta,minecraft:blue_concrete_powder,minecraft:glass_pane,minecraft:glass,minecraft:orange_terracotta,minecraft:gray_terracotta,minecraft:stripped_oak_log,minecraft:stripped_dark_oak_log,minecraft:spruce_leaves,minecraft:mud_bricks,minecraft:mud_brick_stairs,minecraft:packed_mud,minecraft:cobblestone,minecraft:cobblestone_wall,minecraft:cobblestone_stairs,minecraft:cobblestone_slab,minecraft:stone_brick_wall,minecraft:polished_andesite,minecraft:andesite_slab,minecraft:gravel,minecraft:tuff,minecraft:orange_glazed_terracotta,minecraft:brown_glazed_terracotta,minecraft:note_block,minecraft:jukebox,minecraft:barrel,minecraft:stripped_dark_oak_wood,minecraft:stripped_oak_wood"
      };
   }

   private boolean matchesCriteria3(BlockPos blockPos) {
      return blockPos.getX() >= blockPos9.getX()
         && blockPos.getX() <= blockPos10.getX()
         && blockPos.getY() >= blockPos9.getY()
         && blockPos.getY() <= blockPos10.getY()
         && blockPos.getZ() >= blockPos9.getZ()
         && blockPos.getZ() <= blockPos10.getZ();
   }

   private void processData(long local5662_1, Runnable runnable) {
      this.runnable2 = runnable;
      this.timeMillis5 = System.currentTimeMillis() + local5662_1;
   }

   private AutoMineModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Сервер", "Какой список анархий перебирать.", "SpookyTime", "SpookyTime", "FunTime"));
      this.numericTextSetting = this.addSetting(
         new NumericTextSetting("Анархия дома", "Номер анархии, куда лететь чинить кирку: там должен стоять /home рядом с аукционом.", "101", 4).numericOnly()
      );
      this.legacyNumberSetting = this.addSetting(
         new LegacyNumberSetting("Цена стака опыта", "Дороже этого стак опыта на аукционе не покупать.", 200000.0, 10000.0, 1000000.0, 10000.0)
      );
   }

   private boolean checkState() {
      if (this.nested0489 == AutoMineModule.TypeOption.BARITONE_RIDE) {
         return false;
      }

      this.updateState2();
      if (System.currentTimeMillis() >= this.timeMillis2) {
         this.timeMillis2 = System.currentTimeMillis() + 200L;
         this.list2.clear();

         for (BlockPos blockPos : BlockPos.iterate(mc.player.getBlockPos().add(-35, -35, -35), mc.player.getBlockPos().add(35, 35, 35))) {
            if ((
                  mc.world.getBlockState(blockPos).isOf(Blocks.DIAMOND_ORE)
                     || mc.world.getBlockState(blockPos).isOf(Blocks.DEEPSLATE_DIAMOND_ORE)
               )
               && this.matchesCriteria3(blockPos)
               && !(blockPos.getY() - mc.player.getY() > 6.0)
               && !(mc.player.getEyePos().squaredDistanceTo(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > 1225.0)) {
               this.list2.add(blockPos.toImmutable());
            }
         }

         this.intVal = this.list2.size();
      }

      this.list2
         .removeIf(
            blockPos2 -> !mc.world.getBlockState(blockPos2).isOf(Blocks.DIAMOND_ORE)
               && !mc.world.getBlockState(blockPos2).isOf(Blocks.DEEPSLATE_DIAMOND_ORE)
         );
      BlockPos blockPos3 = null;
      double local5667_2 = Double.MAX_VALUE;
      boolean local5667_4 = mc.player.getBlockPos().getY() == blockPos7.getY();

      for (BlockPos blockPos4 : this.list2) {
         double local5667_7 = mc.player.getEyePos().squaredDistanceTo(blockPos4.getX() + 0.5, blockPos4.getY() + 0.5, blockPos4.getZ() + 0.5)
            + (local5667_4 && this.matchesCriteria2(blockPos4) ? 4096.0 : 0.0);
         if (!(local5667_7 >= local5667_2)) {
            local5667_2 = local5667_7;
            blockPos3 = blockPos4;
         }
      }

      if (blockPos3 == null) {
         return false;
      }

      if (!this.arrayDeque.isEmpty()
         && (
            this.arrayDeque.peekFirst().getY() != mc.player.getBlockPos().getY()
               || !this.matchesCriteria4(this.arrayDeque.peekFirst())
               || !this.matchesCriteria4(this.arrayDeque.peekFirst().up())
               || Math.hypot(
                     this.arrayDeque.peekFirst().getX() + 0.5 - mc.player.getX(), this.arrayDeque.peekFirst().getZ() + 0.5 - mc.player.getZ()
                  )
                  > 3.0
         )) {
         this.arrayDeque.clear();
      }

      while (
         !this.arrayDeque.isEmpty()
            && Math.hypot(
                  this.arrayDeque.peekFirst().getX() + 0.5 - mc.player.getX(), this.arrayDeque.peekFirst().getZ() + 0.5 - mc.player.getZ()
               )
               < 0.3
      ) {
         this.arrayDeque.pollFirst();
      }

      if (this.arrayDeque.isEmpty() && local5667_4 && local5667_2 >= 4096.0 && System.currentTimeMillis() >= this.timeMillis4) {
         this.timeMillis4 = System.currentTimeMillis() + 1000L;
         this.processData2(blockPos3);
      }

      boolean local5667_5 = !this.arrayDeque.isEmpty();
      if (!local5667_5 && this.timeMillis3 != 0L && System.currentTimeMillis() - this.timeMillis3 >= 34L) {
         Box box = mc.player
            .getBoundingBox()
            .offset(
               new Vec3d(blockPos3.getX() + 0.5 - mc.player.getX(), 0.0, blockPos3.getZ() + 0.5 - mc.player.getZ())
                  .normalize()
                  .multiply(0.3)
            );
         BlockPos blockPos5 = null;

         for (BlockPos blockPos6 : BlockPos.iterate(
            BlockPos.ofFloored(box.minX, box.minY, box.minZ), BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ)
         )) {
            if (!mc.world.getBlockState(blockPos6).getCollisionShape(mc.world, blockPos6).isEmpty()
               && this.matchesCriteria3(blockPos6)
               && (
                  blockPos5 == null
                     || blockPos6.getY() < blockPos5.getY()
                     || blockPos6.getY() == blockPos5.getY()
                        && mc.player.getEyePos().squaredDistanceTo(blockPos6.getX() + 0.5, blockPos6.getY() + 0.5, blockPos6.getZ() + 0.5)
                           < mc.player.getEyePos().squaredDistanceTo(blockPos5.getX() + 0.5, blockPos5.getY() + 0.5, blockPos5.getZ() + 0.5)
               )) {
               blockPos5 = blockPos6.toImmutable();
            }
         }

         if (blockPos5 != null) {
            blockPos3 = blockPos5;
         }
      }

      if (local5667_5) {
         blockPos3 = this.arrayDeque.peekFirst();
      }

      this.blockPos11 = blockPos3;
      this.flag5 = true;
      boolean local5667_6 = (blockPos3.getX() + 0.5 - mc.player.getEyePos().x) * (blockPos3.getX() + 0.5 - mc.player.getEyePos().x)
            + (blockPos3.getZ() + 0.5 - mc.player.getEyePos().z) * (blockPos3.getZ() + 0.5 - mc.player.getEyePos().z)
         < 0.0925;
      float local5667_7 = local5667_6
         ? mc.player.getYaw()
         : (float)(
            Math.toDegrees(Math.atan2(blockPos3.getZ() + 0.5 - mc.player.getEyePos().z, blockPos3.getX() + 0.5 - mc.player.getEyePos().x))
               - 90.0
         );
      float local5667_8 = (float)(
         -Math.toDegrees(
            Math.atan2(
               blockPos3.getY() + 0.5 - mc.player.getEyePos().y,
               Math.sqrt(
                  (blockPos3.getX() + 0.5 - mc.player.getEyePos().x) * (blockPos3.getX() + 0.5 - mc.player.getEyePos().x)
                     + (blockPos3.getZ() + 0.5 - mc.player.getEyePos().z) * (blockPos3.getZ() + 0.5 - mc.player.getEyePos().z)
               )
            )
         )
      );
      if (local5667_5) {
         local5667_8 = mc.player.getPitch();
      }

      boolean local5667_9 = local5667_5
         ? Math.abs(MathHelper.wrapDegrees(local5667_7 - mc.player.getYaw())) < 3.0F
         : mc.player.raycast(mc.player.getBlockInteractionRange(), 1.0F, false) instanceof BlockHitResult blockHitResult
            && blockHitResult.getType() == Type.BLOCK
            && blockHitResult.getBlockPos().equals(blockPos3);
      if (!local5667_9) {
         RotationManager.getRotationManager()
            .setRotation(
               new RotationAngles(local5667_7, local5667_8),
               new RotationSpeed(
                  Math.abs(MathHelper.wrapDegrees(local5667_7 - mc.player.getYaw())) * 0.3F,
                  Math.abs(local5667_8 - mc.player.getPitch()) * 0.3F,
                  30.0F,
                  30.0F
               ),
               RotationManager.ReturnMode.Empty
            );
      }

      if (local5667_5) {
         return true;
      }

      if (mc.player.raycast(mc.player.getBlockInteractionRange(), 1.0F, false) instanceof BlockHitResult blockHitResult2
         && blockHitResult2.getType() == Type.BLOCK) {
         mc.crosshairTarget = blockHitResult2;
         mc.interactionManager.updateBlockBreakingProgress(blockHitResult2.getBlockPos(), blockHitResult2.getSide());
         mc.player.swingHand(Hand.MAIN_HAND);
      }

      return true;
   }

   private void updateState3() {
      String local5670_1;
      do {
         local5670_1 = this.modeSetting.isSelected("FunTime")
            ? list.get(ThreadLocalRandom.current().nextInt(list.size()))
            : strValArray[ThreadLocalRandom.current().nextInt(strValArray.length)];
      } while (local5670_1.equals(this.strVal));

      mc.player.networkHandler.sendChatCommand(local5670_1);
      this.strVal = local5670_1;
      this.flag3 = false;
      this.processData(1500L, () -> {
         this.nested0489 = AutoMineModule.TypeOption.SCAN_TITLE;
         this.timer.reset();
         this.timer2.reset();
         this.timeMillis = 0L;
      });
   }

   @Override
   protected void onEnable() {
      this.nested0489 = AutoMineModule.TypeOption.SCAN_TITLE;
      this.strVal = null;
      this.flag3 = false;
      this.flag4 = false;
      this.runnable2 = null;
      this.blockPos11 = null;
      this.flag5 = false;
      this.flag = false;
      this.flag2 = false;
      this.timeMillis = 0L;
      this.timeMillis3 = 0L;
      this.arrayDeque.clear();
      this.flag7 = false;
      this.timer.reset();
      this.timer2.reset();
   }

   private boolean checkState2() {
      if (mc.player.getHungerManager().getFoodLevel() <= 6
         || this.nested0489 == AutoMineModule.TypeOption.STAND_READY && mc.player.getHungerManager().getFoodLevel() < 20) {
         if (InventoryUtils.findOffhandSlot(itemStack -> itemStack.contains(DataComponentTypes.FOOD)) == -1) {
            if (this.flag6) {
               mc.options.useKey.setPressed(false);
            }

            this.flag6 = false;
            return false;
         } else if (!mc.player.getMainHandStack().contains(DataComponentTypes.FOOD)) {
            if (InventoryUtils.findOffhandSlot(itemStack2 -> itemStack2.contains(DataComponentTypes.FOOD)) <= 8) {
               InventoryUtils.setSelectedSlot(InventoryUtils.findOffhandSlot(itemStack3 -> itemStack3.contains(DataComponentTypes.FOOD)), false);
            } else if (!InventoryActionQueue.getInstance().hasPendingActions()) {
               InventoryActionQueue.getInstance()
                  .queueSlotSwap(InventoryUtils.findOffhandSlot(itemStack4 -> itemStack4.contains(DataComponentTypes.FOOD)), InventoryUtils.getSelectedSlot(), 1);
            }

            return true;
         } else {
            mc.interactionManager.cancelBlockBreaking();
            mc.options.useKey.setPressed(true);
            this.flag6 = true;
            return true;
         }
      } else {
         if (this.flag6) {
            mc.options.useKey.setPressed(false);
         }

         this.flag6 = false;
         return false;
      }
   }

   private boolean matchesCriteria4(BlockPos blockPos) {
      return mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty();
   }

   @Override
   protected void onDisable() {
      if (this.flag6) {
         mc.options.useKey.setPressed(false);
      }

      this.flag6 = false;
      this.flag7 = false;
      mc.options.sneakKey.setPressed(false);
      mc.options.attackKey.setPressed(false);
      this.itemStack12 = ItemStack.EMPTY;
      if (mc.player != null) {
         if (this.nested0489 == AutoMineModule.TypeOption.BARITONE_RIDE) {
            ClientChat.sendBaritoneMessage("#stop");
         }

         if ((this.nested0489 == AutoMineModule.TypeOption.DIG_MINE || this.nested0489 == AutoMineModule.TypeOption.STAND_READY)
            && mc.interactionManager != null) {
            mc.interactionManager.cancelBlockBreaking();
         }
      }
   }

   private void updateState4() {
      this.flag5 = false;
   }

   public boolean matchesCriteria5(String local5687_1) {
      return local5687_1.replaceAll("(?i)§[0-9a-fk-or]", "").strip().startsWith("[Baritone]")
         || local5687_1.replaceAll("(?i)§[0-9a-fk-or]", "").strip().startsWith("Baritone");
   }

   private void processData2(BlockPos blockPos) {
      BlockPos blockPos2 = mc.player.getBlockPos();
      BlockPos blockPos3 = null;
      boolean local5690_4 = blockPos7.getX() == blockPos8.getX();
      HashMap<BlockPos, BlockPos> local5690_5 = new HashMap<>();
      ArrayDeque<BlockPos> local5690_6 = new ArrayDeque<>();
      local5690_5.put(blockPos2, blockPos2);
      local5690_6.add(blockPos2);

      while (!local5690_6.isEmpty() && blockPos3 == null) {
         BlockPos blockPos4 = local5690_6.pollFirst();
         if (local5690_4
            ? blockPos4.getX() == blockPos7.getX()
               && blockPos4.getZ() > Math.min(blockPos7.getZ(), blockPos8.getZ())
               && blockPos4.getZ() < Math.max(blockPos7.getZ(), blockPos8.getZ())
            : blockPos4.getZ() == blockPos7.getZ()
               && blockPos4.getX() > Math.min(blockPos7.getX(), blockPos8.getX())
               && blockPos4.getX() < Math.max(blockPos7.getX(), blockPos8.getX())) {
            blockPos3 = blockPos4;
         }

         for (Direction direction : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            BlockPos blockPos5 = blockPos4.offset(direction);
            if (!local5690_5.containsKey(blockPos5)
               && Math.abs(blockPos5.getX() - blockPos2.getX()) <= 40
               && Math.abs(blockPos5.getZ() - blockPos2.getZ()) <= 40
               && this.matchesCriteria4(blockPos5)
               && this.matchesCriteria4(blockPos5.up())
               && (!this.matchesCriteria4(blockPos5.down()) || !this.matchesCriteria4(blockPos5.down(2)))) {
               local5690_5.put(blockPos5, blockPos4);
               local5690_6.add(blockPos5);
            }
         }
      }

      this.arrayDeque.clear();
      if (blockPos3 != null) {
         for (BlockPos blockPos6 = blockPos3; !blockPos6.equals(blockPos2); blockPos6 = local5690_5.get(blockPos6)) {
            this.arrayDeque.addFirst(blockPos6);
         }

         int local5690_7 = (local5690_4 ? blockPos.getX() < blockPos3.getX() : blockPos.getZ() < blockPos3.getZ()) ? -1 : 1;
         this.arrayDeque.addLast(local5690_4 ? blockPos3.add(local5690_7, 0, 0) : blockPos3.add(0, 0, local5690_7));
         this.arrayDeque.addLast(local5690_4 ? blockPos3.add(local5690_7 * 2, 0, 0) : blockPos3.add(0, 0, local5690_7 * 2));
         mc.interactionManager.cancelBlockBreaking();
      }
   }

   public boolean matchesCriteria6(ItemStack itemStack) {
      for (Text text : itemStack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines()) {
         if (!text.getString().trim().isEmpty()
            && !AuctionHelperModule.getInstance().formatText2(text.getString()).matches(".*(опыт|содержит|ур|шт).*")) {
            return true;
         }
      }

      return false;
   }

   private enum TypeOption {
      SCAN_TITLE,
      WARP_TELEPORT,
      BARITONE_RIDE,
      STAND_READY,
      DIG_MINE,
      HOME_TELEPORT,
      DUMP_DIAMONDS,
      AH_BUY,
      XP_THROW,
      SPREAD_DIAMONDS;
   }
}
