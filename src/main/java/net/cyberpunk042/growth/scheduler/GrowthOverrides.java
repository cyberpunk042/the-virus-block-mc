package net.cyberpunk042.growth.scheduler;


import net.cyberpunk042.log.Logging;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.cyberpunk042.growth.GrowthBlockDefinition;
import net.cyberpunk042.growth.GrowthProfile;
import net.cyberpunk042.growth.GrowthRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.Identifier;

/**
 * Stores per-entity overrides for growth definitions. {@link GrowthMutation}
 * instances merge into this map and {@link #apply(GrowthBlockDefinition)}
 * rematerializes a new definition with the requested overrides.
 */
public final class GrowthOverrides {
	private final EnumMap<GrowthField, Object> values = new EnumMap<>(GrowthField.class);

	public static GrowthOverrides empty() {
		return new GrowthOverrides();
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	public boolean applyMutation(GrowthMutation mutation) {
		boolean changed = false;
		for (Map.Entry<GrowthField, Object> entry : mutation.values().entrySet()) {
			if (!Objects.equals(values.get(entry.getKey()), entry.getValue())) {
				values.put(entry.getKey(), entry.getValue());
				changed = true;
			}
		}
		for (GrowthField cleared : mutation.clears()) {
			if (values.remove(cleared) != null) {
				changed = true;
			}
		}
		return changed;
	}

	public void clear() {
		values.clear();
	}

	public NbtCompound toNbt() {
		NbtCompound tag = new NbtCompound();
		for (Map.Entry<GrowthField, Object> entry : values.entrySet()) {
			String key = entry.getKey().name();
			switch (entry.getKey().type()) {
				case BOOLEAN -> tag.putBoolean(key, (Boolean) entry.getValue());
				case INT -> tag.putInt(key, (Integer) entry.getValue());
				case DOUBLE -> tag.putDouble(key, (Double) entry.getValue());
				case IDENTIFIER -> tag.putString(key, entry.getValue().toString());
			}
		}
		return tag;
	}

	public String toSnbt() {
		return toNbt().toString();
	}

	public Map<GrowthField, Object> snapshot() {
		return Collections.unmodifiableMap(new EnumMap<>(values));
	}

	public static GrowthOverrides fromNbt(@Nullable NbtCompound tag) {
		GrowthOverrides overrides = new GrowthOverrides();
		if (tag == null) {
			return overrides;
		}
		for (String key : tag.getKeys()) {
			GrowthField field;
			try {
				field = GrowthField.valueOf(key.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException ex) {
				Logging.SCHEDULER.warn("[GrowthOverrides] Ignoring unknown override key '{}'", key);
				continue;
			}
			switch (field.type()) {
				case BOOLEAN -> tag.getBoolean(key).ifPresent(val -> overrides.values.put(field, val));
				case INT -> tag.getInt(key).ifPresent(val -> overrides.values.put(field, val));
				case DOUBLE -> tag.getDouble(key).ifPresent(val -> overrides.values.put(field, val));
				case IDENTIFIER -> tag.getString(key)
						.map(Identifier::tryParse)
						.ifPresent(id -> overrides.values.put(field, id));
			}
		}
		return overrides;
	}

	public static GrowthOverrides fromSnbt(@Nullable String data) {
		if (data == null || data.isEmpty()) {
			return GrowthOverrides.empty();
		}
		try {
			return fromNbt(NbtHelper.fromNbtProviderString(data));
		} catch (CommandSyntaxException ex) {
			return GrowthOverrides.empty();
		}
	}

	public GrowthBlockDefinition apply(GrowthBlockDefinition base) {
		return apply(base, null);
	}

	public GrowthBlockDefinition apply(GrowthBlockDefinition base, @Nullable GrowthRegistry registry) {
		Identifier appliedGrowthProfile = getIdentifier(GrowthField.GROWTH_PROFILE, base.growthProfileId());
		GrowthProfile preset = registry != null ? registry.growthProfile(appliedGrowthProfile) : null;
		boolean presetEnabled = preset != null ? preset.growthEnabled() : base.growthEnabled();
		int presetRate = preset != null ? preset.rateTicks() : base.rateTicks();
		double presetRateScale = preset != null ? preset.rateScale() : base.rateScale();
		double presetStart = preset != null ? preset.startScale() : base.startScale();
		double presetTarget = preset != null ? preset.targetScale() : base.targetScale();
		double presetMin = preset != null ? preset.minScale() : base.minScale();
		double presetMax = preset != null ? preset.maxScale() : base.maxScale();
		return new GrowthBlockDefinition(
				base.id(),
				getBoolean(GrowthField.GROWTH_ENABLED, presetEnabled),
				getInt(GrowthField.RATE_TICKS, presetRate),
				getDouble(GrowthField.RATE_SCALE, presetRateScale),
				getDouble(GrowthField.START_SCALE, presetStart),
				getDouble(GrowthField.TARGET_SCALE, presetTarget),
				getDouble(GrowthField.MIN_SCALE, presetMin),
				getDouble(GrowthField.MAX_SCALE, presetMax),
				getBoolean(GrowthField.HAS_COLLISION, base.hasCollision()),
				getBoolean(GrowthField.DOES_DESTRUCTION, base.doesDestruction()),
				getBoolean(GrowthField.HAS_FUSE, base.hasFuse()),
				getDouble(GrowthField.TOUCH_DAMAGE, base.touchDamage()),
				appliedGrowthProfile,
				getIdentifier(GrowthField.GLOW_PROFILE, base.glowProfileId()),
				getIdentifier(GrowthField.PARTICLE_PROFILE, base.particleProfileId()),
				getIdentifier(GrowthField.FIELD_PROFILE, base.fieldProfileId()),
				getIdentifier(GrowthField.PULL_PROFILE, base.pullProfileId()),
				getIdentifier(GrowthField.PUSH_PROFILE, base.pushProfileId()),
				getIdentifier(GrowthField.FUSE_PROFILE, base.fuseProfileId()),
				getIdentifier(GrowthField.EXPLOSION_PROFILE, base.explosionProfileId()),
				getIdentifier(GrowthField.OPACITY_PROFILE, base.opacityProfileId()),
				getIdentifier(GrowthField.SPIN_PROFILE, base.spinProfileId()),
				getIdentifier(GrowthField.WOBBLE_PROFILE, base.wobbleProfileId()));
	}

	private boolean getBoolean(GrowthField field, boolean fallback) {
		Object value = values.get(field);
		return value instanceof Boolean bool ? bool : fallback;
	}

	private int getInt(GrowthField field, int fallback) {
		Object value = values.get(field);
		return value instanceof Integer integer ? integer : fallback;
	}

	private double getDouble(GrowthField field, double fallback) {
		Object value = values.get(field);
		return value instanceof Number number ? number.doubleValue() : fallback;
	}

	private Identifier getIdentifier(GrowthField field, Identifier fallback) {
		Object value = values.get(field);
		return value instanceof Identifier id && id != null ? id : fallback;
	}
}

