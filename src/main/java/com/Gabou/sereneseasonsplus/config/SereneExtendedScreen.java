package com.Gabou.sereneseasonsplus.config;

import net.Gabou.projectatmosphere.ProjectAtmosphere;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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

    private SereneExtendedList list;



    public SereneExtendedScreen(Screen parent) {
        super(Component.literal("Project Atmosphere Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // read config values...
        this.useAsync = SereneExtendedConfig.USE_ASYNC.get();
        this.tickSnowPillerThreshold = SereneExtendedConfig.TICK_SNOW_PILLER.get();
        this.tickSnowReplacerThreshold = SereneExtendedConfig.TICK_SNOW_REPLACER.get();
        this.seasonalDaylightCycle = SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.get();
        this.customDayCycle = SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.get();
        this.customDayLength = SereneExtendedConfig.CUSTOM_DAY_LENGTH.get();
        this.customNightLength = SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.get();

        int panelW = 420;
        int panelX = (this.width - panelW) / 2;
        int top = 40;
        int bottom = this.height - 40;

        // Create the list (row height 24) and place it
        this.list = new SereneExtendedList(this.minecraft, panelW, this.height, top + 20, bottom - 40, 24);
        this.list.setLeftPos(panelX);
        this.addRenderableWidget(this.list);

        // Buttons
        var asyncBtn = Button.builder(toggleLabel("Use Asynchronous Service", useAsync), b -> {
            int cores = Runtime.getRuntime().availableProcessors();
            int minCores = SereneExtendedConfig.MIN_CORES_FOR_ASYNC;
            if (cores < minCores) {
                b.setMessage(Component.literal("Need " + minCores + "+ cores for Async"));
                return;
            }
            useAsync = !useAsync;
            b.setMessage(toggleLabel("Use Asynchronous Service", useAsync));
        }).bounds(0,0,200,20).build();
        this.list.addRow(Component.literal("Asynchronous Service"), asyncBtn);

        var seasonBtn = Button.builder(toggleLabel("Seasonal Daylight Cycle", seasonalDaylightCycle), b -> {
            seasonalDaylightCycle = !seasonalDaylightCycle;
            customDayCycle = false;
            b.setMessage(toggleLabel("Seasonal Daylight Cycle", seasonalDaylightCycle));
        }).bounds(0,0,200,20).build();
        this.list.addRow(Component.literal("Seasonal Daylight Cycle"), seasonBtn);

        var customBtn = Button.builder(toggleLabel("Custom Daylight Cycle", customDayCycle), b -> {
            customDayCycle = !customDayCycle;
            seasonalDaylightCycle = false;
            b.setMessage(toggleLabel("Custom Daylight Cycle", customDayCycle));
        }).bounds(0,0,200,20).build();
        this.list.addRow(Component.literal("Custom Daylight Cycle"), customBtn);

        // EditBoxes — create ONCE, height=20, do NOT add to screen
        this.maxReplacerBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.maxReplacerBox.setValue(Integer.toString(tickSnowReplacerThreshold));
        this.list.addRow(Component.literal("Tick Threshold Snow Replacer"), this.maxReplacerBox);

        this.maxPillerBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.maxPillerBox.setValue(Integer.toString(tickSnowPillerThreshold));
        this.list.addRow(Component.literal("Tick Threshold Snow Piller"), this.maxPillerBox);

        this.nightLengthBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.nightLengthBox.setValue(Double.toString(customNightLength));
        this.list.addRow(Component.literal("Custom Night Speed"), this.nightLengthBox);

        this.dayLengthBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.dayLengthBox.setValue(Double.toString(customDayLength));
        this.list.addRow(Component.literal("Custom Day Speed"), this.dayLengthBox);

        // Only the list + Done go on the screen; NOT the boxes.
        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), b -> {
                    saveChanges();
                    this.minecraft.setScreen(parent);
                }).bounds(panelX + (panelW - 200) / 2, bottom - 30, 200, 20).build()
        );
    }


    /** Draw dim background + centered panel so it looks modal/focused. */
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g); // dims everything behind

        // optional panel chrome
        int panelW = 480;
        int panelX = (this.width - panelW) / 2;
        int top = 40;
        int bottom = this.height - 40;
        g.fill(panelX - 4, top - 4, panelX + panelW + 4, bottom, 0xAA000000); // dark box
        g.drawString(this.font, "Serene Seasons Plus", panelX + 6, top - 14, 0xFFFFFF, false);
        super.render(g, mouseX, mouseY, partialTick); // renders list + buttons
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
        SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.set(customDayCycle);
        SereneExtendedConfig.CUSTOM_DAY_LENGTH.set(parsed3);
        SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.set(parsed4);

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
    public static void saveCommonConfigForMod(String modId) {
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

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (this.list != null && this.list.mouseClicked(x, y, button)) return true;
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean keyPressed(int key, int sc, int mods) {
        if (this.list != null && this.list.keyPressed(key, sc, mods)) return true;

        // optional belt-and-suspenders: forward to boxes too
        if (this.maxReplacerBox != null && this.maxReplacerBox.keyPressed(key, sc, mods)) return true;
        if (this.maxPillerBox   != null && this.maxPillerBox.keyPressed(key, sc, mods)) return true;
        if (this.nightLengthBox != null && this.nightLengthBox.keyPressed(key, sc, mods)) return true;
        if (this.dayLengthBox   != null && this.dayLengthBox.keyPressed(key, sc, mods)) return true;

        return super.keyPressed(key, sc, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (this.list != null && this.list.charTyped(c, mods)) return true;

        if (this.maxReplacerBox != null && this.maxReplacerBox.charTyped(c, mods)) return true;
        if (this.maxPillerBox   != null && this.maxPillerBox.charTyped(c, mods)) return true;
        if (this.nightLengthBox != null && this.nightLengthBox.charTyped(c, mods)) return true;
        if (this.dayLengthBox   != null && this.dayLengthBox.charTyped(c, mods)) return true;

        return super.charTyped(c, mods);
    }


}

