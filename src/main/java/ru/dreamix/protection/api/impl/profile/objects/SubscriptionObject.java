package ru.dreamix.protection.api.impl.profile.objects;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SubscriptionObject {
   private final String stringTill;
   private final long unixTimeStampTill;

   public SubscriptionObject(String initialStringTill) {
      this.stringTill = initialStringTill;
      this.unixTimeStampTill = LocalDate.parse(initialStringTill, DateTimeFormatter.ofPattern("dd.MM.yyyy")).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
   }

   @Override
   public String toString() {
      return "SubscriptionObject{stringTill='" + this.stringTill + '\'' + ", unixTimeStampTill=" + this.unixTimeStampTill + '}';
   }

   public String getStringTill() {
      return this.stringTill;
   }

   public long getUnixTimeStampTill() {
      return this.unixTimeStampTill;
   }
}
