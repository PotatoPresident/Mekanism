package mekanism.common.registration;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.fabricators_of_create.porting_lib.util.LazyRegistrar;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import net.minecraft.core.Registry;

public class DoubleDeferredRegister<PRIMARY, SECONDARY> {

    private final LazyRegistrar<PRIMARY> primaryRegister;
    private final LazyRegistrar<SECONDARY> secondaryRegister;

    public DoubleDeferredRegister(String modid, Registry<PRIMARY> primaryRegistry, Registry<SECONDARY> secondaryRegistry) {
        primaryRegister = LazyRegistrar.create(primaryRegistry, modid);
        secondaryRegister = LazyRegistrar.create(secondaryRegistry, modid);
    }

    public <P extends PRIMARY, S extends SECONDARY, W extends DoubleWrappedRegistryObject<P, S>> W register(String name, Supplier<P> primarySupplier,
          Supplier<S> secondarySupplier, BiFunction<RegistryObject<P>, RegistryObject<S>, W> objectWrapper) {
        return objectWrapper.apply(primaryRegister.register(name, primarySupplier), secondaryRegister.register(name, secondarySupplier));
    }

    public <P extends PRIMARY, S extends SECONDARY, W extends DoubleWrappedRegistryObject<P, S>> W register(String name, Supplier<P> primarySupplier,
          Function<P, S> secondarySupplier, BiFunction<RegistryObject<P>, RegistryObject<S>, W> objectWrapper) {
        RegistryObject<P> primaryObject = primaryRegister.register(name, primarySupplier);
        return objectWrapper.apply(primaryObject, secondaryRegister.register(name, () -> secondarySupplier.apply(primaryObject.get())));
    }
}