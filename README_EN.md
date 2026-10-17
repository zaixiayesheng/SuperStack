<h1 align="center">SuperStack</h1>

<h3 align="center">超级堆叠</h3>

<div align="center">

Raises the item stack size to a configurable value (**32768** by default) and makes containers actually accept it.
Slot counts are abbreviated, with the exact number on hover — the same approach Sophisticated Backpacks uses.

Chinese version: [README.md](./README.md)

[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-green.svg?style=flat-square)](https://www.minecraft.net)
[![NeoForge 21.1](https://img.shields.io/badge/NeoForge-21.1+-orange.svg?style=flat-square)](https://neoforged.net)
[![Java 21](https://img.shields.io/badge/Java-21-red.svg?style=flat-square&logo=openjdk&logoColor=white)](https://adoptium.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](./LICENSE)
[![Issues](https://img.shields.io/badge/Issues-GitHub-black.svg?style=flat-square&logo=github)](https://github.com/zaixiayesheng/SuperStack/issues)

</div>

<div align="center">

[What it fixes](#-what-it-fixes) ·
[Features](#-features) ·
[Install](#-install) ·
[Config](#-config) ·
[Count display](#-count-display) ·
[Will it eat my items?](#-will-it-eat-my-items-measured) ·
[How it works](#-how-it-works) ·
[Compatibility](#-compatibility) ·
[Building](#-building) ·
[Changelog](#-changelog) ·
[License & feedback](#-license--feedback)

</div>

---

## 📖 What it fixes

Vanilla caps stacks in two places:

1. **Item limit**: the `max_stack_size` component defaults to 64, and saved data is additionally clamped to 99 by `ExtraCodecs.intRange(1, 99)`.
2. **Container limit**: `Container.getMaxStackSize()` returns **99** by default, which covers every container that does not override it — crafting grids, chests, your inventory. Raising the item limit alone still leaves you with 99 per slot in a crafting table.

The count display is the other problem: vanilla draws the number right-aligned without scaling, so four digits already stick out of a 16 pixel slot. The usual workaround is abbreviating to `1K`, which turns everything from 1000 to 1999 into `1K`.

So this mod does three things: **raise the item limit, raise the container limit, and abbreviate slot counts while keeping the exact number on hover**.

## ✨ Features

- **Configurable limit**: 64 - 1000000, 32768 by default
- **Containers really accept it**: not just the item limit, the container's 99 goes away too
- **Small stacks scale proportionally**: ender pearl 16 → 8192; tools and other unstackables stay untouched
- **Readable counts**: 100 - 9999 show the exact number (`1,234`), abbreviation only starts at 10000 (`10.0k`), always truncated
- **Exact value always available**: the tooltip gets an extra line with the precise count
- **Aligned with other mods' slots**: optionally raises `ItemStackHandler` / `ComponentItemHandler` limits so sorting mods do not eat items
- **Save/load safe**: the upper bound of `ExtraCodecs.intRange(1, 99)` becomes the configured value — a stack of 10000 survives a save and reload

## 📥 Install

| File | Required | Notes |
|---|---|---|
| `SuperStack-1.21.1-1.0.0.jar` | ✅ | drop it into `mods/` |
| [NeoForge](https://neoforged.net) 21.1+ | ✅ | needs Java 21 |
| [Stackcraft](https://www.curseforge.com/minecraft/mc-mods/stackcraft) | ❌ | **do not install**, functionally duplicate, logs an incompatibility |

## ⚙️ Config

File: `config/superstack-common.toml`. Restart the game after editing.

| Option | Default | Description |
|---|---|---|
| `maxStackSize` | 32768 | Item stack size (64 - 1000000) |
| `scaleSmallStacks` | true | Scale items with a smaller limit proportionally: original limit × `maxStackSize` ÷ 64 (ender pearl 16 → 8192) |
| `minimumStackSizeThreshold` | 1 | Items whose original limit is at or below this are left alone (by default only unstackable things such as tools) |
| `countDisplay` | COMPACT | How counts are drawn in slots, see below |
| `abbreviateFrom` | 10000 | When abbreviation starts. Matches Sophisticated Backpacks by default: 1000-9999 still show `1,234`, `10.0k` starts at 10000 |
| `tooltipExactCount` | true | Show the exact count in the tooltip on hover (only above 99) |
| `raiseModdedSlotLimits` | true | Also raise slot limits of other mods (`ItemStackHandler` / `ComponentItemHandler`). **On by default — turning it off makes sorting mods lose items**, see below |

## 🔢 Count display

Vanilla drawing is only taken over above 99, following Sophisticated Backpacks:

| Count | Shown in the slot |
|---|---|
| ≤ 99 | whatever vanilla does |
| 100 - 9999 | exact number with separators: `1,234` |
| 10000 - 999999 | `10.0k`, `19.9k`, `123k`, `999k` |
| ≥ 1000000 | `1.00m`, `2.50m` |

- Units are always **truncated**, never rounded up, so the display never overstates the amount;
- Text is scaled to the current font width so it fits a 16 pixel slot, still anchored to the bottom right;
- However short the abbreviation gets, hovering always shows the exact count in the tooltip;
- `countDisplay = "EXACT"` switches to always showing exact numbers (scaled to fit as well).

## 🗃️ Will it eat my items? (measured)

This mod only changes limits and rendering — it does not touch any item movement logic. Whether something breaks comes down to whether slot limits agree everywhere:

| Where | Limit |
|---|---|
| Vanilla containers (chests, inventory, crafting grid, hoppers, ...) | follows `maxStackSize` |
| Other mods' item slots (`ItemStackHandler`, `ComponentItemHandler`) | controlled by `raiseModdedSlotLimits`, on by default → also follows it |

When the two disagree, a sorting mod moving a whole stack around runs out of room, and the leftover is either handed back (well written) or silently dropped (badly written).

Measured with `maxStackSize = 10000`, 27 slots × 10000, sorting with Sophisticated Sorter's "sort inventory":

```
raiseModdedSlotLimits = false    270000 → 2673        267327 items gone
raiseModdedSlotLimits = true     270000 → 270000      nothing lost
```

The reason: Sophisticated Sorter copies slot contents into a NeoForge `ItemStackHandler` and sorts them back, and that handler's limit **defaults to 99** (NeoForge's own default). A larger stack can only put 99 back, and the rest is gone. That is why the option is on by default.

| Sorting mod | Loses items? | Why |
|---|---|---|
| Inventory Profiles Next | No | It simulates locally first, then sends vanilla clicks to the server; its limit comes from `ItemStack.getMaxStackSize()` (so it follows this mod), and vanilla click logic leaves what does not fit where it is |
| Sophisticated Sorter | Only with `raiseModdedSlotLimits` off | See the measurement above. It has its own "never exceed the item limit" guard, which truncates when the slot limit lags behind |

Saving and loading is fine as well: vanilla clamps counts through `ExtraCodecs.intRange(1, 99)`, and this mod replaces the upper bound with the configured value — a stack of 10000 survives a save and reload in testing.

## 🔧 How it works

| Class | What it does |
|---|---|
| `ExtraCodecsMixin` | Replaces the upper bound of `ExtraCodecs.intRange(1, 99)`. Vanilla only uses that range in two places — the `max_stack_size` component and the count field of `ItemStack.CODEC` — both of which are exactly what needs changing |
| `ModBusEvents` | Sets the new `max_stack_size` on every item via `ModifyDefaultComponentsEvent`; items with durability, or that stack to 1, are left alone |
| `ContainerMixin` / `SlotMixin` | Replace the 99 returned by `getMaxStackSize()` with the configured value, leaving any other value untouched |
| `GuiGraphicsMixin` / `CountFormat` | Take over drawing above 99: abbreviate, format, scale and align |
| `ClientEvents` | Adds the exact count line to the tooltip |
| `ItemStackHandlerMixin` / `ComponentItemHandlerMixin` | Only active with `raiseModdedSlotLimits` on, and only ever raise a limit, never lower it |
| `SuperStackLog` | Logs the effective stack limit of each item, for checking what actually got applied |

Nothing here turns limits into absurd numbers — vanilla's 99 / 64 defaults are the only ones replaced, values set by other mods stay as they are.

## 🧩 Compatibility

- Functionally overlaps with **Stackcraft**; pick one (installing both logs an incompatibility).
- Coexists with KubeJS: its `remove_slot_limit` / `default_max_stack_size` pass values through by default and will not push the limit back down.
- Sorting mods: see the measurements above.
- Test on a **new world**: existing oversized stacks will be rejected by vanilla validation once this mod is removed.

## 🛠️ Building

Needs JDK 21.

```bash
gradlew build
```

Output lands in `build/libs/`.

## 📜 Changelog

### Unreleased

- `SuperStackLog`: logs the stack limit each item actually ended up with
- Config cache is now volatile, so hot reloads take effect immediately; a summary is logged on reload
- Minor client and GUI adjustments

### 1.0.0

- Configurable item stack limit (64 - 1000000, 32768 by default)
- Small stacks scale proportionally, with a configurable threshold
- Container / slot limits follow along, no more 99 cap
- Count display: exact (100 - 9999), abbreviated from 10000, configurable start
- Exact count on hover
- Optional raise of other mods' item slot limits, so sorting does not eat items

## 📄 License & feedback

- License: [MIT](./LICENSE)
- Author: zaixiayesheng
- Issues: [GitHub Issues](https://github.com/zaixiayesheng/SuperStack/issues)
