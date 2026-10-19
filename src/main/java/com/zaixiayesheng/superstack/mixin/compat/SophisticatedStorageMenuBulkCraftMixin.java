package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * Sophisticated Core crafts one item per iteration of a loop, so with our large
 * stacks a single shift-click runs tens of thousands of recipe lookups in one
 * tick and freezes the server. This mixin replaces that loop with one batch:
 * count the crafts the inputs and the free destination space allow, consume once,
 * insert once.
 *
 * <p>Cases we do not handle (crafting-grid refill enabled, recipes that leave
 * remaining items, reflection failures) fall through to the original loop, which
 * is bounded by {@link SuperStackConfig#maxCraftsPerClick()}.</p>
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase", remap = false)
public class SophisticatedStorageMenuBulkCraftMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void superstack$bulkCraft(int slotId, int dragType, ClickType clickType, Player player, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (clickType != ClickType.QUICK_MOVE || slotId < 0 || slotId >= menu.slots.size()) {
            return;
        }
        Slot slot = menu.getSlot(slotId);
        if (!(slot instanceof ResultSlot) || slot.getItem().isEmpty()) {
            return;
        }

        Object container = getSlotUpgradeContainer(menu, slot);
        if (container == null || !container.getClass().getName().startsWith("net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.")) {
            return;
        }

        try {
            if (Boolean.TRUE.equals(invoke(container, "shouldRefillCraftingGrid"))) {
                return;
            }
            if (!(invoke(container, "getCraftMatrix") instanceof CraftingContainer matrix)) {
                return;
            }

            ItemStack result = slot.getItem().copy();
            int perCraft = result.getCount();
            if (perCraft <= 0 || hasRemainingItems(player, matrix)) {
                return;
            }

            int crafts = Integer.MAX_VALUE;
            for (int i = 0; i < matrix.getContainerSize(); i++) {
                ItemStack stack = matrix.getItem(i);
                if (!stack.isEmpty()) {
                    crafts = Math.min(crafts, stack.getCount());
                }
            }
            if (crafts == Integer.MAX_VALUE) {
                return;
            }

            ItemStack prototype = result.copyWithCount(1);
            IItemHandler storage = getStorageHandler(menu);
            boolean storageFirst = Boolean.TRUE.equals(invoke(container, "mergeIntoStorageFirst", slot));

            crafts = (int) Math.min(crafts, freeSpace(player, storage, prototype, storageFirst) / perCraft);
            if (crafts < 2) {
                return;
            }

            for (int i = 0; i < matrix.getContainerSize(); i++) {
                if (!matrix.getItem(i).isEmpty()) {
                    matrix.removeItem(i, crafts);
                }
            }

            long remaining = (long) crafts * perCraft;
            IItemHandler first = storageFirst ? storage : new PlayerMainInvWrapper(player.getInventory());
            IItemHandler second = storageFirst ? new PlayerMainInvWrapper(player.getInventory()) : storage;
            remaining = insertChunk(first, prototype, remaining);
            remaining = insertChunk(second, prototype, remaining);
            while (remaining > 0) {
                ItemStack drop = prototype.copyWithCount((int) Math.min(remaining, prototype.getMaxStackSize()));
                player.drop(drop, false);
                remaining -= drop.getCount();
            }

            if (!player.level().isClientSide()) {
                player.awardStat(Stats.ITEM_CRAFTED.get(result.getItem()), crafts);
                result.getItem().onCraftedBy(result, player.level(), player);
                EventHooks.firePlayerCraftingEvent(player, result, matrix);
            }

            matrix.setChanged();
            menu.slotsChanged(matrix);
            ci.cancel();
        } catch (Throwable ignored) {
            // Leave the original behaviour in place.
        }
    }

    private static boolean hasRemainingItems(Player player, CraftingContainer matrix) {
        try {
            CraftingInput input = matrix.asCraftInput();
            List<ItemStack> remaining = player.level().getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, input, player.level());
            for (ItemStack stack : remaining) {
                if (!stack.isEmpty()) {
                    return true;
                }
            }
            return false;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static long insertChunk(IItemHandler handler, ItemStack prototype, long remaining) {
        if (handler == null || remaining <= 0) {
            return remaining;
        }
        ItemStack stack = prototype.copyWithCount((int) Math.min(remaining, prototype.getMaxStackSize()));
        ItemStack left = ItemHandlerHelper.insertItemStacked(handler, stack, false);
        return remaining - (stack.getCount() - left.getCount());
    }

    private static long freeSpace(Player player, IItemHandler storage, ItemStack prototype, boolean storageFirst) {
        long space = storageFirst ? freeSpaceIn(storage, prototype) : 0;
        space += freeSpaceIn(new PlayerMainInvWrapper(player.getInventory()), prototype);
        if (!storageFirst) {
            space += freeSpaceIn(storage, prototype);
        }
        return space;
    }

    private static long freeSpaceIn(IItemHandler handler, ItemStack prototype) {
        if (handler == null) {
            return 0;
        }
        long space = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) {
                space += Math.min(handler.getSlotLimit(i), prototype.getMaxStackSize());
            } else if (ItemStack.isSameItemSameComponents(stack, prototype)) {
                space += Math.max(0, Math.min(stack.getMaxStackSize(), handler.getSlotLimit(i)) - stack.getCount());
            }
        }
        return space;
    }

    private static Object getSlotUpgradeContainer(AbstractContainerMenu menu, Slot slot) {
        try {
            if (invoke(menu, "getSlotUpgradeContainer", slot) instanceof Optional<?> optional) {
                return optional.orElse(null);
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return null;
    }

    private static IItemHandler getStorageHandler(AbstractContainerMenu menu) {
        try {
            Object wrapper = invoke(menu, "getStorageWrapper");
            if (wrapper != null && invoke(wrapper, "getInventoryHandler") instanceof IItemHandler handler) {
                return handler;
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return null;
    }

    private static Object invoke(Object target, String name, Object... args) throws Exception {
        Method method = null;
        for (Class<?> c = target.getClass(); c != null && method == null; c = c.getSuperclass()) {
            for (Method candidate : c.getDeclaredMethods()) {
                if (candidate.getName().equals(name) && candidate.getParameterCount() == args.length) {
                    method = candidate;
                    break;
                }
            }
        }
        if (method == null) {
            throw new NoSuchMethodException(name);
        }
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
