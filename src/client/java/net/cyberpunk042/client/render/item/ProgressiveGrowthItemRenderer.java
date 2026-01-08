package net.cyberpunk042.client.render.item;


import net.cyberpunk042.log.Logging;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.block.growth.ProgressiveGrowthBlock;
import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.growth.GrowthBlockDefinition;
import net.cyberpunk042.growth.scheduler.GrowthOverrides;
import net.cyberpunk042.registry.ModBlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.item.model.special.SpecialModelTypes;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class ProgressiveGrowthItemRenderer implements SpecialModelRenderer<ProgressiveGrowthItemRenderer.Argument> {
	private static final Identifier RENDERER_ID = Identifier.of(TheVirusBlock.MOD_ID, "progressive_growth");
	private static final Vector3f[] UNIT_CUBE_EXTENTS = new Vector3f[]{
			new Vector3f(-0.5F, -0.5F, -0.5F),
			new Vector3f(-0.5F, -0.5F, 0.5F),
			new Vector3f(-0.5F, 0.5F, -0.5F),
			new Vector3f(-0.5F, 0.5F, 0.5F),
			new Vector3f(0.5F, -0.5F, -0.5F),
			new Vector3f(0.5F, -0.5F, 0.5F),
			new Vector3f(0.5F, 0.5F, -0.5F),
			new Vector3f(0.5F, 0.5F, 0.5F)
	};
	private static final Map<ItemDisplayContext, DisplayTransform> DISPLAY_TRANSFORMS = createDisplayTransforms();
	private static boolean registered;
	private static final AtomicBoolean INIT_LOGGED = new AtomicBoolean(false);
	private static final AtomicBoolean FIRST_RENDER_LOGGED = new AtomicBoolean(false);

	private final ProgressiveGrowthBlockEntity entity = new ProgressiveGrowthBlockEntity(BlockPos.ORIGIN, ModBlocks.PROGRESSIVE_GROWTH_BLOCK.getDefaultState());

	public static void bootstrap() {
		if (registered) {
			return;
		}
		registered = true;
		SpecialModelTypes.ID_MAPPER.put(RENDERER_ID, Unbaked.MAP_CODEC);
		if (INIT_LOGGED.compareAndSet(false, true)) {
			Logging.RENDER.info("[GrowthRenderer] Progressive growth item renderer registered");
		}
	}

	@Override
	public void render(@Nullable Argument argument, ItemDisplayContext displayContext, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, boolean glint) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.world == null) {
			return;
		}
		entity.setWorld(client.world);
		Identifier definitionId = argument != null && argument.definitionId() != null ? argument.definitionId() : GrowthBlockDefinition.defaults().id();
		entity.setDefinitionId(definitionId);
		GrowthOverrides overrides = argument != null && argument.overrides() != null ? argument.overrides() : GrowthOverrides.empty();
		entity.replaceOverrides(overrides);
		entity.snapToTargetScale();
		if (FIRST_RENDER_LOGGED.compareAndSet(false, true)) {
			Logging.RENDER.info(
					"[GrowthRenderer] First item render def={} overrides={}",
					definitionId,
					overrides.isEmpty() ? "none" : overrides.toSnbt());
		}
		BlockEntityRenderDispatcher dispatcher = client.getBlockEntityRenderDispatcher();
		BlockEntityRenderer<ProgressiveGrowthBlockEntity> renderer = dispatcher.get(entity);
		if (renderer == null) {
			return;
		}
		matrices.push();
		applyDisplayTransform(displayContext, matrices);
		renderer.render(entity, 0.0F, matrices, vertexConsumers, light, overlay, Vec3d.ZERO);
		matrices.pop();
	}

	@Override
	public void collectVertices(Set<Vector3f> vertices) {
		for (Vector3f corner : UNIT_CUBE_EXTENTS) {
			vertices.add(new Vector3f(corner));
		}
	}

	@Override
	public @Nullable Argument getData(ItemStack stack) {
		Identifier definitionId = ProgressiveGrowthBlock.readDefinitionId(stack);
		GrowthOverrides overrides = ProgressiveGrowthBlock.readOverrides(stack);
		return new Argument(definitionId, overrides);
	}

	@Environment(EnvType.CLIENT)
	public static final class Unbaked implements SpecialModelRenderer.Unbaked {
		private static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public MapCodec<? extends SpecialModelRenderer.Unbaked> getCodec() {
			return MAP_CODEC;
		}

		@Override
		public SpecialModelRenderer<?> bake(LoadedEntityModels entityModels) {
			return new ProgressiveGrowthItemRenderer();
		}
	}

	public record Argument(@Nullable Identifier definitionId, GrowthOverrides overrides) {
	}

	private static void applyDisplayTransform(ItemDisplayContext context, MatrixStack matrices) {
		DisplayTransform transform = DISPLAY_TRANSFORMS.get(context);
		if (transform == null) {
			return;
		}
		Vector3f translation = transform.translation();
		if (translation.x() != 0.0F || translation.y() != 0.0F || translation.z() != 0.0F) {
			matrices.translate(translation.x() / 16.0F, translation.y() / 16.0F, translation.z() / 16.0F);
		}
		matrices.translate(0.5F, 0.5F, 0.5F);
		Vector3f rotation = transform.rotation();
		if (rotation.x() != 0.0F) {
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation.x()));
		}
		if (rotation.y() != 0.0F) {
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation.y()));
		}
		if (rotation.z() != 0.0F) {
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation.z()));
		}
		Vector3f scale = transform.scale();
		if (scale.x() != 1.0F || scale.y() != 1.0F || scale.z() != 1.0F) {
			matrices.scale(scale.x(), scale.y(), scale.z());
		}
		matrices.translate(-0.5F, -0.5F, -0.5F);
	}

	private static Map<ItemDisplayContext, DisplayTransform> createDisplayTransforms() {
		EnumMap<ItemDisplayContext, DisplayTransform> map = new EnumMap<>(ItemDisplayContext.class);
		map.put(ItemDisplayContext.GUI, new DisplayTransform(vec(30.0F, 225.0F, 0.0F), vec(0.0F, 0.0F, 0.0F), vec(0.625F, 0.625F, 0.625F)));
		map.put(ItemDisplayContext.GROUND, new DisplayTransform(vec(0.0F, 0.0F, 0.0F), vec(0.0F, 3.0F, 0.0F), vec(0.25F, 0.25F, 0.25F)));
		map.put(ItemDisplayContext.FIXED, new DisplayTransform(vec(0.0F, 0.0F, 0.0F), vec(0.0F, 0.0F, 0.0F), vec(0.5F, 0.5F, 0.5F)));
		map.put(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, new DisplayTransform(vec(75.0F, 45.0F, 0.0F), vec(0.0F, 2.5F, 0.0F), vec(0.375F, 0.375F, 0.375F)));
		map.put(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, new DisplayTransform(vec(75.0F, -45.0F, 0.0F), vec(0.0F, 2.5F, 0.0F), vec(0.375F, 0.375F, 0.375F)));
		map.put(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, new DisplayTransform(vec(0.0F, 45.0F, 0.0F), vec(0.0F, 0.0F, 0.0F), vec(0.4F, 0.4F, 0.4F)));
		map.put(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, new DisplayTransform(vec(0.0F, 225.0F, 0.0F), vec(0.0F, 0.0F, 0.0F), vec(0.4F, 0.4F, 0.4F)));
		map.put(ItemDisplayContext.HEAD, new DisplayTransform(vec(0.0F, 0.0F, 0.0F), vec(0.0F, 0.0F, 0.0F), vec(1.0F, 1.0F, 1.0F)));
		map.put(ItemDisplayContext.NONE, new DisplayTransform(vec(0.0F, 0.0F, 0.0F), vec(0.0F, 0.0F, 0.0F), vec(1.0F, 1.0F, 1.0F)));
		return map;
	}

	private static Vector3f vec(float x, float y, float z) {
		return new Vector3f(x, y, z);
	}

	private record DisplayTransform(Vector3f rotation, Vector3f translation, Vector3f scale) {
	}
}

