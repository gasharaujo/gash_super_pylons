package net.gasharaujo.superpylons.block;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

final class PylonHub {
    private final PylonBlockEntityBase hub;
    private final List<Link> links = new ArrayList<>();

    private record Link(String dimension, BlockPos pos, UUID id) {}

    PylonHub(PylonBlockEntityBase hub) { this.hub = hub; }

    static ServerLevel findLevel(ServerLevel origin, String dimension) {
        if (origin.dimension().toString().equals(dimension)) return origin;
        for (ServerLevel level : origin.getServer().getAllLevels()) {
            if (level.dimension().toString().equals(dimension)) return level;
        }
        return null;
    }

    private boolean inRange(ServerLevel origin, ServerLevel target, BlockPos pos) {
        int range = hub.hubRange();
        return hub.hasSuperUpgrade() || (origin == target && hub.getBlockPos().distSqr(pos) <= range * range);
    }

    void toggle(PylonBlockEntityBase target, Player player) {
        if (!(hub.getLevel() instanceof ServerLevel origin) || !(target.getLevel() instanceof ServerLevel destination) ||
            target.isHub() || target.isRemoved() || !destination.hasChunkAt(target.getBlockPos())) {
            message(player, "hub_invalid");
            return;
        }
        var link = new Link(destination.dimension().toString(), target.getBlockPos(), target.wireId());
        if (links.remove(link)) {
            message(player, "hub_unlinked");
        } else {
            if (!inRange(origin, destination, link.pos())) {
                message(player, "hub_range");
                return;
            }
            links.removeIf(old -> old.dimension().equals(link.dimension()) && old.pos().equals(link.pos()));
            links.add(link);
            message(player, "hub_linked");
        }
        hub.capabilitiesChanged();
        hub.sync();
    }

    int size() { return hub.isRemoved() ? 0 : links.size(); }

    SuperPylonBlockEntity target(int index) {
        if (index < 0 || index >= size() || !(hub.getLevel() instanceof ServerLevel origin)) return null;
        Link link = links.get(index);
        ServerLevel target = findLevel(origin, link.dimension());
        if (target != null && inRange(origin, target, link.pos()) && target.hasChunkAt(link.pos()) &&
            target.getBlockEntity(link.pos()) instanceof SuperPylonBlockEntity pylon &&
            !pylon.isRemoved() && !pylon.isHub() && pylon.wireId().equals(link.id())) return pylon;
        return null;
    }

    long experience(boolean capacity) {
        long total = 0;
        for (int i = 0; i < size(); i++) {
            var pylon = target(i);
            if (pylon != null) total = sum(total, capacity ? pylon.getExperienceCapacity() : pylon.getExperience());
        }
        return total;
    }

    long moveExperience(long amount, boolean insert, boolean simulate) {
        long moved = 0;
        for (int i = 0; i < size() && moved < amount; i++) {
            var pylon = target(i);
            if (pylon == null || !pylon.isXpTank()) continue;
            moved += insert ? pylon.receiveExperience(amount - moved, simulate) :
                pylon.extractExperience(amount - moved, simulate);
        }
        return moved;
    }

    Component summary() {
        if (!(hub.getLevel() instanceof ServerLevel)) {
            return Component.translatable("tooltip.gash_super_pylons.hub_inspect");
        }
        long items = 0, xp = 0, fluids = 0, energy = 0;
        int loaded = 0;
        for (int i = 0; i < size(); i++) {
            var pylon = target(i);
            if (pylon == null) continue;
            loaded++;
            items = sum(items, pylon.getStoredCount());
            xp = sum(xp, pylon.getExperience());
            fluids = sum(fluids, pylon.getFluidAmount());
            energy = sum(energy, pylon.getEnergy());
        }
        return Component.translatable("message.gash_super_pylons.hub_status", loaded, size(), items, fluids, xp, energy);
    }

    private void message(Player player, String key, Object... args) {
        hub.notifyPlayer(player, Component.translatable("message.gash_super_pylons." + key, args));
    }

    static long sum(long first, long second) {
        return Long.MAX_VALUE - first < second ? Long.MAX_VALUE : first + second;
    }

    String save() {
        var encoded = new StringJoiner(",");
        for (Link link : links) encoded.add(link.dimension() + "|" + link.pos().asLong() + "|" + link.id());
        return encoded.toString();
    }

    void load(String saved) {
        links.clear();
        for (String entry : saved.split(",")) {
            String[] fields = entry.split("\\|", 3);
            if (fields.length != 3) continue;
            try {
                var link = new Link(fields[0], BlockPos.of(Long.parseLong(fields[1])), UUID.fromString(fields[2]));
                if (!links.contains(link)) links.add(link);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed saved links.
            }
        }
    }
}
