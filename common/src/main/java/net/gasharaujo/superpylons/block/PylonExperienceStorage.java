package net.gasharaujo.superpylons.block;

/** Capability for block automation that stores Minecraft XP points. */
public interface PylonExperienceStorage {
    long getExperience();
    long getExperienceCapacity();
    long receiveExperience(long points, boolean simulate);
    long extractExperience(long points, boolean simulate);
}
