package com.github.darksoulq.visage.block;

import com.github.darksoulq.abyssallib.common.serialization.Codecs;
import com.github.darksoulq.abyssallib.world.block.property.Property;
import org.bukkit.Axis;

import java.util.Arrays;
import java.util.EnumSet;

public class AxisProperty extends Property<Axis> {

    public static final EnumSet<Axis> ALL = EnumSet.allOf(Axis.class);

    private final EnumSet<Axis> allowedAxes;

    private AxisProperty(Axis initialValue, EnumSet<Axis> allowedAxes) {
        super(Codecs.STRING.xmap(s -> Axis.valueOf(s.toUpperCase()), Axis::name), initialValue);
        this.allowedAxes = allowedAxes;
    }

    public static AxisProperty all(Axis initialValue) {
        return new AxisProperty(initialValue, ALL.clone());
    }

    public static AxisProperty of(Axis initialValue, Axis... axes) {
        return new AxisProperty(initialValue, EnumSet.copyOf(Arrays.asList(axes)));
    }

    public EnumSet<Axis> getAllowedAxes() {
        return allowedAxes;
    }

    @Override
    public void set(Axis value) {
        if (!allowedAxes.contains(value)) {
            throw new IllegalArgumentException("Axis " + value + " is not allowed for this property.");
        }
        super.set(value);
    }
}