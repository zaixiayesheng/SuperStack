package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 更多熔炉（Iron Furnaces）：把写死的 64 换成配置的堆叠上限。
 *
 * <p>该模组在 {@code canSmelt} 与两个批量熔炼方法里用 64 判断「输出格还能不能再放」，
 * 输入格本身没有限制，所以堆叠上限调高后只会卡输出，产物堆到 64 就不再工作。
 */
@Mixin(targets = "ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase", remap = false)
public class IronFurnacesFurnaceTileMixin {

    @ModifyConstant(
            method = {"tick", "canSmelt", "smeltItemMult", "smeltFactoryItemMult"},
            constant = @Constant(intValue = 64),
            remap = false)
    private static int superstack$raiseFurnaceStackLimit(int original) {
        return SuperStackConfig.maxStackSize();
    }
}