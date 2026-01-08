package net.cyberpunk042.infection.service;

import java.util.ArrayList;
import java.util.List;


/**
 * JSON-backed configuration for service-level knobs (watchdogs, alerts, etc.).
 */
public final class ServiceConfig {
	public Watchdog watchdog = new Watchdog();
	public Effects effects = new Effects();
	public Guardian guardian = new Guardian();
	public Audio audio = new Audio();
	public Singularity singularity = new Singularity();
	public Fuse fuse = new Fuse();
	public Diagnostics diagnostics = new Diagnostics();

	public static ServiceConfig defaults() {
		return new ServiceConfig();
	}

	public static final class Watchdog {
		public Collapse collapse = new Collapse();
		public SingularityWatchdogs singularity = new SingularityWatchdogs();
		public Scheduler scheduler = new Scheduler();
	}

	public static final class Collapse {
		public int stallTicks = 200;
	}

	public static final class SingularityWatchdogs {
		public FuseWatchdog fuse = new FuseWatchdog();
		public CollapsePhaseWatchdog collapse = new CollapsePhaseWatchdog();
	}

	public static final class FuseWatchdog {
		public boolean enabled = false;
		public int maxExtraTicks = 200;
		public boolean autoSkip = false;
	}

	public static final class CollapsePhaseWatchdog {
		public boolean enabled = false;
		public int warnTicks = 200;
		public int abortTicks = 400;
		public boolean autoSkip = false;
	}

	public static final class Scheduler {
		public boolean enabled = false;
		public int warnTasks = 32;
		public int abortTasks = 64;
	}

	public static final class Effects {
		public boolean core = true;
		public boolean ring = true;
		public boolean collapseVeil = true;
		public boolean dissipation = true;
	}

	public static final class Guardian {
		public boolean beams = true;
		public double pushRadius = 12.0D;
		public double pushStrength = 2.0D;
		public boolean knockbackEnabled = true;
		public int beamDurationTicks = 60;

		public GuardianFxService.GuardianKnockbackSettings getKnockbackSettings(int radius, double strengthScale, boolean spawnGuardian) {
			double effectiveRadius = radius;
			double effectiveStrengthScale = strengthScale;
			if (spawnGuardian) {
				if (pushRadius > 0) {
					effectiveRadius = pushRadius;
				}
				if (pushStrength > 0) {
					effectiveStrengthScale *= pushStrength;
				}
			}
			double pushRadiusValue = Math.max(4.0D, effectiveRadius + 4.0D);
			double scale = Math.max(0.1D, effectiveStrengthScale);
			double baseStrengthFactor = (1.2D + effectiveRadius * 0.15D) * scale;
			double verticalFactor = (0.5D + effectiveRadius * 0.05D) * scale;
			boolean apply = !spawnGuardian || knockbackEnabled;
			return new GuardianFxService.GuardianKnockbackSettings(pushRadiusValue, baseStrengthFactor, verticalFactor, apply);
		}
	}

	public static final class Audio {
		public boolean core = true;
		public boolean ring = true;
		public boolean collapseVeil = true;
		public boolean dissipation = true;
	}

	public static final class Singularity {
		public int collapseBarDelayTicks = 60;
		public int collapseCompleteHoldTicks = 40;
		public int coreChargeTicks = 80;
		public int resetDelayTicks = 160;
		public PostReset postReset = new PostReset();
		public SingularityExecution execution = new SingularityExecution();
		public Visuals visuals = new Visuals();
	}

	public static final class PostReset {
		public boolean enabled = true;
		public int delayTicks = 25;
		public int tickDelay = 1;
		public int chunksPerTick = 2;
		public int batchRadius = 1;
	}

	public static final class Visuals {
		public HorizonDarkening horizonDarkening = new HorizonDarkening();
	}

	public static final class HorizonDarkening {
		public boolean enabled = true;
		public String color = "#100107";
		public float maxIntensity = 0.85F;
		public int fadeInTicks = 60;
		public int fadeOutTicks = 80;
		public int startDelayTicks = 20;
		public boolean includeDissipation = true;
		public List<String> triggerPhases = new ArrayList<>(List.of("FUSING", "COLLAPSE", "CORE", "RING"));
	}

	public static final class Fuse {
		public int explosionDelayTicks = 400;
		public int shellCollapseTicks = 20;
		public int pulseIntervalTicks = 8;
	}

	public static final class SingularityExecution {
		public boolean collapseEnabled = true;
		public boolean allowChunkGeneration = true;
		public boolean allowOutsideBorderLoad = true;
		// Legacy fields removed: multithreaded, workerCount, mode
		// CollapseProcessor is now the only collapse system
	}

	public static final class Diagnostics {
		public boolean enabled = true;
		public boolean logChunkSamples = true;
		public boolean logBypasses = true;
		public int logSampleIntervalTicks = 20;
		public LogSpamSettings logSpam = new LogSpamSettings();
	}

	public static final class LogSpamSettings {
		public boolean enableSpamDetection = true;
		public int perSecondThreshold = 10;
		public int perMinuteThreshold = 200;
		public boolean suppressWhenTriggered = true;
	}
}

