package greed.client.event;


public class CameraFovEvent extends CancellableEvent {
   private float floatVal;

   public float computeFloat() {
      return this.floatVal;
   }

   public void processData(float local5859_1) {
      this.floatVal = local5859_1;
   }
}
