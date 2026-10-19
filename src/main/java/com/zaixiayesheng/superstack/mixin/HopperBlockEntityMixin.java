package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Hopper block limit: {@code HopperBlockEntity.setItem} clamps to the container default (64). */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    @Redirect(
            method = "setItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getMaxStackSize()I"),
            require = 0
    )
    private int superstack$limitForSetItem(HopperBlockEntity hopper) {
        return SuperStackConfig.maxStackSize();
    }
}