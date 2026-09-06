# Sweeper Maid

**English** | [简体中文](README_zh.md)

A lightweight, **server-side** Minecraft mod that periodically sweeps dropped items (and
configured extra entities such as arrows) off the ground to keep your server running smoothly —
while keeping the swept items **recoverable** in rotating *dustbins* and protecting valuables in a
non-rotating *recycle bin*.

- **Minecraft:** 1.21.1 &nbsp;•&nbsp; **Loader:** NeoForge 21.1.x &nbsp;•&nbsp; **Java:** 21
- **Side:** server-side only. Vanilla / unmodded clients can join a server running this mod.
  Install it on the client **only** if you want the in-game config screen.

## Features

- **Periodic sweep** of dropped items on a configurable interval, with ActionBar countdown warnings
  (at 60 / 30 / 15 seconds, then every second in the last 10).
- **Rotating dustbins** — swept items are stored in `DUSTBINS_PER_ROTATION` (x) × `ROTATION_COUNT` (y)
  containers. Each sweep fills one *generation* of `x` bins; a generation is cleared and reused only
  after `y` sweeps, so items stay recoverable for `y` sweeps before being auto-removed.
- **Recycle bin** — before a generation is cleared, *protected* items (by rarity threshold, enchanted,
  custom-named, or an explicit item list) are moved to a non-rotating recycle bin instead of being
  deleted.
- **Whitelist / blacklist** — whitelisted items are never swept (left on the ground); blacklisted
  items are swept but discarded (not stored).
- **Chunk overload warning** — admins are notified when a single chunk holds more dropped items than
  a threshold.
- **Performance-first** — the sweep takes a snapshot at the start, then processes a bounded number of
  entities per tick (`SWEEP_ENTITIES_PER_TICK`) to avoid lag spikes; a grace period
  (`MIN_ITEM_AGE_SECONDS`) skips items a player just dropped.
- **Your-language notifications** — every message shown to players is a config string you write
  yourself, in any language.

## Installation

**Server (required target):**
1. Install **NeoForge 1.21.1** (any `21.1.x`).
2. Drop `sweeper_maid-<version>.jar` into the server's `mods/` folder.
3. Start the server. Config is generated at `config/sweeper_maid-common.toml`.

Vanilla and unmodded-NeoForge clients can connect normally — the mod declares itself client-optional.

**Client (optional):** install the same jar to get the in-game config screen
(*Mods → Sweeper Maid → Config*). Useful in single-player or when you host a LAN world.

## Commands

Base command: `/sweepermaid`

| Command | Default permission | Description |
| --- | --- | --- |
| `/sweepermaid clean` | 2 (op) | Run a sweep immediately. |
| `/sweepermaid dustbin [index]` | 0 (everyone) | Open a dustbin's GUI (defaults to `0`). |
| `/sweepermaid recycle` | 0 (everyone) | Open the recycle bin. |
| `/sweepermaid empty all [force]` | 2 (op) | Empty all dustbins. |
| `/sweepermaid empty <index> [force]` | 2 (op) | Empty one dustbin. |
| `/sweepermaid empty recycle` | 2 (op) | Empty the recycle bin. |

`force` skips the recycle rescue and deletes protected items outright. Permission levels are
configurable (`PERMISSION_LEVEL_DUSTBIN` / `_CLEAN` / `_EMPTY`).

## How rotation & the recycle bin work

With the defaults `x = 8`, `y = 2` you get **16 dustbins** (`0–15`): generation 0 = bins `0–7`,
generation 1 = bins `8–15`.

- Sweep #1 fills generation 0 (bins `0–7`).
- Sweep #2 fills generation 1 (bins `8–15`).
- Sweep #3 reuses generation 0: its items (from sweep #1) are cleared first — protected items go to
  the recycle bin, the rest are removed. So items are recoverable for **`y` sweeps**.

After each sweep, players get a clickable chat list of every non-empty dustbin (consecutive bins
shown as a range, e.g. `[Dustbin 0~3]`), plus a warning of which bins will be emptied next sweep.

## Configuration

The config file is `config/sweeper_maid-common.toml`. Selected options:

| Option | Default | Meaning |
| --- | --- | --- |
| `ITEM_SWEEP_INTERVAL` | `600` | Seconds between sweeps. `0` disables automatic sweeping (`/clean` still works). |
| `MIN_ITEM_AGE_SECONDS` | `5` | Items younger than this are skipped this sweep. |
| `SWEEP_ENTITIES_PER_TICK` | `200` | Entities processed per tick during a sweep. `0` = finish in one tick. |
| `DUSTBINS_PER_ROTATION` | `8` | Dustbins per rotation (x). |
| `ROTATION_COUNT` | `2` | Rotations (y) — how many sweeps items survive. |
| `ENABLE_RECYCLE` | `true` | Move protected items to the recycle bin instead of deleting them. |
| `RECYCLE_PROTECT_RARITY` | `RARE` | Protect items at or above this rarity (`COMMON`/`UNCOMMON`/`RARE`/`EPIC`). |
| `RECYCLE_PROTECT_ENCHANTED` / `_NAMED` | `true` | Protect enchanted / custom-named items. |
| `RECYCLE_PROTECT_ITEMS` | `[]` | Item IDs always protected. |
| `ITEM_WHITELIST` | `nether_star, heavy_core` | Never swept. |
| `ITEM_BLACKLIST` | `cobblestone, sand` | Swept but not stored. |
| `EXTRA_ENTITY_TYPES` | `arrow, spectral_arrow, …` | Non-item entity types to also remove. |
| `ITEM_OVERLOAD_THRESHOLD` | `640` | Warn admins when a chunk exceeds this many dropped items. |
| `MESSAGE_*`, `DUSTBIN_NAME`, `RECYCLE_NAME` | — | All player-facing text. Write these in any language. Placeholders like `$1`/`$2` are substituted. |

**Editing the config:**
- **Dedicated / headless server:** edit `config/sweeper_maid-common.toml` directly. Changes are
  applied **live** — no restart needed.
- **Single-player or LAN host (with the mod on the client):** *Mods → Sweeper Maid → Config*.

A Chinese example config is provided in [`config-examples/zh_cn.toml`](config-examples/zh_cn.toml).

## Building from source

```bash
./gradlew build
```

The jar is produced under `build/libs/`.

## Disclaimer

> ⚠️ The feature work in this fork was largely AI-assisted ("vibe coded"). It is provided
> **as-is, with no warranty of any kind** — please test on a backup or staging world before
> relying on it in production.

## License & credits

Licensed under **AGPL-3.0**. Original mod by **Hexagram** — see
[Viola-Siemens/Sweeper-Maid](https://github.com/Viola-Siemens/Sweeper-Maid).
