# 1.21.11 Cleanup And Restructure Plan

This file tracks the cleanup work to do first on `1.21.11-All`, then cherry-pick or replay into `26.1.2-All`.

## Scope

- Do the cleanup in one pass on `1.21.11-All`.
- Renames are allowed when they make ownership clearer.
- Avoid behavior rewrites unless the current structure is already broken or dead.
- Keep loader-specific behavior separated, but move shared logic into common where possible.

## Current Dirty Files To Preserve

These files were already modified before this plan was written:

- `common/src/main/resources/sereneseasonsplus.mixins.json`
- `fabric/src/main/java/com/Gabou/sereneseasonsplus/client/PerformanceWarning.java`
- `neoforge/src/main/java/com/Gabou/sereneseasonsplus/util/PerformanceWarning.java`

Review them before editing so we do not overwrite unrelated work.

## 1. Remove Dead Snow Real Magic Mixin Path

Goal: keep real Snow Real Magic support, remove the dead commented mixin path.

Changes:

- Delete `common/src/main/java/com/Gabou/sereneseasonsplus/mixin/SnowRealMagicCompatMixin.java`.
- Remove `SnowRealMagicCompatMixin` from `common/src/main/resources/sereneseasonsplus.mixins.json` if still present.
- Remove `SNOW_REAL_MAGIC_MIXIN` and its `shouldApplyMixin` branch from `SereneSeasonsPlusMixinPlugin`.
- Do not remove `SnowRealMagicCompatibilityAdapter`; it is the real compatibility layer.
- Recheck `ServerLevelMixin` gating. Snow Real Magic being loaded must not disable the normal server-level hook unless there is a proven replacement.

Expected result:

- Snow Real Magic compatibility continues through the reflective adapter.
- No dead mixin class is loaded or gated.
- Snow Real Magic cannot accidentally disable snow logic by disabling `ServerLevelMixin`.

## 2. Fix Server Level Access Mixin Naming And Wiring

Goal: remove ambiguity around the typo-named server-level access mixin.

Changes:

- Rename `ServelLevelChunkMixin` to `ServerLevelAccessMixin`.
- Rename file accordingly:
  - From `common/src/main/java/com/Gabou/sereneseasonsplus/mixin/ServelLevelChunkMixin.java`
  - To `common/src/main/java/com/Gabou/sereneseasonsplus/mixin/ServerLevelAccessMixin.java`
- Confirm the class is listed in `sereneseasonsplus.mixins.json` if it is required.
- If the mixin is not required, delete it and remove dependent `IServerLevel` usage.
- Move `IServerLevel` to an access package if keeping it.

Expected result:

- No typo class names.
- The interface injection is either clearly active or fully removed.
- `ServerLevelMixin` no longer depends on uncertain access state.

## 3. Create Access Package For Mixin-Injected Interfaces

Goal: stop using `util` as a catch-all for mixin accessors.

Changes:

- Create `common/src/main/java/com/Gabou/sereneseasonsplus/access`.
- Move these interfaces from `util` to `access`:
  - `ISnowTrackedChunk`
  - `IServerLevel`
  - `MinecraftServerAccess`
  - `IScreen`
- Update imports in common, Fabric, and NeoForge sources.
- Keep `IEnvironmentHelper`, `IRainHandler`, and `AsyncExecutorHandler` in `util` unless a better platform package is created.

Expected result:

- Mixin-added access contracts are isolated.
- `util` becomes less noisy.
- Future ports can find injected interfaces quickly.

## 4. Split CommonSnowBlockFeature Into Services

Goal: keep `CommonSnowBlockFeature` as the public facade, but move responsibilities into focused classes.

Current issue:

- `CommonSnowBlockFeature` is too broad. It owns server ticking, config lifecycle, chunk queues, player tracking, snow placement, mutation batching, storm state, and helper methods.

Changes:

- Create `SnowTickScheduler`.
  - Owns `handleServerTick`.
  - Owns tick phase decisions.
  - Owns per-tick processing limits.
  - Owns deferred chunk queue draining.
