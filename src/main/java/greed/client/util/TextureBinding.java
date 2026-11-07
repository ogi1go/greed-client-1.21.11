package greed.client.util;

import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;

public record TextureBinding(Identifier id, TextureSetup setup, TextureSetup linearSetup) {
}
