package net.cyberpunk042.item;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.infection.VirusDifficulty;
import net.cyberpunk042.infection.VirusWorldState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public enum PurificationOption {
	NO_BOOBYTRAPS("no_boobytraps", () -> new ItemStack(Items.TNT)),
	NO_SHELL("no_shell", () -> new ItemStack(Items.OBSIDIAN)),
	HALF_HP("half_hp", () -> ItemStack.EMPTY, Identifier.of(TheVirusBlock.MOD_ID, "textures/gui/heart.png")),
	BLEED_HP("bleed_hp", () -> ItemStack.EMPTY, Identifier.of(TheVirusBlock.MOD_ID, "textures/gui/blood.png"));

	private final String key;
	private final Supplier<ItemStack> iconSupplier;
	@Nullable
	private final Identifier iconTexture;

	PurificationOption(String key, Supplier<ItemStack> iconSupplier) {
		this(key, iconSupplier, null);
	}

	PurificationOption(String key, Supplier<ItemStack> iconSupplier, @Nullable Identifier iconTexture) {
		this.key = key;
		this.iconSupplier = iconSupplier;
		this.iconTexture = iconTexture;
	}

	public ItemStack createIcon() {
		return iconSupplier.get().copy();
	}

	@Nullable
	public Identifier iconTexture() {
		return iconTexture;
	}

	public Text title() {
		return Text.translatable("screen.the-virus-block.purification_totem.option." + key);
	}

	public Text description() {
		return Text.translatable("screen.the-virus-block.purification_totem.description." + key);
	}

	public boolean isVisible(VirusDifficulty difficulty) {
		return this != BLEED_HP || difficulty.allowsBleedOption();
	}

	public void apply(ServerWorld world, ServerPlayerEntity player, VirusWorldState state) {
		if (!isVisible(state.tiers().difficulty())) {
			player.sendMessage(Text.translatable("message.the-virus-block.purification.bleed_hp_locked").formatted(Formatting.RED), true);
			return;
		}
		switch (this) {
			case NO_BOOBYTRAPS -> applyNoBoobytraps(world, player, state);
			case NO_SHELL -> applyNoShell(world, player, state);
			case HALF_HP -> applyHalfHp(world, player, state);
			case BLEED_HP -> applyBleedHp(world, player, state);
		}
		world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1.0F, 0.7F);
	}

	private static void applyNoBoobytraps(ServerWorld world, ServerPlayerEntity player, VirusWorldState state) {
		if (!state.infectionState().dormant()) {
			state.infectionLifecycle().disableBoobytraps();
			player.sendMessage(Text.translatable("message.the-virus-block.purification.no_boobytraps").formatted(Formatting.GREEN), false);
		} else {
			player.sendMessage(Text.translatable("message.the-virus-block.purification_totem.boobytraps_disabled").formatted(Formatting.GRAY), true);
		}
	}

	private static void applyNoShell(ServerWorld world, ServerPlayerEntity player, VirusWorldState state) {
		state.shell().collapse(world, state.getVirusSources());
		player.sendMessage(Text.translatable("message.the-virus-block.purification.no_shell").formatted(Formatting.RED), false);
	}

	private static void applyHalfHp(ServerWorld world, ServerPlayerEntity player, VirusWorldState state) {
		boolean reduced = state.tierProgression().reduceMaxHealth(0.5D);
		player.sendMessage(Text.translatable(
				reduced
						? "message.the-virus-block.purification.max_hp"
						: "message.the-virus-block.purification.max_hp_failed").formatted(Formatting.DARK_RED), false);
	}

	private static void applyBleedHp(ServerWorld world, ServerPlayerEntity player, VirusWorldState state) {
		boolean drained = state.tierProgression().bleedHealth(0.5D);
		player.sendMessage(Text.translatable(
				drained
						? "message.the-virus-block.purification.bleed_hp"
						: "message.the-virus-block.purification.bleed_hp_failed").formatted(Formatting.DARK_RED), false);
	}
}

