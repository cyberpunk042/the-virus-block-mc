package net.cyberpunk042.registry;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.block.entity.MatrixCubeBlockEntity;
import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.block.entity.SingularityBlockEntity;
import net.cyberpunk042.block.entity.VirusBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {
	public static final BlockEntityType<VirusBlockEntity> VIRUS_BLOCK = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.of(TheVirusBlock.MOD_ID, "virus_block"),
			FabricBlockEntityTypeBuilder.create(VirusBlockEntity::new, ModBlocks.VIRUS_BLOCK).build()
	);

	public static final BlockEntityType<SingularityBlockEntity> SINGULARITY_BLOCK = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.of(TheVirusBlock.MOD_ID, "singularity_block"),
			FabricBlockEntityTypeBuilder.create(SingularityBlockEntity::new, ModBlocks.SINGULARITY_BLOCK).build()
	);

	public static final BlockEntityType<MatrixCubeBlockEntity> MATRIX_CUBE = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.of(TheVirusBlock.MOD_ID, "matrix_cube"),
			FabricBlockEntityTypeBuilder.create(MatrixCubeBlockEntity::new, ModBlocks.MATRIX_CUBE).build()
	);

	public static final BlockEntityType<ProgressiveGrowthBlockEntity> PROGRESSIVE_GROWTH = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.of(TheVirusBlock.MOD_ID, "progressive_growth_block"),
			FabricBlockEntityTypeBuilder.create(ProgressiveGrowthBlockEntity::new, ModBlocks.PROGRESSIVE_GROWTH_BLOCK).build()
	);

	private ModBlockEntities() {
	}

	public static void bootstrap() {
		// trigger static init
	}
}

