package com.github.darksoulq.visage.packet;

import com.github.darksoulq.visage.event.PacketReceiveEvent;
import com.github.darksoulq.visage.event.PacketSendEvent;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public final class PacketInterceptor {
    private static final String HANDLER_NAME = "visage_packet_listener";

    public static void inject(Player player) {
        ServerPlayer nms = ((CraftPlayer) player).getHandle();
        Connection connection = nms.connection.connection;
        ChannelPipeline pipeline = connection.channel.pipeline();

        if (pipeline.get(HANDLER_NAME) == null) {
            ChannelDuplexHandler handler = new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    if (msg instanceof Packet<?> packet) {
                        try {
                            if (PacketReceiveEvent.getHandlerList().getRegisteredListeners().length > 0) {
                                PacketReceiveEvent event = new PacketReceiveEvent(player, packet);
                                Bukkit.getPluginManager().callEvent(event);
                                if (event.isCancelled()) {
                                    return;
                                }
                                super.channelRead(ctx, event.getPacket());
                                return;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    super.channelRead(ctx, msg);
                }

                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    if (msg instanceof Packet<?> packet) {
                        try {
                            if (PacketSendEvent.getHandlerList().getRegisteredListeners().length > 0) {
                                PacketSendEvent event = new PacketSendEvent(player, packet);
                                Bukkit.getPluginManager().callEvent(event);
                                if (event.isCancelled()) {
                                    promise.setSuccess();
                                    return;
                                }
                                super.write(ctx, event.getPacket(), promise);
                                return;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    super.write(ctx, msg, promise);
                }
            };

            try {
                pipeline.addBefore("packet_handler", HANDLER_NAME, handler);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void uninject(Player player) {
        ServerPlayer nms = ((CraftPlayer) player).getHandle();
        Connection connection = nms.connection.connection;
        ChannelPipeline pipeline = connection.channel.pipeline();

        try {
            if (pipeline.get(HANDLER_NAME) != null) {
                pipeline.remove(HANDLER_NAME);
            }
        } catch (Throwable ignored) {
        }
    }
}