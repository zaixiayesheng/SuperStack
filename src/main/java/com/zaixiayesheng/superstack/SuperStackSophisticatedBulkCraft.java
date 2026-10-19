package com.zaixiayesheng.superstack;

import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Bulk crafting for the crafting upgrade of Sophisticated Core containers.
 *
 * <p>The upgrade normally crafts one result per loop iteration: it re-checks the recipe, pulls one
 * item per grid slot (optionally from storage), consumes it and inserts one result. With stacks of
 * tens of thousands that costs tens of thousands of iterations in a single server tick. This helper
 * performs the exact same net effect (same inputs consumed, same outputs inserted, same stats) in a
 * single pass, so the click completes immediately.
 *
 * <p>Everything here goes through reflection because SuperStack has no compile time dependency on
 * Sophisticated Core; when a method is missing the helper simply answers {@code false} and the
 * original crafting loop keeps working unchanged.
 */
public final class SuperStackSophisticatedBulkCraft {
    private SuperStackSophisticatedBulkCraft() {
    }

    private static final String CRAFTING_CONTAINER_PACKAGE = "net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.";

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("superstack");

    private static void dbg(String message) {
        LOG.info("[ss-bulk] " + message);
    }

    /** Returns true when the click was fully handled and the original loop must not run. */
    public static boolean tryCraftAll(Object menu, Slot resultSlot, Player player) {
        long started = System.nanoTime();
        boolean handled;
        try {
            handled = doCraftAll(menu, resultSlot, player);
        } catch (Throwable error) {
            dbg("error " + error);
            handled = false;
        }
        dbg("done handled=" + handled + " ms=" + ((System.nanoTime() - started) / 1_000_000.0));
        return handled;
    }

    private static boolean doCraftAll(Object menu, Slot resultSlot, Player player) {
        long started = System.nanoTime();
        try {
            Object container = findCraftingContainer(menu, resultSlot);
            dbg("entry slot=" + resultSlot.getClass().getSimpleName() + " container="
                + (container == null ? "none" : container.getClass().getSimpleName()));
            if (container == null) {
                return false;
            }
            Object matrixObject = invoke(container, "getCraftMatrix");
            if (!(matrixObject instanceof CraftingContainer matrix)) {
                return false;
            }
            boolean refill = shouldRefillCraftingGrid(container);
            IItemHandler storage = null;
            try {
                storage = getStorageHandler(menu);
            } catch (Throwable ignored) {
                // storage is optional; the player inventory is always available
            }

            ItemStack result = resultSlot.getItem();
            if (result.isEmpty()) {
                return false;
            }
            int perCraft = result.getCount();
            if (perCraft <= 0) {
                return false;
            }

            CraftingInput input = matrix.asCraftInput();
            Optional<RecipeHolder<CraftingRecipe>> found =
                player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, player.level());
            if (found.isEmpty()) {
                return false;
            }
            NonNullList<ItemStack> remaining = found.get().value().getRemainingItems(input);
            for (ItemStack stack : remaining) {
                if (!stack.isEmpty()) {
                    return false; // container recipes keep the vanilla/upgrade loop
                }
            }

            int crafts = Integer.MAX_VALUE;
            for (int i = 0; i < matrix.getContainerSize(); i++) {
                ItemStack in = matrix.getItem(i);
                if (in.isEmpty()) {
                    continue;
                }
                long available = refill
                    ? in.getCount() + countInHandler(storage, in) + countInInventory(player, in)
                    : in.getCount();
                crafts = (int) Math.min(crafts, available);
            }
            if (crafts == Integer.MAX_VALUE || crafts <= 0) {
                return false;
            }

            long space = countSpace(storage, result) + countSpace(player, result);
            long wanted = (long) crafts * perCraft;
            if (wanted > space) {
                crafts = (int) (space / perCraft);
            }
            if (crafts <= 0) {
                return false;
            }
            long produced = (long) crafts * perCraft;
            dbg("start crafts=" + crafts + " perCraft=" + perCraft + " produced=" + produced
                + " refill=" + refill + " storage=" + (storage != null) + " preMs="
                + ((System.nanoTime() - started) / 1_000_000.0));

            // consume the inputs: grid first, then storage, then the player inventory
            for (int i = 0; i < matrix.getContainerSize(); i++) {
                ItemStack in = matrix.getItem(i);
                if (in.isEmpty()) {
                    continue;
                }
                ItemStack unit = in.copyWithCount(1);
                int need = crafts;
                int fromGrid = Math.min(in.getCount(), need);
                matrix.removeItem(i, fromGrid);
                need -= fromGrid;
                if (need > 0 && refill) {
                    need -= extractFromHandler(storage, unit, need);
                    if (need > 0) {
                        extractFromInventory(player, unit, need);
                    }
                }
            }

            if (refill) {
                refillGrid(player, matrix, storage);
            }

            ItemStack output = result.copyWithCount((int) produced);
            boolean intoStorageFirst = shouldShiftClickIntoStorage(container);
            insertOutput(player, storage, output, intoStorageFirst);

            player.awardStat(net.minecraft.stats.Stats.ITEM_CRAFTED.get(result.getItem()), crafts);
            result.getItem().onCraftedBy(result, player.level(), player);
            net.neoforged.neoforge.event.EventHooks.firePlayerCraftingEvent(player, result, matrix);
            matrix.setChanged();
            if (menu instanceof net.minecraft.world.inventory.AbstractContainerMenu containerMenu) {
                containerMenu.slotsChanged(matrix);
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object findCraftingContainer(Object menu, Slot slot) throws Exception {
        Method method = findMethod(menu.getClass(), "getSlotUpgradeContainer", Slot.class);
        if (method == null) {
            return null;
        }
        method.setAccessible(true);
        Object value = method.invoke(menu, slot);
        if (!(value instanceof Optional<?> optional) || optional.isEmpty()) {
            return null;
        }
        Object container = optional.get();
        return container.getClass().getName().startsWith(CRAFTING_CONTAINER_PACKAGE) ? container : null;
    }

    private static boolean shouldRefillCraftingGrid(Object container) {
        try {
            Object value = invoke(container, "shouldRefillCraftingGrid");
            return value instanceof Boolean flag && flag;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean shouldShiftClickIntoStorage(Object container) {
        try {
            Object value = invoke(container, "shouldShiftClickIntoStorage");
            return value instanceof Boolean flag && flag;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static IItemHandler getStorageHandler(Object menu) throws Exception {
        Method wrapperGetter = findMethod(menu.getClass(), "getStorageWrapper");
        if (wrapperGetter == null) {
            return null;
        }
        wrapperGetter.setAccessible(true);
        Object wrapper = wrapperGetter.invoke(menu);
        if (wrapper == null) {
            return null;
        }
        Method handlerGetter = findMethod(wrapper.getClass(), "getInventoryHandler");
        if (handlerGetter == null) {
            return null;
        }
        handlerGetter.setAccessible(true);
        Object handler = handlerGetter.invoke(wrapper);
        return handler instanceof IItemHandler itemHandler ? itemHandler : null;
    }

    private static Object invoke(Object target, String name) throws Exception {
        Method method = findMethod(target.getClass(), name);
        if (method == null) {
            throw new NoSuchMethodException(name);
        }
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... params) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException ignored) {
                // walk up
            }
        }
        try {
            return type.getMethod(name, params);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static long countInHandler(IItemHandler handler, ItemStack stack) {
        if (handler == null) {
            return 0L;
        }
        long count = 0L;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack slot = handler.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(slot, stack)) {
                count += slot.getCount();
            }
        }
        return count;
    }

    private static long countInInventory(Player player, ItemStack stack) {
        long count = 0L;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameComponents(slot, stack)) {
                count += slot.getCount();
            }
        }
        return count;
    }

    private static long countSpace(IItemHandler handler, ItemStack stack) {
        if (handler == null) {
            return 0L;
        }
        long space = 0L;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack slot = handler.getStackInSlot(i);
            if (slot.isEmpty()) {
                space += stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(slot, stack)) {
                space += Math.max(0, Math.min(slot.getMaxStackSize(), stack.getMaxStackSize()) - slot.getCount());
            }
        }
        return space;
    }

