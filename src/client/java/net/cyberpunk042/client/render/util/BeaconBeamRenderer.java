package net.cyberpunk042.client.render.util;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class BeaconBeamRenderer {
	private BeaconBeamRenderer() {
	}

	public static void render(MatrixStack matrices,
	                          VertexConsumerProvider consumers,
	                          float innerRadius,
	                          float outerRadius,
	                          int height,
	                          long worldTime,
	                          float tickDelta,
	                          int innerColor,
	                          int outerColor) {
		render(matrices, consumers, BeaconBlockEntityRenderer.BEAM_TEXTURE, innerRadius, outerRadius, height, worldTime, tickDelta, innerColor, outerColor);
	}

	public static void render(MatrixStack matrices,
	                          VertexConsumerProvider consumers,
	                          Identifier texture,
	                          float innerRadius,
	                          float outerRadius,
	                          int height,
	                          long worldTime,
	                          float tickDelta,
	                          int innerColor,
	                          int outerColor) {
		float cycle = Math.floorMod(worldTime, 40L) + tickDelta;
		float phase = MathHelper.fractionalPart(cycle * 0.2F - MathHelper.floor(cycle * 0.1F));
		float vStart = -1.0F + phase;
		VertexConsumer inner = consumers.getBuffer(RenderLayer.getBeaconBeam(texture, false));
		VertexConsumer outer = consumers.getBuffer(RenderLayer.getBeaconBeam(texture, true));
		Entry entry = matrices.peek();
		renderBeamPrism(entry, inner, innerRadius, height, vStart, vStart + height * (0.5F / innerRadius), innerColor);
		renderBeamPrism(entry, outer, outerRadius, height, vStart, vStart + height, outerColor);
	}

	private static void renderBeamPrism(Entry entry, VertexConsumer consumer, float radius, int height, float vStart, float vEnd, int argb) {
		renderFacePair(entry, consumer, -radius, -radius, radius, -radius, height, vStart, vEnd, argb, 0.0F, 0.0F, -1.0F); // north
		renderFacePair(entry, consumer, radius, -radius, radius, radius, height, vStart, vEnd, argb, 1.0F, 0.0F, 0.0F);     // east
		renderFacePair(entry, consumer, -radius, radius, radius, radius, height, vStart, vEnd, argb, 0.0F, 0.0F, 1.0F);     // south
		renderFacePair(entry, consumer, -radius, -radius, -radius, radius, height, vStart, vEnd, argb, -1.0F, 0.0F, 0.0F);  // west
	}

	private static void renderFacePair(Entry entry, VertexConsumer consumer,
	                                   float x1, float z1, float x2, float z2,
	                                   int height, float vStart, float vEnd,
	                                   int argb, float normalX, float normalY, float normalZ) {
		renderFace(entry, consumer, x1, z1, x2, z2, height, vStart, vEnd, argb, normalX, normalY, normalZ);
		renderFace(entry, consumer, x2, z2, x1, z1, height, vStart, vEnd, argb, -normalX, -normalY, -normalZ);
	}

	private static void renderFace(Entry entry, VertexConsumer consumer,
	                               float x1, float z1, float x2, float z2,
	                               int height, float vStart, float vEnd,
	                               int argb, float normalX, float normalY, float normalZ) {
		addBeamVertex(entry, consumer, x1, 0.0F, z1, 0.0F, vStart, argb, normalX, normalY, normalZ);
		addBeamVertex(entry, consumer, x1, height, z1, 0.0F, vEnd, argb, normalX, normalY, normalZ);
		addBeamVertex(entry, consumer, x2, height, z2, 1.0F, vEnd, argb, normalX, normalY, normalZ);
		addBeamVertex(entry, consumer, x2, 0.0F, z2, 1.0F, vStart, argb, normalX, normalY, normalZ);
	}

	private static void addBeamVertex(Entry entry, VertexConsumer consumer,
	                                  float x, float y, float z, float u, float v,
	                                  int argb, float normalX, float normalY, float normalZ) {
		int a = (argb >>> 24) & 0xFF;
		int r = (argb >>> 16) & 0xFF;
		int g = (argb >>> 8) & 0xFF;
		int b = argb & 0xFF;
		consumer.vertex(entry.getPositionMatrix(), x, y, z)
				.color(r, g, b, a)
				.texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal(entry, normalX, normalY, normalZ);
	}
}

