package com.github.darksoulq.visage.entity;

public enum EntityEvent {
    HURT((byte)2),
    DEATH((byte)3),
    ATTACK_ANIMATION((byte)4),
    TAMING_FAILED((byte)6),
    TAMING_SUCCESS((byte)7),
    SHAKE_WATER((byte)8),
    EATING_ACCEPTED((byte)9),
    SHEEP_EATING((byte)10),
    IRON_GOLEM_ROSE((byte)11),
    VILLAGER_MATE((byte)12),
    VILLAGER_ANGRY((byte)13),
    VILLAGER_HAPPY((byte)14),
    WITCH_SPELL((byte)15),
    ZOMBIE_CONVERT((byte)16),
    FIREWORK_EXPLODE((byte)17),
    LOVE_HEARTS((byte)18),
    SQUID_INK((byte)19),
    SPAWN_PARTICLES((byte)20),
    GUARDIAN_ATTACK((byte)21),
    REDUCED_DEBUG_INFO((byte)22),
    REDUCE_DEBUG_INFO_FALSE((byte)23),
    SHIELD_BLOCK((byte)29),
    SHIELD_BREAK((byte)30),
    FISHING_HOOK_BITE((byte)31),
    TOTEM_USE((byte)35),
    DROWN((byte)36),
    BURN((byte)37),
    DOLPHIN_HAPPY((byte)38),
    RAVAGER_STUNNED((byte)39),
    RAVAGER_ROAR((byte)40),
    VILLAGER_SWEAT((byte)42),
    HONEY_SLIDE((byte)53),
    HONEY_JUMP((byte)54),
    SWAP_HANDS((byte)55),
    WOLF_SHAKE((byte)56),
    IRON_GOLEM_REPAIR((byte)61);

    private final byte id;

    EntityEvent(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }
}