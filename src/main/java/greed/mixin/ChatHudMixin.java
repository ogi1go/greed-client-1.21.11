package greed.mixin;

import java.util.List;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChatHud.class)
public interface ChatHudMixin {
   @Accessor("messages")
   List<ChatHudLine> allMessages();

   @Accessor("scrolledLines")
   int chatScrollbarPos();

   @Invoker("refresh")
   void invokeRefreshTrimmedMessages();
}
