package net.cyberpunk042.infection.state;

import net.cyberpunk042.log.Logging;

import net.cyberpunk042.infection.InfectionTier;
import net.cyberpunk042.infection.VirusWorldState;
import net.cyberpunk042.infection.service.AmbientPressureService;
import net.cyberpunk042.infection.service.EffectService;
import net.cyberpunk042.infection.service.HelmetTelemetryService;
import net.cyberpunk042.infection.service.InfectionExposureService;
import net.cyberpunk042.infection.service.VirusBlockTelemetryService;
import net.cyberpunk042.infection.service.GuardianSpawnService;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.cyberpunk042.infection.service.InfectionServices;
import net.cyberpunk042.infection.service.TierEventService;
import net.cyberpunk042.infection.service.VoidTearService;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Facade module consolidating combat and effect-related services.
 * Manages effects, exposure, ambient pressure, helmet telemetry, and tier events.
 */
public final class CombatModule {

	private final VirusWorldState host;
	private final EffectService effectService = new EffectService();
	private final InfectionExposureService exposureService;
	private final AmbientPressureService ambientPressureService;
	private final HelmetTelemetryService helmetTelemetryService;
	private final VirusBlockTelemetryService virusBlockTelemetryService;
	private final VoidTearService voidTearService;
	private final TierEventService tierEventService;
	@Nullable
	private transient GuardianSpawnService guardianSpawnService;

	public CombatModule(VirusWorldState host) {
		this.host = host;
		this.exposureService = new InfectionExposureService(host);
		this.ambientPressureService = new AmbientPressureService(host);
		this.helmetTelemetryService = new HelmetTelemetryService(host);
		this.virusBlockTelemetryService = new VirusBlockTelemetryService(host);
		this.voidTearService = new VoidTearService(host);
		this.tierEventService = new TierEventService(host, voidTearService);
		InfectionServiceContainer services = InfectionServices.container();
		if (services != null) {
			installGuardianSpawnService(services.guardianSpawnService());
		}
	}

	public void installGuardianSpawnService(@Nullable GuardianSpawnService service) {
		this.guardianSpawnService = service != null ? service : new GuardianSpawnService();
	}

	public GuardianSpawnService guardianSpawnService() {
		if (guardianSpawnService == null) {
			InfectionServiceContainer container = InfectionServices.container();
			GuardianSpawnService service = container != null 
					? container.guardianSpawnService() 
					: null;
			installGuardianSpawnService(service);
		}
		return guardianSpawnService;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Service accessors
	// ─────────────────────────────────────────────────────────────────────────────

	public EffectService effects() {
		return effectService;
	}

	public InfectionExposureService exposure() {
		return exposureService;
	}

	public AmbientPressureService ambientPressure() {
		return ambientPressureService;
	}

	public HelmetTelemetryService helmetTelemetry() {
		return helmetTelemetryService;
	}

	public VoidTearService voidTears() {
		return voidTearService;
	}

	public TierEventService tierEvents() {
		return tierEventService;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Tick (called during infection tick)
	// ─────────────────────────────────────────────────────────────────────────────

	/**
	 * Ticks combat-related services.
	 * <p>
	 * NOTE: Consider adding try-catch around each service for defensive error isolation.
	 */
	public void tick(ServerWorld world, InfectionTier tier) {
		tierEventService.runTierEvents(tier);
		ambientPressureService.tick(tier);
		helmetTelemetryService.tick();
		virusBlockTelemetryService.tick();
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// Aura calculations
	// ─────────────────────────────────────────────────────────────────────────────

	public double getActiveAuraRadius() {
		double radius = Math.max(3.0D, host.tiers().currentTier().getBaseAuraRadius() - host.tiers().containmentLevel() * 0.6D);
		if (host.tiers().isApocalypseMode()) {
			radius += 4.0D;
		}
		return radius;
	}

	public boolean isWithinAura(BlockPos pos) {
		if (!host.infectionState().infected() || !host.hasVirusSources()) {
			return false;
		}
		double radius = getActiveAuraRadius();
		double radiusSq = radius * radius;
		Vec3d target = Vec3d.ofCenter(pos);
		for (BlockPos source : host.getVirusSources()) {
			if (source.toCenterPos().squaredDistanceTo(target) <= radiusSq) {
				return true;
			}
		}
		return false;
	}
}

