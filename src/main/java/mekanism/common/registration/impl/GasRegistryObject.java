package mekanism.common.registration.impl;

import javax.annotation.Nonnull;

import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.providers.IGasProvider;
import mekanism.common.registration.WrappedRegistryObject;

public class GasRegistryObject<GAS extends Gas> extends WrappedRegistryObject<GAS> implements IGasProvider {

    public GasRegistryObject(RegistryObject<GAS> registryObject) {
        super(registryObject);
    }

    @Nonnull
    @Override
    public GAS getChemical() {
        return get();
    }
}