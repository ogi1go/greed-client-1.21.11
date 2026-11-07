package greed.client.ui;

import greed.client.util.AuctionItemCatalog;
import greed.client.setting.NumberSetting;
import greed.client.storage.AutoBuyItem;
import greed.client.ui.SmoothScroll;
import greed.client.setting.TextSetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Random;
import net.minecraft.item.Items;

public final class AutoBuySettingsScreen {
   public static final String[] strValArray = new String[]{"Slow", "Medium", "Ultrabuy"};
   public static final float[] floatValArray = new float[]{
      0.41422594F,
      0.5606695F,
      0.29707113F,
      0.65690374F,
      0.41841003F,
      0.7573222F,
      0.59414226F,
      0.37238494F,
      0.42677826F,
      0.53974897F,
      0.37656903F,
      0.8995816F,
      0.5146443F,
      0.53556484F,
      0.28870293F,
      0.50209206F
   };
   private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
   private static final HashMap<String, Long> map = new HashMap<>();
   private static AutoBuySettingsScreen autoBuySettingsScreen;
   public TextSetting textSetting = new TextSetting("Search", "").withPlaceholder("Поиск предметов...");
   public NumberSetting numberSetting = new NumberSetting("Auto Buy Speed", "Select scan frequency and execution speed")
      .withIntegerRange(0, 2)
      .setValue(2.0F);
   public AutoBuySettingsScreen.SubContext3 nested0374 = new AutoBuySettingsScreen.SubContext3("Buy Price (Max)", "125000", 9);
   public AutoBuySettingsScreen.SubContext3 nested03742 = new AutoBuySettingsScreen.SubContext3("Sell Price (Min)", "140000", 9);
   public AutoBuySettingsScreen.SubContext3 nested03743 = new AutoBuySettingsScreen.SubContext3("Minimum Durability", "85", 3);
   public TextSetting textSetting2 = new TextSetting("Telegram Bot Token", "").setValue("7123984120:AAHq_x9Z8k...").withPlaceholder("Bot token...");
   public TextSetting textSetting3 = new TextSetting("Telegram Chat ID", "").setValue("123456789").withPlaceholder("Chat ID...");
   public LinkedHashMap<String, AutoBuySettingsScreen.SubContext> map2 = new LinkedHashMap<>();
   public ArrayList<AutoBuySettingsScreen.DataRecord2> list = new ArrayList<>();
   public float[] floatValArray2 = new float[floatValArray.length];
   public HashMap<String, Long> map3 = new HashMap<>();
   public HashMap<String, Long> map4 = new HashMap<>();
   public SmoothScroll smoothScroll = new SmoothScroll();
   public SmoothScroll smoothScroll2 = new SmoothScroll();
   public float[] floatValArray3;
   public float[] floatValArray4;
   public long timeMillis;
   public double doubleVal;
   public String strVal;
   public long timeMillis2;
   public long timeMillis3;
   public String strVal2 = "";
   private Random random = new Random();
   private long timeMillis4;

   public static String formatText(long local1559_0) {
      return String.format(Locale.US, "%,d", local1559_0);
   }

   public static long computeLong(String local1560_0) {
      if (local1560_0 != null && !local1560_0.isEmpty()) {
         try {
            return Long.parseLong(local1560_0);
         } catch (NumberFormatException exception) {
            return 0L;
         }
      } else {
         return 0L;
      }
   }

   public static long computeLong2(String local1561_0) {
      return map.get(local1561_0) != null
         ? map.get(local1561_0)
         : Math.round((25000L + Math.floorMod(local1561_0.hashCode(), 900) * 1000L) / 1000.0) * 1000L;
   }

   private AutoBuySettingsScreen.SubContext getNested0372(String local1562_1) {
      return new AutoBuySettingsScreen.SubContext(computeLong2(local1562_1), Math.round(computeLong2(local1562_1) * 1.12 / 1000.0) * 1000L);
   }

   public void processData(long local1563_1) {
      if (this.timeMillis4 == 0L) {
         this.timeMillis4 = local1563_1 + 6000L + this.random.nextInt(8000);
      }

      if (local1563_1 >= this.timeMillis4 && !this.map2.isEmpty()) {
         this.timeMillis4 = local1563_1 + 6000L + this.random.nextInt(8000);
         this.floatValArray2[this.random.nextInt(this.floatValArray2.length)] = Math.max(
            0.12F, Math.min(1.0F, floatValArray[this.random.nextInt(this.floatValArray2.length)] + (this.random.nextFloat() - 0.35F) * 0.25F)
         );
      }
   }

   public static AutoBuySettingsScreen getInstance() {
      if (autoBuySettingsScreen == null) {
         autoBuySettingsScreen = new AutoBuySettingsScreen();
      }

      return autoBuySettingsScreen;
   }

