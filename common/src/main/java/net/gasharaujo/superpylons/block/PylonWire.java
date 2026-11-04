package net.gasharaujo.superpylons.block;

import java.util.UUID;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public record PylonWire(Type type, BlockPos target, UUID targetId) {
    public static final int MAX_LENGTH = 32;

    public enum Type {
        ENERGY("energy_wire", 0xFFFFAA22),
        ITEM("item_wire", 0xFFB5D788),
        FLUID("fluid_wire", 0xFF48ABF5),
        XP("xp_wire", 0xFF85EF3C);

        private final String id;
        private final int color;

        Type(String id, int color) {
            this.id = id;
            this.color = color;
        }

        public String id() { return id; }
        public int color() { return color; }

        ItemStack itemStack() {
            return new ItemStack(switch (this) {
                case ENERGY -> SuperPylonsMod.ENERGY_WIRE.get();
                case ITEM -> SuperPylonsMod.ITEM_WIRE.get();
                case FLUID -> SuperPylonsMod.FLUID_WIRE.get();
                case XP -> SuperPylonsMod.XP_WIRE.get();
            });
        }
    }

    String encode() { return type.name() + ":" + target.asLong() + ":" + targetId; }

    static PylonWire decode(String text) {
        String[] fields = text.split(":");
        if (fields.length != 3) return null;
        try {
            return new PylonWire(Type.valueOf(fields[0]), BlockPos.of(Long.parseLong(fields[1])), UUID.fromString(fields[2]));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
