package com.github.darksoulq.visage.entity;

import com.github.darksoulq.visage.util.nms.NMSEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.EntityType;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public class VirtualBlockDisplay extends VirtualDisplay<BlockDisplay> {

    private BlockData blockData;

    public VirtualBlockDisplay(Location location) {
        super(location);
    }

    @Override
    protected BlockDisplay createNmsEntity(ServerLevel level, Location location) {
        BlockDisplay display = new BlockDisplay(NMSEntities.BLOCK_DISPLAY, level);
        display.setPos(location.getX(), location.getY(), location.getZ());
        display.setYRot(location.getYaw());
        display.setXRot(location.getPitch());
        return display;
    }

    public void setBlockData(BlockData blockData) {
        this.blockData = blockData.clone();
        nmsEntity.setBlockState(((CraftBlockData) blockData).getState());
    }

    public BlockData getBlockData() {
        return blockData != null ? blockData.clone() : null;
    }
}