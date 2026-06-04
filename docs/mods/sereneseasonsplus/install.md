# Install Serene Seasons Plus

## Prerequisites
- Minecraft `1.20.1`
- One supported loader:
  - Forge
  - Fabric

## Required Dependencies
- Serene Seasons
- Better Days
- GlitchCore
- Fabric API on Fabric

## Optional Dependencies
- Project Atmosphere on Forge
- Mod Menu on Fabric
- Snow Real Magic

## Install Order
1. Install the correct loader for Minecraft `1.20.1`.
2. Install required dependencies for that loader.
3. Install Serene Seasons Plus.
4. Install optional compatibility mods after the required dependencies.

## Basic Verification
- The game starts without missing dependency errors.
- The mod list shows `Serene Seasons Plus`.
- Fabric creates `config/sereneseasonsplus.json`.
- Forge creates its common config automatically on first launch.
- If seasonal daylight is enabled, day/night speed changes should follow the active Serene Seasons sub-season.

## Loader Notes
### Forge
- `projectatmosphere` is optional in `mods.toml`.
- Use the Forge `1.20.1` build only.

### Fabric
- `fabric.mod.json` requires Fabric Loader, Fabric API, Better Days, and Serene Seasons.
- Mod Menu is suggested, not required.

## If Installation Fails
- Re-check Minecraft version first.
- Re-check loader first.
- Re-check required dependencies first.
- Then see [troubleshooting.md](/G:/mods/SereneSeasonExtendedFix/docs/mods/sereneseasonsplus/troubleshooting.md).
