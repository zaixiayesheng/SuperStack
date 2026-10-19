package com.zaixiayesheng.superstack;

import net.minecraft.world.item.ItemStack;

/** 把一个 {@link ItemStack} 包装成 {@link OverflowGuard.Group}，供兼容补丁做溢出判断。 */
public final class ItemStackGroup implements OverflowGuard.Group {
    private final ItemStack stack;

    public ItemStackGroup(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public long count() {
        return stack.isEmpty() ? 0L : stack.getCount();
    }

    @Override
    public boolean sameItem(OverflowGuard.Group other) {
        return other instanceof ItemStackGroup group && ItemStack.isSameItemSameTags(stack, group.stack);
    }
}
