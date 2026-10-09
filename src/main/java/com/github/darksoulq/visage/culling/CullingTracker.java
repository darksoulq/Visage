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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class CullingTracker {
    private static final Map<UUID, Cullable> cullables = new ConcurrentHashMap<>();
    private static final Map<World, Octree<Cullable>> worldOctrees = new ConcurrentHashMap<>();
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
            worldOctrees.clear();
        }
    }

    public static void unloadWorld(World world) {
        worldOctrees.remove(world);
    }

    public static void clearPlayer(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        playerTracking.remove(uuid);
        for (Cullable cullable : cullables.values()) {
            cullable.destroyFor(player);
        }
    }

    public static void register(Cullable cullable) {
        cullables.put(cullable.getUniqueId(), cullable);
        World world = cullable.getWorld();

        Octree<Cullable> octree = worldOctrees.computeIfAbsent(world, w ->
            new Octree<>(BoundingBox.of(new Vector(VisageConfig.OCTREE_MIN_XYZ, VisageConfig.OCTREE_MIN_Y, VisageConfig.OCTREE_MIN_XYZ), new Vector(VisageConfig.OCTREE_MAX_XYZ, VisageConfig.OCTREE_MAX_Y, VisageConfig.OCTREE_MAX_XYZ)), 0, Cullable::getBoundingBox)
        );
        octree.insert(cullable);
    }

    public static void unregister(UUID uniqueId) {
        Cullable cullable = cullables.remove(uniqueId);
        if (cullable != null) {
            cullable.destroyAll();
            World world = cullable.getWorld();

            Octree<Cullable> octree = worldOctrees.get(world);
            if (octree != null) {
                octree.remove(cullable);
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
    }

    private static void tickTracking() {
        int currentTick = ticks;
        double qs = VisageConfig.TRACKING_QUERY_BOX_SIZE;

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

            Octree<Cullable> octree = worldOctrees.get(world);

            if (octree != null) {
                BoundingBox queryBox = new BoundingBox(eyeX - qs, eyeY - qs, eyeZ - qs, eyeX + qs, eyeY + qs, eyeZ + qs);

                for (Cullable cullable : octree.query(queryBox)) {
                    Predicate<Player> filter = cullable.getVisibilityFilter();
                    VisibilityState state;

                    if (filter != null && !filter.test(p)) {
                        state = VisibilityState.HIDDEN;
                    } else {
                        state = VisibilityCuller.getVisibility(world, eyeX, eyeY, eyeZ, dirX, dirY, dirZ, cullable);
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