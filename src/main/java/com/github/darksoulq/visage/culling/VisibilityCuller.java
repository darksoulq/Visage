package com.github.darksoulq.visage.culling;

import com.github.darksoulq.visage.VisageConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.util.BoundingBox;

public class VisibilityCuller {

    public static VisibilityState getFastVisibility(World eyeWorld, double eyeX, double eyeY, double eyeZ, double dirX, double dirY, double dirZ, Cullable cullable, boolean wasVisible) {
        if (!eyeWorld.equals(cullable.getWorld())) return VisibilityState.HIDDEN;

        BoundingBox box = cullable.getBoundingBox();
        double cX = box.getCenterX();
        double cY = box.getCenterY();
        double cZ = box.getCenterZ();

        double dX = cX - eyeX;
        double dY = cY - eyeY;
        double dZ = cZ - eyeZ;
        double distSq = dX * dX + dY * dY + dZ * dZ;

        double maxDist = Math.min(cullable.getRenderDistance(), VisageConfig.GLOBAL_MAX_RENDER_DISTANCE);
        if (wasVisible) maxDist += VisageConfig.DISTANCE_HYSTERESIS;

        if (distSq > maxDist * maxDist) return VisibilityState.CULLED_DISTANCE;

        double dist = Math.sqrt(distSq);
        if (cullable.isFrustumCullingEnabled() && dist > 0) {
            double normX = dX / dist;
            double normY = dY / dist;
            double normZ = dZ / dist;

            double dot = (dirX * normX) + (dirY * normY) + (dirZ * normZ);

            double sX = box.getMaxX() - box.getMinX();
            double sY = box.getMaxY() - box.getMinY();
            double sZ = box.getMaxZ() - box.getMinZ();
            double boxRadius = Math.sqrt(sX * sX + sY * sY + sZ * sZ) * 0.5;

            double thresholdAngle = VisageConfig.FRUSTUM_THRESHOLD / 2.0;
            if (wasVisible) thresholdAngle += (VisageConfig.FRUSTUM_HYSTERESIS_BUFFER / 2.0);

            double baseThreshold = Math.cos(Math.toRadians(thresholdAngle));
            double threshold = baseThreshold * (1.0 - Math.min(1.0, (boxRadius / dist)));

            if (dot < threshold && dist > boxRadius) {
                return VisibilityState.CULLED_FRUSTUM;
            }
        }

        return VisibilityState.VISIBLE;
    }

    public static boolean doOcclusionCheck(World eyeWorld, double eyeX, double eyeY, double eyeZ, Cullable cullable) {
        BoundingBox box = cullable.getBoundingBox();
        double cX = box.getCenterX();
        double cY = box.getCenterY();
        double cZ = box.getCenterZ();

        double dX = cX - eyeX;
        double dY = cY - eyeY;
        double dZ = cZ - eyeZ;
        double distSq = dX * dX + dY * dY + dZ * dZ;

        if (!cullable.isOcclusionCullingEnabled() || distSq > VisageConfig.OCCLUSION_MAX_DISTANCE * VisageConfig.OCCLUSION_MAX_DISTANCE) {
            return false;
        }

        ServerLevel serverLevel = ((CraftWorld) eyeWorld).getHandle();
        Vec3 eye = new Vec3(eyeX, eyeY, eyeZ);
        Vec3 center = new Vec3(cX, cY, cZ);

        if (isPointVisible(serverLevel, eye, center)) {
            return false;
        }

        Vec3[] corners = new Vec3[]{
            new Vec3(box.getMinX(), box.getMinY(), box.getMinZ()),
            new Vec3(box.getMinX(), box.getMinY(), box.getMaxZ()),
            new Vec3(box.getMinX(), box.getMaxY(), box.getMinZ()),
            new Vec3(box.getMinX(), box.getMaxY(), box.getMaxZ()),
            new Vec3(box.getMaxX(), box.getMinY(), box.getMinZ()),
            new Vec3(box.getMaxX(), box.getMinY(), box.getMaxZ()),
            new Vec3(box.getMaxX(), box.getMaxY(), box.getMinZ()),
            new Vec3(box.getMaxX(), box.getMaxY(), box.getMaxZ())
        };

        for (Vec3 corner : corners) {
            if (isPointVisible(serverLevel, eye, corner)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isPointVisible(ServerLevel level, Vec3 eye, Vec3 target) {
        Vec3 dir = eye.subtract(target);
        if (dir.lengthSqr() > 0.01) {
            target = target.add(dir.normalize().scale(0.1));
        }

        ClipContext context = new ClipContext(
            eye,
            target,
            ClipContext.Block.VISUAL,
            ClipContext.Fluid.NONE,
            CollisionContext.empty()
        );
        BlockHitResult hit = level.clip(context);

        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getLocation().distanceToSqr(eye) >= target.distanceToSqr(eye) - 0.05;
        }

        return true;
    }
}