package com.Gabou.sereneseasonsextended.config;

import net.Gabou.projectatmosphere.ProjectAtmosphere;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Simple in-game configuration screen for Project Atmosphere.
 */
public class SereneExtendedScreen extends Screen {
    private final Screen parent;
    private boolean useAsync;
    private int tickSnowPillerThreshold;
    private int tickSnowReplacerThreshold;

    private EditBox maxPillerBox;

    private EditBox maxReplacerBox;

    private EditBox dayLengthBox;

    private EditBox nightLengthBox;

    private boolean seasonalDaylightCycle;

    private boolean customDayCycle;

    private double customDayLength;

    private double customNightLength;

    private Component replacerLabel = Component.literal("Tick Threshold Snow Replacer:");
    private Component pillerLabel = Component.literal("Tick Threshold Snow Piller:");
    private Component nightLabel = Component.literal("Custom Night Speed:");
    private Component dayLabel = Component.literal("Custom Day Speed:");



    public SereneExtendedScreen(Screen parent) {
        super(Component.literal("Project Atmosphere Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.setFocused(true);
        this.useAsync = SereneExtendedConfig.USE_ASYNC.get();
        this.tickSnowPillerThreshold = SereneExtendedConfig.TICK_SNOW_PILLER.get();
        this.tickSnowReplacerThreshold = SereneExtendedConfig.TICK_SNOW_REPLACER.get();
        this.seasonalDaylightCycle = SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.get();
        this.customDayCycle = SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.get();
        this.customDayLength = SereneExtendedConfig.CUSTOM_DAY_LENGTH.get();
        this.customNightLength = SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.get();


        int center = this.width / 2;
        int y = 40;

        addRenderableWidget(Button.builder(
                toggleLabel("Use Asynchronous Service", useAsync),
                b -> {
                    int cores = Runtime.getRuntime().availableProcessors();
                    if (cores <= SereneExtendedConfig.MIN_CORES_FOR_ASYNC) {
                        // Show a message instead of toggling
                        b.setMessage(Component.literal("Need 6 cores minimum to ensure performance"));
                        return;
                    }
                    useAsync = !useAsync;
                    b.setMessage(toggleLabel("Use Asynchronous Service", useAsync));
                }
        ).bounds(center - 100, y, 200, 20).build());
        y += 24;

        addRenderableWidget(Button.builder(toggleLabel("Seasonal Daylight Cycle", seasonalDaylightCycle), b -> {
            seasonalDaylightCycle = !seasonalDaylightCycle;
            customDayCycle = false;
            b.setMessage(toggleLabel("Enable automatic Seasonal Daylight Cycle", seasonalDaylightCycle));
        }).bounds(center - 100, y, 200, 20).build());
        y += 24;

        addRenderableWidget(Button.builder(toggleLabel("Custom Daylight Cycle", customDayCycle), b -> {
            customDayCycle = !customDayCycle;
            seasonalDaylightCycle = false;
            b.setMessage(toggleLabel("Enable custom Daylight Cycle", customDayCycle));
        }).bounds(center - 100, y, 200, 20).build());
        y += 24;

// Snow Replacer threshold label + box
        this.addRenderableWidget(new StringWidget(center - 100, y, 200, 10, replacerLabel, this.font));
        y += 10;
        this.maxReplacerBox = new EditBox(this.font, center - 100, y, 200, 20, Component.empty());
        this.maxReplacerBox.setValue(Integer.toString(tickSnowReplacerThreshold));
        addRenderableWidget(this.maxReplacerBox);
        y += 24;

// Snow Piller threshold label + box
        this.addRenderableWidget(new StringWidget(center - 100, y, 200, 10, pillerLabel, this.font));
        y += 10;
        this.maxPillerBox = new EditBox(this.font, center - 100, y, 200, 20, Component.empty());
        this.maxPillerBox.setValue(Integer.toString(tickSnowPillerThreshold));
        addRenderableWidget(this.maxPillerBox);
        y += 24;

// Night speed label + box
        this.addRenderableWidget(new StringWidget(center - 100, y, 200, 10, nightLabel, this.font));
        y += 10;
        this.nightLengthBox = new EditBox(this.font, center - 100, y, 200, 20, Component.empty());
        this.nightLengthBox.setValue(Double.toString(customNightLength));
        addRenderableWidget(this.nightLengthBox);
        y += 24;

// Day speed label + box
        this.addRenderableWidget(new StringWidget(center - 100, y, 200, 10, dayLabel, this.font));
        y += 10;
        this.dayLengthBox = new EditBox(this.font, center - 100, y, 200, 20, Component.empty());
        this.dayLengthBox.setValue(Double.toString(customDayLength));
        addRenderableWidget(this.dayLengthBox);
        y += 24;


        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            saveChanges();
            Minecraft.getInstance().setScreen(parent);
        }).bounds(center - 100, this.height - 27, 200, 20).build());
    }

    private Component toggleLabel(String name, boolean enabled) {
        return Component.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }

    private void saveChanges() {
        Component errorMessage;
        int parsed = this.tickSnowPillerThreshold;
        int parsed2 = this.tickSnowReplacerThreshold;
        double parsed3 = this.customDayLength;
        double parsed4 = this.customNightLength;

        try {
            parsed = Integer.parseInt(this.maxPillerBox.getValue());
            parsed2 = Integer.parseInt(this.maxReplacerBox.getValue());
            errorMessage = null;
        } catch (NumberFormatException ignored) {
            errorMessage = Component.literal("Invalid number for one of the Snow Tickers.");
        }

        try {
            parsed3 = Double.parseDouble(this.dayLengthBox.getValue());
            parsed4 = Double.parseDouble(this.nightLengthBox.getValue());
            errorMessage = null;
        } catch (NumberFormatException ignored) {
            errorMessage = Component.literal("Invalid number for one of the DayCycle Speeds.");
        }
        SereneExtendedConfig.USE_ASYNC.set(useAsync);
        SereneExtendedConfig.TICK_SNOW_PILLER.set(parsed);
        SereneExtendedConfig.TICK_SNOW_REPLACER.set(parsed2);
        SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.set(seasonalDaylightCycle);

        try {
            // EITHER A) save via ModConfig
            saveCommonConfigForMod(ProjectAtmosphere.MODID);

            // OR B) if you prefer and your SPEC is registered:
            // SereneExtendedConfig.SPEC.save();

            errorMessage = null;
        } catch (Exception e) {
            errorMessage = Component.literal("Failed to save config: " + e.getMessage());
        }
    }

    /** Finds this mod's COMMON config and saves it. */
    private static void saveCommonConfigForMod(String modId) {
        var set = ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.COMMON);
        if (set == null) return;
        for (ModConfig cfg : set) {
            if (cfg.getModId().equals(modId)) {
                cfg.save(); // writes to disk
                return;
            }
        }
    }

    @Override
    public void onClose() {
        this.setFocused(false);
        Minecraft.getInstance().setScreen(parent);
    }
}

