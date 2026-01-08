package net.cyberpunk042.client.render.blockentity;

import net.cyberpunk042.growth.profile.GrowthOpacityProfile;
import net.cyberpunk042.growth.profile.GrowthSpinProfile;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

/**
 * Shared utility methods for growth block rendering.
 */
public final class GrowthRenderUtils {
    /**
     * One unit of spin speed corresponds to roughly one full rotation per second.
     */
    public static final float SPIN_TIME_SCALE = (float) (Math.PI * 2.0D / 20.0D);

    private GrowthRenderUtils() {}

    /**
     * Centers a box around the origin by subtracting 0.5 from all coordinates.
     */
    public static Box centerBox(Box box) {
        return new Box(
                box.minX - 0.5D,
                box.minY - 0.5D,
                box.minZ - 0.5D,
                box.maxX - 0.5D,
                box.maxY - 0.5D,
                box.maxZ - 0.5D);
    }

    /**
     * Scales a box around its center by the given factor.
     */
    public static Box scaleBox(Box box, float factor) {
        double centerX = (box.minX + box.maxX) * 0.5D;
        double centerY = (box.minY + box.maxY) * 0.5D;
        double centerZ = (box.minZ + box.maxZ) * 0.5D;
        double halfX = (box.maxX - box.minX) * 0.5D * factor;
        double halfY = (box.maxY - box.minY) * 0.5D * factor;
        double halfZ = (box.maxZ - box.minZ) * 0.5D * factor;
        return new Box(
                centerX - halfX,
                centerY - halfY,
                centerZ - halfZ,
                centerX + halfX,
                centerY + halfY,
                centerZ + halfZ);
    }

    /**
     * Decodes a hex color string (e.g., "#FF6E00" or "FF6E00") into RGB floats.
     */
    public static float[] decodeHexColor(String hex) {
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

    /**
     * Computes the rotation angle (in radians) for a layer based on its spin profile.
     */
    public static float computeSpinRotation(float worldTime, GrowthSpinProfile.Layer layer) {
        float magnitude = layer.clampedSpeed() * layer.clampedDirection();
        if (magnitude == 0.0F) {
            return 0.0F;
        }
        return worldTime * SPIN_TIME_SCALE * magnitude;
    }

    /**
     * Gets the rotation axis for a spin layer.
     */
    public static RotationAxis rotationAxis(GrowthSpinProfile.Layer layer) {
        return RotationAxis.of(layer.axisVector());
    }

    // === Formatting helpers for logging ===

    public static String formatOpacityLayer(GrowthOpacityProfile.Layer layer) {
        if (layer == null) {
            return "none";
        }
        return String.format("base=%.3f pulseSpeed=%.3f pulseAmp=%.3f",
                layer.clampedBaseAlpha(),
                layer.clampedPulseSpeed(),
                layer.clampedPulseAmplitude());
    }

    public static String formatSpinLayer(GrowthSpinProfile.Layer layer) {
        if (layer == null) {
            return "none";
        }
        Vector3f axis = layer.axisVector();
        return String.format("speed=%.3f dir=%.3f axis=%.2f/%.2f/%.2f",
                layer.clampedSpeed(),
                layer.clampedDirection(),
                axis.x,
                axis.y,
                axis.z);
    }

    public static String formatSpinDetail(GrowthSpinProfile.Layer spin, float rotation, float[] color) {
        if (spin == null) {
            return "spin=none";
        }
        Vector3f axis = spin.axisVector();
        return String.format("speed=%.3f dir=%.3f axis=%.2f/%.2f/%.2f rot=%.3f color=%s",
                spin.clampedSpeed(),
                spin.clampedDirection(),
                axis.x,
                axis.y,
                axis.z,
                rotation,
                formatColor(color));
    }

    public static String formatColor(float[] color) {
        if (color == null || color.length < 3) {
            return "1.00/1.00/1.00";
        }
        return String.format("%.2f/%.2f/%.2f", color[0], color[1], color[2]);
    }

    public static String describeId(Identifier id) {
        return id != null ? id.toString() : "none";
    }

    public static String formatBox(Box box) {
        if (box == null) {
            return "null";
        }
        return String.format("[%.3f,%.3f,%.3f -> %.3f,%.3f,%.3f]",
                box.minX,
                box.minY,
                box.minZ,
                box.maxX,
                box.maxY,
                box.maxZ);
    }
}
