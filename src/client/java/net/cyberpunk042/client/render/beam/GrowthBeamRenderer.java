package net.cyberpunk042.client.render.beam;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.joml.Matrix4f;

import net.cyberpunk042.network.GrowthBeamPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Client-side renderer for {@link GrowthBeamPayload}. Draws guardian-style beams
 * from the growth block to the targeted entity while the force field is active.
 */
public final class GrowthBeamRenderer {
	private static final Identifier BEAM_TEXTURE = Identifier.of("minecraft", "textures/entity/guardian_beam.png");
	private static final float BASE_WIDTH = 0.08F;
	private static final float FADE_OUT_TICKS = 10.0F;
	private static final Deque<GrowthBeam> ACTIVE_BEAMS = new ArrayDeque<>();

	private GrowthBeamRenderer() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(GrowthBeamPayload.ID, (payload, context) ->
				context.client().execute(() -> handleBeam(payload)));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Iterator<GrowthBeam> iterator = ACTIVE_BEAMS.iterator();
			while (iterator.hasNext()) {
				GrowthBeam beam = iterator.next();
				if (beam.tick()) {
					iterator.remove();
				}
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ACTIVE_BEAMS::clear));
		WorldRenderEvents.AFTER_ENTITIES.register(GrowthBeamRenderer::renderWorld);
	}

	private static void handleBeam(GrowthBeamPayload payload) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientWorld world = client.world;
		if (world == null || !world.getRegistryKey().equals(payload.worldKey())) {
			return;
		}
		Vec3d origin = Vec3d.ofCenter(payload.origin());
		ACTIVE_BEAMS.add(new GrowthBeam(
				origin,
				payload.targetEntityId(),
				payload.targetPosition(),
				payload.durationTicks(),
				payload.pulling(),
				payload.red(),
				payload.green(),
				payload.blue()));
	}

	private static void renderWorld(WorldRenderContext context) {
		if (ACTIVE_BEAMS.isEmpty()) {
			return;
		}
		VertexConsumerProvider providers = context.consumers();
		Camera camera = context.camera();
		ClientWorld world = context.world();
		if (providers == null || camera == null || world == null) {
			return;
		}
		float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
		MatrixStack matrices = context.matrixStack();
		matrices.push();
		render(matrices, providers, world, camera.getPos().x, camera.getPos().y, camera.getPos().z, tickDelta);
		matrices.pop();
	}

	private static void render(MatrixStack matrices, VertexConsumerProvider providers, ClientWorld world, double cameraX, double cameraY, double cameraZ, float tickDelta) {
		if (ACTIVE_BEAMS.isEmpty()) {
			return;
		}
		MatrixStack.Entry entry = matrices.peek();
		Matrix4f positionMatrix = entry.getPositionMatrix();
		for (GrowthBeam beam : ACTIVE_BEAMS) {
			beam.render(positionMatrix, entry, providers, world, cameraX, cameraY, cameraZ, tickDelta);
		}
	}

	private static final class GrowthBeam {
		private final Vec3d origin;
		private final int targetEntityId;
		private Vec3d fallbackTarget;
		private final boolean pulling;
		private final float red;
		private final float green;
		private final float blue;
		private int ticksRemaining;

		private GrowthBeam(Vec3d origin, int targetEntityId, Vec3d fallbackTarget, int duration, boolean pulling, float red, float green, float blue) {
			this.origin = origin;
			this.targetEntityId = targetEntityId;
			this.fallbackTarget = fallbackTarget;
			this.ticksRemaining = duration;
			this.pulling = pulling;
			this.red = red;
			this.green = green;
			this.blue = blue;
		}

		private boolean tick() {
			return --ticksRemaining <= 0;
		}

		private void render(Matrix4f matrix, MatrixStack.Entry entry, VertexConsumerProvider providers, ClientWorld world, double cameraX, double cameraY, double cameraZ, float tickDelta) {
			Vec3d start = origin.subtract(cameraX, cameraY, cameraZ);
			Vec3d targetPos = resolveTarget(world, tickDelta);
			Vec3d end = targetPos.subtract(cameraX, cameraY, cameraZ);
			float width = BASE_WIDTH * (pulling ? 1.2F : 0.9F);

			float alpha = Math.min(1.0F, ticksRemaining / FADE_OUT_TICKS);
			VertexConsumer consumer = providers.getBuffer(RenderLayer.getEyes(BEAM_TEXTURE));

			Vec3d delta = end.subtract(start);
			if (delta.lengthSquared() < 1.0E-4D) {
				return;
			}
			Vec3d up = new Vec3d(0, 1, 0);
			Vec3d perpendicular = delta.crossProduct(up);
			if (perpendicular.lengthSquared() < 1.0E-4D) {
				up = new Vec3d(1, 0, 0);
				perpendicular = delta.crossProduct(up);
			}
			perpendicular = perpendicular.normalize().multiply(width);
			Vec3d perpendicular2 = delta.crossProduct(perpendicular).normalize().multiply(width);

			addQuad(matrix, entry, consumer, start, end, perpendicular, alpha);
			addQuad(matrix, entry, consumer, start, end, perpendicular2, alpha * 0.8F);
		}

		private Vec3d resolveTarget(ClientWorld world, float tickDelta) {
			if (world != null) {
				Entity entity = world.getEntityById(targetEntityId);
				if (entity != null) {
					Vec3d pos = entity.getLerpedPos(tickDelta);
					double height = entity.getHeight() * 0.5D;
					fallbackTarget = pos.add(0.0D, height, 0.0D);
					return fallbackTarget;
				}
			}
			return fallbackTarget;
		}

		private void addQuad(Matrix4f matrix, MatrixStack.Entry entry, VertexConsumer consumer, Vec3d start, Vec3d end, Vec3d offset, float alpha) {
			emitVertex(matrix, entry, consumer, start.add(offset), alpha);
			emitVertex(matrix, entry, consumer, end.add(offset), alpha);
			emitVertex(matrix, entry, consumer, end.subtract(offset), alpha);
			emitVertex(matrix, entry, consumer, start.subtract(offset), alpha);
		}

		private void emitVertex(Matrix4f matrix, MatrixStack.Entry entry, VertexConsumer consumer, Vec3d pos, float alpha) {
			consumer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
					.color(red, green, blue, alpha)
					.texture(0.0F, 0.0F)
					.light(0xF000F0)
					.overlay(OverlayTexture.DEFAULT_UV)
					.normal(entry, 1.0F, 0.0F, 0.0F);
		}
	}
}

