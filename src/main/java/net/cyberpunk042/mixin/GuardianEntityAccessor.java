package net.cyberpunk042.mixin;

import net.minecraft.entity.mob.GuardianEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuardianEntity.class)
public interface GuardianEntityAccessor {

	@Invoker("setBeamTarget")
	void theVirusBlock$setBeamTarget(int entityId);
}

