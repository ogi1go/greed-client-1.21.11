package greed.client.util;

import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.util.Arm;

public class ClientIdentity {
   private ClientBrand clientBrand;
   private String strVal;
   private SyncedClientOptions syncedClientOptions2;

   public String formatText() {
      return "%s %s (%s) · %s · %d чанков · %s"
         .formatted(
            this.clientBrand.formatText(),
            this.strVal,
            this.clientBrand.formatText3(this.strVal),
            this.syncedClientOptions2.language(),
            this.syncedClientOptions2.viewDistance(),
            this.syncedClientOptions2.mainArm() == Arm.LEFT ? "левша" : "правша"
         );
   }

   public SyncedClientOptions getSyncedClientOptions() {
      return this.syncedClientOptions2;
   }

   public String formatText2() {
      return this.strVal;
   }

   public ClientIdentity(ClientBrand initialClientBrand, String local4966_2, SyncedClientOptions syncedClientOptions) {
      this.clientBrand = initialClientBrand;
      this.strVal = local4966_2;
      this.syncedClientOptions2 = syncedClientOptions;
   }

   public ClientBrand getClientBrand() {
      return this.clientBrand;
   }
}
