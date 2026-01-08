package net.cyberpunk042.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.block.BlockState;
import net.minecraft.entity.FallingBlockEntity;

@Mixin(FallingBlockEntity.class)
public interface FallingBlockEntityAccessor {
	@Accessor("blockState")
	void virus$setBlockState(BlockState state);
	
	@Accessor("timeFalling")
	int virus$getTimeFalling();
}


