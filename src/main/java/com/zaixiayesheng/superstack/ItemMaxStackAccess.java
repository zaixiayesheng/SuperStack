package com.zaixiayesheng.superstack;

/**
 * 让启动日志能读到物品「原版」的堆叠上限。
 *
 * <p>1.20.1 的堆叠上限是 {@code Item} 上的一个字段，{@code getMaxStackSize()} 被
 * {@code ItemMixin} 接管后返回的已经是放大过的值，日志里要报「原上限 → 新上限」
 * 就只能直接读那个字段。
 */
public interface ItemMaxStackAccess {
    /** 混入保留的原始堆叠上限（未经本模组缩放）。 */
    int superstack$vanillaMaxStackSize();
}
