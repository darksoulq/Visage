package com.github.darksoulq.visage.util.nms;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class NMSPackets {

    private static MethodHandle CHUNK_X;
    private static MethodHandle CHUNK_Z;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> chunkPacket = Class.forName("net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket");
            CHUNK_X = NMSReflection.findGetter(lookup, chunkPacket, "x", "getX");
            CHUNK_Z = NMSReflection.findGetter(lookup, chunkPacket, "z", "getZ");
        } catch (Exception ignored) {}
    }

    public static int getChunkX(Object packet) {
        try { return (int) CHUNK_X.invoke(packet); } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static int getChunkZ(Object packet) {
        try { return (int) CHUNK_Z.invoke(packet); } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static int getEntityId(Object packet) {
        if (packet.getClass().isRecord()) {
            Object idObj = NMSReflection.getRecordComponent(packet, "entityId");
            if (idObj instanceof Integer id) return id;
        } else {
            Object idObj = NMSReflection.invokeMethod(packet, "getEntityId");
            if (idObj instanceof Integer id) return id;
        }
        return -1;
    }

    public static boolean isAttack(Object packet) {
        Object attackObj = NMSReflection.invokeMethod(packet, "isAttack");
        if (attackObj instanceof Boolean attack) return attack;
        return false;
    }

    public static Object getHand(Object packet) {
        if (packet.getClass().isRecord()) {
            return NMSReflection.getRecordComponent(packet, "hand");
        }
        return null;
    }

    public static Vector getVecLocation(Object packet) {
        Object vecObj = null;
        if (packet.getClass().isRecord()) {
            vecObj = NMSReflection.getRecordComponent(packet, "location");
        }
        if (vecObj != null) {
            double x = NMSReflection.getVecCoord(vecObj, "x");
            double y = NMSReflection.getVecCoord(vecObj, "y");
            double z = NMSReflection.getVecCoord(vecObj, "z");
            return new Vector(x, y, z);
        }
        return null;
    }

    public static boolean isSneaking(Object packet) {
        if (packet.getClass().isRecord()) {
            Object sneakObj = NMSReflection.getRecordComponent(packet, "sneaking");
            if (sneakObj instanceof Boolean sneak) return sneak;
        }
        return false;
    }

    public static Location getActionLocation(Object packet, World world) {
        Object posObj = NMSReflection.invokeMethod(packet, "getPos");
        if (posObj != null) {
            Object xObj = NMSReflection.invokeMethod(posObj, "getX");
            Object yObj = NMSReflection.invokeMethod(posObj, "getY");
            Object zObj = NMSReflection.invokeMethod(posObj, "getZ");

            if (xObj instanceof Integer x && yObj instanceof Integer y && zObj instanceof Integer z) {
                return new Location(world, x, y, z);
            }
        }
        return null;
    }

    public static String getActionType(Object packet) {
        Object actionObj = NMSReflection.invokeMethod(packet, "getAction");
        return actionObj != null ? actionObj.toString() : "";
    }

    public static Location getUseItemLocation(Object packet, World world) {
        Object hitResultObj = NMSReflection.invokeMethod(packet, "getHitResult");
        if (hitResultObj == null) {
            hitResultObj = NMSReflection.invokeMethod(packet, "hitResult");
        }
        if (hitResultObj != null) {
            Object posObj = NMSReflection.invokeMethod(hitResultObj, "getBlockPos");
            if (posObj != null) {
                Object xObj = NMSReflection.invokeMethod(posObj, "getX");
                Object yObj = NMSReflection.invokeMethod(posObj, "getY");
                Object zObj = NMSReflection.invokeMethod(posObj, "getZ");

                if (xObj instanceof Integer x && yObj instanceof Integer y && zObj instanceof Integer z) {
                    return new Location(world, x, y, z);
                }
            }
        }
        return null;
    }
}