package com.github.darksoulq.visage.event;

import com.github.darksoulq.visage.block.VirtualBlockTracker;
import com.github.darksoulq.visage.culling.CullingTracker;
import com.github.darksoulq.visage.packet.PacketInterceptor;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;

public class VisageListener implements Listener {

    public static void register(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new VisageListener(), plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PacketInterceptor.inject(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PacketInterceptor.uninject(event.getPlayer());
        CullingTracker.clearPlayer(event.getPlayer());
        VirtualBlockTracker.clearPlayer(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        CullingTracker.clearPlayer(event.getPlayer());
        VirtualBlockTracker.clearPlayer(event.getPlayer());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        CullingTracker.unloadWorld(event.getWorld());
        VirtualBlockTracker.unloadWorld(event.getWorld());
    }

    @EventHandler
    public void onPacketSend(PacketSendEvent event) {
        Object rawPacket = event.getPacket();
        if (rawPacket instanceof ClientboundLevelChunkWithLightPacket chunkPacket) {
            VirtualBlockTracker.onChunkSent(event.getPlayer(), chunkPacket.x(), chunkPacket.z(), event.getPlayer().getWorld());
        }
    }
}