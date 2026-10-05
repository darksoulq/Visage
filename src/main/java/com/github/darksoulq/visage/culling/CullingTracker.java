package com.github.darksoulq.visage.culling;

import com.github.darksoulq.visage.VisageConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class CullingTracker {
    private static final Map<UUID, Cullable> cullables = new ConcurrentHashMap<>();
    private static final Map<World, Map<Long, List<Cullable>>> spatialGrid = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerTrackingState> playerTracking = new ConcurrentHashMap<>();

    private static int taskId = -1;
    private static int ticks = 0;

    private static class PlayerTrackingState {
        final Map<UUID, VisibilityState> states = new HashMap<>();
        final Map<UUID, Integer> lastSeen = new HashMap<>();
    }

    public static void start(Plugin plugin) {
        if (taskId != -1) return;
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            ticks++;
            if (ticks % VisageConfig.TICK_RATE_TRACKING == 0) tickTracking();
        }, 0, 1).getTaskId();
    }

    public static void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
            for (UUID id : List.copyOf(cullables.keySet())) {
                unregister(id);
            }
            playerTracking.clear();
            spatialGrid.clear();
        }
    }

    public static void unloadWorld(World world) {
        spatialGrid.remove(world);
    }

    private static long getChunkKey(double x, double z) {
        int cx = ((int) Math.floor(x)) >> 4;
        int cz = ((int) Math.floor(z)) >> 4;
        return ((long) cx & 0xFFFFFFFFL) | (((long) cz & 0xFFFFFFFFL) << 32);
    }

    public static void register(Cullable cullable) {
        cullables.put(cullable.getUniqueId(), cullable);
        World world = cullable.getWorld();
        long key = getChunkKey(cullable.getX(), cullable.getZ());

        Map<Long, List<Cullable>> grid = spatialGrid.computeIfAbsent(world, w -> new ConcurrentHashMap<>());
        List<Cullable> bucket = grid.computeIfAbsent(key, k -> new ArrayList<>());

        synchronized (bucket) {
            bucket.add(cullable);
        }
    }

    public static void unregister(UUID uniqueId) {
        Cullable cullable = cullables.remove(uniqueId);
        if (cullable != null) {
            cullable.destroyAll();
            World world = cullable.getWorld();
            long key = getChunkKey(cullable.getX(), cullable.getZ());

            Map<Long, List<Cullable>> grid = spatialGrid.get(world);
            if (grid != null) {
                List<Cullable> bucket = grid.get(key);
                if (bucket != null) {
                    synchronized (bucket) {
                        bucket.remove(cullable);
                    }
                }
            }

            for (PlayerTrackingState pState : playerTracking.values()) {
                pState.states.remove(uniqueId);
                pState.lastSeen.remove(uniqueId);
            }
        }
    }

    public static void reindex(Cullable cullable, Runnable updateAction) {
        if (cullables.containsKey(cullable.getUniqueId())) {
            World oldWorld = cullable.getWorld();
            long oldKey = getChunkKey(cullable.getX(), cullable.getZ());

            updateAction.run();

            World newWorld = cullable.getWorld();
            long newKey = getChunkKey(cullable.getX(), cullable.getZ());

            if (!oldWorld.equals(newWorld) || oldKey != newKey) {
                Map<Long, List<Cullable>> oldGrid = spatialGrid.get(oldWorld);
                if (oldGrid != null) {
                    List<Cullable> oldBucket = oldGrid.get(oldKey);
                    if (oldBucket != null) {
                        synchronized (oldBucket) {
                            oldBucket.remove(cullable);
                        }
                    }
                }

                Map<Long, List<Cullable>> newGrid = spatialGrid.computeIfAbsent(newWorld, w -> new ConcurrentHashMap<>());
                List<Cullable> newBucket = newGrid.computeIfAbsent(newKey, k -> new ArrayList<>());
                synchronized (newBucket) {
                    newBucket.add(cullable);
                }
            }
        } else {
            updateAction.run();
        }
    }

    private static void tickTracking() {
        int currentTick = ticks;
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID uuid = p.getUniqueId();
            PlayerTrackingState pState = playerTracking.computeIfAbsent(uuid, k -> new PlayerTrackingState());

            Location eyeLoc = p.getEyeLocation();
            World world = eyeLoc.getWorld();
            double eyeX = eyeLoc.getX();
            double eyeY = eyeLoc.getY();
            double eyeZ = eyeLoc.getZ();

            Vector dir = eyeLoc.getDirection();
            double dirX = dir.getX();
            double dirY = dir.getY();
            double dirZ = dir.getZ();

            Map<Long, List<Cullable>> grid = spatialGrid.get(world);
            if (grid != null) {
                int cx = ((int) Math.floor(eyeX)) >> 4;
                int cz = ((int) Math.floor(eyeZ)) >> 4;
                int radius = (int) Math.ceil(VisageConfig.TRACKING_QUERY_BOX_SIZE / 16.0);

                for (int x = cx - radius; x <= cx + radius; x++) {
                    for (int z = cz - radius; z <= cz + radius; z++) {
                        long key = ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
                        List<Cullable> bucket = grid.get(key);

                        if (bucket != null) {
                            synchronized (bucket) {
                                for (int i = 0; i < bucket.size(); i++) {
                                    Cullable cullable = bucket.get(i);
                                    Predicate<Player> filter = cullable.getVisibilityFilter();
                                    VisibilityState state;

                                    if (filter != null && !filter.test(p)) {
                                        state = VisibilityState.HIDDEN;
                                    } else {
                                        state = VisibilityCuller.getVisibility(eyeX, eyeY, eyeZ, dirX, dirY, dirZ, cullable);
                                    }

                                    pState.lastSeen.put(cullable.getUniqueId(), currentTick);
                                    VisibilityState oldState = pState.states.getOrDefault(cullable.getUniqueId(), VisibilityState.HIDDEN);

                                    if (state == VisibilityState.VISIBLE && oldState != VisibilityState.VISIBLE) {
                                        cullable.spawnFor(p);
                                    } else if (state != VisibilityState.VISIBLE && oldState == VisibilityState.VISIBLE) {
                                        cullable.destroyFor(p);
                                    }

                                    pState.states.put(cullable.getUniqueId(), state);
                                }
                            }
                        }
                    }
                }
            }

            Iterator<Map.Entry<UUID, VisibilityState>> it = pState.states.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, VisibilityState> entry = it.next();
                UUID cullableId = entry.getKey();
                if (pState.lastSeen.getOrDefault(cullableId, -1) != currentTick) {
                    if (entry.getValue() == VisibilityState.VISIBLE) {
                        Cullable cullable = cullables.get(cullableId);
                        if (cullable != null) {
                            cullable.destroyFor(p);
                        }
                    }
                    pState.lastSeen.remove(cullableId);
                    it.remove();
                }
            }
        }

        playerTracking.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }
}