package net.cyberpunk042.client.render.blockentity;

import net.cyberpunk042.growth.profile.GrowthFieldProfile;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Handles rendering of field profiles (sphere, ring, billboard meshes).
 */
public final class FieldMeshRenderer {
    private static final int MAX_LIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;

    private FieldMeshRenderer() {}

    /**
     * Renders the field profile based on its mesh type.
     */
    public static void renderField(MatrixStack matrices, VertexConsumerProvider vertices,
            GrowthFieldProfile field, float baseScale, float worldTime) {
        float alpha = field.clampedAlpha();
        if (alpha <= 0.01F) {
            return;
        }
        float scale = baseScale * field.clampedScaleMultiplier();
        String mesh = field.meshType() != null ? field.meshType().toLowerCase() : "sphere";
        float[] color = field.decodedColor();
        switch (mesh) {
            case "ring":
                renderRingField(matrices, vertices, field.texture(), scale, alpha, field.spinSpeed(), worldTime, color);
                break;
            case "sphere":
            case "shell":
            default:
                renderBillboardLayer(matrices, vertices, field.texture(), scale, alpha, field.spinSpeed(), worldTime, color);
                break;
        }
    }

    /**
     * Renders a billboard (4-sided spinning quad) layer.
     */
    public static void renderBillboardLayer(MatrixStack matrices, VertexConsumerProvider vertices,
            Identifier texture, float scale, float alpha, float spinSpeed, float worldTime, float[] color) {
        if (texture == null || alpha <= 0.01F) {
            return;
        }
        VertexConsumer consumer = vertices.getBuffer(RenderLayer.getEntityTranslucent(texture));
        matrices.push();
        matrices.scale(scale, scale, scale);
        float rotation = worldTime * 0.02F * spinSpeed;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rotation));

        for (int i = 0; i < 4; i++) {
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float) (Math.PI / 2 * i)));
            renderBillboardQuad(matrices.peek(), consumer, alpha, color);
            matrices.pop();
        }
        matrices.pop();
    }

    /**
     * Renders a ring field mesh.
     */
    public static void renderRingField(MatrixStack matrices, VertexConsumerProvider vertices,
            Identifier texture, float radius, float alpha, float spinSpeed, float worldTime, float[] color) {
        if (texture == null) {
            return;
        }
        VertexConsumer consumer = vertices.getBuffer(RenderLayer.getEntityTranslucent(texture));
        matrices.push();
        matrices.scale(radius, radius, radius);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(worldTime * 0.02F * spinSpeed));
        MatrixStack.Entry entry = matrices.peek();
        float outer = 1.0F;
        float inner = Math.max(0.05F, outer - 0.25F);
        int segments = 48;
        int a = MathHelper.clamp((int) (alpha * 255.0F), 0, 255);
        int r = MathHelper.clamp((int) (color[0] * 255.0F), 0, 255);
        int g = MathHelper.clamp((int) (color[1] * 255.0F), 0, 255);
        int b = MathHelper.clamp((int) (color[2] * 255.0F), 0, 255);
        for (int i = 0; i < segments; i++) {
            double angleStart = (Math.PI * 2.0D * i) / segments;
            double angleEnd = (Math.PI * 2.0D * (i + 1)) / segments;
            emitRingQuad(entry, consumer, angleStart, angleEnd, inner, outer, r, g, b, a);
        }
        matrices.pop();
    }

    private static void emitRingQuad(MatrixStack.Entry entry, VertexConsumer consumer,
            double angleStart, double angleEnd, float inner, float outer, int r, int g, int b, int a) {
        float x1 = (float) (Math.cos(angleStart) * inner);
        float z1 = (float) (Math.sin(angleStart) * inner);
        float x2 = (float) (Math.cos(angleEnd) * inner);
        float z2 = (float) (Math.sin(angleEnd) * inner);
        float x3 = (float) (Math.cos(angleEnd) * outer);
        float z3 = (float) (Math.sin(angleEnd) * outer);
        float x4 = (float) (Math.cos(angleStart) * outer);
        float z4 = (float) (Math.sin(angleStart) * outer);
        consumer.vertex(entry.getPositionMatrix(), x1, 0.0F, z1).color(r, g, b, a).texture(0.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(MAX_LIGHT).normal(entry, 0.0F, 1.0F, 0.0F);
        consumer.vertex(entry.getPositionMatrix(), x2, 0.0F, z2).color(r, g, b, a).texture(1.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(MAX_LIGHT).normal(entry, 0.0F, 1.0F, 0.0F);
        consumer.vertex(entry.getPositionMatrix(), x3, 0.0F, z3).color(r, g, b, a).texture(1.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(MAX_LIGHT).normal(entry, 0.0F, 1.0F, 0.0F);
        consumer.vertex(entry.getPositionMatrix(), x4, 0.0F, z4).color(r, g, b, a).texture(0.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(MAX_LIGHT).normal(entry, 0.0F, 1.0F, 0.0F);
    }

    private static void renderBillboardQuad(MatrixStack.Entry entry, VertexConsumer consumer, float alpha, float[] color) {
        float half = 0.5F;
        int a = MathHelper.clamp((int) (alpha * 255.0F), 0, 255);
        int r = MathHelper.clamp((int) (color[0] * 255.0F), 0, 255);
        int g = MathHelper.clamp((int) (color[1] * 255.0F), 0, 255);
        int b = MathHelper.clamp((int) (color[2] * 255.0F), 0, 255);
        // Front face
        emitBillboardVertex(entry, consumer, -half, -half, 0.0F, 0.0F, 1.0F, r, g, b, a, 0.0F, 0.0F, 1.0F);
        emitBillboardVertex(entry, consumer, half, -half, 0.0F, 1.0F, 1.0F, r, g, b, a, 0.0F, 0.0F, 1.0F);
        emitBillboardVertex(entry, consumer, half, half, 0.0F, 1.0F, 0.0F, r, g, b, a, 0.0F, 0.0F, 1.0F);
        emitBillboardVertex(entry, consumer, -half, half, 0.0F, 0.0F, 0.0F, r, g, b, a, 0.0F, 0.0F, 1.0F);
        // Back face
        emitBillboardVertex(entry, consumer, half, half, 0.0F, 1.0F, 0.0F, r, g, b, a, 0.0F, 0.0F, -1.0F);
        emitBillboardVertex(entry, consumer, -half, half, 0.0F, 0.0F, 0.0F, r, g, b, a, 0.0F, 0.0F, -1.0F);
        emitBillboardVertex(entry, consumer, -half, -half, 0.0F, 0.0F, 1.0F, r, g, b, a, 0.0F, 0.0F, -1.0F);
        emitBillboardVertex(entry, consumer, half, -half, 0.0F, 1.0F, 1.0F, r, g, b, a, 0.0F, 0.0F, -1.0F);
    }

    private static void emitBillboardVertex(MatrixStack.Entry entry, VertexConsumer consumer,
            float x, float y, float z, float u, float v, int r, int g, int b, int a,
            float normalX, float normalY, float normalZ) {
        consumer.vertex(entry.getPositionMatrix(), x, y, z)
                .color(r, g, b, a)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(MAX_LIGHT)
                .normal(entry, normalX, normalY, normalZ);
    }
}
