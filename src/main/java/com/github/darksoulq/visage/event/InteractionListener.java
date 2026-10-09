package com.github.darksoulq.visage.event;

import com.github.darksoulq.visage.Visage;
import com.github.darksoulq.visage.block.VirtualBlock;
import com.github.darksoulq.visage.block.VirtualBlockTracker;
import com.github.darksoulq.visage.entity.VirtualEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
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

        if (rawPacket instanceof ServerboundInteractPacket(int entityId, InteractionHand hand, net.minecraft.world.phys.Vec3 location, boolean sneaking)) {
            VirtualEntity<?> virtualEntity = VirtualEntity.getById(entityId);

            if (virtualEntity != null) {
                event.setCancelled(true);

                InteractAction action = InteractAction.INTERACT;
                EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;
                Vector pos = new Vector(location.x(), location.y(), location.z());

                VirtualInteractEvent interactEvent = new VirtualInteractEvent(event.getPlayer(), virtualEntity, action, slot, pos, sneaking);
                Bukkit.getPluginManager().callEvent(interactEvent);
            }
        } else if (rawPacket instanceof ServerboundAttackPacket(int entityId)) {
            VirtualEntity<?> virtualEntity = VirtualEntity.getById(entityId);

            if (virtualEntity != null) {
                event.setCancelled(true);

                InteractAction action = InteractAction.ATTACK;
                EquipmentSlot slot = EquipmentSlot.HAND;
                Vector pos = null;
                boolean sneaking = false;

                VirtualInteractEvent interactEvent = new VirtualInteractEvent(event.getPlayer(), virtualEntity, action, slot, pos, sneaking);
                Bukkit.getPluginManager().callEvent(interactEvent);
            }
        } else if (rawPacket instanceof ServerboundPlayerActionPacket actionPacket) {
            BlockPos pos = actionPacket.getPos();
            Location loc = new Location(event.getPlayer().getWorld(), pos.getX(), pos.getY(), pos.getZ());
            VirtualBlock block = VirtualBlockTracker.getBlock(loc);

            if (block != null && block.isVisibleTo(event.getPlayer())) {
                Bukkit.getScheduler().runTaskLater(Visage.getPlugin(), () -> {
                    if (event.getPlayer().isOnline()) {
                        event.getPlayer().sendBlockChange(block.getLocation(), block.getBlockData());
                    }
                }, 1L);

                InteractAction action = actionPacket.getAction().name().contains("DESTROY") ? InteractAction.ATTACK : InteractAction.INTERACT;
                VirtualBlockInteractEvent interactEvent = new VirtualBlockInteractEvent(event.getPlayer(), block, action);
                Bukkit.getPluginManager().callEvent(interactEvent);
            }
        } else if (rawPacket instanceof ServerboundUseItemOnPacket usePacket) {
            BlockHitResult hitResult = usePacket.hitResult();
            BlockPos pos = hitResult.getBlockPos();
            Location loc = new Location(event.getPlayer().getWorld(), pos.getX(), pos.getY(), pos.getZ());
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