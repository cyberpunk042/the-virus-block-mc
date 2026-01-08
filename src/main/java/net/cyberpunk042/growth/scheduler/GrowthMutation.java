package net.cyberpunk042.growth.scheduler;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

/**
 * Describes a set of overrides to apply to a {@link net.cyberpunk042.growth.GrowthBlockDefinition}.
 * Scheduler tasks can bundle multiple flag/profile updates into a single mutation.
 */
public final class GrowthMutation {
	private final EnumMap<GrowthField, Object> values = new EnumMap<>(GrowthField.class);
	private final EnumSet<GrowthField> clears = EnumSet.noneOf(GrowthField.class);

	public GrowthMutation setBoolean(GrowthField field, boolean value) {
		validateType(field, GrowthField.Type.BOOLEAN);
		values.put(field, value);
		clears.remove(field);
		return this;
	}

	public GrowthMutation setInt(GrowthField field, int value) {
		validateType(field, GrowthField.Type.INT);
		values.put(field, value);
		clears.remove(field);
		return this;
	}

	public GrowthMutation setDouble(GrowthField field, double value) {
		validateType(field, GrowthField.Type.DOUBLE);
		values.put(field, value);
		clears.remove(field);
		return this;
	}

	public GrowthMutation setIdentifier(GrowthField field, @Nullable Identifier id) {
		validateType(field, GrowthField.Type.IDENTIFIER);
		if (id == null) {
			return clear(field);
		}
		values.put(field, id);
		clears.remove(field);
		return this;
	}

	public GrowthMutation clear(GrowthField field) {
		values.remove(field);
		clears.add(field);
		return this;
	}

	public boolean isEmpty() {
		return values.isEmpty() && clears.isEmpty();
	}

	EnumMap<GrowthField, Object> values() {
		return values;
	}

	EnumSet<GrowthField> clears() {
		return clears;
	}

	public NbtCompound toNbt() {
		NbtCompound root = new NbtCompound();
		if (!values.isEmpty()) {
			NbtCompound valueTag = new NbtCompound();
			for (Map.Entry<GrowthField, Object> entry : values.entrySet()) {
				String key = entry.getKey().name();
				switch (entry.getKey().type()) {
					case BOOLEAN -> valueTag.putBoolean(key, (Boolean) entry.getValue());
					case INT -> valueTag.putInt(key, (Integer) entry.getValue());
					case DOUBLE -> valueTag.putDouble(key, (Double) entry.getValue());
					case IDENTIFIER -> valueTag.putString(key, entry.getValue().toString());
				}
			}
			root.put("values", valueTag);
		}
		if (!clears.isEmpty()) {
			NbtCompound clearTag = new NbtCompound();
			int idx = 0;
			for (GrowthField field : clears) {
				clearTag.putString(String.valueOf(idx++), field.name());
			}
			root.put("clears", clearTag);
		}
		return root;
	}

	public static GrowthMutation fromNbt(@Nullable NbtCompound tag) {
		GrowthMutation mutation = new GrowthMutation();
		if (tag == null) {
			return mutation;
		}
		tag.getCompound("values").ifPresent(values -> {
			for (String key : values.getKeys()) {
				GrowthField field = GrowthField.valueOf(key);
				switch (field.type()) {
					case BOOLEAN -> values.getBoolean(key).ifPresent(val -> mutation.setBoolean(field, val));
					case INT -> values.getInt(key).ifPresent(val -> mutation.setInt(field, val));
					case DOUBLE -> values.getDouble(key).ifPresent(val -> mutation.setDouble(field, val));
					case IDENTIFIER -> values.getString(key)
							.map(Identifier::tryParse)
							.ifPresent(id -> mutation.setIdentifier(field, id));
				}
			}
		});
		tag.getCompound("clears").ifPresent(clears -> {
			for (String key : clears.getKeys()) {
				clears.getString(key).ifPresent(fieldName -> {
					if (!fieldName.isEmpty()) {
						mutation.clear(GrowthField.valueOf(fieldName));
					}
				});
			}
		});
		return mutation;
	}

	private static void validateType(GrowthField field, GrowthField.Type expected) {
		if (field.type() != expected) {
			throw new IllegalArgumentException("Field " + field + " expects " + field.type() + " values");
		}
	}

	@Override
	public String toString() {
		return "GrowthMutation{" +
				"values=" + values +
				", clears=" + clears +
				'}';
	}

	@Override
	public int hashCode() {
		return Objects.hash(values, clears);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof GrowthMutation other)) {
			return false;
		}
		return Objects.equals(values, other.values) && Objects.equals(clears, other.clears);
	}
}

