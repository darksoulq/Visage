package com.github.darksoulq.visage.util.nms;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class NMSMath {

    private static MethodHandle TRANS_TRANS;
    private static MethodHandle TRANS_LEFT;
    private static MethodHandle TRANS_SCALE;
    private static MethodHandle TRANS_RIGHT;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> transClass = Class.forName("com.mojang.math.Transformation");
            TRANS_TRANS = NMSReflection.findGetter(lookup, transClass, "translation", "getTranslation");
            TRANS_LEFT = NMSReflection.findGetter(lookup, transClass, "leftRotation", "getLeftRotation");
            TRANS_SCALE = NMSReflection.findGetter(lookup, transClass, "scale", "getScale");
            TRANS_RIGHT = NMSReflection.findGetter(lookup, transClass, "rightRotation", "getRightRotation");
        } catch (Exception ignored) {}
    }

    public static Vector3f getTranslation(Object transformation) {
        try { return (Vector3f) TRANS_TRANS.invoke(transformation); } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static Quaternionf getLeftRotation(Object transformation) {
        try { return (Quaternionf) TRANS_LEFT.invoke(transformation); } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static Vector3f getScale(Object transformation) {
        try { return (Vector3f) TRANS_SCALE.invoke(transformation); } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static Quaternionf getRightRotation(Object transformation) {
        try { return (Quaternionf) TRANS_RIGHT.invoke(transformation); } catch (Throwable t) { throw new RuntimeException(t); }
    }
}