package net.cyberpunk042.util;

import net.cyberpunk042.TheVirusBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;

public final class VirusMobAllyHelper {
	private static final String ALLY_TAG = TheVirusBlock.MOD_ID + ":virus_ally";

	private VirusMobAllyHelper() {
	}

	public static void mark(MobEntity mob) {
		if (mob == null) {
			return;
		}
		mob.addCommandTag(ALLY_TAG);
	}

	public static boolean isAlly(Entity entity) {
		return entity instanceof LivingEntity living && isAlly(living);
	}

	public static boolean isAlly(LivingEntity living) {
		return living != null && living.getCommandTags().contains(ALLY_TAG);
	}
}

