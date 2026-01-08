package net.cyberpunk042.client.render;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.mixin.client.SpriteContentsAccessor;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

/**
 * Handles replacing vanilla fire textures with corrupted variants at Tier 2
 * without triggering full resource reloads.
 */
@SuppressWarnings("deprecation")
public final class CorruptedFireTextures implements SimpleSynchronousResourceReloadListener {
	private static final Identifier ATLAS = SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;

	private static final Identifier FIRE_0 = Identifier.ofVanilla("block/fire_0");
	private static final Identifier FIRE_1 = Identifier.ofVanilla("block/fire_1");
	private static final Identifier SOUL_FIRE_0 = Identifier.ofVanilla("block/soul_fire_0");
	private static final Identifier SOUL_FIRE_1 = Identifier.ofVanilla("block/soul_fire_1");

	private static final Identifier CORRUPTED_FIRE_0 = Identifier.of(TheVirusBlock.MOD_ID, "block/fire_0");
	private static final Identifier CORRUPTED_FIRE_1 = Identifier.of(TheVirusBlock.MOD_ID, "block/fire_1");
	private static final Identifier CORRUPTED_SOUL_FIRE_0 = Identifier.of(TheVirusBlock.MOD_ID, "block/soul_fire_0");
	private static final Identifier CORRUPTED_SOUL_FIRE_1 = Identifier.of(TheVirusBlock.MOD_ID, "block/soul_fire_1");

	private static boolean corruptedActive;
	private static Sprite fire0;
	private static Sprite fire1;
	private static Sprite soulFire0;
	private static Sprite soulFire1;
	private static Sprite corruptedFire0;
	private static Sprite corruptedFire1;
	private static Sprite corruptedSoulFire0;
	private static Sprite corruptedSoulFire1;

	private static NativeImage[] fire0Backup;
	private static NativeImage[] fire1Backup;
	private static NativeImage[] soulFire0Backup;
	private static NativeImage[] soulFire1Backup;

	private CorruptedFireTextures() {
	}

	public static void bootstrap() {
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new CorruptedFireTextures());
	}

	@Override
	public Identifier getFabricId() {
		return Identifier.of(TheVirusBlock.MOD_ID, "corrupted_fire_textures");
	}

	@Override
	public void reload(ResourceManager manager) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return;
		}
		SpriteAtlasTexture atlas = client.getBakedModelManager().getAtlas(ATLAS);
		disposeBackups();
		fire0 = atlas.getSprite(FIRE_0);
		fire1 = atlas.getSprite(FIRE_1);
		soulFire0 = atlas.getSprite(SOUL_FIRE_0);
		soulFire1 = atlas.getSprite(SOUL_FIRE_1);
		corruptedFire0 = atlas.getSprite(CORRUPTED_FIRE_0);
		corruptedFire1 = atlas.getSprite(CORRUPTED_FIRE_1);
		corruptedSoulFire0 = atlas.getSprite(CORRUPTED_SOUL_FIRE_0);
		corruptedSoulFire1 = atlas.getSprite(CORRUPTED_SOUL_FIRE_1);
		fire0Backup = backupSprite(fire0);
		fire1Backup = backupSprite(fire1);
		soulFire0Backup = backupSprite(soulFire0);
		soulFire1Backup = backupSprite(soulFire1);

		client.execute(() -> {
			if (corruptedActive) {
				applyCorrupted();
			} else {
				applyOriginal();
			}
		});
	}

	public static void setCorrupted(boolean active) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			corruptedActive = active;
			return;
		}
		client.execute(() -> {
			if (corruptedActive == active) {
				return;
			}
			if (active) {
				if (applyCorrupted()) {
					corruptedActive = true;
				}
			} else {
				if (applyOriginal()) {
					corruptedActive = false;
				}
			}
		});
	}

	private static boolean applyCorrupted() {
		if (!spritesReady()) {
			return false;
		}
		copySprite(corruptedFire0, fire0);
		copySprite(corruptedFire1, fire1);
		copySprite(corruptedSoulFire0, soulFire0);
		copySprite(corruptedSoulFire1, soulFire1);
		return true;
	}

	private static boolean applyOriginal() {
		if (!spritesReady()) {
			return false;
		}
		restoreSprite(fire0Backup, fire0);
		restoreSprite(fire1Backup, fire1);
		restoreSprite(soulFire0Backup, soulFire0);
		restoreSprite(soulFire1Backup, soulFire1);
		return true;
	}

	private static boolean spritesReady() {
		return fire0 != null && fire1 != null && soulFire0 != null && soulFire1 != null
				&& fire0Backup != null && fire1Backup != null && soulFire0Backup != null && soulFire1Backup != null;
	}

	@SuppressWarnings("resource")
	private static NativeImage[] backupSprite(Sprite sprite) {
		if (sprite == null) {
			return null;
		}
		NativeImage[] source = ((SpriteContentsAccessor) sprite.getContents()).theVirusBlock$getMipmapImages();
		NativeImage[] backup = new NativeImage[source.length];
		for (int i = 0; i < source.length; i++) {
			NativeImage src = source[i];
			NativeImage copy = new NativeImage(src.getFormat(), src.getWidth(), src.getHeight(), false);
			copy.copyFrom(src);
			backup[i] = copy;
		}
		return backup;
	}

	private static void disposeBackups() {
		closeImages(fire0Backup);
		closeImages(fire1Backup);
		closeImages(soulFire0Backup);
		closeImages(soulFire1Backup);
		fire0Backup = fire1Backup = soulFire0Backup = soulFire1Backup = null;
	}

	private static void closeImages(NativeImage[] images) {
		if (images == null) {
			return;
		}
		for (NativeImage image : images) {
			if (image != null) {
				image.close();
			}
		}
	}

	private static void restoreSprite(NativeImage[] backup, Sprite target) {
		if (backup == null || target == null) {
			return;
		}
		NativeImage[] targetImages = ((SpriteContentsAccessor) target.getContents()).theVirusBlock$getMipmapImages();
		for (int i = 0; i < targetImages.length && i < backup.length; i++) {
			targetImages[i].copyFrom(backup[i]);
		}
		upload(target);
	}

	private static void copySprite(Sprite source, Sprite target) {
		if (source == null || target == null) {
			return;
		}
		NativeImage[] sourceImages = ((SpriteContentsAccessor) source.getContents()).theVirusBlock$getMipmapImages();
		NativeImage[] targetImages = ((SpriteContentsAccessor) target.getContents()).theVirusBlock$getMipmapImages();
		for (int i = 0; i < targetImages.length && i < sourceImages.length; i++) {
			targetImages[i].copyFrom(sourceImages[i]);
		}
		upload(target);
	}

	private static void upload(Sprite sprite) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			return;
		}
		SpriteAtlasTexture atlas = client.getBakedModelManager().getAtlas(sprite.getAtlasId());
		sprite.upload(atlas.getGlTexture());
	}
}

