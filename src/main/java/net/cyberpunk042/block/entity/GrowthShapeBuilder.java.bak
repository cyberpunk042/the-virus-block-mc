package net.cyberpunk042.block.entity;


import net.cyberpunk042.log.Logging;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.cyberpunk042.collision.GrowthCollisionDebug;
import net.cyberpunk042.collision.GrowthCollisionDebug.ShapeMode;
import net.cyberpunk042.growth.GrowthBlockDefinition;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

/**
 * Builds VoxelShapes for growth block outline and collision.
 */
public class GrowthShapeBuilder {

    private static final double INTERACTION_EPSILON = 0.02D;
    private static final double SHELL_WALL_THICKNESS = 1.0D / 16.0D; // 0.0625
    private static final double SHELL_CAP_THICKNESS = 4.0D / 16.0D;  // 0.25
    private static final double SHELL_WALL_HEIGHT = 12.0D / 16.0D;   // 0.75
    private static final double SHELL_MIN_HEIGHT = 0.25D;

    public record ShapeResult(
            VoxelShape outlineShape,
            VoxelShape collisionShape,
            @Nullable List<Box> collisionPanels,
            Box renderBounds
    ) {}

    private GrowthShapeBuilder() {}

    public static ShapeResult build(
            GrowthBlockDefinition definition,
            double currentScale,
            double wobbleOffsetX,
            double wobbleOffsetY,
            double wobbleOffsetZ,
            BlockPos pos
    ) {
        // Render bounds (unclamped, used by renderer only)
        double renderScale = Math.max(0.05D, currentScale);
        double renderHalf = renderScale * 0.5D;
        double renderMinX = 0.5D - renderHalf + wobbleOffsetX;
        double renderMaxX = 0.5D + renderHalf + wobbleOffsetX;
        double renderMinZ = 0.5D - renderHalf + wobbleOffsetZ;
        double renderMaxZ = 0.5D + renderHalf + wobbleOffsetZ;
        double renderMinY = wobbleOffsetY;
        double renderMaxY = renderMinY + Math.max(0.2D, renderScale);
        Box renderBounds = new Box(renderMinX, renderMinY, renderMinZ, renderMaxX, renderMaxY, renderMaxZ);

        // Interaction bounds follow the growth profile limits
        double minScale = Math.max(0.05D, definition.minScale());
        double maxScale = Math.max(minScale, sanitizedMaxScale(definition));
        double clamped = MathHelper.clamp(currentScale, minScale, maxScale);
        double halfHorizontal = Math.max(0.05D, clamped * 0.5D);

        double actualWobbleX = wobbleOffsetX;
        double actualWobbleZ = wobbleOffsetZ;

        double minX = 0.5D - halfHorizontal + actualWobbleX;
        double maxX = 0.5D + halfHorizontal + actualWobbleX;
        double minZ = 0.5D - halfHorizontal + actualWobbleZ;
        double maxZ = 0.5D + halfHorizontal + actualWobbleZ;
        double minY;
        double maxY;
        if (clamped >= 1.0D) {
            // Large blocks become full cubes centered on the block
            double half = clamped * 0.5D;
            minX = 0.5D - half;
            maxX = 0.5D + half;
            minZ = 0.5D - half;
            maxZ = 0.5D + half;
            minY = 0.0D;
            maxY = Math.max(1.0D, clamped);
            // Note: wobble offsets are zeroed for large blocks in the caller
        } else {
            minY = wobbleOffsetY;
            maxY = wobbleOffsetY + Math.max(0.2D, clamped);
        }

        boolean needsPadding = clamped < 1.0D;
        double horizontalPadding = needsPadding ? INTERACTION_EPSILON : 0.0D;
        minX -= horizontalPadding;
        maxX += horizontalPadding;
        minZ -= horizontalPadding;
        maxZ += horizontalPadding;
        double lowerPadding = needsPadding ? INTERACTION_EPSILON : 0.0D;
        double upperPadding = needsPadding && maxY < 1.0D ? INTERACTION_EPSILON : 0.0D;
        minY -= lowerPadding;
        maxY += upperPadding;

        double thickness = 0.05D;
        if (maxX - minX < thickness) {
            double center = (maxX + minX) * 0.5D;
            double half = thickness * 0.5D;
            minX = center - half;
            maxX = center + half;
        }
        if (maxZ - minZ < thickness) {
            double center = (maxZ + minZ) * 0.5D;
            double half = thickness * 0.5D;
            minZ = center - half;
            maxZ = center + half;
        }
        if (maxY - minY < thickness) {
            double center = (maxY + minY) * 0.5D;
            double half = thickness * 0.5D;
            minY = center - half;
            maxY = center + half;
        }

        VoxelShape outlineShape = VoxelShapes.cuboid(minX, minY, minZ, maxX, maxY, maxZ);
        Box outlineBounds = outlineShape.isEmpty() ? null : outlineShape.getBoundingBox();
        
        VoxelShape collisionShape;
        List<Box> collisionPanels;
        if (definition.hasCollision()) {
            CollisionResult cr = buildVanillaShellCollisionShape(outlineBounds, outlineShape, pos);
            collisionShape = cr.shape();
            collisionPanels = cr.panels();
        } else {
            collisionShape = VoxelShapes.empty();
            collisionPanels = null;
        }

        return new ShapeResult(outlineShape, collisionShape, collisionPanels, renderBounds);
    }

    public record CollisionResult(VoxelShape shape, @Nullable List<Box> panels) {}

