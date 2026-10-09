package com.github.darksoulq.visage.block;

import com.github.darksoulq.visage.Visage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Experimental
public class VirtualBlockTracker {
    private static final Map<World, Map<Long, Map<Location, VirtualBlock>>> blocks = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<Location>> playerVisibleBlocks = new ConcurrentHashMap<>();
    private static int taskId = -1;

    public static void start(Plugin plugin) {
        if (taskId != -1) return;
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, VirtualBlockTracker::tickTracking, 0, 4).getTaskId();
    }

    public static void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        blocks.clear();
        playerVisibleBlocks.clear();
    }

    public static void clearPlayer(Player player) {
        playerVisibleBlocks.remove(player.getUniqueId());
    }

    private static void tickTracking() {
        Map<Player, Map<Location, BlockData>> changesToApply = new HashMap<>();
        Map<Player, Set<Location>> removalsToApply = new HashMap<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            World world = p.getWorld();
            Map<Long, Map<Location, VirtualBlock>> worldBlocks = blocks.get(world);
            if (worldBlocks == null || worldBlocks.isEmpty()) continue;

            UUID uuid = p.getUniqueId();
            Set<Location> visibleBlocks = playerVisibleBlocks.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());

            Map<Location, BlockData> show = new HashMap<>();
            Set<Location> hide = new HashSet<>();

            for (Map<Location, VirtualBlock> chunkBlocks : worldBlocks.values()) {
                for (VirtualBlock block : chunkBlocks.values()) {
                    Location loc = block.getLocation();
                    boolean shouldSee = block.isVisibleTo(p);
                    boolean currentlySees = visibleBlocks.contains(loc);

                    if (shouldSee && !currentlySees) {
                        visibleBlocks.add(loc);
                        show.put(loc, block.getBlockData());
                    } else if (!shouldSee && currentlySees) {
                        visibleBlocks.remove(loc);
                        hide.add(loc);
                    }
                }
            }

            if (!show.isEmpty()) changesToApply.put(p, show);
            if (!hide.isEmpty()) removalsToApply.put(p, hide);
        }

        applyBatch(changesToApply, removalsToApply);
    }

    public static long getChunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static void setBlock(VirtualBlock block) {
        Location loc = block.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        long key = getChunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        blocks.computeIfAbsent(world, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(key, k -> new ConcurrentHashMap<>())
            .put(loc, block);

        updateBlock(block);
    }

    public static void setBlocks(List<VirtualBlock> blocksBatch) {
        for (VirtualBlock block : blocksBatch) {
            Location loc = block.getLocation();
            World world = loc.getWorld();
            if (world == null) continue;

            long key = getChunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
            blocks.computeIfAbsent(world, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .put(loc, block);
        }
        updateBlocks(blocksBatch);
    }

    public static void removeBlock(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        long key = getChunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        Map<Long, Map<Location, VirtualBlock>> worldBlocks = blocks.get(world);
        if (worldBlocks != null) {
            Map<Location, VirtualBlock> chunkBlocks = worldBlocks.get(key);
            if (chunkBlocks != null) {
                VirtualBlock removed = chunkBlocks.remove(loc);
                if (removed != null) {
                    Bukkit.getScheduler().runTask(Visage.getPlugin(), () -> {
                        BlockData realData = loc.getBlock().getBlockData();
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getWorld().equals(world)) {
                                Set<Location> visible = playerVisibleBlocks.get(p.getUniqueId());
                                if (visible != null && visible.remove(loc)) {
                                    p.sendBlockChange(loc, realData);
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    public static VirtualBlock getBlock(Location loc) {
        World world = loc.getWorld();
        if (world == null) return null;

        long key = getChunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        Map<Long, Map<Location, VirtualBlock>> worldBlocks = blocks.get(world);
        if (worldBlocks != null) {
            Map<Location, VirtualBlock> chunkBlocks = worldBlocks.get(key);
            if (chunkBlocks != null) {
                return chunkBlocks.get(loc);
            }
        }
        return null;
    }

    public static void updateBlock(VirtualBlock block) {
        updateBlocks(Collections.singletonList(block));
    }

    public static void updateBlocks(Collection<VirtualBlock> blocksBatch) {
        Map<Player, Map<Location, BlockData>> changes = new HashMap<>();
        Map<Player, Set<Location>> removals = new HashMap<>();

        for (VirtualBlock block : blocksBatch) {
            Location loc = block.getLocation();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().equals(loc.getWorld())) {
                    Set<Location> visible = playerVisibleBlocks.computeIfAbsent(p.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
                    boolean shouldSee = block.isVisibleTo(p);
                    boolean currentlySees = visible.contains(loc);

                    if (shouldSee && !currentlySees) {
                        visible.add(loc);
                        changes.computeIfAbsent(p, k -> new HashMap<>()).put(loc, block.getBlockData());
                    } else if (!shouldSee && currentlySees) {
                        visible.remove(loc);
                        removals.computeIfAbsent(p, k -> new HashSet<>()).add(loc);
                    } else if (shouldSee && currentlySees) {
                        changes.computeIfAbsent(p, k -> new HashMap<>()).put(loc, block.getBlockData());
                    }
                }
            }
        }

        applyBatch(changes, removals);
    }

    private static void applyBatch(Map<Player, Map<Location, BlockData>> changes, Map<Player, Set<Location>> removals) {
        if (!changes.isEmpty() || !removals.isEmpty()) {
            Bukkit.getScheduler().runTask(Visage.getPlugin(), () -> {
                for (Map.Entry<Player, Map<Location, BlockData>> entry : changes.entrySet()) {
                    if (entry.getKey().isOnline()) {
                        entry.getKey().sendMultiBlockChange(entry.getValue());
                    }
                }
                for (Map.Entry<Player, Set<Location>> entry : removals.entrySet()) {
                    Player p = entry.getKey();
                    if (p.isOnline()) {
                        Map<Location, BlockData> realBlocks = new HashMap<>();
                        for (Location loc : entry.getValue()) {
                            if (loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                                realBlocks.put(loc, loc.getBlock().getBlockData());
                            }
                        }
                        if (!realBlocks.isEmpty()) {
                            p.sendMultiBlockChange(realBlocks);
                        }
                    }
                }
            });
        }
    }

    public static void onChunkSent(Player player, int chunkX, int chunkZ, World world) {
        Map<Long, Map<Location, VirtualBlock>> worldBlocks = blocks.get(world);
        if (worldBlocks == null) return;

        long key = getChunkKey(chunkX, chunkZ);
        Map<Location, VirtualBlock> chunkBlocks = worldBlocks.get(key);

        if (chunkBlocks != null && !chunkBlocks.isEmpty()) {
            Map<Location, BlockData> changes = new HashMap<>();
            Set<Location> visible = playerVisibleBlocks.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());

            for (VirtualBlock block : chunkBlocks.values()) {
                if (block.isVisibleTo(player)) {
                    Location loc = block.getLocation();
                    changes.put(loc, block.getBlockData());
                    visible.add(loc);
                }
            }
            if (!changes.isEmpty()) {
                Bukkit.getScheduler().runTask(Visage.getPlugin(), () -> {
                    if (player.isOnline()) {
                        player.sendMultiBlockChange(changes);
                    }
                });
            }
        }
    }

    public static void unloadWorld(World world) {
        blocks.remove(world);
    }
}