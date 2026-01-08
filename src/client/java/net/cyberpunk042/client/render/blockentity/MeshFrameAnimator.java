package net.cyberpunk042.client.render.blockentity;


import net.cyberpunk042.log.Logging;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.growth.profile.GrowthGlowProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.metadata.AnimationFrameResourceMetadata;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Handles frame sampling, interpolation, and animated texture generation for mesh mode.
 */
public final class MeshFrameAnimator {
    private static final float DEFAULT_FRAME_TIME_TICKS = 8.0F;
    private static final ConcurrentMap<Identifier, TextureMeta> TEXTURE_META_CACHE = new ConcurrentHashMap<>();
    private static final DynamicTextureCache DYNAMIC_TEXTURE_CACHE = new DynamicTextureCache();

    private MeshFrameAnimator() {}

    /**
     * Resolves the current frame sample for a mesh layer.
     */
    public static FrameSample resolveFrameSample(Identifier texture, float worldTime, float phaseOffset,
            @Nullable GrowthGlowProfile.AnimationOverride override) {
        if (texture == null) {
            return FrameSample.FULL;
        }
        TextureMeta meta = TEXTURE_META_CACHE.computeIfAbsent(texture, MeshFrameAnimator::loadTextureMeta);
        float baseFrameTime = meta.frameTimeTicks() > 0.0F ? meta.frameTimeTicks() : DEFAULT_FRAME_TIME_TICKS;
        float frameTime = baseFrameTime;
        if (override != null && override.frameTimeTicks() != null && override.frameTimeTicks() > 0.0F) {
            frameTime = override.frameTimeTicks();
        }
        // Handle scroll-based animation
        if (override != null && override.scrollSpeed() != null && override.scrollSpeed() != 0.0F) {
            float speed = override.scrollSpeed();
            float offset = (worldTime + phaseOffset * frameTime) * speed;
            GlowQuadEmitter.FrameSlice slice = new GlowQuadEmitter.FrameSlice(0.0F, 1.0F, offset, true);
            return new FrameSample(new FrameVisual(texture, slice), null, 0.0F);
        }
        // Single frame texture
        if (meta.frameCount() <= 1) {
            return new FrameSample(new FrameVisual(texture, GlowQuadEmitter.FrameSlice.FULL), null, 0.0F);
        }
        // Frame-based animation
        List<Integer> frames = null;
        if (override != null && override.hasFrames()) {
            frames = override.frames();
        } else if (meta.hasCustomFrames()) {
            frames = meta.frames();
        }
        float progress = (worldTime + phaseOffset * frameTime) / frameTime;
        int rawIndex = MathHelper.floor(progress);
        float fraction = progress - rawIndex;
        int frameIndex = resolveFrameIndex(rawIndex, frames, meta.frameCount());
        float minV = frameIndex / (float) meta.frameCount();
        float maxV = minV + (1.0F / meta.frameCount());
        boolean interpolate = meta.interpolate();
        if (override != null && override.interpolate() != null) {
            interpolate = override.interpolate();
        }
        if (!interpolate) {
            GlowQuadEmitter.FrameSlice slice = new GlowQuadEmitter.FrameSlice(minV, maxV, 0.0F, false);
            return new FrameSample(new FrameVisual(texture, slice), null, 0.0F);
        }
        // Interpolated animation - use dynamic texture blending
        int nextIndex = resolveFrameIndex(rawIndex + 1, frames, meta.frameCount());
        float blend = MathHelper.clamp(fraction, 0.0F, 1.0F);
        Identifier blendedTexture = DYNAMIC_TEXTURE_CACHE.sampleInterpolatedTexture(texture, meta, frameIndex, nextIndex, blend);
        Identifier resolvedTexture = blendedTexture != null ? blendedTexture : texture;
        return new FrameSample(new FrameVisual(resolvedTexture, GlowQuadEmitter.FrameSlice.FULL), null, 0.0F);
    }

    private static int resolveFrameIndex(int rawIndex, @Nullable List<Integer> frames, int frameCount) {
        if (frames != null && !frames.isEmpty()) {
            int size = frames.size();
            int mapped = rawIndex % size;
            if (mapped < 0) {
                mapped += size;
            }
            return MathHelper.clamp(frames.get(mapped), 0, frameCount - 1);
        }
        int index = rawIndex % frameCount;
        if (index < 0) {
            index += frameCount;
        }
        return index;
    }

