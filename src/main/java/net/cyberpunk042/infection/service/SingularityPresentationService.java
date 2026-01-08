package net.cyberpunk042.infection.service;

import java.util.Objects;
import java.util.function.Predicate;

import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.api.EffectBus;
import net.cyberpunk042.infection.service.GuardianFxService.GuardianResult;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Bridges guardian FX pushes and HUD payload dispatching so {@link
 * VirusWorldState} no longer pokes the individual services directly.
 */
public final class SingularityPresentationService {
	private final SingularityHudService hud;
	private final GuardianFxService guardianFx;
	private final HorizonDarkeningController horizon;

	public SingularityPresentationService() {
		this(new SingularityHudService(), new GuardianFxService());
	}

	public SingularityPresentationService(SingularityHudService hud, GuardianFxService guardianFx) {
		this(hud, guardianFx, new HorizonDarkeningController());
	}

	SingularityPresentationService(SingularityHudService hud,
			GuardianFxService guardianFx,
			HorizonDarkeningController horizon) {
		this.hud = Objects.requireNonNull(hud, "hud");
		this.guardianFx = Objects.requireNonNull(guardianFx, "guardianFx");
		this.horizon = Objects.requireNonNull(horizon, "horizon");
	}

	public GuardianResult pushPlayers(ServerWorld world, BlockPos center,
			int radius,
			double strengthScale,
			boolean spawnGuardian,
			double difficultyKnockback,
			Predicate<ServerPlayerEntity> filter,
			EffectBus effectBus) {
		return guardianFx.pushPlayers(world, center, radius, strengthScale, spawnGuardian, difficultyKnockback, filter, effectBus);
	}

	public void notifyShieldStatus(ServerWorld world, BlockPos pos, boolean active, double radiusSq) {
		guardianFx.notifyShieldStatus(world, pos, active, radiusSq);
	}

	public void notifyShieldFailure(ServerWorld world, BlockPos pos) {
		guardianFx.notifyShieldFailure(world, pos);
	}

	public void updateBossBars(ServerWorld world, VirusWorldState state) {
		hud.updateBossBars(world, state);
		horizon.update(world, state, hud);
	}

	public void syncBorder(ServerWorld world, SingularityHudService.BorderSyncData data) {
		hud.syncBorder(world, data);
	}

	public void broadcastPayload(ServerWorld world, CustomPayload payload) {
		hud.broadcastPayload(world, payload);
	}

	public void broadcastPayload(ServerWorld world, CustomPayload payload, Predicate<ServerPlayerEntity> filter) {
		hud.broadcastPayload(world, payload, filter);
	}

	public void sendPayload(ServerPlayerEntity player, CustomPayload payload) {
		hud.sendPayload(player, payload);
	}
}

