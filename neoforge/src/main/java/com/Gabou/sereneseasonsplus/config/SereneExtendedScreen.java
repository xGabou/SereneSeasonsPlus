package com.Gabou.sereneseasonsplus.config;

import com.Gabou.sereneseasonsplus.SereneSeasonsPlusNeoForge;
import com.Gabou.sereneseasonsplus.util.IScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Simple in-game configuration screen.
 */
public class SereneExtendedScreen extends Screen {
    private final Screen parent;
    private boolean snowFeatureEnabled;
    private int tickSnowReplacerThreshold;
    private int maxSnowHeight;

    private EditBox maxReplacerBox;
    private EditBox maxSnowHeightBox;
    private EditBox dayLengthBox;

    private EditBox nightLengthBox;

    private boolean seasonalDaylightCycle;

    private boolean customDayCycle;

    private double customDayLength;

    private double customNightLength;

    private boolean grassFlowerGrowth;

    private Component replacerLabel = Component.literal("Common Feature Threshold:");
    private Component pillerLabel = Component.literal("");
    private Component snowHeightLabel = Component.literal("Max Snow Height (layers):");
    private Component nightLabel = Component.literal("Custom Night Speed:");
    private Component dayLabel = Component.literal("Custom Day Speed:");
    private Component grassFlowerLabel = Component.literal("Grass Flower:");

    private SereneExtendedList list;
    /**
     * Creates the configuration screen.
     *
     * @param parent parent screen to return to
     */
    public SereneExtendedScreen(Screen parent) {
        super(Component.literal("Serene Seasons Plus Config"));
        this.parent = parent;
    }

