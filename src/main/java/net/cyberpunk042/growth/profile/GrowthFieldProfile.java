package net.cyberpunk042.growth.profile;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Visual shell definition rendered around the growth block (independent of glow).
 */
public record GrowthFieldProfile(
		Identifier id,
		String meshType,
		Identifier texture,
		float alpha,
		float spinSpeed,
		float scaleMultiplier,
		String colorHex) {

	public static GrowthFieldProfile defaults() {
		return none();
	}

	public static GrowthFieldProfile none() {
		return new GrowthFieldProfile(
				Identifier.of("the-virus-block", "none_field"),
				"none",
				null,
				0.0F,
				0.0F,
				1.0F,
				"#00000000");
	}

	public static GrowthFieldProfile standardShell() {
		return new GrowthFieldProfile(
				Identifier.of("the-virus-block", "default_field"),
				"sphere",
				Identifier.of("the-virus-block", "textures/misc/singularity_sphere_1.png"),
				0.45F,
				1.0F,
				1.35F,
				"#FFFFFFFF");
	}

	public String meshType() {
		return meshType != null && !meshType.isBlank() ? meshType : "sphere";
	}

	public float clampedAlpha() {
		return MathHelper.clamp(alpha, 0.0F, 1.0F);
	}

	public float clampedScaleMultiplier() {
		return scaleMultiplier > 0.0F ? scaleMultiplier : 1.35F;
	}

	public float[] decodedColor() {
		String hex = colorHex;
		if (hex == null || hex.isEmpty()) {
			return new float[] { 1.0F, 1.0F, 1.0F };
		}
		String normalized = hex.startsWith("#") ? hex.substring(1) : hex;
		try {
			int value = (int) Long.parseLong(normalized, 16);
			float r = ((value >> 16) & 0xFF) / 255.0F;
			float g = ((value >> 8) & 0xFF) / 255.0F;
			float b = (value & 0xFF) / 255.0F;
			return new float[] { r, g, b };
		} catch (NumberFormatException ex) {
			return new float[] { 1.0F, 1.0F, 1.0F };
		}
	}
}

