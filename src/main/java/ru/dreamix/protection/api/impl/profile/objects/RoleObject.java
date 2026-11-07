package ru.dreamix.protection.api.impl.profile.objects;


public enum RoleObject {
   DEFAULT("User", 0),
   BETA("Beta", 1),
   YOUTUBE("YouTube", 2),
   ADMIN("Administrator", 3);

   private final String translatedName;
   private final int priority;

   @Override
   public String toString() {
      return this.translatedName;
   }

   public boolean priorityHigherOrEquals(RoleObject roleObject) {
      return this.priority >= roleObject.priority;
   }

   private RoleObject(String initialTranslatedName, int initialPriority) {
      this.translatedName = initialTranslatedName;
      this.priority = initialPriority;
   }
}
