package ru.dreamix.protection.api.impl.profile.models;

import ru.dreamix.protection.api.impl.profile.objects.RoleObject;
import ru.dreamix.protection.api.impl.profile.objects.SubscriptionObject;


public class ProfileInformationModel {
   private final String name;
   private final SubscriptionObject subscriptionObject;
   private final int userIdentifier;
   private final RoleObject role;
   private final String launchedProduct;

   @Override
   public String toString() {
      return "ProfileInformationModel{name='"
         + this.name
         + '\''
         + ", subscriptionObject='"
         + this.subscriptionObject
         + '\''
         + ", userIdentifier="
         + this.userIdentifier
         + ", role="
         + this.role
         + ", launchedProduct="
         + this.launchedProduct
         + '}';
   }

   public String getName() {
      return this.name;
   }

   public SubscriptionObject getSubscriptionObject() {
      return this.subscriptionObject;
   }

   public int getUserIdentifier() {
      return this.userIdentifier;
   }

   public RoleObject getRole() {
      return this.role;
   }

   public String getLaunchedProduct() {
      return this.launchedProduct;
   }

   public ProfileInformationModel(String initialName, SubscriptionObject initialSubscriptionObject, int initialUserIdentifier, RoleObject initialRole, String initialLaunchedProduct) {
      this.name = initialName;
      this.subscriptionObject = initialSubscriptionObject;
      this.userIdentifier = initialUserIdentifier;
      this.role = initialRole;
      this.launchedProduct = initialLaunchedProduct;
   }
}
