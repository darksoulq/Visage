package com.github.darksoulq.visage.event;

import com.github.darksoulq.visage.entity.VirtualEntity;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import org.bukkit.Bukkit;
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
        if (event.getPacket() instanceof ServerboundInteractPacket interactPacket) {
            int entityId = interactPacket.entityId();
            VirtualEntity<?> virtualEntity = VirtualEntity.getById(entityId);

            if (virtualEntity != null) {
                event.setCancelled(true);

                EquipmentSlot slot = interactPacket.hand() == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;
                Vector pos = interactPacket.location() != null ? new Vector(interactPacket.location().x, interactPacket.location().y, interactPacket.location().z) : null;
                boolean sneaking = interactPacket.usingSecondaryAction();

                VirtualInteractEvent interactEvent = new VirtualInteractEvent(event.getPlayer(), virtualEntity, InteractAction.INTERACT, slot, pos, sneaking);
                Bukkit.getPluginManager().callEvent(interactEvent);
            }
        }
    }
}