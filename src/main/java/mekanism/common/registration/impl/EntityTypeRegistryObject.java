package mekanism.common.registration.impl;

import javax.annotation.Nonnull;

import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import mekanism.api.providers.IEntityTypeProvider;
import mekanism.common.registration.WrappedRegistryObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class EntityTypeRegistryObject<ENTITY extends Entity> extends WrappedRegistryObject<EntityType<ENTITY>> implements IEntityTypeProvider {

    public EntityTypeRegistryObject(RegistryObject<EntityType<ENTITY>> registryObject) {
        super(registryObject);
    }

    @Nonnull
    @Override
    public EntityType<ENTITY> getEntityType() {
        return get();
    }
}