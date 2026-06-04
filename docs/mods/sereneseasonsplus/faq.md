# FAQ

## Does Serene Seasons Plus Add Commands?
No confirmed in-game commands were found in the current source tree.

## Does It Require Serene Seasons?
Yes.

## Does It Require Better Days?
Yes.

## Does It Require GlitchCore?
Yes in the current source and published dependency configuration.

## Does It Require Fabric API?
Yes on Fabric.

## Does It Work With Project Atmosphere?
Official source integration exists on Forge. Do not claim Fabric support unless you have separate confirmation.

## Does It Work With Snow Real Magic?
Current source and the `v5.0.0` changelog mention optional support. Confirm the exact released version before answering questions about older builds.

## Which Minecraft Versions Are Supported?
This source tree targets `1.20.1`.

## Which Loaders Are Supported?
Forge and Fabric.

## Where Is The Config File?
- Fabric: `config/sereneseasonsplus.json`
- Forge: auto-generated common config file for the mod

## Why Are My Custom Day/Night Values Ignored?
Because seasonal daylight takes priority when `enableSeasonalDaylightCycle=true`.

## Why Is Snow Behavior Not Instant?
The current snow system batches and throttles chunk work. A future fast-lane optimization is planned, but it is not documented as released yet.

## What Version Should Support Use?
Current repository metadata disagrees:
- `mod_version=0.5.0.0`
- release notes label `v5.0.0-1.20.1`
Treat the public release name as `TODO` until confirmed.
