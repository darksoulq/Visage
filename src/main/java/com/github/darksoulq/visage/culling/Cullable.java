package com.github.darksoulq.visage.culling;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.UUID;
import java.util.function.Predicate;

public interface Cullable {
    UUID getUniqueId();
    Location getLocation();
    World getWorld();
    BoundingBox getBoundingBox();
    void spawnFor(Player player);
    void destroyFor(Player player);
    void destroyAll();
    Predicate<Player> getVisibilityFilter();
    void setVisibilityFilter(Predicate<Player> filter);
}