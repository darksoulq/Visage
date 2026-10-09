package com.github.darksoulq.visage;

import com.github.darksoulq.visage.block.VirtualBlockTracker;
import com.github.darksoulq.visage.culling.CullingTracker;
import com.github.darksoulq.visage.event.InteractionListener;
import com.github.darksoulq.visage.event.VisageListener;
import com.github.darksoulq.visage.packet.PacketInterceptor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Visage {

    private static Plugin plugin;

    public static void init(Plugin pluginInstance) {
        plugin = pluginInstance;
        CullingTracker.start(plugin);
        VirtualBlockTracker.start(plugin);
        InteractionListener.register(plugin);
        VisageListener.register(plugin);

        for (Player player : Bukkit.getOnlinePlayers()) {
            PacketInterceptor.inject(player);
        }
    }

    public static Plugin getPlugin() {
        return plugin;
    }

    public static void shutdown() {
        CullingTracker.stop();
        VirtualBlockTracker.stop();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PacketInterceptor.uninject(player);
        }
    }
}