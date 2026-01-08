package net.cyberpunk042.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

import net.cyberpunk042.config.InfectionConfigRegistry.ConfigHandle;

/**
 * Centralized palette for client-side colors (shield meshes, guardian beams,
 * singularity FX, corrupted block tinting, etc). The JSON file maps human
 * readable names to ARGB values so multiple systems can reuse the same entry
 * without copy/pasting literal integers.
 */
public final class ColorConfig {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
			.resolve("the-virus-block")
			.resolve("colors.json");
	private static PaletteSettings palette = new PaletteSettings();
	private static final Map<String, ColorSlot> SLOT_LOOKUP = new LinkedHashMap<>();
	private static final Map<String, Integer> BASIC_COLORS = new LinkedHashMap<>();

	static {
		InfectionConfigRegistry.register(ConfigHandle.clientOnly("colors", ColorConfig::load, ColorConfig::save));
		for (ColorSlot slot : ColorSlot.values()) {
			registerSlotAlias(slot.key(), slot);
			registerSlotAlias(slot.name().toLowerCase(Locale.ROOT), slot);
		}
		registerBasicColor("white", 0xFFFFFFFF);
		registerBasicColor("black", 0xFF000000);
		registerBasicColor("gray", 0xFF808080);
		registerBasicColor("light_gray", 0xFFBFBFBF);
		registerBasicColor("dark_gray", 0xFF404040);
		registerBasicColor("red", 0xFFFF3B3B);
		registerBasicColor("crimson", 0xFFAF1E2D);
		registerBasicColor("orange", 0xFFFF8C1A);
		registerBasicColor("gold", 0xFFFFC14F);
		registerBasicColor("yellow", 0xFFFFEB3B);
		registerBasicColor("lime", 0xFF8AE234);
		registerBasicColor("green", 0xFF2ECC71);
		registerBasicColor("teal", 0xFF1ABC9C);
		registerBasicColor("cyan", 0xFF17C0EB);
		registerBasicColor("blue", 0xFF3498DB);
		registerBasicColor("navy", 0xFF1F618D);
		registerBasicColor("purple", 0xFF9B59B6);
		registerBasicColor("magenta", 0xFFE84393);
		registerBasicColor("pink", 0xFFF78FB3);
	}

	private static void registerSlotAlias(String key, ColorSlot slot) {
		if (key == null || key.isBlank()) {
			return;
		}
		SLOT_LOOKUP.put(key.toLowerCase(Locale.ROOT), slot);
	}

	private static void registerBasicColor(String name, int argb) {
		if (name == null || name.isBlank()) {
			return;
		}
		BASIC_COLORS.put(name.toLowerCase(Locale.ROOT), argb);
	}

	private ColorConfig() {
	}

