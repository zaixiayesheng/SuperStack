package com.zaixiayesheng.superstack.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 机械动力：过滤器上的「提取数量」设置。
 *
 * <p>原版这个数值的取值范围就是被过滤物品的上限（最多 64），Create 用它来画那条刻度条：
 * {@code ValueSettingsScreen} 里宽度 = {@code (maxValue + 1) * scale + 刻度数 * 9}。
 * 堆叠上限抬到 32768 之后，取值范围变成 32768，刻度条被拉到几千像素、直接飞出屏幕。
 *
 * <p>这里把「设置项允许的最大值」压回原版的 64，界面恢复原样；实际搬运仍按游戏原本的规则走。
 */
@Mixin(targets = "com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour", remap = false)
public abstract class CreateFilteringBehaviourMixin {

    @Shadow
    private int count;

    @ModifyReturnValue(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"))
    private int superstack$capSettingRange(int original) {
        return Math.min(original, 64);
    }

    @Inject(method = "read(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;Z)V", at = @At("RETURN"))
    private void superstack$clampLoadedCount(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (this.count > 64) {
            this.count = 64;
        }
    }
}
