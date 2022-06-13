package mekanism.common.registration;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import mekanism.api.annotations.FieldsAreNonnullByDefault;

@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public class DoubleWrappedRegistryObject<PRIMARY, SECONDARY> implements INamedEntry {

    private final RegistryObject<PRIMARY> primaryRO;
    private final RegistryObject<SECONDARY> secondaryRO;

    public DoubleWrappedRegistryObject(RegistryObject<PRIMARY> primaryRO, RegistryObject<SECONDARY> secondaryRO) {
        this.primaryRO = primaryRO;
        this.secondaryRO = secondaryRO;
    }

    @Nonnull
    public PRIMARY getPrimary() {
        return primaryRO.get();
    }

    @Nonnull
    public SECONDARY getSecondary() {
        return secondaryRO.get();
    }

    @Override
    public String getInternalRegistryName() {
        return primaryRO.getId().getPath();
    }
}