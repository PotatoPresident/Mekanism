package mekanism.common.tile;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.MekanismAPI;
import mekanism.api.TileNetworkList;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import mekanism.api.gas.IGasItem;
import mekanism.common.Mekanism;
import mekanism.common.MekanismBlock;
import mekanism.common.Upgrade;
import mekanism.common.Upgrade.IUpgradeInfoHandler;
import mekanism.common.base.FluidHandlerWrapper;
import mekanism.common.base.IComparatorSupport;
import mekanism.common.base.IFluidHandlerWrapper;
import mekanism.api.sustained.ISustainedData;
import mekanism.common.base.ITankManager;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.network.PacketTileEntity;
import mekanism.common.tile.prefab.TileEntityMachine;
import mekanism.common.util.ChargeUtils;
import mekanism.common.util.FluidContainerUtils;
import mekanism.common.util.ItemDataUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.PipeUtils;
import mekanism.common.util.TileUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class TileEntityRotaryCondensentrator extends TileEntityMachine implements ISustainedData, IFluidHandlerWrapper, IGasHandler, IUpgradeInfoHandler, ITankManager,
      IComparatorSupport {

    private static final int[] GAS_SLOTS = {0, 1};
    private static final int[] LIQUID_SLOTS = {2, 3};
    private static final int[] ENERGY_SLOT = {4};

    public static final int MAX_FLUID = 10000;
    public GasTank gasTank = new GasTank(MAX_FLUID);
    public FluidTank fluidTank = new FluidTank(MAX_FLUID);
    /**
     * 0: gas -> fluid; 1: fluid -> gas
     */
    public int mode;

    public int gasOutput = 256;

    public double clientEnergyUsed;
    private int currentRedstoneLevel;

    public TileEntityRotaryCondensentrator() {
        super(MekanismBlock.ROTARY_CONDENSENTRATOR, 5);
    }

    @Override
    public void onUpdate() {
        if (!world.isRemote) {
            ChargeUtils.discharge(4, this);

            if (mode == 0) {
                TileUtils.receiveGas(getInventory().get(1), gasTank);
                if (FluidContainerUtils.isFluidContainer(getInventory().get(2))) {
                    FluidContainerUtils.handleContainerItemFill(this, fluidTank, 2, 3);
                }

                //TODO: Promote this stuff to being a proper RECIPE (at the very least in 1.14)
                if (getEnergy() >= getEnergyPerTick() && MekanismUtils.canFunction(this) && isValidGas(gasTank.getGas()) &&
                    (fluidTank.getFluid().isEmpty() || (fluidTank.getFluid().getAmount() < MAX_FLUID && gasEquals(gasTank.getGas(), fluidTank.getFluid())))) {
                    int operations = getUpgradedUsage();
                    double prev = getEnergy();

                    setActive(true);
                    fluidTank.fill(new FluidStack(gasTank.getGas().getGas().getFluid(), operations), FluidAction.EXECUTE);
                    gasTank.draw(operations, true);
                    setEnergy(getEnergy() - getEnergyPerTick() * operations);
                    clientEnergyUsed = prev - getEnergy();
                } else {
                    setActive(false);
                }
            } else if (mode == 1) {
                TileUtils.drawGas(getInventory().get(0), gasTank);
                TileUtils.emitGas(this, gasTank, gasOutput, getLeftSide());

                if (FluidContainerUtils.isFluidContainer(getInventory().get(2))) {
                    FluidContainerUtils.handleContainerItemEmpty(this, fluidTank, 2, 3);
                }

                //TODO: Promote this stuff to being a proper RECIPE (at the very least in 1.14)
                if (getEnergy() >= getEnergyPerTick() && MekanismUtils.canFunction(this) && isValidFluid(fluidTank.getFluid()) &&
                    (gasTank.isEmpty() || (gasTank.getStored() < MAX_FLUID && gasEquals(gasTank.getGas(), fluidTank.getFluid())))) {
                    int operations = getUpgradedUsage();
                    double prev = getEnergy();

                    setActive(true);
                    //TODO: Recipe system instead of this
                    Gas value = MekanismAPI.GAS_REGISTRY.getValue(fluidTank.getFluid().getFluid().getRegistryName());
                    if (value != null) {
                        gasTank.receive(new GasStack(value, operations), true);
                        fluidTank.drain(operations, FluidAction.EXECUTE);
                        setEnergy(getEnergy() - getEnergyPerTick() * operations);
                        clientEnergyUsed = prev - getEnergy();
                    }
                } else {
                    setActive(false);
                }
            }
            int newRedstoneLevel = getRedstoneLevel();
            if (newRedstoneLevel != currentRedstoneLevel) {
                world.updateComparatorOutputLevel(pos, getBlockType());
                currentRedstoneLevel = newRedstoneLevel;
            }
        }
    }

    public int getUpgradedUsage() {
        int possibleProcess = (int) Math.pow(2, upgradeComponent.getUpgrades(Upgrade.SPEED));
        if (mode == 0) { //Gas to fluid
            possibleProcess = Math.min(Math.min(gasTank.getStored(), fluidTank.getCapacity() - fluidTank.getFluidAmount()), possibleProcess);
        } else { //Fluid to gas
            possibleProcess = Math.min(Math.min(fluidTank.getFluidAmount(), gasTank.getNeeded()), possibleProcess);
        }
        possibleProcess = Math.min((int) (getEnergy() / getEnergyPerTick()), possibleProcess);
        return possibleProcess;
    }

    public boolean isValidGas(GasStack g) {
        return g != null && g.getGas().hasFluid();

    }

    public boolean gasEquals(GasStack gas, @Nonnull FluidStack fluid) {
        return !fluid.isEmpty() && gas != null && gas.getGas().hasFluid() && gas.getGas().getFluid() == fluid.getFluid();
    }

    public boolean isValidFluid(@Nonnull Fluid f) {
        return MekanismAPI.GAS_REGISTRY.getValue(f.getRegistryName()) != null;
    }

    public boolean isValidFluid(@Nonnull FluidStack f) {
        return isValidFluid(f.getFluid());
    }

    @Override
    public void handlePacketData(PacketBuffer dataStream) {
        if (!world.isRemote) {
            int type = dataStream.readInt();
            if (type == 0) {
                mode = mode == 0 ? 1 : 0;
            }
            for (PlayerEntity player : playersUsing) {
                Mekanism.packetHandler.sendTo(new PacketTileEntity(this), (ServerPlayerEntity) player);
            }
            return;
        }

        super.handlePacketData(dataStream);
        if (world.isRemote) {
            mode = dataStream.readInt();
            clientEnergyUsed = dataStream.readDouble();
            TileUtils.readTankData(dataStream, fluidTank);
            TileUtils.readTankData(dataStream, gasTank);
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(mode);
        data.add(clientEnergyUsed);
        TileUtils.addTankData(data, fluidTank);
        TileUtils.addTankData(data, gasTank);
        return data;
    }

    @Override
    public void read(CompoundNBT nbtTags) {
        super.read(nbtTags);
        mode = nbtTags.getInt("mode");
        gasTank.read(nbtTags.getCompound("gasTank"));
        if (nbtTags.contains("fluidTank")) {
            fluidTank.readFromNBT(nbtTags.getCompound("fluidTank"));
        }
    }

    @Nonnull
    @Override
    public CompoundNBT write(CompoundNBT nbtTags) {
        super.write(nbtTags);
        nbtTags.putInt("mode", mode);
        nbtTags.put("gasTank", gasTank.write(new CompoundNBT()));
        if (!fluidTank.getFluid().isEmpty()) {
            nbtTags.put("fluidTank", fluidTank.writeToNBT(new CompoundNBT()));
        }
        return nbtTags;
    }

    @Override
    public int receiveGas(Direction side, @Nonnull GasStack stack, boolean doTransfer) {
        return gasTank.receive(stack, doTransfer);
    }

    @Nonnull
    @Override
    public GasStack drawGas(Direction side, int amount, boolean doTransfer) {
        return gasTank.draw(amount, doTransfer);
    }

    @Override
    public boolean canDrawGas(Direction side, @Nonnull Gas type) {
        return mode == 1 && side == getLeftSide() && gasTank.canDraw(type);
    }

    @Override
    public boolean canReceiveGas(Direction side, @Nonnull Gas type) {
        return mode == 0 && side == getLeftSide() && gasTank.canReceive(type);
    }

    @Nonnull
    @Override
    public GasTankInfo[] getTankInfo() {
        return new GasTankInfo[]{gasTank};
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction side) {
        if (isCapabilityDisabled(capability, side)) {
            return LazyOptional.empty();
        }
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.orEmpty(capability, LazyOptional.of(() -> this));
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.orEmpty(capability, LazyOptional.of(() -> new FluidHandlerWrapper(this, side)));
        }
        return super.getCapability(capability, side);
    }

    @Override
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, Direction side) {
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return side != null && side != getLeftSide();
        }
        return super.isCapabilityDisabled(capability, side);
    }

    @Override
    public void writeSustainedData(ItemStack itemStack) {
        if (!fluidTank.getFluid().isEmpty()) {
            ItemDataUtils.setCompound(itemStack, "fluidTank", fluidTank.getFluid().writeToNBT(new CompoundNBT()));
        }
        if (gasTank.getGas() != null) {
            ItemDataUtils.setCompound(itemStack, "gasTank", gasTank.getGas().write(new CompoundNBT()));
        }
    }

    @Override
    public void readSustainedData(ItemStack itemStack) {
        fluidTank.setFluid(FluidStack.loadFluidStackFromNBT(ItemDataUtils.getCompound(itemStack, "fluidTank")));
        gasTank.setGas(GasStack.readFromNBT(ItemDataUtils.getCompound(itemStack, "gasTank")));
    }

    @Override
    public int fill(Direction from, @Nonnull FluidStack resource, FluidAction fluidAction) {
        return fluidTank.fill(resource, fluidAction);
    }

    @Nonnull
    @Override
    public FluidStack drain(Direction from, int maxDrain, FluidAction fluidAction) {
        return fluidTank.drain(maxDrain, fluidAction);
    }

    @Override
    public boolean canFill(Direction from, @Nonnull FluidStack fluid) {
        return mode == 1 && from == getLeftSide() && (fluidTank.getFluid().isEmpty() ? isValidFluid(fluid) : fluidTank.getFluid().isFluidEqual(fluid));
    }

    @Override
    public boolean canDrain(Direction from, @Nonnull FluidStack fluid) {
        return mode == 0 && from == getRightSide() && FluidContainerUtils.canDrain(fluidTank.getFluid(), fluid);
    }

    @Override
    public IFluidTank[] getTankInfo(Direction from) {
        if (from == getRightSide()) {
            return getAllTanks();
        }
        return PipeUtils.EMPTY;
    }

    @Override
    public IFluidTank[] getAllTanks() {
        return new IFluidTank[]{fluidTank};
    }

    @Override
    public List<ITextComponent> getInfo(Upgrade upgrade) {
        return upgrade == Upgrade.SPEED ? upgrade.getExpScaledInfo(this) : upgrade.getMultScaledInfo(this);
    }

    @Override
    public Object[] getTanks() {
        return new Object[]{gasTank, fluidTank};
    }

    @Nonnull
    @Override
    public int[] getSlotsForFace(@Nonnull Direction side) {
        if (side == getLeftSide()) {
            //Gas
            return GAS_SLOTS;
        } else if (side == getRightSide()) {
            //Fluid
            return LIQUID_SLOTS;
        }
        return ENERGY_SLOT;
    }

    @Override
    public boolean isItemValidForSlot(int slot, @Nonnull ItemStack stack) {
        if (slot == 0) {
            //Gas
            return stack.getItem() instanceof IGasItem;
        } else if (slot == 2) {
            //Fluid
            return FluidContainerUtils.isFluidContainer(stack);
        } else if (slot == 4) {
            return ChargeUtils.canBeDischarged(stack);
        }
        return false;
    }

    @Override
    public int getRedstoneLevel() {
        if (mode == 0) {
            return MekanismUtils.redstoneLevelFromContents(gasTank.getStored(), gasTank.getMaxGas());
        }
        return MekanismUtils.redstoneLevelFromContents(fluidTank.getFluidAmount(), fluidTank.getCapacity());
    }
}