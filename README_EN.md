<p align="center"><img src="logo.png" width="160" alt="SuperStack logo"></p>

<h1 align="center">SuperStack</h1>

<h3 align="center">超级堆叠</h3>

<div align="center">

Raises the item stack limit from 64 to a configurable value (32,768 by default) and teaches containers, slots, saved data, network sync and crafting about the new limit.

Chinese version: [README.md](./README.md)

[![Release](https://img.shields.io/github/v/release/zaixiayesheng/SuperStack?filter=v21.1*&style=flat-square&label=Release&color=blue)](https://github.com/zaixiayesheng/SuperStack/releases)
[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-green.svg?style=flat-square)](https://www.minecraft.net)
[![NeoForge 21.1](https://img.shields.io/badge/NeoForge-21.1+-orange.svg?style=flat-square)](https://neoforged.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](./LICENSE)

[What it is](#what-it-is) · [Install](#install) · [Stack limit](#stack-limit) · [Count display](#count-display) · [Crafting and containers](#crafting-and-containers) · [Slot compatibility](#slot-compatibility) · [Configuration](#configuration) · [FAQ](#faq) · [Compatibility](#compatibility) · [Development](#development) · [Changelog](#changelog)

</div>

---

## What it is

Vanilla caps stacks in several places, and changing only one of them does nothing. This mod takes over all of them:

- **Item limit**: the `max_stack_size` data component defaults to 64.
- **Saved data**: `ItemStack.CODEC` only accepts counts of 1–99, so larger stacks are rejected when saving.
- **Network**: container packets write the count as a single byte, so 32768 comes back as 0.
- **Containers and slots**: `Container.getMaxStackSize()` returns 99 by default, and chests, backpacks and crafting grids all inherit it.
- **Crafting**: shift-clicking the result crafts one item per round in vanilla, so a grid holding tens of thousands of items runs tens of thousands of rounds inside a single tick.

The limit defaults to 32,768 (configurable 64–1,000,000). Small stacks scale proportionally (16 → 8,192); stackable-by-1 items and items with durability are left alone.

## Install

| File | Required | Notes |
|---|---|---|
| `SuperStack-<game version>-<version>.jar` | Yes | Download the matching build from Releases |
| Minecraft 1.21.1 + NeoForge 21.1+ | Yes | Needs Java 21 |

- Install on both client and server. With only one side, the other still uses 64 and rejects the overflow.
- Mekanism, Applied Energistics 2, Sophisticated, and Iron Furnaces are optional: their patches only load when the mod is present, otherwise a single log line is written.

## Stack limit

| Vanilla limit | With default settings |
|---|---|
| 64 | 32,768 |
| 16 | 8,192 |
| 8 | 4,096 |
| 1 | 1 (unchanged) |

The multiplier is `maxStackSize / 64`, i.e. ×512 by default. Rules:

- Items with durability (tools, armor, Meka gear) are untouched.
- Items whose vanilla limit is at or below `minimumStackSizeThreshold` (1 by default) are untouched.
- With `scaleSmallStacks` disabled, every item uses `maxStackSize`.
- The limit itself can go up to 2,147,483,647; 32,768 is only a conservative default.

Saving and network sync are rewritten as well: counts embedded in items (for example the nine slots carried by an upgrade) are stored as ints and synced as VarInts, so a stack of 32,768 survives a save/load round trip.

## Count display

The mod only takes over slot rendering when the count exceeds 99, and the tooltip always shows the exact number:

| Count | Slot rendering |
|---|---|
| ≤ 99 | vanilla |
| 100 – 9,999 | exact with thousands separators, e.g. `1,234` |
| 10,000 – 999,999 | `10.0k`, `123k`, `999k` |
| ≥ 1,000,000 | `1.00m`, `2.50m` |

Abbreviations are truncated, never rounded up. The text is scaled to fit the 16-pixel slot and anchored to its bottom-right corner.

The tooltip reads "current / limit", e.g. `Count: 12,345 / 32,768`. Inside Sophisticated screens both the overlay and the tooltip line defer to Sophisticated's own rendering, so the numbers never double up.

## Crafting and containers

### Bulk crafting

Shift-clicking the crafting result first computes how many crafts the scarcest grid stack allows and how many the inventory can hold, takes the smaller number, then consumes the ingredients once and distributes the output once:

- Works for the 3×3 crafting table and the 2×2 inventory grid.
- Recipes with remaining items (milk buckets, potions) are batched too: buckets and bottles are placed back into the grid after the batch.
- No auto-refill: the grid stops when it runs out, exactly like vanilla.
- Vanilla crafting screens only. Sophisticated and Applied Energistics 2 crafting screens keep their own logic.

### Container limits

Chests, hoppers, furnaces, double chests and randomizable containers all follow the configured limit instead of stopping at 64 / 99.

### Breaking containers

Any stack above 128 drops as **one** item entity. Otherwise vanilla splits it into roughly count/20 entities — a 32,768 stack becomes about 1,600 drops, which can flood the server with entities and cause stalls or a hard lock-up when a full chest is broken.

## Slot compatibility

Vanilla containers follow automatically. For other mods there are two cases: slots extending `Slot` / `Container` simply follow, while mods implementing their own inventory interface are raised through `raiseModdedSlotLimits` (raise only, never lower).

| Mod | Version | What is fixed |
|---|---|---|
| Mekanism | 10.7.19.x | Machine and GUI slots hardcode 99; they now follow the config |
| Applied Energistics 2 | 19.2.x | Terminal slots stop at 64 and `InternalInventory` defaults to 99; crafted output is no longer split into stacks of 99 when moved to the inventory or cursor |
| Sophisticated Core / Storage / Backpacks | 1.5.x / 1.6.x / 3.26.x | The crafting upgrade's 9 slots stop at 99; container slot limits and screen rendering are managed by Sophisticated itself |
| Iron Furnaces | 4.3.x | Output slot hardcodes 64; it now follows the config |

The patches live in a separate `superstack.compat.mixins.json` with `required: false`: when a target class is missing, only a log line is written and startup continues.

## Configuration

The file is `config/superstack-common.toml`; restart after editing.

| Option | Default | Range | Meaning |
|---|---|---|---|
| `maxStackSize` | 32768 | 64–2,147,483,647 | Item stack limit |
| `compatStackLimit` | 2147483647 | 1–2,147,483,647 | Slot limit shown to supported mods (Mekanism, AE2, Sophisticated, Iron Furnaces, Mystical Agriculture); follows the stack size by default, lower it only if a mod misbehaves |
| `scaleSmallStacks` | true | — | Scale small stacks (16 → 8,192); disable to use the limit for everything |
| `minimumStackSizeThreshold` | 1 | 0–64 | Items at or below this vanilla limit are not raised |
| `countDisplay` | COMPACT | COMPACT / EXACT | Slot count rendering style |
| `abbreviateFrom` | 10000 | 1,000–1,000,000 | Where abbreviations start |
| `tooltipExactCount` | true | — | Show the exact count in tooltips |
| `raiseModdedSlotLimits` | true | — | Raise slot limits of other mods |

## FAQ

**What happens if I uninstall it?**
Stacks above the vanilla limit are rejected when loading, the log records `Tried to load invalid item`, and those items are lost. Split big stacks back to 64 before removing the mod.

**Server has it, client does not?**
The client still shows and works with 64, and the server rejects the overflow — it looks like "clicking does nothing". Install on both sides.

**Why is a stack 8,192 instead of 32,768?**
Items with a vanilla limit of 16 are scaled proportionally. Disable `scaleSmallStacks` to use the full limit everywhere.

**Why is there no SuperStack overlay in Sophisticated screens?**
Sophisticated draws its own abbreviated counts there, so the mod steps aside.

**Crafting is still slow?**
Only vanilla crafting screens are batched. Sophisticated and Applied Energistics 2 keep their own per-item loops, which this mod does not change.

**Anything odd in the creative menu?**
Creative tabs group by component value, so each item still has a single entry — expected.

## Compatibility

| Scenario | Behaviour |
|---|---|
| Vanilla containers / hoppers / furnaces | Follow the configured limit |
| KubeJS | Its `remove_slot_limit` passes through and does not lower the limit |
| Inventory sorting mods | Read `ItemStack.getMaxStackSize()` and leave the overflow in place |
| Sophisticated "sort inventory" | Has its own "do not exceed the item limit" guard; measured with `maxStackSize = 10000` and 27 slots: 270000 → 2673 with `raiseModdedSlotLimits` off, 270000 → 270000 with it on |
| Functional Storage | Drawer capacity scales with the item limit — expected |
| Immersive Engineering / EnderIO / Create | These ship their own inventory interfaces with hardcoded limits; not changed here |

The mod changes saved data, so back up or test in a throwaway world first.

## Development

### Implementation

| Place | What it does |
|---|---|
| `ExtraCodecsMixin` | Raises the upper bound of `ExtraCodecs.intRange(1, 99)` to the config, covering both the `max_stack_size` component and `ItemStack.CODEC` |
| `ModBusEvents` | `ModifyDefaultComponentsEvent` writes the new `max_stack_size` for every item, skipping durable and stackable-by-1 items |
| `ContainerMixin` / `SlotMixin` | Replaces a returned 99 with the configured limit, leaving other values alone |
| `ItemStackHandlerMixin` / `ComponentItemHandlerMixin` | Slot limits under `raiseModdedSlotLimits`, raise only |
| `CraftingMenuMixin` / `InventoryMenuMixin` + `SuperStackBulkCraft` | Bulk crafting: count crafts, consume once, distribute once |
| `BulkCraftMath` | Pure arithmetic: availability, crafts, output, space, chunking, conservation check — with JUnit tests |
| `ContainersDropStackMixin` | Drops big stacks as a single entity |
| `GuiGraphicsMixin` / `CountFormat` | Abbreviated, scaled count rendering |
| `ClientEvents` | Adds the "current / limit" tooltip line |
| `mixin/compat/*` | Per-mod slot patches in a separate, optional config |

### Building

Requires JDK 21:

```
gradlew build     # jar in build/libs/SuperStack-1.21.1-21.1-*.jar
gradlew test      # 8 unit tests for BulkCraftMath
```

## Changelog

### 21.1-1.0.6
- MoreMachine big-stack slots no longer exceed the configured stack size (they used to hold 8x, going past the item stack limit; the surplus was dropped on reload).
- Compat slot limits no longer add a separate ceiling: `compatStackLimit` follows the stack size by default, so modded machine slots accept the same large stacks; lower it only if a mod misuses the value.
- MoreMachine big-stack slots: the x8 multiplication now saturates instead of throwing once the stack limit passes 268 million.

- Mystical Agriculture and Mystical Automation: their shared Cucumber library defaulted every slot to 64; slots now use the compat limit (32,768 by default), so the infuser and other machines accept large stacks. Slots with an explicit limit keep it.
- Sophisticated Storage: sorting now does nothing when matching items add up past an int; previously the total overflowed negative and both stacks were wiped.
- Slot limits exposed to supported mods come from `compatStackLimit`, which follows the stack size by default; lower it if a mod misbehaves with very large values.
- The stack limit can now be configured up to 2,147,483,647 (the 32-bit int limit); the range is 64–2,147,483,647 instead of 64–1,000,000. The default stays 32,768.
- Bulk crafting keeps every intermediate value in a long and writes output plus returned items in chunks bounded by their own stack limits, so a two-billion stack cannot overflow and silently lose items.
- Ground item merging is guarded: two big stacks no longer take part in vanilla's 64-per-step merge, which could overflow the added counts; stacks of 64 or less merge as before.
- Verified: a chest holding 2,000,000,000 stone and an item entity holding 2,147,483,647 dirt keep their counts across a save and reload; two two-billion stacks next to each other do not merge.

<details>
<summary><b>21.1-1.0.5</b></summary>

- Shift-clicking the crafting result now crafts everything at once: the mod computes how many crafts the ingredients allow and how many the inventory can hold, takes the smaller value, then consumes and distributes in one pass. Vanilla re-checks the recipe, consumes items and inserts output every round, so tens of thousands of items froze the game or timed out on servers.
- Nine stacks of iron (32,768 × 9) now turns into 32,768 iron blocks in a single click without a server tick timeout.
- Recipes with remaining items (milk buckets and similar) are batched too: buckets and bottles are placed back after the batch.
- The tooltip count is now "current / limit", e.g. `Count: 12,345 / 32,768`.
- Slot numbers are anchored to the slot's bottom-right corner and no longer drift upwards.
- Iron Furnaces output limit follows the config instead of stopping at 64.
- Added the missing `pack.mcmeta`.
- Breaking chests, barrels, furnaces and hoppers drops a big stack as a single entity; before, a 32,768 stack became roughly 1,600 item entities.

</details>

<details>
<summary><b>21.1-1.0.4</b></summary>

- The Applied Energistics 2 patch now adds `getSlotLimit` directly. 1.0.3 injected into classes that only implemented the interface without declaring the method, which crashed on startup when AE2 was installed.

</details>

<details>
<summary><b>21.1-1.0.3</b></summary>

- In AE2 crafting terminals (including the wireless and comprehensive terminals), crafted output is no longer split into stacks of 99 by `InternalInventory`'s default limit when moved into the inventory or cursor.

</details>

<details>
<summary><b>21.1-1.0.2</b></summary>

- The 9-slot crafting grid of the Sophisticated crafting upgrade no longer caps at 99; the patch moved from the container slot to `StatefulComponentItemHandler#getSlotLimit`.

</details>

<details>
<summary><b>21.1-1.0.1</b></summary>

- Container and slot limits follow the config instead of stopping at 99.
- Mekanism and Applied Energistics 2 slots no longer stop at 64 / 99.
- Sophisticated screens no longer show this mod's count overlay.
- Breaking these containers no longer splits the contents into stacks of 64.
- Patches are skipped automatically when the mod is absent.

</details>

<details>
<summary><b>21.1-1.0.0</b></summary>

- Configurable stack limit (64–1,000,000, default 32,768) with proportional scaling and a threshold for small stacks.
- Count display: exact up to 9,999, abbreviated from 10,000, exact value in tooltips.

</details>

## License and credits

[MIT](./LICENSE) © 2026 zaixiayesheng. The logo is drawn for this project.

## Feedback

Report issues on [Issues](https://github.com/zaixiayesheng/SuperStack/issues). Please include the Minecraft and NeoForge versions, the mod version, `logs/latest.log`, and the container or crafting flow involved.
