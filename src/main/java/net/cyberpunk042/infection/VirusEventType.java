package net.cyberpunk042.infection;

import com.mojang.serialization.Codec;

public enum VirusEventType {
	MUTATION_PULSE,
	SKYFALL,
	PASSIVE_REVOLT,
	MOB_BUFF_STORM,
	COLLAPSE_SURGE,
	VOID_TEAR,
	INVERSION,
	VIRUS_BLOOM,
	ENTITY_DUPLICATION,
	SINGULARITY;

	public static final Codec<VirusEventType> CODEC = Codec.STRING.xmap(VirusEventType::valueOf, VirusEventType::name);
}

