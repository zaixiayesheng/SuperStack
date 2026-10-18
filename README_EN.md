<h1 align="center">SuperStack</h1>

<h3 align="center">超级堆叠</h3>

<div align="center">

Vanilla caps stacks in two places: the <code>max_stack_size</code> component defaults to 64 and saved counts are clamped to 99, and <code>Container.getMaxStackSize()</code> returns 99 by default.
This mod replaces both with a configurable value (32768 by default) and abbreviates slot counts while keeping the exact number on hover.

Chinese version: [README.md](./README.md)

[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-green.svg?style=flat-square)](https://www.minecraft.net)
[![NeoForge 21.1](https://img.shields.io/badge/NeoForge-21.1+-orange.svg?style=flat-square)](https://neoforged.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](./LICENSE)

</div>

---

## What it fixes

Vanilla caps stacks in two places.

The first is the item itself: the `max_stack_size` component defaults to 64, and counts saved to disk are additionally clamped to 99 by `ExtraCodecs.intRange(1, 99)`.

The second is the container: `Container.getMaxStackSize()` returns 99 by default, and every container that does not override it inherits that — crafting grids, chests, your inventory. Raising the item limit alone still leaves you with 99 per slot in a crafting table.

The count display is the other annoyance. Vanilla draws the number right-aligned without scaling, so four digits already stick out of a 16 pixel slot. The usual workaround is abbreviating to `1K`, which turns everything from 1000 to 1999 into `1K`.

So this mod does three things: raise the item limit, raise the container limit, and abbreviate slot counts while keeping the exact number on hover. Installation is dropping the jar into `mods/`; it needs NeoForge 21.1+ and Java 21.

## Config

File: `config/superstack-common.toml`. Restart the game after editing.

| Option | Default | Description |
|---|---|---|
| `maxStackSize` | 32768 | Item stack size (64 - 1000000) |
| `scaleSmallStacks` | true | Scale items with a smaller limit proportionally: original limit × `maxStackSize` ÷ 64 (ender pearl 16 → 8192) |
| `minimumStackSizeThreshold` | 1 | Items whose original limit is at or below this are left alone (by default only unstackables such as tools) |
| `countDisplay` | COMPACT | How counts are drawn in slots, see the next section |
| `abbreviateFrom` | 10000 | When abbreviation starts. Matches Sophisticated Backpacks by default: 1000 - 9999 still show `1,234`, `10.0k` starts at 10000 |
| `tooltipExactCount` | true | Show the exact count in the tooltip on hover (only above 99) |
| `raiseModdedSlotLimits` | true | Also raise slot limits of other mods (`ItemStackHandler` / `ComponentItemHandler`). On by default — turning it off can leave a sorting flow unable to fit a full stack, see "Compatibility with sorting mods" |

## Count display

Vanilla drawing is only taken over above 99, following Sophisticated Backpacks:

| Count | Shown in the slot |
|---|---|
| ≤ 99 | whatever vanilla does |
| 100 - 9999 | exact number with separators: `1,234` |
| 10000 - 999999 | `10.0k`, `19.9k`, `123k`, `999k` |
| ≥ 1000000 | `1.00m`, `2.50m` |

Units are always truncated, never rounded up, so the display never overstates the amount. Text is scaled to the current font width so it fits a 16 pixel slot, still anchored to the bottom right. However short the abbreviation gets, hovering always shows the exact count in the tooltip. `countDisplay = "EXACT"` switches to always showing exact numbers, scaled to fit as well.

## Compatibility with sorting mods

This mod only changes stack limits and count rendering — it does not move items. What happens in practice depends on whether slot limits agree:

| Where | Limit |
|---|---|
| Vanilla containers (chests, inventory, crafting grid, hoppers, ...) | follows `maxStackSize` |
| Other mods' item slots (`ItemStackHandler`, `ComponentItemHandler`) | controlled by `raiseModdedSlotLimits`, on by default |

When the two disagree, a sorting mod moving a full stack can run out of room; whether the leftover is handed back depends on that mod's own handling.

Measured with `maxStackSize = 10000`, 27 slots, using Sophisticated Backpacks' "sort inventory":

```
raiseModdedSlotLimits = false    270000 → 2673        267327 missing
raiseModdedSlotLimits = true     270000 → 270000      nothing missing
```

The difference comes from the sorting flow copying slot contents into an item handler whose default limit is 99, so a large stack can only be written back as 99; with `raiseModdedSlotLimits` on, the limit follows the config. That is why it is on by default.

| Sorting mod | Behaviour | Notes |
|---|---|---|
| Inventory Profiles Next | unaffected | simulates locally and sends vanilla clicks; it reads `ItemStack.getMaxStackSize()`, so it follows this mod, and leftovers stay where they are |
| Sophisticated Backpacks sorting | unaffected by default | see the measurement above; it guards against exceeding the item limit and truncates when the slot limit is lower |
## How it works

| Class | What it does |
|---|---|
| `ExtraCodecsMixin` | Replaces the upper bound of `ExtraCodecs.intRange(1, 99)`. Vanilla only uses that range in two places — the `max_stack_size` component and the count field of `ItemStack.CODEC` — both of which are exactly what needs changing |
| `ModBusEvents` | Sets the new `max_stack_size` on every item via `ModifyDefaultComponentsEvent`; items with durability, or that stack to 1, are left alone |
| `ContainerMixin` / `SlotMixin` | Replace the 99 returned by `getMaxStackSize()` with the configured value, leaving any other value untouched |
| `GuiGraphicsMixin` / `CountFormat` | Take over drawing above 99: abbreviate, format, scale and align |
| `ClientEvents` | Adds the exact count line to the tooltip |
| `ItemStackHandlerMixin` / `ComponentItemHandlerMixin` | Only active with `raiseModdedSlotLimits` on, and only ever raise a limit, never lower it |

Nothing here turns limits into absurd numbers — vanilla's 99 / 64 defaults are the only ones replaced, values set by other mods stay as they are.

## Compatibility

- Coexists with KubeJS: its `remove_slot_limit` / `default_max_stack_size` pass values through by default and will not push the limit back down.
- Sorting-mod behaviour is covered under "Compatibility with sorting mods".
- Testing on a new world is recommended: existing oversized stacks will be rejected by vanilla validation once this mod is removed.

## Building

Needs JDK 21.

```bash
gradlew build
```

Output lands in `build/libs/`.

## Changelog

### 1.0.0

- Configurable stack limit (64 - 1000000, default 32768); smaller stacks scale proportionally with an optional threshold
- Count display: exact up to 9999, abbreviated above, exact value on hover
- Container and slot limits follow the configured value instead of stopping at 99
- Mekanism, Applied Energistics 2 and the Sophisticated series no longer stay at 64 or 99
- AE2 crafting terminals (wireless and comprehensive work terminal included) no longer split crafted output into stacks of 99
- The 9-slot crafting grid in the Sophisticated crafting upgrade no longer stops at 99
- Sophisticated GUIs no longer get a second count overlay from this mod
- Breaking those containers no longer scatters contents as 64-sized stacks
- Compatibility patches are skipped automatically when the related mod is absent
## License

[MIT](./LICENSE) · by zaixiayesheng · [issues](https://github.com/zaixiayesheng/SuperStack/issues)
