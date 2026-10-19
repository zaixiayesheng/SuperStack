package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Chest / barrel / shulker box limit.
 *
 * <p>{@code RandomizableContainerBlockEntity.setItem} clamps the incoming stack to
 * {@code this.getMaxStackSize()} (the {@code Container} default, 64 on 1.20.1), so anything above 64
 * is cut down as soon as it is placed into the block. The clamp is redirected to the configured limit.
 */
@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerBlockEntityMixin {

    @Redirect(
            method = "setItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/RandomizableContainerBlockEntity;getMaxStackSize()I"),
            require = 0
    )
    private int superstack$limitForSetItem(RandomizableContainerBlockEntity blockEntity) {
        return SuperStackConfig.maxStackSize();
    }
}