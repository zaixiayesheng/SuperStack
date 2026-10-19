<p align="center"><img src="logo.png" width="160" alt="SuperStack logo"></p>

<h1 align="center">超级堆叠</h1>

<h3 align="center">SuperStack</h3>

<div align="center">

把物品堆叠上限从 64 抬到可配置值（默认 32768），并让容器、槽位、存档、网络同步与合成全部认这个上限。

English version: [README_EN.md](./README_EN.md)

[![Release](https://img.shields.io/github/v/release/zaixiayesheng/SuperStack?filter=v21.1*&style=flat-square&label=Release&color=blue)](https://github.com/zaixiayesheng/SuperStack/releases)
[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-green.svg?style=flat-square)](https://www.minecraft.net)
[![NeoForge 21.1](https://img.shields.io/badge/NeoForge-21.1+-orange.svg?style=flat-square)](https://neoforged.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](./LICENSE)

[这是什么](#这是什么) · [安装](#安装) · [堆叠上限](#堆叠上限) · [数量显示](#数量显示) · [合成与容器](#合成与容器) · [槽位兼容](#槽位兼容) · [配置](#配置) · [常见问题](#常见问题) · [兼容性](#兼容性) · [开发者](#开发者) · [更新日志](#更新日志)

</div>

---

## 这是什么

原版把堆叠卡在好几个地方，只改其中一处没有用，本模组把这几处一起接管：

- **物品上限**：`max_stack_size` 数据组件默认 64。
- **存档**：`ItemStack.CODEC` 的 count 只接受 1～99，超过的堆叠存盘时会被拒绝。
- **网络**：容器同步包按单字节写数量，32768 读回来会变成 0。
- **容器与槽位**：`Container.getMaxStackSize()` 默认返回 99，箱子、背包、合成格都吃这个值。
- **合成**：Shift 点击合成结果时原版一轮只合一件，一格三万多材料会在同一个 tick 里跑三万多轮。

堆叠上限默认 32768（可配 64～1,000,000），小堆叠按比例放大（16 → 8192），有耐久度与原本堆 1 个的物品不动。

## 安装

| 文件 | 必需 | 说明 |
|---|---|---|
| `SuperStack-<游戏版本>-<版本号>.jar` | 是 | 从 Releases 下载对应版本，单个 jar |
| Minecraft 1.21.1 + NeoForge 21.1+ | 是 | 运行平台，需要 Java 21 |

- 客户端与服务端都要装：只装一边时，另一边仍按 64 处理，放不进去的部分由服务端拒绝。
- 通用机械、应用能源 2、精妙系列、更多熔炉都是可选的：装了才启用对应槽位补丁，没装时只记一行日志。

## 堆叠上限

| 原版上限 | 抬升后（默认配置） |
|---|---|
| 64 | 32,768 |
| 16 | 8,192 |
| 8 | 4,096 |
| 1 | 1（不放大） |

倍率固定为 `maxStackSize ÷ 64`，即默认 ×512。规则：

- 带耐久度的物品（工具、护甲、Meka 装备）不改，仍是 1。
- 原上限不超过 `minimumStackSizeThreshold`（默认 1）的物品不改。
- 关掉 `scaleSmallStacks` 后，所有物品一律用 `maxStackSize`。

存档与网络同步一并改写：内嵌在物品里的数量（例如升级物品携带的 9 个槽）按 int 存盘、按 VarInt 同步，32768 存完再读回来仍是 32768。

## 数量显示

槽位里只在数量超过 99 时接管绘制，悬停提示始终给出精确值：

| 数量 | 槽位里的显示 |
|---|---|
| ≤ 99 | 原版写法 |
| 100 ～ 9,999 | 带千位分隔符的精确值，如 `1,234` |
| 10,000 ～ 999,999 | `10.0k`、`123k`、`999k` |
| ≥ 1,000,000 | `1.00m`、`2.50m` |

缩写一律向下截断，不会显示得比实际多。数字按字体宽度缩放到正好塞进 16 像素格子，并贴住格子右下角。

悬停提示写成「当前 / 上限」，例如 `数量：12,345 / 32,768`。精妙系列界面里角标与提示框都让给精妙自己绘制，避免两套数字叠在一起。

## 合成与容器

### 批量合成

Shift 点击合成结果时，先算「合成格最少的那一格够合几次」与「背包还能放几次」，取较小值，然后一次扣料、一次分配产物：

- 3×3 工作台与 2×2 背包合成都生效。
- 带剩余物品的配方（桶装牛奶、药水之类）同样批量：扣完料按次数把桶、瓶补回合成格。
- 不自动补料：合成格用完就停，与原版行为一致。
- 只作用于原版合成界面。精妙、应用能源 2 的合成界面走各自的实现，不受影响。

### 容器上限

箱子、漏斗、熔炉、双层箱、随机化容器的上限都跟随配置值，不再停在 64 / 99。

### 拆掉容器

一格超过 128 个的堆叠落成**一个**物品实体；不然原版会把它拆成约 count/20 个实体——一个 32768 的堆叠约 1600 个掉落物，塞满的箱子拆掉会让实体数量瞬间暴涨，造成卡顿甚至卡死服务器。

## 槽位兼容

原版容器自动跟随。其它模组分两类：继承 `Slot` / `Container` 的跟上；自己实现库存接口的由 `raiseModdedSlotLimits` 统一抬高（只升不降）。

| 模组 | 版本 | 修的是什么 |
|---|---|---|
| 通用机械 | 10.7.19.x | 机器槽位与界面槽默认写死 99，跟随配置 |
| 应用能源 2 | 19.2.x | 终端槽位停在 64、`InternalInventory` 默认 99；合成产物写入背包或光标时不再被拆成 99 一组 |
| 精妙核心 / 存储 / 背包 | 1.5.x / 1.6.x / 3.26.x | 合成升级的 9 格合成格停在 99；容器槽上限与界面数量由精妙自己管理，本模组不接管 |
| 更多熔炉 | 4.3.x | 输出槽上限写死 64，跟随配置 |

补丁写在独立的 `superstack.compat.mixins.json`，`required: false`：目标类不存在时只记一行日志，不影响启动。

## 配置

配置文件 `config/superstack-common.toml`，改完重启生效。

| 配置项 | 默认 | 范围 | 说明 |
|---|---|---|---|
| `maxStackSize` | 32768 | 64～1,000,000 | 物品堆叠上限 |
| `scaleSmallStacks` | true | — | 小堆叠按比例放大（16 → 8192），关掉则一律用上限 |
| `minimumStackSizeThreshold` | 1 | 0～64 | 原上限不超过该值的不放大 |
| `countDisplay` | COMPACT | COMPACT / EXACT | 槽位数量显示方式 |
| `abbreviateFrom` | 10000 | 1,000～1,000,000 | 从多少数量开始缩写 |
| `tooltipExactCount` | true | — | 悬停时显示精确数量 |
| `raiseModdedSlotLimits` | true | — | 是否抬高其它模组的物品槽上限 |

## 常见问题

**卸载后会怎样？**
存档里超过原版上限的堆叠在读取时会被原版校验拒绝，日志记一条 `Tried to load invalid item`，那些物品会消失。卸载前先把大堆叠拆回 64。

**服务端装了、客户端没装？**
客户端仍按 64 显示与操作，放不进去的部分由服务端拒绝，表现为「点了没反应」。两端都要装。

**为什么一叠不是 32768？**
16 上限的物品按比例放大成 8192。想要全部 32768，把 `scaleSmallStacks` 关掉。

**精妙界面里看不到本模组的角标？**
精妙自己会画缩写数量，本模组在那类界面里主动让位。

**合成还是慢？**
只有原版工作台与 2×2 做了批量。精妙、应用能源 2 的合成界面走它们自己的逐次逻辑，本模组不改。

**创造模式物品栏有问题吗？**
创造页签按组件值分组，上限放大后同一物品仍只有一个条目，属于正常。

## 兼容性

| 场景 | 表现 |
|---|---|
| 原版容器 / 漏斗 / 熔炉 | 上限跟随配置 |
| KubeJS | 它的 `remove_slot_limit` 默认透传，不会把上限压回去 |
| 一键背包整理 Next | 读 `ItemStack.getMaxStackSize()`，放不下的留在原地 |
| 精妙背包「整理背包」 | 自带「不超过物品上限」保护；实测 `maxStackSize = 10000`、27 格：关掉 `raiseModdedSlotLimits` 时 270000 → 2673，开着时 270000 → 270000 |
| 功能性存储 | 抽屉容量按物品上限换算，会随本模组放大，属预期 |
| 沉浸工程 / EnderIO / 机械动力 | 这些模组自建库存接口并写死了上限，本模组不改 |

本模组会改变存档内容，建议先备份或在测试存档里试。

## 开发者

### 实现方式

| 位置 | 做法 |
|---|---|
| `ExtraCodecsMixin` | 把 `ExtraCodecs.intRange(1, 99)` 的上界换成配置值，同时覆盖 `max_stack_size` 组件与 `ItemStack.CODEC` |
| `ModBusEvents` | `ModifyDefaultComponentsEvent` 给每个物品写入新的 `max_stack_size`，跳过有耐久度与原本堆 1 个的 |
| `ContainerMixin` / `SlotMixin` | `getMaxStackSize()` 返回 99 时换成配置值，其它值原样保留 |
| `ItemStackHandlerMixin` / `ComponentItemHandlerMixin` | 槽位上限受 `raiseModdedSlotLimits` 控制，只升不降 |
| `CraftingMenuMixin` / `InventoryMenuMixin` + `SuperStackBulkCraft` | 批量合成：算次数、一次扣料、一次分配 |
| `BulkCraftMath` | 纯算术：可用量、次数、产出、空间、分块与守恒校验，带 JUnit 测试 |
| `ContainersDropStackMixin` | 大堆叠拆容器时落成单个实体 |
| `GuiGraphicsMixin` / `CountFormat` | 数量缩写与缩放绘制 |
| `ClientEvents` | 提示框补一行「当前 / 上限」 |
| `mixin/compat/*` | 按模组划分的槽位补丁，独立配置、缺失时跳过 |

### 构建

需要 JDK 21：

```
gradlew build     # 产物在 build/libs/SuperStack-1.21.1-21.1-*.jar
gradlew test      # BulkCraftMath 的 8 个单元测试
```

## 更新日志

### 21.1-1.0.5

- Shift 点击合成结果改成一次合完：先算「材料够几次」与「背包还能放几次」取较小值，再一次性扣料与分配。原来每轮都要重查配方、扣材料、塞背包，一格三万多材料会在同一个 tick 里跑三万多轮，表现为卡死或联机超时。
- 9 组铁（32768 × 9）合铁块现在点一次直接出 32768 个，不再触发服务端 tick 超时。
- 带剩余物品的配方（桶装牛奶之类）也批量：按次数把桶、瓶补回合成格。
- 提示框数量改成「当前 / 上限」，例如 `数量：12,345 / 32,768`。
- 槽位数字改为贴格子右下角缩放，不再往格子上方飘。
- 更多熔炉输出槽上限跟随配置，不再卡 64。
- 补上 `pack.mcmeta`，不再被当成「缺少元数据」。
- 拆掉箱子、桶、熔炉、漏斗时，一格大堆叠落成一个实体；此前 32768 的堆叠会散成约 1600 个掉落物。

<details>
<summary><b>21.1-1.0.4</b></summary>

- 应用能源 2 的槽位补丁改成直接补 `getSlotLimit`。1.0.3 把补丁打在只实现接口、没有声明该方法的类上，装有应用能源 2 时会在启动阶段抛错。

</details>

<details>
<summary><b>21.1-1.0.3</b></summary>

- 应用能源 2 的合成终端（含无线终端与综合工作终端）里，产物写进背包或光标时不再被 `InternalInventory` 的默认上限拆成 99 一组。

</details>

<details>
<summary><b>21.1-1.0.2</b></summary>

- 精妙系列合成升级的 9 格合成格不再卡 99，补丁从容器槽位移到 `StatefulComponentItemHandler#getSlotLimit`。

</details>

<details>
<summary><b>21.1-1.0.1</b></summary>

- 容器与槽位上限跟着配置走，不再停在 99。
- 通用机械与应用能源 2 的槽位不再停在 64 / 99。
- 精妙系列界面不再叠加本模组的数量角标。
- 拆掉这些容器时掉落不再拆成一地 64 一组。
- 对应模组没装时补丁自动跳过。

</details>

<details>
<summary><b>21.1-1.0.0</b></summary>

- 可配置的物品堆叠上限（64～1,000,000，默认 32768），小堆叠按比例放大并可设阈值跳过。
- 数量显示：100～9,999 精确、10,000 起缩写，悬停给精确值。

</details>

## 许可证与署名

[MIT](./LICENSE) © 2026 zaixiayesheng。图标由本项目自行绘制。

## 反馈

问题请在 [Issues](https://github.com/zaixiayesheng/SuperStack/issues) 提交，附上 Minecraft 与 NeoForge 版本、本模组版本、`logs/latest.log`，以及涉及的容器或合成流程。
