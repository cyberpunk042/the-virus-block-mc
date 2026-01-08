package net.cyberpunk042.registry;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.entity.CorruptedTntEntity;
import net.cyberpunk042.entity.CorruptedWormEntity;
import net.cyberpunk042.entity.FallingMatrixCubeEntity;
import net.cyberpunk042.entity.VirusFuseEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

@SuppressWarnings("deprecation")
public final class ModEntities {

	public static final RegistryKey<EntityType<?>> FALLING_MATRIX_CUBE_KEY =
			RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(TheVirusBlock.MOD_ID, "falling_matrix_cube"));
	public static final RegistryKey<EntityType<?>> CORRUPTED_WORM_KEY =
			RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(TheVirusBlock.MOD_ID, "corrupted_worm"));
	public static final RegistryKey<EntityType<?>> CORRUPTED_TNT_KEY =
			RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(TheVirusBlock.MOD_ID, "corrupted_tnt"));
	public static final RegistryKey<EntityType<?>> VIRUS_FUSE_KEY =
			RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(TheVirusBlock.MOD_ID, "virus_fuse"));

	public static final EntityType<FallingMatrixCubeEntity> FALLING_MATRIX_CUBE =
			FabricEntityTypeBuilder.<FallingMatrixCubeEntity>create(SpawnGroup.MISC, FallingMatrixCubeEntity::new)
					.dimensions(EntityDimensions.fixed(0.98F, 0.98F))
					.trackRangeBlocks(64)
					.trackedUpdateRate(10)
					.build(FALLING_MATRIX_CUBE_KEY);
	public static final EntityType<CorruptedWormEntity> CORRUPTED_WORM =
			FabricEntityTypeBuilder.<CorruptedWormEntity>create(SpawnGroup.MONSTER, CorruptedWormEntity::new)
					.dimensions(EntityDimensions.fixed(0.4F, 0.3F))
					.trackRangeBlocks(32)
					.trackedUpdateRate(3)
					.build(CORRUPTED_WORM_KEY);
	public static final EntityType<CorruptedTntEntity> CORRUPTED_TNT =
			FabricEntityTypeBuilder.<CorruptedTntEntity>create(SpawnGroup.MISC, CorruptedTntEntity::new)
					.dimensions(EntityDimensions.fixed(0.98F, 0.98F))
					.trackRangeBlocks(10)
					.trackedUpdateRate(10)
					.fireImmune()
					.build(CORRUPTED_TNT_KEY);
	public static final EntityType<VirusFuseEntity> VIRUS_FUSE =
			FabricEntityTypeBuilder.<VirusFuseEntity>create(SpawnGroup.MISC, VirusFuseEntity::new)
					.dimensions(EntityDimensions.fixed(0.98F, 0.98F))
					.trackRangeBlocks(32)
					.trackedUpdateRate(1)
					.fireImmune()
					.build(VIRUS_FUSE_KEY);

	private ModEntities() {
	}

	public static void bootstrap() {
		Registry.register(Registries.ENTITY_TYPE, FALLING_MATRIX_CUBE_KEY, FALLING_MATRIX_CUBE);
		Registry.register(Registries.ENTITY_TYPE, CORRUPTED_WORM_KEY, CORRUPTED_WORM);
		Registry.register(Registries.ENTITY_TYPE, CORRUPTED_TNT_KEY, CORRUPTED_TNT);
		Registry.register(Registries.ENTITY_TYPE, VIRUS_FUSE_KEY, VIRUS_FUSE);
		FabricDefaultAttributeRegistry.register(CORRUPTED_WORM, SilverfishEntity.createSilverfishAttributes());
	}
}