    @Override
    /**
     * Initializes widgets and loads values from the config.
     */
    protected void init() {
        this.snowFeatureEnabled = SereneExtendedConfig.SNOWSTORM_ENABLED.get();
        this.tickSnowReplacerThreshold = SereneExtendedConfig.TICK_SNOW_REPLACER.get();
        this.maxSnowHeight = SereneExtendedConfig.MAX_SNOW_ACCUMULATION_LAYERS.get();
        this.seasonalDaylightCycle = SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.get();
        this.customDayCycle = SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.get();
        this.customDayLength = SereneExtendedConfig.CUSTOM_DAY_LENGTH.get();
        this.customNightLength = SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.get();
        this.grassFlowerGrowth = SereneExtendedConfig.GRASS_FLOWER_GROWTH_ENABLED.get();

        int panelW = 420;
        int panelX = (this.width - panelW) / 2;
        int top = 40;
        int bottom = this.height - 40;

        this.list = new SereneExtendedList(this.minecraft, panelW, this.height, top + 20, 24);
        try {
            this.list.getClass().getMethod("setX", int.class).invoke(this.list, panelX);
        } catch (Throwable t) {

        }
        this.addRenderableWidget(this.list);

        var snowFeatureBtn = Button.builder(toggleLabel("Snow Features", snowFeatureEnabled), b -> {
            snowFeatureEnabled = !snowFeatureEnabled;
            b.setMessage(toggleLabel("Snow Features", snowFeatureEnabled));
        }).bounds(0,0,200,20).build();
        this.list.addRow(Component.literal("Snow Features"), snowFeatureBtn);

        var seasonBtn = Button.builder(toggleLabel("Seasonal Daylight Cycle", seasonalDaylightCycle), b -> {
            seasonalDaylightCycle = !seasonalDaylightCycle;
            customDayCycle = false;
            b.setMessage(toggleLabel("Seasonal Daylight Cycle", seasonalDaylightCycle));
        }).bounds(0,0,200,20).build();
        this.list.addRow(Component.literal("Seasonal Daylight Cycle"), seasonBtn);

        var grassFlowerBtn = Button.builder(toggleLabel("Grass and Flower Growth", grassFlowerGrowth), b -> {
            grassFlowerGrowth = !grassFlowerGrowth;
            b.setMessage(toggleLabel("Grass and Flower Growth", grassFlowerGrowth));
        }).bounds(0,0,200,20).build();
        this.list.addRow(Component.literal("Grass and Flower Growth"), grassFlowerBtn);

        var customBtn = Button.builder(toggleLabel("Custom Daylight Cycle", customDayCycle), b -> {
            customDayCycle = !customDayCycle;
            seasonalDaylightCycle = false;
            b.setMessage(toggleLabel("Custom Daylight Cycle", customDayCycle));
        }).bounds(0,0,200,20).build();
        this.list.addRow(Component.literal("Custom Daylight Cycle"), customBtn);


        this.maxReplacerBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.maxReplacerBox.setValue(Integer.toString(tickSnowReplacerThreshold));
        this.list.addRow(Component.literal("Common Feature Threshold"), this.maxReplacerBox);

        this.maxSnowHeightBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.maxSnowHeightBox.setValue(Integer.toString(this.maxSnowHeight));
        this.list.addRow(Component.literal("Max Snow Height (layers)"), this.maxSnowHeightBox);

        this.nightLengthBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.nightLengthBox.setValue(Double.toString(customNightLength));
        this.list.addRow(Component.literal("Custom Night Speed"), this.nightLengthBox);

        this.dayLengthBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.dayLengthBox.setValue(Double.toString(customDayLength));
        this.list.addRow(Component.literal("Custom Day Speed"), this.dayLengthBox);


        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), b -> {
                    saveChanges();
                    this.minecraft.setScreen(parent);
                }).bounds(panelX + (panelW - 200) / 2, bottom - 30, 200, 20).build()
        );
    }


    @Override
    /**
     * Draws background, panel chrome, and delegates to list/widgets.
     */
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);

        int panelW = 480;
        int panelX = (this.width - panelW) / 2;
        int top = 40;
        int bottom = this.height - 40;
        g.fill(panelX - 4, top - 4, panelX + panelW + 4, bottom, 0xAA000000);
        g.drawString(this.font, "Serene Seasons Plus", panelX + 6, top - 14, 0xFFFFFF, false);
        ((IScreen)(Object)this).sereneseasonsplus$renderNoBackground(g, mouseX, mouseY, partialTick);
    }

    /**
     * Formats a toggle label with ON/OFF state.
     */
    private Component toggleLabel(String name, boolean enabled) {
        return Component.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }

    /**
     * Validates inputs and writes changes to config, then persists them.
     */
    private void saveChanges() {
        Component errorMessage;
        int parsed2 = this.tickSnowReplacerThreshold;
        int parsedSnowHeight = this.maxSnowHeight;
        double parsed3 = this.customDayLength;
        double parsed4 = this.customNightLength;


        try {
            parsed2 = Integer.parseInt(this.maxReplacerBox.getValue());
            parsedSnowHeight = Integer.parseInt(this.maxSnowHeightBox.getValue());
            errorMessage = null;
        } catch (NumberFormatException ignored) {
            errorMessage = Component.literal("Invalid number for a Snow setting.");
        }

        try {
            parsed3 = Double.parseDouble(this.dayLengthBox.getValue());
            parsed4 = Double.parseDouble(this.nightLengthBox.getValue());
            errorMessage = null;
        } catch (NumberFormatException ignored) {
            errorMessage = Component.literal("Invalid number for one of the DayCycle Speeds.");
        }
        SereneExtendedConfig.TICK_SNOW_REPLACER.set(parsed2);
        SereneExtendedConfig.SNOWSTORM_ENABLED.set(snowFeatureEnabled);
        SereneExtendedConfig.MAX_SNOW_ACCUMULATION_LAYERS.set(parsedSnowHeight);
        SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.set(seasonalDaylightCycle);
        SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.set(customDayCycle);
        SereneExtendedConfig.CUSTOM_DAY_LENGTH.set(parsed3);
        SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.set(parsed4);
        SereneExtendedConfig.GRASS_FLOWER_GROWTH_ENABLED.set(grassFlowerGrowth);

        try {
            saveToFile();
            errorMessage = null;
        } catch (Exception e) {
            errorMessage = Component.literal("Failed to save config: " + e.getMessage());
        }
    }

    /**
     * Persists current settings to the TOML config using NightConfig.
     */
    private void saveToFile() {
        var path = net.neoforged.fml.loading.FMLPaths.CONFIGDIR
                .get()
                .resolve(SereneSeasonsPlusNeoForge.MODID + "-common.toml");

        // Build & load config file safely
        var cfg = com.electronwill.nightconfig.core.file.CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .preserveInsertionOrder()
                .build();
        cfg.load();

        // Update only known keys
        cfg.set("snowstorm.enabled", snowFeatureEnabled);
        cfg.set("snowStorms.maxSnowAccumulationLayers", maxSnowHeight);
        cfg.set("snowPillerAndReplacer.tickSnowReplacer", tickSnowReplacerThreshold);
        cfg.set("seasonalDaylightCycle.enableSeasonalDaylightCycle", seasonalDaylightCycle);
        cfg.set("seasonalDaylightCycle.customCycleLength", customDayCycle);
        cfg.set("seasonalDaylightCycle.customDayLength", customDayLength);
        cfg.set("seasonalDaylightCycle.customNightLength", customNightLength);
        cfg.set("grassFlowerGrowth.enabled", grassFlowerGrowth);

        cfg.save();
        cfg.close();
    }


    @Override
    /**
     * Closes the screen and returns to the parent.
     */
    public void onClose() {
        this.setFocused(false);
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    /**
     * Delegates click handling to the list and then widgets.
     */
    public boolean mouseClicked(double x, double y, int button) {
        if (this.list != null && this.list.mouseClicked(x, y, button)) return true;
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean keyPressed(int key, int sc, int mods) {
        if (this.list != null && this.list.keyPressed(key, sc, mods)) return true;


        if (this.maxReplacerBox != null && this.maxReplacerBox.keyPressed(key, sc, mods)) return true;
        if (this.maxSnowHeightBox   != null && this.maxSnowHeightBox.keyPressed(key, sc, mods)) return true;
        if (this.nightLengthBox != null && this.nightLengthBox.keyPressed(key, sc, mods)) return true;
        if (this.dayLengthBox   != null && this.dayLengthBox.keyPressed(key, sc, mods)) return true;

        return super.keyPressed(key, sc, mods);
    }

    /**
     * Forwards typed characters to the list and text boxes.
     */
    @Override
    public boolean charTyped(char c, int mods) {
        if (this.list != null && this.list.charTyped(c, mods)) return true;

        if (this.maxReplacerBox != null && this.maxReplacerBox.charTyped(c, mods)) return true;
        if (this.maxSnowHeightBox   != null && this.maxSnowHeightBox.charTyped(c, mods)) return true;
        if (this.nightLengthBox != null && this.nightLengthBox.charTyped(c, mods)) return true;
        if (this.dayLengthBox   != null && this.dayLengthBox.charTyped(c, mods)) return true;

        return super.charTyped(c, mods);
    }


}

