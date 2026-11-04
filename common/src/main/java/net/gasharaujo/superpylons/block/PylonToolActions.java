package net.gasharaujo.superpylons.block;

import java.util.function.Consumer;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public final class PylonToolActions {
    public enum Kind {
        PAINT(null), COPY(null), WRENCH(null), ENERGY(PylonWire.Type.ENERGY), ITEM(PylonWire.Type.ITEM),
        FLUID(PylonWire.Type.FLUID), XP(PylonWire.Type.XP);

        private final PylonWire.Type wire;

        Kind(PylonWire.Type wire) { this.wire = wire; }
        boolean isWire() { return wire != null; }
    }

    private PylonToolActions() {}

    static void use(Kind kind, PylonBlockEntityBase pylon, Player player, ItemStack stack, String saved, Consumer<String> save) {
        if (!(pylon.getLevel() instanceof ServerLevel level)) return;
        switch (kind) {
            case PAINT -> {
                if (player.getOffhandItem().getItem() instanceof BlockItem block) {
                    pylon.paintFrame(block.getBlock());
                } else if (player.isShiftKeyDown()) {
                    save.accept(BuiltInRegistries.BLOCK.getKey(pylon.framePaint()).toString());
                } else {
                    pylon.paintFrame(PylonToolItem.blockById(saved));
                }
            }
            case COPY -> {
                if (player.isShiftKeyDown()) {
                    String setup = pylon.saveUpgrades();
                    save.accept(setup.isEmpty() ? "-" : setup);
                } else if (!saved.isEmpty()) {
                    pylon.applyUpgradeSetup(player, saved);
                }
            }
            case WRENCH -> useWrench(pylon, player, level, saved, save);
            default -> useWire(kind.wire, pylon, player, level, stack, saved, save);
        }
    }

    private static void useWire(PylonWire.Type type, PylonBlockEntityBase pylon, Player player, ServerLevel level,
                                ItemStack stack, String saved, Consumer<String> save) {
        if (player.isShiftKeyDown()) {
            save.accept("");
            return;
        }
        if (saved.isEmpty()) {
            save.accept(level.dimension() + "|" + pylon.getBlockPos().asLong() + "|" + pylon.wireId());
            player.sendSystemMessage(Component.translatable("message.gash_super_pylons.wire_select"));
            return;
        }
        save.accept("");
        String[] fields = saved.split("\\|", 3);
        BlockPos start = wireStart(fields);
        if (start != null && fields[0].equals(level.dimension().toString()) && level.hasChunkAt(start) &&
            level.getBlockEntity(start) instanceof PylonBlockEntityBase source && source.wireId().toString().equals(fields[2])) {
            if (source.connectWire(type, pylon, player)) stack.shrink(1);
        } else {
            player.sendSystemMessage(Component.translatable("message.gash_super_pylons.wire_missing"));
        }
    }

    private static void useWrench(PylonBlockEntityBase pylon, Player player, ServerLevel level,
                                  String saved, Consumer<String> save) {
        if (player.isShiftKeyDown()) {
            if (pylon.isWorkshop()) {
                save.accept(level.dimension() + "|" + pylon.getBlockPos().asLong() + "|" + pylon.wireId());
                player.sendSystemMessage(Component.translatable("message.gash_super_pylons.workshop_select"));
            }
            return;
        }
        String[] fields = saved.split("\\|", 3);
        BlockPos start = wireStart(fields);
        if (start != null && fields[0].equals(level.dimension().toString()) && level.hasChunkAt(start) &&
            level.getBlockEntity(start) instanceof PylonBlockEntityBase source && source.wireId().toString().equals(fields[2]) &&
            source.linkAuxiliary(pylon, player)) save.accept("");
    }

    private static BlockPos wireStart(String[] fields) {
        if (fields.length != 3) return null;
        try {
            return BlockPos.of(Long.parseLong(fields[1]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static void tooltip(Kind kind, String saved, Consumer<Component> lines) {
        if (saved.isEmpty()) return;
        switch (kind) {
            case PAINT -> lines.accept(Component.translatable("tooltip.gash_super_pylons.paint", PylonToolItem.blockById(saved).getName()));
            case COPY -> {
                lines.accept(Component.translatable("tooltip.gash_super_pylons.setup"));
                if (saved.equals("-")) lines.accept(Component.translatable("tooltip.gash_super_pylons.setup_empty"));
                for (String id : saved.split(",")) {
                    PylonUpgrade upgrade = PylonUpgrade.byId(id);
                    if (upgrade != null) lines.accept(new ItemStack(SuperPylonsMod.UPGRADES.get(upgrade).get()).getHoverName());
                }
            }
            default -> {
                BlockPos start = wireStart(saved.split("\\|", 3));
                if (start != null) {
                    lines.accept(Component.translatable("tooltip.gash_super_pylons.wire", start.getX(), start.getY(), start.getZ()));
                }
            }
        }
    }
}
