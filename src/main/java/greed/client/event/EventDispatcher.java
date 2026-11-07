package greed.client.event;

public interface EventDispatcher {
   void subscribe(Object var1);

   void unsubscribe(Object var1);

   void rebuildListeners();
}
