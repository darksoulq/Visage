package com.github.darksoulq.visage.util.nms;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;

public class NMSReflection {

    public static MethodHandle findGetter(MethodHandles.Lookup lookup, Class<?> clazz, String fieldName, String methodName) throws Exception {
        try {
            Method m = clazz.getDeclaredMethod(methodName);
            m.setAccessible(true);
            return lookup.unreflect(m);
        } catch (NoSuchMethodException e) {
            try {
                Method m = clazz.getDeclaredMethod(fieldName);
                m.setAccessible(true);
                return lookup.unreflect(m);
            } catch (NoSuchMethodException e2) {
                Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                return lookup.unreflectGetter(f);
            }
        }
    }

    public static Object getRecordComponent(Object record, String name) {
        try {
            for (RecordComponent comp : record.getClass().getRecordComponents()) {
                if (comp.getName().equals(name)) {
                    return comp.getAccessor().invoke(record);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static Object invokeMethod(Object obj, String name) {
        try {
            Method m = obj.getClass().getMethod(name);
            m.setAccessible(true);
            return m.invoke(obj);
        } catch (Exception ignored) {}
        return null;
    }

    public static double getVecCoord(Object vec, String name) {
        try {
            return (double) vec.getClass().getField(name).get(vec);
        } catch (Exception e) {
            try {
                return (double) vec.getClass().getMethod(name).invoke(vec);
            } catch (Exception ex) {}
        }
        return 0.0;
    }
}