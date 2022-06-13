package mekanism.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

import io.github.fabricators_of_create.porting_lib.transfer.item.IItemHandler;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.Mekanism;
import mekanism.common.config.MekanismConfig;
import mekanism.common.item.interfaces.IItemSustainedInventory;
import mekanism.common.lib.inventory.TileTransitRequest;
import mekanism.common.lib.security.IOwnerItem;
import mekanism.common.lib.security.ISecurityItem;
import mekanism.common.lib.security.ISecurityObject;
import mekanism.common.lib.security.SecurityMode;
import mekanism.common.recipe.upgrade.ItemRecipeData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Contract;

public final class InventoryUtils {

    private InventoryUtils() {
    }

    /**
     * Helper to drop the contents of an inventory when it is destroyed if it is public or the cause of the destruction has access to the inventory.
     */
    public static void dropItemContents(ItemEntity entity, DamageSource source) {
        ItemStack stack = entity.getItem();
        if (!entity.level.isClientSide && !stack.isEmpty() && stack.getItem() instanceof IItemSustainedInventory sustainedInventory) {
            boolean shouldDrop;
            if (source.getEntity() instanceof Player player) {
                //If the destroyer is a player use security utils to properly check for access
                shouldDrop = SecurityUtils.canAccess(player, stack);
            } else if (!(stack.getItem() instanceof ISecurityItem) && stack.getItem() instanceof IOwnerItem ownerItem) {
                // otherwise, if it is an owner item but not a security item make sure the owner matches,
                // and only allow it if protection is disabled or the item doesn't have an owner yet
                shouldDrop = !MekanismConfig.general.allowProtection.get() || ownerItem.getOwnerUUID(stack) == null;
            } else {
                // finally, if the destroyer wasn't a player and not just an owner item but not a security item,
                // wrap it as a security item and allow dropping if it doesn't have security, or it is public
                ISecurityObject security = SecurityUtils.wrapSecurityItem(stack);
                shouldDrop = !security.hasSecurity() || security.getSecurityMode() == SecurityMode.PUBLIC;
            }
            if (shouldDrop) {
                ListTag storedContents = sustainedInventory.getInventory(stack);
                for (IInventorySlot slot : ItemRecipeData.readContents(storedContents)) {
                    if (!slot.isEmpty()) {
                        ItemStack slotStack = slot.getStack();
                        int count = slotStack.getCount();
                        int max = slotStack.getMaxStackSize();
                        if (count > max) {
                            //If we have more than a stack of the item (such as we are a bin) or some other thing that allows for compressing
                            // stack counts, drop as many stacks as we need at their max size
                            while (count > max) {
                                entity.level.addFreshEntity(new ItemEntity(entity.level, entity.getX(), entity.getY(), entity.getZ(), StackUtils.size(slotStack, max)));
                                count -= max;
                            }
                            if (count > 0) {
                                //If we have anything left to drop afterward, do so
                                entity.level.addFreshEntity(new ItemEntity(entity.level, entity.getX(), entity.getY(), entity.getZ(), StackUtils.size(slotStack, count)));
                            }
                        } else {
                            //If we have a valid stack, we can just directly drop that instead without requiring any copies
                            // as while IInventorySlot#getStack says to not mutate the stack, our slot is a dummy slot
                            entity.level.addFreshEntity(new ItemEntity(entity.level, entity.getX(), entity.getY(), entity.getZ(), slotStack));
                        }
                    }
                }
            }
        }
    }

    /**
     * Like {@link ItemHandlerHelper#canItemStacksStack(ItemStack, ItemStack)} but empty stacks mean equal (either param). Thiakil: not sure why.
     *
     * @param toInsert stack a
     * @param inSlot   stack b
     *
     * @return true if they are compatible
     */
    public static boolean areItemsStackable(ItemStack toInsert, ItemStack inSlot) {
        if (toInsert.isEmpty() || inSlot.isEmpty()) {
            return true;
        }
        return ItemHandlerHelper.canItemStacksStack(inSlot, toInsert);
    }

    @Nullable
    public static IItemHandler assertItemHandler(String desc, BlockEntity tile, Direction side) {
        Optional<IItemHandler> capability = CapabilityUtils.getCapability(tile, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).resolve();
        if (capability.isPresent()) {
            return capability.get();
        }
        Mekanism.logger.warn("'{}' was wrapped around a non-IItemHandler inventory. This should not happen!", desc, new Exception());
        if (tile == null) {
            Mekanism.logger.warn(" - null tile");
        } else {
            Mekanism.logger.warn(" - details: {} {}", tile, tile.getBlockPos());
        }
        return null;
    }

    public static boolean isItemHandler(BlockEntity tile, Direction side) {
        return CapabilityUtils.getCapability(tile, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side).isPresent();
    }

    public static TileTransitRequest getEjectItemMap(BlockEntity tile, Direction side, List<IInventorySlot> slots) {
        return getEjectItemMap(new TileTransitRequest(tile, side), slots);
    }

    @Contract("_, _ -> param1")
    public static <REQUEST extends TileTransitRequest> REQUEST getEjectItemMap(REQUEST request, List<IInventorySlot> slots) {
        // shuffle the order we look at our slots to avoid ejection patterns
        List<IInventorySlot> shuffled = new ArrayList<>(slots);
        Collections.shuffle(shuffled);
        for (IInventorySlot slot : shuffled) {
            //Note: We are using EXTERNAL as that is what we actually end up using when performing the extraction in the end
            ItemStack simulatedExtraction = slot.extractItem(slot.getCount(), Action.SIMULATE, AutomationType.EXTERNAL);
            if (!simulatedExtraction.isEmpty()) {
                request.addItem(simulatedExtraction, slots.indexOf(slot));
            }
        }
        return request;
    }
}