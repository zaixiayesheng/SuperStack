package com.zaixiayesheng.superstack.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 存档里的堆叠数量。
 *
 * <p>1.20.1 的 {@code ItemStack.save} 把数量写成一个 <b>byte</b>
 * （{@code putByte("Count", (byte) count)}），读的时候也是 {@code getByte("Count")}，
 * 所以超过 127 个的堆叠存盘再读回来就会变成一个负数 / 1 个 —— 存档直接废掉。
 *
 * <p>这里在写的时候把数量额外写成一个 int 键（同名 "Count"，类型不同），
 * 读的时候如果发现是 int 就用它覆盖。≤127 的堆叠仍然走原版的 byte，
 * 存档格式对原版保持兼容（只有超上限的堆叠才多一个 int）。
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow
    private int count;

    @Inject(method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void superstack$readLargeCount(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("Count", Tag.TAG_INT)) {
            this.count = tag.getInt("Count");
        }
    }

    @Inject(method = "save(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    private void superstack$writeLargeCount(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.count > Byte.MAX_VALUE) {
            // 原版刚写进去的 byte 是负数，用同名的 int 键盖掉
            tag.putInt("Count", this.count);
        }
    }
}
