package com.github.darksoulq.visage.block;

import com.github.darksoulq.abyssallib.world.block.BlockEntity;
import com.github.darksoulq.visage.entity.VirtualDisplay;
import com.github.darksoulq.visage.entity.VirtualEntity;
import com.github.darksoulq.visage.entity.VirtualEntityGroup;
import org.bukkit.Location;
import org.bukkit.block.Block;

public abstract class ModelledBlockEntity extends BlockEntity {

    protected VirtualEntityGroup modelGroup;

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
        syncLight();
    }

    protected abstract void buildModel(VirtualEntityGroup group, Location location);

    public abstract void updateModel();

    public void syncLight() {
        if (modelGroup != null) {
            Block b = getBlock().getLocation().getBlock();
            int blockLight = b.getLightFromBlocks();
            int skyLight = b.getLightFromSky();
            for (VirtualEntity<?> child : modelGroup.getChildren()) {
                if (child instanceof VirtualDisplay<?> display) {
                    display.setBrightness(blockLight, skyLight);
                    display.flush();
                }
            }
        }
    }

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