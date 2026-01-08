package net.cyberpunk042.growth.profile;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Controls opacity/alpha animation for the glow layers. Keeping opacity in its
 * own profile allows block definitions to mix and match glow textures with
 * different transparency or pulse behaviors.
 */
public record GrowthOpacityProfile(
		Identifier id,
		Layer primary,
		Layer secondary) {

	public static GrowthOpacityProfile defaults() {
		return new GrowthOpacityProfile(
				Identifier.of("the-virus-block", "default_opacity"),
				Layer.defaults(),
				Layer.defaults());
	}

	public Layer primaryLayer() {
		return primary != null ? primary : Layer.defaults();
	}

	public Layer secondaryLayer() {
		return secondary != null ? secondary : Layer.defaults();
	}

	public record Layer(
			float baseAlpha,
			float pulseSpeed,
			float pulseAmplitude) {

		private static final Layer DEFAULT = new Layer(1.0F, 0.0F, 0.0F);

		public static Layer defaults() {
			return DEFAULT;
		}

		public float clampedBaseAlpha() {
			return MathHelper.clamp(baseAlpha, 0.0F, 1.0F);
		}

		public float clampedPulseSpeed() {
			return Math.max(0.0F, pulseSpeed);
		}

		public float clampedPulseAmplitude() {
			return MathHelper.clamp(pulseAmplitude, 0.0F, 1.0F);
		}

		public float animatedAlpha(float worldTime) {
			float clampedBase = clampedBaseAlpha();
			float amplitude = clampedPulseAmplitude();
			float speed = clampedPulseSpeed();
			if (amplitude == 0.0F || speed == 0.0F) {
				return clampedBase;
			}
			float delta = MathHelper.sin(worldTime * speed) * amplitude;
			return MathHelper.clamp(clampedBase + delta, 0.0F, 1.0F);
		}
	}
}

