package com.github.darksoulq.visage.block.custom;

import com.github.darksoulq.abyssallib.world.block.BlockEntity;
import com.github.darksoulq.visage.entity.EntityGroupable;
import com.github.darksoulq.visage.entity.VirtualDisplay;
import com.github.darksoulq.visage.entity.VirtualEntity;
import com.github.darksoulq.visage.entity.VirtualEntityGroup;
import org.bukkit.Location;
import org.bukkit.block.Block;

public abstract class ModelledBlockEntity extends BlockEntity {

    protected VirtualEntityGroup modelGroup;
    protected boolean syncLight = true;

    public ModelledBlockEntity(ModelledCustomBlock block) {
        super(block);
    }

    public void spawnModel() {
        if (modelGroup != null) {
            modelGroup.destroyAll();
        }
        Location loc = getBlock().getLocation().clone().add(0.5, 0.5, 0.5);
        modelGroup = new VirtualEntityGroup(loc);
        buildModel(modelGroup, loc);
        updateModel();
        modelGroup.syncLight();
    }

    protected abstract void buildModel(VirtualEntityGroup group, Location location);

    public abstract void updateModel();

    public void destroyModel() {
        if (modelGroup != null) {
            modelGroup.destroyAll();
            modelGroup = null;
        }
    }

    public VirtualEntityGroup getModelGroup() {
        return modelGroup;
    }
}