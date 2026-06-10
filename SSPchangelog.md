# Serene Seasons Plus - v5.1.0-1.20.1

## Highlights

- Snow updates should now appear much faster when chunks load or seasons change.
- Snow placement is safer around important blocks.
- Winter snow can still cover natural ground clutter like grass, tall grass, flowers, ferns, and leaf litter.
- Grass and flower regrowth is more natural and no longer only brings back dandelions.

## Fixed

- Snow should no longer delete important blocks such as rails, buttons, levers, and carpets.
- Improved snow replacement behavior so natural plants can be covered by snow without breaking protected blocks.
- Fixed the old Snow Real Magic compatibility path that could prevent normal snow logic from running correctly.
- Improved Better Days compatibility messaging so users know when SSP is changing Better Days time speeds.

## Improved

- Snow processing is now much more responsive without trying to do all work in one laggy tick.
- Chunk loading snow sync is faster and should feel closer to instant.
- Performance handling for snow replacement and melting has been smoothed out.
- Shared Fabric and Forge screens were cleaned up, reducing duplicate code and future maintenance issues.

## Notes

- THIS BUILD REQUIRES PA OVER 0.9.0.0. If you are using an older version of PA, please update to it. This build was released in preparation for PA v0.9.0.0.
- Better Days dynamic time compatibility is now clearly exposed as a config option.
- On newer versions, Better Days' own seasonal time system is preferred when available.
