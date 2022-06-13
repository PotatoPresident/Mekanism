package mekanism.common.registration.impl;

import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import mekanism.api.text.ILangEntry;
import mekanism.common.registration.WrappedRegistryObject;
import net.minecraft.Util;
import net.minecraft.sounds.SoundEvent;

public class SoundEventRegistryObject<SOUND extends SoundEvent> extends WrappedRegistryObject<SOUND> implements ILangEntry {

    private final String translationKey;

    public SoundEventRegistryObject(RegistryObject<SOUND> registryObject) {
        super(registryObject);
        translationKey = Util.makeDescriptionId("sound_event", this.registryObject.getId());
    }

    @Override
    public String getTranslationKey() {
        return translationKey;
    }
}