    private static TextureMeta loadTextureMeta(Identifier texture) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return TextureMeta.DEFAULT;
        }
        ResourceManager manager = client.getResourceManager();
        try {
            Optional<Resource> optional = manager.getResource(texture);
            if (optional.isEmpty()) {
                return TextureMeta.DEFAULT;
            }
            Resource resource = optional.get();
            ResourceMetadata metadata = resource.getMetadata();
            Optional<AnimationResourceMetadata> animationOpt = metadata.decode(AnimationResourceMetadata.SERIALIZER);
            try (InputStream stream = resource.getInputStream()) {
                NativeImage image = NativeImage.read(stream);
                int width = image.getWidth();
                int height = image.getHeight();
                image.close();
                int frameCount = Math.max(1, height / Math.max(1, width));
                AnimationResourceMetadata animation = animationOpt.orElse(null);
                float frameTime = animation != null ? animation.defaultFrameTime() : DEFAULT_FRAME_TIME_TICKS;
                List<Integer> frames = animation != null && animation.frames().isPresent()
                        ? animation.frames().get().stream().map(AnimationFrameResourceMetadata::index).toList()
                        : List.of();
                boolean interpolate = animation != null && animation.interpolate();
                return new TextureMeta(width, height, frameCount, frameTime, frames, interpolate);
            }
        } catch (IOException ex) {
            Logging.RENDER.warn("[MeshFrameAnimator] Failed to load texture metadata for {}", texture, ex);
            return TextureMeta.DEFAULT;
        }
    }

    @Nullable
    static NativeImage loadNativeImage(Identifier texture) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return null;
        }
        ResourceManager manager = client.getResourceManager();
        try {
            Optional<Resource> optional = manager.getResource(texture);
            if (optional.isEmpty()) {
                return null;
            }
            try (InputStream stream = optional.get().getInputStream()) {
                return NativeImage.read(stream);
            }
        } catch (IOException ex) {
            Logging.RENDER.warn("[MeshFrameAnimator] Failed to load texture image for {}", texture, ex);
            return null;
        }
    }

    // === Inner types ===

    public record FrameSample(FrameVisual primary, @Nullable FrameVisual secondary, float secondaryWeight) {
        public static final FrameSample FULL = new FrameSample(null, null, 0.0F);

        public boolean hasSecondary() {
            return secondary != null && secondaryWeight > 0.0F;
        }
    }

    public record FrameVisual(Identifier texture, GlowQuadEmitter.FrameSlice slice) {}

    public record TextureMeta(int width, int height, int frameCount, float frameTimeTicks, List<Integer> frames, boolean interpolate) {
        public static final TextureMeta DEFAULT = new TextureMeta(16, 16, 1, DEFAULT_FRAME_TIME_TICKS, List.of(), false);

        public boolean hasCustomFrames() {
            return frames != null && !frames.isEmpty();
        }
    }

    /**
     * Cache for dynamically generated interpolated textures.
     */
    private static final class DynamicTextureCache {
        private final ConcurrentMap<Identifier, AnimatedTexture> cache = new ConcurrentHashMap<>();

        @Nullable
        Identifier sampleInterpolatedTexture(Identifier baseTexture, TextureMeta meta, int frameA, int frameB, float weight) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                return null;
            }
            AnimatedTexture animation = cache.computeIfAbsent(baseTexture, id -> AnimatedTexture.create(client, id, meta));
            if (animation == null) {
                return null;
            }
            return animation.sample(frameA, frameB, weight);
        }
    }

    /**
     * Manages a dynamically blended texture for interpolated animation.
     */
    private static final class AnimatedTexture {
        private final Identifier dynamicId;
        private final NativeImage sourceImage;
        private final NativeImage dynamicImage;
        private final NativeImageBackedTexture dynamicTexture;
        private final int frameWidth;
        private final int frameHeight;
        private final int frameCount;
        private int lastFrameA = Integer.MIN_VALUE;
        private int lastFrameB = Integer.MIN_VALUE;
        private float lastWeight = Float.NaN;

        private AnimatedTexture(Identifier dynamicId, NativeImage sourceImage, NativeImage dynamicImage,
                NativeImageBackedTexture dynamicTexture, int frameCount) {
            this.dynamicId = dynamicId;
            this.sourceImage = sourceImage;
            this.dynamicImage = dynamicImage;
            this.dynamicTexture = dynamicTexture;
            this.frameWidth = sourceImage.getWidth();
            this.frameCount = Math.max(1, frameCount);
            this.frameHeight = Math.max(1, sourceImage.getHeight() / this.frameCount);
        }

        static AnimatedTexture create(MinecraftClient client, Identifier baseTexture, TextureMeta meta) {
            NativeImage source = loadNativeImage(baseTexture);
            if (source == null) {
                return null;
            }
            NativeImage dynamic = new NativeImage(source.getFormat(), source.getWidth(),
                    Math.max(1, source.getHeight() / Math.max(1, meta.frameCount())), false);
            Identifier dynamicId = Identifier.of(TheVirusBlock.MOD_ID, "mesh/" + sanitize(baseTexture));
            NativeImageBackedTexture texture = new NativeImageBackedTexture(dynamicId::toString, dynamic);
            client.getTextureManager().registerTexture(dynamicId, texture);
            return new AnimatedTexture(dynamicId, source, dynamic, texture, meta.frameCount());
        }

        Identifier sample(int frameA, int frameB, float weight) {
            int clampedA = Math.floorMod(frameA, frameCount);
            int clampedB = Math.floorMod(frameB, frameCount);
            float clampedWeight = MathHelper.clamp(weight, 0.0F, 1.0F);
            if (!needsUpload(clampedA, clampedB, clampedWeight)) {
                return dynamicId;
            }
            if (clampedWeight <= 0.0F) {
                copyFrame(clampedA);
            } else if (clampedWeight >= 1.0F) {
                copyFrame(clampedB);
            } else {
                blendFrames(clampedA, clampedB, clampedWeight);
            }
            dynamicTexture.upload();
            lastFrameA = clampedA;
            lastFrameB = clampedB;
            lastWeight = clampedWeight;
            return dynamicId;
        }

        private boolean needsUpload(int frameA, int frameB, float weight) {
            return frameA != lastFrameA || frameB != lastFrameB || Math.abs(weight - lastWeight) > 1.0E-4F;
        }

        private void copyFrame(int frameIndex) {
            int sourceY = frameIndex * frameHeight;
            for (int y = 0; y < frameHeight; y++) {
                for (int x = 0; x < frameWidth; x++) {
                    int color = sourceImage.getColorArgb(x, sourceY + y);
                    dynamicImage.setColorArgb(x, y, color);
                }
            }
        }

        private void blendFrames(int frameA, int frameB, float weight) {
            int sourceYA = frameA * frameHeight;
            int sourceYB = frameB * frameHeight;
            for (int y = 0; y < frameHeight; y++) {
                for (int x = 0; x < frameWidth; x++) {
                    int colorA = sourceImage.getColorArgb(x, sourceYA + y);
                    int colorB = sourceImage.getColorArgb(x, sourceYB + y);
                    int blended = blendArgb(colorA, colorB, weight);
                    dynamicImage.setColorArgb(x, y, blended);
                }
            }
        }

        private static int blendArgb(int colorA, int colorB, float weight) {
            int a = blendComponent(getAlpha(colorA), getAlpha(colorB), weight);
            int r = blendComponent(getRed(colorA), getRed(colorB), weight);
            int g = blendComponent(getGreen(colorA), getGreen(colorB), weight);
            int b = blendComponent(getBlue(colorA), getBlue(colorB), weight);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        private static int getAlpha(int color) { return (color >>> 24) & 0xFF; }
        private static int getRed(int color) { return (color >>> 16) & 0xFF; }
        private static int getGreen(int color) { return (color >>> 8) & 0xFF; }
        private static int getBlue(int color) { return color & 0xFF; }

        private static int blendComponent(int a, int b, float weight) {
            return MathHelper.clamp(MathHelper.floor(MathHelper.lerp(weight, a, b) + 0.5F), 0, 255);
        }

        private static String sanitize(Identifier id) {
            return (id.getNamespace() + "_" + id.getPath()).replace(':', '_').replace('/', '_').replace('.', '_');
        }
    }
}
