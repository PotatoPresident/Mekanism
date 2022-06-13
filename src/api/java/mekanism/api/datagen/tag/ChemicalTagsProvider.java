package mekanism.api.datagen.tag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.slurry.Slurry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * Helper classes for implementing tag providers for various chemical types.
 */
public abstract class ChemicalTagsProvider<CHEMICAL extends Chemical<CHEMICAL>> extends FabricTagProvider<CHEMICAL> {

    private final String baseName;

    protected ChemicalTagsProvider(FabricDataGenerator gen, Registry<CHEMICAL> registry, String path, String baseName) {
        super(gen, registry, path, baseName);
        this.baseName = baseName;
    }

    @Nonnull
    @Override
    public String getName() {
        return baseName + " Tags: " + getFabricDataGenerator().getModId();
    }

    public abstract static class GasTagsProvider extends ChemicalTagsProvider<Gas> {

        protected GasTagsProvider(FabricDataGenerator gen) {
            super(gen, MekanismAPI.gasRegistry(), "gas", "Gas");
        }
    }

    public abstract static class InfuseTypeTagsProvider extends ChemicalTagsProvider<InfuseType> {

        protected InfuseTypeTagsProvider(FabricDataGenerator gen) {
            super(gen, MekanismAPI.infuseTypeRegistry(),"infuse_type", "Infuse Type");
        }
    }

    public abstract static class PigmentTagsProvider extends ChemicalTagsProvider<Pigment> {

        protected PigmentTagsProvider(FabricDataGenerator gen) {
            super(gen, MekanismAPI.pigmentRegistry(), "pigmet", "Pigment");
        }
    }

    public abstract static class SlurryTagsProvider extends ChemicalTagsProvider<Slurry> {

        protected SlurryTagsProvider(FabricDataGenerator gen) {
            super(gen, MekanismAPI.slurryRegistry(), "slurry", "Slurry");
        }
    }
}