- Create `SnowPlacementService`.
  - Owns `canReceiveSnowAt`.
  - Owns `placeOrQueueLayers`.
  - Owns `queueSnowLayersIfNeeded`.
  - Owns `queueClearIfNeeded`.
  - Owns `clampLayersForColumnCap`.
- Create `SnowLifecycleService`.
  - Owns server start/stop cleanup.
  - Owns config reload updates.
  - Owns season-change resets.
- Leave these in `CommonSnowBlockFeature` as facade methods where other classes already call them:
  - `isSnowFeatureEnabled`
  - `getTickCounter`
  - `getSnowHeightCap`
  - `enqueueChunkForSnowApply`
  - `enqueueChunkForSnowMelt`
  - `accumulateColumnUpdate`
  - `tryFreezeWaterAt`
- Remove unused or duplicate helpers after extraction.

Expected result:

- `CommonSnowBlockFeature` becomes orchestration/facade instead of a 600+ line god class.
- Services can be ported independently to `26.1.2`.
- Injection code stays stable while internals become easier to test and reason about.

## 5. Reduce ServerLevelMixin To Injection Glue

Goal: keep mixins thin and move logic to normal Java services.

Current issue:

- `ServerLevelMixin` directly contains precipitation logic, snow safety checks, freeze logic, destroyed-column checks, tick evaluation, and weather overrides.

Changes:

- Create `ServerPrecipitationService`.
  - Owns snow placement safety checks.
  - Owns destroyed-column skip checks.
  - Owns snow update accumulation after vanilla precipitation.
- Create `SeasonalWeatherOverrideService`.
  - Owns seasonal raining override decisions.
  - Owns freeze redirection behavior.
- Keep `ServerLevelMixin` focused on:
  - capturing vanilla locals,
  - redirecting vanilla calls,
  - passing data to services.
- Remove unused captured fields:
  - `atmosphere$freezePos`
  - `atmosphere$level`
  if they are not used after verification.

Expected result:

- Future Minecraft version ports mostly touch injection points, not business logic.
- Snow logic becomes easier to compare between `1.21.11` and `26.1.2`.

## 6. Deduplicate Fabric And NeoForge Config UI

Goal: reduce duplicate UI classes between loaders.

Current duplicate targets:

- `fabric/src/main/java/com/Gabou/sereneseasonsplus/client/config/SereneExtendedScreen.java`
- `neoforge/src/main/java/com/Gabou/sereneseasonsplus/config/SereneExtendedScreen.java`
- `fabric/src/main/java/com/Gabou/sereneseasonsplus/client/config/SereneExtendedList.java`
- `neoforge/src/main/java/com/Gabou/sereneseasonsplus/config/SereneExtendedList.java`

Changes:

- Move shared screen/list layout logic into common client classes if the build supports common client code.
- If common client code is too annoying in this build, create a shared config model/helper in common and keep platform UI wrappers thin.
- Keep loader-specific entrypoints and config save/load wiring separate.

Expected result:

- Less duplicate UI maintenance.
- Cleaner cherry-pick to `26.1.2`.

## 7. Normalize Performance Warning Classes

Goal: remove duplicate or misplaced client screen classes.

Changes:

- Compare Fabric and NeoForge `PerformanceWarning` implementations.
- Move shared screen body to a common client class if possible.
- Keep platform-specific open-screen scheduling only in loader modules.
- Rename NeoForge class package if needed; a client `Screen` should not live under `util`.

Expected result:

- One implementation of the warning UI.
- Loader modules only decide when to show it.

## 8. Rename Ambiguous Fields And Methods

Goal: make intent clear without changing behavior.

Changes:

- Rename `snowPill` to `pendingColumnUpdates` or remove it if redundant.
- Rename `passifSnowBlocks` to `processPassiveSnowBlocks`.
- Remove or rename `calculateBlocksToReplace1`; it currently reads like leftover code.
- Rename typo/comment artifacts and French/English mixed names where they obscure behavior.
- Replace unclear local comments with concise intent comments only where needed.

Expected result:

- Easier review and fewer mistakes during porting.

## 9. Centralize Constants And Processing Limits

Goal: make tuning snow performance and behavior easier.

Changes:

