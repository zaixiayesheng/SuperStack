package com.zaixiayesheng.superstack;

import java.util.ArrayList;
import java.util.List;

/**
 * 判断「同一类物品的总数」是否会超出 int。精妙存储整理背包时把所有同物品堆叠压成一个 int 总量，
 * 两个 int 上限的堆叠相加就会溢出成负数，随后整组被清空。这里用 long 先算一遍，超了就放弃整理。
 *
 * <p>刻意不引用任何 Minecraft 类型，方便直接单测。
 */
public final class OverflowGuard {
    private OverflowGuard() {
    }

    /** 一类可比较、可累加的物品。 */
    public interface Group {
        long count();

        boolean sameItem(Group other);
    }

    /**
     * @return 只要有一类物品的总数超过 {@link Integer#MAX_VALUE} 就返回 true
     */
    public static boolean totalsExceedInt(Iterable<? extends Group> groups) {
        List<Group> representatives = new ArrayList<>();
        List<Long> totals = new ArrayList<>();
        for (Group group : groups) {
            if (group.count() <= 0L) {
                continue;
            }
            int index = indexOf(representatives, group);
            if (index < 0) {
                representatives.add(group);
                totals.add(group.count());
            } else {
                long total = totals.get(index) + group.count();
                if (total > Integer.MAX_VALUE) {
                    return true;
                }
                totals.set(index, total);
            }
        }
        return false;
    }

    private static int indexOf(List<Group> representatives, Group group) {
        for (int i = 0; i < representatives.size(); i++) {
            if (representatives.get(i).sameItem(group)) {
                return i;
            }
        }
        return -1;
    }
}
