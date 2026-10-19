package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.ItemStackGroup;
import com.zaixiayesheng.superstack.OverflowGuard;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sophisticated Core compacts every matching stack into a single int total while sorting. Two stacks of
 * Integer.MAX_VALUE overflow that total into a negative number, and the sorter then wipes the slots that
 * held them. Skip sorting whenever any item's total does not fit into an int, so nothing is deleted.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.util.InventorySorter", remap = false)
public abstract class SophisticatedInventorySorterMixin {
    private static final String SORT_HANDLER = "sortHandler(Lnet/minecraftforge/items/IItemHandlerModifiable;Ljava/util/Comparator;Ljava/util/Set;Ljava/util/Set;Ljava/util/function/BiPredicate;)V";

    @Inject(method = SORT_HANDLER, at = @At("HEAD"), cancellable = true, remap = false)
    private static void superstack$skipSortOnOverflow(IItemHandlerModifiable handler, Comparator<?> comparator, Set<Integer> noSortSlots,
            Set<Integer> memorizedSlots, BiPredicate<Integer, ItemStack> matchesMemorizedSlot, CallbackInfo ci) {
        List<ItemStackGroup> groups = new ArrayList<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (!noSortSlots.contains(slot)) {
                groups.add(new ItemStackGroup(handler.getStackInSlot(slot)));
            }
        }
        if (OverflowGuard.totalsExceedInt(groups)) {
            ci.cancel();
        }
    }
}
