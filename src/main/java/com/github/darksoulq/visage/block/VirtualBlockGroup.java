package com.github.darksoulq.visage.block;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@ApiStatus.Experimental
public class VirtualBlockGroup {
    private final Map<Location, VirtualBlock> blocks = new ConcurrentHashMap<>();
    private Predicate<Player> visibilityFilter = null;

    public void addBlock(VirtualBlock block) {
        block.setGroup(this);
        blocks.put(block.getLocation(), block);
        VirtualBlockTracker.setBlock(block);
    }

    public void addBlocks(Collection<VirtualBlock> newBlocks) {
        for (VirtualBlock block : newBlocks) {
            block.setGroup(this);
            blocks.put(block.getLocation(), block);
        }
        VirtualBlockTracker.setBlocks(new ArrayList<>(newBlocks));
    }

    public void removeBlock(Location loc) {
        VirtualBlock block = blocks.remove(loc);
        if (block != null) {
            block.setGroup(null);
            VirtualBlockTracker.removeBlock(loc);
        }
    }

    public void removeBlock(VirtualBlock block) {
        removeBlock(block.getLocation());
    }

    public void setVisibilityFilter(Predicate<Player> visibilityFilter) {
        this.visibilityFilter = visibilityFilter;
        VirtualBlockTracker.updateBlocks(blocks.values());
    }

    public Predicate<Player> getVisibilityFilter() {
        return visibilityFilter;
    }

    public Collection<VirtualBlock> getBlocks() {
        return Collections.unmodifiableCollection(blocks.values());
    }

    public void destroyAll() {
        for (VirtualBlock block : blocks.values()) {
            block.setGroup(null);
            VirtualBlockTracker.removeBlock(block.getLocation());
        }
        blocks.clear();
    }
}