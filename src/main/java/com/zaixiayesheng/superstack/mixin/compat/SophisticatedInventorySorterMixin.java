package com.zaixiayesheng.superstack.mixin.compat;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sophisticated Core 的整理会把同类物品压成一个 {@code int} 总数，再按每个槽位的上限往槽里铺。
 *
 * <p>两处会吞物品：一是总数用 int 累加，两个 21 亿的堆叠相加就溢出成负数，随后那一组被整组清空；二是铺的时候
 * {@code Math.min(剩余数量, 槽位上限)}，槽位上限小于堆叠数量时只放上限那部分，剩余的在槽位铺完后直接消失。
 *
 * <p>这里在入口接管：用 long 记账，按顺序铺满每个可排槽位（每槽容量取物品自身的上限，本模组的核心补丁已保证它
 * 不小于当前数量），先模拟一遍；槽位放不下就完全不动，绝不少放。精妙以外的处理器只在真的溢出时才接管。
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.util.InventorySorter", remap = false)
public abstract class SophisticatedInventorySorterMixin {
    private static final String SORT_HANDLER = "sortHandler(Lnet/neoforged/neoforge/items/IItemHandlerModifiable;Ljava/util/Comparator;Ljava/util/Set;Ljava/util/Set;Ljava/util/function/BiPredicate;)V";

    private static Method keyOf;

    @Inject(method = SORT_HANDLER, at = @At("HEAD"), cancellable = true, remap = false)
    private static void superstack$sortWithLongTotals(IItemHandlerModifiable handler, Comparator<?> comparator,
            Set<Integer> noSortSlots, Set<Integer> memorizedSlots, BiPredicate<Integer, ItemStack> matchesMemorizedSlot,
            CallbackInfo ci) {
        if (!memorizedSlots.isEmpty()) {
            // 「记住槽位」功能要按槽位匹配，交回原逻辑。
            return;
        }

        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (!noSortSlots.contains(slot) && accessible(handler, slot)) {
                slots.add(slot);
            }
        }
        if (slots.isEmpty()) {
            return;
        }

        List<ItemStack> prototypes = new ArrayList<>();
        List<Long> totals = new ArrayList<>();
        for (int slot : slots) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int index = indexOf(prototypes, stack);
            if (index < 0) {
                prototypes.add(stack.copy());
                totals.add((long) stack.getCount());
            } else {
                totals.set(index, totals.get(index) + stack.getCount());
            }
        }
        if (prototypes.isEmpty()) {
            return;
        }

        boolean overflow = false;
        for (long total : totals) {
            if (total > Integer.MAX_VALUE) {
                overflow = true;
                break;
            }
        }
        boolean sophisticated = handler.getClass().getName().startsWith("net.p3pp3rf1y.sophisticated");
        if (!overflow && !sophisticated) {
            return;
        }

        List<Integer> order = sortedOrder(prototypes, totals, comparator);
        if (order == null) {
            return;
        }

        // 先模拟一遍：槽位够不够放。放不下就原样不动。
        int needed = 0;
        for (int index : order) {
            long remaining = totals.get(index);
            long perSlot = Math.max(1, prototypes.get(index).getMaxStackSize());
            needed += (int) Math.min(Integer.MAX_VALUE, (remaining + perSlot - 1) / perSlot);
            if (needed > slots.size()) {
                return;
            }
        }

        int cursor = 0;
        for (int index : order) {
            ItemStack prototype = prototypes.get(index);
            long remaining = totals.get(index);
            long perSlot = Math.max(1, prototype.getMaxStackSize());
            while (remaining > 0 && cursor < slots.size()) {
                int slot = slots.get(cursor++);
                int placed = (int) Math.min(remaining, perSlot);
                ItemStack out = prototype.copy();
                out.setCount(placed);
                setSlot(handler, slot, out);
                remaining -= placed;
            }
        }
        while (cursor < slots.size()) {
            int slot = slots.get(cursor++);
            if (!handler.getStackInSlot(slot).isEmpty()) {
                setSlot(handler, slot, ItemStack.EMPTY);
            }
        }
        ci.cancel();
    }

    private static int indexOf(List<ItemStack> prototypes, ItemStack stack) {
        for (int i = 0; i < prototypes.size(); i++) {
            if (ItemStack.isSameItemSameComponents(prototypes.get(i), stack)) {
                return i;
            }
        }
        return -1;
    }

    /** 精妙的 {@code InventoryHandler#isSlotAccessible} 会挡掉少数槽位，这里同样跳过，避免把物品塞进不可见槽。 */
    private static boolean accessible(IItemHandlerModifiable handler, int slot) {
        try {
            Method method = handler.getClass().getMethod("isSlotAccessible", int.class);
            Object result = method.invoke(handler, slot);
            return !(result instanceof Boolean flag) || flag;
        } catch (NoSuchMethodException missing) {
            return true;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            return true;
        }
    }

    /** 精妙自己的 {@code setSlotStack} 会走内容变更回调，优先用它。 */
    private static void setSlot(IItemHandlerModifiable handler, int slot, ItemStack stack) {
        try {
            Method method = handler.getClass().getMethod("setSlotStack", int.class, ItemStack.class);
            method.invoke(handler, slot, stack);
            return;
        } catch (NoSuchMethodException missing) {
            // 普通处理器没有这个方法，退回接口方法。
        } catch (ReflectiveOperationException | RuntimeException failure) {
            // 反射失败也退回接口方法，保证整理本身不中断。
        }
        handler.setStackInSlot(slot, stack);
    }

    /**
     * 用模组自己的比较器排序；比较器针对 {@code ItemStackKey}，这里通过反射造出同名 key（不引第三方类型）。
     * 数量只用于排序，超过 int 时按 int 上限截断，不影响最终放置的数量。
     */
    private static List<Integer> sortedOrder(List<ItemStack> prototypes, List<Long> totals, Comparator<?> comparator) {
        try {
            Method factory = keyFactory();
            List<Map.Entry<Object, Integer>> entries = new ArrayList<>();
            Map<Object, Integer> indexByKey = new IdentityHashMap<>();
            for (int i = 0; i < prototypes.size(); i++) {
                Object key = factory.invoke(null, prototypes.get(i));
                if (key == null || indexByKey.putIfAbsent(key, i) != null) {
                    return null;
                }
                int count = (int) Math.min(totals.get(i), (long) Integer.MAX_VALUE);
                entries.add(new AbstractMap.SimpleImmutableEntry<>(key, count));
            }
            sortRaw(comparator, entries);
            List<Integer> order = new ArrayList<>();
            for (Map.Entry<Object, Integer> entry : entries) {
                Integer index = indexByKey.get(entry.getKey());
                if (index == null) {
                    return null;
                }
                order.add(index);
            }
            return order.size() == prototypes.size() ? order : null;
        } catch (ReflectiveOperationException failure) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void sortRaw(Comparator<?> comparator, List<Map.Entry<Object, Integer>> entries) {
        entries.sort((Comparator) comparator);
    }

    private static Method keyFactory() throws ReflectiveOperationException {
        Method cached = keyOf;
        if (cached == null) {
            Class<?> type = Class.forName("net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey");
            cached = type.getMethod("of", ItemStack.class);
            keyOf = cached;
        }
        return cached;
    }
}
