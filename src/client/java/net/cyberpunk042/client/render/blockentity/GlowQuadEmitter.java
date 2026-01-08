package net.cyberpunk042.client.render.blockentity;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

/**
 * Handles low-level quad and cube emission for glow rendering.
 */
public final class GlowQuadEmitter {
    private static final int MAX_LIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;

    private GlowQuadEmitter() {}

    /**
     * Renders a cube with the given texture and frame slice (for mesh mode).
     */
    public static void renderCubeLayer(MatrixStack matrices, VertexConsumerProvider vertices,
            Identifier texture, Box box, float alpha, float[] color, FrameSlice frame) {
        if (texture == null || alpha <= 0.01F) {
            return;
        }
        VertexConsumer consumer = vertices.getBuffer(RenderLayer.getEntityTranslucent(texture));
        MatrixStack.Entry entry = matrices.peek();
        int a = MathHelper.clamp((int) (alpha * 255.0F), 0, 255);

        float minX = (float) box.minX;
        float maxX = (float) box.maxX;
        float minY = (float) box.minY;
        float maxY = (float) box.maxY;
        float minZ = (float) box.minZ;
        float maxZ = (float) box.maxZ;

        // Front (+Z)
        emitQuad(entry, consumer,
                minX, minY, maxZ, 0.0F, mapV(1.0F, frame),
                maxX, minY, maxZ, 1.0F, mapV(1.0F, frame),
                maxX, maxY, maxZ, 1.0F, mapV(0.0F, frame),
                minX, maxY, maxZ, 0.0F, mapV(0.0F, frame),
                0.0F, 0.0F, 1.0F, color, a);

        // Back (-Z)
        emitQuad(entry, consumer,
                maxX, minY, minZ, 0.0F, mapV(1.0F, frame),
                minX, minY, minZ, 1.0F, mapV(1.0F, frame),
                minX, maxY, minZ, 1.0F, mapV(0.0F, frame),
                maxX, maxY, minZ, 0.0F, mapV(0.0F, frame),
                0.0F, 0.0F, -1.0F, color, a);

        // Right (+X)
        emitQuad(entry, consumer,
                maxX, minY, maxZ, 0.0F, mapV(1.0F, frame),
                maxX, minY, minZ, 1.0F, mapV(1.0F, frame),
                maxX, maxY, minZ, 1.0F, mapV(0.0F, frame),
                maxX, maxY, maxZ, 0.0F, mapV(0.0F, frame),
                1.0F, 0.0F, 0.0F, color, a);

        // Left (-X)
        emitQuad(entry, consumer,
                minX, minY, minZ, 0.0F, mapV(1.0F, frame),
                minX, minY, maxZ, 1.0F, mapV(1.0F, frame),
                minX, maxY, maxZ, 1.0F, mapV(0.0F, frame),
                minX, maxY, minZ, 0.0F, mapV(0.0F, frame),
                -1.0F, 0.0F, 0.0F, color, a);

        // Top (+Y)
        emitQuad(entry, consumer,
                minX, maxY, maxZ, 0.0F, mapV(1.0F, frame),
                maxX, maxY, maxZ, 1.0F, mapV(1.0F, frame),
                maxX, maxY, minZ, 1.0F, mapV(0.0F, frame),
                minX, maxY, minZ, 0.0F, mapV(0.0F, frame),
                0.0F, 1.0F, 0.0F, color, a);

        // Bottom (-Y)
        emitQuad(entry, consumer,
                minX, minY, minZ, 0.0F, mapV(1.0F, frame),
                maxX, minY, minZ, 1.0F, mapV(1.0F, frame),
                maxX, minY, maxZ, 1.0F, mapV(0.0F, frame),
                minX, minY, maxZ, 0.0F, mapV(0.0F, frame),
                0.0F, -1.0F, 0.0F, color, a);
    }

