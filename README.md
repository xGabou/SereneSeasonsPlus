# Serene Seasons Plus

## Fixed-length seasonal cycles

Enable `dayNightCycle.keepFullDayNightCycleAtFixedLength`, then set `fullDayNightCycleLengthInRealMinutes` to the desired elapsed duration of a complete day and night. Use `1440` for a 24-hour cycle. Daylight and darkness are redistributed by the current in-game sub-season; this setting does not use the computer's date or clock. Leave the option disabled to preserve the original seasonal speed behavior.
Day and night length now depends on the current season (longer days in summer, longer nights in winter), and snow blocks melt based on biome temperature — except during the winter season. During snowfall, snow blocks now pile up indefinitely for a more realistic buildup.
