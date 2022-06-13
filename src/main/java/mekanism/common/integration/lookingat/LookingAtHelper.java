package mekanism.common.integration.lookingat;

import mekanism.api.chemical.ChemicalStack;
import mekanism.api.math.FloatingLong;
import net.minecraft.network.chat.Component;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidStack;

public interface LookingAtHelper {

    void addText(Component text);

    void addEnergyElement(FloatingLong energy, FloatingLong maxEnergy);

    void addFluidElement(FluidStack stored, int capacity);

    void addChemicalElement(ChemicalStack<?> stored, long capacity);
}