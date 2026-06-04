# Serene Seasons Plus Support Changelog

## 0.5.0.0 / 5.0.0
Status: version label needs confirmation

### Added
- Chunk-load snow reconciliation.
- Staged snow services for apply, melt, state access, local updates, history queries, reconciliation, and batching.
- Shared snow column inspection and a dedicated accumulation policy layer.
- Provider-owned mutation handling for managed snow states.
- Optional Snow Real Magic support described in current source and release notes.

### Fixed
- Duplicate snow evaluation scheduling.
- Incomplete chunk queue clearing.
- Project Atmosphere rain-position handling.
- Snow Real Magic coexistence with local snowfall handling.
- Multiple tracked-state and queued-mutation consistency paths.

### Changed
- Snow behavior was refactored toward a reconciliation-based architecture.
- Local active melting now uses bounded sampling.
- Snow mutations now flow through a dedicated batch pipeline.

## 4.0.6
Source: forge/changelog.md

### Added
- `snowToggle` option for chunk-based snow features.
- `snowHeight` config option.

### Fixed
- Snow immediately respawning after being broken by a player.
- Tick crash with Project Atmosphere.

### Changed
- Removed outdated asynchronous snow logic.

## 4.0.4
Source: fabric/changelog.md

### Added
- `snowToggle` option for chunk-based snow features.
- `snowHeight` config option.

### Fixed
- Snow immediately respawning after being broken by a player.
- Tick crash with Project Atmosphere.

### Changed
- Removed outdated asynchronous snow logic.
