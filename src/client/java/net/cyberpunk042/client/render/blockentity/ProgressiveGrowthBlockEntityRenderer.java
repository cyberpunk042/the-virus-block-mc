package net.cyberpunk042.client.render.blockentity;


import net.cyberpunk042.log.Logging;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.block.growth.ProgressiveGrowthBlock;
import net.cyberpunk042.growth.profile.GrowthFieldProfile;
import net.cyberpunk042.growth.profile.GrowthGlowProfile;
import net.cyberpunk042.growth.GrowthBlockDefinition;
import net.cyberpunk042.growth.profile.GrowthOpacityProfile;
import net.cyberpunk042.growth.profile.GrowthSpinProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

/**
 * Renderer for the Progressive Growth Block.
 * <p>
 * Delegates to extracted helper classes:
 * <ul>
 *   <li>{@link GrowthRenderUtils} - Box math, color decoding, spin calculations</li>
 *   <li>{@link GlowQuadEmitter} - Low-level vertex/quad emission</li>
 *   <li>{@link GlowTextureResolver} - Sprite atlas lookups</li>
 *   <li>{@link MeshFrameAnimator} - Frame sampling and interpolation</li>
 *   <li>{@link FieldMeshRenderer} - Field profile rendering</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
@SuppressWarnings("deprecation")
public final class ProgressiveGrowthBlockEntityRenderer implements BlockEntityRenderer<ProgressiveGrowthBlockEntity> {
    private static final int MAX_LIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;
    private static final float MIN_LAYER_ALPHA = 0.01F;
    
    // Logging state
    private static final AtomicBoolean INIT_LOGGED = new AtomicBoolean(false);
    private static final Set<BlockPos> FIRST_RENDER_LOGGED = ConcurrentHashMap.newKeySet();
    private static final Set<BlockPos> PROFILE_DETAIL_LOGGED = ConcurrentHashMap.newKeySet();
    private static final Set<BlockPos> NULL_GLOW_LOGGED = ConcurrentHashMap.newKeySet();
    private static final Set<Identifier> GLOW_FALLBACK_LOGGED = ConcurrentHashMap.newKeySet();
    private static final Set<Identifier> OPACITY_FALLBACK_LOGGED = ConcurrentHashMap.newKeySet();
    private static final Set<Identifier> SPIN_FALLBACK_LOGGED = ConcurrentHashMap.newKeySet();
    private static final ConcurrentMap<LayerKey, LayerSnapshot> LAYER_STATE_LOG = new ConcurrentHashMap<>();

    public ProgressiveGrowthBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        if (INIT_LOGGED.compareAndSet(false, true)) {
            Logging.RENDER.topic("growth").info("[GrowthRenderer] Progressive growth renderer registered");
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.execute(() -> GlowTextureResolver.dumpGlowSprites(client));
            client.execute(() -> GlowTextureResolver.preloadGlowTextures(client));
        }
    }

    @Override
    public void render(ProgressiveGrowthBlockEntity entity, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light, int overlay, Vec3d cameraPos) {
        if (entity.getWorld() == null) {
            return;
        }

        BlockPos pos = entity.getPos();
        float scale = Math.max(0.01F, entity.getRenderScale(tickDelta));
        GrowthGlowProfile glow = entity.resolveGlowProfile();
        if (glow == null) {
            if (NULL_GLOW_LOGGED.add(pos.toImmutable())) {
                Logging.RENDER.topic("growth").warn("[GrowthRenderer] Missing glow profile for {} at {}", entity.getDefinitionId(), pos);
            }
            return;
        }
        matrices.push();
        matrices.translate(0.5D, 0.5D, 0.5D);

        GrowthBlockDefinition definition = entity.definitionSnapshot();
        GrowthOpacityProfile opacity = entity.resolveOpacityProfile();
        GrowthSpinProfile spinProfile = entity.resolveSpinProfile();
        GrowthFieldProfile field = entity.resolveFieldProfile();
        logProfileFallbacks(definition, glow, opacity, spinProfile);
        logProfileDetails(entity, glow, opacity, spinProfile, field);
        float worldTime = entity.getWorld().getTime() + tickDelta;
        Vec3d wobble = entity.getRenderWobble(definition, tickDelta);
        if (wobble.lengthSquared() > 1.0E-5) {
            matrices.translate(wobble.x, wobble.y, wobble.z);
        }

        float fusePulse = entity.isFuseArmed()
                ? (0.35F + 0.35F * MathHelper.sin(worldTime * 0.4F))
                : 0.0F;
        float primaryAlpha = MathHelper.clamp(opacity.primaryLayer().animatedAlpha(worldTime) + fusePulse, 0.0F, 1.0F);
        float secondaryAlpha = MathHelper.clamp(opacity.secondaryLayer().animatedAlpha(worldTime) + fusePulse * 0.8F, 0.0F, 1.0F);

        float[] primaryColor = GrowthRenderUtils.decodeHexColor(glow.primaryColorHex());
        float[] secondaryColor = GrowthRenderUtils.decodeHexColor(glow.secondaryColorHex());
        GrowthSpinProfile.Layer primarySpin = spinProfile.primaryLayer();
        GrowthSpinProfile.Layer secondarySpin = spinProfile.secondaryLayer();
        float primaryRotation = GrowthRenderUtils.computeSpinRotation(worldTime, primarySpin);
        float secondaryRotation = GrowthRenderUtils.computeSpinRotation(worldTime, secondarySpin);

        Box renderBox = entity.getRenderBounds();
        Box outlineBox;
        if (renderBox != null) {
            outlineBox = GrowthRenderUtils.centerBox(renderBox);
        } else {
            VoxelShape outlineShape = entity.shape(ProgressiveGrowthBlock.ShapeType.OUTLINE);
            outlineBox = GrowthRenderUtils.centerBox(outlineShape.getBoundingBox());
        }
        Box innerBox = GrowthRenderUtils.scaleBox(outlineBox, 0.7F);
        
        MeshFrameAnimator.FrameSample primarySample = glow.primaryUseMesh()
                ? MeshFrameAnimator.resolveFrameSample(glow.primaryTexture(), worldTime, 0.0F, glow.primaryMeshAnimation())
                : MeshFrameAnimator.FrameSample.FULL;
        MeshFrameAnimator.FrameSample secondarySample = glow.secondaryUseMesh()
                ? MeshFrameAnimator.resolveFrameSample(glow.secondaryTexture(), worldTime, 0.5F, glow.secondaryMeshAnimation())
                : MeshFrameAnimator.FrameSample.FULL;

        renderGlowLayer(entity, "primary", glow.primaryUseMesh(), glow.primaryTexture(), outlineBox,
                primaryAlpha, primaryColor, primarySpin, primaryRotation, matrices, vertexConsumers, primarySample);
        renderGlowLayer(entity, "secondary", glow.secondaryUseMesh(), glow.secondaryTexture(), innerBox,
                secondaryAlpha, secondaryColor, secondarySpin, secondaryRotation, matrices, vertexConsumers, secondarySample);

        logFirstRender(entity, glow, field, scale);
        if (shouldRenderDebugBounds()) {
            renderDebugOutline(matrices, vertexConsumers, outlineBox);
        }
        if (TheVirusBlock.LOGGER.isDebugEnabled()) {
            Logging.RENDER.topic("growth").debug(
                    "[GrowthRenderer] def={} scale={} glow={} field={} fuse={} primaryAlpha={} secondaryAlpha={}",
                    entity.getDefinitionId(),
                    String.format("%.3f", scale),
                    glow.id(),
                    field != null ? field.id() : "none",
                    entity.isFuseArmed(),
                    String.format("%.2f", primaryAlpha),
                    String.format("%.2f", secondaryAlpha));
        }
        if (field != null) {
            FieldMeshRenderer.renderField(matrices, vertexConsumers, field, scale, worldTime);
        }

        matrices.pop();
    }

    // ========================================================================
    // Layer Rendering
    // ========================================================================

    private void renderGlowLayer(ProgressiveGrowthBlockEntity entity,
            String layerName,
            boolean useMesh,
            Identifier texture,
            Box box,
            float alpha,
            float[] color,
            GrowthSpinProfile.Layer spin,
            float rotation,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            MeshFrameAnimator.FrameSample frame) {

        if (useMesh) {
            if (texture == null) {
                logLayerOutcome(entity, layerName, LayerMode.MESH_NO_TEXTURE, alpha, null, box, "Texture missing");
                return;
            }
            if (alpha <= MIN_LAYER_ALPHA) {
                logLayerOutcome(entity, layerName, LayerMode.MESH_SKIP_ALPHA, alpha, texture, box,
                        "Alpha below visibility threshold");
                return;
            }
            renderMeshLayer(box, alpha, color, spin, rotation, matrices, vertexConsumers, frame);
            logLayerOutcome(entity, layerName, LayerMode.MESH_RENDER, alpha, texture, box,
                    GrowthRenderUtils.formatSpinDetail(spin, rotation, color));
            return;
        }

        if (texture == null) {
            logLayerOutcome(entity, layerName, LayerMode.BLOCK_NO_TEXTURE, alpha, null, box, "Texture missing");
            return;
        }
        if (alpha <= MIN_LAYER_ALPHA) {
            logLayerOutcome(entity, layerName, LayerMode.BLOCK_SKIP_ALPHA, alpha, texture, box,
                    "Alpha below visibility threshold");
            return;
        }
        if (!renderNativeLayer(texture, box, alpha, color, matrices, vertexConsumers)) {
            logLayerOutcome(entity, layerName, LayerMode.BLOCK_NO_TEXTURE, alpha, texture, box, "Sprite missing");
            return;
        }
        logLayerOutcome(entity, layerName, LayerMode.BLOCK_RENDER, alpha, texture, box, "native");
    }

    private static void renderMeshLayer(Box box, float alpha, float[] color,
            GrowthSpinProfile.Layer spin, float rotation, MatrixStack matrices, VertexConsumerProvider vertices,
            MeshFrameAnimator.FrameSample frame) {
        if (alpha <= MIN_LAYER_ALPHA || frame.primary() == null || frame.primary().texture() == null) {
            return;
        }
        matrices.push();
        if (rotation != 0.0F) {
            matrices.multiply(GrowthRenderUtils.rotationAxis(spin).rotation(rotation));
        }
        GlowQuadEmitter.renderCubeLayer(matrices, vertices, frame.primary().texture(), box, alpha, color, frame.primary().slice());
        if (frame.hasSecondary()) {
            float secondaryAlpha = alpha * MathHelper.clamp(frame.secondaryWeight(), 0.0F, 1.0F);
            if (secondaryAlpha > MIN_LAYER_ALPHA && frame.secondary().texture() != null) {
                GlowQuadEmitter.renderCubeLayer(matrices, vertices, frame.secondary().texture(), box, secondaryAlpha, color, frame.secondary().slice());
            }
        }
        matrices.pop();
    }

    private static boolean renderNativeLayer(Identifier texture, Box box, float alpha, float[] color, MatrixStack matrices,
            VertexConsumerProvider vertices) {
        if (texture == null || alpha <= MIN_LAYER_ALPHA) {
            return false;
        }
        Sprite sprite = GlowTextureResolver.resolveSprite(texture);
        if (sprite == null) {
            Logging.RENDER.topic("growth").warn("[GrowthRenderer] Native sprite missing for {}", texture);
            return false;
        }
        matrices.push();
        VertexConsumer consumer = sprite.getTextureSpecificVertexConsumer(
                vertices.getBuffer(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)));
        GlowQuadEmitter.renderCubeWithSprite(matrices.peek(), consumer, box, alpha, sprite, color);
        matrices.pop();
        return true;
    }

    // ========================================================================
    // Debug Rendering
    // ========================================================================

    private static boolean shouldRenderDebugBounds() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.getEntityRenderDispatcher().shouldRenderHitboxes();
    }

    private static void renderDebugOutline(MatrixStack matrices, VertexConsumerProvider vertices, Box box) {
        VertexConsumer consumer = vertices.getBuffer(RenderLayer.getLines());
        MatrixStack.Entry entry = matrices.peek();
        Box expanded = box.expand(0.002D);
        drawLine(entry, consumer, expanded.minX, expanded.minY, expanded.minZ, expanded.maxX, expanded.minY, expanded.minZ);
        drawLine(entry, consumer, expanded.maxX, expanded.minY, expanded.minZ, expanded.maxX, expanded.minY, expanded.maxZ);
        drawLine(entry, consumer, expanded.maxX, expanded.minY, expanded.maxZ, expanded.minX, expanded.minY, expanded.maxZ);
        drawLine(entry, consumer, expanded.minX, expanded.minY, expanded.maxZ, expanded.minX, expanded.minY, expanded.minZ);
        drawLine(entry, consumer, expanded.minX, expanded.maxY, expanded.minZ, expanded.maxX, expanded.maxY, expanded.minZ);
        drawLine(entry, consumer, expanded.maxX, expanded.maxY, expanded.minZ, expanded.maxX, expanded.maxY, expanded.maxZ);
        drawLine(entry, consumer, expanded.maxX, expanded.maxY, expanded.maxZ, expanded.minX, expanded.maxY, expanded.maxZ);
        drawLine(entry, consumer, expanded.minX, expanded.maxY, expanded.maxZ, expanded.minX, expanded.maxY, expanded.minZ);
        drawLine(entry, consumer, expanded.minX, expanded.minY, expanded.minZ, expanded.minX, expanded.maxY, expanded.minZ);
        drawLine(entry, consumer, expanded.maxX, expanded.minY, expanded.minZ, expanded.maxX, expanded.maxY, expanded.minZ);
        drawLine(entry, consumer, expanded.maxX, expanded.minY, expanded.maxZ, expanded.maxX, expanded.maxY, expanded.maxZ);
        drawLine(entry, consumer, expanded.minX, expanded.minY, expanded.maxZ, expanded.minX, expanded.maxY, expanded.maxZ);
    }

    private static void drawLine(MatrixStack.Entry entry, VertexConsumer consumer,
            double x1, double y1, double z1,
            double x2, double y2, double z2) {
        consumer.vertex(entry.getPositionMatrix(), (float) x1, (float) y1, (float) z1)
                .color(1.0F, 0.25F, 0.25F, 1.0F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(MAX_LIGHT)
                .normal(entry, 0.0F, 1.0F, 0.0F);
        consumer.vertex(entry.getPositionMatrix(), (float) x2, (float) y2, (float) z2)
                .color(1.0F, 0.25F, 0.25F, 1.0F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(MAX_LIGHT)
                .normal(entry, 0.0F, 1.0F, 0.0F);
    }

    // ========================================================================
    // Logging
    // ========================================================================

    private static void logProfileFallbacks(GrowthBlockDefinition definition, GrowthGlowProfile glow, GrowthOpacityProfile opacity, GrowthSpinProfile spin) {
        if (definition.glowProfileId() != null && !definition.glowProfileId().equals(glow.id())
                && GLOW_FALLBACK_LOGGED.add(definition.glowProfileId())) {
            Logging.RENDER.topic("growth").warn("[GrowthRenderer] Glow profile {} missing, fallback {}", definition.glowProfileId(), glow.id());
        }
        if (definition.opacityProfileId() != null && !definition.opacityProfileId().equals(opacity.id())
                && OPACITY_FALLBACK_LOGGED.add(definition.opacityProfileId())) {
            Logging.RENDER.topic("growth").warn("[GrowthRenderer] Opacity profile {} missing, fallback {}", definition.opacityProfileId(), opacity.id());
        }
        if (definition.spinProfileId() != null && !definition.spinProfileId().equals(spin.id())
                && SPIN_FALLBACK_LOGGED.add(definition.spinProfileId())) {
            Logging.RENDER.topic("growth").warn("[GrowthRenderer] Spin profile {} missing, fallback {}", definition.spinProfileId(), spin.id());
        }
    }

    private static void logFirstRender(ProgressiveGrowthBlockEntity entity, GrowthGlowProfile glow, GrowthFieldProfile field, float scale) {
        BlockPos pos = entity.getPos();
        if (!FIRST_RENDER_LOGGED.add(pos.toImmutable())) {
            return;
        }
        Logging.RENDER.topic("growth").info(
                "[GrowthRenderer] First render {} def={} glow={} field={} scale={}",
                pos,
                entity.getDefinitionId(),
                glow.id(),
                field != null ? field.id() : "none",
                String.format("%.3f", scale));
    }

    private static void logProfileDetails(ProgressiveGrowthBlockEntity entity, GrowthGlowProfile glow, GrowthOpacityProfile opacity,
            GrowthSpinProfile spin, GrowthFieldProfile field) {
        BlockPos pos = entity.getPos().toImmutable();
        if (!PROFILE_DETAIL_LOGGED.add(pos)) {
            return;
        }
        Logging.RENDER.topic("growth").info(
                "[GrowthRenderer] Profiles {} def={} glow={} primaryTex={} primaryUseMesh={} secondaryTex={} secondaryUseMesh={}",
                pos,
                entity.getDefinitionId(),
                glow.id(),
                GrowthRenderUtils.describeId(glow.primaryTexture()),
                glow.primaryUseMesh(),
                GrowthRenderUtils.describeId(glow.secondaryTexture()),
                glow.secondaryUseMesh());
        if (opacity != null) {
            Logging.RENDER.topic("growth").info(
                    "[GrowthRenderer] Profiles {} opacity={} primary({}) secondary({})",
                    pos,
                    opacity.id(),
                    GrowthRenderUtils.formatOpacityLayer(opacity.primaryLayer()),
                    GrowthRenderUtils.formatOpacityLayer(opacity.secondaryLayer()));
        }
        if (spin != null) {
            Logging.RENDER.topic("growth").info(
                    "[GrowthRenderer] Profiles {} spin={} primary({}) secondary({})",
                    pos,
                    spin.id(),
                    GrowthRenderUtils.formatSpinLayer(spin.primaryLayer()),
                    GrowthRenderUtils.formatSpinLayer(spin.secondaryLayer()));
        }
        if (field != null) {
            Logging.RENDER.topic("growth").info(
                    "[GrowthRenderer] Profiles {} field={} mesh={} alpha={} spin={} scale={} texture={} color={}",
                    pos,
                    field.id(),
                    field.meshType(),
                    String.format("%.3f", field.clampedAlpha()),
                    String.format("%.3f", field.spinSpeed()),
                    String.format("%.3f", field.clampedScaleMultiplier()),
                    GrowthRenderUtils.describeId(field.texture()),
                    GrowthRenderUtils.formatColor(field.decodedColor()));
        }
    }

    private void logLayerOutcome(ProgressiveGrowthBlockEntity entity,
            String layerName,
            LayerMode mode,
            float alpha,
            Identifier texture,
            Box box,
            String detail) {
        BlockPos pos = entity.getPos().toImmutable();
        LayerKey key = new LayerKey(pos, layerName);
        String textureId = GrowthRenderUtils.describeId(texture);
        LayerSnapshot snapshot = new LayerSnapshot(mode, textureId);
        LayerSnapshot previous = LAYER_STATE_LOG.put(key, snapshot);
        if (snapshot.equals(previous)) {
            return;
        }
        Logging.RENDER.topic("growth").info(
                "[GrowthRenderer] Layer {} pos={} def={} mode={} alpha={} texture={} box={} detail={}",
                layerName,
                pos,
                entity.getDefinitionId(),
                mode,
                String.format("%.3f", alpha),
                textureId,
                GrowthRenderUtils.formatBox(box),
                detail);
    }

    // ========================================================================
    // Inner Types
    // ========================================================================

    private enum LayerMode {
        MESH_RENDER,
        MESH_SKIP_ALPHA,
        MESH_NO_TEXTURE,
        BLOCK_RENDER,
        BLOCK_SKIP_ALPHA,
        BLOCK_NO_TEXTURE
    }

    private record LayerKey(BlockPos pos, String layer) {}

    private record LayerSnapshot(LayerMode mode, String textureId) {}
}
