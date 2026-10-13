package com.github.darksoulq.visage.event;

import com.github.darksoulq.visage.Visage;
import com.github.darksoulq.visage.block.VirtualBlock;
import com.github.darksoulq.visage.block.VirtualBlockTracker;
import com.github.darksoulq.visage.entity.VirtualEntity;
import com.github.darksoulq.visage.util.nms.NMSPackets;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class InteractionListener implements Listener {

    public static void register(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new InteractionListener(), plugin);
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveEvent event) {
        Object rawPacket = event.getPacket();
        String name = rawPacket.getClass().getSimpleName();

        if (name.equals("ServerboundInteractPacket")) {
            int entityId = NMSPackets.getEntityId(rawPacket);
            VirtualEntity<?> virtualEntity = VirtualEntity.getById(entityId);

            if (virtualEntity != null) {
                event.setCancelled(true);
                boolean isAttack = NMSPackets.isAttack(rawPacket);
                InteractAction action = isAttack ? InteractAction.ATTACK : InteractAction.INTERACT;

                Object handObj = NMSPackets.getHand(rawPacket);
                EquipmentSlot slot = EquipmentSlot.HAND;
                if (handObj != null && handObj.toString().contains("OFF_HAND")) {
                    slot = EquipmentSlot.OFF_HAND;
                }

                Vector pos = NMSPackets.getVecLocation(rawPacket);
                boolean sneaking = NMSPackets.isSneaking(rawPacket);

                VirtualInteractEvent interactEvent = new VirtualInteractEvent(event.getPlayer(), virtualEntity, action, slot, pos, sneaking);
                Bukkit.getPluginManager().callEvent(interactEvent);
            }
        } else if (name.equals("ServerboundAttackPacket")) {
            int entityId = NMSPackets.getEntityId(rawPacket);
            VirtualEntity<?> virtualEntity = VirtualEntity.getById(entityId);

            if (virtualEntity != null) {
                event.setCancelled(true);
                VirtualInteractEvent interactEvent = new VirtualInteractEvent(event.getPlayer(), virtualEntity, InteractAction.ATTACK, EquipmentSlot.HAND, null, false);
                Bukkit.getPluginManager().callEvent(interactEvent);
            }
        } else if (name.equals("ServerboundPlayerActionPacket")) {
            Location loc = NMSPackets.getActionLocation(rawPacket, event.getPlayer().getWorld());
            if (loc != null) {
                VirtualBlock block = VirtualBlockTracker.getBlock(loc);

                if (block != null && block.isVisibleTo(event.getPlayer())) {
                    Bukkit.getScheduler().runTaskLater(Visage.getPlugin(), () -> {
                        if (event.getPlayer().isOnline()) {
                            event.getPlayer().sendBlockChange(block.getLocation(), block.getBlockData());
                        }
                    }, 1L);

                    String actionName = NMSPackets.getActionType(rawPacket);
                    InteractAction action = actionName.contains("DESTROY") ? InteractAction.ATTACK : InteractAction.INTERACT;
                    VirtualBlockInteractEvent interactEvent = new VirtualBlockInteractEvent(event.getPlayer(), block, action);
                    Bukkit.getPluginManager().callEvent(interactEvent);
                }
            }
        } else if (name.equals("ServerboundUseItemOnPacket")) {
            Location loc = NMSPackets.getUseItemLocation(rawPacket, event.getPlayer().getWorld());
            if (loc != null) {
                VirtualBlock block = VirtualBlockTracker.getBlock(loc);

                if (block != null && block.isVisibleTo(event.getPlayer())) {
                    Bukkit.getScheduler().runTaskLater(Visage.getPlugin(), () -> {
                        if (event.getPlayer().isOnline()) {
                            event.getPlayer().sendBlockChange(block.getLocation(), block.getBlockData());
                        }
                    }, 1L);

                    VirtualBlockInteractEvent interactEvent = new VirtualBlockInteractEvent(event.getPlayer(), block, InteractAction.INTERACT);
                    Bukkit.getPluginManager().callEvent(interactEvent);
                }
            }
        }
    }
}