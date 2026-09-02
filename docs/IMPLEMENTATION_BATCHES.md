# WorldBorder - Implementation batches

Work was completed in order. See [SYSTEM.md](SYSTEM.md) for locked design. See [TEST_MATRIX.md](TEST_MATRIX.md) for manual checks.

---

## Batch 1 - Project scaffold

- [x] `pom.xml` (Java 17, spigot-api 1.21.8, antrun copy to TFMC/WorldBorder)
- [x] `WorldBorder.java` bootstrap (singleton, folders, config copy)
- [x] `plugin.yml`, bundled `config.yml` template

**Test:** `mvn package` succeeds; plugin enables on server.

---

## Batch 2 - Config + region model

- [x] `cache/Cache.java`
- [x] `border/Region.java` with `zoneAt(x, z, graceInset)`
- [x] `loader/ConfigLoader.java`
- [x] `loadConfigs()` on enable; log border count

**Test:** Config loads; invalid borders skipped with warning.

---

## Batch 3 - Border tick

- [x] `border/BorderManager.java` - damage OUTSIDE, title WARNING
- [x] Survival/Adventure only; `worldborder.bypass` skip
- [x] Wired `start()` / `stop()` in main class

**Test:** Manual zone behavior at test coords.

---

## Batch 4 - Commands + reload

- [x] `command/CommandManager.java` - `/worldborder reload`, `/worldborder debug`
- [x] `reloadAll()` restarts tick with new interval
- [x] `plugin.yml` commands and permissions

**Test:** Reload without restart; debug shows zone.

---

## Batch 5 - Polish + docs

- [x] Clear warning title when leaving WARNING zone
- [x] `docs/SYSTEM.md`, `docs/TEST_MATRIX.md`, this file

**Test:** T4/T5 from test matrix; full checklist before production deploy.
