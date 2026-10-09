package com.github.darksoulq.visage.util;

import com.github.darksoulq.visage.entity.EntityEvent;
import com.github.darksoulq.visage.entity.VirtualEntity;
import com.github.darksoulq.visage.packet.PacketIO;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.entity.Entity;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class EntityPacketUtil {

    public static Entity getNmsEntity(Object entity) {
        if (entity instanceof VirtualEntity<?> ve) {
            return ve.getHandle();
        } else if (entity instanceof org.bukkit.entity.Entity be) {
            return ((CraftEntity) be).getHandle();
        }
        return null;
    }

    public static void sendAnimation(Player viewer, Object entity, int action) {
        Entity nmsEntity = getNmsEntity(entity);
        if (nmsEntity != null) {
            PacketIO.send(viewer, new ClientboundAnimatePacket(nmsEntity, action));
        }
    }

    public static void sendAnimation(Collection<Player> viewers, Object entity, int action) {
        Entity nmsEntity = getNmsEntity(entity);
        if (nmsEntity != null) {
            PacketIO.send(viewers, new ClientboundAnimatePacket(nmsEntity, action));
        }
    }

    public static void sendEntityEvent(Player viewer, Object entity, EntityEvent event) {
        Entity nmsEntity = getNmsEntity(entity);
        if (nmsEntity != null) {
            PacketIO.send(viewer, new ClientboundEntityEventPacket(nmsEntity, event.getId()));
        }
    }

    public static void sendEntityEvent(Collection<Player> viewers, Object entity, EntityEvent event) {
        Entity nmsEntity = getNmsEntity(entity);
        if (nmsEntity != null) {
            PacketIO.send(viewers, new ClientboundEntityEventPacket(nmsEntity, event.getId()));
        }
    }

    public static void sendRotateHead(Player viewer, Object entity, byte yHeadRot) {
        Entity nmsEntity = getNmsEntity(entity);
        if (nmsEntity != null) {
            PacketIO.send(viewer, new ClientboundRotateHeadPacket(nmsEntity, yHeadRot));
        }
    }

    public static void sendRotateHead(Collection<Player> viewers, Object entity, byte yHeadRot) {
        Entity nmsEntity = getNmsEntity(entity);
        if (nmsEntity != null) {
            PacketIO.send(viewers, new ClientboundRotateHeadPacket(nmsEntity, yHeadRot));
        }
    }

    public static void sendEquipment(Player viewer, Object entity, Map<EquipmentSlot, ItemStack> equipment) {
        Entity nmsEntity = getNmsEntity(entity);
        if (nmsEntity != null && !equipment.isEmpty()) {
            List<Pair<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack>> slots = new ArrayList<>();
            for (Map.Entry<EquipmentSlot, ItemStack> entry : equipment.entrySet()) {
                slots.add(Pair.of(toNmsSlot(entry.getKey()), CraftItemStack.asNMSCopy(entry.getValue())));
            }
            PacketIO.send(viewer, new ClientboundSetEquipmentPacket(nmsEntity.getId(), slots, false));
        }
    }

    public static void sendEquipment(Collection<Player> viewers, Object entity, Map<EquipmentSlot, ItemStack> equipment) {
        Entity nmsEntity = getNmsEntity(entity);
        if (nmsEntity != null && !equipment.isEmpty()) {
            List<Pair<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack>> slots = new ArrayList<>();
            for (Map.Entry<EquipmentSlot, ItemStack> entry : equipment.entrySet()) {
                slots.add(Pair.of(toNmsSlot(entry.getKey()), CraftItemStack.asNMSCopy(entry.getValue())));
            }
            PacketIO.send(viewers, new ClientboundSetEquipmentPacket(nmsEntity.getId(), slots, false));
        }
    }

    private static net.minecraft.world.entity.EquipmentSlot toNmsSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HAND -> net.minecraft.world.entity.EquipmentSlot.MAINHAND;
            case OFF_HAND -> net.minecraft.world.entity.EquipmentSlot.OFFHAND;
            case FEET -> net.minecraft.world.entity.EquipmentSlot.FEET;
            case LEGS -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case CHEST -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case HEAD -> net.minecraft.world.entity.EquipmentSlot.HEAD;
            case BODY -> net.minecraft.world.entity.EquipmentSlot.BODY;
            case SADDLE -> net.minecraft.world.entity.EquipmentSlot.SADDLE;
        };
    }
}