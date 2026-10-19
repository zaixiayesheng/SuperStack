<p align="center"><img src="logo.png" width="160" alt="SuperStack logo"></p>

<h1 align="center">超级堆叠</h1>

<h3 align="center">SuperStack</h3>

<div align="center">

把物品的堆叠上限从 64 提上去，并让容器、槽位、存档与网络同步都认这个新上限。这是 1.20.1 / Forge 47.4 的构建。

English version: [README_EN.md](./README_EN.md)

[![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-green.svg?style=flat-square)](https://www.minecraft.net)
[![Forge 47.4](https://img.shields.io/badge/Forge-47.4+-orange.svg?style=flat-square)](https://files.minecraftforge.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](./LICENSE)

</div>

---

## 这是什么

原版把堆叠卡在三个地方，只改其中一个都没用：

- **物品上限**：`Item.maxStackSize` 默认 64，且是 `final` 字段。
- **存档与网络**：1.20.1 把数量写成 **byte**（`ItemStack.save()` 的 `putByte("Count", …)`，`FriendlyByteBuf.writeItemStack` 也是 `writeByte`），超过 127 会直接溢出成负数或 0。
- **容器上限**：`Container.getMaxStackSize()` 默认返回 99，没自己重写这个方法的容器都吃这个值。

本模组把这三处一起抬到可配置值（默认 32768），并把槽位里的数量显示改成「缩写 + 悬停看精确值」。

## 安装

| 文件 | 必需 | 说明 |
|---|---|---|
| `SuperStack-1.20.1-20.1-1.0.1.jar` | 是 | 本模组，单个 jar |
| Minecraft 1.20.1 + Forge 47.4+ | 是 | 运行平台（需要 Java 17） |

- 通用机械 10.4.16.x、应用能源 2 15.4.x、精妙核心 / 存储 / 背包都是**可选**的：装了才启用对应槽位补丁，没装自动跳过并在日志里记一行。
- 客户端与服务端都要装，否则两边对堆叠上限的认知不一致。

## 堆叠上限

`maxStackSize` 控制物品上限，默认 32768（范围 64～1000000）。

- 有耐久度的物品（工具、护甲、Meka 装备）不动。
- 原本只能堆 1 个的物品不动（`minimumStackSizeThreshold = 1`）。
- 其余物品按 `原上限 × maxStackSize ÷ 64` 等比放大，例如末影珍珠 16 → 8192、鸡蛋 16 → 8192、药水 1 不变。
- 关掉 `scaleSmallStacks` 则一律按 `maxStackSize` 处理。

1.20.1 的数量按 int 写进 NBT（字段名仍是 `Count`，类型从 byte 换成 int），网络同步改写为 VarInt，因此 32768 存盘再读回仍是 32768，联机也不会被截断。

## 数量显示

只在数量超过 99 时接管绘制：

| 数量 | 槽位里显示 |
|---|---|
| ≤ 99 | 原版写法 |
| 100 ～ 9999 | 精确数字带千位分隔符，如 `1,234` |
| 10000 ～ 999999 | `10.0k`、`123k`、`999k` |
| ≥ 1000000 | `1.00m`、`2.50m` |

单位一律截断不四舍五入，文字按字体宽度缩放到塞进 16 像素格子；悬停时提示框给出精确数量。

## 配置

配置文件 `config/superstack-common.toml`，改完重启生效。

| 配置项 | 默认 | 范围 | 说明 |
|---|---|---|---|
| `maxStackSize` | 32768 | 64～1000000 | 物品堆叠上限 |
| `scaleSmallStacks` | true | — | 小堆叠按比例放大，关掉则一律用上限 |
| `minimumStackSizeThreshold` | 1 | 0～64 | 原上限不超过该值的不放大 |
| `countDisplay` | COMPACT | COMPACT / EXACT | 槽位数量显示方式 |
| `abbreviateFrom` | 10000 | 1000～1000000 | 多少数量开始用 k / m 缩写 |
| `tooltipExactCount` | true | — | 悬停时显示精确数量 |
| `raiseModdedSlotLimits` | true | — | 是否同时抬高其它模组的物品槽上限 |

## 与其它模组的槽位

原版容器（箱子、背包、合成格、漏斗）通过 `Slot#getMaxStackSize` 跟着本模组走；Forge 的 `ItemStackHandler` 由 `raiseModdedSlotLimits` 统一抬高（只升不降）。

| 模组 | 补丁位置 | 修的是什么 |
|---|---|---|
| 通用机械 | `BasicInventorySlot#getLimit` | 机器内部槽与界面槽默认写死 99 |
| 应用能源 2 | `AppEngInternalInventory#getSlotLimit`、`InternalInventoryItemHandler` | 网络库存停在 64 / 99 |
| 应用能源 2（合成终端） | `PlayerInternalInventory` / `CarriedItemInventory` 补 `getSlotLimit` | 合成产物进入背包或光标时被拆成 99 一组 |
| 精妙核心 / 存储 / 背包 | `InventoryHandler#getSlotLimit` | 容器槽停在 64（按 64 倍率实现） |

这些补丁写在独立的 `superstack.compat.mixins.json` 里，`required: false`：目标类不存在时只记一行日志，不影响启动。

Mixin 0.8.5 不支持往接口里注入，所以 1.20.1 没有给 `Container.getMaxStackSize()` 打补丁，容器侧统一在 `Slot` 层解决。

## 与整理类模组的兼容

本模组不参与任何物品搬运，只保证「各处的上限一致」。两边不一致时，整理流程把整叠物品搬进一个默认上限 99 的处理器再排回去就会截断。

| 整理模组 | 表现 |
|---|---|
| 一键背包整理Next | 不受影响，读取上限走 `ItemStack.getMaxStackSize()`，放不下的留在原地 |
| 精妙背包整理 | 默认不受影响；它自带「不超过物品上限」的保护 |

## 常见问题

- **卸载后会怎样**：存档里超过原版上限的堆叠在读取时会被拒绝并记 `Tried to load invalid item`，那些物品会消失；1.20.1 的数量若是被旧版按 byte 写坏的，甚至可能读成负数。卸载前先把大堆叠拆回 64。
- **服务端装了客户端没装**：客户端仍按 64 显示与操作，放不进去的部分由服务端拒绝，表现为「点了没反应」。
- **精妙界面里看不到本模组的角标**：精妙自己会画数量，本模组在那类界面主动让位。
- **合成格仍是 99**：1.20.1 的 `Container` 是接口，Mixin 0.8.5 无法注入，合成格靠 `Slot` 层抬高；如果某模组自建了槽类并覆写 `getMaxStackSize`，需要单独补。

## 兼容性

- 与通用机械、应用能源 2、精妙系列一起使用时槽位上限保持一致；拆掉这些容器时掉落不再按 64 一组炸开。
- 建议在新存档或备份后测试：这是会改变存档数据的模组。

## 开发者

### 实现方式

| 位置 | 做法 |
|---|---|
| `ItemMixin` | `@Shadow @Final private int maxStackSize`，在 `getMaxStackSize` 的 HEAD 返回缩放后的值 |
| `ItemStackMixin` | 读档时若 `Count` 是 int 就按 int 取值；存盘时数量超过 `Byte.MAX_VALUE` 就写 int |
| `FriendlyByteBufMixin` | `writeItemStack` / `readItem` 改成 VarInt 写读数量 |
| `SlotMixin` | 把 `getMaxStackSize()` 返回的 99 换成配置值 |
| `ItemStackHandlerMixin` | Forge 类（`remap = false`），受 `raiseModdedSlotLimits` 控制 |
| `GuiGraphicsMixin` / `CountFormat` | 数量 > 99 时接管绘制，负责缩写、精确写法与缩放对齐 |
| `ClientEvents` | 提示框补一行精确数量 |
| `mixin/compat/*` | 六个按模组划分的槽位补丁，独立配置、缺失时自动跳过 |

### 构建

需要 JDK 17 与 Forge 47.4 的开发环境，`gradlew build`，产物在 `build/libs/`。编译期需要把 Mekanism / 应用能源 2 / 精妙核心的 1.20.1 jar 放进 `libs/`（`compileOnly`，只为让 Mixin 注解处理器解析字符串目标）。

## 更新日志

### 20.1-1.0.1

- Shift 点击合成结果会一次把能合的全都合完。原版是一轮一轮地跑，每轮重新查配方、扣材料、塞背包，一格三万多个物品就要在同一个 tick 里跑三万多次，服务端直接超时；现在先算材料够合几次、背包还能放几次，取较小值，一次扣材料、一次分配产物。
- 9 组铁（32768 × 9）合成铁块点一次直接出 32768 个铁块。
- 带「剩余物品」的配方（桶装牛奶之类）也批量处理：一次扣材料后按合成次数把容器物品补回合成格。
- 只作用于原版工作台与 2×2 合成；精妙、应用能源 2 等自建合成界面保持原样。
- 提示框里的数量改成「当前 / 上限」。
- 槽位上的数字改为贴格子右下角缩放。
- 更多熔炉（Iron Furnaces）的输出上限跟随本模组的堆叠上限，不再卡在 64；上一版这个补丁把静态方法 `tick` 一起写了进去，导致整条补丁注入失败，本版已修。
- 精妙系列（背包 / 箱子 / 桶）的槽位上限跟随配置值；此前槽位被写死的 64 卡住，一整叠 32768 会被拆成很多个 64。
- 玩家物品栏也跟随配置值。1.20.1 的 `Container.getMaxStackSize()` 默认是 64，物品栏、拾取与 `/give` 直接吃这个默认值，所以物品上限抬高后背包每格还是 64；本版把槽位与物品栏内部的合并上限一起换掉。
- 补上 `pack.mcmeta`，不再被资源包与数据包加载器当成「缺少元数据」。
- 箱子、漏斗、熔炉、双层箱等原版容器的上限也跟随配置值。1.20.1 里它们的 `getMaxStackSize()` 默认是 64，塞进去会被压回 64。
- 创造模式客户端发来的超过 64 的堆叠不再被服务端丢掉（1.20.1 的创造发包写死 `count <= 64`，超出的整包丢弃），拿起 32768 的东西不会在开箱子后变成空格。
- 更多熔炉自己的物品栏上限也修了（`TileEntityInventory` 自己声明了 `getMaxStackSize`），Forge 的物品栏包装层同样跟随配置值，模组机器用管道输入不再被卡在 64。
- 拆掉箱子、桶、熔炉、漏斗这类原版容器时，一格大堆叠现在落成**一个**物品实体，不再散成上千个。
- 背包满时关工作台，合成格里的内容会尽量塞回背包；塞不下的整叠掉落，不再拆成几十组 64。
- 修掉精妙合成格补丁与更多熔炉补丁的注入目标错误。

### 20.1-1.0.0

- 首个 Forge 1.20.1 版本，功能与 1.21.1 线对齐：可配置的堆叠上限（默认 32768）、小堆叠按比例放大、精确与缩写数量显示。
- 1.20.1 的数量是 byte，改写成 int 存 NBT、VarInt 走网络，超过 127 不再溢出。
- 带上 1.21.1 线的兼容修复：通用机械 / 应用能源 2 / 精妙系列的槽位跟着配置走，AE2 合成产物不再拆成 99 一组，精妙界面不叠角标。
- 精妙界面不显示本模组角标；拆容器时掉落不再按 64 一组。

## 许可证与反馈

[MIT](./LICENSE) · 作者 zaixiayesheng · [问题反馈](https://github.com/zaixiayesheng/SuperStack/issues)

提交问题时请附上 Minecraft 与 Forge 版本、本模组版本、`logs/latest.log`，以及涉及的容器。
