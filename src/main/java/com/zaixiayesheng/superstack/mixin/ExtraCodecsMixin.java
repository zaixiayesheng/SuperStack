package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 原版把堆叠数量限制在 1~99：{@code ExtraCodecs.intRange(1, 99)}。
 * 整个原版只有两处用到这个范围，都是我们要改的：
 *
 * <ul>
 *   <li>{@code DataComponents.MAX_STACK_SIZE} —— 物品的堆叠上限组件；</li>
 *   <li>{@code ItemStack.CODEC} 的 count 字段 —— 存盘 / 读取时的校验范围，
 *       不改的话大堆叠在存档读写时会被判非法。</li>
 * </ul>
 *
 * 所以直接改这个方法的上界，比按 lambda 名字去定位调用点稳得多。
 */
@Mixin(ExtraCodecs.class)
public abstract class ExtraCodecsMixin {
    @ModifyVariable(method = "intRange", at = @At("HEAD"), argsOnly = true, index = 1)
    private static int superstack$raiseUpperBound(int bound) {
        return SuperStackConfig.raiseCodecUpperBound(bound);
    }
}