   public void processData2(String local1565_1) {
      if (this.map2.remove(local1565_1) == null) {
         this.map2.put(local1565_1, this.getNested0372(local1565_1));
      } else if (local1565_1.equals(this.strVal)) {
         this.strVal = null;
         this.timeMillis2 = System.currentTimeMillis();
      }

      this.map3.put(local1565_1, System.currentTimeMillis());
   }

   static {
      map.put("Элитры Крушителя", 2400000L);
      map.put("Сфера Афины", 1600000L);
      map.put("Чарка", 1250000L);
      map.put("Меч Крушителя", 890000L);
      map.put("Незеритовый слиток", 720000L);
      map.put("Булава Крушителя", 540000L);
      map.put("Спавнер", 480000L);
      map.put("Талисман Крушителя", 310000L);
   }

   public void processData3(String local1567_1, long local1567_2) {
      this.list
         .add(
            0,
            new AutoBuySettingsScreen.DataRecord2(
               AuctionItemCatalog.getAutoBuyItem(local1567_1.replaceAll("\\s+[xх]\\d+$", "").trim()) == null
                  ? new AutoBuyItem.SubContext().getNested05238(local1567_1.replaceAll("\\s+[xх]\\d+$", "").trim()).getNested05234(Items.PAPER).getAutoBuyItem()
                  : AuctionItemCatalog.getAutoBuyItem(local1567_1.replaceAll("\\s+[xх]\\d+$", "").trim()),
               local1567_1.matches(".*\\s+[xх]\\d+$") ? Integer.parseInt(local1567_1.replaceAll(".*\\s+[xх](\\d+)$", "$1")) : 1,
               LocalTime.now().format(dateTimeFormatter),
               local1567_2
            )
         );
      if (this.list.size() > 40) {
         this.list.remove(this.list.size() - 1);
      }

      this.timeMillis += local1567_2;
   }

   public boolean matchesCriteria(String local1568_1) {
      return this.map2.containsKey(local1568_1);
   }

   private AutoBuySettingsScreen() {
      AuctionItemCatalog.updateState();
      System.arraycopy(floatValArray, 0, this.floatValArray2, 0, this.floatValArray2.length);

      for (String local1569_4 : new String[]{
         "Шлем Крушителя",
         "Нагрудник Крушителя",
         "Поножи Крушителя",
         "Ботинки Крушителя",
         "Меч Крушителя",
         "Булава Крушителя",
         "Сфера Афины",
         "Талисман Крушителя",
         "Чарка",
         "Элитры Крушителя",
         "Хлопушка",
         "Шалкер"
      }) {
         if (AuctionItemCatalog.getAutoBuyItem(local1569_4) != null) {
            this.map2.put(local1569_4, this.getNested0372(local1569_4));
         }
      }

      this.nested0374.withPlaceholder("$0");
      this.nested03742.withPlaceholder("$0");
      this.nested03743.withPlaceholder("0%");
   }

   public ArrayList<AutoBuyItem> getArrayList() {
      if (!this.textSetting.getValue().toLowerCase(Locale.ROOT).trim().equals(this.strVal2)) {
         this.strVal2 = this.textSetting.getValue().toLowerCase(Locale.ROOT).trim();
         this.timeMillis3 = System.currentTimeMillis();
         this.smoothScroll.targetOffset = 0.0F;
      }

      ArrayList<AutoBuyItem> local1570_1 = new ArrayList<>();

      for (AutoBuyItem autoBuyItem : AuctionItemCatalog.getArrayList2()) {
         if (this.strVal2.isEmpty()
            || autoBuyItem.name().toLowerCase(Locale.ROOT).contains(this.strVal2)
            || autoBuyItem.category().toLowerCase(Locale.ROOT).contains(this.strVal2)) {
            local1570_1.add(autoBuyItem);
         }
      }

      return local1570_1;
   }

   public static final class SubContext {
      public AutoBuySettingsScreen.SubContext3 nested03744;
      public AutoBuySettingsScreen.SubContext3 nested03745;

      SubContext(long local1571_1, long local1571_3) {
         this.nested03744 = new AutoBuySettingsScreen.SubContext3("Buy Price (Max)", Long.toString(local1571_1), 9);
         this.nested03745 = new AutoBuySettingsScreen.SubContext3("Sell Price (Min)", Long.toString(local1571_3), 9);
         this.nested03744.withPlaceholder("$0");
         this.nested03745.withPlaceholder("$0");
      }
   }

   public record DataRecord2(AutoBuyItem item, int count, String time, long priceCoins) {
   }

   public static final class SubContext3 extends TextSetting {
      @Override
      public void insertText(String local1580_1) {
         super.insertText(local1580_1 == null ? "" : local1580_1.replaceAll("\\D", ""));
      }

      public SubContext3(String initialName, String local1581_2, int local1581_3) {
         super(initialName, "", local1581_2, local1581_3);
      }
   }
}
