package net.cyberpunk042.client.render.blockentity;

import net.cyberpunk042.block.entity.SingularityBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class SingularityBlockEntityRenderer implements BlockEntityRenderer<SingularityBlockEntity> {
	public SingularityBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(SingularityBlockEntity entity, float tickDelta, MatrixStack matrices,
	                   VertexConsumerProvider vertexConsumers, int light, int overlay, Vec3d cameraPos) {
		// Rendering handled by block model and global visuals.
	}
}

