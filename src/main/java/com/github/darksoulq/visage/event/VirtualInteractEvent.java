package com.github.darksoulq.visage.event;

import com.github.darksoulq.visage.entity.VirtualEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

public class VirtualInteractEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final VirtualEntity<?> entity;
    private final InteractAction action;
    private final EquipmentSlot hand;
    private final Vector clickPosition;
    private final boolean sneaking;

    public VirtualInteractEvent(Player player, VirtualEntity<?> entity, InteractAction action, EquipmentSlot hand, Vector clickPosition, boolean sneaking) {
        super(true);
        this.player = player;
        this.entity = entity;
        this.action = action;
        this.hand = hand;
        this.clickPosition = clickPosition;
        this.sneaking = sneaking;
    }

    public Player getPlayer() {
        return player;
    }

    public VirtualEntity<?> getEntity() {
        return entity;
    }

    public InteractAction getAction() {
        return action;
    }

    public EquipmentSlot getHand() {
        return hand;
    }

    public Vector getClickPosition() {
        return clickPosition;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}