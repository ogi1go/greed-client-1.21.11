package greed.client.module;

import greed.client.event.EventBus;
import greed.client.util.InputBinding;
import greed.client.MinecraftAccess;
import greed.client.util.SoundPlayer;
import greed.client.setting.Setting;

import java.util.ArrayList;

public abstract class Module implements MinecraftAccess {
   public String name;
   public String description;
   public ModuleCategory category;
   private boolean enabled;
   private int keyCode = -1;
   public boolean bindingActive;
   private final ArrayList<Setting<?>> settings = new ArrayList<>();

   public void setEnabledWithoutCallbacks(boolean newEnabled) {
      if (newEnabled != this.enabled) {
         this.enabled = newEnabled;
         if (newEnabled) {
            EventBus.getInstance().subscribe(this);
         } else {
            EventBus.getInstance().unsubscribe(this);
         }
      }
   }

   protected <S extends Setting<?>> S addSetting(S local2728_1) {
      this.settings.add(local2728_1);
      this.bindingActive = true;
      return local2728_1;
   }

   public Module withKeyCode(int newKeyCode) {
      this.keyCode = newKeyCode;
      return this;
   }

   public boolean hasKeybind() {
      return this.keyCode != -1;
   }

   public int getKeyCode() {
      return this.keyCode;
   }

   protected Module() {
      ModuleInfo annotation = this.getClass().getAnnotation(ModuleInfo.class);
      this.name = annotation.name();
      this.description = annotation.description();
      this.category = annotation.category();
      ModuleManager.registerModule(this);
   }

   public String getKeyName() {
      return !this.hasKeybind() ? "" : InputBinding.keyboardKey(this.keyCode).getDisplayName();
   }

   public void toggle() {
      this.setEnabled(!this.enabled);
   }

   protected void addSettings(Setting<?>... local2735_1) {
      for (Setting<?> local2735_5 : local2735_1) {
         this.settings.add(local2735_5);
      }

      this.bindingActive = true;
   }

   public boolean hasSettings() {
      return !this.settings.isEmpty();
   }

   protected void onEnable() {
   }

   public ArrayList<Setting<?>> getSettings() {
      return this.settings;
   }

   protected Module(String initialName, String local2739_2, ModuleCategory initialCategory) {
      this.name = initialName;
      this.description = local2739_2 == null ? "" : local2739_2;
      this.category = initialCategory;
      ModuleManager.registerModule(this);
   }

   protected void onDisable() {
   }

   public void setEnabledFlag(boolean newEnabled) {
      this.enabled = newEnabled;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean newEnabled) {
      if (newEnabled != this.enabled) {
         this.enabled = newEnabled;
         if (newEnabled) {
            EventBus.getInstance().subscribe(this);
            this.onEnable();
         } else {
            EventBus.getInstance().unsubscribe(this);
            this.onDisable();
         }

         SoundPlayer.processData(newEnabled ? "notify_type1_enabled" : "notify_type1_disabled");
      }
   }
}
