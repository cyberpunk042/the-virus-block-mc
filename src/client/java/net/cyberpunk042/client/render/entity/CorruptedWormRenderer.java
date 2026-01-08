package net.cyberpunk042.client.render.entity;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.SilverfishEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;

public class CorruptedWormRenderer extends SilverfishEntityRenderer {
	private static final Identifier TEXTURE = Identifier.of(TheVirusBlock.MOD_ID, "textures/entity/corrupted_worm.png");

	public CorruptedWormRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public Identifier getTexture(LivingEntityRenderState state) {
		return TEXTURE;
	}
}

