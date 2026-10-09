package com.github.darksoulq.visage.event;

import com.github.darksoulq.visage.block.VirtualBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class VirtualBlockInteractEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final VirtualBlock block;
    private final InteractAction action;

    public VirtualBlockInteractEvent(Player player, VirtualBlock block, InteractAction action) {
        super(true);
        this.player = player;
        this.block = block;
        this.action = action;
    }

    public Player getPlayer() {
        return player;
    }

    public VirtualBlock getBlock() {
        return block;
    }

    public InteractAction getAction() {
        return action;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}