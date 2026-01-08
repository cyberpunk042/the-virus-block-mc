package net.cyberpunk042.block.growth;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.cyberpunk042.TheVirusBlock;
import net.cyberpunk042.block.entity.ProgressiveGrowthBlockEntity;
import net.cyberpunk042.growth.GrowthBlockDefinition;
import net.cyberpunk042.growth.GrowthRegistry;
import net.cyberpunk042.growth.scheduler.GrowthOverrides;
import net.cyberpunk042.infection.service.InfectionServiceContainer;
import net.cyberpunk042.infection.service.InfectionServices;
import net.cyberpunk042.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Block wrapper for progressive growth profiles. Rendering/FX are handled by
 * the associated block entity.
 */
public class ProgressiveGrowthBlock extends BlockWithEntity {
	public static final IntProperty LIGHT_LEVEL = IntProperty.of("light_level", 0, 15);
	public static final MapCodec<ProgressiveGrowthBlock> CODEC = createCodec(ProgressiveGrowthBlock::new);
	private static final String NBT_DEFINITION_ID = "DefinitionId";
	private static final String NBT_OVERRIDES = "Overrides";
	private static final Identifier BLOCK_ENTITY_ID = Identifier.of(TheVirusBlock.MOD_ID, "progressive_growth_block");

	public ProgressiveGrowthBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager.getDefaultState().with(LIGHT_LEVEL, 10));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(LIGHT_LEVEL);
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.INVISIBLE;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new ProgressiveGrowthBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		if (world.isClient) {
			return validateTicker(type, ModBlockEntities.PROGRESSIVE_GROWTH, ProgressiveGrowthBlockEntity::clientTick);
		}
		return validateTicker(type, ModBlockEntities.PROGRESSIVE_GROWTH, ProgressiveGrowthBlockEntity::serverTick);
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
		super.onPlaced(world, pos, state, placer, itemStack);
		if (world.isClient) {
			return;
		}
		if (!(world.getBlockEntity(pos) instanceof ProgressiveGrowthBlockEntity growth)) {
			return;
		}
		InfectionServiceContainer c = InfectionServices.container();
		GrowthRegistry registry = c != null ? c.growth() : null;
		Identifier fromStack = readDefinitionId(itemStack);
		Identifier defaultId = registry != null ? registry.defaultDefinition().id() : GrowthBlockDefinition.defaults().id();
		growth.setDefinitionId(fromStack != null ? fromStack : defaultId);
		GrowthOverrides overrides = readOverrides(itemStack);
		if (!overrides.isEmpty()) {
			growth.replaceOverrides(overrides);
		}
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return resolveShape(world, pos, ShapeType.OUTLINE);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return resolveShape(world, pos, ShapeType.COLLISION);
	}

	@Override
	public VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return resolveShape(world, pos, ShapeType.CAMERA);
	}

	@Override
	public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
		VoxelShape shape = resolveShape(world, pos, ShapeType.RAYTRACE);
		return shape.isEmpty() ? super.getRaycastShape(state, world, pos) : shape;
	}

	@Override
	protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof ProgressiveGrowthBlockEntity growth)) {
			return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
		}
		if (world.isClient) {
			return growth.canManualFusePreview(ProgressiveGrowthBlockEntity.ManualFuseCause.INTERACT, stack)
					? ActionResult.SUCCESS
					: ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
		}
		boolean armed = growth.handleManualFuse((ServerWorld) world, player, hand, ProgressiveGrowthBlockEntity.ManualFuseCause.INTERACT);
		if (armed) {
			player.swingHand(hand, true);
			return ActionResult.SUCCESS;
		}
		return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
	}

	@Override
	public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
		if (!world.isClient && world.getBlockEntity(pos) instanceof ProgressiveGrowthBlockEntity growth) {
			growth.handleManualFuse((ServerWorld) world, player, Hand.MAIN_HAND, ProgressiveGrowthBlockEntity.ManualFuseCause.ATTACK);
		}
		super.onBlockBreakStart(state, world, pos, player);
	}

	private VoxelShape resolveShape(BlockView world, BlockPos pos, ShapeType type) {
		if (world.getBlockEntity(pos) instanceof ProgressiveGrowthBlockEntity growth) {
			return growth.shape(type);
		}
		return type == ShapeType.COLLISION ? VoxelShapes.empty() : VoxelShapes.fullCube();
	}

	public enum ShapeType {
		OUTLINE,
		COLLISION,
		CAMERA,
		RAYTRACE
	}

	public static void applyDefinitionId(ItemStack stack, Identifier id) {
		if (stack == null || id == null) {
			return;
		}
		NbtComponent.set(DataComponentTypes.BLOCK_ENTITY_DATA, stack, beTag -> {
			ensureBlockEntityId(beTag);
			beTag.putString(NBT_DEFINITION_ID, id.toString());
		});
	}

	@Nullable
	public static Identifier readDefinitionId(ItemStack stack) {
		if (stack == null) {
			return null;
		}
		NbtComponent beComponent = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
		if (beComponent == null) {
			return null;
		}
		NbtCompound beTag = beComponent.copyNbt();
		if (!beTag.contains(NBT_DEFINITION_ID)) {
			return null;
		}
		String raw = beTag.getString(NBT_DEFINITION_ID).orElse(null);
		return raw != null ? Identifier.tryParse(raw) : null;
	}

	public static GrowthOverrides readOverrides(ItemStack stack) {
		if (stack == null) {
			return GrowthOverrides.empty();
		}
		NbtComponent beComponent = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
		if (beComponent == null) {
			return GrowthOverrides.empty();
		}
		String raw = beComponent.copyNbt().getString(NBT_OVERRIDES).orElse("");
		return GrowthOverrides.fromSnbt(raw);
	}

	public static void applyOverrides(ItemStack stack, GrowthOverrides overrides) {
		if (stack == null) {
			return;
		}
		if (overrides == null || overrides.isEmpty()) {
			NbtComponent.set(DataComponentTypes.BLOCK_ENTITY_DATA, stack, beTag -> {
				ensureBlockEntityId(beTag);
				beTag.remove(NBT_OVERRIDES);
			});
		} else {
			String snbt = overrides.toSnbt();
			NbtComponent.set(DataComponentTypes.BLOCK_ENTITY_DATA, stack, beTag -> {
				ensureBlockEntityId(beTag);
				beTag.putString(NBT_OVERRIDES, snbt);
			});
		}
	}

	private static void ensureBlockEntityId(NbtCompound tag) {
		if (tag == null) {
			return;
		}
		tag.putString("id", BLOCK_ENTITY_ID.toString());
	}
}

