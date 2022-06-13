package mekanism.api.providers;

import javax.annotation.Nonnull;

import io.github.fabricators_of_create.porting_lib.extensions.FluidExtensions;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidStack;

public interface IFluidProvider extends IBaseProvider {

    /**
     * Gets the fluid this provider represents.
     */
    @Nonnull
    Fluid getFluid();

    /**
     * Creates a fluid stack of the given size using the fluid this provider represents.
     *
     * @param size Size of the stack.
     */
    @Nonnull
    default FluidStack getFluidStack(int size) {
        return new FluidStack(getFluid(), size);
    }

    @Override
    default ResourceLocation getRegistryName() {
        return Registry.FLUID.getKey(getFluid());
    }

    @Override
    default Component getTextComponent() {
        return getFluidStack(1).getDisplayName();
    }

    @Override
    default String getTranslationKey() {
        return ((FluidExtensions)this.getFluid()).getAttributes().getTranslationKey();
    }
}