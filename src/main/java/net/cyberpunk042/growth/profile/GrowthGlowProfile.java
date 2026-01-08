package net.cyberpunk042.growth.profile;

import java.util.List;

import net.minecraft.util.Identifier;

/**
 * Visual preset for progressive growth blocks. Controls textures and whether
 * each glow layer should render as a custom mesh or reuse the native block
 * model. Opacity and spin behavior are delegated to dedicated profiles.
 */
public record GrowthGlowProfile(
		Identifier id,
		Identifier primaryTexture,
		Identifier secondaryTexture,
		boolean primaryUseMesh,
		boolean secondaryUseMesh,
		String primaryColorHex,
		String secondaryColorHex,
		AnimationOverride primaryMeshAnimation,
		AnimationOverride secondaryMeshAnimation,
		int lightLevel) {

	private static final String DEFAULT_COLOR = "#FFFFFF";
	private static final int DEFAULT_LIGHT_LEVEL = 10;

	public static GrowthGlowProfile defaults() {
		return new GrowthGlowProfile(
				Identifier.of("the-virus-block", "magma"),
				Identifier.of("the-virus-block", "textures/block/glow_magma_primary.png"),
				Identifier.of("the-virus-block", "textures/block/glow_magma_secondary.png"),
				false,
				false,
				DEFAULT_COLOR,
				DEFAULT_COLOR,
				null,
				null,
				DEFAULT_LIGHT_LEVEL);
	}

	public record AnimationOverride(Float frameTimeTicks, Boolean interpolate, List<Integer> frames, Float scrollSpeed) {
		public boolean hasFrames() {
			return frames != null && !frames.isEmpty();
		}
	}
}

