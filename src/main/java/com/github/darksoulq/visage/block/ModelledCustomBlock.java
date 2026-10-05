package com.github.darksoulq.visage.block;

import com.github.darksoulq.abyssallib.server.event.ActionResult;
import com.github.darksoulq.abyssallib.world.block.CustomBlock;
import net.kyori.adventure.key.Key;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.EnumSet;

public abstract class ModelledCustomBlock extends CustomBlock {

    public ModelledCustomBlock(Key id) {
        super(id);
    }

    public ModelledCustomBlock(Key id, Material material) {
        super(id, material);
    }

    @Override
    public void place(Block block, boolean loading) {
        super.place(block, loading);
        if (!loading && getEntity() instanceof ModelledBlockEntity entity) {
            entity.spawnModel();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getEntity() instanceof ModelledBlockEntity entity) {
            entity.spawnModel();
        }
    }

    @Override
    public void onUnLoad() {
        super.onUnLoad();
        if (getEntity() instanceof ModelledBlockEntity entity) {
            entity.destroyModel();
        }
    }

    @Override
    public ActionResult onBreak(Player player, Location loc, ItemStack tool) {
        if (getEntity() instanceof ModelledBlockEntity entity) {
            entity.destroyModel();
        }
        return super.onBreak(player, loc, tool);
    }

    protected BlockFace getTargetFace(Player player) {
        RayTraceResult result = player.rayTraceBlocks(7.0, org.bukkit.FluidCollisionMode.NEVER);
        return (result != null && result.getHitBlockFace() != null) ? result.getHitBlockFace() : BlockFace.UP;
    }

    public boolean setupDirectional(Player player, DirectionProperty property) {
        if (property == null) return false;
        property.set(calculateFace(player, property.getAllowedFaces()));
        return true;
    }

    public boolean setupHorizontal(Player player, DirectionProperty property) {
        if (property == null) return false;
        property.set(calculateHorizontalFace(player, property.getAllowedFaces()));
        return true;
    }

    public boolean setupRotation(Player player, RotationProperty property) {
        if (property == null) return false;
        property.set(calculateRotation(player));
        return true;
    }

    public boolean setupAxis(Player player, AxisProperty property) {
        if (property == null) return false;
        property.set(calculateAxis(getTargetFace(player), property.getAllowedAxes()));
        return true;
    }

    public boolean setupAttachment(Player player, AttachmentProperty property) {
        if (property == null) return false;
        property.set(calculateAttachment(getTargetFace(player), property.getAllowedAttachments()));
        return true;
    }

    public BlockFace calculateHorizontalFace(Player player, EnumSet<BlockFace> allowed) {
        BlockFace facing = player.getFacing().getOppositeFace();
        if (allowed.contains(facing)) return facing;
        for (BlockFace f : DirectionProperty.HORIZONTAL) {
            if (allowed.contains(f)) return f;
        }
        return allowed.isEmpty() ? BlockFace.NORTH : allowed.iterator().next();
    }

    public BlockFace calculateFace(Player player, EnumSet<BlockFace> allowed) {
        BlockFace facing = player.getFacing().getOppositeFace();
        float pitch = player.getPitch();

        if (pitch < -45 && allowed.contains(BlockFace.UP)) {
            return BlockFace.UP;
        } else if (pitch > 45 && allowed.contains(BlockFace.DOWN)) {
            return BlockFace.DOWN;
        }

        if (allowed.contains(facing)) return facing;
        return calculateHorizontalFace(player, allowed);
    }

    public int calculateRotation(Player player) {
        return (Math.round(player.getLocation().getYaw() / 22.5f) + 8) & 15;
    }

    public Axis calculateAxis(BlockFace clickedFace, EnumSet<Axis> allowed) {
        Axis axis = switch (clickedFace) {
            case NORTH, SOUTH -> Axis.Z;
            case EAST, WEST -> Axis.X;
            default -> Axis.Y;
        };
        if (allowed.contains(axis)) return axis;
        return allowed.isEmpty() ? Axis.Y : allowed.iterator().next();
    }

    public Attachment calculateAttachment(BlockFace clickedFace, EnumSet<Attachment> allowed) {
        Attachment attachment = switch (clickedFace) {
            case UP -> Attachment.FLOOR;
            case DOWN -> Attachment.CEILING;
            default -> Attachment.WALL;
        };
        if (allowed.contains(attachment)) return attachment;
        return allowed.isEmpty() ? Attachment.FLOOR : allowed.iterator().next();
    }

    @Override
    public abstract ModelledBlockEntity createBlockEntity(Location loc);
}