    public static CollisionResult buildVanillaShellCollisionShape(@Nullable Box outlineBounds, VoxelShape fallback, BlockPos pos) {
        if (outlineBounds == null || GrowthCollisionDebug.getShapeMode() != ShapeMode.SHELL) {
            List<Box> panels = outlineBounds == null ? null : Collections.singletonList(outlineBounds.offset(pos));
            return new CollisionResult(fallback, panels);
        }
        double minX = outlineBounds.minX;
        double maxX = outlineBounds.maxX;
        double minY = outlineBounds.minY;
        double maxY = outlineBounds.maxY;
        double minZ = outlineBounds.minZ;
        double maxZ = outlineBounds.maxZ;
        double height = Math.max(0.0D, maxY - minY);
        if (height < SHELL_MIN_HEIGHT) {
            return new CollisionResult(fallback, Collections.singletonList(outlineBounds.offset(pos)));
        }

        double capMinY = Math.max(minY, maxY - Math.min(SHELL_CAP_THICKNESS, height));
        if (capMinY >= maxY) {
            return new CollisionResult(fallback, Collections.singletonList(outlineBounds.offset(pos)));
        }

        List<Box> localPanels = new ArrayList<>();
        Box roof = new Box(minX, capMinY, minZ, maxX, maxY, maxZ);
        localPanels.add(roof);
        VoxelShape shell = VoxelShapes.cuboid(roof);

        double wallTopY = Math.min(capMinY, minY + SHELL_WALL_HEIGHT);
        if (wallTopY > minY + 1.0E-4D) {
            double westMaxX = minX + SHELL_WALL_THICKNESS;
            double eastMinX = maxX - SHELL_WALL_THICKNESS;
            double northMaxZ = minZ + SHELL_WALL_THICKNESS;
            double southMinZ = maxZ - SHELL_WALL_THICKNESS;

            if (westMaxX > minX) {
                Box panel = new Box(minX, minY, minZ, westMaxX, wallTopY, maxZ);
                localPanels.add(panel);
                shell = VoxelShapes.union(shell, VoxelShapes.cuboid(panel));
            }
            if (eastMinX < maxX) {
                Box panel = new Box(eastMinX, minY, minZ, maxX, wallTopY, maxZ);
                localPanels.add(panel);
                shell = VoxelShapes.union(shell, VoxelShapes.cuboid(panel));
            }
            if (northMaxZ > minZ) {
                Box panel = new Box(minX, minY, minZ, maxX, wallTopY, northMaxZ);
                localPanels.add(panel);
                shell = VoxelShapes.union(shell, VoxelShapes.cuboid(panel));
            }
            if (southMinZ < maxZ) {
                Box panel = new Box(minX, minY, southMinZ, maxX, wallTopY, maxZ);
                localPanels.add(panel);
                shell = VoxelShapes.union(shell, VoxelShapes.cuboid(panel));
            }
        }

        List<Box> worldPanels;
        if (!localPanels.isEmpty()) {
            worldPanels = new ArrayList<>(localPanels.size());
            for (Box local : localPanels) {
                worldPanels.add(local.offset(pos));
            }
        } else {
            worldPanels = Collections.singletonList(outlineBounds.offset(pos));
        }

        return new CollisionResult(shell.isEmpty() ? fallback : shell, worldPanels);
    }

    public static void debugCollisionShape(
            World world,
            BlockPos pos,
            GrowthBlockDefinition definition,
            double clampedScale,
            double lastLoggedScale,
            VoxelShape outlineShape,
            VoxelShape collisionShape,
            Box renderBounds,
            @Nullable List<Box> collisionPanels,
            long lastDebugTick
    ) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        if (Math.abs(clampedScale - lastLoggedScale) < 0.05D) {
            return;
        }
        Box outlineBox = outlineShape == null || outlineShape.isEmpty() ? null : outlineShape.getBoundingBox();
        Box collisionBox = collisionShape == null || collisionShape.isEmpty() ? null : collisionShape.getBoundingBox();
        Logging.GROWTH.info("[GrowthShape] world={} pos={} def={} scale={} outlineBox={} collisionBox={} renderBounds={} hasCollision={}",
                serverWorld.getRegistryKey().getValue(),
                pos,
                definition.id(),
                String.format("%.3f", clampedScale),
                outlineBox,
                collisionBox,
                renderBounds,
                definition.hasCollision());
        if (definition.hasCollision() && collisionShape != null && !collisionShape.isEmpty()) {
            long now = serverWorld.getTime();
            if (now - lastDebugTick >= 20L) {
                List<Box> debugBoxes = collisionPanels != null ? collisionPanels : new ArrayList<>();
                if (collisionPanels == null) {
                    for (Box local : collisionShape.getBoundingBoxes()) {
                        debugBoxes.add(local.offset(pos));
                    }
                }
                int idx = 0;
                for (Box worldBox : debugBoxes) {
                    Logging.GROWTH.info("[GrowthShapeBounds] world={} def={} idx={} world={}",
                            serverWorld.getRegistryKey().getValue(),
                            definition.id(),
                            idx++,
                            worldBox);
                    if (idx >= 6) {
                        break;
                    }
                }
            }
        }
    }

    private static final double MIN_SCALE_SPAN = 1.0E-4D;

    private static double sanitizedMaxScale(GrowthBlockDefinition definition) {
        double min = definition.minScale();
        double max = definition.maxScale();
        if (max - min < MIN_SCALE_SPAN) {
            return min + MIN_SCALE_SPAN;
        }
        return max;
    }
}
