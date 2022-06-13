package mekanism.common.registration.impl;

import java.util.function.Supplier;
import mekanism.common.registration.WrappedDeferredRegister;
import net.minecraft.core.Registry;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class IRecipeSerializerDeferredRegister extends WrappedDeferredRegister<RecipeSerializer<?>> {

    public IRecipeSerializerDeferredRegister(String modid) {
        super(modid, Registry.RECIPE_SERIALIZER);
    }

    public <RECIPE extends Recipe<?>> IRecipeSerializerRegistryObject<RECIPE> register(String name, Supplier<RecipeSerializer<RECIPE>> sup) {
        return register(name, sup, IRecipeSerializerRegistryObject::new);
    }
}