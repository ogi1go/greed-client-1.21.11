package greed.client.ui.hud;

import greed.client.util.Animation;

public abstract class DraggableHudElement {
   public final Animation visibilityAnimation = new Animation();
   public boolean visible;
   public float yOffset;
   public int sortIndex;
   public boolean initialized;
}
