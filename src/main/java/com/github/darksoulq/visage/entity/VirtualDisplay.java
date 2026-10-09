package com.github.darksoulq.visage.entity;

import com.mojang.math.Transformation;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import org.bukkit.Location;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Method;

public abstract class VirtualDisplay<T extends Display> extends VirtualEntity<T> {

    private static Method SET_TELEPORT_DURATION_METHOD;

    static {
        try {
            SET_TELEPORT_DURATION_METHOD = Display.class.getDeclaredMethod("setPosRotInterpolationDuration", int.class);
            SET_TELEPORT_DURATION_METHOD.setAccessible(true);
        } catch (Exception ignored) {}
    }

    protected org.bukkit.util.Transformation transformation = new org.bukkit.util.Transformation(
        new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1), new Quaternionf()
    );
    protected Billboard billboard = Billboard.FIXED;
    protected int interpolationDuration = 0;
    protected int interpolationDelay = 0;
    protected int teleportDuration = 0;
    protected float viewRange = 1.0f;
    protected float shadowRadius = 0.0f;
    protected float shadowStrength = 1.0f;
    protected int glowColorOverride = -1;
    protected int blockLight = -1;
    protected int skyLight = -1;

    public VirtualDisplay(Location location) {
        super(location);
    }

    public void setTransformation(Vector3f translation, Quaternionf leftRotation, Vector3f scale, Quaternionf rightRotation) {
        this.transformation = new org.bukkit.util.Transformation(translation, leftRotation, scale, rightRotation);
        nmsEntity.setTransformation(new Transformation(translation, leftRotation, scale, rightRotation));
    }

    public void setTransformation(org.bukkit.util.Transformation transformation) {
        this.transformation = transformation;
        nmsEntity.setTransformation(new Transformation(
            new Vector3f(transformation.getTranslation()),
            new Quaternionf(transformation.getLeftRotation()),
            new Vector3f(transformation.getScale()),
            new Quaternionf(transformation.getRightRotation())
        ));
    }

    public void setTransformation(Matrix4f matrix) {
        Transformation nmsTransform = new Transformation(matrix);
        this.transformation = new org.bukkit.util.Transformation(
            new Vector3f(nmsTransform.translation()),
            new Quaternionf(nmsTransform.leftRotation()),
            new Vector3f(nmsTransform.scale()),
            new Quaternionf(nmsTransform.rightRotation())
        );
        nmsEntity.setTransformation(nmsTransform);
    }

    public void setBillboard(Billboard billboard) {
        this.billboard = billboard;
        Display.BillboardConstraints nmsConstraint = switch (billboard) {
            case FIXED -> Display.BillboardConstraints.FIXED;
            case VERTICAL -> Display.BillboardConstraints.VERTICAL;
            case HORIZONTAL -> Display.BillboardConstraints.HORIZONTAL;
            case CENTER -> Display.BillboardConstraints.CENTER;
        };
        nmsEntity.setBillboardConstraints(nmsConstraint);
    }

    public void setInterpolationDuration(int ticks) {
        this.interpolationDuration = ticks;
        nmsEntity.setTransformationInterpolationDuration(ticks);
    }

    public void setInterpolationDelay(int ticks) {
        this.interpolationDelay = ticks;
        nmsEntity.setTransformationInterpolationDelay(ticks);
    }

    public void setTeleportDuration(int ticks) {
        this.teleportDuration = ticks;
        if (SET_TELEPORT_DURATION_METHOD != null) {
            try {
                SET_TELEPORT_DURATION_METHOD.invoke(nmsEntity, ticks);
            } catch (Exception ignored) {}
        }
    }

    public void setViewRange(float range) {
        this.viewRange = range;
        nmsEntity.setViewRange(range);
    }

    public void setShadowRadius(float radius) {
        this.shadowRadius = radius;
        nmsEntity.setShadowRadius(radius);
    }

    public void setShadowStrength(float strength) {
        this.shadowStrength = strength;
        nmsEntity.setShadowStrength(strength);
    }

    public void setGlowColorOverride(int argb) {
        this.glowColorOverride = argb;
        nmsEntity.setGlowColorOverride(argb);
    }

    public void setBrightness(int blockLight, int skyLight) {
        this.blockLight = blockLight;
        this.skyLight = skyLight;
        nmsEntity.setBrightnessOverride(new Brightness(blockLight, skyLight));
    }

    public void resetBrightness() {
        this.blockLight = -1;
        this.skyLight = -1;
        nmsEntity.setBrightnessOverride(null);
    }

    public org.bukkit.util.Transformation getTransformation() {
        return transformation;
    }

    public Matrix4f getTransformationMatrix() {
        return new Matrix4f()
            .translation(transformation.getTranslation())
            .rotate(transformation.getLeftRotation())
            .scale(transformation.getScale())
            .rotate(transformation.getRightRotation());
    }

    public Billboard getBillboard() {
        return billboard;
    }

    public int getInterpolationDuration() {
        return interpolationDuration;
    }

    public int getInterpolationDelay() {
        return interpolationDelay;
    }

    public int getTeleportDuration() {
        return teleportDuration;
    }

    public float getViewRange() {
        return viewRange;
    }

    public float getShadowRadius() {
        return shadowRadius;
    }

    public float getShadowStrength() {
        return shadowStrength;
    }

    public int getGlowColorOverride() {
        return glowColorOverride;
    }

    public int getBlockLight() {
        return blockLight;
    }

    public int getSkyLight() {
        return skyLight;
    }
}