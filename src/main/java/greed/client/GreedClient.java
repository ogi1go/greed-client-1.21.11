package greed.client;

import greed.client.storage.AccountStore;
import greed.client.command.AutowardenCommand;
import greed.client.command.BlockespCommand;
import greed.client.ui.ClickGuiScreen;
import greed.client.command.ClientCommand;
import greed.client.event.ClientTickEvent;
import greed.client.command.CommandDispatcher;
import greed.client.command.ConfigCommand;
import greed.client.cosmetic.CosmeticFeatureRenderer;
import greed.client.ui.MainMenuScreen;
import greed.client.event.Event;
import greed.client.event.EventBus;
import greed.client.command.FriendCommand;
import greed.client.command.HelpCommand;
import greed.client.ui.hud.HudLayout;

import greed.client.command.MacroCommand;
import greed.client.storage.MacroStore;
import greed.client.module.ModuleManager;
import greed.client.module.misc.UnHookModule;

import ru.dreamix.protection.api.impl.profile.ProfileAPI;
import greed.client.command.RctCommand;
import greed.client.command.WaypointCommand;
import greed.client.util.FrameTimer;
import greed.client.util.WaypointManager;
import greed.client.command.ResourcePackCommand;
import greed.client.ui.UiWarmup;
import greed.client.render.ThemePaletteBuffer;
import greed.client.render.ItemRenderCache;
import greed.client.render.PipelineWarmup;
import greed.client.storage.ConfigStore;
import greed.client.util.IrcClient;
import greed.client.cosmetic.CosmeticRenderDispatcher;
import greed.client.cosmetic.CosmeticWarmup;
import greed.client.render.Render2D;
import ru.dreamix.protection.api.impl.profile.objects.RoleObject;
import greed.client.command.StaffCommand;

