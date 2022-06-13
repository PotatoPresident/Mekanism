package mekanism.common.registration.impl;

import javax.annotation.Nonnull;

import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.ModuleData;
import mekanism.api.providers.IModuleDataProvider;
import mekanism.common.registration.WrappedRegistryObject;

public class ModuleRegistryObject<MODULE extends ICustomModule<MODULE>> extends WrappedRegistryObject<ModuleData<MODULE>> implements IModuleDataProvider<MODULE> {

    public ModuleRegistryObject(RegistryObject<ModuleData<MODULE>> registryObject) {
        super(registryObject);
    }

    @Nonnull
    @Override
    public ModuleData<MODULE> getModuleData() {
        return get();
    }
}