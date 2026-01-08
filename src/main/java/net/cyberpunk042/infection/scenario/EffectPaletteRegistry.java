package net.cyberpunk042.infection.scenario;


import net.cyberpunk042.log.Logging;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.cyberpunk042.TheVirusBlock;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

final class EffectPaletteRegistry {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Path PALETTE_DIR = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("the-virus-block")
			.resolve("effect_palettes");

	private static final Map<Identifier, EffectPaletteConfig> PALETTES = new HashMap<>();
	private static boolean loaded;

	private EffectPaletteRegistry() {
	}

	static EffectPaletteConfig resolve(Identifier id) {
		ensureLoaded();
		return Optional.ofNullable(PALETTES.get(id))
				.orElseGet(() -> {
					Logging.SCENARIO.warn("[EffectPalette] Missing palette {}. Falling back to overworld palette.", id);
					return PALETTES.getOrDefault(Identifier.of(TheVirusBlock.MOD_ID, "overworld"), defaultOverworld());
				});
	}

	private static void ensureLoaded() {
		if (loaded) {
			return;
		}
		PALETTES.clear();
		createDirectories();
		loadDefaultPalettes();
		try (var stream = Files.list(PALETTE_DIR)) {
			stream.filter(path -> path.toString().endsWith(".json"))
					.forEach(EffectPaletteRegistry::loadPalette);
		} catch (IOException ex) {
			Logging.SCENARIO.error("[EffectPalette] Failed to read palettes", ex);
		}
		loaded = true;
	}

	private static void loadPalette(Path path) {
		try (Reader reader = Files.newBufferedReader(path)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			if (!parsed.isJsonObject()) {
				Logging.SCENARIO.warn("[EffectPalette] Skipping {} (not a JSON object)", path.getFileName());
				return;
			}
			EffectPaletteConfig palette = parsePalette(parsed.getAsJsonObject());
			if (palette.id() == null) {
				Logging.SCENARIO.warn("[EffectPalette] Skipping {} (missing id)", path.getFileName());
				return;
			}
			PALETTES.put(palette.id(), palette);
		} catch (IOException ex) {
			Logging.SCENARIO.error("[EffectPalette] Failed to load palette {}", path, ex);
		}
	}

	private static void createDirectories() {
		try {
			Files.createDirectories(PALETTE_DIR);
		} catch (IOException ex) {
			Logging.SCENARIO.error("[EffectPalette] Failed to create directory {}", PALETTE_DIR, ex);
		}
	}

	private static void loadDefaultPalettes() {
		EffectPaletteConfig overworld = defaultOverworld();
		EffectPaletteConfig nether = defaultNether();
		PALETTES.put(overworld.id(), overworld);
		PALETTES.put(nether.id(), nether);
		writeDefaultIfMissing("overworld.json", overworld);
		writeDefaultIfMissing("nether.json", nether);
	}

	private static void writeDefaultIfMissing(String fileName, EffectPaletteConfig palette) {
		Path target = PALETTE_DIR.resolve(fileName);
		if (Files.exists(target)) {
			return;
		}
		try (var writer = Files.newBufferedWriter(target)) {
			GSON.toJson(serializePalette(palette), writer);
		} catch (IOException ex) {
			Logging.SCENARIO.error("[EffectPalette] Failed to write default palette {}", target, ex);
		}
	}

