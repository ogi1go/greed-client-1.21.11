package greed.client.setting;

import java.util.function.Supplier;

public abstract class Setting<T extends Setting<T>> {
   public final String name;
   public final String description;
   private Supplier<Boolean> visibility = () -> Boolean.TRUE;
   private Runnable changeListener;

   public T onChange(Runnable newChangeListener) {
      this.changeListener = newChangeListener;
      return this.self();
   }

   protected Setting(String initialName) {
      this(initialName, "");
   }

   public boolean isVisible() {
      Boolean local1553_1 = this.visibility.get();
      return local1553_1 == null || local1553_1;
   }

   protected final T self() {
      return (T)(Object)this;
   }

   public T visibleWhen(Supplier<Boolean> newVisibility) {
      return this.withVisibility(newVisibility);
   }

   protected void notifyChanged() {
      if (this.changeListener != null) {
         this.changeListener.run();
      }
   }

   protected Setting(String initialName, String local1557_2) {
      this.name = initialName;
      this.description = local1557_2 == null ? "" : local1557_2;
   }

   public T withVisibility(Supplier<Boolean> newVisibility) {
      if (newVisibility != null) {
         this.visibility = newVisibility;
      }

      return this.self();
   }
}