	public static synchronized void load() {
		boolean dirty = false;
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				PaletteSettings loaded = GSON.fromJson(reader, PaletteSettings.class);
				if (loaded != null && loaded.namedColors != null) {
					palette = loaded;
				}
			} catch (IOException ignored) {
			}
		} else {
			dirty = true;
		}

		if (palette.namedColors == null) {
			palette.namedColors = new LinkedHashMap<>();
		}

		for (ColorSlot slot : ColorSlot.values()) {
			if (palette.namedColors.putIfAbsent(slot.key(), slot.defaultHex()) == null) {
				dirty = true;
			}
		}
		if (dirty) {
			save();
		}
	}

	public static synchronized void save() {
		try {
			if (CONFIG_PATH.getParent() != null) {
				Files.createDirectories(CONFIG_PATH.getParent());
			}
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(palette, writer);
			}
		} catch (IOException ignored) {
		}
	}

	public static ColorDefinition color(ColorSlot slot) {
		return new ColorDefinition(argb(slot));
	}

	public static int argb(ColorSlot slot) {
		String configured = palette.namedColors.get(slot.key());
		return parseColorWithFallback(configured, slot.defaultArgb());
	}

	public static Map<String, String> snapshot() {
		if (palette.namedColors == null) {
			return Map.of();
		}
		return Map.copyOf(palette.namedColors);
	}

	public static Integer resolveNamedColor(String name) {
		if (name == null) {
			return null;
		}
		String normalized = name.trim();
		if (normalized.isEmpty()) {
			return null;
		}
		if (palette.namedColors != null) {
			for (Map.Entry<String, String> entry : palette.namedColors.entrySet()) {
				if (entry.getKey().equalsIgnoreCase(normalized)) {
					try {
						return parseColorStrict(entry.getValue());
					} catch (IllegalArgumentException ignored) {
						return null;
					}
				}
			}
		}
		Integer basic = BASIC_COLORS.get(normalized.toLowerCase(Locale.ROOT));
		if (basic != null) {
			return basic;
		}
		ColorSlot slot = SLOT_LOOKUP.get(normalized.toLowerCase(Locale.ROOT));
		return slot != null ? slot.defaultArgb() : null;
	}

	public static Integer parseUserColor(String value) {
		Integer named = resolveNamedColor(value);
		if (named != null) {
			return named;
		}
		try {
			return parseColorStrict(value);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	public static List<String> colorKeys() {
		LinkedHashSet<String> keys = new LinkedHashSet<>();
		if (palette.namedColors != null) {
			keys.addAll(palette.namedColors.keySet());
		}
		for (ColorSlot slot : ColorSlot.values()) {
			keys.add(slot.key());
			keys.add(slot.name().toLowerCase(Locale.ROOT));
		}
		keys.addAll(BASIC_COLORS.keySet());
		return List.copyOf(keys);
	}

	public static List<String> basicColorKeys() {
		return List.copyOf(BASIC_COLORS.keySet());
	}

	private static int parseColorWithFallback(String value, int fallback) {
		try {
			return parseColorStrict(value);
		} catch (IllegalArgumentException ignored) {
			return fallback;
		}
	}

	private static int parseColorStrict(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Color value is blank");
		}
		String trimmed = value.trim();
		if (trimmed.startsWith("#")) {
			trimmed = trimmed.substring(1);
		} else if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
			trimmed = trimmed.substring(2);
		}
		return hexToArgb(trimmed);
	}

	private static int hexToArgb(String hex) {
		int length = hex.length();
		if (length == 6) {
			int rgb = (int) Long.parseLong(hex, 16);
			return 0xFF000000 | rgb;
		}
		if (length == 8) {
			return (int) Long.parseLong(hex, 16);
		}
		if (length == 3) {
			int r = Character.digit(hex.charAt(0), 16);
			int g = Character.digit(hex.charAt(1), 16);
			int b = Character.digit(hex.charAt(2), 16);
			if (r < 0 || g < 0 || b < 0) {
				throw new IllegalArgumentException("Invalid short hex color: " + hex);
			}
			int rr = (r << 4) | r;
			int gg = (g << 4) | g;
			int bb = (b << 4) | b;
			return 0xFF000000 | (rr << 16) | (gg << 8) | bb;
		}
		throw new IllegalArgumentException("Unsupported hex color length: " + length);
	}

	public record ColorDefinition(int argb) {
		public float red() {
			return ((argb >>> 16) & 0xFF) / 255.0F;
		}

		public float green() {
			return ((argb >>> 8) & 0xFF) / 255.0F;
		}

		public float blue() {
			return (argb & 0xFF) / 255.0F;
		}

		public int withAlpha(float alpha) {
			int clamped = (int) (Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F) & 0xFF;
			return withAlpha(clamped);
		}

		public int withAlpha(int alphaByte) {
			int clamped = Math.max(0, Math.min(255, alphaByte));
			return (clamped << 24) | (argb & 0x00FFFFFF);
		}
	}

	private static final class PaletteSettings {
		Map<String, String> namedColors = new LinkedHashMap<>();
	}

	public enum ColorSlot {
		CORRUPTED_CRYING_OBSIDIAN("corruptedCryingObsidian", 0xFF7C44FF, "Primary tint for corrupted crying obsidian blocks"),
		CORRUPTED_DIAMOND("corruptedDiamond", 0xFF4BFFE3, "Primary tint for corrupted diamond blocks"),
		CORRUPTED_GOLD("corruptedGold", 0xFFFFC54F, "Primary tint for corrupted gold blocks"),
		CORRUPTED_FIRE("corruptedFire", 0xFFA11A1A, "Custom fire color when the infection corrupts vanilla fire"),
		CORRUPTED_SOUL_FIRE("corruptedSoulFire", 0xFF4D0F7A, "Custom fire color when the infection corrupts soul fire"),
		CORRUPTED_LAVA("corruptedLava", 0xFF8A1818, "Overlay applied to lava while fluids are corrupted"),
		CORRUPTED_WATER("corruptedWater", 0xFF2F7A2F, "Overlay applied to water while fluids are corrupted"),
		SINGULARITY_BORDER_COLLAPSE("singularityBorderCollapse", 0xFF050708, "Barrier color while the shell is collapsing"),
		SINGULARITY_BORDER_CORE("singularityBorderCore", 0xFFB00E1D, "Barrier color during the core pulse"),
		SINGULARITY_BORDER_FLARE("singularityBorderFlare", 0xFFFF3A2F, "Hot streak color while the core overcharges"),
		SINGULARITY_BORDER_DISSIPATE("singularityBorderDissipate", 0xFF32163A, "Barrier color used while dissipating"),
		SINGULARITY_BEAM_PRIMARY("singularityBeamPrimary", 0xFFF64A4A, "Primary guardian beam color during shell collapse"),
		SINGULARITY_BEAM_SECONDARY("singularityBeamSecondary", 0xFF050505, "Secondary guardian beam color / black phase"),
		SHIELD_FIELD_PRIMARY("shieldFieldPrimary", 0xFF1B1F3C, "Default shield field primary tint when profiles omit one"),
		SHIELD_FIELD_SECONDARY("shieldFieldSecondary", 0xFFE53F4F, "Default shield field secondary tint"),
		UI_DIFFICULTY_BACKGROUND("uiDifficultyBackground", 0xAA000000, "Virus difficulty screen background fill"),
		UI_DIFFICULTY_BORDER("uiDifficultyBorder", 0xFF552266, "Virus difficulty screen border"),
		UI_WARNING_TEXT("uiWarningText", 0xFFCC5555, "Warning text accent color for UI overlays"),
		UI_DIFFICULTY_BUTTON_BORDER_SELECTED("uiDifficultyButtonBorderSelected", 0xFF66FFAA, "Selected difficulty button border"),
		UI_DIFFICULTY_BUTTON_BORDER_HOVER("uiDifficultyButtonBorderHover", 0xFF8888EE, "Hovered difficulty button border"),
		UI_DIFFICULTY_BUTTON_BORDER_IDLE("uiDifficultyButtonBorderIdle", 0xFF444444, "Idle difficulty button border"),
		UI_DIFFICULTY_BUTTON_FILL_IDLE("uiDifficultyButtonFillIdle", 0x66000000, "Idle difficulty button fill"),
		UI_DIFFICULTY_BUTTON_FILL_HOVER("uiDifficultyButtonFillHover", 0x88333366, "Hovered difficulty button fill"),
		UI_PURIFICATION_BACKGROUND("uiPurificationBackground", 0xCC060606, "Purification totem screen background fill"),
		UI_PURIFICATION_BORDER("uiPurificationBorder", 0xFF6E3FDB, "Purification totem screen border"),
		UI_PURIFICATION_TITLE("uiPurificationTitle", 0xFF404040, "Purification totem title color"),
		UI_PURIFICATION_OPTION_BORDER_IDLE("uiPurificationOptionBorderIdle", 0xFF6E3FDB, "Purification option border (inactive)"),
		UI_PURIFICATION_OPTION_BORDER_HOVER("uiPurificationOptionBorderHover", 0xFFE6A90F, "Purification option border (hovered)"),
		UI_PURIFICATION_OPTION_FILL_IDLE("uiPurificationOptionFillIdle", 0x66202020, "Purification option fill (inactive)"),
		UI_PURIFICATION_OPTION_FILL_HOVER("uiPurificationOptionFillHover", 0x66FFD088, "Purification option fill (hovered)");

		private final String key;
		private final int defaultArgb;
		private final String description;

		ColorSlot(String key, int defaultArgb, String description) {
			this.key = key;
			this.defaultArgb = defaultArgb;
			this.description = description;
		}

		public String key() {
			return key;
		}

		public int defaultArgb() {
			return defaultArgb;
		}

		public String defaultHex() {
			return String.format("#%08X", defaultArgb);
		}

		public String description() {
			return description;
		}
	}
}

