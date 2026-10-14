package com.github.darksoulq.visage.culling;

import com.github.darksoulq.visage.VisageConfig;
import com.github.darksoulq.visage.util.Octree;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

public class CullingTracker {
    private static final Map<UUID, Cullable> cullables = new ConcurrentHashMap<>();
    private static final Map<World, Octree<Cullable>> worldOctrees = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerTrackingState> playerTracking = new ConcurrentHashMap<>();

    private static final ReentrantReadWriteLock STATE_LOCK = new ReentrantReadWriteLock();

    private static int asyncTaskId = -1;
    private static int syncTaskId = -1;
    private static long currentTick = 0;

    private static class PlayerTrackingState {
        final Set<UUID> currentlySpawned = ConcurrentHashMap.newKeySet();
        final Set<UUID> asyncTargetVisible = ConcurrentHashMap.newKeySet();
        final Map<UUID, Long> occlusionCooldowns = new ConcurrentHashMap<>();
    }

    public static void start(Plugin plugin) {
        if (asyncTaskId != -1) return;

        asyncTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, CullingTracker::sweepAsync, 0, VisageConfig.TICK_RATE_TRACKING).getTaskId();
        syncTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            currentTick++;
            processSyncReconciliation();
        }, 0, 1).getTaskId();
    }

    public static void stop() {
        if (asyncTaskId != -1) Bukkit.getScheduler().cancelTask(asyncTaskId);
        if (syncTaskId != -1) Bukkit.getScheduler().cancelTask(syncTaskId);
        asyncTaskId = -1;
        syncTaskId = -1;

        STATE_LOCK.writeLock().lock();
        try {
            for (UUID id : List.copyOf(cullables.keySet())) {
                unregister(id);
            }
            playerTracking.clear();
            worldOctrees.clear();
        } finally {
            STATE_LOCK.writeLock().unlock();
        }
    }

    public static void unloadWorld(World world) {
        STATE_LOCK.writeLock().lock();
        try {
            worldOctrees.remove(world);
        } finally {
            STATE_LOCK.writeLock().unlock();
        }
    }

    public static void clearPlayer(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        PlayerTrackingState state = playerTracking.remove(uuid);
        if (state != null) {
            for (UUID cId : state.currentlySpawned) {
                Cullable cullable = cullables.get(cId);
                if (cullable != null) cullable.destroyFor(player);
            }
        }
    }

    public static void register(Cullable cullable) {
        STATE_LOCK.writeLock().lock();
        try {
            cullables.put(cullable.getUniqueId(), cullable);
            World world = cullable.getWorld();

            Octree<Cullable> octree = worldOctrees.computeIfAbsent(world, w ->
                new Octree<>(BoundingBox.of(new Vector(VisageConfig.OCTREE_MIN_XYZ, VisageConfig.OCTREE_MIN_Y, VisageConfig.OCTREE_MIN_XYZ), new Vector(VisageConfig.OCTREE_MAX_XYZ, VisageConfig.OCTREE_MAX_Y, VisageConfig.OCTREE_MAX_XYZ)), 0, Cullable::getBoundingBox)
            );
            octree.insert(cullable);
        } finally {
            STATE_LOCK.writeLock().unlock();
        }
    }

    public static void unregister(UUID uniqueId) {
        STATE_LOCK.writeLock().lock();
        try {
            Cullable cullable = cullables.remove(uniqueId);
            if (cullable != null) {
                cullable.destroyAll();
                World world = cullable.getWorld();

                Octree<Cullable> octree = worldOctrees.get(world);
                if (octree != null) {
                    octree.remove(cullable);
                }

                for (PlayerTrackingState pState : playerTracking.values()) {
                    pState.currentlySpawned.remove(uniqueId);
                    pState.asyncTargetVisible.remove(uniqueId);
                    pState.occlusionCooldowns.remove(uniqueId);
                }
            }
        } finally {
            STATE_LOCK.writeLock().unlock();
        }
    }

    public static void reindex(Cullable cullable, Runnable updateAction) {
        STATE_LOCK.writeLock().lock();
        try {
            if (cullables.containsKey(cullable.getUniqueId())) {
                World oldWorld = cullable.getWorld();

                Octree<Cullable> oldOctree = worldOctrees.get(oldWorld);
                if (oldOctree != null) {
                    oldOctree.remove(cullable);
                }

                updateAction.run();

                World newWorld = cullable.getWorld();
                Octree<Cullable> newOctree = worldOctrees.computeIfAbsent(newWorld, w ->
                    new Octree<>(BoundingBox.of(new Vector(VisageConfig.OCTREE_MIN_XYZ, VisageConfig.OCTREE_MIN_Y, VisageConfig.OCTREE_MIN_XYZ), new Vector(VisageConfig.OCTREE_MAX_XYZ, VisageConfig.OCTREE_MAX_Y, VisageConfig.OCTREE_MAX_XYZ)), 0, Cullable::getBoundingBox)
                );
                newOctree.insert(cullable);
            } else {
                updateAction.run();
            }
        } finally {
            STATE_LOCK.writeLock().unlock();
        }
    }

    private static void sweepAsync() {
        double searchPadding = Math.max(VisageConfig.DISTANCE_HYSTERESIS, 5.0);
        double maxGlobalDist = VisageConfig.GLOBAL_MAX_RENDER_DISTANCE + searchPadding;

        STATE_LOCK.readLock().lock();
        try {
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID pUuid = p.getUniqueId();
                PlayerTrackingState state = playerTracking.computeIfAbsent(pUuid, k -> new PlayerTrackingState());

                Location eyeLoc = p.getEyeLocation();
                World world = eyeLoc.getWorld();
                double eyeX = eyeLoc.getX(), eyeY = eyeLoc.getY(), eyeZ = eyeLoc.getZ();
                Vector dir = eyeLoc.getDirection();

                Octree<Cullable> octree = worldOctrees.get(world);
                Set<UUID> visibleThisSweep = new HashSet<>();

                if (octree != null) {
                    BoundingBox queryBox = new BoundingBox(
                        eyeX - maxGlobalDist, eyeY - maxGlobalDist, eyeZ - maxGlobalDist,
                        eyeX + maxGlobalDist, eyeY + maxGlobalDist, eyeZ + maxGlobalDist
                    );

                    for (Cullable cullable : octree.query(queryBox)) {
                        UUID cId = cullable.getUniqueId();
                        boolean wasSpawned = state.currentlySpawned.contains(cId);

                        Predicate<Player> filter = cullable.getVisibilityFilter();
                        if (filter != null && !filter.test(p)) continue;

                        VisibilityState fastState = VisibilityCuller.getFastVisibility(world, eyeX, eyeY, eyeZ, dir.getX(), dir.getY(), dir.getZ(), cullable, wasSpawned);

                        if (fastState == VisibilityState.VISIBLE) {
                            visibleThisSweep.add(cId);
                        }
                    }
                }

                state.asyncTargetVisible.retainAll(visibleThisSweep);
                state.asyncTargetVisible.addAll(visibleThisSweep);
            }
        } finally {
            STATE_LOCK.readLock().unlock();
        }
    }

    private static void processSyncReconciliation() {
        playerTracking.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);

        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerTrackingState state = playerTracking.get(p.getUniqueId());
            if (state == null) continue;

            Location eyeLoc = p.getEyeLocation();
            double eyeX = eyeLoc.getX(), eyeY = eyeLoc.getY(), eyeZ = eyeLoc.getZ();
            World world = eyeLoc.getWorld();

            Iterator<UUID> spawnedIt = state.currentlySpawned.iterator();
            while (spawnedIt.hasNext()) {
                UUID cId = spawnedIt.next();
                if (!state.asyncTargetVisible.contains(cId)) {
                    Cullable c = cullables.get(cId);
                    if (c != null) {
                        c.destroyFor(p);
                    }
                    spawnedIt.remove();
                }
            }

            int spawnsThisTick = 0;
            for (UUID cId : state.asyncTargetVisible) {
                if (state.currentlySpawned.contains(cId)) continue;
                if (spawnsThisTick >= VisageConfig.MAX_SPAWNS_PER_TICK) break;

                Cullable spawnCandidate = cullables.get(cId);
                if (spawnCandidate == null || !spawnCandidate.getWorld().equals(world)) continue;

                long lastCheck = state.occlusionCooldowns.getOrDefault(cId, 0L);
                if (currentTick - lastCheck < VisageConfig.TICK_RATE_TRACKING) {
                    continue;
                }

                boolean occluded = VisibilityCuller.doOcclusionCheck(world, eyeX, eyeY, eyeZ, spawnCandidate);
                state.occlusionCooldowns.put(cId, currentTick);

                if (!occluded) {
                    state.currentlySpawned.add(cId);
                    spawnCandidate.spawnFor(p);
                    spawnsThisTick++;
                }
            }
        }
    }
}