import java.util.ArrayList;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.StartTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreedClient implements ModInitializer, ClientModInitializer {
   public static final String TEXT_GREED = "greed";
   private static final GreedClient greedClient = new GreedClient();
   private static final Logger LOGGER = LoggerFactory.getLogger("greed");
   private long loadTimeMillis;
   private boolean loaded;
   private int autosaveTicks;
   private CommandDispatcher commandDispatcher;
   private KeyBinding openGuiKey;
   private final FrameTimer frameTimer = new FrameTimer();

   public boolean loaded() {
      return this.loaded;
   }

   public static Identifier id(String local6600_0) {
      return Identifier.of("greed", local6600_0);
   }

   public KeyBinding openGuiKey() {
      return this.openGuiKey;
   }

   private void registerOpenKey() {
      this.openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.greed.clickgui", Type.KEYSYM, 344, Category.GAMEPLAY));
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)minecraftClient -> {
         while (this.openGuiKey.wasPressed()) {
            if (minecraftClient.currentScreen == null && !UnHookModule.isUnhooked()) {
               minecraftClient.setScreen(ClickGuiScreen.instance());
            }
         }
      });
      if (Boolean.getBoolean("greed.openGui") || System.getenv("GREED_OPEN_GUI") != null) {
         ClientTickEvents.END_CLIENT_TICK.register((EndTick)minecraftClient2 -> {
            if (minecraftClient2.currentScreen instanceof MainMenuScreen && !UnHookModule.isUnhooked()) {
               minecraftClient2.setScreen(ClickGuiScreen.instance());
            }
         });
      }
   }

   public CommandDispatcher commandDispatcher() {
      return this.commandDispatcher;
   }

   private void registerTicks() {
      ClientTickEvents.START_CLIENT_TICK.register((StartTick)minecraftClient -> {
         if (EventBus.getInstance().hasListeners(ClientTickEvent.class)) {
            EventBus.getInstance().post(new ClientTickEvent(Event.Stage.PRE));
         }
      });
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)minecraftClient2 -> {
         if (EventBus.getInstance().hasListeners(ClientTickEvent.class)) {
            EventBus.getInstance().post(new ClientTickEvent(Event.Stage.POST));
         }

         if (this.loaded && ++this.autosaveTicks >= 1200) {
            this.autosaveTicks = 0;
            ConfigStore.autosave();
         }
      });
   }

   public static Logger logger() {
      return LOGGER;
   }

   @Override
   public String toString() {
      return "Greed(loadTimeMs="
         + this.loadTimeMs()
         + ", loaded="
         + this.loaded()
         + ", autosaveTicks="
         + this.autosaveTicks()
         + ", commandDispatcher="
         + this.commandDispatcher()
         + ", openGuiKey="
         + this.openGuiKey()
         + ", framelibrary="
         + this.framelibrary()
         + ")";
   }

   public int autosaveTicks() {
      return this.autosaveTicks;
   }

   private void registerCosmetics() {
      CosmeticWarmup.updateState3();
      CosmeticWarmup.updateState();
      LivingEntityFeatureRendererRegistrationCallback.EVENT
         .register((LivingEntityFeatureRendererRegistrationCallback)(local6603_0, local6603_1, registrationHelper, context) -> {
            if (local6603_1 instanceof PlayerEntityRenderer<?> local6603_4) {
               registrationHelper.register(new CosmeticFeatureRenderer(local6603_4));
            }
         });
      CosmeticRenderDispatcher.updateState();
   }

   @Override
   public boolean equals(Object local6612_1) {
      if (local6612_1 == this) {
         return true;
      } else if (!(local6612_1 instanceof GreedClient currentGreedClient)) {
         return false;
      } else {
         if (!currentGreedClient.canEqual(this)) {
            return false;
         }

         if (this.loadTimeMs() != currentGreedClient.loadTimeMs()) {
            return false;
         }

         if (this.loaded() != currentGreedClient.loaded()) {
            return false;
         }

         if (this.autosaveTicks() != currentGreedClient.autosaveTicks()) {
            return false;
         }

         Object local6612_3 = this.commandDispatcher();
         Object local6612_4 = currentGreedClient.commandDispatcher();
         if (local6612_3 == null ? local6612_4 == null : local6612_3.equals(local6612_4)) {
            Object local6612_5 = this.openGuiKey();
            Object local6612_6 = currentGreedClient.openGuiKey();
            if (local6612_5 == null ? local6612_6 == null : local6612_5.equals(local6612_6)) {
               Object local6612_7 = this.framelibrary();
               Object local6612_8 = currentGreedClient.framelibrary();
               return local6612_7 == null ? local6612_8 == null : local6612_7.equals(local6612_8);
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public GreedClient commandDispatcher(CommandDispatcher newCommandDispatcher) {
      this.commandDispatcher = newCommandDispatcher;
      return this;
   }

   public void unload() {
      ConfigStore.saveActiveConfig();
      ItemRenderCache.updateState6();
      Render2D.shutdown();
      this.loaded = false;
      LOGGER.info("shutdown");
   }

   public void load() {
      ProfileAPI.create("user", 1, RoleObject.ADMIN);
      this.loadTimeMillis = System.currentTimeMillis();
      System.setProperty("sun.font.layout.ffm", "false");
      Render2D.initialize();
      ItemRenderCache.updateState();
      ThemePaletteBuffer.updateState();
      UiWarmup.updateState5();
      HudLayout.updateState2();
      ModuleManager.initializeModules();
      EventBus.getInstance().subscribe(AccountStore.getAccountStore());
      EventBus.getInstance().subscribe(WaypointManager.getInstance());
      EventBus.getInstance().subscribe(MacroStore.getInstance());
      EventBus.getInstance().subscribe(IrcClient.getInstance());
      this.registerCosmetics();
      this.initCommands();
      this.registerOpenKey();
      this.registerTicks();
      greed.client.util.greedtone.PathExecutor.init();
      ConfigStore.loadLastConfig();
      ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)minecraftClient -> this.unload());
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         if (this.loaded) {
            ConfigStore.saveActiveConfig();
         }
      }));
      ResourceLoader.get(ResourceType.CLIENT_RESOURCES).registerReloader(id("render_warmup"), (SynchronousResourceReloader)resourceManager -> {
         MinecraftClient minecraftClient2 = MinecraftClient.getInstance();
         if (minecraftClient2 != null) {
            minecraftClient2.execute(() -> {
               Render2D.reloadTextures();
               PipelineWarmup.resetWarmup();
               UiWarmup.updateState3();
            });
         }
      });
      this.loaded = true;
   }

   public GreedClient loaded(boolean newLoaded) {
      this.loaded = newLoaded;
      return this;
   }

   public GreedClient loadTimeMs(long newLoadTimeMillis) {
      this.loadTimeMillis = newLoadTimeMillis;
      return this;
   }

   public GreedClient autosaveTicks(int newAutosaveTicks) {
      this.autosaveTicks = newAutosaveTicks;
      return this;
   }

   public void onInitializeClient() {
      greedClient.load();
   }

   public FrameTimer framelibrary() {
      return this.frameTimer;
   }

   public GreedClient openGuiKey(KeyBinding keyBinding) {
      this.openGuiKey = keyBinding;
      return this;
   }

   @Override
   public int hashCode() {
      int i1 = 59;
      int i = 1;
      long loadTimeMs = this.loadTimeMs();
      i = i * 59 + (int)(loadTimeMs >>> 32 ^ loadTimeMs);
      i = i * 59 + (this.loaded() ? 79 : 97);
      i = i * 59 + this.autosaveTicks();
      Object local6627_5 = this.commandDispatcher();
      i = i * 59 + (local6627_5 == null ? 43 : local6627_5.hashCode());
      Object local6627_6 = this.openGuiKey();
      i = i * 59 + (local6627_6 == null ? 43 : local6627_6.hashCode());
      Object local6627_7 = this.framelibrary();
      return i * 59 + (local6627_7 == null ? 43 : local6627_7.hashCode());
   }

   public static GreedClient inst() {
      return greedClient;
   }

   private void initCommands() {
      ArrayList<ClientCommand> clientCommands = new ArrayList<>();
      clientCommands.add(new ResourcePackCommand());
      clientCommands.add(new ConfigCommand());
      clientCommands.add(new FriendCommand());
      clientCommands.add(new WaypointCommand());
      clientCommands.add(new MacroCommand());
      clientCommands.add(new StaffCommand());
      clientCommands.add(new BlockespCommand());
      clientCommands.add(new AutowardenCommand());
      clientCommands.add(new RctCommand());
//      clientCommands.add(new IrcCommand());
//      clientCommands.add(new PartyCommand());
      clientCommands.add(new greed.client.command.GotoCommand());
      clientCommands.add(new HelpCommand(clientCommands));
      this.commandDispatcher = new CommandDispatcher(clientCommands, ".");
   }

   public void onInitialize() {
      LOGGER.info("Greed initialized");
   }

   public long loadTimeMs() {
      return this.loadTimeMillis;
   }

   protected boolean canEqual(Object local6635_1) {
      return local6635_1 instanceof GreedClient;
   }
}
