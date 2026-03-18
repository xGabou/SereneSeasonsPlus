# Snow System Study And Refactor Plan

## Scope
This document studies the current `CommonSnowBlockFeature` snow piling and melting implementation and defines the staged refactor direction implemented in this pass.

## 1. Current Behavior Summary

### What the current feature does correctly
- Persists per-chunk tracked snow columns, ice positions, destroyed storm columns, and storm progress through chunk serialization.
- Maintains winter-wide storm history through `SnowHistorySavedData` and environment-level counters through `SnowSavedData`.
- Batches many snow world edits through `pendingChanges` and flushes tracked column updates at batch end.
- Supports both historical snow application and active-storm piling.
- Preserves a local player-driven update loop for nearby piling and melting.
- Tracks blocks broken by players during an active storm so the same storm does not immediately repopulate them.
- Hooks vanilla chunk weather ticks and Snow Real Magic’s tick entrypoint.

### How snow piling works today
- `SnowLogic.evaluate` decides whether a chunk should enqueue an apply or melt task based on temperature, season, global storm history, and tracked snow totals.
- `handleServerTick` processes queued apply tasks in `ChunkQueue`.
- Apply uses three fallback passes:
  - `syncTrackedColumnsToWorld` tries to restore tracked snow columns directly.
  - `applySnowHistoryPass` applies a finished-storm baseline plus a finished or active storm pattern.
  - `applySnowPatternFromActiveRecord` uses the active storm record if history baseline did nothing.
- `passifSnowBlocks` also adds or removes nearby snow around players for local responsiveness.
- `SnowChunkWeatherLogic.run` intercepts vanilla-like snow addition and freeze logic and feeds tracked data through `accumulateColumnUpdate`.

### How melting works today
- `SnowLogic.evaluate` enqueues melt tasks when chunks are too warm for snow in warm sub-seasons or in early winter before any storm has happened.
- `meltSnowInChunk` decrements the topmost tracked block per X/Z column by one layer each pass.
- It also clears some covered untracked snow near the surface and thaws tracked ice back to water.
- `passifSnowBlocks` removes nearby snow around players based on the handler’s replacement count.

### How chunk load behavior works today
- `handleOnChunkLoad` only queues the chunk into `snowQueue`.
- `chunkHandler` only computes missing chunk metadata:
  - cached surface height
  - estimated count of available snow columns
- It does not perform chunk reconciliation on load.
- As a result, a chunk can load without its logically expected historical snow until later active chunk ticking or local player-driven updates happen.

### How tracked data and queued changes are used
- Tracked data lives on `ISnowTrackedChunk` and is serialized into chunk NBT.
- World changes are first stored in `pendingChanges`.
- After a block is changed, `accumulateColumnUpdate` stores tracked map changes in `pendingColumnMapUpdates`.
- `finalizeChunkBatch` flushes pending tracked updates into chunk state, records frozen ice positions, and marks touched chunks unsaved.

## 2. Architectural Problems

### Mixed responsibilities
`CommonSnowBlockFeature` currently mixes:
- server lifecycle
- chunk load metadata work
- chunk reconciliation policy
- active storm progress logic
- snow placement policy
- snow melt policy
- block compatibility assumptions
- world mutation batching
- player-local update logic
- snow column scanning and placement topology

This makes reasoning and future extension expensive.

### Static mutable state risks
- `pendingChanges`, `pendingColumnMapUpdates`, `pendingIceAdds`, `chunksToDirty`, `playerPositions`, `snowQueue`, `tickCounter`, and several config flags are all process-global mutable state.
- That state is not encapsulated by responsibility.
- Some cleanup paths are incomplete. `ChunkQueue.clear()` did not clear all internal queues before this refactor pass.

### Lifecycle risks
- Chunk load only warms metadata and never reconciles expected snow state.
- `CommonSnowBlockFeature.clear()` previously did not own all lifecycle-managed state because chunk load queueing and mutation bookkeeping were spread across unrelated fields.
- `needUpdateSnowFeature` was repeatedly applied without being reset.
- `snowQueue` held `LevelChunk` instances directly instead of chunk positions, increasing stale reference risk.

### Synchronization and threading concerns
- Several structures use concurrent containers, but the system conceptually expects main-thread world mutation.
- The concurrent containers are not a coherent threading model; they are mostly acting as static bags.
- The batch state and chunk load state were accessible from many methods without a boundary.

