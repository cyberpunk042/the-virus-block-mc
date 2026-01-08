package net.cyberpunk042.infection.singularity;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.registry.ModBlocks;
import net.cyberpunk042.infection.profile.CollapseFillMode;
import net.cyberpunk042.infection.profile.CollapseFillShape;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

/**
 * Minimal helper that emulates the vanilla /fill command for collapse purposes.
 * It replaces every block within the provided box with air, respecting
 * protected blocks when requested.
 */
public final class BulkFillHelper {
	// No longer limiting - we set commandModificationBlockLimit gamerule high
	private static final int FILL_COMMAND_LIMIT = Integer.MAX_VALUE;

	private BulkFillHelper() {
	}

	public static int clearVolume(ServerWorld world,
			BlockBox box,
			CollapseFillMode mode,
			CollapseFillShape shape,
			boolean respectProtected,
			int outlineThickness,
			boolean useNativeFill,
			int updateFlags) {
		if (shouldUseNativeFill(useNativeFill, shape, box)) {
			int result = runNativeFillForShape(world, box, mode, shape, outlineThickness);
			if (result >= 0) {
				return result;
			}
		}

		BlockPos.Mutable mutable = new BlockPos.Mutable();
		BlockState air = Blocks.AIR.getDefaultState();
		int minX = box.getMinX();
		int minY = box.getMinY();
		int minZ = box.getMinZ();
		int maxX = box.getMaxX();
		int maxY = box.getMaxY();
		int maxZ = box.getMaxZ();
		int cleared = 0;
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					if (!shouldFillShape(x, y, z, minX, minY, minZ, maxX, maxY, maxZ, shape, outlineThickness)) {
						continue;
					}
					mutable.set(x, y, z);
					BlockState state = world.getBlockState(mutable);
					if (shouldSkip(state)) {
						continue;
					}
					if (respectProtected && state.getHardness(world, mutable) < 0.0F) {
						continue;
					}
					if (mode == CollapseFillMode.DESTROY) {
						if (world.breakBlock(mutable, false)) {
							cleared++;
						}
						continue;
					}
					world.setBlockState(mutable, air, updateFlags);
					cleared++;
				}
			}
		}
		return cleared;
	}

	private static boolean shouldUseNativeFill(boolean useNativeFill, CollapseFillShape shape, BlockBox box) {
		if (!useNativeFill) {
			return false;
		}
		// All shapes can use native fill
		return true;
	}

	/**
	 * Runs native /fill command with appropriate mode for the shape.
	 * - MATRIX uses "replace" or "destroy"
	 * - OUTLINE uses "outline" (native Minecraft fill mode)
	 * - WALLS uses 4 separate wall commands (faster than outline!) with thickness support
	 * - COLUMN/ROW/VECTOR use slice-based fill
	 */
	private static int runNativeFillForShape(ServerWorld world, BlockBox box, CollapseFillMode mode, CollapseFillShape shape, int thickness) {
		return switch (shape) {
			case MATRIX -> runNativeFill(world, box, mode, "replace");
			case OUTLINE -> runNativeFill(world, box, mode, "outline");
			case WALLS -> runNativeFillWalls(world, box, mode, thickness);
			case COLUMN -> runNativeFillColumn(world, box, mode);
			case ROW -> runNativeFillRow(world, box, mode);
			case VECTOR -> runNativeFillVector(world, box, mode);
		};
	}

	/**
	 * Fill 4 vertical walls (north, south, west, east) - much faster than outline!
	 * No outline calculation needed, just 4 simple rectangular fills.
	 * Supports thickness by doing multiple passes.
	 */
	private static int runNativeFillWalls(ServerWorld world, BlockBox box, CollapseFillMode mode, int thickness) {
		int minX = box.getMinX();
		int minY = box.getMinY();
		int minZ = box.getMinZ();
		int maxX = box.getMaxX();
		int maxY = box.getMaxY();
		int maxZ = box.getMaxZ();
		
		int total = 0;
		int t = Math.max(1, thickness);
		
		// For each layer of thickness
		for (int layer = 0; layer < t; layer++) {
			int innerMinX = minX + layer;
			int innerMinZ = minZ + layer;
			int innerMaxX = maxX - layer;
			int innerMaxZ = maxZ - layer;
			
			// Skip if box became too small
			if (innerMinX >= innerMaxX || innerMinZ >= innerMaxZ) break;
			
			// North wall (minZ face)
			BlockBox north = new BlockBox(innerMinX, minY, innerMinZ, innerMaxX, maxY, innerMinZ);
			int n = runNativeFill(world, north, mode, "replace");
			if (n > 0) total += n;
			
			// South wall (maxZ face)
			BlockBox south = new BlockBox(innerMinX, minY, innerMaxZ, innerMaxX, maxY, innerMaxZ);
			int s = runNativeFill(world, south, mode, "replace");
			if (s > 0) total += s;
			
			// West wall (minX face) - exclude corners already done
			BlockBox west = new BlockBox(innerMinX, minY, innerMinZ + 1, innerMinX, maxY, innerMaxZ - 1);
			int w = runNativeFill(world, west, mode, "replace");
			if (w > 0) total += w;
			
			// East wall (maxX face) - exclude corners already done
			BlockBox east = new BlockBox(innerMaxX, minY, innerMinZ + 1, innerMaxX, maxY, innerMaxZ - 1);
			int e = runNativeFill(world, east, mode, "replace");
			if (e > 0) total += e;
		}
		
		return total;
	}

	private static int runNativeFill(ServerWorld world,
			BlockBox box,
			CollapseFillMode mode,
			String fillMode) {
		MinecraftServer server = world.getServer();
		if (server == null) {
			return -1;
		}
		ServerCommandSource source = server.getCommandSource()
				.withWorld(world)
				.withPosition(new Vec3d(box.getMinX(), box.getMinY(), box.getMinZ()))
				.withRotation(Vec2f.ZERO)
				.withLevel(4);
		// For destroy mode, use "destroy" instead of shape mode
		String modeArg = (mode == CollapseFillMode.DESTROY) ? "destroy" : fillMode;
		String command = String.format(Locale.ROOT,
				"fill %d %d %d %d %d %d minecraft:air %s",
				box.getMinX(),
				box.getMinY(),
				box.getMinZ(),
				box.getMaxX(),
				box.getMaxY(),
				box.getMaxZ(),
				modeArg);
		try {
			// Log the command being executed
			System.out.println("[BulkFillHelper] Executing: " + command);
			server.getCommandManager().executeWithPrefix(source, command);
			System.out.println("[BulkFillHelper] Command executed successfully");
			return (int) boxVolume(box);
		} catch (Exception ex) {
			System.out.println("[BulkFillHelper] Exception: " + ex.getMessage());
			ex.printStackTrace();
			return -1;
		}
	}

	/**
	 * Fill a vertical column through the center of the box.
	 */
	private static int runNativeFillColumn(ServerWorld world, BlockBox box, CollapseFillMode mode) {
		int centerX = (box.getMinX() + box.getMaxX()) / 2;
		int centerZ = (box.getMinZ() + box.getMaxZ()) / 2;
		BlockBox columnBox = new BlockBox(centerX, box.getMinY(), centerZ, centerX, box.getMaxY(), centerZ);
		return runNativeFill(world, columnBox, mode, "replace");
	}

	/**
	 * Fill a horizontal row along X axis through the center.
	 */
	private static int runNativeFillRow(ServerWorld world, BlockBox box, CollapseFillMode mode) {
		int centerY = (box.getMinY() + box.getMaxY()) / 2;
		int centerZ = (box.getMinZ() + box.getMaxZ()) / 2;
		BlockBox rowBox = new BlockBox(box.getMinX(), centerY, centerZ, box.getMaxX(), centerY, centerZ);
		return runNativeFill(world, rowBox, mode, "replace");
	}

	/**
	 * Fill along the longest axis (vector) of the box.
	 */
	private static int runNativeFillVector(ServerWorld world, BlockBox box, CollapseFillMode mode) {
		int sizeX = box.getMaxX() - box.getMinX() + 1;
		int sizeY = box.getMaxY() - box.getMinY() + 1;
		int sizeZ = box.getMaxZ() - box.getMinZ() + 1;
		
		if (sizeY >= sizeX && sizeY >= sizeZ) {
			// Y is longest - fill column
			return runNativeFillColumn(world, box, mode);
		} else if (sizeX >= sizeZ) {
			// X is longest - fill row along X
			return runNativeFillRow(world, box, mode);
		} else {
			// Z is longest - fill row along Z
			int centerX = (box.getMinX() + box.getMaxX()) / 2;
			int centerY = (box.getMinY() + box.getMaxY()) / 2;
			BlockBox rowBox = new BlockBox(centerX, centerY, box.getMinZ(), centerX, centerY, box.getMaxZ());
			return runNativeFill(world, rowBox, mode, "replace");
		}
	}

	public static boolean clearFluidRange(ServerWorld world, BlockBox box, String blockId, boolean useNativeFill) {
		if (box == null || blockId == null || blockId.isBlank()) {
			return false;
		}
		if (boxVolume(box) > FILL_COMMAND_LIMIT) {
			return false;
		}
		return runNativeFillReplace(world, box, blockId) >= 0;
	}

	/**
	 * Runs /fill with replace filter (e.g., replace only water blocks).
	 */
	private static int runNativeFillReplace(ServerWorld world, BlockBox box, String replaceFilter) {
		MinecraftServer server = world.getServer();
		if (server == null) {
			return -1;
		}
		ServerCommandSource source = server.getCommandSource()
				.withWorld(world)
				.withPosition(new Vec3d(box.getMinX(), box.getMinY(), box.getMinZ()))
				.withRotation(Vec2f.ZERO)
				.withLevel(4)
				.withSilent();
		String command = String.format(Locale.ROOT,
				"fill %d %d %d %d %d %d minecraft:air replace %s",
				box.getMinX(),
				box.getMinY(),
				box.getMinZ(),
				box.getMaxX(),
				box.getMaxY(),
				box.getMaxZ(),
				replaceFilter);
		try {
			server.getCommandManager().executeWithPrefix(source, command);
			return (int) boxVolume(box);
		} catch (Exception ex) {
			return -1;
		}
	}

	private static long boxVolume(BlockBox box) {
		return (long) (box.getMaxX() - box.getMinX() + 1)
				* (box.getMaxY() - box.getMinY() + 1)
				* (box.getMaxZ() - box.getMinZ() + 1);
	}

	public static boolean shouldFillShape(int x,
			int y,
			int z,
			int minX,
			int minY,
			int minZ,
			int maxX,
			int maxY,
			int maxZ,
			CollapseFillShape shape,
			int thickness) {
		int halfThickness = Math.max(0, (thickness - 1) / 2);
		return switch (shape) {
			case MATRIX -> true;
			case COLUMN -> {
				// Vertical column centered in X/Z, with thickness
				int centerX = (minX + maxX) >> 1;
				int centerZ = (minZ + maxZ) >> 1;
				yield Math.abs(x - centerX) <= halfThickness && Math.abs(z - centerZ) <= halfThickness;
			}
			case ROW -> {
				// Horizontal row along X axis, centered in Y/Z, with thickness
				int centerY = (minY + maxY) >> 1;
				int centerZ = (minZ + maxZ) >> 1;
				yield Math.abs(y - centerY) <= halfThickness && Math.abs(z - centerZ) <= halfThickness;
			}
			case VECTOR -> shouldFillVector(x, y, z, minX, minY, minZ, maxX, maxY, maxZ, halfThickness);
			case OUTLINE -> outlineMask(x, y, z, minX, minY, minZ, maxX, maxY, maxZ, thickness);
			case WALLS -> wallsMask(x, z, minX, minZ, maxX, maxZ, thickness);  // 4 vertical walls only
		};
	}

	private static boolean shouldFillVector(int x,
			int y,
			int z,
			int minX,
			int minY,
			int minZ,
			int maxX,
			int maxY,
			int maxZ,
			int halfThickness) {
		int sizeX = maxX - minX + 1;
		int sizeY = maxY - minY + 1;
		int sizeZ = maxZ - minZ + 1;
		// If any dimension is 1, fill everything
		if (sizeX <= 1 || sizeZ <= 1 || sizeY <= 1) {
			return true;
		}
		// Fill along the longest dimension with thickness perpendicular
		if (sizeY >= sizeX && sizeY >= sizeZ) {
			// Y is longest - fill along Y, thickness in X/Z
			int centerX = (minX + maxX) >> 1;
			int centerZ = (minZ + maxZ) >> 1;
			return Math.abs(x - centerX) <= halfThickness && Math.abs(z - centerZ) <= halfThickness;
		}
		if (sizeX >= sizeZ) {
			// X is longest - fill along X, thickness in Y/Z
			int centerY = (minY + maxY) >> 1;
			int centerZ = (minZ + maxZ) >> 1;
			return Math.abs(y - centerY) <= halfThickness && Math.abs(z - centerZ) <= halfThickness;
		}
		// Z is longest - fill along Z, thickness in X/Y
		int centerX = (minX + maxX) >> 1;
		int centerY = (minY + maxY) >> 1;
		return Math.abs(x - centerX) <= halfThickness && Math.abs(y - centerY) <= halfThickness;
	}

	private static boolean shouldSkip(BlockState state) {
		return state.isAir()
				|| state.isOf(ModBlocks.VIRUS_BLOCK)
				|| state.isOf(ModBlocks.SINGULARITY_BLOCK);
	}

	private static boolean outlineMask(int x,
			int y,
			int z,
			int minX,
			int minY,
			int minZ,
			int maxX,
			int maxY,
			int maxZ,
			int thickness) {
		if (thickness <= 0) {
			return x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
		}
		int dx = Math.min(Math.abs(x - minX), Math.abs(maxX - x));
		int dy = Math.min(Math.abs(y - minY), Math.abs(maxY - y));
		int dz = Math.min(Math.abs(z - minZ), Math.abs(maxZ - z));
		return dx < thickness || dy < thickness || dz < thickness;
	}

	/**
	 * Mask for 4 vertical walls only (north, south, west, east).
	 * Does NOT include top/bottom faces - only checks X and Z distance.
	 */
	private static boolean wallsMask(int x, int z, int minX, int minZ, int maxX, int maxZ, int thickness) {
		if (thickness <= 0) {
			return x == minX || x == maxX || z == minZ || z == maxZ;
		}
		int dx = Math.min(Math.abs(x - minX), Math.abs(maxX - x));
		int dz = Math.min(Math.abs(z - minZ), Math.abs(maxZ - z));
		return dx < thickness || dz < thickness;
	}
}

