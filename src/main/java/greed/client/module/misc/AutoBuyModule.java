package greed.client.module.misc;

import greed.client.util.AuctionItemCatalog;
import greed.client.ui.AutoBuySettingsScreen;
import greed.client.setting.BooleanSetting;
import greed.client.util.ClientChat;
import greed.client.event.ClientTickEvent;
import greed.client.ui.GreedAutoBuyScreen;
import greed.client.event.EventListener;
import greed.client.setting.KeybindSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.NumberSetting;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.util.InventoryUtils;
import greed.client.ui.panel.AutoBuyPanel;
import greed.client.storage.AutoBuyItem;
import greed.client.setting.SectionSetting;
import greed.client.util.Stopwatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

@ModuleInfo(name = "AutoBuy", description = "Скупает лоты с аукциона по своим ценам, меню открывается биндом.", category = ModuleCategory.MISC)
public final class AutoBuyModule extends Module {
   private static AutoBuyModule autoBuyModule;
   private static final Pattern pattern = Pattern.compile(".*выставлен на продажу за.*");
   private static final Pattern pattern2 = Pattern.compile(".*у вас купили.*");
   private static final Pattern pattern3 = Pattern.compile("^\\[☃] У Вас купили (.+) за \\$([\\d,]+) на /ah$");
   private static final Pattern pattern4 = Pattern.compile(".*вы успешно купили.*");
   private static final Pattern pattern5 = Pattern.compile(".*максимальная цена на этот товар.*");
   private static final Pattern pattern6 = Pattern.compile(".*вы убрали предмет с продажи.*");
   private static final String[] strValArray = new String[]{
      "Игрок", "Барон", "Страж", "Герой", "Аспид", "Сквид", "Глава", "Элита", "Титан", "Принц", "Князь", "Герцог"
   };
   private static final int[] intValArray = new int[]{3, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 21};
   public KeybindSetting keybindSetting;
   public BooleanSetting autoBuy;
   public BooleanSetting autoParser;
   public NumberSetting buyPercent;
   public NumberSetting sellPercent;
   public NumberSetting relistDiscount;
   public NumberSetting reparse;
   private boolean flag;
   private AutoBuyModule.BuyMode nested0408 = AutoBuyModule.BuyMode.IDLE;
   private final Stopwatch timer = new Stopwatch();
   private final Stopwatch timer2 = new Stopwatch();
   private final Stopwatch timer3 = new Stopwatch();
   private final Stopwatch timer4 = new Stopwatch();
   private final Stopwatch timer5 = new Stopwatch();
   private final Stopwatch timer6 = new Stopwatch();
   private final Stopwatch timer7 = new Stopwatch();
   private final ArrayList<String> list = new ArrayList<>();
   private final ArrayList<String> list2 = new ArrayList<>();
   private final ArrayList<String> list3 = new ArrayList<>();
   private final ArrayList<Integer> list4 = new ArrayList<>();
   private final HashMap<String, Long> map2 = new HashMap<>();
   private final HashMap<String, Long> map3 = new HashMap<>();
   private ServerInfo serverInfo;
   private String strVal;
   private String strVal2 = "ah";
   private String strVal3 = "";
   private String strVal4 = "";
   private String strVal5 = "";
   private int intVal = 1;
   private boolean flag2;
   private boolean flag3;
   private int intVal2;
   private boolean flag4;
   private boolean flag5;
   private boolean flag6;
   private boolean flag7;
   private final HashSet<String> set = new HashSet<>();
   private int intVal3;
   private int intVal4;
   private int intVal5;
   private int intVal6 = intValArray[0];
   private final HashMap<String, Long> map4 = new HashMap<>();
   private final HashMap<String, Integer> map5 = new HashMap<>();
   private final Stopwatch timer8 = new Stopwatch();
   private int intVal7;
   private boolean flag8;
   private final HashSet<String> set2 = new HashSet<>();
   private final Stopwatch timer9 = new Stopwatch();
   private final Stopwatch timer10 = new Stopwatch();
   private final Stopwatch timer11 = new Stopwatch();
   private int intVal8;
   private int intVal9;
   private final HashSet<String> set3 = new HashSet<>();
   private int intVal10;
   private long timeMillis;
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (mc.player != null && mc.getWindow() != null) {
         if (this.keybindSetting.isBound()
            && this.keybindSetting.getBinding().isPressed(mc.getWindow().getHandle())
            && !this.flag
            && !(mc.currentScreen instanceof ChatScreen)
            && !(mc.currentScreen instanceof GreedAutoBuyScreen)) {
            AutoBuyPanel.getInstance().updateState();
            mc.setScreen(GreedAutoBuyScreen.instance());
         }

         this.flag = this.keybindSetting.isBound() && this.keybindSetting.getBinding().isPressed(mc.getWindow().getHandle());
         this.timer7.reset();
         if (mc.getCurrentServerEntry() != null) {
            this.serverInfo = mc.getCurrentServerEntry();
         }

         ScoreboardObjective scoreboardObjective = mc.world == null
            ? null
            : mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
         if (scoreboardObjective != null
            && scoreboardObjective.getDisplayName().getString().split("-").length > 1
            && !scoreboardObjective.getDisplayName().getString().split("-")[1].replaceAll("\\D", "").isEmpty()) {
            this.strVal = "an" + scoreboardObjective.getDisplayName().getString().split("-")[1].replaceAll("\\D", "");
         }

         label838:
         for (ScoreboardEntry scoreboardEntry : scoreboardObjective == null ? List.<ScoreboardEntry>of() : mc.world.getScoreboard().getScoreboardEntries(scoreboardObjective)) {
            String local2633_5 = AuctionHelperModule.getInstance()
               .formatText2(
                  Team.decorateName(mc.world.getScoreboard().getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name())
                     .getString()
                     .replaceAll("(?i)§[0-9a-fk-or]", "")
               );
            if (local2633_5.contains("ранг")) {
               for (int index = 0; index < strValArray.length; index++) {
                  if (local2633_5.contains(AuctionHelperModule.getInstance().formatText2(strValArray[index]))) {
                     this.intVal6 = intValArray[index];
                     break label838;
                  }
               }
               break;
            }
         }

         if (this.nested0408 == AutoBuyModule.BuyMode.IDLE
            && InventoryUtils.findOffhandSlot(itemStack -> AuctionItemCatalog.matchesCriteria3(itemStack.getItem())) != -1
            && (
               InventoryUtils.findEmptySlot(0, 35) == -1 && this.timer10.hasExceededMillis(8000L << this.intVal9)
                  || !this.list2.isEmpty() && this.computeInt2() > 20 && this.timer10.hasExceededMillis(60000L)
            )) {
            this.timer10.reset();
            this.timer.reset();
            this.nested0408 = AutoBuyModule.BuyMode.STASH;
         }

         if (this.nested0408 == AutoBuyModule.BuyMode.IDLE
            && !this.list2.isEmpty()
            && InventoryUtils.findEmptySlot(0, 35) == -1
            && this.timer11.hasExceededMillis(5000L << this.intVal9)) {
            this.intVal3 = this.list2.size();
            this.timer.reset();
            this.nested0408 = AutoBuyModule.BuyMode.SELL_PICK;
         }

         if (this.nested0408 == AutoBuyModule.BuyMode.IDLE) {
            for (int index2 = 0; index2 <= 35; index2++) {
               AutoBuyItem autoBuyItem = mc.player.getInventory().getStack(index2).isEmpty()
                  ? null
                  : AuctionItemCatalog.getAutoBuyItem3(mc.player.getInventory().getStack(index2));
               if (autoBuyItem != null
                  && AutoBuySettingsScreen.getInstance().map2.containsKey(autoBuyItem.name())
                  && !this.set.contains(autoBuyItem.name())
                  && !this.list2.contains(autoBuyItem.name())) {
                  this.list2.add(autoBuyItem.name());
               }
            }
         }

         if (this.nested0408 == AutoBuyModule.BuyMode.IDLE
            && this.intVal5 < this.intVal6
            && this.list2.size() >= this.intVal6 - this.intVal5
            && this.timer9.hasExceededMillis(2000L)
            && this.timer11.hasExceededMillis(5000L << this.intVal9)) {
            this.intVal3 = this.intVal6 - this.intVal5;
            this.timer.reset();
            this.nested0408 = AutoBuyModule.BuyMode.SELL_PICK;
         }

         if (this.nested0408 == AutoBuyModule.BuyMode.IDLE
            && this.autoBuy.isChecked()
            && !this.list2.isEmpty()
            && this.intVal5 >= this.intVal6
            && this.computeInt3() >= this.intVal5
            && this.timer9.hasExceededMillis(3000L)
            && this.timer8.hasExceededMillis(15000L)) {
            this.timer8.reset();
            this.timer.reset();
            this.nested0408 = AutoBuyModule.BuyMode.RELIST_SEARCH;
         }

         if (this.nested0408 == AutoBuyModule.BuyMode.IDLE && this.timer5.hasExceededMillis(60000L)) {
            this.timer5.reset();
            this.timer.reset();
            this.nested0408 = AutoBuyModule.BuyMode.RESELL;
         }

         if (this.autoParser.isChecked()
            && this.nested0408 == AutoBuyModule.BuyMode.IDLE
            && this.reparse.intValue() > 0
            && this.timer4.hasExceededMillis(this.reparse.intValue() * 60000L)) {
            this.list.addAll(AutoBuySettingsScreen.getInstance().map2.keySet());
            this.timer.reset();
            this.nested0408 = AutoBuyModule.BuyMode.SEND;
         }

         switch (this.nested0408) {
            case IDLE:
            default:
               {
break;
}
            case SEND:
               {
if (!this.timer.hasExceededMillis(this.computeLong2(300L, 200L, 400L))) {
                  return;
               }

               if (this.list.isEmpty()) {
                  this.timer4.reset();
                  if (this.autoBuy.isChecked() && (!this.flag2 || this.list2.isEmpty())) {
                     mc.player.networkHandler.sendChatCommand(this.strVal2);
                  }

                  if (this.flag2 && !this.list2.isEmpty()) {
                     this.intVal3 = this.list2.size();
                  }

                  this.timer.reset();
                  this.nested0408 = this.flag2 && !this.list2.isEmpty() ? AutoBuyModule.BuyMode.SELL_PICK : AutoBuyModule.BuyMode.IDLE;
                  return;
               }

               this.strVal4 = this.list.remove(0);
               mc.player.networkHandler.sendChatCommand("ah search " + this.strVal4);
               this.timer.reset();
               this.nested0408 = AutoBuyModule.BuyMode.WAIT_LOTS;
               break;
}
            case WAIT_LOTS:
               {
if (!this.checkState4()) {
                  if (!this.timer.hasExceededMillis(2500L)) {
                     return;
                  }

                  this.timer.reset();
                  this.nested0408 = AutoBuyModule.BuyMode.SEND;
                  return;
               }

               if (!this.timer.hasExceededMillis(this.computeLong2(450L, 350L, 600L))) {
                  return;
               }

               int local2633_3 = -1;

               for (int index3 = 0; index3 < mc.player.currentScreenHandler.slots.size() - 36 - 9; index3++) {
                  ItemStack itemStack2 = mc.player.currentScreenHandler.getSlot(index3).getStack();
                  if (!itemStack2.isEmpty()
                     && AuctionItemCatalog.getAutoBuyItem(this.strVal4) != null
                     && AuctionItemCatalog.matchesCriteria2(itemStack2, AuctionItemCatalog.getAutoBuyItem(this.strVal4))
                     && AuctionHelperModule.getInstance().computeInt(itemStack2) > 0
                     && (local2633_3 == -1 || AuctionHelperModule.getInstance().computeInt(itemStack2) / Math.max(1, itemStack2.getCount()) < local2633_3)) {
                     local2633_3 = AuctionHelperModule.getInstance().computeInt(itemStack2) / Math.max(1, itemStack2.getCount());
                  }
               }

               if (local2633_3 != -1) {
                  this.map2.put(this.strVal4, Math.max(1L, Math.round(local2633_3 * this.sellPercent.floatValue() / 100.0F)));
               }

               if (local2633_3 != -1) {
                  this.map4.remove(this.strVal4);
                  this.map5.remove(this.strVal4);
               }

               if (AutoBuySettingsScreen.getInstance().map2.get(this.strVal4) != null) {
                  AutoBuySettingsScreen.getInstance().map2.get(this.strVal4)
                     .nested03744
                     .setValue(Long.toString(Math.max(1L, Math.round(local2633_3 * this.buyPercent.floatValue() / 100.0F))));
                  AutoBuySettingsScreen.getInstance().map2.get(this.strVal4)
                     .nested03745
                     .setValue(Long.toString(Math.max(1L, Math.round(local2633_3 * this.sellPercent.floatValue() / 100.0F))));
               }

               this.timer.reset();
               this.nested0408 = AutoBuyModule.BuyMode.SEND;
               break;
}
            case SELL_PICK:
               {
if (this.list2.isEmpty() || this.intVal3 <= 0) {
                  this.flag2 = false;
                  this.list4.clear();
                  if (this.autoBuy.isChecked()) {
                     mc.player.networkHandler.sendChatCommand(this.strVal2);
                  }

                  this.timer.reset();
                  this.nested0408 = AutoBuyModule.BuyMode.IDLE;
                  return;
               }

               if (!this.timer.hasExceededMillis(250L)) {
                  return;
               }

               if (this.intVal8 > 3) {
                  this.intVal8 = 0;
                  this.intVal3 = 0;
                  this.flag2 = false;
                  this.list4.clear();
                  this.timer11.reset();
                  if (this.autoBuy.isChecked()) {
                     mc.player.networkHandler.sendChatCommand(this.strVal2);
                  }

                  this.timer.reset();
                  this.nested0408 = AutoBuyModule.BuyMode.IDLE;
                  return;
               }

               if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                  mc.player.closeHandledScreen();
                  this.timer.reset();
                  return;
               }

               if (AuctionItemCatalog.getAutoBuyItem(this.list2.get(0)) != null
                  && AuctionItemCatalog.matchesCriteria2(mc.player.getMainHandStack(), AuctionItemCatalog.getAutoBuyItem(this.list2.get(0)))
                  && mc.player.getMainHandStack().getCount()
                     == this.computeInt(this.list2.get(0), mc.player.getMainHandStack().getCount())) {
                  this.timer.reset();
                  this.nested0408 = AutoBuyModule.BuyMode.SELL_SEND;
                  return;
               }

               for (int index4 = 0; index4 < this.list2.size(); index4++) {
                  for (Slot slot : mc.player.currentScreenHandler.slots) {
                     if (slot.inventory == mc.player.getInventory()
                        && AuctionItemCatalog.getAutoBuyItem(this.list2.get(index4)) != null
                        && AuctionItemCatalog.matchesCriteria2(slot.getStack(), AuctionItemCatalog.getAutoBuyItem(this.list2.get(index4)))
                        && slot.getStack().getCount() == this.computeInt(this.list2.get(index4), slot.getStack().getCount())) {
                        this.list2.add(0, this.list2.remove(index4));
                        if (slot.getIndex() <= 8) {
                           InventoryUtils.setSelectedSlot(slot.getIndex(), false);
                        } else {
                           InventoryUtils.sendSlotClickPacket(slot.id, InventoryUtils.getSelectedSlot(), SlotActionType.SWAP, false);
                        }

                        this.timer.reset();
                        this.nested0408 = AutoBuyModule.BuyMode.SELL_SEND;
                        return;
                     }
                  }
               }

               for (int index5 = 0; index5 < this.list2.size(); index5++) {
                  for (Slot slot2 : mc.player.currentScreenHandler.slots) {
                     if (slot2.inventory == mc.player.getInventory()
                        && AuctionItemCatalog.getAutoBuyItem(this.list2.get(index5)) != null
                        && AuctionItemCatalog.matchesCriteria2(slot2.getStack(), AuctionItemCatalog.getAutoBuyItem(this.list2.get(index5)))
                        && slot2.getStack().getCount() > this.computeInt(this.list2.get(index5), slot2.getStack().getCount())) {
                        for (Slot slot3 : mc.player.currentScreenHandler.slots) {
                           if (slot3.inventory == mc.player.getInventory() && !slot3.hasStack()) {
                              InventoryUtils.sendSlotClickPacket(slot2.id, 0, SlotActionType.PICKUP, false);

                              for (int index6 = 0;
                                 index6 < this.computeInt(this.list2.get(index5), slot2.getStack().getCount());
                                 index6++
                              ) {
                                 InventoryUtils.sendSlotClickPacket(slot3.id, 1, SlotActionType.PICKUP, false);
                              }

                              InventoryUtils.sendSlotClickPacket(slot2.id, 0, SlotActionType.PICKUP, false);
                              this.timer.reset();
                              return;
                           }
                        }
                     }
                  }
               }

               if (!this.timer.hasExceededMillis(1500L)) {
                  return;
               }

               if (InventoryUtils.findOffhandSlot(itemStack3 -> AuctionItemCatalog.matchesCriteria3(itemStack3.getItem())) != -1
                  && this.timer10.hasExceededMillis(8000L)) {
                  this.timer10.reset();
                  this.timer.reset();
                  this.nested0408 = AutoBuyModule.BuyMode.STASH;
                  return;
               }

               this.intVal8++;
               this.list2
                  .removeIf(
                     local2619_1 -> (
                           AuctionItemCatalog.getAutoBuyItem(local2619_1) == null
                              || InventoryUtils.findOffhandSlot(
                                    itemStack4 -> AuctionItemCatalog.matchesCriteria2(itemStack4, AuctionItemCatalog.getAutoBuyItem(local2619_1))
                                 )
                                 == -1
                        )
                        && (
                           InventoryUtils.findOffhandSlot(itemStack5 -> AuctionItemCatalog.matchesCriteria3(itemStack5.getItem())) == -1
                              || this.set3.contains(local2619_1)
                        )
                  );
               this.intVal9 = Math.min(6, this.intVal9 + 1);
               this.intVal3 = 0;
               this.flag2 = false;
               this.list4.clear();
               this.timer11.reset();
               if (this.autoBuy.isChecked()) {
                  mc.player.networkHandler.sendChatCommand(this.strVal2);
               }

               this.timer.reset();
               this.nested0408 = AutoBuyModule.BuyMode.IDLE;
               break;
}
            case SELL_SEND:
               {
if (!this.timer.hasExceededMillis(200L)) {
                  return;
               }

               if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                  mc.player.closeHandledScreen();
                  this.timer.reset();
                  return;
               }

               if (this.list2.isEmpty() || AuctionItemCatalog.getAutoBuyItem3(mc.player.getMainHandStack()) == null) {
                  this.timer.reset();
                  this.nested0408 = AutoBuyModule.BuyMode.SELL_PICK;
                  return;
               }

               this.strVal5 = AuctionItemCatalog.getAutoBuyItem3(mc.player.getMainHandStack()).name();
               if ((
                     AutoBuySettingsScreen.getInstance().map2.get(this.strVal5) != null
                        ? AutoBuySettingsScreen.computeLong(AutoBuySettingsScreen.getInstance().map2.get(this.strVal5).nested03745.getValue())
                        : this.map2.getOrDefault(this.strVal5, 0L)
                  )
                  <= 0L) {
                  if (!this.list.contains(this.strVal5)) {
                     this.list.add(this.strVal5);
                  }

                  this.flag2 = true;
                  this.timer.reset();
                  this.nested0408 = AutoBuyModule.BuyMode.SEND;
                  return;
               }

               this.flag5 = false;
               this.flag6 = false;
               this.flag7 = false;
               this.intVal = mc.player.getMainHandStack().getCount();
               if (this.computeLong(this.strVal5) * this.intVal > 10000000L && this.intVal > 1 && this.checkState3()) {
                  this.list2.add(this.strVal5);
                  this.timer.reset();
                  return;
               }

               long local2633_3 = Math.max(1L, this.computeLong(this.strVal5) * mc.player.getMainHandStack().getCount());
               mc.player.networkHandler.sendChatCommand("ah sell " + local2633_3);
               this.timer.reset();
               this.nested0408 = AutoBuyModule.BuyMode.SELL_WAIT;
               break;
}
            case SELL_WAIT:
               {
if (this.flag5) {
                  this.flag5 = false;
                  this.intVal8 = 0;
                  this.intVal3--;
                  this.timer.reset();
                  this.nested0408 = AutoBuyModule.BuyMode.SELL_PICK;
                  return;
               }

               if (this.flag6) {
                  this.flag6 = false;
                  if (mc.player.getMainHandStack().getCount() > 1 && this.checkState3()) {
                     this.list2.add(this.strVal5);
                     this.flag7 = true;
                     this.timer.reset();
                     return;
                  }

                  this.set.add(this.strVal5);
                  this.list2.remove(this.strVal5);
                  this.intVal8++;
                  this.timer.reset();
                  this.nested0408 = AutoBuyModule.BuyMode.SELL_PICK;
                  return;
               }

               if (this.flag7) {
                  if (!this.timer.hasExceededMillis(450L)) {
                     return;
                  }

                  this.flag7 = false;
                  mc.player
                     .networkHandler
                     .sendChatCommand("ah sell " + Math.max(1L, this.computeLong(this.strVal5) * mc.player.getMainHandStack().getCount()));
                  this.timer.reset();
                  return;
               }

               if (!this.timer.hasExceededMillis(1500L)) {
                  return;
               }

               this.intVal8++;
               if (!this.list2.isEmpty()) {
                  this.list2.add(this.list2.remove(0));
               }

               this.timer.reset();
               this.nested0408 = AutoBuyModule.BuyMode.SELL_PICK;
               break;
}
            case FLIP_SEARCH:
               {
if (!this.timer.hasExceededMillis(250L)) {
                  return;
               }

               mc.player.networkHandler.sendChatCommand("ah " + mc.player.getName().getString());
               this.timer.reset();
               this.nested0408 = AutoBuyModule.BuyMode.FLIP_BUY;
               break;
}
            case FLIP_BUY:
               {
if (!this.checkState4()) {
                  if (!this.timer.hasExceededMillis(2500L)) {
                     return;
                  }

                  this.timer.reset();
                  this.nested0408 = AutoBuyModule.BuyMode.IDLE;
                  return;
               }

               if (!this.timer.hasExceededMillis(this.computeLong2(600L, 450L, 700L))) {
                  return;
               }

               ArrayList<Integer> local2633_3 = new ArrayList<>();

               for (int index7 = 0; index7 < mc.player.currentScreenHandler.slots.size() - 36 - 9; index7++) {
                  ItemStack itemStack6 = mc.player.currentScreenHandler.getSlot(index7).getStack();
                  if (!itemStack6.isEmpty()
                     && AuctionItemCatalog.getAutoBuyItem3(itemStack6) != null
                     && AuctionHelperModule.getInstance().computeInt(itemStack6) > 0
                     && !AuctionHelperModule.getInstance()
                        .formatText2(itemStack6.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                        .contains("не актуален")) {
                     local2633_3.add(index7);
                  }
               }

               local2633_3.sort(
                  Comparator.comparingInt(
                     local2621_0 -> AuctionHelperModule.getInstance().computeInt(mc.player.currentScreenHandler.getSlot(local2621_0).getStack())
                  )
               );

               for (int local2633_5 : local2633_3) {
                  if (this.intVal10 <= 0) {
                     break;
                  }

                  ItemStack itemStack7 = mc.player.currentScreenHandler.getSlot(local2633_5).getStack();
                  if (!this.list4.contains(local2633_5)
                     && (this.computeInt4() < 0 || AuctionHelperModule.getInstance().computeInt(itemStack7) <= this.computeInt4())) {
                     this.list4.add(local2633_5);
                     this.intVal10--;
                     this.list.add(AuctionItemCatalog.getAutoBuyItem3(itemStack7).name());
                     this.list2.add(AuctionItemCatalog.getAutoBuyItem3(itemStack7).name());
                     InventoryUtils.sendSlotClickPacket(local2633_5, 0, SlotActionType.QUICK_MOVE, true);
                     this.timer.reset();
                     if (this.intVal10 > 0) {
                        return;
                     }
                     break;
                  }
               }

               this.flag2 = true;
               this.timer.reset();
               this.nested0408 = this.list.isEmpty() ? AutoBuyModule.BuyMode.IDLE : AutoBuyModule.BuyMode.SEND;
               break;
}
            case RESELL:
               {
if (!this.timer.hasExceededMillis(250L)) {
                  return;
               }

               mc.player.networkHandler.sendChatCommand("ah resell");
               this.timer.reset();
               this.nested0408 = AutoBuyModule.BuyMode.RESUME_PAGE;
               break;
}
            case REJOIN_HUB:
               {
if (!this.timer.hasExceededMillis(400L)) {
                  return;
               }

               mc.player.networkHandler.sendChatCommand("hub");
               this.timer.reset();
               this.nested0408 = AutoBuyModule.BuyMode.REJOIN_ANARCHY;
               break;
}
            case REJOIN_ANARCHY:
               {
if (!this.timer.hasExceededMillis(1500L)) {
                  return;
               }

               if (this.strVal != null) {
                  mc.player.networkHandler.sendChatCommand(this.strVal);
               }

               this.timer.reset();
               this.nested0408 = AutoBuyModule.BuyMode.RESUME_PAGE;
               break;
}
            case RESUME_PAGE:
               {
if (!this.timer.hasExceededMillis(800L)) {
                  return;
               }

               if (this.autoBuy.isChecked()) {
                  mc.player.networkHandler.sendChatCommand(this.strVal2);
               }

               this.list4.clear();
               this.timer.reset();
               this.nested0408 = AutoBuyModule.BuyMode.IDLE;
               break;
}
            case STASH:
               {
if (!this.timer.hasExceededMillis(120L)) {
                  return;
               }

               if (!this.checkState()) {
                  if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                     mc.player.closeHandledScreen();
                     return;
                  }

                  for (Slot slot4 : mc.player.currentScreenHandler.slots) {
                     if (slot4.inventory == mc.player.getInventory() && AuctionItemCatalog.matchesCriteria3(slot4.getStack().getItem())) {
                        InventoryUtils.sendSlotClickPacket(slot4.id, 1, SlotActionType.PICKUP, false);
                        this.timer.reset();
                        return;
                     }
                  }

                  this.timer.reset();
                  this.nested0408 = AutoBuyModule.BuyMode.IDLE;
                  return;
               }

               for (Slot slot5 : mc.player.currentScreenHandler.slots) {
                  if (slot5.inventory != mc.player.getInventory()
                     && !this.list2.isEmpty()
                     && AuctionItemCatalog.getAutoBuyItem(this.list2.get(0)) != null
                     && AuctionItemCatalog.matchesCriteria2(slot5.getStack(), AuctionItemCatalog.getAutoBuyItem(this.list2.get(0)))) {
                     InventoryUtils.sendSlotClickPacket(slot5.id, 0, SlotActionType.QUICK_MOVE, false);
                     mc.player.closeHandledScreen();
                     this.timer.reset();
                     this.nested0408 = AutoBuyModule.BuyMode.SELL_PICK;
                     return;
                  }
               }

               for (Slot slot6 : mc.player.currentScreenHandler.slots) {
                  if (slot6.inventory == mc.player.getInventory()
                     && slot6.hasStack()
                     && this.computeInt2() > 20
                     && !this.timer10.hasExceededMillis(8000L)
                     && AuctionItemCatalog.getAutoBuyItem3(slot6.getStack()) != null) {
                     InventoryUtils.sendSlotClickPacket(slot6.id, 0, SlotActionType.QUICK_MOVE, false);
                     this.timer.reset();
                     return;
                  }
               }

               if (!this.list2.isEmpty()) {
                  this.set3.add(this.list2.get(0));
               }

               mc.player.closeHandledScreen();
               this.timer.reset();
               this.nested0408 = AutoBuyModule.BuyMode.SELL_PICK;
               break;
}
            case RELIST_SEARCH:
               {
if (!this.timer.hasExceededMillis(250L)) {
                  return;
               }

               if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                  mc.player.closeHandledScreen();
                  this.timer.reset();
                  return;
               }

