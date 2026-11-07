package greed.client.event;

import greed.client.event.Event;
import net.minecraft.entity.Entity;

public class AttackEvent extends Event {
    private final Entity target;

    public AttackEvent(Entity target) {
        this.target = target;
        setStage(Stage.PRE);
    }

    public Entity getTarget() {
        return target;
    }
}