# Troubleshooting

## Common Install Mistakes
### Wrong Minecraft Version
- This source tree only targets `1.20.1`.

### Missing Required Dependencies
- Serene Seasons Plus requires:
  - Serene Seasons
  - Better Days
  - GlitchCore
  - Fabric API on Fabric

### Wrong Loader Build
- Forge and Fabric builds are separate.
- Do not mix them.

## Common Startup Or Crash Causes
### Missing Dependency Errors
- Install the required mods listed above.

### Optional Mod Assumptions
- Project Atmosphere is optional and only explicitly integrated on Forge in the current source tree.
- Snow Real Magic support is present in the current source and `v5.0.0` changelog, but older release support should be verified before promising it.

## Common Config Mistakes
### Custom Day Or Night Values Do Nothing
- `enableSeasonalDaylightCycle=true` takes priority.
- To use custom values, disable seasonal daylight and enable `customCycleLength`.

### Fabric Real-Time Canadian Seasons Does Not Persist
- Current source review shows `realTimeCanadianSeasons` is defined on Fabric but not loaded or saved by the Fabric config file.
- Treat this as a current known issue.

### tickSnowPiller Appears To Do Nothing
- Current source review found no active use of `tickSnowPiller`.
- Treat it as a current known issue until confirmed otherwise.

## Workaround Steps
- Start from a clean dependency set for the correct loader.
- Test without optional compatibility mods first.
- Re-enable optional mods one at a time.
- On Fabric, re-check `sereneseasonsplus.json` after changing settings.

## When To Check Known Issues
- If the problem matches one of the source-reviewed issues in `known-issues.json`.
- If config changes do not persist.
- If a setting appears to have no effect.
