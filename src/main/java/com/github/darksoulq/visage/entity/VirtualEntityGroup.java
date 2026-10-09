package com.github.darksoulq.visage.entity;

import com.github.darksoulq.visage.culling.CullingTracker;
import com.github.darksoulq.visage.packet.PacketIO;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
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
import java.util.function.Consumer;
import java.util.function.Predicate;

public class VirtualEntityGroup implements EntityGroupable {
    private final UUID uniqueId;
    private final List<EntityGroupable> children;
    private final Map<EntityGroupable, GroupTransform> localTransforms;
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

    public void modify(Consumer<VirtualEntityGroup> action) {
        action.accept(this);
        flush();
    }

    public void addEntity(EntityGroupable entity) {
        if (entity instanceof VirtualDisplay<?> display) {
            addEntity(entity, new Vector3f(), new Quaternionf(display.getTransformation().getLeftRotation()));
        } else {
            addEntity(entity, new Vector3f(), new Quaternionf());
        }
    }

    public void addEntity(EntityGroupable entity, Vector3f localOffset, Quaternionf localRotation) {
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

    public void removeEntity(EntityGroupable entity) {
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
        for (EntityGroupable child : children) {
            updateChild(child);
        }
        CullingTracker.reindex(this, this::recalculateBounds);
    }

    private void updateChild(EntityGroupable child) {
        GroupTransform transform = localTransforms.get(child);
        if (transform == null) return;
        child.applyGroupTransform(coreLocation, transform.localTranslation(), transform.localRotation(), groupRotation);
    }

    @Override
    public void applyGroupTransform(Location coreLoc, Vector3f localTranslation, Quaternionf localRotation, Quaternionf groupRot) {
        Vector3f offset = new Vector3f(localTranslation);
        offset.rotate(groupRot);
        Location newLoc = coreLoc.clone().add(offset.x, offset.y, offset.z);
        this.setGroupRotation(new Quaternionf(groupRot).mul(localRotation));
        this.teleport(newLoc);
    }

    @Override
    public void collectPackets(List<Packet<? super ClientGamePacketListener>> packets) {
        for (EntityGroupable child : children) {
            child.collectPackets(packets);
        }
    }

    @Override
    public void flush() {
        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        collectPackets(packets);
        if (!packets.isEmpty() && !viewers.isEmpty()) {
            ClientboundBundlePacket bundle = new ClientboundBundlePacket(packets);
            for (UUID viewerId : viewers) {
                Player p = Bukkit.getPlayer(viewerId);
                if (p != null) PacketIO.send(p, bundle);
            }
        }
    }

    public void teleport(Location newLocation) {
        CullingTracker.reindex(this, () -> {
            this.coreLocation = newLocation.clone();
            for (EntityGroupable child : children) {
                updateChild(child);
            }
            recalculateBounds();
        });
    }

    public List<EntityGroupable> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void recalculateBounds() {
        this.world = coreLocation.getWorld();

        if (children.isEmpty()) {
            this.boundingBox = BoundingBox.of(this.coreLocation, 0, 0, 0);
            return;
        }

        BoundingBox merged = null;
        for (EntityGroupable child : children) {
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
            for (EntityGroupable child : children) {
                child.spawnFor(player);
            }
        }
    }

    @Override
    public void destroyFor(Player player) {
        if (viewers.remove(player.getUniqueId())) {
            for (EntityGroupable child : children) {
                child.destroyFor(player);
            }
        }
    }

    @Override
    public void destroyAll() {
        CullingTracker.unregister(this.uniqueId);
        for (EntityGroupable child : children) {
            child.destroyAll();
        }
        children.clear();
        localTransforms.clear();
        viewers.clear();
    }
}