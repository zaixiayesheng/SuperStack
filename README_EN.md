<p align="center"><img src="logo.png" width="160" alt="SuperStack logo"></p>

<h1 align="center">SuperStack</h1>

<h3 align="center">超级堆叠</h3>

<div align="center">

Raises the item stack limit from 64 to a configurable value (32,768 by default) and makes containers, slots, saved data, network sync and crafting all respect it.

Chinese version: [README.md](./README.md)

[![Release](https://img.shields.io/github/v/release/zaixiayesheng/SuperStack?filter=v21.1*&style=flat-square&label=Release&color=blue)](https://github.com/zaixiayesheng/SuperStack/releases)
[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-green.svg?style=flat-square)](https://www.minecraft.net)
[![NeoForge 21.1](https://img.shields.io/badge/NeoForge-21.1+-orange.svg?style=flat-square)](https://neoforged.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](./LICENSE)

[What it is](#what-it-is) · [Install](#install) · [Stack limit](#stack-limit) · [Count display](#count-display) · [Crafting and containers](#crafting-and-containers) · [Slot compatibility](#slot-compatibility) · [Configuration](#configuration) · [FAQ](#faq) · [Compatibility](#compatibility) · [Developers](#developers) · [Changelog](#changelog)

</div>

---

## What it is

Vanilla caps stacks in several separate places, so changing only one of them does nothing. This mod takes over all of them:

- **Item limit**: the `max_stack_size` data component defaults to 64.
- **Saved data**: `ItemStack.CODEC` only accepts counts of 1 to 99, so larger stacks are rejected when saving.
- **Network**: container sync packets write the count as a single byte, so 32,768 comes back as 0.
- **Containers and slots**: `Container.getMaxStackSize()` returns 99 by default, and chests, inventories and crafting grids all use it.
- **Crafting**: shift-clicking the crafting result crafts one item per iteration in vanilla, so a grid holding thirty thousand items runs thirty thousand iterations in one tick.

The limit defaults to 65,536 (safe range 64 to 65,536, up to 1,073,741,823 with the experimental switch). Small stacks scale proportionally (16 becomes 16,384). Items with durability, and items that stack to 1, are left alone.

## Install

| File | Required | Notes |
|---|---|---|
| `SuperStack-<game version>-<version>.jar` | Yes | Download the matching version from Releases; a single jar |
| Minecraft 1.21.1 + NeoForge 21.1+ | Yes | Runtime platform, requires Java 21 |

- Install it on both client and server. With only one side installed, the other side still works with 64 and the server rejects whatever does not fit.
- Mekanism, Applied Energistics 2, Sophisticated Core/Storage/Backpacks and Iron Furnaces are optional: their slot patches only activate when the mod is present, otherwise a single log line is written.

## Stack limit

| Vanilla limit | Raised to (default config) |
|---|---|
| 64 | 32,768 |
| 16 | 8,192 |
| 8 | 4,096 |
| 1 | 1 (unchanged) |

The multiplier is fixed at `maxStackSize / 64`, so x512 by default. Rules:

- Items with durability (tools, armour, Meka gear) are not changed.
- With `keepUnstackable` on, tools, buckets and other one-per-slot items stay at one.
- With `scaleSmallItems` disabled, every item uses `maxStackSize`.
- The limit can go up to 1,073,741,823, but anything above 65,536 needs `experimentalStackSizes`;
  mods that never expected huge stacks may crash or lose items there. 65,536 is the verified safe value.

Saved data and network sync are rewritten as well: counts embedded in items (for example the 9 slots carried by an upgrade item) are stored as an int and synced as a VarInt, so 32,768 survives a save and reload.

## Count display

Slot rendering is only taken over above 99. The tooltip always shows the exact value:

| Count | Shown in the slot |
|---|---|
| 99 or less | vanilla |
| 100 to 9,999 | exact with thousands separators, e.g. `1,234` |
| 10,000 to 999,999 | `10.0k`, `123k`, `999k` |
| 1,000,000 and above | `1.00m`, `2.50m` |

Abbreviations are always truncated, never rounded up. The text is scaled to fit the 16-pixel slot and anchored to its bottom-right corner.

Tooltips read "current / limit", e.g. `Count: 12,345 / 32,768`. In Sophisticated screens both the overlay and the tooltip line are yielded to Sophisticated's own rendering, so two sets of numbers never overlap.

## Crafting and containers

### Bulk crafting

Shift-clicking the crafting result computes "how many crafts the scarcest grid slot allows" and "how many crafts the inventory can hold", takes the smaller value, then consumes the ingredients once and distributes the output once:

- Works for the 3x3 crafting table and the 2x2 inventory grid.
- Recipes with remaining items (milk buckets, potions) are batched too: the buckets and bottles are put back into the grid afterwards.
- No auto-refill: crafting stops when the grid runs out, exactly like vanilla.
- Only vanilla crafting menus are affected. Sophisticated and Applied Energistics 2 have their own crafting implementations and are untouched.

### Container limits

Chests, hoppers, furnaces, double chests and randomly-ticking containers follow the configured value instead of stopping at 64 or 99.

### Breaking containers

A stack larger than 128 drops as **one** item entity. Otherwise vanilla splits it into roughly count/20 entities, so a 32,768 stack becomes about 1,600 drops, which floods the world with entities and can stall or lock up a server when a full chest is broken.

## Slot compatibility

Vanilla containers follow automatically. Other mods fall into two groups: those extending `Slot` / `Container` follow, and those with their own inventory interfaces are raised by `otherModSlots` (raise only, never lower).

| Mod | Version | What was fixed |
|---|---|---|
| Mekanism | 10.7.19.x | Machine and menu slots were hard-coded to 99; they follow the config now |
| Applied Energistics 2 | 19.2.x | Terminal slots stopped at 64 and `InternalInventory` defaults to 99; crafted output written to the inventory or cursor is no longer split into 99s ; import/export buses move a whole stack per operation |
| Sophisticated Core / Storage / Backpacks | 1.5.x / 1.6.x / 3.26.x | The 9-slot crafting upgrade grid stopped at 99; container slot limits and on-screen counts stay under Sophisticated's own management |
| Iron Furnaces | 4.3.x | Output slot limit was hard-coded to 64; it follows the config now |
| Oritech | 1.2.x | Machine and storage slots were hard-coded to 64; they follow the config now |
| Ad Astra | 1.16.x | Uses vanilla containers, so no extra patch is needed |
| Mystical Agriculture / Automation | 8.0.x | Cucumber's shared slot default was hard-coded to 64; infusion machines accept large stacks now |
| Create | 6.0.x | Fan processing time scaled with the real stack count (32,768 became 2,048x slower); the count used is capped, and the filter "extract amount" screen no longer stretches past the screen edge |

Patches live in a separate `superstack.compat.mixins.json` marked `required: false`: when a target class is missing, one log line is written and startup continues.

## Configuration

File: `config/superstack-common.toml`. Changes apply after a restart.

| Option | Default | Range | Meaning |
|---|---|---|---|
| `maxStackSize` | 65536 | 64 to 1,073,741,823 | Items per slot; above 65,536 needs the experimental switch; restart after changing |
| `experimentalStackSizes` | false | on / off | Allow limits above 65,536; risks crashes and item loss |
| `scaleSmallItems` | true | on / off | Items that stack below 64 (ender pearls, eggs) scale up too |
| `keepUnstackable` | true | on / off | Keep tools, buckets and armour at one per slot |
| `countStyle` | COMPACT | COMPACT / EXACT | Abbreviate slot counts (12.3k) or always draw the full number |
| `abbreviateAbove` | 10000 | 1,000 to 1,000,000 | Count from which abbreviations start |
| `tooltipExact` | true | on / off | Show the exact count on the tooltip |
| `otherModSlots` | true | on / off | Widen other mods' slot limits so sorting does not truncate stacks |
| `fanStackCount` | 64 | 1 to 64 | Stack count used for Create fan processing time (not present on 1.20.1) |

`experimentalStackSizes` only gates how high the limit may be set; `compatStackLimit` is gone and supported mods' slot limits now follow `maxStackSize` directly. The first three live under `[basic]`, the rest under `[advanced]`.

Applied Energistics 2 buses follow `maxStackSize` as well; turning off `compat.ae2` restores one-item-at-a-time transfers.

Comments inside the generated config file follow the JVM language: Chinese or English, falling back to Chinese for other locales. Force one with `-Dsuperstack.config.lang=zh` or `=en`. An existing `superstack-common.toml` is not rewritten automatically; delete it to regenerate the comments in the new language.

## FAQ

**What happens if I uninstall it?**
Stacks above the vanilla limit are rejected when loading; the log records `Tried to load invalid item` and those items are lost. Split big stacks back to 64 before removing the mod.

**Server has it, client does not?**
The client still shows and works with 64, and the server rejects the overflow, which looks like "clicking does nothing". Install on both sides.

**Why is a stack not 32,768?**
Items with a 16 limit scale to 8,192. Disable `scaleSmallItems` to make everything use the limit.

**Why can I not see this mod's count overlay in Sophisticated screens?**
Sophisticated draws abbreviated counts itself, so this mod steps aside in those screens.

**Crafting is still slow?**
Only the vanilla crafting table and the 2x2 grid are batched. Sophisticated and Applied Energistics 2 run their own per-iteration logic and are not changed.

**Anything odd in the creative menu?**
Creative tabs group by component value, so each item still has a single entry after the limit is raised. That is expected.

## Compatibility

| Scenario | Behaviour |
|---|---|
| Vanilla containers / hoppers / furnaces | Follow the configured limit |
| KubeJS | Its `remove_slot_limit` passes values through and does not push the limit back down |
| Inventory sorting mods | Read `ItemStack.getMaxStackSize()`; whatever does not fit stays where it was |
| Sophisticated "sort inventory" | Has its own "do not exceed the item limit" guard; measured with `maxStackSize = 10000` and 27 slots: with `otherModSlots` off 270000 became 2673, with it on 270000 stayed 270000 |
| EnderIO / Create | Both define their own inventories with hard-coded limits: EnderIO machine slots follow the structure definition and Create counts internally by 64. The stack limit only changes how much fits, not those numbers (Create fans and filters are handled separately) |

This mod changes saved data. Back up your world, or test it in a copy first.

## Developers

### How it works

| Location | What it does |
|---|---|
| `ExtraCodecsMixin` | Replaces the upper bound of `ExtraCodecs.intRange(1, 99)`, covering both the `max_stack_size` component and `ItemStack.CODEC` |
| `ModBusEvents` | `ModifyDefaultComponentsEvent` writes the new `max_stack_size` to every item, skipping items with durability and items that stack to 1 |
| `ContainerMixin` / `SlotMixin` | Return the configured value when `getMaxStackSize()` is 99; other values are passed through |
| `ItemStackHandlerMixin` / `ComponentItemHandlerMixin` | Slot limits controlled by `otherModSlots`, raise only |
| `CraftingMenuMixin` / `InventoryMenuMixin` + `SuperStackBulkCraft` | Bulk crafting: count crafts, consume once, distribute once |
| `BulkCraftMath` | Pure arithmetic: availability, crafts, output, space, chunking, conservation checks, with JUnit tests |
| `ContainersDropStackMixin` | Large stacks drop as a single entity when a container is broken |
| `ItemEntityMergeGuardMixin` | Merges use long arithmetic and the configured limit instead of the hard-coded 64 |
| `GuiGraphicsMixin` / `CountFormat` | Abbreviated, scaled count rendering |
| `ClientEvents` | Adds the "current / limit" tooltip line |
| `mixin/compat/*` | Per-mod slot patches in a separate config, skipped when the mod is absent |

### Building

Requires JDK 21:

```
gradlew build     # output in build/libs/SuperStack-1.21.1-21.1-*.jar
gradlew test      # 8 unit tests for BulkCraftMath
```

## Changelog

### 21.1-1.1.0

Local build, not published yet.

- Drag-splitting divides the count in int instead of float: vanilla rounds counts above 16,777,216 through a 24-bit mantissa, so dragging one 1,073,741,823 stack across two slots produced 536,870,912 each and one extra item.
- Sophisticated Core (storage, backpacks) ships its own drag-split helper; that one is now integer based too, so the same drag no longer gains an item there.
- Oritech machine and storage slots no longer stop at 64; Ad Astra already uses vanilla containers and needs no patch.
- Fixed: with a billion-scale stack limit, joining a server could be kicked by the recipe sync packet (`update_recipes`), because recipe ingredient counts exceeded the codec range; the count range is now always opened up to the int limit.
- Generator Galore: with stacked fuels, the enchantment and container-return branches no longer replace the whole fuel slot with a single item.

- Default limit is now 65,536 (1024x64): anything above it requires `experimentalStackSizes`,
  and values above the safe bound are clamped with a log warning when the switch is off.
- The item count codec range is pinned to `Int.MAX_VALUE / 2` instead of following the config;
  a config-driven bound used to break the `update_recipes` packet on join.
- Applied Energistics 2 import/export buses now move a whole stack per operation instead of one item: before, even fully speed-upgraded buses capped at 64 operations x 1 item = 64 items/tick.
- It follows `maxStackSize` automatically with no extra config; turning off `compat.ae2` restores the vanilla one-by-one behaviour.

### 21.1-1.0.6
- The stack limit can go up to 1,073,741,823 (half of the 32-bit int ceiling), range 64-1,073,741,823;
  above 65,536 requires `experimentalStackSizes`. Default is 65,536.
- Bulk crafting now counts in long: ingredients, products and returned containers are all computed in long and written back in chunks capped by each stack limit, so a two-billion stack does not lose items to int overflow.
- Ground item merging follows the configured limit: counts are added as long and the hard-coded 64 inside vanilla `merge` is replaced by the config value. Measured: 1.5B + 0.5B merges into 2B, while 1.5B + 1.5B stays split.
- Compat slots now simply follow the stack limit instead of having their own ceiling; the Applied Energistics, Mekanism, Sophisticated, MoreMachine, Mystical Agriculture and Create patches can each be disabled.
- MoreMachine big-stack slots: the x8 limit uses a saturating multiply and is clamped back to the configured stack limit instead of overflowing to 262,144 and being truncated on save.
- Mystical Agriculture / Mystical Automation: the shared Cucumber library hard-codes a slot limit of 64; it now uses the compat limit so infusers and friends accept big stacks, while explicitly sized slots keep their value.
- Sophisticated Storage: sorting is skipped when the combined count of identical items exceeds int; previously it overflowed negative and wiped both stacks.
- Create fans: the count used for processing time is capped by `fanStackCount` (default 64), so small stacks behave exactly like vanilla and big stacks count as 64. The filter/extraction amount setting is clamped to 64 so its screen no longer blows up.
- Config rework: sections are now Basic / Advanced / Mod compat, keys are unified (`scaleSmallItems`, `countStyle`, `otherModSlots`, ...), and both the config screen and the generated comments follow the system language.


<details>
<summary><b>21.1-1.0.5</b></summary>

- Shift-clicking the crafting result now crafts everything at once: the number of crafts is the smaller of "what the ingredients allow" and "what the inventory can hold", consumed and distributed in one pass. Previously every iteration re-checked the recipe, consumed ingredients and filled the inventory, so a grid holding thirty thousand items ran thirty thousand iterations in one tick, which froze the game or timed out multiplayer.
- Nine stacks of iron (32,768 x 9) now produce 32,768 iron blocks in a single click without tripping the server tick timeout.
- Recipes with remaining items (milk buckets and the like) are batched too: buckets and bottles are put back into the grid afterwards.
- Tooltips now read "current / limit", e.g. `Count: 12,345 / 32,768`.
- Slot numbers are anchored to the bottom-right corner instead of floating above the slot.
- Iron Furnaces output slot follows the config instead of stopping at 64.
- Added `pack.mcmeta`, no more "missing metadata" warnings.
- Breaking chests, barrels, furnaces and hoppers drops a large stack as a single entity; a 32,768 stack used to scatter into about 1,600 drops.

</details>

<details>
<summary><b>21.1-1.0.4</b></summary>

- Applied Energistics 2 slot patches now add the method directly. 1.0.3 patched classes that only implement the interface without declaring the method, which crashed on startup when AE2 was installed.

</details>

<details>
<summary><b>21.1-1.0.3</b></summary>

- In the Applied Energistics 2 crafting terminal (including the wireless terminal and the combined work terminal), crafted output written to the inventory or cursor is no longer split into 99s by `InternalInventory`'s default limit.

</details>

<details>
<summary><b>21.1-1.0.2</b></summary>

- The 9-slot crafting grid of Sophisticated's crafting upgrade no longer stops at 99; the patch moved from container slots to `StatefulComponentItemHandler#getSlotLimit`.

</details>

<details>
<summary><b>21.1-1.0.1</b></summary>

- Container and slot limits follow the config instead of stopping at 99.
- Mekanism and Applied Energistics 2 slots no longer stop at 64 or 99.
- Sophisticated screens no longer show this mod's count overlay on top of their own.
- Breaking those containers no longer scatters the contents into piles of 64.
- Patches are skipped automatically when the corresponding mod is absent.

</details>

<details>
<summary><b>21.1-1.0.0</b></summary>

- Configurable item stack limit (64 to 1,073,741,823, safe default 65,536) with proportional scaling for small stacks and a threshold to skip them.
- Count display: exact up to 9,999, abbreviated from 10,000, exact value in the tooltip.

</details>

## License and credits

[MIT](./LICENSE) copyright 2026 zaixiayesheng. The icon is drawn by this project.

## Feedback

Please open an [issue](https://github.com/zaixiayesheng/SuperStack/issues) and include the Minecraft and NeoForge versions, the mod version, `logs/latest.log`, and the container or crafting flow involved.
