package greed.client.util;

import greed.client.event.EventBus;
import greed.client.event.EventListener;
import greed.client.MinecraftAccess;
import greed.client.event.PacketEvent;
import greed.client.event.PlayerTickEvent;

import java.util.Locale;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.c2s.common.ClientOptionsC2SPacket;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.util.Identifier;

public final class ClientBrandManager implements MinecraftAccess {
   private static ClientBrandManager clientBrandManager;
   private final ClientBrandGenerator clientBrandGenerator = new ClientBrandGenerator();
   private ClientIdentity clientIdentity;
   private boolean flag;
   private final EventListener<PacketEvent> packetListener = packetEvent -> {
      if (packetEvent.isIncoming()) {
         if (packetEvent.getPacket() instanceof GameJoinS2CPacket) {
            this.flag = true;
         }
      } else if (this.clientIdentity != null) {
         if (packetEvent.getPacket() instanceof CustomPayloadC2SPacket customPayloadC2SPacket) {
            if (customPayloadC2SPacket.payload() instanceof BrandCustomPayload) {
               packetEvent.setPacket(new CustomPayloadC2SPacket(new BrandCustomPayload(this.clientIdentity.getClientBrand().formatText3(this.clientIdentity.formatText2()))));
            } else {
               Identifier identifier = customPayloadC2SPacket.payload().getId().id();
               if (!identifier.getNamespace().equals("minecraft") || identifier.getPath().equals("register") || identifier.getPath().equals("unregister")) {
                  packetEvent.cancel();
               }
            }
         } else {
            if (packetEvent.getPacket() instanceof ClientOptionsC2SPacket clientOptionsC2SPacket) {
               SyncedClientOptions syncedClientOptions = clientOptionsC2SPacket.options();
               SyncedClientOptions syncedClientOptions2 = this.clientIdentity.getSyncedClientOptions();
               packetEvent.setPacket(
                  new ClientOptionsC2SPacket(
                     new SyncedClientOptions(
                        syncedClientOptions2.language(),
                        Math.max(syncedClientOptions.viewDistance(), syncedClientOptions2.viewDistance()),
                        syncedClientOptions.chatVisibility(),
                        syncedClientOptions.chatColorsEnabled(),
                        syncedClientOptions2.playerModelParts(),
                        syncedClientOptions2.mainArm(),
                        syncedClientOptions2.filtersText(),
                        syncedClientOptions2.allowsServerListing(),
                        syncedClientOptions2.particleStatus()
                     )
                  )
               );
            }
         }
      }
   };
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> {
      if (this.flag && this.clientIdentity != null && mc.player != null && mc.world != null) {
         this.flag = false;
         mc.inGameHud.getChatHud().clear(true);
      }
   };

   public static ClientBrandManager getInstance() {
      if (clientBrandManager == null) {
         clientBrandManager = new ClientBrandManager();
      }

      return clientBrandManager;
   }

   public void processData(String local1145_1) {
      this.flag = false;
      if (local1145_1 != null && local1145_1.toLowerCase(Locale.ROOT).contains("holyworld")) {
         this.clientIdentity = this.clientBrandGenerator.getClientIdentity();
         EventBus.getInstance().subscribe(this);
      } else {
         this.clientIdentity = null;
         EventBus.getInstance().unsubscribe(this);
      }
   }
}
