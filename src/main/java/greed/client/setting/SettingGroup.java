package greed.client.setting;

import greed.client.module.Module;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class SettingGroup {
   public final String strVal;
   public final Object object;
   public final List<Setting<?>> list;
   private static final Map<Module, SettingGroup.DataRecord> cacheByModule = new IdentityHashMap<>();

   private SettingGroup(String name, Object object, List<Setting<?>> settings) {
      this.strVal = name;
      this.object = object;
      this.list = settings;
   }

   public boolean checkState() {
      return this.strVal != null;
   }

   public static List<SettingGroup> getList(Module module) {
      List<Setting<?>> settings = module.getSettings();
      SettingGroup.DataRecord cachedRecord = cacheByModule.get(module);
      int settingsSize = settings.size();
      Setting<?> lastSetting = settingsSize == 0 ? null : settings.get(settingsSize - 1);
      if (cachedRecord == null || cachedRecord.size != settingsSize || cachedRecord.last != lastSetting) {
         cachedRecord = new SettingGroup.DataRecord(List.copyOf(getList2(module)), settingsSize, lastSetting);
         cacheByModule.put(module, cachedRecord);
      }

      return cachedRecord.sections;
   }

   public static boolean matchesCriteria(List<SettingGroup> groups) {
      for (SettingGroup settingGroup : groups) {
         for (Setting<?> setting : settingGroup.list) {
            if (setting.isVisible()) {
               return true;
            }
         }
      }

      return false;
   }

   public static List<SettingGroup> getList2(Module module) {
      List<SettingGroup> groups = new ArrayList<>();
      String sectionName = null;
      Object module2 = module;
      List<Setting<?>> settings = new ArrayList<>();

      for (Setting<?> setting : module.getSettings()) {
         if (setting instanceof SectionSetting sectionSetting) {
            if (!settings.isEmpty() || sectionName != null) {
               groups.add(new SettingGroup(sectionName, module2, settings));
            }

            sectionName = sectionSetting.name;
            module2 = sectionSetting;
            settings = new ArrayList<>();
         } else {
            settings.add(setting);
         }
      }

      if (!settings.isEmpty() || sectionName != null) {
         groups.add(new SettingGroup(sectionName, module2, settings));
      }

      return groups;
   }

   private record DataRecord(List<SettingGroup> sections, int size, Setting<?> last) {
   }
}
