package com.github.darksoulq.visage.entity;

import com.github.darksoulq.visage.culling.Cullable;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import org.bukkit.Location;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public interface EntityGroupable extends Cullable {
    void teleport(Location location);
    void flush();
    void collectPackets(List<Packet<? super ClientGamePacketListener>> packets);
    void applyGroupTransform(Location coreLocation, Vector3f localTranslation, Quaternionf localRotation, Quaternionf groupRotation);
}