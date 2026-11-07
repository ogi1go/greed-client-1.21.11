package greed.client.cosmetic;

public enum CosmeticCategory {
   BODYWEAR("Рюкзаки", "Рюкзак", "bodywear", "cos_bodywear.png"),
   WINGS("Крылья", "Крылья", "wings", "cos_wings.png"),
   HAT("Головные уборы", "Убор", "hat", "cos_hat.png"),
   ACCESSORY("Аксессуары", "Аксессуар", "accessory", "cos_accessory.png"),
   PET("Питомцы", "Питомец", "pet", "cos_pet.png"),
   SWORD("Мечи", "Меч", "sword", "cos_sword.png", true),
   MACE("Булавы", "Булава", "mace", "cos_mace.png", true),
   SHIELD("Щиты", "Щит", "shield", "cos_shield.png", true);

   private final String strVal;
   private final String strVal2;
   private final String strVal3;
   private final String strVal4;
   private final boolean flag;

   private CosmeticCategory(String local7227_3, String local7227_4, String local7227_5, String local7227_6) {
      this(local7227_3, local7227_4, local7227_5, local7227_6, false);
   }

   private CosmeticCategory(String local7228_3, String local7228_4, String local7228_5, String local7228_6, boolean local7228_7) {
      this.strVal = local7228_3;
      this.strVal2 = local7228_4;
      this.strVal3 = local7228_5;
      this.strVal4 = "assets/greed/textures/gui/mainmenu/icons/" + local7228_6;
      this.flag = local7228_7;
   }

   public boolean checkState() {
      return this.flag;
   }

   public String formatText() {
      return this.strVal;
   }

   public String formatText2() {
      return this.strVal2;
   }

   public String formatText3() {
      return this.strVal3;
   }

   public String formatText4() {
      return this.strVal4;
   }
}
