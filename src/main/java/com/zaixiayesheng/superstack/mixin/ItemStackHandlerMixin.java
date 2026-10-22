package com.zaixiayesheng.superstack.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge 的 ItemStackHandler（很多模组的机器槽位用它）。
 * 只有配置里打开 raiseModdedSlotLimits 才生效，而且只升不降 —— 直接改成
 * 一个天文数字会让别的模组的插入判断出问题（有的模组就是这么被搞坏的）。
 */
@Mixin(ItemStackHandler.class)
public abstract class ItemStackHandlerMixin {

    /** Generator Galore 的燃料槽处理器；它的 setStackInSlot 会把整槽覆盖成单个物品。 */
    private static final String GENERATOR_GALORE_FUEL_HANDLER =
            "cy.jdkdigital.generatorgalore.common.container.ManualItemHandler";

    @Shadow
    @Final
    protected NonNullList<ItemStack> stacks;

    @Shadow
    protected abstract void onContentsChanged(int slot);

    @ModifyReturnValue(method = "getSlotLimit", at = @At("RETURN"))
    private int superstack$raiseModdedLimit(int original) {
        return SuperStackConfig.raiseModdedSlotLimit(original);
    }

    /**
     * Generator Galore 只在燃料槽剩 1 个时才走「剩余物品 / 附魔书」分支，堆叠数大于 1 时
     * 直接把整槽替换成单个物品，多出来的燃料就此消失。这里接管那种替换：扣 1 个燃料，
     * 把 mod 想塞回来的物品（书本、空桶等非燃料）挪到其它槽位。
     */
    @Inject(method = "setStackInSlot", at = @At("HEAD"), cancellable = true)
    private void superstack$consumeGeneratorGaloreFuel(int slot, ItemStack stack, CallbackInfo ci) {
        if (!GENERATOR_GALORE_FUEL_HANDLER.equals(this.getClass().getName())) {
            return;
        }
        if (slot != 0 || stack.isEmpty()) {
            return;
        }

        ItemStack current = this.stacks.get(slot);
        if (current.getCount() <= 1) {
            return;
        }
        ItemStackHandler self = (ItemStackHandler) (Object) this;
        if (self.isItemValid(slot, stack)) {
            // 正常往燃料槽里放燃料（或菜单里的同种合并），交给原逻辑。
            return;
        }

        ci.cancel();

        ItemStack remainingFuel = current.copy();
        remainingFuel.shrink(1);
        this.stacks.set(slot, remainingFuel);
        this.onContentsChanged(slot);

        ItemStack leftover = stack.copy();
        for (int i = 0; i < this.stacks.size() && !leftover.isEmpty(); i++) {
            if (i == slot) {
                continue;
            }
            leftover = self.insertItem(i, leftover, false);
        }
    }
}
