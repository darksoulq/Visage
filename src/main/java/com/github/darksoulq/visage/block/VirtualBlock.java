package com.github.darksoulq.visage.block;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Predicate;

@ApiStatus.Experimental
public class VirtualBlock {
    private final Location location;
    private BlockData blockData;
    private Predicate<Player> visibilityFilter;
    private VirtualBlockGroup group;

    public VirtualBlock(Location location, BlockData blockData) {
        this.location = location.clone();
        this.blockData = blockData.clone();
    }

    public Location getLocation() {
        return location.clone();
    }

    public BlockData getBlockData() {
        return blockData.clone();
    }

    public void setBlockData(BlockData blockData) {
        this.blockData = blockData.clone();
        VirtualBlockTracker.updateBlock(this);
    }

    public Predicate<Player> getVisibilityFilter() {
        return visibilityFilter;
    }

    public void setVisibilityFilter(Predicate<Player> visibilityFilter) {
        this.visibilityFilter = visibilityFilter;
        VirtualBlockTracker.updateBlock(this);
    }

    public VirtualBlockGroup getGroup() {
        return group;
    }

    public void setGroup(VirtualBlockGroup group) {
        this.group = group;
        VirtualBlockTracker.updateBlock(this);
    }

    public boolean isVisibleTo(Player player) {
        if (group != null && group.getVisibilityFilter() != null && !group.getVisibilityFilter().test(player)) {
            return false;
        }
        return visibilityFilter == null || visibilityFilter.test(player);
    }
}