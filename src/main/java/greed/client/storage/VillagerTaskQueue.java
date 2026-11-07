package greed.client.storage;

import greed.client.event.EventBus;
import greed.client.event.EventListener;
import greed.client.event.PlayerTickEvent;



import java.util.LinkedList;
import java.util.Queue;

public class VillagerTaskQueue {
   private Queue<VillagerTaskQueue.SubContext> queue = new LinkedList<>();
   private final EventListener<PlayerTickEvent> playerTickListener = playerTickEvent -> this.processData2(playerTickEvent);

   public void processData(VillagerTaskQueue.SubContext local4991_1) {
      this.queue.clear();
      this.queue.add(local4991_1);
      this.processData2(new PlayerTickEvent());
   }

   public void updateState() {
      this.queue.clear();
   }

   public void processData2(Object local4994_1) {
      if (this.queue.peek() != null) {
         if (this.queue.peek().matchesCriteria(local4994_1)) {
            this.queue.poll();
         }
      }
   }

   public VillagerTaskQueue() {
      EventBus.getInstance().subscribe(this);
   }

   public static class SubContext {
      private Queue<VillagerTaskQueue.SubContext2<?>> queue2 = new LinkedList<>();
      private int intVal = 0;
      private int intVal2 = 400;

      public boolean matchesCriteria(Object local5000_1) {
         if (this.queue2.peek() == null) {
            return true;
         }

         for (int index = 0;
            index < 64
               && this.queue2.peek() != null
               && this.queue2.peek().valClass.isInstance(local5000_1)
               && this.queue2.peek().matchesCriteria2(local5000_1);
            index++
         ) {
            this.queue2.poll();
            this.intVal = 0;
         }

         if (this.queue2.isEmpty()) {
            return true;
         } else if (++this.intVal > this.intVal2) {
            this.queue2.clear();
            return true;
         } else {
            return false;
         }
      }

      public VillagerTaskQueue.SubContext getNested0471(int local5001_1) {
         this.intVal2 = Math.max(1, local5001_1);
         return this;
      }

      public <E> VillagerTaskQueue.SubContext getNested04712(Class<E> local5002_1, VillagerTaskQueue.SubInterface3<E> local5002_2) {
         this.queue2.add(new VillagerTaskQueue.SubContext2<>(local5002_1, local5002_2));
         return this;
      }
   }

   static class SubContext2<E> {
      private final Class<E> valClass;
      private final VillagerTaskQueue.SubInterface3<E> nested0470;
   
      public SubContext2(Class<E> local4996_1, VillagerTaskQueue.SubInterface3<E> local4996_2) {
         this.valClass = local4996_1;
         this.nested0470 = local4996_2;
      }
   
      boolean matchesCriteria2(Object local4997_1) {
         return this.nested0470.matchesCriteria3(this.valClass.cast(local4997_1));
      }
   }

   public static interface SubInterface3<E> {
      boolean matchesCriteria3(E var1);
   }
}
