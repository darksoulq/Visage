package com.github.darksoulq.visage.util.nms;

import net.minecraft.world.entity.EntityType;

import java.lang.reflect.Field;

public class NMSEntities {

    public static final EntityType<?> BLOCK_DISPLAY = getEntityType("BLOCK_DISPLAY");
    public static final EntityType<?> ITEM_DISPLAY = getEntityType("ITEM_DISPLAY");
    public static final EntityType<?> TEXT_DISPLAY = getEntityType("TEXT_DISPLAY");
    public static final EntityType<?> INTERACTION = getEntityType("INTERACTION");
    public static final EntityType<?> ITEM = getEntityType("ITEM");

    private static EntityType<?> getEntityType(String name) {
        try {
            Class<?> clazz;
            try {
                clazz = Class.forName("net.minecraft.world.entity.EntityTypes");
            } catch (ClassNotFoundException e) {
                clazz = Class.forName("net.minecraft.world.entity.EntityType");
            }
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            return (EntityType<?>) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}