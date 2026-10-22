<p align="center"><img src="logo.png" width="160" alt="SuperStack logo"></p>

<h1 align="center">SuperStack</h1>

<h3 align="center">超级堆叠</h3>

<div align="center">

Raises the item stack limit from 64 to a configurable value (65,536 by default) and teaches containers, slots, saved data, network sync and crafting about the new limit.

Chinese version: [README.md](./README.md)

[![Release](https://img.shields.io/github/v/release/zaixiayesheng/SuperStack?filter=v20.1*&style=flat-square&label=Release&color=blue)](https://github.com/zaixiayesheng/SuperStack/releases)
[![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-green.svg?style=flat-square)](https://www.minecraft.net)
[![Forge 47.4](https://img.shields.io/badge/Forge-47.4+-orange.svg?style=flat-square)](https://files.minecraftforge.net)
[![License: AGPL-3.0](./LICENSE)](https://www.gnu.org/licenses/agpl-3.0.txt)

[What it is](#what-it-is) · [Install](#install) · [Stack limit](#stack-limit) · [Count display](#count-display) · [Crafting and containers](#crafting-and-containers) · [Slot compatibility](#slot-compatibility) · [Configuration](#configuration) · [FAQ](#faq) · [Compatibility](#compatibility) · [Development](#development) · [Changelog](#changelog)

</div>

---

## What it is

In 1.20.1 the stack limit is capped in several places, and changing only one of them does nothing. This mod takes over all of them:

- **Item limit**: `Item.maxStackSize` is a private final field fixed at construction, 64 by default.
- **Saved data**: `ItemStack` writes its count as a single byte (`putByte("Count", ...)`), so values above 127 overflow.
- **Network**: `FriendlyByteBuf` also writes the count as a single byte, so 32,768 arrives as 0.
- **Containers**: `Container.getMaxStackSize()` returns 64 by default, and chests, hoppers, furnaces and the player inventory all inherit it.
- **Crafting**: shift-clicking the result crafts one item per round, so a grid holding tens of thousands of items runs tens of thousands of rounds inside a single tick.

The limit defaults to 65,536 (safe range 64 to 65,536, up to 1,073,741,823 with the experimental switch). Small stacks scale proportionally (16 becomes 16,384); stackable-by-1 items and items with durability are left alone.

## Install

| File | Required | Notes |
|---|---|---|
| `SuperStack-<game version>-<version>.jar` | Yes | Download the matching build from Releases |
| Minecraft 1.20.1 + Forge 47.4+ | Yes | Needs Java 17 |

- Install on both client and server. With only one side, the other still uses 64 and rejects the overflow.
- Mekanism, Applied Energistics 2, Sophisticated, and Iron Furnaces are optional: their patches only load when the mod is present, otherwise a single log line is written.

## Stack limit

| Vanilla limit | With default settings |
|---|---|
| 64 | 65,536 |
| 16 | 8,192 |
| 8 | 4,096 |
| 1 | 1 (unchanged) |

The multiplier is `maxStackSize / 64`, i.e. ×512 by default. Rules:

- Items with durability (tools, armor, Meka gear) are untouched.
- Items whose vanilla limit is at or below `keepUnstackable` (1 by default) are untouched.
- With `scaleSmallItems` disabled, every item uses `maxStackSize`.
- The limit goes up to 1,073,741,823; anything above 65,536 needs `experimentalStackSizes`, and 65,536 is the safe default.

Counts are stored as ints in NBT and synced as VarInts; stacks above 64 sent by the creative client are no longer discarded by the server.

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

Chests, hoppers, furnaces, double chests, randomizable containers and the player inventory all follow the configured limit instead of stopping at 64. Forge inventory wrappers (`InvWrapper` and `SidedInvWrapper`) follow as well, so pipes no longer stop at 64.

### Breaking containers

Any stack above 128 drops as **one** item entity. Otherwise vanilla splits it into roughly count/20 entities — a 32,768 stack becomes about 1,600 drops, which can flood the server with entities and cause stalls or a hard lock-up when a full chest is broken.

With a full inventory, closing a crafting table now pushes grid contents back into the inventory and drops the remainder as whole stacks instead of dozens of 64s.

## Slot compatibility

Vanilla containers follow automatically. For other mods there are two cases: slots extending `Slot` / `Container` simply follow, while mods implementing their own inventory interface are raised through `otherModSlots` (raise only, never lower).

| Mod | Version | What is fixed |
|---|---|---|
| Mekanism | 10.4.16.x | Machine and GUI slots hardcode 99; they now follow the config |
| Applied Energistics 2 | 15.4.11 | Terminal slots stop at 64 and `InternalInventory` defaults to 99; crafted output is no longer split into stacks of 99 when moved to the invento; import/export buses move a whole stack per operation |y or cursor |
| Sophisticated Core / Storage / Backpacks | 1.5.x / 1.5.x / 3.26.x | The crafting upgrade's 9 slots stop at 99; container slot limits and screen rendering are managed by Sophisticated itself |
| Iron Furnaces | 4.1.8 | Inventory and output slots hardcode 64; both follow the config |
| Create | 6.0.8 | Fans time processing by `fanStackCount` items instead of the real count |
| Immersive Engineering | 10.2.0-183 | Crates, toolboxes, item batchers, circuit tables, turrets and bottling-machine inputs hardcode 64; they now follow the config |

The patches live in a separate `superstack.compat.mixins.json` with `required: false`: when a target class is missing, only a log line is written and startup continues.

## Configuration

The file is `config/superstack-common.toml`; restart after editing.

| Option | Default | Range | Meaning |
|---|---|---|---|
| `maxStackSize` | 65536 | 64 to 1,073,741,823 | Items per slot; restart after changing |
| `experimentalStackSizes` | false | on / off | Allow values above 65536 (up to 1,073,741,823) |
| `scaleSmallItems` | true | on / off | Items that stack below 64 (ender pearls, eggs) scale up too |
| `keepUnstackable` | true | on / off | Keep tools, buckets and armour at one per slot |
| `countStyle` | COMPACT | COMPACT / EXACT | Abbreviate slot counts (12.3k) or always draw the full number |
| `abbreviateAbove` | 10000 | 1,000 to 1,000,000 | Count from which abbreviations start |
| `tooltipExact` | true | on / off | Show the exact count on the tooltip |
| `otherModSlots` | true | on / off | Widen other mods' slot limits so sorting does not truncate stacks |
| `fanStackCount` | 64 | 1 to 64 | Stack count used for Create fan processing time |

Only the first option needs attention; `compatStackLimit` is gone and supported mods' slot limits now follow `maxStackSize` directly. The first three live under `[basic]`, the rest under `[advanced]`.

Applied Energistics 2 buses follow `maxStackSize` as well; turning off `compat.ae2` restores one-item-at-a-time transfers.

Comments inside the generated config file follow the JVM language: Chinese or English, falling back to Chinese for other locales. Force one with `-Dsuperstack.config.lang=zh` or `=en`. An existing `superstack-common.toml` is not rewritten automatically; delete it to regenerate the comments in the new language.

## FAQ

**What happens if I uninstall it?**
Stacks above the vanilla limit are rejected when loading, the log records `Tried to load invalid item`, and those items are lost. Split big stacks back to 64 before removing the mod.

**Server has it, client does not?**
The client still shows and works with 64, and the server rejects the overflow — it looks like "clicking does nothing". Install on both sides.

**Why is a stack 16,384 instead of 65,536?**
Items with a vanilla limit of 16 are scaled proportionally. Disable `scaleSmallItems` to use the full limit everywhere.

**Why did creative-mode stacks used to vanish?**
1.20.1 hardcodes `count <= 64` when handling the creative slot packet, so larger stacks were dropped wholesale. This mod relaxes that check.

**Crafting is still slow?**
Only vanilla crafting screens are batched. Sophisticated and Applied Energistics 2 keep their own per-item loops, which this mod does not change.

## Compatibility

| Scenario | Behaviour |
|---|---|
| Vanilla containers / hoppers / furnaces / player inventory | Follow the configured limit |
| Forge `InvWrapper` / `SidedInvWrapper` | Slot limits follow the config; pipes no longer stop at 64 |
| KubeJS | Its `remove_slot_limit` passes through and does not lower the limit |
| Inventory sorting mods | Read `ItemStack.getMaxStackSize()` and leave the overflow in place |
| Functional Storage | Drawer capacity scales with the item limit — expected |
| Immersive Engineering / Create | Crates, toolboxes, batchers, circuit tables, turrets, bottling inputs and fan processing follow the config |
| EnderIO | Its inventory layouts carry their own hardcoded limits; not changed here |

The mod changes saved data, so back up or test in a throwaway world first.

## Development

### Implementation

| Place | What it does |
|---|---|
| `ItemMixin` | Returns the scaled value from `getMaxStackSize()` |
| `ItemStackMixin` | Reads the count as an int from NBT and writes ints above 127 |
| `FriendlyByteBufMixin` | Uses VarInts for counts in `writeItemStack` / `readItem` |
| `SlotMixin` / `PlayerInventoryMixin` / `SimpleContainerMixin` | Replaces 64 / 99 with the configured limit using `@Redirect` on remappable call sites |
| `RandomizableContainerBlockEntityMixin` / `HopperBlockEntityMixin` / `AbstractFurnaceBlockEntityMixin` / `CompoundContainerMixin` | Chest, hopper, furnace and double-chest clamping and output limits |
| `CreativeSlotLimitMixin` | Relaxes the hardcoded `count <= 64` check of the creative packet |
| `CraftingMenuMixin` / `InventoryMenuMixin` + `SuperStackBulkCraft` | Bulk crafting: count crafts, consume once, distribute once |
| `BulkCraftMath` | Pure arithmetic: availability, crafts, output, space, chunking, conservation check — with JUnit tests |
| `ContainersDropStackMixin` | Drops big stacks as a single entity |
| `GuiGraphicsMixin` / `CountFormat` | Abbreviated, scaled count rendering |
| `ClientEvents` | Adds the "current / limit" tooltip line |
| `mixin/compat/*` | Per-mod slot patches in a separate, optional config |

### Building

Requires JDK 17:

```
gradlew build     # jar in build/libs/SuperStack-1.20.1-20.1-*.jar
gradlew test      # 8 unit tests for BulkCraftMath
```

Jars needed by the third-party patches live in `libs/` as `compileOnly` dependencies and are not packaged.

## Changelog

### 20.1-1.1.0

- The default maximum stack is now 65,536; anything above it needs `experimentalStackSizes` (up to 1,073,741,823).
- Drag splitting now divides in integers, so one 2^30-1 stack split across two slots no longer creates an extra item.
- A stack never reports a maximum below its own count, which keeps the "limit - count" headroom non-negative.
- Ported the AE2 generic-storage and Sophisticated patches to 1.20.1 (interfaces accept whole stacks from pipes, sorting no longer eats items).
- Two more mods covered: Create fans time processing by `fanStackCount` items instead of the real count (verified against 6.0.8), and Immersive Engineering crates, toolboxes, item batchers, circuit tables, turrets and bottling-machine inputs no longer hardcode 64.


### 20.1-1.0.3

- Applied Energistics 2 import/export buses now move a whole stack per operation instead of one item: before, even fully speed-upgraded buses capped at 64 operations x 1 item = 64 items/tick.
- It follows `maxStackSize` automatically with no extra config; turning off `compat.ae2` restores the vanilla one-by-one behaviour.

### 20.1-1.0.2
- The stack limit can now go up to 2,147,483,647 (the 32-bit int ceiling), range 64-2,147,483,647, default is still 32768.
- Bulk crafting now counts in long: ingredients, products and returned containers are all computed in long and written back in chunks capped by each stack limit, so a two-billion stack does not lose items to int overflow.
- Ground item merging follows the configured limit: counts are added as long and the hard-coded 64 inside vanilla `merge` is replaced by the config value; stacks above the limit stay split.
- Compat slots now simply follow the stack limit instead of having their own ceiling; the Applied Energistics, Mekanism, Sophisticated, MoreMachine, Mystical Agriculture and Iron Furnaces patches can each be disabled.
- MoreMachine big-stack slots: the x8 limit uses a saturating multiply, so limits above 268M no longer overflow and throw.
- Sophisticated Storage: sorting is skipped when the combined count of identical items exceeds int; previously it overflowed negative and wiped both stacks.
- Iron Furnaces: fixed an OOM caused by loading the container limit as an inventory array size (furnaces could fail to load and lose contents).
- Config rework: sections are now Basic / Advanced / Mod compat, keys are unified (`scaleSmallItems`, `countStyle`, `otherModSlots`, ...), and both the config screen and the generated comments follow the system language.


<details>
<summary><b>20.1-1.0.1</b></summary>

- Shift-clicking the crafting result now crafts everything at once: the mod computes how many crafts the ingredients allow and how many the inventory can hold, takes the smaller value, then consumes and distributes in one pass. Vanilla re-checks the recipe, consumes items and inserts output every round, so tens of thousands of items timed out servers.
- Nine stacks of iron (32,768 × 9) now turns into 32,768 iron blocks in a single click without a tick timeout.
- Recipes with remaining items (milk buckets and similar) are batched too: buckets and bottles are placed back after the batch.
- The tooltip count is now "current / limit", e.g. `Count: 12,345 / 32,768`.
- Slot numbers are anchored to the slot's bottom-right corner and no longer drift upwards.
- Iron Furnaces output limit follows the config instead of stopping at 64; the previous build also targeted the static `tick` method, which broke the whole patch.
- The Iron Furnaces inventory limit is fixed too (`TileEntityInventory` declares its own `getMaxStackSize`), and Forge inventory wrappers follow the config.
- The player inventory follows the config: in 1.20.1 pickups, `/give` and the inventory all use the `Container` default of 64.
- Chests, hoppers, furnaces and double chests follow the config, so inserting no longer clamps to 64.
- Creative-mode stacks above 64 are no longer discarded by the server and no longer turn into empty slots when opening a chest.
- With a full inventory, closing a crafting table pushes grid contents back into the inventory and drops whole stacks instead of dozens of 64s.
- Breaking containers drops a big stack as a single entity instead of thousands.
- Added the missing `pack.mcmeta`.

</details>

<details>
<summary><b>20.1-1.0.0</b></summary>

- First Forge 1.20.1 build, feature-aligned with the 1.21.1 line: configurable stack limit (32,768 by default), proportional scaling for small stacks, exact and abbreviated counts.
- 1.20.1 stores counts as a single byte; they are now stored as ints in NBT and synced as VarInts, so values above 127 no longer overflow.
- Carries the 1.21.1 compatibility work: Mekanism / Applied Energistics 2 slots follow the config, AE2 output is no longer split into 99s, and Sophisticated screens keep their own overlay.

</details>

## License and credits

[AGPL-3.0](./LICENSE) copyright 2026 zaixiayesheng. The icon is drawn by this project.

Released under the **GNU Affero General Public License v3.0**: you may use, copy, distribute and modify it freely, but if you distribute a modified version (including making it available over a network) you must publish the complete corresponding source under AGPL-3.0 as well; it may not be relicensed as closed source. Releases 1.0.7 and earlier were published under the MIT license that applied at the time.

## Feedback

Report issues on [Issues](https://github.com/zaixiayesheng/SuperStack/issues). Please include the Minecraft and Forge versions, the mod version, `logs/latest.log`, and the container or crafting flow involved.
