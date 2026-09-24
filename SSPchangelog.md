# Serene Seasons Plus v5.2.1 -> NeoForge 1.21.11

## Fixed

- Fixed Better Days sleep cycles waking players at an incorrect time. SSP now applies a configurable wake time of 1000 (7:00 AM) after Better Days completes sleep.

# Serene Seasons Plus v5.2.0

## Fixed

- Newly loaded chunks now add or melt their SSP-owned snow before the player sees the chunk, eliminating the delayed terrain pop-in after teleports.
- Chunk-load snow patterns are deterministic, so reloading a chunk no longer reshuffles its storm coverage.
- Snow placement now treats the `sereneseasonsplus:snow_replaceable` block tag as the single authority for replacing non-air blocks.
- Snow can no longer overwrite protected blocks through vanilla precipitation redirects or delayed world-mutation queues.
- Delayed snow changes now verify that the block still matches the state seen when the change was queued, protecting rails and other blocks placed in the meantime.
- Snow clearing no longer removes unrelated blocks when tracked snow data becomes stale.
- Fixed the config screen applying Minecraft's background blur twice.
- Fixed config-screen sizing, clipped controls, toggle state updates, validation feedback, and NeoForge config saving.
- Updated chunk snow restoration to the current per-storm synchronization behavior used by the other supported versions.

## Changed

- Natural ground-cover replacements are now declared explicitly in the snow-replaceable tag instead of being accepted by a hidden fallback.
- Removed the older snow-deficit and destroyed-column reapplication behavior.
- CurseForge publishing now uses Mod Publisher and produces release uploads for version 5.2.0.
