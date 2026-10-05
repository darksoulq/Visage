package com.github.darksoulq.visage.culling;

import com.github.darksoulq.visage.VisageConfig;

public class VisibilityCuller {
    public static VisibilityState getVisibility(double eyeX, double eyeY, double eyeZ, double dirX, double dirY, double dirZ, Cullable cullable) {
        double cX = cullable.getCenterX();
        double cY = cullable.getCenterY();
        double cZ = cullable.getCenterZ();
        double radius = cullable.getCullingRadius();

        double dX = cX - eyeX;
        double dY = cY - eyeY;
        double dZ = cZ - eyeZ;
        double distSq = dX * dX + dY * dY + dZ * dZ;

        if (distSq > VisageConfig.CULLING_DISTANCE_SQUARED) {
            return VisibilityState.CULLED_DISTANCE;
        }

        double dist = Math.sqrt(distSq);
        if (dist > 0) {
            dX /= dist;
            dY /= dist;
            dZ /= dist;

            double dot = (dirX * dX) + (dirY * dY) + (dirZ * dZ);
            double threshold = VisageConfig.FRUSTUM_THRESHOLD - (radius / dist);

            if (dot < threshold && dist > radius) {
                return VisibilityState.CULLED_FRUSTUM;
            }
        }

        return VisibilityState.VISIBLE;
    }
}