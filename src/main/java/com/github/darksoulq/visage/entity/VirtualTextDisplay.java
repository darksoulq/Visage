package com.github.darksoulq.visage.entity;

import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.EntityTypes;
import org.bukkit.Color;
import org.bukkit.Location;

import java.lang.reflect.Method;

public class VirtualTextDisplay extends VirtualDisplay<TextDisplay> {

    private static Method SET_BACKGROUND_COLOR_METHOD;

    static {
        try {
            SET_BACKGROUND_COLOR_METHOD = TextDisplay.class.getDeclaredMethod("setBackgroundColor", int.class);
            SET_BACKGROUND_COLOR_METHOD.setAccessible(true);
        } catch (Exception ignored) {}
    }

    private net.kyori.adventure.text.Component text = net.kyori.adventure.text.Component.empty();
    private int backgroundColor = 1073741824;
    private int lineWidth = 200;
    private byte textOpacity = -1;
    private boolean shadowed = false;
    private boolean seeThrough = false;
    private boolean defaultBackground = true;
    private TextAlignment alignment = TextAlignment.CENTER;

    public VirtualTextDisplay(Location location) {
        super(location);
    }

    @Override
    protected TextDisplay createNmsEntity(ServerLevel level, Location location) {
        TextDisplay display = new TextDisplay(EntityTypes.TEXT_DISPLAY, level);
        display.setPos(location.getX(), location.getY(), location.getZ());
        display.setYRot(location.getYaw());
        display.setXRot(location.getPitch());
        display.setFlags(TextDisplay.FLAG_USE_DEFAULT_BACKGROUND);
        return display;
    }

    public void setText(net.kyori.adventure.text.Component component) {
        this.text = component;
        nmsEntity.setText(PaperAdventure.asVanilla(component));
    }

    public void setBackgroundColor(Color color) {
        setBackgroundColor(color.asARGB());
    }

    public void setBackgroundColor(int argb) {
        this.backgroundColor = argb;
        nmsEntity.getEntityData().set(TextDisplay.DATA_BACKGROUND_COLOR_ID, argb);
        if (SET_BACKGROUND_COLOR_METHOD != null) {
            try {
                SET_BACKGROUND_COLOR_METHOD.invoke(nmsEntity, argb);
            } catch (Exception ignored) {}
        }
    }

    public void setLineWidth(int width) {
        this.lineWidth = width;
        nmsEntity.getEntityData().set(TextDisplay.DATA_LINE_WIDTH_ID, width);
    }

    public void setTextOpacity(byte opacity) {
        this.textOpacity = opacity;
        nmsEntity.setTextOpacity(opacity);
    }

    public void setShadowed(boolean shadowed) {
        this.shadowed = shadowed;
        updateFlag(TextDisplay.FLAG_SHADOW, shadowed);
    }

    public void setSeeThrough(boolean seeThrough) {
        this.seeThrough = seeThrough;
        updateFlag(TextDisplay.FLAG_SEE_THROUGH, seeThrough);
    }

    public void setDefaultBackground(boolean defaultBackground) {
        this.defaultBackground = defaultBackground;
        updateFlag(TextDisplay.FLAG_USE_DEFAULT_BACKGROUND, defaultBackground);
    }

    public void setAlignment(TextAlignment alignment) {
        this.alignment = alignment;
        byte flags = nmsEntity.getFlags();
        flags &= ~TextDisplay.FLAG_ALIGN_LEFT;
        flags &= ~TextDisplay.FLAG_ALIGN_RIGHT;

        if (alignment == TextAlignment.LEFT) {
            flags |= TextDisplay.FLAG_ALIGN_LEFT;
        } else if (alignment == TextAlignment.RIGHT) {
            flags |= TextDisplay.FLAG_ALIGN_RIGHT;
        }

        nmsEntity.setFlags(flags);
    }

    private void updateFlag(byte flagMask, boolean state) {
        byte flags = nmsEntity.getFlags();
        if (state) {
            flags |= flagMask;
        } else {
            flags &= ~flagMask;
        }
        nmsEntity.setFlags(flags);
    }

    public net.kyori.adventure.text.Component getText() {
        return text;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getLineWidth() {
        return lineWidth;
    }

    public byte getTextOpacity() {
        return textOpacity;
    }

    public boolean isShadowed() {
        return shadowed;
    }

    public boolean isSeeThrough() {
        return seeThrough;
    }

    public boolean isDefaultBackground() {
        return defaultBackground;
    }

    public TextAlignment getAlignment() {
        return alignment;
    }
}