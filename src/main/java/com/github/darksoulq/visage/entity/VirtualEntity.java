package com.github.darksoulq.visage.entity;

import com.github.darksoulq.visage.culling.CullingTracker;
import com.github.darksoulq.visage.packet.PacketIO;
import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
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

public abstract class VirtualEntity<T extends Entity> implements EntityGroupable {
    private static final Map<Integer, VirtualEntity<?>> ENTITY_REGISTRY = new ConcurrentHashMap<>();
    private static Field PASSENGERS_FIELD;

    static {
        try {
            PASSENGERS_FIELD = ClientboundSetPassengersPacket.class.getDeclaredField("passengers");
            PASSENGERS_FIELD.setAccessible(true);
        } catch (Exception ignored) {}
    }

    protected final T nmsEntity;
    protected final Location location;
    protected BoundingBox boundingBox;
    protected final UUID uniqueId;
    protected final Set<UUID> viewers = new CopyOnWriteArraySet<>();
    protected Predicate<Player> visibilityFilter = null;
    protected final List<VirtualEntity<?>> passengers = new ArrayList<>();

    protected World world;

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

    public T getHandle() {
        return nmsEntity;
    }

    public void modify(Consumer<VirtualEntity<T>> action) {
        action.accept(this);
        flush();
    }

    @Override
    public void collectPackets(List<Packet<? super ClientGamePacketListener>> packets) {
        var data = nmsEntity.getEntityData().packDirty();
        if (data != null) {
            packets.add(new ClientboundSetEntityDataPacket(nmsEntity.getId(), data));
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

    @Override
    public void applyGroupTransform(Location coreLocation, Vector3f localTranslation, Quaternionf localRotation, Quaternionf groupRotation) {
        Vector3f offset = new Vector3f(localTranslation);
        offset.rotate(groupRotation);
        Location newLoc = coreLocation.clone().add(offset.x, offset.y, offset.z);

        if (this instanceof VirtualDisplay<?> display) {
            this.teleport(newLoc);
            Quaternionf finalRot = new Quaternionf(groupRotation).mul(localRotation);
            org.bukkit.util.Transformation base = display.getTransformation();
            display.setTransformation(base.getTranslation(), finalRot, base.getScale(), base.getRightRotation());
            display.flush();
        } else {
            Vector3f euler = new Vector3f();
            groupRotation.getEulerAnglesXYZ(euler);
            newLoc.setYaw((float) Math.toDegrees(-euler.y));
            newLoc.setPitch((float) Math.toDegrees(euler.x));
            this.teleport(newLoc);
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

    public void setPassengers(List<VirtualEntity<?>> passengers) {
        this.passengers.clear();
        if (passengers != null) {
            this.passengers.addAll(passengers);
        }
        if (viewers.isEmpty() || PASSENGERS_FIELD == null) return;
        int[] arr = new int[this.passengers.size()];
        for (int i = 0; i < this.passengers.size(); i++) {
            arr[i] = this.passengers.get(i).getEntityId();
        }
        try {
            ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(nmsEntity);
            PASSENGERS_FIELD.set(packet, arr);
            ClientboundBundlePacket bundle = new ClientboundBundlePacket(List.of(packet));
            for (UUID viewerId : viewers) {
                Player p = Bukkit.getPlayer(viewerId);
                if (p != null) PacketIO.send(p, bundle);
            }
        } catch (Exception ignored) {}
    }

    public List<VirtualEntity<?>> getPassengers() {
        return Collections.unmodifiableList(passengers);
    }

    private void recalculateBounds() {
        this.world = location.getWorld();
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

            if (!passengers.isEmpty() && PASSENGERS_FIELD != null) {
                int[] arr = new int[passengers.size()];
                for (int i = 0; i < passengers.size(); i++) arr[i] = passengers.get(i).getEntityId();
                try {
                    ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(nmsEntity);
                    PASSENGERS_FIELD.set(packet, arr);
                    packets.add(packet);
                } catch (Exception ignored) {}
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