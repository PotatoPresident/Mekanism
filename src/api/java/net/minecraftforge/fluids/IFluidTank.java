/*
 * Minecraft Forge - Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fluids;

import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidStack;
import mekanism.api.Action;

import javax.annotation.Nonnull;

/**
 * This interface represents a Fluid Tank. IT IS NOT REQUIRED but is provided for convenience.
 * You are free to handle Fluids in any way that you wish - this is simply an easy default way.
 * DO NOT ASSUME that these objects are used internally in all cases.
 */
public interface IFluidTank {

    /**
     * @return FluidStack representing the fluid in the tank, null if the tank is empty.
     */
    @Nonnull
    FluidStack getFluid();

    /**
     * @return Current amount of fluid in the tank.
     */
    long getFluidAmount();

    /**
     * @return Capacity of this fluid tank.
     */
    long getCapacity();

    /**
     * @param stack Fluidstack holding the Fluid to be queried.
     * @return If the tank can hold the fluid (EVER, not at the time of query).
     */
    boolean isFluidValid(FluidStack stack);

    /**
     * @param resource FluidStack attempting to fill the tank.
     * @param action   If SIMULATE, the fill will only be simulated.
     * @return Amount of fluid that was accepted (or would be, if simulated) by the tank.
     */
    long fill(FluidStack resource, Action action);

    /**
     * @param maxDrain Maximum amount of fluid to be removed from the container.
     * @param action   If SIMULATE, the drain will only be simulated.
     * @return Amount of fluid that was removed (or would be, if simulated) from the tank.
     */
    @Nonnull
    FluidStack drain(long maxDrain, Action action);

    /**
     * @param resource Maximum amount of fluid to be removed from the container.
     * @param action   If SIMULATE, the drain will only be simulated.
     * @return FluidStack representing fluid that was removed (or would be, if simulated) from the tank.
     */
    @Nonnull
    FluidStack drain(FluidStack resource, Action action);

}