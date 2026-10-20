<p align="center"><img src="logo.png" width="160" alt="SuperStack logo"></p>

<h1 align="center">SuperStack</h1>

<h3 align="center">超级堆叠</h3>

<div align="center">

Raises the item stack limit from 64 to a configurable value (32,768 by default) and teaches containers, slots, saved data, network sync and crafting about the new limit.

Chinese version: [README.md](./README.md)

[![Release](https://img.shields.io/github/v/release/zaixiayesheng/SuperStack?filter=v20.1*&style=flat-square&label=Release&color=blue)](https://github.com/zaixiayesheng/SuperStack/releases)
[![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-green.svg?style=flat-square)](https://www.minecraft.net)
[![Forge 47.4](https://img.shields.io/badge/Forge-47.4+-orange.svg?style=flat-square)](https://files.minecraftforge.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](./LICENSE)

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

The limit defaults to 32,768 (configurable 64–1,000,000). Small stacks scale proportionally (16 → 8,192); stackable-by-1 items and items with durability are left alone.

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
| 64 | 32,768 |
| 16 | 8,192 |
| 8 | 4,096 |
| 1 | 1 (unchanged) |

The multiplier is `maxStackSize / 64`, i.e. ×512 by default. Rules:

- Items with durability (tools, armor, Meka gear) are untouched.
- Items whose vanilla limit is at or below `minimumStackSizeThreshold` (1 by default) are untouched.
- With `scaleSmallStacks` disabled, every item uses `maxStackSize`.
- The limit itself can go up to 2,147,483,647; 32,768 is only a conservative default.

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

Vanilla containers follow automatically. For other mods there are two cases: slots extending `Slot` / `Container` simply follow, while mods implementing their own inventory interface are raised through `raiseModdedSlotLimits` (raise only, never lower).

| Mod | Version | What is fixed |
|---|---|---|
| Mekanism | 10.4.16.x | Machine and GUI slots hardcode 99; they now follow the config |
| Applied Energistics 2 | 15.4.11 | Terminal slots stop at 64 and `InternalInventory` defaults to 99; crafted output is no longer split into stacks of 99 when moved to the inventory or cursor |
| Sophisticated Core / Storage / Backpacks | 1.5.x / 1.5.x / 3.26.x | The crafting upgrade's 9 slots stop at 99; container slot limits and screen rendering are managed by Sophisticated itself |
| Iron Furnaces | 4.1.8 | Inventory and output slots hardcode 64; both follow the config |

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
| Immersive Engineering / EnderIO / Create | These ship their own inventory interfaces with hardcoded limits; not changed here |

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

### 20.1-1.0.2
- Compat slot limits no longer add a separate ceiling: `compatStackLimit` follows the stack size by default, so modded machine slots accept the same large stacks; lower it only if a mod misuses the value.
- MoreMachine big-stack slots: the x8 multiplication now saturates instead of throwing once the stack limit passes 268 million.

- Sophisticated Storage: sorting now does nothing when matching items add up past an int; previously the total overflowed negative and both stacks were wiped.
- Slot limits exposed to supported mods come from `compatStackLimit`, which follows the stack size by default; lower it if a mod misbehaves with very large values.
- Iron Furnaces: fixed an out-of-memory crash while loading a furnace, caused by the mod using the container limit as its inventory size.
- The stack limit can now be configured up to 2,147,483,647 (the 32-bit int limit); the range is 64–2,147,483,647 instead of 64–1,000,000. The default stays 32,768.
- Bulk crafting keeps every intermediate value in a long and writes output plus returned items in chunks bounded by their own stack limits, so a two-billion stack cannot overflow and silently lose items.
- Ground item merging is guarded: two big stacks no longer take part in vanilla's 64-per-step merge, which could overflow the added counts; stacks of 64 or less merge as before.
- Verified: a chest holding 2,000,000,000 stone and an item entity holding 2,147,483,647 dirt keep their counts across a save and reload; two two-billion stacks next to each other do not merge.

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

[MIT](./LICENSE) © 2026 zaixiayesheng. The logo is drawn for this project.

## Feedback

Report issues on [Issues](https://github.com/zaixiayesheng/SuperStack/issues). Please include the Minecraft and Forge versions, the mod version, `logs/latest.log`, and the container or crafting flow involved.