	private static EffectPaletteConfig defaultOverworld() {
		return new EffectPaletteConfig(
				Identifier.of(TheVirusBlock.MOD_ID, "overworld"),
				new EffectPaletteConfig.SimpleEvent(
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "enchant")),
						new EffectPaletteConfig.Sound(
								Identifier.of("minecraft", "block.beacon.ambient"),
								"hostile",
								1.5F,
								0.6F,
								10)),
				new EffectPaletteConfig.SimpleEvent(
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "explosion_emitter")),
						new EffectPaletteConfig.Sound(
								Identifier.of("minecraft", "entity.wither.spawn"),
								"hostile",
								4.0F,
								0.5F,
								0)),
				new EffectPaletteConfig.SimpleEvent(
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "small_flame")),
						null),
				new EffectPaletteConfig.RingPulseEvent(
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "portal")),
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "reverse_portal")),
						Identifier.of("minecraft", "deepslate"),
						new EffectPaletteConfig.Sound(
								Identifier.of("minecraft", "ambient.nether_wastes.mood"),
								"ambient",
								1.0F,
								1.0F,
								20),
						new EffectPaletteConfig.Sound(
								Identifier.of("minecraft", "block.end_portal.spawn"),
								"hostile",
								1.1F,
								0.5F,
								10)),
				new EffectPaletteConfig.DissipationEvent(
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "reverse_portal")),
						null,
						new EffectPaletteConfig.Sound(
								Identifier.of("minecraft", "block.respawn_anchor.deplete"),
								"hostile",
								1.0F,
								1.0F,
								5)),
				new EffectPaletteConfig.CollapseVeilEvent(
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "sculk_soul")),
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "reverse_portal")),
						new EffectPaletteConfig.Sound(
								Identifier.of("minecraft", "entity.warden.sonic_boom"),
								"hostile",
								1.2F,
								0.6F,
								0)));
	}

	private static EffectPaletteConfig defaultNether() {
		return new EffectPaletteConfig(
				Identifier.of(TheVirusBlock.MOD_ID, "nether"),
				new EffectPaletteConfig.SimpleEvent(
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "soul_fire_flame")),
						new EffectPaletteConfig.Sound(
								Identifier.of("minecraft", "block.respawn_anchor.charge"),
								"hostile",
								1.4F,
								0.8F,
								8)),
				new EffectPaletteConfig.SimpleEvent(
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "explosion_emitter")),
						new EffectPaletteConfig.Sound(
								Identifier.of("minecraft", "entity.blaze.shoot"),
								"hostile",
								4.0F,
								0.4F,
								0)),
				new EffectPaletteConfig.SimpleEvent(
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "dripping_lava")),
						null),
				new EffectPaletteConfig.RingPulseEvent(
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "lava")),
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "ash")),
						Identifier.of("minecraft", "netherrack"),
						new EffectPaletteConfig.Sound(
								Identifier.of("minecraft", "ambient.nether_wastes.mood"),
								"ambient",
								1.0F,
								0.7F,
								16),
						new EffectPaletteConfig.Sound(
								Identifier.of("minecraft", "block.respawn_anchor.deplete"),
								"hostile",
								1.1F,
								0.8F,
								12)),
				new EffectPaletteConfig.DissipationEvent(
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "smoke")),
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "ash")),
						new EffectPaletteConfig.Sound(
								Identifier.of("minecraft", "entity.piglin_brute.angry"),
								"hostile",
								0.8F,
								0.5F,
								4)),
				new EffectPaletteConfig.CollapseVeilEvent(
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "ash")),
						new EffectPaletteConfig.Particle(Identifier.of("minecraft", "smoke")),
						new EffectPaletteConfig.Sound(
								Identifier.of("minecraft", "entity.ghast.scream"),
								"hostile",
								1.3F,
								0.7F,
								0)));
	}

	private static EffectPaletteConfig parsePalette(JsonObject root) {
		Identifier id = parseIdentifier(root.get("id"));
		return new EffectPaletteConfig(
				id,
				parseSimpleEvent(root.getAsJsonObject("core_charge")),
				parseSimpleEvent(root.getAsJsonObject("core_detonation")),
				parseSimpleEvent(root.getAsJsonObject("ring_charge")),
				parseRingPulse(root.getAsJsonObject("ring_pulse")),
				parseDissipation(root.getAsJsonObject("dissipation")),
				parseCollapseVeil(root.getAsJsonObject("collapse_veil")));
	}

	private static EffectPaletteConfig.SimpleEvent parseSimpleEvent(JsonObject node) {
		if (node == null) {
			return null;
		}
		return new EffectPaletteConfig.SimpleEvent(parseParticle(node.get("particle")), parseSound(node.getAsJsonObject("sound")));
	}

	private static EffectPaletteConfig.RingPulseEvent parseRingPulse(JsonObject node) {
		if (node == null) {
			return null;
		}
		return new EffectPaletteConfig.RingPulseEvent(
				parseParticle(node.get("primary_particle")),
				parseParticle(node.get("secondary_particle")),
				parseIdentifier(node.get("debris_block")),
				parseSound(node.getAsJsonObject("ambient_sound")),
				parseSound(node.getAsJsonObject("pulse_sound")));
	}

	private static EffectPaletteConfig.DissipationEvent parseDissipation(JsonObject node) {
		if (node == null) {
			return null;
		}
		return new EffectPaletteConfig.DissipationEvent(
				parseParticle(node.get("primary_particle")),
				parseParticle(node.get("secondary_particle")),
				parseSound(node.getAsJsonObject("sound")));
	}

	private static EffectPaletteConfig.CollapseVeilEvent parseCollapseVeil(JsonObject node) {
		if (node == null) {
			return null;
		}
		return new EffectPaletteConfig.CollapseVeilEvent(
				parseParticle(node.get("primary_particle")),
				parseParticle(node.get("secondary_particle")),
				parseSound(node.getAsJsonObject("sound")));
	}

	private static EffectPaletteConfig.Particle parseParticle(JsonElement node) {
		Identifier id = parseIdentifier(node);
		return id != null ? new EffectPaletteConfig.Particle(id) : null;
	}

	private static EffectPaletteConfig.Sound parseSound(JsonObject node) {
		if (node == null) {
			return null;
		}
		Identifier id = parseIdentifier(node.get("id"));
		if (id == null) {
			return null;
		}
		String category = node.has("category") ? node.get("category").getAsString() : "ambient";
		float volume = node.has("volume") ? node.get("volume").getAsFloat() : 1.0F;
		float pitch = node.has("pitch") ? node.get("pitch").getAsFloat() : 1.0F;
		int interval = node.has("interval_ticks") ? node.get("interval_ticks").getAsInt() : 0;
		return new EffectPaletteConfig.Sound(id, category, volume, pitch, interval);
	}

	private static JsonObject serializePalette(EffectPaletteConfig palette) {
		JsonObject root = new JsonObject();
		root.addProperty("id", palette.id().toString());
		root.add("core_charge", serializeSimpleEvent(palette.coreCharge()));
		root.add("core_detonation", serializeSimpleEvent(palette.coreDetonation()));
		root.add("ring_charge", serializeSimpleEvent(palette.ringCharge()));
		root.add("ring_pulse", serializeRingPulse(palette.ringPulse()));
		root.add("dissipation", serializeDissipation(palette.dissipation()));
		root.add("collapse_veil", serializeCollapseVeil(palette.collapseVeil()));
		return root;
	}

	private static JsonObject serializeSimpleEvent(EffectPaletteConfig.SimpleEvent event) {
		if (event == null) {
			return null;
		}
		JsonObject node = new JsonObject();
		if (event.particle() != null) {
			node.addProperty("particle", event.particle().id().toString());
		}
		JsonObject sound = serializeSound(event.sound());
		if (sound != null) {
			node.add("sound", sound);
		}
		return node;
	}

	private static JsonObject serializeRingPulse(EffectPaletteConfig.RingPulseEvent event) {
		if (event == null) {
			return null;
		}
		JsonObject node = new JsonObject();
		if (event.primaryParticle() != null) {
			node.addProperty("primary_particle", event.primaryParticle().id().toString());
		}
		if (event.secondaryParticle() != null) {
			node.addProperty("secondary_particle", event.secondaryParticle().id().toString());
		}
		if (event.debrisBlock() != null) {
			node.addProperty("debris_block", event.debrisBlock().toString());
		}
		JsonObject ambient = serializeSound(event.ambientSound());
		if (ambient != null) {
			node.add("ambient_sound", ambient);
		}
		JsonObject pulse = serializeSound(event.pulseSound());
		if (pulse != null) {
			node.add("pulse_sound", pulse);
		}
		return node;
	}

	private static JsonObject serializeDissipation(EffectPaletteConfig.DissipationEvent event) {
		if (event == null) {
			return null;
		}
		JsonObject node = new JsonObject();
		if (event.primaryParticle() != null) {
			node.addProperty("primary_particle", event.primaryParticle().id().toString());
		}
		if (event.secondaryParticle() != null) {
			node.addProperty("secondary_particle", event.secondaryParticle().id().toString());
		}
		JsonObject sound = serializeSound(event.sound());
		if (sound != null) {
			node.add("sound", sound);
		}
		return node;
	}

	private static JsonObject serializeCollapseVeil(EffectPaletteConfig.CollapseVeilEvent event) {
		if (event == null) {
			return null;
		}
		JsonObject node = new JsonObject();
		if (event.primaryParticle() != null) {
			node.addProperty("primary_particle", event.primaryParticle().id().toString());
		}
		if (event.secondaryParticle() != null) {
			node.addProperty("secondary_particle", event.secondaryParticle().id().toString());
		}
		JsonObject sound = serializeSound(event.sound());
		if (sound != null) {
			node.add("sound", sound);
		}
		return node;
	}

	private static JsonObject serializeSound(EffectPaletteConfig.Sound sound) {
		if (sound == null) {
			return null;
		}
		JsonObject node = new JsonObject();
		node.addProperty("id", sound.id().toString());
		node.addProperty("category", sound.category());
		node.addProperty("volume", sound.volume());
		node.addProperty("pitch", sound.pitch());
		node.addProperty("interval_ticks", sound.intervalTicks());
		return node;
	}

	private static Identifier parseIdentifier(JsonElement element) {
		if (element == null || !element.isJsonPrimitive()) {
			return null;
		}
		return Identifier.tryParse(element.getAsString());
	}
}

