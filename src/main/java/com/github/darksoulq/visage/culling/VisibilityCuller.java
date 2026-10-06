package com.github.darksoulq.visage.culling;

import com.github.darksoulq.visage.VisageConfig;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

public class VisibilityCuller {
    public static VisibilityState getVisibility(World eyeWorld, double eyeX, double eyeY, double eyeZ, double dirX, double dirY, double dirZ, Cullable cullable) {
        if (!eyeWorld.equals(cullable.getWorld())) return VisibilityState.HIDDEN;

        BoundingBox box = cullable.getBoundingBox();
        double cX = box.getCenterX();
        double cY = box.getCenterY();
        double cZ = box.getCenterZ();

        double dX = cX - eyeX;
        double dY = cY - eyeY;
        double dZ = cZ - eyeZ;
        double distSq = dX * dX + dY * dY + dZ * dZ;

        if (distSq > VisageConfig.CULLING_DISTANCE_SQUARED) return VisibilityState.CULLED_DISTANCE;

        double dist = Math.sqrt(distSq);
        if (dist > 0) {
            double normX = dX / dist;
            double normY = dY / dist;
            double normZ = dZ / dist;

            double dot = (dirX * normX) + (dirY * normY) + (dirZ * normZ);

            double sX = box.getMaxX() - box.getMinX();
            double sY = box.getMaxY() - box.getMinY();
            double sZ = box.getMaxZ() - box.getMinZ();
            double boxRadius = Math.sqrt(sX * sX + sY * sY + sZ * sZ) * 0.5;

            double threshold = VisageConfig.FRUSTUM_THRESHOLD * (boxRadius / dist);

            if (dot < threshold && dist > boxRadius) {
                return VisibilityState.CULLED_FRUSTUM;
            }
        }

        return VisibilityState.VISIBLE;
    }
}