package greed.client.event;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class EventBus<T> implements EventDispatcher {
   private static final EventBus<Object> INSTANCE = new EventBus<>();
   private final Map<Field, Type> eventTypes = new ConcurrentHashMap<>();
   private final Map<Long, EventListener<T>> listenerFieldCache = new ConcurrentHashMap<>();
   private final Map<Type, Set<ListenerRegistration<T>>> registeredListeners = new ConcurrentHashMap<>();
   private final Map<Type, Set<ListenerRegistration<T>>> activeListeners = new ConcurrentHashMap<>();
   private boolean dispatching;
   private boolean rebuildPending;

   public boolean hasListeners(Class<?> local1790_1) {
      Set<ListenerRegistration<T>> local1790_2 = this.activeListeners.get(local1790_1);
      return local1790_2 != null && !local1790_2.isEmpty();
   }

   @Override
   public void subscribe(Object owner) {
      if (owner != null) {
         this.removeOwnerListeners(owner);
         Arrays.stream(owner.getClass().getDeclaredFields())
            .filter(field -> field.getType() == EventListener.class)
            .forEach(
               field2 -> {
                  EventListener<T> listener = this.readListenerField(owner, field2);
                  if (listener != null) {
                     this.registeredListeners
                        .computeIfAbsent(this.resolveEventType(field2), type -> new CopyOnWriteArraySet<>())
                        .add(
                           new ListenerRegistration<>(
                              owner,
                              listener,
                              field2.isAnnotationPresent(EventPriority.class) ? field2.getAnnotation(EventPriority.class).value() : 0
                           )
                        );
                  }
               }
            );
         this.scheduleListenerRebuild();
      }
   }

   @Override
   public void rebuildListeners() {
      this.activeListeners.clear();
      this.registeredListeners
         .forEach(
            (type, local1803_2) -> this.activeListeners
               .put(
                  type,
                  (Set)local1803_2.stream()
                     .sorted(Comparator.<ListenerRegistration<T>>comparingInt(local1799_0 -> local1799_0.getPriority()).reversed())
                     .filter(local1811_0 -> local1811_0.getListener() != null)
                     .collect(Collectors.toCollection(LinkedHashSet::new))
               )
         );
   }

   public static EventBus<Object> getInstance() {
      return INSTANCE;
   }

   private EventListener<T> readListenerField(Object local1798_1, Field field) {
      long local1798_3 = (long)System.identityHashCode(local1798_1) << 32 | field.hashCode() & 4294967295L;
      return this.listenerFieldCache.computeIfAbsent(local1798_3, local1794_2 -> {
         boolean local1794_3 = field.canAccess(local1798_1);
         if (!local1794_3) {
            field.setAccessible(true);
         }

         try {
            if (field.get(local1798_1) instanceof EventListener<?> local1794_5) {
               return (EventListener<T>)local1794_5;
            }
         } catch (IllegalAccessException exception) {
            throw new RuntimeException("Error getting field value", exception);
         } finally {
            field.setAccessible(local1794_3);
         }

         return null;
      });
   }

   @Override
   public void unsubscribe(Object local1800_1) {
      if (local1800_1 != null) {
         this.removeOwnerListeners(local1800_1);
         this.listenerFieldCache.keySet().removeIf(local1789_1 -> local1789_1 >>> 32 == System.identityHashCode(local1800_1));
         this.scheduleListenerRebuild();
      }
   }

   public void post(T local1805_1) {
      if (local1805_1 != null) {
         Set<ListenerRegistration<T>> local1805_2 = this.activeListeners.get(local1805_1.getClass());
         if (local1805_2 != null && !local1805_2.isEmpty()) {
            this.dispatching = true;

            try {
               for (ListenerRegistration<T> local1805_4 : local1805_2) {
                  local1805_4.getListener().onEvent(local1805_1);
               }
            } finally {
               this.dispatching = false;
               if (this.rebuildPending) {
                  this.rebuildPending = false;
                  this.rebuildListeners();
               }
            }
         }
      }
   }

   private void removeOwnerListeners(Object local1807_1) {
      this.registeredListeners.values().forEach(local1796_1 -> local1796_1.removeIf(local1804_1 -> local1804_1.getOwner() == local1807_1));
   }

   private void scheduleListenerRebuild() {
      if (this.dispatching) {
         this.rebuildPending = true;
      } else {
         this.rebuildListeners();
      }
   }

   private Type resolveEventType(Field field) {
      return this.eventTypes.computeIfAbsent(field, field2 -> ((ParameterizedType)field2.getGenericType()).getActualTypeArguments()[0]);
   }
}
