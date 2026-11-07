package greed.client.storage;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public final class AccountEntry {
   private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
   private static final DateTimeFormatter dateTimeFormatter2 = DateTimeFormatter.ofPattern("HH:mm:ss");
   private String strVal;
   private UUID uUID2;
   private AccountEntry.TypeOption nested0488;
   private String strVal2;
   private String strVal3;
   private long timeMillis;
   private long timeMillis2;
   private boolean flag;
   private ArrayList<String[]> list = new ArrayList<>();

   public static String formatText(long local5615_0) {
      long local5615_2 = local5615_0 / 60000L;
      long local5615_4 = local5615_2 / 1440L;
      long local5615_6 = local5615_2 / 60L % 24L;
      long local5615_8 = local5615_2 % 60L;
      StringBuilder stringBuilder = new StringBuilder();
      if (local5615_4 > 0L) {
         stringBuilder.append(local5615_4).append("d ");
      }

      if (local5615_4 > 0L || local5615_6 > 0L) {
         stringBuilder.append(local5615_6).append("h ");
      }

      stringBuilder.append(local5615_8).append('m');
      return stringBuilder.toString();
   }

   public void processData(long local5616_1) {
      this.timeMillis2 = this.timeMillis2 + Math.max(0L, local5616_1);
   }

   public static AccountEntry getAccountEntry(String local5617_0) {
      return new AccountEntry(
         local5617_0, getUUID(local5617_0), AccountEntry.TypeOption.CRACKED, "—", "Player", System.currentTimeMillis(), 0L, false
      );
   }

   public ArrayList<String[]> getArrayList() {
      return this.list;
   }

   public String formatText2() {
      return this.strVal2;
   }

   public AccountEntry.TypeOption getNested0488() {
      return this.nested0488;
   }

   public String formatText3() {
      return dateTimeFormatter.format(Instant.ofEpochMilli(this.timeMillis).atZone(ZoneId.systemDefault()));
   }

   public long computeLong() {
      return this.timeMillis;
   }

   public void processData2(boolean local5623_1) {
      this.flag = local5623_1;
   }

   public void processData3(String local5624_1) {
      this.strVal3 = local5624_1;
   }

   public static UUID getUUID(String local5625_0) {
      return UUID.nameUUIDFromBytes(("OfflinePlayer:" + local5625_0).getBytes(StandardCharsets.UTF_8));
   }

   public AccountEntry(
      String local5626_1,
      UUID uUID,
      AccountEntry.TypeOption local5626_3,
      String local5626_4,
      String local5626_5,
      long local5626_6,
      long local5626_8,
      boolean local5626_10
   ) {
      this.strVal = local5626_1;
      this.uUID2 = uUID;
      this.nested0488 = local5626_3;
      this.strVal2 = local5626_4;
      this.strVal3 = local5626_5;
      this.timeMillis = local5626_6;
      this.timeMillis2 = local5626_8;
      this.flag = local5626_10;
   }

   public UUID getUUID2() {
      return this.uUID2;
   }

   public void processData4(UUID uUID) {
      this.uUID2 = uUID;
   }

   public void processData5(String local5630_1) {
      if (local5630_1 != null && !local5630_1.isBlank()) {
         this.list.add(0, new String[]{local5630_1.trim(), dateTimeFormatter2.format(Instant.now().atZone(ZoneId.systemDefault()))});

         while (this.list.size() > 50) {
            this.list.remove(this.list.size() - 1);
         }
      }
   }

   public long computeLong2() {
      return this.timeMillis2;
   }

   public void processData6(String local5632_1) {
      this.strVal2 = local5632_1;
   }

   public String formatText4() {
      return this.strVal;
   }

   public boolean checkState() {
      return this.flag;
   }

   public void processData7(String local5635_1) {
      this.strVal = local5635_1;
   }

   public String formatText5() {
      return this.strVal3;
   }

   public enum TypeOption {
      CRACKED,
      MICROSOFT;
   }
}