### Chunk consistency risks
- Tracked chunk maps can differ from world blocks if a chunk loads with historical snow expected but no apply task is triggered.
- `syncTrackedColumnsToWorld` checked pending chunk state by scanning the entire pending change map for matching chunk coordinates.
- Apply paths directly mutate tracked chunk maps before queued world changes are finalized, which can temporarily diverge tracked state from actual blocks.

### Compatibility risks
- Core logic hardcodes vanilla `Blocks.SNOW`, `Blocks.SNOW_BLOCK`, and `Blocks.ICE` assumptions in many places.
- Unsupported external snow blocks are not abstracted.
- Snow Real Magic compat currently cancels its tick and runs SS+ logic again even though SS+ already hooks vanilla `tickChunk`, creating duplicate work.
- There is no clear provider boundary for future mod-specific snow state implementations.

### Maintainability problems
- The main class is over 1,400 lines.
- Vertical snow-column scanning logic is duplicated across apply, melt, cap-clamp, and history checks.
- There is no single owner for “what should happen” versus “apply this to the world.”
- The method `passifSnowBlocks` is both misspelled and overloaded with policy and mutation behavior.

## 3. Performance Analysis

### Expensive current paths
- `passifSnowBlocks` iterates every tracked player and can probe up to `MAX_ATTEMPTS` random columns each cycle.
- `findSnowBlockInRadius` scans a square radius and up to 11 Y-levels, which is expensive with many players and large view distances.
- `applySnowHistoryPass`, `applySnowPattern`, and `applyCombinedFinishedPattern` rescan many columns and repeatedly read world state vertically.
- `clampLayersForColumnCap` performs a fresh up/down column scan for each placement.
- `syncTrackedColumnsToWorld` checked whether a chunk had pending work by streaming all pending change keys.

### Repeated scans and reads
- Column stack totals were recomputed independently in several methods instead of through one shared inspector.
- Placement topology and layer totals were recomputed from scratch inside each pass.
- Chunk load metadata computed “available snow columns” using a full 16x16 scan but did not reuse that moment to schedule reconciliation.

### Current complexity of important paths
- Chunk apply passes are effectively `O(columns * column_height)` and often repeat that work multiple times in one task.
- `findSnowBlockInRadius` is `O(radius^2 * y_band)` per melt attempt around a player.
- The prior pending-work check in `syncTrackedColumnsToWorld` added `O(pending_changes)` overhead per chunk sync attempt.

### Work that should be cached or deferred
- Chunk load metadata should drive one-shot chunk reconciliation instead of being computed and then discarded.
- Pending-work checks should be chunk-indexed, not global-stream scanned.
- Column topology should be computed through a shared inspector.

## 4. World State Correctness Analysis

### Tracked snow vs world blocks
- Tracked columns can become stale if a chunk loads and no reconciliation happens.
- Apply paths can update tracked maps before the queued block mutation is committed, which is survivable but not ideal.

### Queued changes vs tracked chunk maps
- `pendingColumnMapUpdates` and tracked chunk maps can temporarily diverge until `finalizeChunkBatch`.
- If a chunk unloads before finalize, pending updates are lost from tracked chunk storage.

### Chunk load and unload safety
- Holding direct `LevelChunk` references in `snowQueue` was weaker than queueing `ChunkPos` and resolving the loaded chunk when processed.
- Load behavior did not guarantee reconciliation.

### Partial storm progress reliability
- Storm progress is persisted per chunk and tied to a storm id, which is the correct direction.
- However the evaluation and apply scheduling around it were spread across several callsites, making timing hard to reason about.

### Pile and melt conflicts
- Both apply and melt tasks can be scheduled in neighboring logic paths.
- The load path did not explicitly arbitrate between “sync tracked snow” and “warm chunk should melt.”

## 5. Mod Compatibility Analysis

### Current behavior with Snow Real Magic or similar mods
- SS+ cancels Snow Real Magic’s world tick and runs its own logic through `SnowRealMagicCompatMixin`.
- SS+ already runs its own logic from `ServerLevel.tickChunk`, so SRM worlds incur duplicate SS+ snow evaluation unless corrected.
- The current implementation does not provide a native abstraction for non-vanilla snow states.

### Hardcoded vanilla assumptions
- Direct checks against `Blocks.SNOW`, `Blocks.SNOW_BLOCK`, `Blocks.ICE`, and `SnowLayerBlock.LAYERS` appear throughout the core logic.
- Placement assumes snow can be restored by directly mutating vanilla blocks.

