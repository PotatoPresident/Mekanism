package mekanism.common.registration.impl;

import java.util.function.UnaryOperator;

import io.github.fabricators_of_create.porting_lib.util.NonNullSupplier;
import mekanism.api.MekanismAPI;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.ModuleData;
import mekanism.api.gear.ModuleData.ModuleDataBuilder;
import mekanism.api.providers.IItemProvider;
import mekanism.common.registration.WrappedDeferredRegister;

public class ModuleDeferredRegister extends WrappedDeferredRegister<ModuleData<?>> {

    public ModuleDeferredRegister(String modid) {
        super(modid, MekanismAPI.moduleRegistry());
    }

    public ModuleRegistryObject<?> registerMarker(String name, IItemProvider itemProvider, UnaryOperator<ModuleDataBuilder<?>> builderModifier) {
        return register(name, builderModifier.apply(ModuleDataBuilder.marker(itemProvider)));
    }

    public <MODULE extends ICustomModule<MODULE>> ModuleRegistryObject<MODULE> register(String name, NonNullSupplier<MODULE> supplier, IItemProvider itemProvider,
                                                                                        UnaryOperator<ModuleDataBuilder<MODULE>> builderModifier) {
        return register(name, builderModifier.apply(ModuleDataBuilder.custom(supplier, itemProvider)));
    }

    public <MODULE extends ICustomModule<MODULE>> ModuleRegistryObject<MODULE> register(String name, ModuleDataBuilder<MODULE> builder) {
        return register(name, () -> new ModuleData<>(builder), ModuleRegistryObject::new);
    }
}