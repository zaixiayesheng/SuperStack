package com.zaixiayesheng.superstack;

import net.minecraft.core.component.DataComponents;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/** 给物品设置新的堆叠上限。由 {@link SuperStack} 在 mod 事件总线上注册。 */
public final class ModBusEvents {
    private ModBusEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        int max = SuperStackConfig.maxStackSize();
        boolean scale = SuperStackConfig.scaleSmallStacks();
        int threshold = SuperStackConfig.minimumStackSizeThreshold();

        event.getAllItems()
                // 有耐久度的（工具、护甲）不动；原本只能堆 1 个的也不动
                .filter(item -> !item.components().has(DataComponents.DAMAGE)
                        && !item.components().has(DataComponents.MAX_DAMAGE)
                        && item.getDefaultMaxStackSize() > threshold)
                .forEach(item -> {
                    int vanillaMax = item.getDefaultMaxStackSize();
                    int newMax = scale
                            ? (int) Math.clamp((long) vanillaMax * max / 64L, 1L, (long) max)
                            : max;
                    if (newMax != vanillaMax) {
                        event.modify(item, builder -> builder.set(DataComponents.MAX_STACK_SIZE, newMax));
                    }
                });
    }
}