    private static long countSpace(Player player, ItemStack stack) {
        long space = 0L;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot.isEmpty()) {
                space += stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(slot, stack)) {
                space += Math.max(0, Math.min(slot.getMaxStackSize(), stack.getMaxStackSize()) - slot.getCount());
            }
        }
        return space;
    }

    private static int extractFromHandler(IItemHandler handler, ItemStack stack, int amount) {
        if (handler == null) {
            return 0;
        }
        int extracted = 0;
        for (int i = 0; i < handler.getSlots() && extracted < amount; i++) {
            ItemStack slot = handler.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(slot, stack)) {
                ItemStack taken = handler.extractItem(i, amount - extracted, false);
                extracted += taken.getCount();
            }
        }
        return extracted;
    }

    private static int extractFromInventory(Player player, ItemStack stack, int amount) {
        int extracted = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && extracted < amount; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameComponents(slot, stack)) {
                int take = Math.min(amount - extracted, slot.getCount());
                player.getInventory().removeItem(i, take);
                extracted += take;
            }
        }
        return extracted;
    }

    private static void refillGrid(Player player, CraftingContainer matrix, IItemHandler storage) {
        for (int i = 0; i < matrix.getContainerSize(); i++) {
            ItemStack in = matrix.getItem(i);
            if (in.isEmpty()) {
                continue;
            }
            int max = Math.min(in.getMaxStackSize(), matrix.getMaxStackSize());
            int need = max - in.getCount();
            if (need <= 0) {
                continue;
            }
            ItemStack copy = in.copy();
            int fromStorage = extractFromHandler(storage, copy, need);
            need -= fromStorage;
            if (need > 0) {
                need -= extractFromInventory(player, copy, need);
            }
            int added = max - in.getCount() - Math.max(0, need);
            if (added > 0) {
                matrix.setItem(i, copy.copyWithCount(in.getCount() + added));
            }
        }
    }

    private static void insertOutput(Player player, IItemHandler storage, ItemStack output, boolean intoStorageFirst) {
        ItemStack leftover = output;
        if (intoStorageFirst && storage != null) {
            leftover = ItemHandlerHelper.insertItemStacked(storage, leftover, false);
        }
        leftover = insertIntoPlayer(player, leftover);
        if (!leftover.isEmpty() && !intoStorageFirst && storage != null) {
            leftover = ItemHandlerHelper.insertItemStacked(storage, leftover, false);
        }
        if (!leftover.isEmpty()) {
            player.drop(leftover, false);
        }
    }

    private static ItemStack insertIntoPlayer(Player player, ItemStack stack) {
        ItemStack remaining = stack;
        for (int i = 0; i < player.getInventory().getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot.isEmpty()) {
                int take = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                player.getInventory().setItem(i, remaining.copyWithCount(take));
                remaining = remaining.copyWithCount(remaining.getCount() - take);
            } else if (ItemStack.isSameItemSameComponents(slot, remaining)) {
                int space = Math.max(0, Math.min(slot.getMaxStackSize(), remaining.getMaxStackSize()) - slot.getCount());
                int take = Math.min(space, remaining.getCount());
                if (take > 0) {
                    slot.grow(take);
                    remaining = remaining.copyWithCount(remaining.getCount() - take);
                }
            }
        }
        return remaining;
    }
}
