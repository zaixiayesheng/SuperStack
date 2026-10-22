package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.ItemStackGroup;
import com.zaixiayesheng.superstack.OverflowGuard;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sophisticated Core compacts every matching stack into a single int total while sorting. With int-sized
 * stacks that total overflows and the sorter wipes the slots that held them.
 *
 * <p>Older builds cancelled sorting for the whole container as soon as any item's total did not fit into an
 * int, which made large containers completely unsortable. This version keeps the guard for the normal case
 * and, when an overflow is present, performs the sort itself with long arithmetic: merge by item identity,
 * then redistribute across the sortable slots up to each slot limit. If the sortable slots cannot hold the
 * total (slot limits smaller than the stack counts), it falls back to not sorting at all instead of deleting.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.util.InventorySorter", remap = false)
public abstract class SophisticatedInventorySorterMixin {
    private static final String SORT_HANDLER = "sortHandler(Lnet/neoforged/neoforge/items/IItemHandlerModifiable;Ljava/util/Comparator;Ljava/util/Set;Ljava/util/Set;Ljava/util/function/BiPredicate;)V";

    @Inject(method = SORT_HANDLER, at = @At("HEAD"), cancellable = true, remap = false)
    private static void superstack$sortWithoutOverflow(IItemHandlerModifiable handler, Comparator<?> comparator,
            Set<Integer> noSortSlots, Set<Integer> memorizedSlots, BiPredicate<Integer, ItemStack> matchesMemorizedSlot,
            CallbackInfo ci) {
        List<Integer> sortableSlots = new ArrayList<>();
        List<ItemStack> occupied = new ArrayList<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (noSortSlots.contains(slot)) {
                continue;
            }
            sortableSlots.add(slot);
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                occupied.add(stack.copy());
            }
        }
        if (occupied.isEmpty()) {
            return;
        }

        List<ItemStackGroup> groups = new ArrayList<>();
        for (ItemStack stack : occupied) {
            groups.add(new ItemStackGroup(stack));
        }
        if (!OverflowGuard.totalsExceedInt(groups)) {
            return;
        }

        // The mod's int-based compaction would overflow. Do the sort here instead, but only if the sortable
        // slots can actually hold everything; otherwise leave the container untouched.
        long capacity = 0L;
        for (int slot : sortableSlots) {
            capacity += Math.max(0, Math.min(handler.getSlotLimit(slot), (long) occupied.get(0).getMaxStackSize()));
            if (capacity < 0L) {
                capacity = Long.MAX_VALUE / 2;
            }
        }
        long total = 0L;
        for (ItemStack stack : occupied) {
            total += stack.getCount();
        }
        if (total > capacity) {
            ci.cancel();
            return;
        }

        @SuppressWarnings("unchecked")
        Comparator<ItemStack> order = (Comparator<ItemStack>) comparator;
        occupied.sort(order);

        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack stack : occupied) {
            if (!merged.isEmpty() && ItemStack.isSameItemSameComponents(merged.get(merged.size() - 1), stack)) {
                ItemStack last = merged.get(merged.size() - 1);
                long sum = (long) last.getCount() + stack.getCount();
                last.setCount((int) Math.min(sum, Integer.MAX_VALUE));
                if (sum > Integer.MAX_VALUE) {
                    ItemStack extra = stack.copy();
                    extra.setCount((int) (sum - Integer.MAX_VALUE));
                    merged.add(extra);
                }
            } else {
                merged.add(stack);
            }
        }

        int slotIndex = 0;
        for (ItemStack stack : merged) {
            long remaining = stack.getCount();
            while (remaining > 0L && slotIndex < sortableSlots.size()) {
                int slot = sortableSlots.get(slotIndex);
                long limit = Math.min(handler.getSlotLimit(slot), stack.getMaxStackSize());
                long put = Math.min(remaining, Math.max(0L, limit));
                if (put <= 0L) {
                    slotIndex++;
                    continue;
                }
                ItemStack out = stack.copy();
                out.setCount((int) put);
                handler.setStackInSlot(slot, out);
                remaining -= put;
                slotIndex++;
            }
        }
        for (; slotIndex < sortableSlots.size(); slotIndex++) {
            handler.setStackInSlot(sortableSlots.get(slotIndex), ItemStack.EMPTY);
        }
        ci.cancel();
    }
}
