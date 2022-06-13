package mekanism.common;

import mekanism.common.recipe.MekanismRecipeType;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.annotation.Nonnull;

public class ReloadListener implements SimpleSynchronousResourceReloadListener {

    @Override
    public void onResourceManagerReload(@Nonnull ResourceManager resourceManager) {
        CommonWorldTickHandler.flushTagAndRecipeCaches = true;
        MekanismRecipeType.clearCache();
    }

    @Override
    public ResourceLocation getFabricId() {
        return Mekanism.rl("resources");
    }
}