package net.gasharaujo.superpylons;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Empty mod entry point. Gameplay features will be added in later versions. */
@Mod(SuperPylonsMod.MOD_ID)
public final class SuperPylonsMod {
    public static final String MOD_ID = "gash_super_pylons";
    private static final Logger LOGGER = LogUtils.getLogger();

    public SuperPylonsMod() {
        LOGGER.info("Gash's Super Pylons loaded ({})", MOD_ID);
    }
}
