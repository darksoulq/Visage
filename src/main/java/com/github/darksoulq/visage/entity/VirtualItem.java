package com.github.darksoulq.visage.entity;

import com.github.darksoulq.visage.util.nms.NMSEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class VirtualItem extends VirtualEntity<ItemEntity> {

    private ItemStack itemStack;

    public VirtualItem(Location location) {
        super(location);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ItemEntity createNmsEntity(ServerLevel level, Location location) {
        ItemEntity entity = new ItemEntity((EntityType<? extends ItemEntity>) NMSEntities.ITEM, level);
        entity.setPos(location.getX(), location.getY(), location.getZ());
        entity.setYRot(location.getYaw());
        entity.setXRot(location.getPitch());
        return entity;
    }

    public void setItemStack(ItemStack item) {
        this.itemStack = item != null ? item.clone() : null;
        nmsEntity.setItem(CraftItemStack.asNMSCopy(item));
    }

    public ItemStack getItemStack() {
        return itemStack != null ? itemStack.clone() : null;
    }
}