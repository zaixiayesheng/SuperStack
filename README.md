<h1 align="center">超级堆叠</h1>

<h3 align="center">SuperStack</h3>

<div align="center">

原版把堆叠卡在两个地方：物品的 <code>max_stack_size</code> 默认 64，存档读写时数量还被 99 卡着；容器的 <code>getMaxStackSize()</code> 默认也是 99。
这个模组把这两个值换成可配置的（默认 32768），并把槽位里的数量显示改成「缩写 + 悬停看精确值」。

English version: [README_EN.md](./README_EN.md)

[![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-green.svg?style=flat-square)](https://www.minecraft.net)
[![Forge 47.4](https://img.shields.io/badge/Forge-47.4+-orange.svg?style=flat-square)](https://files.minecraftforge.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](./LICENSE)

</div>

---

## 它解决什么

原版有两处把堆叠卡死。

第一处是物品上限：`Item.maxStackSize` 默认 64；而且数量和网络同步都按 byte 读写，超过 127 就溢出（32768 会变成 0）。

第二处是容器上限：`Container.getMaxStackSize()` 默认返回 99，所有没自己重写这个方法的容器都吃这个值——合成格、箱子、背包。所以光把物品上限调大，工作台里照样只放得下 99 个。

数量显示是顺带的坑。原版那个数字右对齐、不缩放，四位数就戳出 16 像素的格子了。常见的做法是缩写成 `1K`，但那样 1000 到 1999 全变成 `1K`，看不出具体多少。

这个模组对应做三件事：放高物品上限、放高容器上限、把数量显示做成「槽位缩写 + 悬停精确」。安装只需要把 jar 丢进 `mods/`，依赖 Forge 47.4+ 和 Java 17。

## 配置

配置文件 `config/superstack-common.toml`，改完重启游戏生效。

| 配置项 | 默认 | 说明 |
|---|---|---|
| `maxStackSize` | 32768 | 物品堆叠上限（64 ~ 1000000） |
| `scaleSmallStacks` | true | 小堆叠物品按比例放大：原上限 × `maxStackSize` ÷ 64（末影珍珠 16 → 8192） |
| `minimumStackSizeThreshold` | 1 | 原上限不超过这个值的不放大（默认只有原本堆 1 个的，比如工具） |
| `countDisplay` | COMPACT | 槽位里的数量显示方式，见下一节 |
| `abbreviateFrom` | 10000 | 数量到多少开始用单位缩写。默认和精妙背包一致：1000 ~ 9999 写 `1,234`，10000 起才是 `10.0k` |
| `tooltipExactCount` | true | 悬停时在提示框里显示精确数量（超过 99 才显示） |
| `raiseModdedSlotLimits` | true | 是否也放大其它模组的物品槽上限（`ItemStackHandler` / `ComponentItemHandler`）。默认打开，关掉后部分整理流程可能装不下整组大堆叠，见「与整理类模组的兼容」 |

## 数量显示

数量超过 99 才接管原版的绘制，规则对着精妙背包来：

| 数量 | 槽位里显示 |
|---|---|
| ≤ 99 | 原版怎么写就怎么写 |
| 100 ~ 9999 | 精确数字，带千位分隔符：`1,234` |
| 10000 ~ 999999 | `10.0k`、`19.9k`、`123k`、`999k` |
| ≥ 1000000 | `1.00m`、`2.50m` |

单位一律截断不四舍五入（精妙也是截断），免得显示得比实际多。文字按当前字体宽度缩放到正好塞进 16 像素的格子，仍然贴着右下角。不管缩写得多简略，悬停时提示框里都有精确数量。`countDisplay = "EXACT"` 可以改成一律显示精确数字，同样会缩放塞进格子。

## 与整理类模组的兼容

本模组只改堆叠上限和数量显示，不参与任何物品搬运。实际表现取决于各处的槽位上限是否一致：

| 位置 | 上限 |
|---|---|
| 原版容器（箱子、背包、合成格、漏斗……） | 跟着 `maxStackSize` |
| 其它模组的物品槽（`ItemStackHandler`、`ComponentItemHandler`） | 由 `raiseModdedSlotLimits` 控制，默认打开，也跟着 |

两边不一致时，整理模组搬运整叠物品会装不下，装不下的部分是否退回取决于该模组自己的处理方式。

实测（`maxStackSize = 10000`、27 格、用精妙背包的「整理背包」）：

```
raiseModdedSlotLimits = false    270000 → 2673        少了 267327
raiseModdedSlotLimits = true     270000 → 270000      一个不少
```

差异来自整理流程会把槽位内容放进一个默认上限 99 的物品处理器再排回去，大堆叠只能写回 99；打开 `raiseModdedSlotLimits` 后上限跟着配置走，因此这一项默认开启。

| 整理模组 | 实际表现 | 说明 |
|---|---|---|
| 一键背包整理Next | 不受影响 | 先在本地沙盒里模拟再发原版点击，读取上限用 `ItemStack.getMaxStackSize()`，跟着本模组走，放不下的留在原地 |
| 精妙背包整理 | 默认不受影响 | 见上面的实测；它自带「不超过物品上限」的保护，槽位上限低于物品上限时会截断 |

存档读写正常：原版用 `ExtraCodecs.intRange(1, 99)` 限制数量，本模组把这个范围的上界换成配置值，实测 10000 的堆叠存盘再读回来还是 10000。
## 实现方式

| 位置 | 做法 |
|---|---|
| `ExtraCodecsMixin` | 把 `ExtraCodecs.intRange(1, 99)` 的上界换成配置值。原版只有两处用到这个范围：物品的 `max_stack_size` 组件和 `ItemStack.CODEC` 的 count 字段，正好都是要改的 |
| `ModBusEvents` | `ModifyDefaultComponentsEvent` 里给每个物品设置新的 `max_stack_size`；有耐久度的、原本堆 1 个的不动 |
| `ContainerMixin` / `SlotMixin` | 把 `getMaxStackSize()` 返回的 99 换成配置值，其它值原样保留 |
| `GuiGraphicsMixin` / `CountFormat` | 数量超过 99 时接管绘制，负责缩写、精确写法和缩放对齐 |
| `ClientEvents` | 提示框里补一行精确数量 |
| `ItemStackHandlerMixin` / `ComponentItemHandlerMixin` | 只在 `raiseModdedSlotLimits` 打开时生效，而且只升不降 |

全程不把上限改成天文数字，只替换原版的 99 / 64 这些默认值，其它模组自己设的数值不动。

## 兼容性

- 和 KubeJS 共存没问题：它的 `remove_slot_limit` / `default_max_stack_size` 默认透传，不会把上限压回去。
- 和整理类模组的表现见「与整理类模组的兼容」。
- 建议新开存档测试：已经存在的大堆叠物品在卸载本模组后会被原版校验拒绝。

## 构建

需要 JDK 17。

```bash
gradlew build
```

产物在 `build/libs/`。

## 更新日志

### 1.0.0

- 首个 Forge 47.4 / 1.20.1 版本：物品上限、存档数量、网络同步、槽位上限一起改
- 通用机械 / 应用能源 2 / 精妙核心的容器与合成格不再卡 64 / 99
- 应用能源 2 合成终端的产物不再被拆成 99 一组（无线终端、综合工作终端同一条路径）
- 精妙系列自己的界面不叠加本模组的数量角标
- 拆掉精妙容器时掉落不再按 64 一组炸成一地
- 可配置的物品堆叠上限（64 ~ 1000000，默认 32768），小堆叠按比例放大并可设阈值跳过
- 数量显示：100 ~ 9999 精确、10000 起缩写，悬停给精确值
- 启动时把每个物品实际拿到的堆叠上限记进日志

## 许可

[MIT](./LICENSE) · 作者 zaixiayesheng · [问题反馈](https://github.com/zaixiayesheng/SuperStack/issues)