    /**
     * Renders a cube using a sprite from the atlas (for native mode).
     */
    public static void renderCubeWithSprite(MatrixStack.Entry entry, VertexConsumer consumer,
            Box box, float alpha, Sprite sprite, float[] color) {
        float minX = (float) box.minX;
        float maxX = (float) box.maxX;
        float minY = (float) box.minY;
        float maxY = (float) box.maxY;
        float minZ = (float) box.minZ;
        float maxZ = (float) box.maxZ;
        float minU = sprite.getFrameU(0.0F);
        float maxU = sprite.getFrameU(1.0F);
        float minV = sprite.getFrameV(0.0F);
        float maxV = sprite.getFrameV(1.0F);
        int alphaInt = MathHelper.clamp((int) (alpha * 255.0F), 0, 255);

        // Front (+Z)
        emitQuad(entry, consumer,
                minX, minY, maxZ, minU, maxV,
                maxX, minY, maxZ, maxU, maxV,
                maxX, maxY, maxZ, maxU, minV,
                minX, maxY, maxZ, minU, minV,
                0.0F, 0.0F, 1.0F, color, alphaInt);

        // Back (-Z)
        emitQuad(entry, consumer,
                maxX, minY, minZ, minU, maxV,
                minX, minY, minZ, maxU, maxV,
                minX, maxY, minZ, maxU, minV,
                maxX, maxY, minZ, minU, minV,
                0.0F, 0.0F, -1.0F, color, alphaInt);

        // Right (+X)
        emitQuad(entry, consumer,
                maxX, minY, maxZ, minU, maxV,
                maxX, minY, minZ, maxU, maxV,
                maxX, maxY, minZ, maxU, minV,
                maxX, maxY, maxZ, minU, minV,
                1.0F, 0.0F, 0.0F, color, alphaInt);

        // Left (-X)
        emitQuad(entry, consumer,
                minX, minY, minZ, minU, maxV,
                minX, minY, maxZ, maxU, maxV,
                minX, maxY, maxZ, maxU, minV,
                minX, maxY, minZ, minU, minV,
                -1.0F, 0.0F, 0.0F, color, alphaInt);

        // Top (+Y)
        emitQuad(entry, consumer,
                minX, maxY, maxZ, minU, maxV,
                maxX, maxY, maxZ, maxU, maxV,
                maxX, maxY, minZ, maxU, minV,
                minX, maxY, minZ, minU, minV,
                0.0F, 1.0F, 0.0F, color, alphaInt);

        // Bottom (-Y)
        emitQuad(entry, consumer,
                minX, minY, minZ, minU, maxV,
                maxX, minY, minZ, maxU, maxV,
                maxX, minY, maxZ, maxU, minV,
                minX, minY, maxZ, minU, minV,
                0.0F, -1.0F, 0.0F, color, alphaInt);
    }

    /**
     * Emits a single quad with four vertices.
     */
    public static void emitQuad(MatrixStack.Entry entry, VertexConsumer consumer,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            float x4, float y4, float z4, float u4, float v4,
            float normalX, float normalY, float normalZ,
            float[] color, int alpha) {
        emitVertex(entry, consumer, x1, y1, z1, u1, v1, color, alpha, normalX, normalY, normalZ);
        emitVertex(entry, consumer, x2, y2, z2, u2, v2, color, alpha, normalX, normalY, normalZ);
        emitVertex(entry, consumer, x3, y3, z3, u3, v3, color, alpha, normalX, normalY, normalZ);
        emitVertex(entry, consumer, x4, y4, z4, u4, v4, color, alpha, normalX, normalY, normalZ);
    }

    /**
     * Emits a single vertex.
     */
    public static void emitVertex(MatrixStack.Entry entry, VertexConsumer consumer,
            float x, float y, float z, float u, float v, float[] color, int alpha,
            float normalX, float normalY, float normalZ) {
        int r = MathHelper.clamp((int) (color[0] * 255.0F), 0, 255);
        int g = MathHelper.clamp((int) (color[1] * 255.0F), 0, 255);
        int b = MathHelper.clamp((int) (color[2] * 255.0F), 0, 255);
        consumer.vertex(entry.getPositionMatrix(), x, y, z)
                .color(r, g, b, alpha)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(MAX_LIGHT)
                .normal(entry, normalX, normalY, normalZ);
    }

    /**
     * Maps a V coordinate based on the frame slice for scrolling/frame animation.
     */
    public static float mapV(float originalV, FrameSlice slice) {
        float value = MathHelper.lerp(originalV, slice.minV(), slice.maxV()) + slice.scrollOffset();
        if (slice.wrap()) {
            float offsetBase = MathHelper.floor(slice.scrollOffset());
            return MathHelper.lerp(originalV, slice.minV(), slice.maxV()) + (slice.scrollOffset() - offsetBase);
        }
        return value;
    }

    /**
     * Represents a slice of a texture strip for frame animation.
     */
    public record FrameSlice(float minV, float maxV, float scrollOffset, boolean wrap) {
        public static final FrameSlice FULL = new FrameSlice(0.0F, 1.0F, 0.0F, false);
    }
}
