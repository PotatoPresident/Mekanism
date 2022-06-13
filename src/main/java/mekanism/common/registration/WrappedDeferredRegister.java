package mekanism.common.registration;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import io.github.fabricators_of_create.porting_lib.util.LazyRegistrar;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import net.minecraft.core.Registry;

public class WrappedDeferredRegister<T> {

    protected final LazyRegistrar<T> internal;

    protected WrappedDeferredRegister(String modid, Registry<T> registry) {
        internal = LazyRegistrar.create(registry, modid);
    }

    protected <I extends T, W extends WrappedRegistryObject<I>> W register(String name, Supplier<I> sup, Function<RegistryObject<I>, W> objectWrapper) {
        return objectWrapper.apply(internal.register(name, sup));
    }

    /**
     * Only call this from mekanism and for custom registries
     */
    public void createAndRegister(IEventBus bus, Class<T> type) {
        createAndRegister(bus, type, UnaryOperator.identity());
    }

    /**
     * Only call this from mekanism and for custom registries
     */
    public void createAndRegister(IEventBus bus, Class<T> type, UnaryOperator<RegistryBuilder<T>> builder) {
        internal.makeRegistry(type, () -> builder.apply(new RegistryBuilder<>()));
        register(bus);
    }

    public void register(IEventBus bus) {
        internal.register(bus);
    }
}