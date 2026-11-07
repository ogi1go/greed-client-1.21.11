package greed.client.event;

import greed.client.event.Event;
import net.minecraft.entity.Entity;

public class InteractEntityEvent extends CancellableEvent {
    private final Entity entity;
    private boolean cancelled;

    public InteractEntityEvent(Entity entity) {
        this.entity = entity;
        setStage(Stage.PRE);
    }

    public Entity getEntity() {
        return entity;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}