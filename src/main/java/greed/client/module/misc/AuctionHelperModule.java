package greed.client.module.misc;

import greed.client.setting.BooleanSetting;
import greed.client.event.ContainerRenderEvent;
import greed.client.event.EventListener;
import greed.client.setting.KeybindSetting;
import greed.client.setting.ModeSetting;
import greed.client.module.Module;
import greed.client.module.ModuleCategory;
import greed.client.module.ModuleInfo;
import greed.client.setting.MultiChoiceSetting;
import greed.client.setting.NumberSetting;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;
import greed.client.setting.NumericTextSetting;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

@ModuleInfo(
   name = "AuctionHelper",
   description = "Подсвечивает самые выгодные лоты на аукционе, фильтрует их по зачарованиям и ищет предмет из руки по бинду.",
   category = ModuleCategory.MISC
)
public final class AuctionHelperModule extends Module {
   private static AuctionHelperModule auctionHelperModule;
   public BooleanSetting calculator;
   public NumberSetting durability;
   public BooleanSetting checkBalance;
   public BooleanSetting onlyEnchanted;
   public BooleanSetting countFilter;
   public ModeSetting modeSetting;
   public NumberSetting stackSize;
   public BooleanSetting itemFilters;
   public MultiChoiceSetting armorFilter;
   public MultiChoiceSetting elytraFilter;
   public MultiChoiceSetting swordsFilter;
   public MultiChoiceSetting pickaxesFilter;
   public MultiChoiceSetting axesFilter;
   public MultiChoiceSetting shovelsFilter;
   public MultiChoiceSetting exceptionsFilter;
   public NumericTextSetting numericTextSetting;
   public KeybindSetting keybindSetting;
   private final Pattern pattern = Pattern.compile("(\\d+)\\*(\\d+)");
   private boolean flag;
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (packetEvent.isOutgoing() && this.calculator.isChecked() && packetEvent.getPacket() instanceof CommandExecutionC2SPacket commandExecutionC2SPacket) {
         String local3610_3 = this.formatText(commandExecutionC2SPacket.command());
         if (!local3610_3.equals(commandExecutionC2SPacket.command())) {
            packetEvent.setPacket(new CommandExecutionC2SPacket(local3610_3));
         }
      }
   };
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      boolean local3600_2 = this.keybindSetting.getBinding().isPressed(mc.getWindow().getHandle());
      if (local3600_2 && !this.flag && mc.currentScreen == null) {
         this.updateState();
      }

      this.flag = local3600_2;
   };
   private final EventListener<ContainerRenderEvent> containerRenderListener = containerRenderEvent -> {
      if (mc.player != null
         && mc.world != null
         && containerRenderEvent.getHandledScreen() instanceof GenericContainerScreen genericContainerScreen
         && this.matchesCriteria3(genericContainerScreen.getTitle().getString())) {
         Optional<Integer> local3617_3 = this.getOptional();
         List<AuctionHelperModule.AuctionItemEntry> local3617_4 = genericContainerScreen.getScreenHandler()
            .slots
            .stream()
            .filter(newSlot -> newSlot.hasStack() && this.matchesCriteria(newSlot.getStack()))
            .map(newSlot2 -> new AuctionHelperModule.AuctionItemEntry(this.computeInt(newSlot2.getStack()), newSlot2))
            .filter(
               local3619_2 -> local3619_2.price() >= 0 && (!this.checkBalance.isChecked() || local3617_3.isEmpty() || local3619_2.price() <= local3617_3.get())
            )
            .toList();
         this.renderElement(
            containerRenderEvent.getDrawContext(), local3617_4.stream().min(Comparator.comparingInt(AuctionHelperModule.AuctionItemEntry::price)).orElse(null), -16711936
         );
         this.renderElement(
            containerRenderEvent.getDrawContext(),
            local3617_4.stream()
               .filter(local3611_0 -> local3611_0.slot().getStack().getCount() > 1)
               .min(Comparator.comparingDouble(local3618_0 -> (double)local3618_0.price() / local3618_0.slot().getStack().getCount()))
               .orElse(null),
            -16711681
         );
      }
   };

   private boolean matchesCriteria(ItemStack itemStack) {
      if (itemStack.isOf(Items.GRAY_DYE) && itemStack.getName().getString().contains("Товар не актуален")) {
         return false;
      }

      if (itemStack.isDamageable()
         && (float)(itemStack.getMaxDamage() - itemStack.getDamage()) / itemStack.getMaxDamage() < this.durability.floatValue() / 100.0F) {
         return false;
      }

      if (this.onlyEnchanted.isChecked() && itemStack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT).isEmpty()) {
         return false;
      }

      if (this.countFilter.isChecked()) {
         if (this.modeSetting.isSelected("Точно") && itemStack.getCount() != this.stackSize.intValue()) {
            return false;
         }

         if (this.modeSetting.isSelected("Минимум") && itemStack.getCount() < this.stackSize.intValue()) {
            return false;
         }
      }

      if (!this.itemFilters.isChecked()) {
         return true;
      }

      Item item = itemStack.getItem();
      if (this.exceptionsFilter.matchesOption("Стрелы") && item instanceof ArrowItem) {
         return false;
      }

      if (this.exceptionsFilter.matchesOption("Пузырьки") && itemStack.isOf(Items.GLASS_BOTTLE)) {
         return false;
      }

      if (this.exceptionsFilter.matchesOption("Дыхание дракона") && itemStack.isOf(Items.DRAGON_BREATH)) {
         return false;
      }

      if (this.matchesCriteria2(itemStack)) {
         if (this.exceptionsFilter.matchesOption("Броня")) {
            return false;
         }

         if (this.armorFilter.matchesOption("Без шипов") && this.computeInt3(itemStack, "minecraft:thorns") != -1) {
            return false;
         }

         if (this.armorFilter.matchesOption("Защита V") && this.computeInt3(itemStack, "minecraft:protection") < 5) {
            return false;
         }

         if (this.armorFilter.matchesOption("Прочность V") && this.computeInt3(itemStack, "minecraft:unbreaking") < 5) {
            return false;
         }

         if (this.armorFilter.matchesOption("Починка") && this.computeInt3(itemStack, "minecraft:mending") == -1) {
            return false;
         }

         if (this.armorFilter.matchesOption("Починка II") && this.computeInt3(itemStack, "minecraft:mending") < 2) {
            return false;
         }
      }

      if (itemStack.isOf(Items.ELYTRA)) {
         if (this.exceptionsFilter.matchesOption("Элитры")) {
            return false;
         }

         if (this.elytraFilter.matchesOption("Прочность V") && this.computeInt3(itemStack, "minecraft:unbreaking") < 5) {
            return false;
         }

         if (this.elytraFilter.matchesOption("Починка") && this.computeInt3(itemStack, "minecraft:mending") == -1) {
            return false;
         }
      }

      if (this.exceptionsFilter.matchesOption("Тотемы") && itemStack.isOf(Items.TOTEM_OF_UNDYING)) {
         return false;
      }

      if (this.exceptionsFilter.matchesOption("Головы") && itemStack.isOf(Items.PLAYER_HEAD)) {
         return false;
      }

      if (this.exceptionsFilter.matchesOption("Трезубцы") && item instanceof TridentItem) {
         return false;
      }

      if (this.exceptionsFilter.matchesOption("Луки") && item instanceof BowItem) {
         return false;
      }

      if (this.exceptionsFilter.matchesOption("Арбалеты") && item instanceof CrossbowItem) {
         return false;
      }

      if (this.exceptionsFilter.matchesOption("Огниво") && item instanceof FlintAndSteelItem) {
         return false;
      }

      if (this.exceptionsFilter.matchesOption("Конская броня")
         && "minecraft".equals(Registries.ITEM.getId(item).getNamespace())
         && Registries.ITEM.getId(item).getPath().endsWith("_horse_armor")) {
         return false;
      }

      if (itemStack.isIn(ItemTags.SWORDS)) {
         if (this.exceptionsFilter.matchesOption("Мечи")) {
            return false;
         }

         if (this.swordsFilter.matchesOption("С отдачей") && this.computeInt3(itemStack, "minecraft:knockback") == -1) {
            return false;
         }

         if (this.swordsFilter.matchesOption("Без отдачи") && this.computeInt3(itemStack, "minecraft:knockback") != -1) {
            return false;
         }

         if (this.swordsFilter.matchesOption("Острота") && this.computeInt3(itemStack, "minecraft:sharpness") == -1) {
            return false;
         }

         if (this.swordsFilter.matchesOption("Прочность") && this.computeInt3(itemStack, "minecraft:unbreaking") == -1) {
            return false;
         }

         if (this.swordsFilter.matchesOption("Яд") && this.computeInt2(itemStack, "poison") == -1) {
            return false;
         }

         if (this.swordsFilter.matchesOption("Обнаружение") && this.computeInt2(itemStack, "detection") == -1) {
            return false;
         }

         if (this.swordsFilter.matchesOption("Окисление") && this.computeInt2(itemStack, "oxidation") == -1) {
            return false;
         }

         if (this.swordsFilter.matchesOption("Вампиризм") && this.computeInt2(itemStack, "vampirism") == -1) {
            return false;
         }
      }

      if (itemStack.isIn(ItemTags.PICKAXES)) {
         if (this.exceptionsFilter.matchesOption("Кирки")) {
            return false;
         }

         if (this.pickaxesFilter.matchesOption("Эффективность") && this.computeInt3(itemStack, "minecraft:efficiency") == -1) {
            return false;
         }

         if (this.pickaxesFilter.matchesOption("Починка") && this.computeInt3(itemStack, "minecraft:mending") == -1) {
            return false;
         }

         if (this.pickaxesFilter.matchesOption("Удача") && this.computeInt3(itemStack, "minecraft:fortune") == -1) {
            return false;
         }

         if (this.pickaxesFilter.matchesOption("Прочность") && this.computeInt3(itemStack, "minecraft:unbreaking") == -1) {
            return false;
         }

         if (this.pickaxesFilter.matchesOption("Шёлковое касание") && this.computeInt3(itemStack, "minecraft:silk_touch") == -1) {
            return false;
         }

         if (this.pickaxesFilter.matchesOption("Переплавка") && this.computeInt2(itemStack, "smelting") == -1) {
            return false;
         }

         if (this.pickaxesFilter.matchesOption("Магнит") && this.computeInt2(itemStack, "magnet") == -1) {
            return false;
         }

         if (this.pickaxesFilter.matchesOption("Бульдозер") && this.computeInt2(itemStack, "buldozing") == -1 && this.computeInt2(itemStack, "megabuldozing") == -1) {
            return false;
         }

         if (this.pickaxesFilter.matchesOption("Без тяжести") && this.computeInt2(itemStack, "heavy") != -1) {
            return false;
         }
      }

      if (itemStack.isIn(ItemTags.AXES)) {
         if (this.exceptionsFilter.matchesOption("Топоры")) {
            return false;
         }

         if (this.axesFilter.matchesOption("Эффективность") && this.computeInt3(itemStack, "minecraft:efficiency") == -1) {
            return false;
         }

         if (this.axesFilter.matchesOption("Починка") && this.computeInt3(itemStack, "minecraft:mending") == -1) {
            return false;
         }

         if (this.axesFilter.matchesOption("Удача") && this.computeInt3(itemStack, "minecraft:fortune") == -1) {
            return false;
         }

         if (this.axesFilter.matchesOption("Прочность") && this.computeInt3(itemStack, "minecraft:unbreaking") == -1) {
            return false;
         }

         if (this.axesFilter.matchesOption("Магнит") && this.computeInt2(itemStack, "magnet") == -1) {
            return false;
         }

         if (this.axesFilter.matchesOption("Лесоруб") && this.computeInt2(itemStack, "lumberjack") == -1) {
            return false;
         }

         if (this.axesFilter.matchesOption("Пингер") && this.computeInt2(itemStack, "pinger") == -1) {
            return false;
         }

         if (this.axesFilter.matchesOption("Бульдозер") && this.computeInt2(itemStack, "buldozing") == -1 && this.computeInt2(itemStack, "megabuldozing") == -1) {
            return false;
         }
      }

      if (itemStack.isIn(ItemTags.SHOVELS)) {
         if (this.exceptionsFilter.matchesOption("Лопаты")) {
            return false;
         }

         if (this.shovelsFilter.matchesOption("Эффективность") && this.computeInt3(itemStack, "minecraft:efficiency") == -1) {
            return false;
         }

         if (this.shovelsFilter.matchesOption("Починка") && this.computeInt3(itemStack, "minecraft:mending") == -1) {
            return false;
         }

         if (this.shovelsFilter.matchesOption("Удача") && this.computeInt3(itemStack, "minecraft:fortune") == -1) {
            return false;
         }

         if (this.shovelsFilter.matchesOption("Прочность") && this.computeInt3(itemStack, "minecraft:unbreaking") == -1) {
            return false;
         }

         if (this.shovelsFilter.matchesOption("Магнит") && this.computeInt2(itemStack, "magnet") == -1) {
            return false;
         }

         if (this.shovelsFilter.matchesOption("Бульдозер") && this.computeInt2(itemStack, "buldozing") == -1 && this.computeInt2(itemStack, "megabuldozing") == -1) {
            return false;
         }
      }

      return !itemStack.isIn(ItemTags.HOES) || !this.exceptionsFilter.matchesOption("Мотыги");
   }

   private Optional<Integer> getOptional() {
      Scoreboard scoreboard = mc.world.getScoreboard();
      ScoreboardObjective objectiveForSlot = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      return objectiveForSlot == null
         ? Optional.empty()
         : scoreboard.getScoreboardEntries(objectiveForSlot)
            .stream()
            .filter(scoreboardEntry -> !scoreboardEntry.hidden())
            .map(scoreboardEntry2 -> Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry2.owner()), scoreboardEntry2.name()).getString())
            .map(this::formatText2)
            .filter(local3598_0 -> local3598_0.contains("монет") || local3598_0.contains("баланс"))
            .map(this::getOptional2)
            .flatMap(Optional::stream)
            .findFirst();
   }

   public static AuctionHelperModule getInstance() {
      if (auctionHelperModule == null) {
         auctionHelperModule = new AuctionHelperModule();
      }

      return auctionHelperModule;
   }

   private String formatText(String local3602_1) {
      Matcher matcher = this.pattern.matcher(local3602_1);
      StringBuilder stringBuilder = new StringBuilder();

      while (matcher.find()) {
         try {
            matcher.appendReplacement(stringBuilder, String.valueOf(Long.parseLong(matcher.group(1)) * Long.parseLong(matcher.group(2))));
         } catch (NumberFormatException exception) {
            matcher.appendReplacement(stringBuilder, matcher.group(0));
         }
      }

      matcher.appendTail(stringBuilder);
      return stringBuilder.toString();
   }

   private AuctionHelperModule() {
      this.calculator = this.addSetting(new BooleanSetting("Калькулятор", "Считает выражения вида 2*64 в отправляемых командах (/ah sell 12*12).", true));
      this.durability = this.addSetting(
         new NumberSetting("Прочность", "Минимальная оставшаяся прочность лота.")
            .withRange(0.0F, 100.0F)
            .withStep(1.0F)
            .withSuffix("%")
            .setValue(95.0F)
      );
      this.checkBalance = this.addSetting(new BooleanSetting("Учитывать баланс", "Не подсвечивает лоты дороже баланса.", true));
      this.onlyEnchanted = this.addSetting(new BooleanSetting("Только зачарованные", "Пропускает лоты без зачарований.", false));
      this.countFilter = this.addSetting(new BooleanSetting("Фильтр количества", "Отбирает лоты по размеру стака.", false));
      this.modeSetting = this.addSetting(new ModeSetting("Режим количества", "Точно ровно столько, Минимум не меньше.", "Точно", "Точно", "Минимум"));
      this.stackSize = this.addSetting(new NumberSetting("Размер стака", "Сколько предметов должно быть в лоте.").withIntegerRange(1, 64).setValue(1.0F));
      this.modeSetting.withVisibility(() -> this.countFilter.isChecked());
      this.stackSize.withVisibility(() -> this.countFilter.isChecked());
      this.itemFilters = this.addSetting(new BooleanSetting("Фильтры предметов", "Требования к зачарованиям по типам предметов.", false));
      this.armorFilter = this.addSetting(
         new MultiChoiceSetting("Броня", "Каким требованиям должна отвечать броня.")
            .withOptions("Без шипов", "Защита V", "Прочность V", "Починка", "Починка II")
      );
      this.elytraFilter = this.addSetting(new MultiChoiceSetting("Элитры", "Каким требованиям должны отвечать элитры.").withOptions("Прочность V", "Починка"));
      this.swordsFilter = this.addSetting(
         new MultiChoiceSetting("Мечи", "Каким требованиям должны отвечать мечи.")
            .withOptions("С отдачей", "Без отдачи", "Острота", "Прочность", "Яд", "Обнаружение", "Окисление", "Вампиризм")
      );
      this.pickaxesFilter = this.addSetting(
         new MultiChoiceSetting("Кирки", "Каким требованиям должны отвечать кирки.")
            .withOptions("Эффективность", "Починка", "Удача", "Прочность", "Шёлковое касание", "Переплавка", "Магнит", "Бульдозер", "Без тяжести")
      );
      this.axesFilter = this.addSetting(
         new MultiChoiceSetting("Топоры", "Каким требованиям должны отвечать топоры.")
            .withOptions("Эффективность", "Починка", "Удача", "Прочность", "Магнит", "Лесоруб", "Пингер", "Бульдозер")
      );
      this.shovelsFilter = this.addSetting(
         new MultiChoiceSetting("Лопаты", "Каким требованиям должны отвечать лопаты.")
            .withOptions("Эффективность", "Починка", "Удача", "Прочность", "Магнит", "Бульдозер")
      );
      this.exceptionsFilter = this.addSetting(
         new MultiChoiceSetting("Исключения", "Какие лоты не подсвечивать вовсе.")
            .withOptions(
               "Стрелы",
               "Пузырьки",
               "Дыхание дракона",
               "Броня",
               "Элитры",
               "Тотемы",
               "Головы",
               "Трезубцы",
               "Луки",
               "Арбалеты",
               "Огниво",
               "Конская броня",
               "Мечи",
               "Кирки",
               "Топоры",
               "Лопаты",
               "Мотыги"
            )
      );
      this.armorFilter.withVisibility(() -> this.itemFilters.isChecked());
      this.elytraFilter.withVisibility(() -> this.itemFilters.isChecked());
      this.swordsFilter.withVisibility(() -> this.itemFilters.isChecked());
      this.pickaxesFilter.withVisibility(() -> this.itemFilters.isChecked());
      this.axesFilter.withVisibility(() -> this.itemFilters.isChecked());
      this.shovelsFilter.withVisibility(() -> this.itemFilters.isChecked());
      this.exceptionsFilter.withVisibility(() -> this.itemFilters.isChecked());
      this.numericTextSetting = this.addSetting(new NumericTextSetting("Команда поиска", "Команда, к которой допишется имя предмета из руки.", "ah search", 50));
      this.keybindSetting = this.addSetting(new KeybindSetting("Поиск с руки", "Ищет предмет из главной руки на аукционе."));
   }

   public int computeInt(ItemStack itemStack) {
      for (Text text : itemStack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines()) {
         if (this.formatText2(text.getString()).contains("цена") || this.formatText2(text.getString()).contains("монет")) {
            return this.getOptional2(text.getString()).orElse(-1);
         }
      }

      return -1;
   }

   public void updateState() {
      if (mc.player != null && !mc.player.getMainHandStack().isEmpty()) {
         String local3607_1 = Formatting.strip(mc.player.getMainHandStack().getName().getString().trim().replace("\"", "\\\""))
            .replace("[★]", "")
            .replace("fff", "")
            .replace("ggg", "")
            .replace("xxx", "")
            .replace("123", "")
            .trim();
         if (!local3607_1.isEmpty()) {
            String local3607_2 = this.numericTextSetting.getValue().isBlank() ? "ah search" : this.numericTextSetting.getValue().stripLeading();
            mc.player.networkHandler.sendChatCommand((local3607_2.startsWith("/") ? local3607_2.substring(1) : local3607_2) + " " + local3607_1);
         }
      }
   }

   private boolean matchesCriteria2(ItemStack itemStack) {
      return itemStack.isIn(ItemTags.HEAD_ARMOR)
         || itemStack.isIn(ItemTags.CHEST_ARMOR)
         || itemStack.isIn(ItemTags.LEG_ARMOR)
         || itemStack.isIn(ItemTags.FOOT_ARMOR);
   }

   public String formatText2(String local3613_1) {
      return local3613_1.toLowerCase(Locale.ROOT).replace('o', 'о').replace('e', 'е').replace('a', 'а').replace('c', 'с');
   }

   private int computeInt2(ItemStack itemStack, String local3616_2) {
      NbtList listOrEmpty = itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getListOrEmpty("custom-enchantments");

      for (int index = 0; index < listOrEmpty.size(); index++) {
         if (listOrEmpty.getCompoundOrEmpty(index).getString("type", "").equals(local3616_2)) {
            return listOrEmpty.getCompoundOrEmpty(index).getInt("level", -1);
         }
      }

      return -1;
   }

   private int computeInt3(ItemStack itemStack, String local3621_2) {
      for (Entry<RegistryEntry<Enchantment>> local3621_4 : itemStack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
         .getEnchantmentEntries()) {
         if (((RegistryEntry)local3621_4.getKey()).matchesId(Identifier.of(local3621_2))) {
            return local3621_4.getIntValue();
         }
      }

      return -1;
   }

   public boolean matchesCriteria3(String local3623_1) {
      return local3623_1.contains("1A0ꀄ")
         || local3623_1.contains("ꈁꀀꈂꌲꈂꀁ§0ꈃꄙ")
         || local3623_1.toLowerCase().contains("漢:")
         || local3623_1.toLowerCase().contains("аукцион")
         || local3623_1.toLowerCase().contains("поиск:")
         || local3623_1.toLowerCase().contains("п:")
         || local3623_1.toLowerCase().contains("категория:");
   }

   private Optional<Integer> getOptional2(String local3624_1) {
      String local3624_2 = local3624_1.replaceAll("\\D", "");
      return !local3624_2.isEmpty() && local3624_2.length() <= 9 ? Optional.of(Integer.parseInt(local3624_2)) : Optional.empty();
   }

   private void renderElement(DrawContext drawContext, AuctionHelperModule.AuctionItemEntry local3625_2, int local3625_3) {
      if (local3625_2 != null) {
         drawContext.fill(local3625_2.slot().x, local3625_2.slot().y, local3625_2.slot().x + 16, local3625_2.slot().y + 16, local3625_3);
      }
   }

   private record AuctionItemEntry(int price, Slot slot) {
   }
}
