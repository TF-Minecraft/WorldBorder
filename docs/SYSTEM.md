# WorldBorder - System design

Punish players who cross the configured square world border on the X/Z plane. Admins set real border coordinates per world on deploy; the jar ships a config template only.

See [IMPLEMENTATION_BATCHES.md](IMPLEMENTATION_BATCHES.md) for build history. See [TEST_MATRIX.md](TEST_MATRIX.md) for the manual checklist.

## Zones

Each tick, player position is classified against their world's border:

| Zone | Condition | Effect |
|------|-----------|--------|
| SAFE | Inside border, more than `grace-inset` blocks from any edge | None |
| WARNING | Inside border, within `grace-inset` blocks of an edge | Red title + subtitle |
| OUTSIDE | Past border bounds (X or Z outside min/max) | `damage` HP every `tick-interval-ticks` |

Bounds are **inclusive** on the inside. Y is ignored.

Default timing: 8 HP every 10 ticks (0.5s at 20 TPS).

## Who is affected

- **Survival** and **Adventure** only
- Creative, Spectator, and dead players are skipped
- `worldborder.bypass` skips damage and warnings

## Config (`config.yml`)

| Key | Purpose |
|-----|---------|
| `tick-interval-ticks` | How often to check players (default 10) |
| `damage` | HP dealt per tick when OUTSIDE (default 8.0) |
| `grace-inset` | Warning band depth inside the border (default 20) |
| `title-warning` | Title shown in WARNING zone |
| `subtitle-warning` | Subtitle shown in WARNING zone |
| `worlds` | Optional filter. Empty = enforce any world with a `borders` entry |
| `borders.<world>` | Per-world `min-x`, `max-x`, `min-z`, `max-z` (block coords) |

Example:

```yaml
borders:
  world:
    min-x: 6200
    max-x: 6600
    min-z: 6200
    max-z: 6600
```

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/worldborder reload` | `worldborder.admin` | Reload config and restart tick task |
| `/worldborder debug` | `worldborder.admin` | Show world, position, zone, and border bounds |

## Permissions

| Node | Default | Description |
|------|---------|-------------|
| `worldborder.admin` | op | Reload and debug commands |
| `worldborder.bypass` | false | Ignore border damage and warnings |

## Title behavior

- WARNING zone: title re-sent each tick while near the edge
- Leaving WARNING (SAFE, OUTSIDE, gamemode change, bypass, or unconfigured world): title cleared via `resetTitle()`
