<p align="center"><img src="logo.png" width="160" alt="SuperStack logo"></p>

<h1 align="center">超级堆叠</h1>

<h3 align="center">SuperStack</h3>

<div align="center">

把物品的堆叠上限从 64 提上去，并让容器、槽位、存档与网络同步都认这个新上限。

English version: [README_EN.md](./README_EN.md)

[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-green.svg?style=flat-square)](https://www.minecraft.net)
[![NeoForge 21.1](https://img.shields.io/badge/NeoForge-21.1+-orange.svg?style=flat-square)](https://neoforged.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](./LICENSE)

</div>

---

## 这是什么

原版把堆叠卡在三个地方，只改其中一个都没用：

- **物品上限**：`max_stack_size` 组件默认 64。
- **存档与网络**：`ItemStack.CODEC` 的 count 只接受 1～99，`FriendlyByteBuf` 也按上限写数量，所以上限调大后存盘和联机同步会失败或被截断。
- **容器上限**：`Container.getMaxStackSize()` 默认返回 99，没自己重写这个方法的容器（合成格、箱子、背包）都吃这个值。

本模组把这三处一起抬到可配置值（默认 32768），另外把槽位里的数量显示改成「缩写 + 悬停看精确值」，因为原版那串数字超过三位就会戳出格子。

## 安装

| 文件 | 必需 | 说明 |
|---|---|---|
| `SuperStack-1.21.1-1.0.4.jar` | 是 | 本模组，单个 jar |
| Minecraft 1.21.1 + NeoForge 21.1+ | 是 | 运行平台（需要 Java 21） |

- 通用机械、应用能源 2、精妙核心 / 存储 / 背包都是**可选**的：装了才会启用对应的槽位补丁，没装自动跳过，日志里只记一行。
- 客户端与服务端都要装，否则两边对堆叠上限的认知不一致。

## 堆叠上限

`maxStackSize` 控制物品上限，默认 32768（范围 64～1000000）。

- 有耐久度的物品（工具、护甲、Meka 装备）不动。
- 原本只能堆 1 个的物品不动（`minimumStackSizeThreshold = 1`）。
- 其余物品按 `原上限 × maxStackSize ÷ 64` 等比放大：末影珍珠 16 → 8192，鸡蛋 16 → 8192，药水 1 不变。把 `scaleSmallStacks` 关掉则一律按 `maxStackSize` 处理。

存档与网络同步由同一套改写接管：内嵌在物品 NBT 里的数量（比如升级物品里的九个槽）按 int 存、按 VarInt 同步，因此 32768 的堆叠存盘再读回来仍是 32768。

## 数量显示

只在数量超过 99 时接管绘制：

| 数量 | 槽位里显示 |
|---|---|
| ≤ 99 | 原版写法 |
| 100 ～ 9999 | 精确数字带千位分隔符，如 `1,234` |
| 10000 ～ 999999 | `10.0k`、`19.9k`、`123k`、`999k` |
| ≥ 1000000 | `1.00m`、`2.50m` |

单位一律截断不四舍五入，避免显示得比实际多。文字按字体宽度缩放到正好塞进 16 像素格子；悬停时提示框里始终给出精确数量。

## 配置

配置文件 `config/superstack-common.toml`，改完重启生效。

| 配置项 | 默认 | 范围 | 说明 |
|---|---|---|---|
| `maxStackSize` | 32768 | 64～1000000 | 物品堆叠上限 |
| `scaleSmallStacks` | true | — | 小堆叠按比例放大（16 → 8192），关掉则一律用上限 |
| `minimumStackSizeThreshold` | 1 | 0～64 | 原上限不超过该值的不放大（工具、护甲） |
| `countDisplay` | COMPACT | COMPACT / EXACT | 槽位数量显示方式 |
| `abbreviateFrom` | 10000 | 1000～1000000 | 多少数量开始用 k / m 缩写 |
| `tooltipExactCount` | true | — | 悬停时显示精确数量 |
| `raiseModdedSlotLimits` | true | — | 是否同时抬高其它模组的物品槽上限 |

## 与其它模组的槽位

原版容器（箱子、背包、合成格、漏斗）跟着本模组走。其它模组自建的槽位分两种情况：继承原版 `Slot` / `Container` 的自动跟上；自己实现 `ItemStackHandler` / `ComponentItemHandler` 的由 `raiseModdedSlotLimits` 统一抬高（只升不降）。

| 模组 | 补丁位置 | 修的是什么 |
|---|---|---|
| 通用机械 | `BasicInventorySlot#getLimit` | 机器内部槽与界面槽默认写死 99 |
| 应用能源 2 | `AppEngInternalInventory#getSlotLimit`、`InternalInventoryItemHandler`、`AppEngSlot` | 网络库存与终端槽停在 64 / 99 |
| 应用能源 2（合成终端） | `PlayerInternalInventory` / `CarriedItemInventory` 补上 `getSlotLimit` | 合成产物进入背包或光标时被拆成 99 一组 |
| 精妙核心 / 存储 / 背包 | `InventoryHandler#getSlotLimit`、`StatefulComponentItemHandler#getSlotLimit` | 容器槽停在 64、合成升级的 9 格合成格停在 99 |

以上补丁写在独立的 `superstack.compat.mixins.json` 里，`required: false`：目标类不存在时只记一行日志，不影响启动。

## 与整理类模组的兼容

本模组不参与任何物品搬运，只保证「各处的上限一致」。两边不一致时，整理流程把整叠物品搬进一个默认上限 99 的处理器再排回去，就会截断。

实测（`maxStackSize = 10000`、27 格、精妙背包的「整理背包」）：

```
raiseModdedSlotLimits = false    270000 → 2673        少了 267327
raiseModdedSlotLimits = true     270000 → 270000      一个不少
```

| 整理模组 | 表现 |
|---|---|
| 一键背包整理Next | 不受影响，读取上限走 `ItemStack.getMaxStackSize()`，放不下的留在原地 |
| 精妙背包整理 | 默认不受影响；它自带「不超过物品上限」的保护 |

## 常见问题

- **卸载后会怎样**：存档里超过原版上限的堆叠在读取时会被原版校验拒绝并记一条 `Tried to load invalid item`，那些物品会消失。卸载前先把大堆叠拆回 64。
- **服务端装了客户端没装**：客户端仍按 64 显示与操作，放不进去的部分由服务端拒绝，表现为「点了没反应」。两端都要装。
- **精妙界面里看不到本模组的角标**：精妙自己会画缩写数量，本模组在那类界面里主动让位，避免叠字。
- **创造模式物品栏显示异常**：创造页签按组件值分组，上限放大后同一物品仍只有一个条目，属于正常现象。

## 兼容性

- KubeJS 共存正常：它的 `remove_slot_limit` / `default_max_stack_size` 默认透传，不会把上限压回去。
- 与通用机械、应用能源 2、精妙系列一起使用时，槽位上限与本模组保持一致；拆掉这些容器时掉落不再按 64 一组炸开（1.0.1 起）。
- 建议在新存档或备份后测试：这是会改变存档数据的模组。

## 开发者

### 实现方式

| 位置 | 做法 |
|---|---|
| `ExtraCodecsMixin` | 把 `ExtraCodecs.intRange(1, 99)` 的上界换成配置值，同时覆盖 `max_stack_size` 组件与 `ItemStack.CODEC` 的 count |
| `ModBusEvents` | `ModifyDefaultComponentsEvent` 里给每个物品写入新的 `max_stack_size`，跳过有耐久度与原本堆 1 个的 |
| `ContainerMixin` / `SlotMixin` | 把 `getMaxStackSize()` 返回的 99 换成配置值，其它值原样保留 |
| `GuiGraphicsMixin` / `CountFormat` | 数量 > 99 时接管绘制，负责缩写、精确写法与缩放对齐 |
| `ClientEvents` | 提示框补一行精确数量 |
| `ItemStackHandlerMixin` / `ComponentItemHandlerMixin` | 受 `raiseModdedSlotLimits` 控制，只升不降 |
| `mixin/compat/*` | 六个按模组划分的槽位补丁，独立配置、缺失时自动跳过 |

### 构建

需要 JDK 21，`gradlew build`，产物在 `build/libs/`。

## 更新日志

### 1.0.4

- 应用能源 2 的槽位补丁改成直接补 `getSlotLimit` 方法。1.0.3 把补丁打在只实现接口、没有声明该方法的类上，装有应用能源 2 时会在启动阶段抛错，本版修掉。

<details>
<summary><b>1.0.3</b></summary>

- 应用能源 2 的合成终端（含无线终端与综合工作终端）里，产物写进背包或光标时不再被 `InternalInventory` 的默认上限拆成 99 一组。

</details>

<details>
<summary><b>1.0.2</b></summary>

- 精妙系列合成升级的 9 格合成格不再卡 99：补丁从容器槽位移到 `StatefulComponentItemHandler#getSlotLimit`。

</details>

<details>
<summary><b>1.0.1</b></summary>

- 容器与槽位上限跟着配置走，不再停在 99。
- 通用机械、应用能源 2、精妙系列的槽位不再停在 64 / 99。
- 精妙系列界面不再叠加本模组的数量角标。
- 拆掉这些容器时掉落不再拆成一地 64 一组。
- 对应模组没装时补丁自动跳过。

</details>

<details>
<summary><b>1.0.0</b></summary>

- 可配置的物品堆叠上限（64～1000000，默认 32768），小堆叠按比例放大并可设阈值跳过。
- 数量显示：100～9999 精确、10000 起缩写，悬停给精确值。

</details>

## 许可证与反馈

[MIT](./LICENSE) · 作者 zaixiayesheng · [问题反馈](https://github.com/zaixiayesheng/SuperStack/issues)

提交问题时请附上 Minecraft 与加载器版本、本模组版本、`logs/latest.log`，以及涉及的容器或整理流程。
