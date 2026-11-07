package greed.mixin;



import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerInteractionManager.class)
public interface InteractionManagerSlotAccessor {
    @Accessor("lastSelectedSlot")
    void rich$setLastSelectedSlot(int slot);

    @Accessor("lastSelectedSlot")
    int rich$getLastSelectedSlot();
}