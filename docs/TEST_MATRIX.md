# WorldBorder - Test matrix

Manual checks on a test server. Player-facing strings must not contain U+2014 (em dash).

Configure a test border before running (adjust world name as needed):

```yaml
borders:
  world:
    min-x: 6200
    max-x: 6600
    min-z: 6200
    max-z: 6600
```

Then `/worldborder reload`.

## Core behavior

| # | Check |
|---|--------|
| T1 | Inside center (Survival, e.g. 6400/6400): no title, no damage |
| T2 | Within grace inset (e.g. 6590/6400): red title + "Turn back" subtitle |
| T3 | Outside bounds (e.g. 6601/6400): 8 HP every ~0.5s until death |
| T4 | Walk from WARNING back to SAFE: title clears |
| T5 | Walk from WARNING to OUTSIDE: title clears, damage starts |

## Gamemodes and permissions

| # | Check |
|---|--------|
| T6 | Creative outside: no damage, no title |
| T7 | Adventure outside: damage applies |
| T8 | `worldborder.bypass`: no damage or warning |

## Config and commands

| # | Check |
|---|--------|
| T9 | World with no `borders` entry: no effect |
| T10 | `/worldborder reload` picks up config changes (e.g. `grace-inset`) |
| T11 | `/worldborder debug` reports correct zone at your position |
| T12 | Player-facing strings in config and plugin contain no em dash (U+2014) |

## Regression

| # | Check |
|---|--------|
| R1 | Invalid border (`min-x > max-x`) skipped with warning; plugin still enables |
| R2 | Console `/worldborder debug`: "Players only." |
| R3 | Non-op without `worldborder.admin`: "No permission." |
