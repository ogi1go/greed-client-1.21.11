package greed.client.render;

import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;

public record TextureReference(Identifier id, TextureSetup setup) {
}
