package net.cyberpunk042.infection.singularity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;

/**
 * Provides precomputed slice column layouts so that ring-slice mode can visit a
 * full 16-column stripe of a chunk in a single step. Each slice is one block
 * thick and there are exactly 16 slices per chunk per facing.
 */
public final class SingularityRingSlices {
	public static final int SLICE_COUNT = 16;

	private static final int[][][] SLICE_TABLE = buildSliceTable();
	private static final int[] EMPTY = new int[0];

	private SingularityRingSlices() {
	}

	public static SliceFacing resolve(ChunkPos chunk, @Nullable BlockPos center) {
		if (center == null) {
			return SliceFacing.EAST;
		}
		double dx = chunk.getCenterX() - (center.getX() + 0.5D);
		double dz = chunk.getCenterZ() - (center.getZ() + 0.5D);
		if (Math.abs(dx) >= Math.abs(dz)) {
			return dx >= 0.0D ? SliceFacing.EAST : SliceFacing.WEST;
		}
		return dz >= 0.0D ? SliceFacing.SOUTH : SliceFacing.NORTH;
	}

	public static int[] columnsForSlice(SliceFacing facing, int sliceIndex) {
		if (sliceIndex < 0 || sliceIndex >= SLICE_COUNT) {
			return EMPTY;
		}
		return SLICE_TABLE[facing.ordinal()][sliceIndex];
	}

	public static BlockBox boundsForSlice(ChunkPos chunk,
			SliceFacing facing,
			int sliceIndex,
			int minY,
			int maxY) {
		int depth = MathHelper.clamp(sliceIndex, 0, SLICE_COUNT - 1);
		int startX = chunk.getStartX();
		int endX = chunk.getEndX();
		int startZ = chunk.getStartZ();
		int endZ = chunk.getEndZ();
		int clampedMinY = Math.min(minY, maxY);
		int clampedMaxY = Math.max(minY, maxY);

		if (facing.axisX()) {
			int localX = facing.positive() ? (SLICE_COUNT - 1 - depth) : depth;
			int worldX = startX + localX;
			return new BlockBox(worldX, clampedMinY, startZ, worldX, clampedMaxY, endZ);
		}
		int localZ = facing.positive() ? (SLICE_COUNT - 1 - depth) : depth;
		int worldZ = startZ + localZ;
		return new BlockBox(startX, clampedMinY, worldZ, endX, clampedMaxY, worldZ);
	}

	public static BlockBox expandForOutline(BlockBox original,
			ChunkPos chunk,
			SliceFacing facing,
			int thickness) {
		if (original == null || thickness <= 1) {
			return original;
		}
		int extra = Math.max(0, thickness - 1);
		int minX = original.getMinX();
		int maxX = original.getMaxX();
		int minZ = original.getMinZ();
		int maxZ = original.getMaxZ();
		if (facing.axisX()) {
			minX = Math.max(chunk.getStartX(), minX - extra);
			maxX = Math.min(chunk.getEndX(), maxX + extra);
		} else {
			minZ = Math.max(chunk.getStartZ(), minZ - extra);
			maxZ = Math.min(chunk.getEndZ(), maxZ + extra);
		}
		return new BlockBox(minX, original.getMinY(), minZ, maxX, original.getMaxY(), maxZ);
	}

	private static int[][][] buildSliceTable() {
		SliceFacing[] faces = SliceFacing.values();
		int[][][] table = new int[faces.length][SLICE_COUNT][];
		for (SliceFacing facing : faces) {
			for (int slice = 0; slice < SLICE_COUNT; slice++) {
				table[facing.ordinal()][slice] = buildSliceColumns(facing, slice);
			}
		}
		return table;
	}

	private static int[] buildSliceColumns(SliceFacing facing, int slice) {
		int[] columns = new int[SLICE_COUNT];
		int write = 0;
		if (facing.axisX()) {
			int localX = facing.positive() ? (SLICE_COUNT - 1 - slice) : slice;
			localX = clampColumn(localX);
			for (int localZ = 0; localZ < SLICE_COUNT; localZ++) {
				columns[write++] = encode(localX, localZ);
			}
		} else {
			int localZ = facing.positive() ? (SLICE_COUNT - 1 - slice) : slice;
			localZ = clampColumn(localZ);
			for (int localX = 0; localX < SLICE_COUNT; localX++) {
				columns[write++] = encode(localX, localZ);
			}
		}
		return columns;
	}

	private static int clampColumn(int value) {
		if (value < 0) {
			return 0;
		}
		if (value >= SLICE_COUNT) {
			return SLICE_COUNT - 1;
		}
		return value;
	}

	private static int encode(int localX, int localZ) {
		return (localZ << 4) | (localX & 15);
	}

	public enum SliceFacing {
		EAST(true, true),
		WEST(true, false),
		SOUTH(false, true),
		NORTH(false, false);

		private final boolean axisX;
		private final boolean positive;

		SliceFacing(boolean axisX, boolean positive) {
			this.axisX = axisX;
			this.positive = positive;
		}

		public boolean axisX() {
			return axisX;
		}

		public boolean positive() {
			return positive;
		}
	}
}

