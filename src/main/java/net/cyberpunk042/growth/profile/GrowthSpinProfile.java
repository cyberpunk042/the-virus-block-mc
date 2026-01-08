package net.cyberpunk042.growth.profile;

import org.joml.Vector3f;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Controls spin behavior for glow layers. Each layer may specify its own axis,
 * speed multiplier, and direction multiplier, allowing different layers to spin
 * independently without overloading the glow profile.
 */
public record GrowthSpinProfile(
		Identifier id,
		Layer primary,
		Layer secondary) {

	public static GrowthSpinProfile defaults() {
		return new GrowthSpinProfile(
				Identifier.of("the-virus-block", "default_spin"),
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
			float speed,
			float directionMultiplier,
			float axisX,
			float axisY,
			float axisZ) {

		private static final Layer DEFAULT = new Layer(0.0F, 1.0F, 0.0F, 1.0F, 0.0F);

		public static Layer defaults() {
			return DEFAULT;
		}

		public float clampedSpeed() {
			return MathHelper.clamp(speed, -32.0F, 32.0F);
		}

		public float clampedDirection() {
			return MathHelper.clamp(directionMultiplier, -4.0F, 4.0F);
		}

		public Vector3f axisVector() {
			Vector3f axis = new Vector3f(axisX, axisY, axisZ);
			if (axis.lengthSquared() < 1.0E-4F) {
				axis.set(0.0F, 1.0F, 0.0F);
			}
			return axis.normalize();
		}
	}
}

