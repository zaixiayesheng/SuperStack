<p align="center"><img src="logo.png" width="160" alt="SuperStack logo"></p>

<h1 align="center">SuperStack</h1>

<h3 align="center">超级堆叠</h3>

<div align="center">

Raises the item stack limit above 64 and makes containers, slots, world saving and network sync agree on the new cap.

Chinese version: [README.md](./README.md)

[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-green.svg?style=flat-square)](https://www.minecraft.net)
[![NeoForge 21.1](https://img.shields.io/badge/NeoForge-21.1+-orange.svg?style=flat-square)](https://neoforged.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](./LICENSE)

</div>

---

## What it is

Vanilla caps stacks in three places, and changing only one of them does not work:

- **Item cap**: the `max_stack_size` component defaults to 64.
- **Save and network**: `ItemStack.CODEC` only accepts a count of 1-99, and `FriendlyByteBuf` writes the count with the same bound, so a raised cap would fail to save or sync.
- **Container cap**: `Container.getMaxStackSize()` returns 99, which every container that does not override it (crafting grids, chests, backpacks) inherits.

This mod raises all three to a configurable value (default 32768), and replaces the stack counter in slots with an abbreviated number plus an exact count in the tooltip, because the vanilla counter overflows a 16-pixel slot at four digits.

## Install

| File | Required | Notes |
|---|---|---|
| `SuperStack-1.21.1-1.0.4.jar` | yes | The mod, single jar |
| Minecraft 1.21.1 + NeoForge 21.1+ | yes | Requires Java 21 |

- Mekanism, Applied Energistics 2 and Sophisticated Core / Storage / Backpacks are **optional**: their slot patches only load when the mod is present, otherwise a single log line is written.
- Install on both client and server, or the two sides disagree about the stack limit.

## Stack limit

`maxStackSize` sets the item cap, default 32768 (range 64-1,000,000).

- Items with durability (tools, armour, Meka gear) are left alone.
- Items that only stack to 1 are left alone (`minimumStackSizeThreshold = 1`).
- Everything else scales as `vanilla cap x maxStackSize / 64`: ender pearl 16 becomes 8192, egg 16 becomes 8192, potions stay at 1. Turning `scaleSmallStacks` off makes every item use the full cap instead.

Saving and network sync are covered by the same rewrite: counts stored inside item NBT (for example the nine slots of an upgrade item) use an int in NBT and a VarInt on the wire, so a stack of 32768 survives a save and reload as 32768.

## Number display

The vanilla counter is only taken over above 99:

| Count | Shown in the slot |
|---|---|
| 99 or less | vanilla |
| 100 - 9,999 | exact with separators, `1,234` |
| 10,000 - 999,999 | `10.0k`, `19.9k`, `123k`, `999k` |
| 1,000,000 and up | `1.00m`, `2.50m` |

Units are truncated, never rounded up, so the display never claims more than the real count. The text is scaled to fit the 16-pixel slot, and the tooltip always shows the exact number.

## Config

`config/superstack-common.toml`, restart required.

| Option | Default | Range | Purpose |
|---|---|---|---|
| `maxStackSize` | 32768 | 64-1,000,000 | Item stack cap |
| `scaleSmallStacks` | true | - | Scale small stacks by their vanilla ratio (16 becomes 8192) |
| `minimumStackSizeThreshold` | 1 | 0-64 | Skip items whose vanilla cap is at or below this |
| `countDisplay` | COMPACT | COMPACT / EXACT | Slot counter style |
| `abbreviateFrom` | 10000 | 1,000-1,000,000 | Where k / m suffixes start |
| `tooltipExactCount` | true | - | Show the exact count in the tooltip |
| `raiseModdedSlotLimits` | true | - | Also raise other mods' item slot limits |

## Slots in other mods

Vanilla containers (chests, backpacks, crafting grids, hoppers) follow the mod. Custom slots fall into two groups: those extending vanilla `Slot` / `Container` follow automatically, while `ItemStackHandler` / `ComponentItemHandler` implementations are raised by `raiseModdedSlotLimits` (only ever upwards).

| Mod | Patch site | What it fixes |
|---|---|---|
| Mekanism | `BasicInventorySlot#getLimit` | Machine and GUI slots hard-coded to 99 |
| Applied Energistics 2 | `AppEngInternalInventory#getSlotLimit`, `InternalInventoryItemHandler`, `AppEngSlot` | Network inventory and terminal slots stuck at 64 / 99 |
| Applied Energistics 2 (crafting terminals) | `PlayerInternalInventory` / `CarriedItemInventory` gain `getSlotLimit` | Crafted output split into 99s when moved to the inventory or cursor |
| Sophisticated Core / Storage / Backpacks | `InventoryHandler#getSlotLimit`, `StatefulComponentItemHandler#getSlotLimit` | Storage slots stuck at 64 and the crafting upgrade's 9-slot grid stuck at 99 |

All of these live in a separate `superstack.compat.mixins.json` with `required: false`: if a target class is missing, one log line is written and startup continues.

## Sorting mods

This mod never moves items; it only keeps the limits consistent. When they disagree, a sorting pass that stages stacks in a 99-capped handler truncates them.

Measured with `maxStackSize = 10000`, 27 slots, Sophisticated Backpacks' "sort backpack":

```
raiseModdedSlotLimits = false    270000 -> 2673        lost 267327
raiseModdedSlotLimits = true     270000 -> 270000      nothing lost
```

| Sorting mod | Result |
|---|---|
| Inventory Sorter Next | Unaffected; it reads `ItemStack.getMaxStackSize()` and leaves what does not fit |
| Sophisticated sorting | Unaffected by default; it guards against exceeding the item cap |

## FAQ

- **What happens if I uninstall it?** Stacks above the vanilla cap are rejected when the world loads and logged as `Tried to load invalid item`; those items are gone. Split large stacks back to 64 first.
- **Server only, client without the mod**: the client still shows and moves 64-sized stacks, and the server refuses the rest, which looks like "clicking does nothing". Install on both sides.
- **No counter in Sophisticated screens**: those screens draw their own abbreviated count, so this mod steps aside to avoid overlapping text.
- **Creative menu looks odd**: entries are grouped by component value, so a raised cap still produces one entry per item, which is expected.

## Compatibility

- KubeJS coexistence works: its `remove_slot_limit` / `default_max_stack_size` pass through and do not push the cap back down.
- With Mekanism, Applied Energistics 2 and Sophisticated installed, slot limits follow this mod; breaking those containers no longer scatters 64-sized piles (since 1.0.1).
- Test in a new world or a backup: this mod changes saved data by design.

## For developers

### Implementation

| Site | Behaviour |
|---|---|
| `ExtraCodecsMixin` | Replaces the upper bound of `ExtraCodecs.intRange(1, 99)`, covering both the `max_stack_size` component and `ItemStack.CODEC`'s count |
| `ModBusEvents` | Writes the new `max_stack_size` for every item in `ModifyDefaultComponentsEvent`, skipping durability items and stack-1 items |
| `ContainerMixin` / `SlotMixin` | Replaces a returned 99 with the configured cap, other values pass through |
| `GuiGraphicsMixin` / `CountFormat` | Takes over the counter above 99: abbreviation, exact form, scaling |
| `ClientEvents` | Adds the exact count line to tooltips |
| `ItemStackHandlerMixin` / `ComponentItemHandlerMixin` | Gated by `raiseModdedSlotLimits`, only raise |
| `mixin/compat/*` | Six per-mod slot patches in a separate, non-required config |

### Build

JDK 21, `gradlew build`, output in `build/libs/`.

## Changelog

### 1.0.4

- The AE2 slot patch now adds `getSlotLimit` directly. 1.0.3 targeted classes that only implement the interface without declaring the method, which crashed startup when AE2 was installed.

<details>
<summary><b>1.0.3</b></summary>

- Crafted output in AE2 crafting terminals (including the wireless and the combined terminal) is no longer split into 99s when written to the inventory or cursor.

</details>

<details>
<summary><b>1.0.2</b></summary>

- The 9-slot crafting grid of the Sophisticated crafting upgrade no longer caps at 99: the patch moved from the slot to `StatefulComponentItemHandler#getSlotLimit`.

</details>

<details>
<summary><b>1.0.1</b></summary>

- Container and slot limits follow the config instead of stopping at 99.
- Mekanism, AE2 and Sophisticated slots no longer stop at 64 / 99.
- Sophisticated screens no longer get this mod's counter drawn on top.
- Breaking those containers no longer drops 64-sized piles.
- Patches are skipped when the matching mod is absent.

</details>

<details>
<summary><b>1.0.0</b></summary>

- Configurable item stack cap (64-1,000,000, default 32768), small stacks scaled by ratio with a threshold to skip.
- Number display: exact between 100 and 9,999, abbreviated above, exact count in the tooltip.

</details>

## License and feedback

[MIT](./LICENSE) · by zaixiayesheng · [issues](https://github.com/zaixiayesheng/SuperStack/issues)

Please include your Minecraft and loader version, the mod version, `logs/latest.log`, and the container or sorting flow involved.
