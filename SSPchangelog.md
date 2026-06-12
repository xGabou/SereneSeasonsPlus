# Serene Seasons Plus v5.1.1

## Added

- Snow can now land on leaf canopies instead of only appearing underneath trees.
- Ice is now included in SSP snow processing, including melt handling.
- Storm application now tracks how many storms a chunk has already received instead of relying on destroyed-column reapply checks.

## Changed

- Snow placement now uses the real top surface, including leaves, for column sampling.
- Snow under leaves is no longer treated as exposed sky for placement and melt logic.
- Snow and ice melt checks are stricter and now avoid thawing ice during snowy season cold conditions.
- Chunk snow application now follows the server storm count rather than re-evaluating whether a chunk has the “right” snow amount.
- Active player-driven melt and chunk melt passes now use the same ice-handling rules.

## Fixed

- Snow no longer fails to appear on leaf tops.
- Snow on leaves now melts normally when it should.
- Ice no longer gets processed as if it were hidden snow under a roof.
- Winter ice should no longer slowly melt from the old heat-like fallback behavior.

## Removed

- Removed the old destroyed-column reapply behavior from snow placement and precipitation handling.
- Removed the old “did the player break this exact column” gating from storm reapplication.

