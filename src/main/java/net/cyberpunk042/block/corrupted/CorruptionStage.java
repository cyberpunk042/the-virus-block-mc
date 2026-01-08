package net.cyberpunk042.block.corrupted;

import net.minecraft.util.StringIdentifiable;

public enum CorruptionStage implements StringIdentifiable {
	STAGE_1("stage_1"),
	STAGE_2("stage_2");

	private final String name;

	CorruptionStage(String name) {
		this.name = name;
	}

	@Override
	public String asString() {
		return name;
	}
}


