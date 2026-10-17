<h1 align="center">超级堆叠</h1>

<h3 align="center">SuperStack</h3>

<div align="center">

把物品的堆叠上限提高到可配置的值（默认 **32768**），并且让容器真的放得下。
数量显示走「槽位缩写 + 悬停看精确值」，和精妙背包一个路子。

English version: [README_EN.md](./README_EN.md)

[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-green.svg?style=flat-square)](https://www.minecraft.net)
[![NeoForge 21.1](https://img.shields.io/badge/NeoForge-21.1+-orange.svg?style=flat-square)](https://neoforged.net)
[![Java 21](https://img.shields.io/badge/Java-21-red.svg?style=flat-square&logo=openjdk&logoColor=white)](https://adoptium.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](./LICENSE)
[![Issues](https://img.shields.io/badge/Issues-GitHub-black.svg?style=flat-square&logo=github)](https://github.com/zaixiayesheng/SuperStack/issues)

</div>

<div align="center">

[它解决什么](#-它解决什么) ·
[特性](#-特性) ·
[安装](#-安装) ·
[配置](#-配置) ·
[数量显示](#-数量显示) ·
[会不会吞东西](#-会不会吞东西) ·
[实现方式](#-实现方式) ·
[兼容性](#-兼容性) ·
[构建](#-构建) ·
[更新日志](#-更新日志) ·
[许可与反馈](#-许可与反馈)

</div>

---

## 📖 它解决什么

原版有两处把堆叠卡死：

1. **物品上限**：物品的 `max_stack_size` 组件默认 64，而且存档读写时数量还被 `ExtraCodecs.intRange(1, 99)` 卡在 99 以内。
2. **容器上限**：`Container.getMaxStackSize()` 默认返回 **99**（合成格、箱子、背包里那些没自己重写这个方法的容器全吃这个值）。所以光把物品上限调大，工作台里还是只放得下 99 个。

数量显示也是坑：原版那个数字右对齐、不缩放，四位数就戳出 16 像素的格子了。常见做法是缩写成 `1K`，但那样 1000~1999 全变成 `1K`，看不出具体多少。

本模组对应做三件事：**放高物品上限、放高容器上限、把数量显示做成「槽位缩写 + 悬停精确」**。

## ✨ 特性

- **上限可调**：64 ~ 1000000，默认 32768
- **容器真的放得下**：不只是物品上限，容器槽位的 99 也一起换掉
- **小堆叠按比例放大**：末影珍珠 16 → 8192；工具这类原本堆 1 个的不动
- **数量显示不糊**：100 ~ 9999 显示精确数字（`1,234`），10000 起才缩写（`10.0k`），一律截断不四舍五入
- **始终能看精确值**：悬停提示框里补一行精确数量
- **和其它模组的槽位对齐**：可选放大 `ItemStackHandler` / `ComponentItemHandler` 的上限，避免整理类模组吞东西
- **存档往返安全**：把 `ExtraCodecs.intRange(1, 99)` 的上界换成配置值，10000 的堆叠存盘再读回来还是 10000

## 📥 安装

| 文件 | 必需 | 说明 |
|---|---|---|
| `SuperStack-1.21.1-1.0.0.jar` | ✅ | 放到 `mods/` 即可 |
| [NeoForge](https://neoforged.net) 21.1+ | ✅ | 需要 Java 21 |

## ⚙️ 配置

配置文件 `config/superstack-common.toml`，改完重启游戏生效。

| 配置项 | 默认 | 说明 |
|---|---|---|
| `maxStackSize` | 32768 | 物品堆叠上限（64 ~ 1000000） |
| `scaleSmallStacks` | true | 小堆叠物品按比例放大：原上限 × `maxStackSize` ÷ 64（末影珍珠 16 → 8192） |
| `minimumStackSizeThreshold` | 1 | 原上限不超过这个值的不放大（默认只有原本堆 1 个的，比如工具） |
| `countDisplay` | COMPACT | 槽位里的数量显示方式，见「数量显示」 |
| `abbreviateFrom` | 10000 | 数量到多少开始用单位缩写。默认和精妙背包一致：1000~9999 写 `1,234`，10000 起才是 `10.0k` |
| `tooltipExactCount` | true | 悬停时在提示框里显示精确数量（超过 99 才显示） |
| `raiseModdedSlotLimits` | true | 是否也放大其它模组的物品槽上限（`ItemStackHandler` / `ComponentItemHandler`）。**默认打开，关掉会让整理类模组丢东西**，见下 |

## 🔢 数量显示

数量超过 99 才接管原版的绘制，规则对着精妙背包来：

| 数量 | 槽位里显示 |
|---|---|
| ≤ 99 | 原版怎么写就怎么写 |
| 100 ~ 9999 | 精确数字，带千位分隔符：`1,234` |
| 10000 ~ 999999 | `10.0k`、`19.9k`、`123k`、`999k` |
| ≥ 1000000 | `1.00m`、`2.50m` |

- 单位一律**截断**不四舍五入（精妙也是截断），免得显示得比实际多；
- 文字按当前字体宽度缩放到正好塞进 16 像素的格子，仍然贴着右下角；
- 不管缩写得多简略，悬停时提示框里都有精确数量；
- `countDisplay = "EXACT"` 可以改成一律显示精确数字（同样会缩放塞进格子）。

## 🗃️ 会不会吞东西（实测）

本模组只改上限和显示，不碰任何物品搬运逻辑。会不会出问题，取决于「各处槽位上限是否一致」：

| 位置 | 上限 |
|---|---|
| 原版容器（箱子、背包、合成格、漏斗……） | 跟着 `maxStackSize` |
| 其它模组的物品槽（`ItemStackHandler`、`ComponentItemHandler`） | 由 `raiseModdedSlotLimits` 控制，默认打开 → 也跟着 |

两边不一致时，整理模组把一整叠搬来搬去就会装不下，装不下的部分要么被退回来（写得好），要么被吞掉（写得不好）。

实测（`maxStackSize = 10000`，27 格 × 10000，用精妙整理的「整理背包」）：

```
raiseModdedSlotLimits = false    270000 → 2673        少了 267327
raiseModdedSlotLimits = true     270000 → 270000      一个不少
```

原因是精妙整理整理时会把槽位内容复制进一个 NeoForge 的 `ItemStackHandler` 再排回去，而这个槽的上限**默认只有 99**（NeoForge 自己的默认值），大堆叠排回去时就只放得下 99，剩下的直接没了。所以这项默认打开。

| 整理模组 | 会不会吞 | 说明 |
|---|---|---|
| 一键背包整理Next | 不会 | 先在本地沙盒里模拟一遍，再发原版点击给服务端执行；读取上限用 `ItemStack.getMaxStackSize()`（跟着本模组走），放不下就留在原地 |
| 精妙整理 | 关掉 `raiseModdedSlotLimits` 会，默认不会 | 见上面的实测。它自己有「不超过物品上限」的保护，槽位上限跟不上时反而会截断 |

存档读写也没问题：原版靠 `ExtraCodecs.intRange(1, 99)` 限制数量，本模组把这个范围的上界换成配置值，实测 10000 的堆叠存盘再读回来还是 10000。

## 🔧 实现方式

| 位置 | 做法 |
|---|---|
| `ExtraCodecsMixin` | 把 `ExtraCodecs.intRange(1, 99)` 的上界换成配置值。原版只有两处用到这个范围：物品的 `max_stack_size` 组件和 `ItemStack.CODEC` 的 count 字段，正好都是要改的 |
| `ModBusEvents` | `ModifyDefaultComponentsEvent` 里给每个物品设置新的 `max_stack_size`；有耐久度的、原本堆 1 个的不动 |
| `ContainerMixin` / `SlotMixin` | 把 `getMaxStackSize()` 返回的 99 换成配置值，其它值原样保留 |
| `GuiGraphicsMixin` / `CountFormat` | 数量超过 99 时接管绘制，负责缩写、精确写法和缩放对齐 |
| `ClientEvents` | 提示框里补一行精确数量 |
| `ItemStackHandlerMixin` / `ComponentItemHandlerMixin` | 只在 `raiseModdedSlotLimits` 打开时生效，而且只升不降 |
| `SuperStackLog` | 把每个物品实际拿到的堆叠上限记进日志，排查「到底改没改到」时用 |

全程不把上限改成天文数字，只替换原版的 99 / 64 这些默认值，其它模组自己设的数值不动。

## 🧩 兼容性

- 和 KubeJS 共存没问题：它的 `remove_slot_limit` / `default_max_stack_size` 默认透传，不会把上限压回去。
- 和整理类模组的实测见「会不会吞东西」。
- 装了本模组后建议**新开存档**测试：已经存在的大堆叠物品在卸载本模组后会被原版校验拒绝。

## 🛠️ 构建

需要 JDK 21。

```bash
gradlew build
```

产物在 `build/libs/`。

## 📜 更新日志

### 未发布

- 新增 `SuperStackLog`：把实际生效的堆叠上限记进日志
- 配置缓存改成 volatile，热重载后立即生效，并在重载时打印一次汇总
- 客户端与 GUI 侧的小调整

### 1.0.0

- 可配置的物品堆叠上限（64 ~ 1000000，默认 32768）
- 小堆叠按比例放大，可设阈值跳过
- 容器 / 槽位上限跟着走，不再卡 99
- 数量显示：精确（100 ~ 9999）/ 单位缩写（10000 起）/ 缩写起点可调
- 悬停显示精确数量
- 可选放大其它模组的物品槽上限，避免整理时吞东西

## 📄 许可与反馈

- 许可：[MIT](./LICENSE)
- 作者：zaixiayesheng
- 问题反馈：[GitHub Issues](https://github.com/zaixiayesheng/SuperStack/issues)
