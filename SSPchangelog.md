# Serene Seasons Plus v5.2.1 -> Fabric & NeoForge 1.21.1

## Added

- Day/night cycles can now be configured beyond seven real-time hours, including a full 24-hour cycle, without synchronizing the world to the computer clock.

## Changed

- Renamed config categories, settings, and in-game labels so their purpose and units are clear.
- Added a plain-language description for every config setting and hover descriptions throughout the in-game config screen.
- On the first launch after updating, the old config is preserved as a backup, a fresh config is generated, and a one-time warning explains the reset.

# Serene Seasons Plus v5.1.2 -> NeoForge 1.21.1

## Fixed

- Fixed Better Days sleep cycles waking players at an incorrect time. SSP now applies a configurable wake time of 1000 (7:00 AM) after Better Days completes sleep.

# Serene Seasons Plus v5.1.1 -> 1.20.1 & 1.21.1 
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

