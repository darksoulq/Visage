package com.github.darksoulq.visage.entity;

import com.github.darksoulq.visage.culling.Cullable;
import com.github.darksoulq.visage.culling.CullingTracker;
import com.github.darksoulq.visage.packet.PacketIO;
import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

public abstract class VirtualEntity<T extends Entity> implements Cullable {
    private static final Map<Integer, VirtualEntity<?>> ENTITY_REGISTRY = new ConcurrentHashMap<>();

    protected final T nmsEntity;
    protected final Location location;
    protected BoundingBox boundingBox;
    protected final UUID uniqueId;
    protected final Set<UUID> viewers = new CopyOnWriteArraySet<>();
    protected Predicate<Player> visibilityFilter = null;

    protected World world;
    protected double locX, locZ;
    protected double centerX, centerY, centerZ;
    protected double cullingRadius;

    public VirtualEntity(Location location) {
        this.location = location.clone();
        this.uniqueId = UUID.randomUUID();
        this.boundingBox = BoundingBox.of(this.location, 0.5, 0.5, 0.5);
        this.recalculateBounds();

        ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
        this.nmsEntity = createNmsEntity(level, this.location);
        this.nmsEntity.setUUID(this.uniqueId);

        ENTITY_REGISTRY.put(this.nmsEntity.getId(), this);
        CullingTracker.register(this);
    }

    public static VirtualEntity<?> getById(int entityId) {
        return ENTITY_REGISTRY.get(entityId);
    }

    protected abstract T createNmsEntity(ServerLevel level, Location location);

    public int getEntityId() {
        return nmsEntity.getId();
    }

    public void flush() {
        var data = nmsEntity.getEntityData().packDirty();
        if (data != null && !viewers.isEmpty()) {
            ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(nmsEntity.getId(), data);
            for (UUID viewerId : viewers) {
                Player p = Bukkit.getPlayer(viewerId);
                if (p != null) PacketIO.send(p, packet);
            }
        }
    }

    public void teleport(Location newLocation) {
        CullingTracker.reindex(this, () -> {
            this.location.setX(newLocation.getX());
            this.location.setY(newLocation.getY());
            this.location.setZ(newLocation.getZ());
            this.location.setYaw(newLocation.getYaw());
            this.location.setPitch(newLocation.getPitch());
            this.location.setWorld(newLocation.getWorld());

            this.recalculateBounds();

            this.nmsEntity.setPos(newLocation.getX(), newLocation.getY(), newLocation.getZ());
            this.nmsEntity.setYRot(newLocation.getYaw());
            this.nmsEntity.setXRot(newLocation.getPitch());

            if (!viewers.isEmpty()) {
                PositionMoveRotation pos = new PositionMoveRotation(
                    new Vec3(newLocation.getX(), newLocation.getY(), newLocation.getZ()),
                    Vec3.ZERO,
                    newLocation.getYaw(),
                    newLocation.getPitch()
                );
                ClientboundTeleportEntityPacket packet = new ClientboundTeleportEntityPacket(nmsEntity.getId(), pos, Collections.emptySet(), false);
                for (UUID viewerId : viewers) {
                    Player p = Bukkit.getPlayer(viewerId);
                    if (p != null) PacketIO.send(p, packet);
                }
            }
        });
    }

    private void recalculateBounds() {
        this.world = location.getWorld();
        this.locX = location.getX();
        this.locZ = location.getZ();
        this.centerX = (boundingBox.getMaxX() + boundingBox.getMinX()) / 2.0;
        this.centerY = (boundingBox.getMaxY() + boundingBox.getMinY()) / 2.0;
        this.centerZ = (boundingBox.getMaxZ() + boundingBox.getMinZ()) / 2.0;
        double rX = (boundingBox.getMaxX() - boundingBox.getMinX()) / 2.0;
        double rY = (boundingBox.getMaxY() - boundingBox.getMinY()) / 2.0;
        double rZ = (boundingBox.getMaxZ() - boundingBox.getMinZ()) / 2.0;
        this.cullingRadius = Math.sqrt(rX * rX + rY * rY + rZ * rZ);
    }

    public void setGlowing(boolean glowing) {
        nmsEntity.setGlowingTag(glowing);
    }

    public void setInvisible(boolean invisible) {
        nmsEntity.setInvisible(invisible);
    }

    public void setCustomName(net.kyori.adventure.text.Component name) {
        nmsEntity.setCustomName(name != null ? PaperAdventure.asVanilla(name) : null);
    }

    public void setCustomNameVisible(boolean visible) {
        nmsEntity.setCustomNameVisible(visible);
    }

    public Set<UUID> getViewers() {
        return Collections.unmodifiableSet(viewers);
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
        return location;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public double getX() {
        return locX;
    }

    @Override
    public double getZ() {
        return locZ;
    }

    @Override
    public double getCenterX() {
        return centerX;
    }

    @Override
    public double getCenterY() {
        return centerY;
    }

    @Override
    public double getCenterZ() {
        return centerZ;
    }

    @Override
    public double getCullingRadius() {
        return cullingRadius;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBox boundingBox) {
        CullingTracker.reindex(this, () -> {
            this.boundingBox = boundingBox;
            this.recalculateBounds();
        });
    }

    @Override
    public void spawnFor(Player player) {
        if (viewers.add(player.getUniqueId())) {
            List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
            packets.add(new ClientboundAddEntityPacket(
                nmsEntity.getId(),
                nmsEntity.getUUID(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getPitch(),
                location.getYaw(),
                nmsEntity.getType(),
                0,
                Vec3.ZERO,
                (double) location.getYaw()
            ));

            var data = nmsEntity.getEntityData().getNonDefaultValues();
            if (data != null) {
                packets.add(new ClientboundSetEntityDataPacket(nmsEntity.getId(), data));
            }

            PacketIO.send(player, new ClientboundBundlePacket(packets));
        }
    }

    @Override
    public void destroyFor(Player player) {
        if (viewers.remove(player.getUniqueId())) {
            PacketIO.send(player, new ClientboundRemoveEntitiesPacket(nmsEntity.getId()));
        }
    }

    @Override
    public void destroyAll() {
        CullingTracker.unregister(this.uniqueId);
        ENTITY_REGISTRY.remove(this.nmsEntity.getId());
        if (viewers.isEmpty()) return;

        ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(nmsEntity.getId());
        for (UUID viewerId : viewers) {
            Player p = Bukkit.getPlayer(viewerId);
            if (p != null) {
                PacketIO.send(p, packet);
            }
        }
        viewers.clear();
    }
}