# Snow Refactor Plan

## Goal
Move the snow system from a monolithic feature class toward a staged architecture without breaking saved chunk data or storm history.

## Implemented Stage

### New classes introduced in this pass
- `common/src/main/java/com/Gabou/sereneseasonsplus/features/SnowBlockCompatibility.java`
  - Contract for managed snow and ice state interpretation.
- `common/src/main/java/com/Gabou/sereneseasonsplus/features/SnowWorldMutation.java`
  - Provider-owned queued world mutation contract.
- `common/src/main/java/com/Gabou/sereneseasonsplus/features/AdaptiveSnowBlockCompatibility.java`
  - Runtime compatibility facade that combines safe vanilla handling with optional external adapters.
- `common/src/main/java/com/Gabou/sereneseasonsplus/features/VanillaSnowBlockCompatibility.java`
  - Default safe implementation for vanilla snow and ice.
- `common/src/main/java/com/Gabou/sereneseasonsplus/features/SnowRealMagicCompatibilityAdapter.java`
  - Optional native adapter for Snow Real Magic managed snow states and mutations.
- `common/src/main/java/com/Gabou/sereneseasonsplus/features/SnowColumnInspector.java`
  - Shared column topology and layer-total inspector.
- `common/src/main/java/com/Gabou/sereneseasonsplus/features/SnowMutationBatch.java`
  - Owner of queued world mutations, tracked-column pending updates, and dirty chunk marking.
- `common/src/main/java/com/Gabou/sereneseasonsplus/features/SnowChunkLoadReconciler.java`
  - Owner of chunk load deduplication, metadata warmup, and initial apply or melt scheduling.
- `common/src/main/java/com/Gabou/sereneseasonsplus/features/SnowStateService.java`
  - Wrapper around tracked chunk snow state operations.
- `common/src/main/java/com/Gabou/sereneseasonsplus/features/ActiveSnowUpdateService.java`
  - Owner of near-player dynamic piling and melting.
- `common/src/main/java/com/Gabou/sereneseasonsplus/features/SnowHistoryQueryService.java`
  - Owner of storm-history aggregation queries.
- `common/src/main/java/com/Gabou/sereneseasonsplus/features/SnowChunkApplyService.java`
  - Owner of historical baseline application and storm-pattern piling.
- `common/src/main/java/com/Gabou/sereneseasonsplus/features/SnowChunkMeltService.java`
  - Owner of melt passes and thaw behavior.
- `common/src/main/java/com/Gabou/sereneseasonsplus/features/logic/SnowAccumulationPolicy.java`
  - Pure decision layer for chunk apply or melt scheduling.

### Existing class responsibilities after this pass
- `CommonSnowBlockFeature`
  - Public entry facade.
  - Delegates chunk load work, tracked-state access, local active updates, history queries, apply policy, melt policy, block interpretation, and batch mutation work to dedicated components.
- `SnowLogic`
  - Chunk-level policy decision for when to enqueue apply or melt work.
- `SnowChunkWeatherLogic`
  - Tick-time integration point with vanilla weather behavior.
- `ISnowTrackedChunk` and chunk mixins
  - Persistent chunk state model.

## Next Stages

### Stage 2
- Add reconciliation reason types and metrics so chunk-load, active-storm, warm-melt, and local-update work can be profiled independently.
- Add a bounded fast-lane scheduler for visible snow work:
  - prioritize `CHUNK_LOAD`, `LOCAL_NEAR_PLAYER`, and active snowfall ahead of background reconciliation
  - reserve a small per-tick burst budget for newly loaded and player-relevant chunks
  - keep distant historical reconcile and cleanup on the existing conservative slow lane
  - optionally shorten evaluation cadence only for chunks that are near players or actively snowing
  - preserve TPS by reordering visible work instead of increasing total global work

### Stage 3
- Optionally expose compatibility provider registration through API.
- Add additional native providers for supported external snow mods beyond Snow Real Magic.

### Stage 4
- Tighten tracked-state finalization so queued mutation intent and chunk maps converge even under chunk unload edge cases.

## File-Level Change Plan

### Keep stable
- `ISnowTrackedChunk`
- chunk serializer mixins
- `SnowHistorySavedData`
- `SnowSavedData`

### Continue to slim down
- `CommonSnowBlockFeature`
  - Remove remaining placement and melt policy once extracted.
- `ServerLevelMixin`
  - Keep only the minimal integration hooks needed to invoke shared logic.

## Safety Rules
- Do not change chunk NBT field names without a migration.
- Do not replace unsupported external snow blocks unless a provider explicitly claims them.
- Keep chunk load reconciliation chunk-local and bounded.
- Keep batch world mutation separate from decision logic.
- When Snow Real Magic is present, prefer coexistence boundaries over blanket cancellation of its local snowfall tick.
- When an external provider owns a managed snow state, queue provider-specific mutations instead of flattening that state into vanilla block writes.
