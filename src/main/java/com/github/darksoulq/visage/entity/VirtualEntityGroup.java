package com.github.darksoulq.visage.entity;

import com.github.darksoulq.visage.culling.Cullable;
import com.github.darksoulq.visage.culling.CullingTracker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

public class VirtualEntityGroup implements Cullable {
    private final UUID uniqueId;
    private final List<VirtualEntity<?>> children;
    private final Map<VirtualEntity<?>, GroupTransform> localTransforms;
    private Location coreLocation;
    private BoundingBox boundingBox;
    private Predicate<Player> visibilityFilter = null;
    private Quaternionf groupRotation;
    private final Set<UUID> viewers = new CopyOnWriteArraySet<>();

    private World world;

    public record GroupTransform(Vector3f localTranslation, Quaternionf localRotation) {}

    public VirtualEntityGroup(Location coreLocation) {
        this.uniqueId = UUID.randomUUID();
        this.children = new ArrayList<>();
        this.localTransforms = new ConcurrentHashMap<>();
        this.coreLocation = coreLocation.clone();
        this.boundingBox = BoundingBox.of(this.coreLocation, 0, 0, 0);
        this.groupRotation = new Quaternionf();
        this.recalculateBounds();
        CullingTracker.register(this);
    }

    public void addEntity(VirtualEntity<?> entity) {
        if (entity instanceof VirtualDisplay<?> display) {
            addEntity(entity, new Vector3f(), new Quaternionf(display.getTransformation().getLeftRotation()));
        } else {
            addEntity(entity, new Vector3f(), new Quaternionf());
        }
    }

    public void addEntity(VirtualEntity<?> entity, Vector3f localOffset, Quaternionf localRotation) {
        CullingTracker.unregister(entity.getUniqueId());
        children.add(entity);
        localTransforms.put(entity, new GroupTransform(localOffset, localRotation));
        updateChild(entity);
        CullingTracker.reindex(this, this::recalculateBounds);

        for (UUID viewerId : viewers) {
            Player p = Bukkit.getPlayer(viewerId);
            if (p != null) {
                entity.spawnFor(p);
            }
        }
    }

    public void removeEntity(VirtualEntity<?> entity) {
        if (children.remove(entity)) {
            localTransforms.remove(entity);
            for (UUID viewerId : viewers) {
                Player p = Bukkit.getPlayer(viewerId);
                if (p != null) {
                    entity.destroyFor(p);
                }
            }
            entity.destroyAll();
            CullingTracker.reindex(this, this::recalculateBounds);
        }
    }

    public void setGroupRotation(Quaternionf rotation) {
        this.groupRotation = rotation;
        for (VirtualEntity<?> child : children) {
            updateChild(child);
        }
        CullingTracker.reindex(this, this::recalculateBounds);
    }

    private void updateChild(VirtualEntity<?> child) {
        GroupTransform transform = localTransforms.get(child);
        if (transform == null) return;

        Vector3f offset = new Vector3f(transform.localTranslation());
        offset.rotate(groupRotation);

        Location newLoc = coreLocation.clone().add(offset.x, offset.y, offset.z);

        if (child instanceof VirtualDisplay<?> display) {
            child.teleport(newLoc);
            Quaternionf finalRot = new Quaternionf(groupRotation).mul(transform.localRotation());
            org.bukkit.util.Transformation base = display.getTransformation();
            display.setTransformation(base.getTranslation(), finalRot, base.getScale(), base.getRightRotation());
            display.flush();
        } else {
            Vector3f euler = new Vector3f();
            groupRotation.getEulerAnglesXYZ(euler);
            newLoc.setYaw((float) Math.toDegrees(-euler.y));
            newLoc.setPitch((float) Math.toDegrees(euler.x));
            child.teleport(newLoc);
        }
    }

    public void teleport(Location newLocation) {
        CullingTracker.reindex(this, () -> {
            this.coreLocation = newLocation.clone();
            for (VirtualEntity<?> child : children) {
                updateChild(child);
            }
            recalculateBounds();
        });
    }

    public List<VirtualEntity<?>> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void recalculateBounds() {
        this.world = coreLocation.getWorld();

        if (children.isEmpty()) {
            this.boundingBox = BoundingBox.of(this.coreLocation, 0, 0, 0);
            return;
        }

        BoundingBox merged = null;
        for (VirtualEntity<?> child : children) {
            if (merged == null) {
                merged = child.getBoundingBox().clone();
            } else {
                merged.union(child.getBoundingBox());
            }
        }

        this.boundingBox = merged;
    }

    @Override
    public Predicate<Player> getVisibilityFilter() {
        return visibilityFilter;
    }

    @Override
    public void setVisibilityFilter(Predicate<Player> filter) {
        this.visibilityFilter = filter;
    }

    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public Location getLocation() {
        return coreLocation;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    @Override
    public void spawnFor(Player player) {
        if (viewers.add(player.getUniqueId())) {
            for (VirtualEntity<?> child : children) {
                child.spawnFor(player);
            }
        }
    }

    @Override
    public void destroyFor(Player player) {
        if (viewers.remove(player.getUniqueId())) {
            for (VirtualEntity<?> child : children) {
                child.destroyFor(player);
            }
        }
    }

    @Override
    public void destroyAll() {
        CullingTracker.unregister(this.uniqueId);
        for (VirtualEntity<?> child : children) {
            child.destroyAll();
        }
        children.clear();
        localTransforms.clear();
        viewers.clear();
    }
}