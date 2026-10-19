package com.zaixiayesheng.superstack.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 网络同步里的物品数量。
 *
 * <p>1.20.1 把数量写成 byte，读取端也按 byte 读，所以客户端看到的永远只有 -128~127：
 * 32768 个的堆叠过去会变成 0 个。这里把「写数量、读数量」换成 VarInt，
 * 两端都是本模组时数量才能原样传过去。
 */
@Mixin(FriendlyByteBuf.class)
public abstract class FriendlyByteBufMixin {
    /**
     * 这个方法不在 SRG 映射里（Forge 补丁加进来的重载，正式环境保持原名），所以 remap = false。
     * 原版的 {@code writeItem(ItemStack)} 只是转调它，正式环境改名成 SRG 后仍然走到这里。
     */
    @Inject(
            method = "writeItemStack(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/network/FriendlyByteBuf;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void superstack$writeItemStack(ItemStack stack, boolean limitedTag,
                                           CallbackInfoReturnable<FriendlyByteBuf> cir) {
        FriendlyByteBuf self = (FriendlyByteBuf) (Object) this;
        if (stack.isEmpty()) {
            self.writeBoolean(false);
        } else {
            self.writeBoolean(true);
            Item item = stack.getItem();
            self.writeId(BuiltInRegistries.ITEM, item);
            self.writeVarInt(stack.getCount());

            CompoundTag tag = null;
            if (item.isDamageable(stack) || item.shouldOverrideMultiplayerNbt()) {
                tag = limitedTag ? stack.getShareTag() : stack.getTag();
            }
            self.writeNbt(tag);
        }
        cir.setReturnValue(self);
    }

    @Inject(method = "readItem()Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void superstack$readItem(CallbackInfoReturnable<ItemStack> cir) {
        FriendlyByteBuf self = (FriendlyByteBuf) (Object) this;
        if (!self.readBoolean()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        Item item = self.readById(BuiltInRegistries.ITEM);
        int count = self.readVarInt();
        ItemStack stack = new ItemStack(item, count);
        stack.readShareTag(self.readNbt());
        cir.setReturnValue(stack);
    }
}