package com.zaixiayesheng.superstack;

import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * One click crafts everything the grid and the inventory can pay for.
 *
 * <p>Vanilla loops quickMoveStack once per crafted item, which turns a 32768 stack into tens of
 * thousands of iterations inside a single tick. This path counts the payable crafts, consumes the
 * ingredients once and inserts the output once, so the cost no longer depends on the stack size.
 */
public final class SuperStackBulkCraft {

    private SuperStackBulkCraft() {
    }

    /**
     * Tries to finish the whole batch behind the result slot in one go.
     *
     * @return true when the batch was handled (the caller should stop the vanilla loop)
     */
    public static boolean tryCraftAll(AbstractContainerMenu menu, CraftingContainer craftSlots,
                                      ResultContainer resultSlots, Player player, int inventoryStart, int inventoryEnd) {
        ItemStack result = resultSlots.getItem(0);
        if (result.isEmpty() || result.getCount() <= 0) {
            return false;
        }

        NonNullList<ItemStack> remainingItems = NonNullList.withSize(9, ItemStack.EMPTY);
        Optional<CraftingRecipe> found =
                player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftSlots, player.level());
        if (found.isPresent()) {
            NonNullList<ItemStack> computed = found.get().getRemainingItems(craftSlots);
            for (int i = 0; i < Math.min(computed.size(), remainingItems.size()); i++) {
                remainingItems.set(i, computed.get(i));
            }
        }

        int limitedByInput = Integer.MAX_VALUE;
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (!stack.isEmpty()) {
                limitedByInput = Math.min(limitedByInput, stack.getCount());
            }
        }
        if (limitedByInput == Integer.MAX_VALUE) {
            return false;
        }

        int perCraft = result.getCount();
        long freeSpace = countFreeSpace(menu, result, inventoryStart, inventoryEnd);
        int crafts = BulkCraftMath.capBySpace(limitedByInput, freeSpace, perCraft);
        if (crafts <= 0) {
            return false;
        }

        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            if (!craftSlots.getItem(i).isEmpty()) {
                craftSlots.removeItem(i, crafts);
            }
        }

        for (int i = 0; i < remainingItems.size() && i < craftSlots.getContainerSize(); i++) {
            ItemStack rem = remainingItems.get(i);
            if (rem.isEmpty()) {
                continue;
            }
            ItemStack gave = rem.copyWithCount(rem.getCount() * crafts);
            ItemStack current = craftSlots.getItem(i);
            if (current.isEmpty()) {
                craftSlots.setItem(i, gave);
            } else if (ItemStack.isSameItemSameTags(current, gave)) {
                gave.grow(current.getCount());
                craftSlots.setItem(i, gave);
            } else if (!player.getInventory().add(gave)) {
                player.drop(gave, false);
            }
        }

        long remainingOutput = (long) crafts * perCraft;
        while (remainingOutput > 0) {
            int chunk = BulkCraftMath.nextChunk(remainingOutput, result.getMaxStackSize());
            int leftover = insert(menu, result, chunk, inventoryStart, inventoryEnd);
            if (leftover > 0) {
                ItemStack drop = result.copyWithCount(leftover);
                if (!player.getInventory().add(drop)) {
                    player.drop(drop, false);
                }
            }
            remainingOutput -= chunk;
        }

        result.onCraftedBy(player.level(), player, crafts * perCraft);
        player.awardStat(Stats.ITEM_CRAFTED.get(result.getItem()), crafts);
        ForgeEventFactory.firePlayerCraftingEvent(player, result, craftSlots);

        menu.slotsChanged(craftSlots);
        return true;
    }

    private static long countFreeSpace(AbstractContainerMenu menu, ItemStack result, int start, int end) {
        long space = 0L;
        for (int i = start; i < end; i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            int limit = slot.getMaxStackSize(result);
            if (stack.isEmpty()) {
                space += limit;
            } else if (ItemStack.isSameItemSameTags(stack, result)) {
                space += Math.max(0, limit - stack.getCount());
            }
        }
        return space;
    }

    private static int insert(AbstractContainerMenu menu, ItemStack result, int count, int start, int end) {
        int remaining = count;
        for (int i = start; i < end && remaining > 0; i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            int limit = slot.getMaxStackSize(result);
            if (stack.isEmpty()) {
                int amount = Math.min(remaining, limit);
                slot.set(result.copyWithCount(amount));
                slot.setChanged();
                remaining -= amount;
            } else if (ItemStack.isSameItemSameTags(stack, result)) {
                int amount = Math.min(remaining, limit - stack.getCount());
                if (amount > 0) {
                    stack.grow(amount);
                    slot.setChanged();
                    remaining -= amount;
                }
            }
        }
        return remaining;
    }
}