               this.intVal7 = 0;
               this.flag8 = true;
               this.set2.clear();
               this.list4.clear();
               mc.player.networkHandler.sendChatCommand("ah " + mc.player.getName().getString());
               this.timer.reset();
               this.nested0408 = AutoBuyModule.BuyMode.RELIST_PULL;
               break;
}
            case RELIST_PULL:
               {
if (!this.checkState4()) {
                  if (!this.timer.hasExceededMillis(2500L)) {
                     return;
                  }

                  this.timer.reset();
                  this.nested0408 = this.intVal7 > 0 ? AutoBuyModule.BuyMode.SELL_PICK : AutoBuyModule.BuyMode.RESUME_PAGE;
                  this.intVal3 = this.intVal7;
                  return;
               }

               if (!this.flag8 && !this.timer.hasExceededMillis(1500L)) {
                  return;
               }

               if (!this.timer.hasExceededMillis(this.computeLong2(600L, 450L, 350L)) || System.currentTimeMillis() - this.timeMillis < 90L) {
                  return;
               }

               for (int index8 = 0; index8 < mc.player.currentScreenHandler.slots.size() - 36 - 9; index8++) {
                  ItemStack itemStack8 = mc.player.currentScreenHandler.getSlot(index8).getStack();
                  if (!itemStack8.isEmpty()
                     && AuctionItemCatalog.getAutoBuyItem3(itemStack8) != null
                     && this.intVal7 < this.intVal6
                     && InventoryUtils.findEmptySlot(0, 35) != -1) {
                     String local2633_5 = AuctionItemCatalog.getAutoBuyItem3(itemStack8).name();
                     if (this.set2.add(local2633_5)) {
                        long local2633_6 = AuctionHelperModule.getInstance().computeInt(itemStack8) <= 0
                           ? this.computeLong(local2633_5)
                           : AuctionHelperModule.getInstance().computeInt(itemStack8) / Math.max(1, itemStack8.getCount());
                        long local2633_8 = AutoBuySettingsScreen.getInstance().map2.get(local2633_5) == null
                           ? 1L
                           : Math.max(1L, AutoBuySettingsScreen.computeLong(AutoBuySettingsScreen.getInstance().map2.get(local2633_5).nested03744.getValue()));
                        this.map4
                           .put(
                              local2633_5,
                              this.map5.getOrDefault(local2633_5, 0) == 0
                                 ? local2633_6
                                 : Math.max(local2633_8, Math.round(local2633_6 * (1.0 - this.relistDiscount.floatValue() / 100.0)))
                           );
                        this.map5.put(local2633_5, this.map5.getOrDefault(local2633_5, 0) + 1);
                     }

                     this.list2.add(Math.min(this.intVal7, this.list2.size()), local2633_5);
                     this.intVal7++;
                     this.flag8 = false;
                     InventoryUtils.sendSlotClickPacket(index8, 0, SlotActionType.QUICK_MOVE, false);
                     this.timer.reset();
                     return;
                  }
               }

               this.timer8.reset();
               this.intVal3 = this.intVal7;
               this.list4.clear();
               this.timer.reset();
               this.nested0408 = this.intVal7 > 0 ? AutoBuyModule.BuyMode.SELL_PICK : AutoBuyModule.BuyMode.RESUME_PAGE;
}
         }

         if (this.nested0408 == AutoBuyModule.BuyMode.IDLE && this.autoBuy.isChecked()) {
            if (this.flag3) {
               this.flag3 = false;
               if (mc.currentScreen != null) {
                  mc.player.closeHandledScreen();
               }

               mc.player.networkHandler.sendChatCommand(this.strVal2);
               this.flag4 = false;
               this.list4.clear();
               this.timer3.reset();
            } else if (mc.player.currentScreenHandler == mc.player.playerScreenHandler
               || !AuctionHelperModule.getInstance().formatText2(this.strVal3).contains("подтверждение")
                  && !AuctionHelperModule.getInstance().formatText2(this.strVal3).contains("подозрительная цена")) {
               if (!this.checkState4()) {
                  if (!(mc.currentScreen instanceof ChatScreen)
                     && !(mc.currentScreen instanceof GreedAutoBuyScreen)
                     && this.timer3.hasExceededMillis(300L)) {
                     mc.player.networkHandler.sendChatCommand(this.strVal2);
                     this.list4.clear();
                     this.timer3.reset();
                  }
               } else {
                  if (this.flag4) {
                     if (!this.timer2.hasExceededMillis(400L)) {
                        return;
                     }

                     this.flag4 = false;
                  }

                  int local2633_3 = -1;
                  long local2633_4 = Long.MAX_VALUE;

                  for (int index9 = 0; index9 < mc.player.currentScreenHandler.slots.size() - 36 - 9; index9++) {
                     ItemStack itemStack9 = mc.player.currentScreenHandler.getSlot(index9).getStack();
                     AutoBuyItem autoBuyItem2 = !itemStack9.isEmpty() && !this.list4.contains(index9)
                        ? AuctionItemCatalog.getAutoBuyItem3(itemStack9)
                        : null;
                     if (autoBuyItem2 != null
                        && AutoBuySettingsScreen.getInstance().matchesCriteria(autoBuyItem2.name())
                        && !AuctionHelperModule.getInstance()
                           .formatText2(itemStack9.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                           .contains("не актуален")
                        && (
                           AutoBuySettingsScreen.computeLong(AutoBuySettingsScreen.getInstance().map2.get(autoBuyItem2.name()).nested03744.getValue()) <= 0L
                              || AuctionHelperModule.getInstance().computeInt(itemStack9) / Math.max(1, itemStack9.getCount())
                                 <= AutoBuySettingsScreen.computeLong(AutoBuySettingsScreen.getInstance().map2.get(autoBuyItem2.name()).nested03744.getValue())
                        )
                        && AuctionHelperModule.getInstance().computeInt(itemStack9) >= 0
                        && (this.computeInt4() < 0 || AuctionHelperModule.getInstance().computeInt(itemStack9) <= this.computeInt4())
                        && AuctionHelperModule.getInstance().computeInt(itemStack9) < local2633_4) {
                        local2633_3 = index9;
                        local2633_4 = AuctionHelperModule.getInstance().computeInt(itemStack9);
                     }
                  }

                  if (local2633_3 != -1 && this.computeInt3() > this.intVal5) {
                     if (System.currentTimeMillis() - this.timeMillis >= 90L) {
                        this.list4.add(local2633_3);
                        InventoryUtils.sendSlotClickPacket(local2633_3, 0, SlotActionType.QUICK_MOVE, true);
                        this.flag4 = true;
                        this.timer2.reset();
                        this.timer3.reset();
                     }
                  } else if (this.timer3.hasExceededMillis(400L)) {
                     InventoryUtils.sendSlotClickPacket(mc.player.currentScreenHandler.slots.size() - 36 - 5, 0, SlotActionType.QUICK_MOVE, false);
                     this.list4.clear();
                     this.timer3.reset();
                  }
               }
            } else if (mc.player
               .currentScreenHandler
               .slots
               .stream()
               .anyMatch(
                  slot7 -> slot7.hasStack()
                     && AuctionHelperModule.getInstance()
                        .formatText2(slot7.getStack().getName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                        .contains("не актуален")
               )) {
               mc.player.closeHandledScreen();
               mc.player.networkHandler.sendChatCommand(this.strVal2);
               this.flag4 = false;
               this.timer3.reset();
            } else {
               InventoryUtils.sendSlotClickPacket(0, 0, SlotActionType.QUICK_MOVE, false);
               this.timer3.reset();
            }
         }
      }
   };
   private final EventListener<ClientTickEvent> clientTickListener = clientTickEvent -> {
      if (clientTickEvent.isPre() && (mc.player == null || mc.world == null)) {
         this.timer.reset();
         if (this.serverInfo != null
            && (
               mc.currentScreen instanceof DisconnectedScreen
                  || mc.currentScreen instanceof TitleScreen
                  || mc.currentScreen instanceof MultiplayerScreen
            )
            && this.timer7.hasExceededMillis(5000L)) {
            this.timer7.reset();
            this.nested0408 = AutoBuyModule.BuyMode.REJOIN_ANARCHY;
            ConnectScreen.connect(
               new MultiplayerScreen(new TitleScreen()), mc, ServerAddress.parse(this.serverInfo.address), this.serverInfo, false, null
            );
         }
      }
   };
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (packetEvent.isOutgoing()
         && this.nested0408 == AutoBuyModule.BuyMode.IDLE
         && packetEvent.getPacket() instanceof CommandExecutionC2SPacket commandExecutionC2SPacket
         && (commandExecutionC2SPacket.command().equals("ah") || commandExecutionC2SPacket.command().startsWith("ah "))
         && !commandExecutionC2SPacket.command().startsWith("ah sell")) {
         this.strVal2 = commandExecutionC2SPacket.command();
      }

      if (packetEvent.isIncoming()
         && (packetEvent.getPacket() instanceof InventoryS2CPacket || packetEvent.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket)) {
         this.timeMillis = System.currentTimeMillis();
      }

      if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof OpenScreenS2CPacket openScreenS2CPacket) {
         this.strVal3 = openScreenS2CPacket.getName().getString();
      }

      if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof CloseScreenS2CPacket) {
         this.strVal3 = "";
      }

      if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
         if (pattern4.matcher(AuctionHelperModule.getInstance().formatText2(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))).matches()) {
            this.flag4 = false;
            if (this.nested0408 == AutoBuyModule.BuyMode.FLIP_BUY) {
               this.intVal5 = Math.max(0, this.intVal5 - 1);
            }

            for (AutoBuyItem autoBuyItem3 : AuctionItemCatalog.getArrayList2()) {
               if (AuctionHelperModule.getInstance()
                  .formatText2(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                  .contains(AuctionHelperModule.getInstance().formatText2(autoBuyItem3.name()))) {
                  this.list2.add(autoBuyItem3.name());
                  this.set3.remove(autoBuyItem3.name());
                  break;
               }
            }

            this.intVal2++;
         } else if (pattern6.matcher(AuctionHelperModule.getInstance().formatText2(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", "")))
            .matches()) {
            this.intVal5 = Math.max(0, this.intVal5 - 1);
            this.flag8 = true;
         } else if (pattern2.matcher(AuctionHelperModule.getInstance().formatText2(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", "")))
            .matches()) {
            this.intVal4++;
            this.intVal5 = Math.max(0, this.intVal5 - 1);
            this.intVal9 = 0;
            this.timer9.reset();
            this.timer8.reset();
            String local2641_3 = gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim();
            if (pattern3.matcher(local2641_3).matches()) {
               this.map4.remove(pattern3.matcher(local2641_3).replaceAll("$1").replaceAll("\\s+[xх]\\d+$", "").trim());
               this.map5.remove(pattern3.matcher(local2641_3).replaceAll("$1").replaceAll("\\s+[xх]\\d+$", "").trim());
               mc.execute(
                  () -> AutoBuySettingsScreen.getInstance()
                     .processData3(
                        pattern3.matcher(local2641_3).replaceAll("$1"),
                        AutoBuySettingsScreen.computeLong(pattern3.matcher(local2641_3).replaceAll("$2").replace(",", ""))
                     )
               );
            }
         } else if (pattern5.matcher(AuctionHelperModule.getInstance().formatText2(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", "")))
            .matches()) {
            if (this.nested0408 == AutoBuyModule.BuyMode.SELL_WAIT) {
               this.flag6 = true;
            }
         } else if (!AuctionHelperModule.getInstance()
            .formatText2(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
            .contains("не так быстро")) {
            if (AuctionHelperModule.getInstance()
               .formatText2(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
               .contains("этот товар уже купили")) {
               this.flag3 = true;
            } else if (pattern.matcher(AuctionHelperModule.getInstance().formatText2(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", "")))
               .matches()) {
               this.flag5 = true;
               this.intVal9 = 0;
               if (this.nested0408 == AutoBuyModule.BuyMode.SELL_WAIT) {
                  this.intVal5 = Math.min(this.intVal6, this.intVal5 + 1);
               }

               if (!this.list2.remove(this.strVal5) && !this.list2.isEmpty()) {
                  this.list2.remove(0);
               }

               if (AuctionItemCatalog.getAutoBuyItem(this.strVal5) != null
                  && InventoryUtils.findOffhandSlot(itemStack10 -> AuctionItemCatalog.matchesCriteria2(itemStack10, AuctionItemCatalog.getAutoBuyItem(this.strVal5)))
                     == -1
                  && InventoryUtils.findOffhandSlot(itemStack11 -> AuctionItemCatalog.matchesCriteria3(itemStack11.getItem())) == -1) {
                  this.list2.removeIf(local2607_1 -> local2607_1.equals(this.strVal5));
               }
            } else if (!gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim().equalsIgnoreCase("Данная команда недоступна в режиме AFK")
               && !gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim().equalsIgnoreCase("Недопустимо нажимать в режиме AFK")) {
               if (AuctionHelperModule.getInstance()
                  .formatText2(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                  .contains("освободите хранилище или арендуйте больше слотов")) {
                  this.intVal5 = this.intVal6;
               }

               if (AuctionHelperModule.getInstance()
                     .formatText2(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                     .contains("освободите хранилище или арендуйте больше слотов")
                  && InventoryUtils.findEmptySlot(0, 35) != -1) {
                  if (this.nested0408 == AutoBuyModule.BuyMode.IDLE
                     || this.nested0408 == AutoBuyModule.BuyMode.SELL_PICK
                     || this.nested0408 == AutoBuyModule.BuyMode.SELL_SEND
                     || this.nested0408 == AutoBuyModule.BuyMode.SELL_WAIT) {
                     this.intVal10 = 3;
                     this.list4.clear();
                     this.timer.reset();
                     this.nested0408 = AutoBuyModule.BuyMode.FLIP_SEARCH;
                  }
               }
            } else {
               this.nested0408 = AutoBuyModule.BuyMode.REJOIN_HUB;
               this.timer.reset();
            }
         }
      }
   };

   private boolean checkState() {
      return mc.player.currentScreenHandler != mc.player.playerScreenHandler && !AuctionHelperModule.getInstance().matchesCriteria3(this.strVal3);
   }

   @Override
   protected void onDisable() {
      this.autoBuy.setValue(false);
      this.autoParser.setValue(false);
      this.list.clear();
      this.list3.clear();
      this.list4.clear();
      this.flag2 = false;
      this.intVal2 = 0;
      this.intVal3 = 0;
      this.intVal4 = 0;
      this.intVal5 = 0;
      this.flag4 = false;
      this.intVal10 = 0;
      this.flag6 = false;
      this.flag7 = false;
      this.set.clear();
      this.map4.clear();
      this.map5.clear();
      this.intVal7 = 0;
      this.flag8 = false;
      this.set2.clear();
      this.intVal9 = 0;
      this.intVal8 = 0;
      this.set3.clear();
      this.intVal6 = intValArray[0];
      this.nested0408 = AutoBuyModule.BuyMode.IDLE;
   }

   private boolean matchesCriteria(String local2612_1) {
      return this.list3.contains(local2612_1)
         || AuctionItemCatalog.getAutoBuyItem(local2612_1) != null && AuctionItemCatalog.matchesCriteria4(AuctionItemCatalog.getAutoBuyItem(local2612_1));
   }

   private long computeLong(String local2613_1) {
      if (this.map4.getOrDefault(local2613_1, 0L) > 0L) {
         return this.map4.get(local2613_1);
      } else {
         return AutoBuySettingsScreen.getInstance().map2.get(local2613_1) != null
            ? AutoBuySettingsScreen.computeLong(AutoBuySettingsScreen.getInstance().map2.get(local2613_1).nested03745.getValue())
            : this.map2.getOrDefault(local2613_1, 0L);
      }
   }

   private long computeLong2(long local2615_1, long local2615_3, long local2615_5) {
      return local2615_5;
   }

   public boolean checkState2() {
      return this.autoBuy.isChecked();
   }

   public static AutoBuyModule getInstance() {
      if (autoBuyModule == null) {
         autoBuyModule = new AutoBuyModule();
      }

      return autoBuyModule;
   }

   private int computeInt(String local2624_1, int local2624_2) {
      return AuctionItemCatalog.getAutoBuyItem(local2624_1) != null && AuctionItemCatalog.matchesCriteria4(AuctionItemCatalog.getAutoBuyItem(local2624_1)) ? 1 : local2624_2;
   }

   private boolean checkState3() {
      Slot slot = null;
      Slot slot2 = null;

      for (Slot slot3 : mc.player.currentScreenHandler.slots) {
         if (slot3.inventory == mc.player.getInventory() && slot3.getIndex() <= 35) {
            if (slot3.getIndex() == InventoryUtils.getSelectedSlot()) {
               slot = slot3;
            } else if (slot2 == null && !slot3.hasStack()) {
               slot2 = slot3;
            }
         }
      }

      if (slot != null && slot2 != null) {
         InventoryUtils.sendSlotClickPacket(slot.id, 1, SlotActionType.PICKUP, false);
         InventoryUtils.sendSlotClickPacket(slot2.id, 0, SlotActionType.PICKUP, false);
         return true;
      } else {
         return false;
      }
   }

   private int computeInt2() {
      int local2628_1 = 0;

      for (int index = 0; index <= 35; index++) {
         if (!mc.player.getInventory().getStack(index).isEmpty()) {
            local2628_1++;
         }
      }

      return local2628_1;
   }

   public void updateState() {
      this.autoParser.setValue(!this.autoParser.isChecked());
      this.list.clear();
      this.nested0408 = AutoBuyModule.BuyMode.IDLE;
      if (!this.autoParser.isChecked()) {
         ClientChat.processData8("Автопарсер остановлен.");
      } else if (AutoBuySettingsScreen.getInstance().map2.isEmpty()) {
         ClientChat.processData8("В меню AutoBuy не выбрано ни одного предмета.");
      } else {
         this.list.addAll(AutoBuySettingsScreen.getInstance().map2.keySet());
         this.timer.reset();
         this.nested0408 = AutoBuyModule.BuyMode.SEND;
      }
   }

   private boolean checkState4() {
      return mc.player.currentScreenHandler != mc.player.playerScreenHandler && AuctionHelperModule.getInstance().matchesCriteria3(this.strVal3);
   }

   public void updateState2() {
      this.autoBuy.setValue(!this.autoBuy.isChecked());
      this.list4.clear();
      ClientChat.processData8(
         this.autoBuy.isChecked()
            ? "Автобай запущен, предметов в списке: &7" + AutoBuySettingsScreen.getInstance().map2.size() + "&r, лотов по донату: &7" + this.intVal6
            : "Автобай остановлен."
      );
   }

   private int computeInt3() {
      return 36 - this.computeInt2();
   }

   private int computeInt4() {
      return mc.world != null && mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) != null
         ? mc.world
            .getScoreboard()
            .getTeams()
            .stream()
            .filter(team -> team.getName().startsWith("TAB-Sidebar-"))
            .map(team2 -> team2.getPrefix().getString().toLowerCase(Locale.ROOT))
            .filter(local2623_0 -> local2623_0.contains("монеты") || local2623_0.contains("$"))
            .map(local2640_0 -> local2640_0.replaceAll("\\D", ""))
            .filter(local2609_0 -> !local2609_0.isEmpty() && local2609_0.length() <= 9)
            .map(Integer::parseInt)
            .findFirst()
            .orElse(-1)
         : -1;
   }

   public boolean checkState5() {
      return this.autoParser.isChecked();
   }

   private AutoBuyModule() {
      this.addSetting(new SectionSetting("Скупка"));
      this.keybindSetting = this.addSetting(new KeybindSetting("Открыть меню", "Клавиша, которая открывает окно AutoBuy."));
      this.autoBuy = this.addSetting(new BooleanSetting("Автобай", "Скупать выбранные предметы на открытой странице аукциона.", false));
      this.autoParser = this.addSetting(new BooleanSetting("Автопарсер", "Проходить выбранные предметы и подставлять им цены.", false));
      this.relistDiscount = this.addSetting(
         new NumberSetting(
               "Скидка перевыставления", "На сколько процентов дешеветь помеченному лоту за каждый круг перевыставления, ниже цены покупки не опускается."
            )
            .withIntegerRange(1, 50)
            .withSuffix("%")
            .setValue(5.0F)
      );
      this.reparse = this.addSetting(
         new NumberSetting("Перепарс", "Через сколько минут автопарсер заново проходит все предметы, 0 значит один проход.")
            .withIntegerRange(0, 60)
            .withSuffix(" мин")
            .setValue(10.0F)
      );
      this.buyPercent = new NumberSetting("Процент покупки", "Какой процент от самого дешёвого лота автопарсер ставит в лимит покупки.")
         .withIntegerRange(10, 200)
         .withSuffix("%")
         .setValue(95.0F);
      this.sellPercent = new NumberSetting("Процент продажи", "Какой процент от самого дешёвого лота автопарсер ставит в цену продажи.")
         .withIntegerRange(10, 400)
         .withSuffix("%")
         .setValue(115.0F);
   }

   public static final class PriceSlider extends SliderWidget {
      private final String strVal6;
      private final NumberSetting numberSetting;

      @Override
      protected void updateMessage() {
         this.setMessage(Text.literal(this.strVal6 + ": " + this.numberSetting.intValue() + "%"));
      }

      @Override
      protected void applyValue() {
         this.numberSetting.setFraction((float)this.value);
      }

      public PriceSlider(int local2646_1, int local2646_2, int local2646_3, String local2646_4, NumberSetting initialNumberSetting) {
         super(local2646_1, local2646_2, local2646_3, 20, Text.literal(local2646_4 + ": " + initialNumberSetting.intValue() + "%"), initialNumberSetting.getFraction());
         this.strVal6 = local2646_4;
         this.numberSetting = initialNumberSetting;
      }
   }

   private enum BuyMode {
      IDLE,
      SEND,
      WAIT_LOTS,
      SELL_PICK,
      SELL_SEND,
      SELL_WAIT,
      FLIP_SEARCH,
      FLIP_BUY,
      RESELL,
      REJOIN_HUB,
      REJOIN_ANARCHY,
      RESUME_PAGE,
      STASH,
      RELIST_SEARCH,
      RELIST_PULL;
   }
}