### Missing abstraction
- There was no boundary separating “managed SS+ snow blocks” from arbitrary world snow-like blocks.
- There was no compatibility strategy beyond ad hoc mixins.

### Compatibility strategy
- Introduce a `SnowBlockCompatibility` boundary.
- The default implementation only manages known-safe vanilla snow and ice.
- Unsupported external snow blocks are treated as opaque world state:
  - SS+ does not try to melt or rewrite them.
  - SS+ only places managed snow into safe supported positions.
- This is safe degradation now and allows future native adapters for Snow Real Magic or other mods without touching policy logic again.

### Updated compatibility status after this pass
- The compatibility boundary now supports provider-owned mutations instead of only direct queued blockstates.
- Snow Real Magic now has a native optional adapter for supported managed snow states:
  - SS+ can read SRM layer counts during reconciliation and melt/apply passes.
  - SS+ can queue SRM-compatible mutations for managed states without flattening them into vanilla assumptions.
  - SRM still keeps its own immersive local snowfall tick, so SS+ does not double-run local snowfall when SRM is installed.
- Unsupported snow implementations still degrade safely because only adapters that explicitly claim ownership are allowed to rewrite blocks.

## 6. Refactor Proposal

### Target architecture
- `SnowBlockCompatibility`
  - Defines which snow and ice states SS+ manages.
  - Owns safe block-state interpretation.
- `SnowWorldMutation`
  - Represents a queued world operation instead of assuming every provider can be expressed as a target `BlockState`.
- `SnowColumnInspector`
  - Central place for placement topology and vertical stack analysis.
- `SnowChunkLoadReconciler`
  - Owns chunk load queueing, deduplication, metadata initialization, and first reconciliation scheduling.
- `SnowMutationBatch`
  - Owns queued world mutations, tracked-column pending updates, dirty chunk marking, and batch finalization.
- `CommonSnowBlockFeature`
  - Remains the entry facade for now.
  - Delegates instead of owning every state structure directly.

### Why this is better
- Separates decision logic from world mutation.
- Makes chunk load reconciliation explicit instead of accidental.
- Removes stale `LevelChunk` queueing.
- Reduces repeated world reads in the hottest reconciliation paths.
- Establishes a compatibility seam for future snow providers.
- Preserves current saved chunk data and storm history format.

## 7. Migration Plan

### Safe staged migration
1. Keep `ISnowTrackedChunk`, chunk NBT storage, and storm history saved data unchanged.
2. Extract support components while leaving public `CommonSnowBlockFeature` entry points stable.
3. Route chunk load through the new reconciler.
4. Route queued world mutation bookkeeping through the new batch owner.
5. Route block interpretation and column scanning through shared compatibility and inspector helpers.
6. In future passes, split remaining policy logic out of `CommonSnowBlockFeature` into:
  - chunk evaluation policy
  - local active update policy
  - reconciliation executor
  - provider-specific compatibility implementations

### Saved data compatibility
- This pass preserves chunk NBT field names and saved data schema.
- Existing tracked snow columns, destroyed columns, storm progress, and storm history remain valid.

## Implemented In This Pass
- Added a formal study and refactor plan.
- Added chunk-load reconciliation scheduling.
- Added a compatibility boundary for managed snow/ice states.
- Added a shared column inspector.
- Added a chunk state service to wrap tracked chunk state access.
- Added a dedicated local active snow update service.
- Added a dedicated mutation batch owner.
- Extracted chunk apply policy into a dedicated service.
- Extracted chunk melt policy into a dedicated service.
- Extracted snow-history aggregation into a dedicated query service.
- Added a pure snow accumulation policy for chunk scheduling decisions.
- Replaced the old local melt radius scan with bounded random sampling around players.
- Corrected Project Atmosphere local precipitation handling so positional rain checks are respected.
- Switched Snow Real Magic integration to coexistence mode so SRM keeps its immersive local snowfall tick instead of being suppressed.
- Upgraded the compatibility boundary to provider-owned queued mutations so non-vanilla managed snow can be applied and cleared without flattening to vanilla block assumptions.
- Added an adaptive Snow Real Magic adapter that reads SRM-managed layer counts and issues SRM-compatible conversion and clear mutations during reconciliation and melting.
- Corrected duplicate evaluation with Snow Real Magic and incomplete queue clearing.
