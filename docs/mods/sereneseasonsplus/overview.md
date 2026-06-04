# Serene Seasons Plus

## What It Does
Serene Seasons Plus is an addon for Serene Seasons that adds seasonal daylight behavior and season-aware snow behavior.

## Main Features
- Seasonal daylight cycle through Better Days integration.
- Optional custom day and night speed overrides.
- Snow accumulation, melting, and chunk-load snow reconciliation.
- Storm-history-based snow buildup in the current source tree.
- Optional real-time Canadian season sync using the `America/Toronto` timezone.
- Warm-season grass and flower growth.

## Who It Is For
- Players and pack authors already using Serene Seasons.
- Servers that want seasonal daylight changes.
- Worlds that want more persistent seasonal snow behavior.

## Required Dependencies
- Serene Seasons
- Better Days
- GlitchCore
- Fabric API on Fabric

## Optional Dependencies
- Project Atmosphere on Forge
- Mod Menu on Fabric
- Snow Real Magic

## Important Compatibility Notes
- Minecraft support in this source tree is `1.20.1`.
- Loader support in this source tree is Forge and Fabric.
- Project Atmosphere is only explicitly integrated on Forge in the current source tree.
- Current source and the `v5.0.0` changelog mention optional Snow Real Magic support. Confirm version scope when answering questions about older releases.
- Repository version labels are inconsistent:
  - `gradle.properties` uses `0.5.0.0`
  - `SSPchangelog.md` uses `v5.0.0-1.20.1`
  Treat public release naming as `TODO` until confirmed.
