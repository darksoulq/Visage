package com.github.darksoulq.visage;

public class VisageConfig {
    public static double GLOBAL_MAX_RENDER_DISTANCE = 45.0;
    public static double FRUSTUM_THRESHOLD = 90.0;
    public static int TICK_RATE_TRACKING = 4;
    public static int OCTREE_MAX_DEPTH = 2048;
    public static int OCTREE_MAX_ENTRIES = 128;
    public static double OCTREE_MIN_XYZ = -30000000.0;
    public static double OCTREE_MAX_XYZ = 30000000.0;
    public static double OCTREE_MIN_Y = -64.0;
    public static double OCTREE_MAX_Y = 320.0;
    public static double OCCLUSION_MAX_DISTANCE = 24.0;

    public static double DISTANCE_HYSTERESIS = 3.0;
    public static double FRUSTUM_HYSTERESIS_BUFFER = 25.0;
    public static int MAX_SPAWNS_PER_TICK = 45;
}