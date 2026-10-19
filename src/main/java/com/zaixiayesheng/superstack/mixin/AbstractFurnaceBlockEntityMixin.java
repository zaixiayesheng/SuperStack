package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla furnace limit.
 *
 * <p>{@code setItem} clamps to the container default (64) and the tick path uses that value as the
 * per-smelt output cap, so a 32768 stack could never sit in a furnace slot. Both call sites are
 * redirected to the configured limit.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Redirect(
            method = "setItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getMaxStackSize()I"),
            require = 0
    )
    private int superstack$limitForSetItem(AbstractFurnaceBlockEntity furnace) {
        return SuperStackConfig.maxStackSize();
    }

    @Redirect(
            method = "serverTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getMaxStackSize()I"),
            require = 0
    )
    private static int superstack$limitForTick(AbstractFurnaceBlockEntity furnace) {
        return SuperStackConfig.maxStackSize();
    }
}