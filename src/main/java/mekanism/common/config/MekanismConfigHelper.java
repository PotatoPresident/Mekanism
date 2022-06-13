package mekanism.common.config;

import mekanism.common.Mekanism;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraftforge.api.ModLoadingContext;

import java.nio.file.Path;

public class MekanismConfigHelper {

    private MekanismConfigHelper() {
    }

    public static final Path CONFIG_DIR;

    static {
        CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(Mekanism.MOD_NAME);
    }

    /**
     * Creates a mod config so that {@link net.minecraftforge.fml.config.ConfigTracker} will track it and sync server configs from server to client.
     */
    public static void registerConfig(ModContainer modContainer, IMekanismConfig config) {
        MekanismModConfig modConfig = new MekanismModConfig(modContainer, config);
        if (config.addToContainer()) {
            ModLoadingContext.registerConfig(modContainer.getMetadata().getId(), config.getConfigType(), modConfig.getSpec());
        }
    }
}