package greed.client.event;


public class ListenerRegistration<T> {
   private final Object owner;
   private final EventListener<T> listener;
   private final int priority;

   public ListenerRegistration(Object initialOwner, EventListener<T> initialListener, int initialPriority) {
      this.owner = initialOwner;
      this.listener = initialListener;
      this.priority = initialPriority;
   }

   public EventListener<T> getListener() {
      return this.listener;
   }

   public int getPriority() {
      return this.priority;
   }

   public Object getOwner() {
      return this.owner;
   }
}
