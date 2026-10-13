package com.github.darksoulq.visage.culling;

public enum VisibilityState {
    VISIBLE,
    CULLED_DISTANCE,
    CULLED_FRUSTUM,
    CULLED_OCCLUSION,
    HIDDEN
}