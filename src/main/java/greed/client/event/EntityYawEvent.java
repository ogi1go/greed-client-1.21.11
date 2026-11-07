package greed.client.event;

import net.minecraft.entity.Entity;

public class EntityYawEvent extends TickEvent {
   private Entity entity;
   private float margin;

   public Entity getEntity() {
      return this.entity;
   }

   public float getMargin() {
      return this.margin;
   }

   public EntityYawEvent(Entity entity, float local5162_2) {
      this.entity = entity;
      this.margin = local5162_2;
   }

   public void setEntity(Entity entity) {
      this.entity = entity;
   }

   public void setMargin(float local5164_1) {
      this.margin = local5164_1;
   }
}
