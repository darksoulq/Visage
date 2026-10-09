package com.github.darksoulq.visage.block.custom;

import com.github.darksoulq.abyssallib.common.serialization.Codecs;
import com.github.darksoulq.abyssallib.world.block.property.Property;

public class RotationProperty extends Property<Integer> {

    public RotationProperty(int initialValue) {
        super(Codecs.INT, initialValue);
    }

    @Override
    public void set(Integer value) {
        if (value < 0 || value > 15) {
            throw new IllegalArgumentException("Rotation must be between 0 and 15");
        }
        super.set(value);
    }
}