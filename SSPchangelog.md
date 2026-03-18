# Serene Seasons Plus - v5.0.0-1.20.1

## Changelog
### Added
- Chunk-load snow reconciliation so chunks can restore expected snow state when they become relevant.
- A staged snow architecture with dedicated services for state access, chunk apply, chunk melt, local active updates, history queries, chunk-load reconciliation, and mutation batching.
- A shared snow column inspector and a pure accumulation policy layer.
- A compatibility boundary for managed snow states, including provider-owned queued mutations.
- Native optional Snow Real Magic support for reading and mutating supported managed snow states during reconciliation and melting.
### Fixed
- Duplicate snow evaluation paths that could schedule the same work more than once.
- Incomplete chunk queue clearing during lifecycle resets.
- Project Atmosphere rain handling so positional precipitation is respected instead of treating rain as global.
- Snow Real Magic coexistence so immersive local SRM snowfall still runs while SS+ avoids fighting it.
- Several cases where tracked snow state and queued world changes could drift more easily than necessary.
### Changed / Removed
- Refactored the snow system away from a single god class toward a long-term reconciler-based design.
- `CommonSnowBlockFeature` now acts as a facade that delegates to dedicated services instead of owning most snow behavior directly.
- Local active melting now uses bounded sampling instead of the older heavier radius scan.
- Chunk and world snow mutations now flow through a dedicated batch pipeline.

### Notes
- Please report any issues you find on the Discord server: https://discord.gg/2jRhTJgYz4
- Real-time Canadian season sync: aligns sub-seasons to real calendar months (Eastern time) so winter is sharp and other core seasons stay distinct.
- Planned next optimization: add a bounded fast lane for chunk-load and near-player snow so visible accumulation appears sooner without raising global TPS cost.
