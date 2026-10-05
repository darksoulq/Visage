package com.github.darksoulq.visage.event;

import com.github.darksoulq.visage.culling.CullingTracker;
import com.github.darksoulq.visage.packet.PacketInterceptor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        CullingTracker.unloadWorld(event.getWorld());
    }
}