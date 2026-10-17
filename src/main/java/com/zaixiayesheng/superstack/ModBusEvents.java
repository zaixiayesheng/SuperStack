package com.zaixiayesheng.superstack;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

import java.util.LinkedHashSet;
import java.util.Set;

/** 给物品设置新的堆叠上限。由 {@link SuperStack} 在 mod 事件总线上注册。 */
public final class ModBusEvents {
    private ModBusEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        int max = SuperStackConfig.maxStackSize();
        int threshold = SuperStackConfig.minimumStackSizeThreshold();

        // 边改边统计。这里报的是「真正生效的结果」，不是我们以为会生效的结果
        int[] unchanged = {0};
        int[] changed = {0};
        // 缩放后反而比 maxStackSize 小的原上限（原版就是 16、4 这两种），用来在日志里列出对应关系。
        // 缩放后正好顶到 maxStackSize 的（原版 64 就是）不算，否则日志会读成「64 被缩小了」
        Set<Integer> scaledBelowMax = new LinkedHashSet<>();

        event.getAllItems().forEach(item -> {
            // 有耐久度的（工具、护甲）不动；原本只能堆 1 个的也不动
            if (item.components().has(DataComponents.DAMAGE)
                    || item.components().has(DataComponents.MAX_DAMAGE)
                    || item.getDefaultMaxStackSize() <= threshold) {
                unchanged[0]++;
                return;
            }

            int vanillaMax = item.getDefaultMaxStackSize();
            int newMax = SuperStackConfig.scaledMax(vanillaMax);
            if (newMax == vanillaMax) {
                unchanged[0]++;
                return;
            }

            event.modify(item, builder -> builder.set(DataComponents.MAX_STACK_SIZE, newMax));
            changed[0]++;
            if (newMax < max) {
                scaledBelowMax.add(vanillaMax);
            }
        });

        logApplied(changed[0], unchanged[0], scaledBelowMax, max);
    }

    /** 启动时那行「到底改了多少物品」。 */
    private static void logApplied(int changed, int unchanged, Set<Integer> scaledBelowMax, int max) {
        if (changed == 0) {
            SuperStackLog.warn("没有物品的堆叠上限被改动：检查一下 maxStackSize（当前 " + max
                    + "）和 minimumStackSizeThreshold 是不是把范围卡没了。");
            return;
        }

        SuperStackLog.info("已应用堆叠上限：改了 " + changed + " 个物品，"
                + "保持原版上限的有 " + unchanged + " 个（有耐久度的工具护甲、以及默认只堆 1 个的）");

        if (!scaledBelowMax.isEmpty()) {
            int[] sizes = scaledBelowMax.stream().mapToInt(Integer::intValue).toArray();
            SuperStackLog.info("小堆叠按比例放大：" + SuperStackConfig.summarizeDefaults(sizes));
        }
    }
}
