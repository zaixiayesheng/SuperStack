<h1 align="center">SuperStack</h1>

<h3 align="center">超级堆叠</h3>

<div align="center">

Raises the item stack limit above 64 and makes containers, slots, world saving and network sync agree on the new cap. This is the 1.20.1 / Forge 47.4 build.

Chinese version: [README.md](./README.md)

[![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-green.svg?style=flat-square)](https://www.minecraft.net)
[![Forge 47.4](https://img.shields.io/badge/Forge-47.4+-orange.svg?style=flat-square)](https://files.minecraftforge.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](./LICENSE)

</div>

---

## What it is

Vanilla caps stacks in three places, and changing only one of them does not work:

- **Item cap**: `Item.maxStackSize` defaults to 64 and is a `final` field.
- **Save and network**: 1.20.1 writes the count as a **byte** (`ItemStack.save()` uses `putByte("Count", ...)` and `FriendlyByteBuf.writeItemStack` uses `writeByte`), so anything above 127 overflows into a negative number or zero.
- **Container cap**: `Container.getMaxStackSize()` returns 99, which every container that does not override it inherits.

This mod raises all three to a configurable value (default 32768) and replaces the slot counter with an abbreviated number plus an exact count in the tooltip.

## Install

| File | Required | Notes |
|---|---|---|
| `SuperStack-1.20.1-1.0.0.jar` | yes | The mod, single jar |
| Minecraft 1.20.1 + Forge 47.4+ | yes | Requires Java 17 |

- Mekanism 10.4.16.x, Applied Energistics 2 15.4.x and Sophisticated Core / Storage / Backpacks are **optional**: their patches only load when the mod is present, otherwise a single log line is written.
- Install on both client and server, or the two sides disagree about the stack limit.

## Stack limit

`maxStackSize` sets the item cap, default 32768 (range 64-1,000,000).

- Items with durability (tools, armour, Meka gear) are left alone.
- Items that only stack to 1 are left alone (`minimumStackSizeThreshold = 1`).
- Everything else scales as `vanilla cap x maxStackSize / 64`: ender pearl 16 becomes 8192, egg 16 becomes 8192, potions stay at 1.
- Turning `scaleSmallStacks` off makes every item use the full cap instead.

On 1.20.1 the count is stored as an int in NBT (the key is still `Count`, only the tag type changes from byte to int) and synced as a VarInt, so a stack of 32768 survives a save, a reload and multiplayer.

## Number display

The vanilla counter is only taken over above 99:

| Count | Shown in the slot |
|---|---|
| 99 or less | vanilla |
| 100 - 9,999 | exact with separators, `1,234` |
| 10,000 - 999,999 | `10.0k`, `123k`, `999k` |
| 1,000,000 and up | `1.00m`, `2.50m` |

Units are truncated, never rounded up. The text is scaled to fit the 16-pixel slot and the tooltip always shows the exact number.

## Config

`config/superstack-common.toml`, restart required.

| Option | Default | Range | Purpose |
|---|---|---|---|
| `maxStackSize` | 32768 | 64-1,000,000 | Item stack cap |
| `scaleSmallStacks` | true | - | Scale small stacks by their vanilla ratio |
| `minimumStackSizeThreshold` | 1 | 0-64 | Skip items whose vanilla cap is at or below this |
| `countDisplay` | COMPACT | COMPACT / EXACT | Slot counter style |
| `abbreviateFrom` | 10000 | 1,000-1,000,000 | Where k / m suffixes start |
| `tooltipExactCount` | true | - | Show the exact count in the tooltip |
| `raiseModdedSlotLimits` | true | - | Also raise other mods' item slot limits |

## Slots in other mods

Vanilla containers (chests, backpacks, crafting grids, hoppers) follow the mod through `Slot#getMaxStackSize`, while Forge's `ItemStackHandler` implementations are raised by `raiseModdedSlotLimits` (only ever upwards).

| Mod | Patch site | What it fixes |
|---|---|---|
| Mekanism | `BasicInventorySlot#getLimit` | Machine and GUI slots hard-coded to 99 |
| Applied Energistics 2 | `AppEngInternalInventory#getSlotLimit`, `InternalInventoryItemHandler` | Network inventory stuck at 64 / 99 |
| Applied Energistics 2 (crafting terminals) | `PlayerInternalInventory` / `CarriedItemInventory` gain `getSlotLimit` | Crafted output split into 99s when moved to the inventory or cursor |
| Sophisticated Core / Storage / Backpacks | `InventoryHandler#getSlotLimit` | Storage slots stuck at 64 (implemented as a multiple of 64) |

All of these live in a separate `superstack.compat.mixins.json` with `required: false`: if a target class is missing, one log line is written and startup continues.

Mixin 0.8.5 cannot inject into interfaces, so 1.20.1 does not patch `Container.getMaxStackSize()`; containers are handled at the `Slot` level instead.

## Sorting mods

This mod never moves items; it only keeps the limits consistent. When they disagree, a sorting pass that stages stacks in a 99-capped handler truncates them.

| Sorting mod | Result |
|---|---|
| Inventory Sorter Next | Unaffected; it reads `ItemStack.getMaxStackSize()` and leaves what does not fit |
| Sophisticated sorting | Unaffected by default; it guards against exceeding the item cap |

## FAQ

- **What happens if I uninstall it?** Stacks above the vanilla cap are rejected when the world loads and logged as `Tried to load invalid item`; those items are gone. Split large stacks back to 64 first.
- **Server only, client without the mod**: the client still shows and moves 64-sized stacks, and the server refuses the rest, which looks like "clicking does nothing".
- **No counter in Sophisticated screens**: those screens draw their own count, so this mod steps aside.
- **Crafting grids still stop at 99**: `Container` is an interface on 1.20.1 and Mixin 0.8.5 cannot inject into it, so grids are raised through `Slot`. A mod whose slot class overrides `getMaxStackSize` needs its own patch.

## Compatibility

- With Mekanism, Applied Energistics 2 and Sophisticated installed, slot limits follow this mod; breaking those containers no longer scatters 64-sized piles.
- Test in a new world or a backup: this mod changes saved data by design.

## For developers

### Implementation

| Site | Behaviour |
|---|---|
| `ItemMixin` | `@Shadow @Final private int maxStackSize`, returns the scaled value from `getMaxStackSize` |
| `ItemStackMixin` | Reads `Count` as an int when present; writes an int when the count exceeds `Byte.MAX_VALUE` |
| `FriendlyByteBufMixin` | Rewrites `writeItemStack` / `readItem` to carry the count as a VarInt |
| `SlotMixin` | Replaces a returned 99 with the configured cap |
| `ItemStackHandlerMixin` | Forge class (`remap = false`), gated by `raiseModdedSlotLimits` |
| `GuiGraphicsMixin` / `CountFormat` | Takes over the counter above 99: abbreviation, exact form, scaling |
| `ClientEvents` | Adds the exact count line to tooltips |
| `mixin/compat/*` | Six per-mod slot patches in a separate, non-required config |

### Build

JDK 17 and a Forge 47.4 development environment, `gradlew build`, output in `build/libs/`. The 1.20.1 jars of Mekanism, Applied Energistics 2 and Sophisticated Core go into `libs/` as `compileOnly` so the Mixin annotation processor can resolve the string targets.

## Changelog

### 1.0.0

- First Forge 1.20.1 build with feature parity to the 1.21.1 line: configurable cap (default 32768), scaled small stacks, exact and abbreviated counters.
- 1.20.1 stores the count as a byte, so the NBT path uses an int and the network path uses a VarInt; values above 127 no longer overflow.
- Carries the compatibility work from the 1.21.1 releases: Mekanism, AE2 and Sophisticated slots follow the cap, AE2 crafted output is no longer split into 99s, and Sophisticated screens keep their own counter.
- Sophisticated screens no longer get this mod's counter; breaking those containers no longer drops 64-sized piles.

## License and feedback

[MIT](./LICENSE) · by zaixiayesheng · [issues](https://github.com/zaixiayesheng/SuperStack/issues)

Please include your Minecraft and Forge version, the mod version, `logs/latest.log`, and the container involved.
