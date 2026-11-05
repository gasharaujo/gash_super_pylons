package net.gasharaujo.superpylons.block;

import net.minecraft.server.level.ServerPlayer;

final class PylonExperience {
    private static final int MAX_LEVEL = (Integer.MAX_VALUE - 112) / 9 + 30;

    private PylonExperience() {}

    static long collect(ServerPlayer player, int levels, long space) {
        int oldLevel = player.experienceLevel;
        if (oldLevel <= 0 || oldLevel > MAX_LEVEL) return 0;
        int oldProgress = progressPoints(player);
        int newLevel = Math.max(0, oldLevel - levels);
        int newProgress = Math.min(oldProgress, levelCost(newLevel) - 1);
        long collected = pointsAtLevel(oldLevel) + oldProgress - pointsAtLevel(newLevel) - newProgress;
        if (collected > space) return 0;
        setExperience(player, newLevel, newProgress);
        return collected;
    }

    static long dispense(ServerPlayer player, int levels, long stored) {
        int oldLevel = player.experienceLevel;
        if (oldLevel < 0 || oldLevel >= MAX_LEVEL || stored == 0) return 0;
        long before = pointsAtLevel(oldLevel) + progressPoints(player);
        int maxLevel = Math.min(MAX_LEVEL, oldLevel + levels);
        long spent = Math.min(stored, pointsAtLevel(maxLevel) - before);
        long after = before + spent;
        int level = oldLevel;
        while (level < maxLevel && pointsAtLevel(level + 1) <= after) level++;
        setExperience(player, level, (int) (after - pointsAtLevel(level)));
        return spent;
    }

    private static int progressPoints(ServerPlayer player) {
        int cost = levelCost(player.experienceLevel);
        return (int) Math.max(0, Math.min(cost - 1, Math.round((double) player.experienceProgress * cost)));
    }

    private static int levelCost(int level) {
        if (level >= 30) return 112 + 9 * (level - 30);
        if (level >= 15) return 37 + 5 * (level - 15);
        return 7 + 2 * level;
    }

    static long pointsAtLevel(int level) {
        // Sum the vanilla cost of each preceding level, including both formula changes.
        if (level <= 15) return (long) level * (level + 6);
        if (level <= 30) return 315 + sumCosts(level - 15, 37, 5);
        return 1395 + sumCosts(level - 30, 112, 9);
    }

    private static long sumCosts(long levels, int first, int step) {
        return levels * first + levels * (levels - 1) / 2 * step;
    }

    private static void setExperience(ServerPlayer player, int level, int progress) {
        player.setExperienceLevels(level);
        player.setExperiencePoints(progress);
        player.totalExperience = (int) Math.min(Integer.MAX_VALUE, pointsAtLevel(level) + progress);
    }
}
