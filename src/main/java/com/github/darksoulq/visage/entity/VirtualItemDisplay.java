package com.github.darksoulq.visage.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemDisplayContext;
import org.bukkit.Location;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class VirtualItemDisplay extends VirtualDisplay<ItemDisplay> {

    private ItemStack itemStack;
    private ItemDisplayTransform itemDisplayTransform = ItemDisplayTransform.NONE;

    public VirtualItemDisplay(Location location) {
        super(location);
    }

    @Override
    protected ItemDisplay createNmsEntity(ServerLevel level, Location location) {
        ItemDisplay display = new ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
        display.setPos(location.getX(), location.getY(), location.getZ());
        display.setYRot(location.getYaw());
        display.setXRot(location.getPitch());
        return display;
    }

    public void setItemStack(ItemStack item) {
        this.itemStack = item != null ? item.clone() : null;
        nmsEntity.setItemStack(CraftItemStack.asNMSCopy(item));
    }

    public void setItemDisplayContext(ItemDisplayTransform transform) {
        this.itemDisplayTransform = transform;
        ItemDisplayContext nmsContext = switch (transform) {
            case NONE -> ItemDisplayContext.NONE;
            case THIRD_PERSON_LEFT_HAND -> ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            case THIRD_PERSON_RIGHT_HAND -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            case FIRST_PERSON_LEFT_HAND -> ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            case FIRST_PERSON_RIGHT_HAND -> ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
            case HEAD -> ItemDisplayContext.HEAD;
            case GUI -> ItemDisplayContext.GUI;
            case GROUND -> ItemDisplayContext.GROUND;
            case FIXED -> ItemDisplayContext.FIXED;
            case ON_SHELF -> ItemDisplayContext.ON_SHELF;
        };
        nmsEntity.setItemTransform(nmsContext);
    }

    public ItemStack getItemStack() {
        return itemStack != null ? itemStack.clone() : null;
    }

    public ItemDisplayTransform getItemDisplayContext() {
        return itemDisplayTransform;
    }
}