- Create `SnowProcessingLimits` or similar.
- Move hardcoded values out of core loops where reasonable:
  - queue attempts,
  - max inspected chunks per tick,
  - max processed chunks per tick,
  - mutation batch divisor,
  - tick phase interval,
  - active storm target defaults,
  - storm intensity defaults.
- Keep config-backed values separate from fixed internal safety limits.

Expected result:

- Performance tuning does not require reading mixins and large service classes.
- Load-time and chunk-processing behavior becomes easier to reason about.

## 10. Replace Direct Exception Printing

Goal: use logger output consistently.

Changes:

- Replace `printStackTrace` in Fabric config code with logger output.
- Search all modules for `System.out`, `printStackTrace`, and empty catches.
- Keep reflective adapter failures quiet only where absence of optional mods is expected.

Expected result:

- Cleaner logs.
- Better diagnostics for real config failures.

## 11. Suggested One-Pass Order

Use this order to keep breakage contained:

1. Remove dead SRM mixin and plugin gate.
2. Fix or remove server-level access mixin typo.
3. Move access interfaces into `access`.
4. Extract services from `CommonSnowBlockFeature`.
5. Extract service logic from `ServerLevelMixin`.
6. Deduplicate performance warning UI.
7. Deduplicate config UI where practical.
8. Rename ambiguous fields/methods.
9. Centralize constants.
10. Replace direct exception printing.
11. Run build/tests for Fabric and NeoForge.
12. Cherry-pick the cleanup commit into `26.1.2-All`.

## Validation Checklist

- `git status` reviewed before edits.
- Mixin JSON contains only real mixins.
- No class listed in mixin JSON lacks `@Mixin`.
- Snow Real Magic loaded does not disable the only server-level snow hook.
- Fabric build passes.
- NeoForge build passes.
- Existing snow safety behavior still prevents snow from deleting important blocks.
- Grass/flower regrowth behavior still uses varied flowers, not only dandelions.
- Chunk load reconciliation still avoids heavy work directly during chunk load.

## Completed In This Pass

- Removed the dead Snow Real Magic mixin class.
- Removed the stale mixin plugin that disabled `ServerLevelMixin` when Snow Real Magic was loaded.
- Registered the server-level access mixin directly as `ServerLevelAccessMixin`.
- Renamed/moved mixin-injected access interfaces into `com.Gabou.sereneseasonsplus.access`.
- Deleted the unused `ScreenMixin` and `IScreen` path.
- Moved identical Fabric/NeoForge `PerformanceWarning` UI into common.
- Moved identical Fabric/NeoForge `SereneExtendedList` UI into common.
- Updated Fabric and NeoForge config screens to render without the removed `IScreen` mixin cast.
- Added `SnowProcessingLimits` for the main hardcoded snow processing limits.
- Renamed `snowPill` to `pendingColumnUpdates`.
- Renamed `passifSnowBlocks` to `processPassiveSnowBlocks`.
- Removed the leftover debug coordinate check in `isExposedToSky`.
- Added `ServerPrecipitationService` for snow placement safety, destroyed-column checks, and tracked precipitation placement.
- Reused `ServerPrecipitationService` from both `ServerLevelMixin` and `SnowChunkWeatherLogic`.
- Replaced Fabric config `printStackTrace` with logger output.
- Added an explicit Better Days dynamic time compatibility toggle and one-time warning for the 1.21.11 reflection fallback.
- Replaced the slow 5-phase snow replacement queue with per-tick time-budgeted draining for chunk reconciliation, chunk apply/melt tasks, and block mutations.
- Made grass, tall grass, ferns, flowers, and leaf-litter-like ground blocks valid snow replacement targets while keeping important blocks protected.

## Deferred

- Full extraction of `CommonSnowBlockFeature` into `SnowTickScheduler`, `SnowPlacementService`, and `SnowLifecycleService`.
- Full deduplication of Fabric/NeoForge config screens, because the persistence backends still differ significantly.
- Renaming config keys containing `snowPillerAndReplacer`; those are persisted user config keys and changing them would need migration logic.

## Validation Completed

- `./gradlew.bat compileJava`
- `./gradlew.bat build`
