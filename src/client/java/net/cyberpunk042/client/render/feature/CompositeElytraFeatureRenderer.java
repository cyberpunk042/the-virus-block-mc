package net.cyberpunk042.client.render.feature;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.registry.ModItems;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

public class CompositeElytraFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
	private static final Identifier ELYTRA_TEXTURE =
			Identifier.of(TheVirusBlock.MOD_ID, "textures/entity/elytra/composite.png");
	private final ElytraEntityModel model;

	public CompositeElytraFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context, LoadedEntityModels models) {
		super(context);
		this.model = new ElytraEntityModel(models.getModelPart(EntityModelLayers.ELYTRA));
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
		ItemStack chest = state.equippedChestStack;
		if (state.invisible || !chest.isOf(ModItems.COMPOSITE_ELYTRA)) {
			return;
		}
		matrices.push();
		matrices.translate(0.0F, 0.0F, 0.125F);
		this.model.setAngles(state);
		renderModel(this.model, ELYTRA_TEXTURE, matrices, vertexConsumers, light, state, Colors.WHITE);
		matrices.pop();
	}
}

