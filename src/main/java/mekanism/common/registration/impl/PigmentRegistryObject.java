package mekanism.common.registration.impl;

import javax.annotation.Nonnull;

import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.providers.IPigmentProvider;
import mekanism.common.registration.WrappedRegistryObject;

public class PigmentRegistryObject<PIGMENT extends Pigment> extends WrappedRegistryObject<PIGMENT> implements IPigmentProvider {

    public PigmentRegistryObject(RegistryObject<PIGMENT> registryObject) {
        super(registryObject);
    }

    @Nonnull
    @Override
    public PIGMENT getChemical() {
        return get();
    }
}