package mekanism.api.recipes.ingredients;

import mekanism.api.annotations.NonNull;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidStack;

/**
 * Base implementation for how Mekanism handle's FluidStack Ingredients.
 *
 * Create instances of this using {@link mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess#fluid()}.
 */
public abstract class FluidStackIngredient implements InputIngredient<@NonNull FluidStack> {
}