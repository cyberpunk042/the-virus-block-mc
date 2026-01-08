package net.cyberpunk042.client.render.blockentity;


import net.cyberpunk042.log.Logging;
import java.util.List;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Handles sprite atlas lookups and texture preloading for glow rendering.
 */
@SuppressWarnings("deprecation")
public final class GlowTextureResolver {
    private GlowTextureResolver() {}

    /**
     * Resolves a texture path to a sprite from the block atlas.
     */
    @Nullable
    public static Sprite resolveSprite(Identifier texture) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || texture == null) {
            return null;
        }
        Identifier spriteId = toSpriteId(texture);
        Sprite sprite = client.getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(spriteId);
        if (sprite.getContents().getId().getPath().equals("missingno")) {
            Logging.RENDER.warn("[GlowTextureResolver] Atlas returned missing sprite for {} -> {}", texture, spriteId);
        }
        return sprite;
    }

    /**
     * Dumps all glow sprites to the log for debugging.
     */
    public static void dumpGlowSprites(MinecraftClient client) {
        List<Identifier> glowTextures = glowTextureIds();
        for (Identifier id : glowTextures) {
            Sprite sprite = client.getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(id);
            Logging.RENDER.info("[GlowTextureResolver] Atlas contains {} -> {}", id, sprite.getContents().getId());
        }
    }

    /**
     * Preloads glow textures into the texture manager for mesh mode.
     */
    public static void preloadGlowTextures(MinecraftClient client) {
        glowTextureResources().forEach(id -> {
            client.getTextureManager().registerTexture(id, new ResourceTexture(id));
        });
    }

    /**
     * Converts a texture path to a sprite ID (strips textures/ prefix and .png suffix).
     */
    public static Identifier toSpriteId(Identifier texture) {
        String path = texture.getPath();
        if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - 4);
        }
        return Identifier.of(texture.getNamespace(), path);
    }

    /**
     * Returns the list of glow texture sprite IDs (without textures/ prefix).
     */
    public static List<Identifier> glowTextureIds() {
        return List.of(
                Identifier.of(TheVirusBlock.MOD_ID, "block/glow_magma_primary"),
                Identifier.of(TheVirusBlock.MOD_ID, "block/glow_magma_secondary"),
                Identifier.of(TheVirusBlock.MOD_ID, "block/glow_lava_primary"),
                Identifier.of(TheVirusBlock.MOD_ID, "block/glow_lava_secondary"),
                Identifier.of(TheVirusBlock.MOD_ID, "block/glow_glowstone_primary"),
                Identifier.of(TheVirusBlock.MOD_ID, "block/glow_glowstone_secondary"),
                Identifier.of(TheVirusBlock.MOD_ID, "block/glow_beam_primary"),
                Identifier.of(TheVirusBlock.MOD_ID, "block/glow_beam_secondary"));
    }

    /**
     * Returns the list of glow texture resource paths (with textures/ prefix).
     */
    public static List<Identifier> glowTextureResources() {
        return List.of(
                Identifier.of(TheVirusBlock.MOD_ID, "textures/block/glow_magma_primary.png"),
                Identifier.of(TheVirusBlock.MOD_ID, "textures/block/glow_magma_secondary.png"),
                Identifier.of(TheVirusBlock.MOD_ID, "textures/block/glow_lava_primary.png"),
                Identifier.of(TheVirusBlock.MOD_ID, "textures/block/glow_lava_secondary.png"),
                Identifier.of(TheVirusBlock.MOD_ID, "textures/block/glow_glowstone_primary.png"),
                Identifier.of(TheVirusBlock.MOD_ID, "textures/block/glow_glowstone_secondary.png"),
                Identifier.of(TheVirusBlock.MOD_ID, "textures/block/glow_beam_primary.png"),
                Identifier.of(TheVirusBlock.MOD_ID, "textures/block/glow_beam_secondary.png"));
    }
}
