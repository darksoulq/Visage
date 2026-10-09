package com.github.darksoulq.visage.block.custom;

import com.github.darksoulq.abyssallib.common.serialization.Codec;
import com.github.darksoulq.abyssallib.common.serialization.Codecs;
import com.github.darksoulq.abyssallib.world.block.property.Property;
import org.bukkit.block.BlockFace;

import java.util.Arrays;
import java.util.EnumSet;

public class DirectionProperty extends Property<BlockFace> {

    public static final Codec<BlockFace> CODEC = Codecs.STRING.xmap(
        s -> BlockFace.valueOf(s.toUpperCase()),
        BlockFace::name
    );

    public static final EnumSet<BlockFace> HORIZONTAL = EnumSet.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);
    public static final EnumSet<BlockFace> ALL = EnumSet.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);

    private final EnumSet<BlockFace> allowedFaces;

    private DirectionProperty(BlockFace initialValue, EnumSet<BlockFace> allowedFaces) {
        super(CODEC, initialValue);
        this.allowedFaces = allowedFaces;
    }

    public static DirectionProperty horizontal(BlockFace initialValue) {
        return new DirectionProperty(initialValue, HORIZONTAL.clone());
    }

    public static DirectionProperty all(BlockFace initialValue) {
        return new DirectionProperty(initialValue, ALL.clone());
    }

    public static DirectionProperty of(BlockFace initialValue, BlockFace... faces) {
        return new DirectionProperty(initialValue, EnumSet.copyOf(Arrays.asList(faces)));
    }

    public EnumSet<BlockFace> getAllowedFaces() {
        return allowedFaces;
    }

    @Override
    public void set(BlockFace value) {
        if (!allowedFaces.contains(value)) {
            throw new IllegalArgumentException("BlockFace " + value + " is not allowed for this direction property.");
        }
        super.set(value);
    }
}