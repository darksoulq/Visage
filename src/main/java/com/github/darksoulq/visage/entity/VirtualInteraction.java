package com.github.darksoulq.visage.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import org.bukkit.Location;

public class VirtualInteraction extends VirtualEntity<Interaction> {

    private float width = 0.0f;
    private float height = 0.0f;

    public VirtualInteraction(Location location) {
        super(location);
    }

    @Override
    protected Interaction createNmsEntity(ServerLevel level, Location location) {
        Interaction interaction = new Interaction(EntityTypes.INTERACTION, level);
        interaction.setPos(location.getX(), location.getY(), location.getZ());
        interaction.setYRot(location.getYaw());
        interaction.setXRot(location.getPitch());
        return interaction;
    }

    public void setDimensions(float width, float height) {
        this.width = width;
        this.height = height;
        nmsEntity.setWidth(width);
        nmsEntity.setHeight(height);
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}