# Snow Optimization Notes

These notes document possible algorithmic improvements for the physical snow system. They are not the current implementation plan. The planned direction is to use LOD-based snow rendering for large-scale visual snow coverage instead of making every distant chunk physically reconcile snow blocks.

## Context

The current snow system prioritizes correctness for real blocks:

- storm history and baseline snow accumulation
- tracked snow columns per chunk
- batched world mutations
- Snow Real Magic compatibility
- safeguards against deleting important blocks
- gradual queued apply and melt work

Further optimizing physical snow placement is possible, but every physical block update has a cost. If the goal is better loading time and large-scale visual coverage, rendering snow with LOD should be preferred over expanding block mutation work.

## Deferred Physical Snow Improvements

### 1. Budgeted Column Work Scheduler

Instead of processing whole chunk passes, store progress inside each chunk task.

Possible task state:

- chunk position
- task type: apply, melt, reconcile
- current column index from `0` to `255`
- priority
- retry attempts
- last processed tick

Each server tick would process a fixed amount of work:

- up to `X` chunk tasks
- up to `Y` columns per chunk task
- up to `Z` world mutations

This would make snow work predictable. The tradeoff is that chunk reconciliation may take several ticks instead of completing immediately.

### 2. Dirty Column Tracking

Track only columns that need recalculation instead of scanning all 256 chunk columns every time.

Columns could become dirty from:

- snow placement or removal
- player block placement or breaking
- chunk load
- weather transition
- season transition
- tracked snow state mismatch

This reduces useless scans, but it adds bookkeeping and requires reliable invalidation.

### 3. Cached Placement Anchors

Cache per-column snow placement metadata:

- anchor Y
- top managed snow Y
- whether the column can receive snow
- available snow-column status

Invalidate the cache when a block changes in that column.

This can reduce repeated heightmap and vertical scans, but incorrect invalidation would cause visible snow errors.

### 4. Priority Queues

Not all snow work has equal importance.

Suggested priority order:

- chunks near players
- active storm chunks visible to players
- loaded chunks after season change
- distant loaded chunks
- cleanup and reconciliation tasks

This improves perceived responsiveness without increasing total work.

### 5. Cached Global Snow Metrics

Values such as global minimum and average snow history can be cached while storm history is unchanged.

Examples:

- global minimum layer sum
- global average layer sum
- active storm record
- combined finished storm record

Recompute only when storm history changes.

### 6. Adaptive Cooldowns

Chunks that repeatedly produce no changes can wait longer before being reconsidered.

Example:

- first no-op: short cooldown
- repeated no-op: longer cooldown
- season/weather/block change: clear cooldown

This avoids hammering stable chunks, but requires careful invalidation when conditions change.

### 7. Gradual Season Sweeps

Season changes should not force immediate heavy recalculation of every loaded chunk.

Preferred approach:

- enqueue all loaded chunks
- prioritize player-near chunks
- process gradually
- skip chunks already known clean for the target season

This avoids season-transition lag spikes.

## Better Direction: LOD Snow Rendering

LOD rendering is likely a better long-term solution for distant or large-scale snow coverage.

Recommended split:

- physical snow blocks for nearby, interactive, gameplay-relevant areas
- LOD-rendered snow for distant terrain and visual continuity
- physical reconciliation only when chunks become relevant or player-near

This reduces world mutation cost and avoids spending server time making distant chunks physically accurate when the player only needs visual snow coverage.

## Practical Recommendation

Do not invest heavily in deeper physical snow reconciliation unless gameplay requires it. Keep the current physical system safe and bounded, then shift large-scale snow appearance to LOD rendering.

