package com.Gabou.sereneseasonsplus.config;

import com.Gabou.sereneseasonsplus.client.config.SereneExtendedList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Simple in-game configuration screen for Project Atmosphere.
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
    private EditBox fullDayNightCycleLengthBox;

    private boolean seasonalDaylightCycle;

    private boolean betterDaysDynamicTimeCompat;
    private boolean keepFullDayNightCycleAtFixedLength;

    private boolean customDayCycle;

    private double customDayLength;

    private double customNightLength;
    private double fullDayNightCycleLengthInRealMinutes;

    private boolean grassFlowerGrowth;

    private boolean realTimeCanadianSeasons;

    private Component replacerLabel = Component.literal("Snow Replacement Interval (ticks):");
    private Component pillerLabel = Component.literal("");
    private Component snowHeightLabel = Component.literal("Maximum Snow Layers per Column:");
    private Component nightLabel = Component.literal("Custom Night Speed Multiplier:");
    private Component dayLabel = Component.literal("Custom Day Speed Multiplier:");

    private SereneExtendedList list;



    /**
     * Constructs the Serene Seasons Plus configuration screen.
     *
     * @param parent screen to return to when closed
     */
    public SereneExtendedScreen(Screen parent) {
        super(Component.literal("Serene Seasons Plus Config"));
        this.parent = parent;
    }

    /**
     * Initializes UI widgets and populates them from current config values.
     */
    @Override
    protected void init() {
        
        this.snowFeatureEnabled = SereneExtendedConfig.SNOWSTORM_ENABLED.get();
        this.tickSnowReplacerThreshold = SereneExtendedConfig.TICK_SNOW_REPLACER.get();
        this.maxSnowHeight = SereneExtendedConfig.MAX_SNOW_ACCUMULATION_LAYERS.get();
        this.seasonalDaylightCycle = SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.get();
        this.betterDaysDynamicTimeCompat = SereneExtendedConfig.ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT.get();
        this.keepFullDayNightCycleAtFixedLength = SereneExtendedConfig.KEEP_FULL_DAY_NIGHT_CYCLE_AT_FIXED_LENGTH.get();
        this.fullDayNightCycleLengthInRealMinutes = SereneExtendedConfig.FULL_DAY_NIGHT_CYCLE_LENGTH_IN_REAL_MINUTES.get();
        this.customDayCycle = SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.get();
        this.customDayLength = SereneExtendedConfig.CUSTOM_DAY_LENGTH.get();
        this.customNightLength = SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.get();
        this.grassFlowerGrowth = SereneExtendedConfig.GRASS_FLOWER_GROWTH_ENABLED.get();
        this.realTimeCanadianSeasons = SereneExtendedConfig.REAL_TIME_CANADIAN_SEASONS.get();


        int panelW = 420;
        int panelX = (this.width - panelW) / 2;
        int top = 40;
        int bottom = this.height - 40;

        
        this.list = new SereneExtendedList(this.minecraft, panelW, this.height, top + 20, bottom - 40, 24);
        this.list.setLeftPos(panelX);
        this.addRenderableWidget(this.list);

        
        var snowFeatureBtn = Button.builder(toggleLabel("Seasonal Snow Accumulation", snowFeatureEnabled), b -> {
            snowFeatureEnabled = !snowFeatureEnabled;
            b.setMessage(toggleLabel("Seasonal Snow Accumulation", snowFeatureEnabled));
        }).bounds(0,0,200,20).build();
        snowFeatureBtn.setTooltip(Tooltip.create(Component.literal("Allows the mod to place, replace, and accumulate snow during supported seasonal weather.")));
        this.list.addRow(Component.literal("Seasonal Snow Accumulation"), snowFeatureBtn);

        var seasonBtn = Button.builder(toggleLabel("Season-Based Day/Night Lengths", seasonalDaylightCycle), b -> {
            seasonalDaylightCycle = !seasonalDaylightCycle;
            customDayCycle = false;
            b.setMessage(toggleLabel("Season-Based Day/Night Lengths", seasonalDaylightCycle));
        }).bounds(0,0,200,20).build();
        seasonBtn.setTooltip(Tooltip.create(Component.literal("Changes daylight and nighttime proportions using the current in-game sub-season.")));
        this.list.addRow(Component.literal("Season-Based Day/Night Lengths"), seasonBtn);

        var betterDaysCompatBtn = Button.builder(toggleLabel("Seasonal Better Days Time Control", betterDaysDynamicTimeCompat), b -> {
            betterDaysDynamicTimeCompat = !betterDaysDynamicTimeCompat;
            b.setMessage(toggleLabel("Seasonal Better Days Time Control", betterDaysDynamicTimeCompat));
        }).bounds(0,0,200,20).build();
        betterDaysCompatBtn.setTooltip(Tooltip.create(Component.literal("Allows Serene Seasons Plus to control Better Days day and night speed values.")));
        this.list.addRow(Component.literal("Seasonal Better Days Time Control"), betterDaysCompatBtn);

        var fixedCycleBtn = Button.builder(toggleLabel("Fixed Full Day/Night Cycle Length", keepFullDayNightCycleAtFixedLength), b -> {
            keepFullDayNightCycleAtFixedLength = !keepFullDayNightCycleAtFixedLength;
            b.setMessage(toggleLabel("Fixed Full Day/Night Cycle Length", keepFullDayNightCycleAtFixedLength));
        }).bounds(0,0,200,20).build();
        fixedCycleBtn.setTooltip(Tooltip.create(Component.literal("Keeps one complete day and night at the configured elapsed duration while seasons change the daylight/night split. This does not sync to the computer clock.")));
        this.list.addRow(Component.literal("Fixed Full Day/Night Cycle Length"), fixedCycleBtn);

        var customBtn = Button.builder(toggleLabel("Custom Day/Night Speed Multipliers", customDayCycle), b -> {
            customDayCycle = !customDayCycle;
            seasonalDaylightCycle = false;
            b.setMessage(toggleLabel("Custom Day/Night Speed Multipliers", customDayCycle));
        }).bounds(0,0,200,20).build();
        customBtn.setTooltip(Tooltip.create(Component.literal("Uses the custom day and night speed multipliers when seasonal day/night changes are disabled.")));
        this.list.addRow(Component.literal("Custom Day/Night Speed Multipliers"), customBtn);

        var realTimeBtn = Button.builder(toggleLabel("Eastern Canada Calendar Sync", realTimeCanadianSeasons), b -> {
            realTimeCanadianSeasons = !realTimeCanadianSeasons;
            b.setMessage(toggleLabel("Eastern Canada Calendar Sync", realTimeCanadianSeasons));
        }).bounds(0,0,200,20).build();
        realTimeBtn.setTooltip(Tooltip.create(Component.literal("Synchronizes seasons with the current date in the America/Toronto time zone. Leave off for normal in-game progression.")));
        this.list.addRow(Component.literal("Eastern Canada Calendar Sync"), realTimeBtn);


        var grassFlowerBtn = Button.builder(toggleLabel("Seasonal Grass/Flower Growth", grassFlowerGrowth), b -> {
            grassFlowerGrowth = !grassFlowerGrowth;
            b.setMessage(toggleLabel("Seasonal Grass/Flower Growth", grassFlowerGrowth));
        }).bounds(0,0,200,20).build();
        grassFlowerBtn.setTooltip(Tooltip.create(Component.literal("Allows extra grass and flower growth during warm seasons.")));
        this.list.addRow(Component.literal("Seasonal Grass/Flower Growth"), grassFlowerBtn);

        
        this.maxReplacerBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.maxReplacerBox.setValue(Integer.toString(tickSnowReplacerThreshold));
        this.maxReplacerBox.setTooltip(Tooltip.create(Component.literal("Minecraft ticks between snow replacement and accumulation scans. Lower values update more frequently but use more processing.")));
        this.list.addRow(Component.literal("Snow Replacement Interval (ticks)"), this.maxReplacerBox);

        this.maxSnowHeightBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.maxSnowHeightBox.setValue(Integer.toString(this.maxSnowHeight));
        this.maxSnowHeightBox.setTooltip(Tooltip.create(Component.literal("Maximum snow layers in one vertical column. Eight layers equal one full snow block.")));
        this.list.addRow(Component.literal("Maximum Snow Layers per Column"), this.maxSnowHeightBox);

        this.fullDayNightCycleLengthBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.fullDayNightCycleLengthBox.setValue(Double.toString(fullDayNightCycleLengthInRealMinutes));
        this.fullDayNightCycleLengthBox.setTooltip(Tooltip.create(Component.literal("Elapsed real-world minutes for one complete day and night in fixed-length mode. Use 1440 for 24 hours.")));
        this.list.addRow(Component.literal("Full Cycle Length (Real Minutes)"), this.fullDayNightCycleLengthBox);

        this.nightLengthBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.nightLengthBox.setValue(Double.toString(customNightLength));
        this.nightLengthBox.setTooltip(Tooltip.create(Component.literal("Better Days nighttime speed multiplier. Below 1 makes nights longer; above 1 makes them shorter.")));
        this.list.addRow(Component.literal("Custom Night Speed Multiplier"), this.nightLengthBox);

        this.dayLengthBox = new EditBox(this.font, 0, 0, 200, 20, Component.empty());
        this.dayLengthBox.setValue(Double.toString(customDayLength));
        this.dayLengthBox.setTooltip(Tooltip.create(Component.literal("Better Days daytime speed multiplier. Below 1 makes days longer; above 1 makes them shorter.")));
        this.list.addRow(Component.literal("Custom Day Speed Multiplier"), this.dayLengthBox);

        
        this.addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), b -> {
                    saveChanges();
                    this.minecraft.setScreen(parent);
                }).bounds(panelX + (panelW - 200) / 2, bottom - 30, 200, 20).build()
        );
    }

    /**
     * Renders the screen background, title, and child widgets.
     *
     * @param g           GUI graphics context
     * @param mouseX      mouse x
     * @param mouseY      mouse y
     * @param partialTick partial tick time
     */
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g); 

        
        int panelW = 480;
        int panelX = (this.width - panelW) / 2;
        int top = 40;
        int bottom = this.height - 40;
        g.fill(panelX - 4, top - 4, panelX + panelW + 4, bottom, 0xAA000000); 
        g.drawString(this.font, "Serene Seasons Plus", panelX + 6, top - 14, 0xFFFFFF, false);
        super.render(g, mouseX, mouseY, partialTick); 
    }

    /**
     * Builds a simple ON/OFF label for a toggleable option.
     *
     * @param name    option label
     * @param enabled current state
     * @return the composed label component
     */
    private Component toggleLabel(String name, boolean enabled) {
        return Component.literal(name + ": " + (enabled ? "ON" : "OFF"));
    }

    /**
     * Reads field values, validates and persists them to the COMMON config.
     */
    private void saveChanges() {
        Component errorMessage;
        int parsed2 = this.tickSnowReplacerThreshold;
        int parsedSnowHeight = this.maxSnowHeight;
        double parsed3 = this.customDayLength;
        double parsed4 = this.customNightLength;
        double parsedFullDayNightCycleMinutes = this.fullDayNightCycleLengthInRealMinutes;

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
            parsedFullDayNightCycleMinutes = Double.parseDouble(this.fullDayNightCycleLengthBox.getValue());
            errorMessage = null;
        } catch (NumberFormatException ignored) {
            errorMessage = Component.literal("Invalid number for one of the DayCycle Speeds.");
        }
        SereneExtendedConfig.TICK_SNOW_REPLACER.set(parsed2);
        SereneExtendedConfig.SNOWSTORM_ENABLED.set(snowFeatureEnabled);
        SereneExtendedConfig.MAX_SNOW_ACCUMULATION_LAYERS.set(parsedSnowHeight);
        SereneExtendedConfig.ENABLE_SEASONAL_DAYLIGHT_CYCLE.set(seasonalDaylightCycle);
        SereneExtendedConfig.ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT.set(betterDaysDynamicTimeCompat);
        SereneExtendedConfig.KEEP_FULL_DAY_NIGHT_CYCLE_AT_FIXED_LENGTH.set(keepFullDayNightCycleAtFixedLength);
        SereneExtendedConfig.FULL_DAY_NIGHT_CYCLE_LENGTH_IN_REAL_MINUTES.set(parsedFullDayNightCycleMinutes);
        SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.set(customDayCycle);
        SereneExtendedConfig.CUSTOM_DAY_LENGTH.set(parsed3);
        SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.set(parsed4);
        SereneExtendedConfig.GRASS_FLOWER_GROWTH_ENABLED.set(grassFlowerGrowth);
        SereneExtendedConfig.REAL_TIME_CANADIAN_SEASONS.set(realTimeCanadianSeasons);


        try {
            
            saveCommonConfigForMod("sereneseasonsplus");

            
            

            errorMessage = null;
        } catch (Exception e) {
            errorMessage = Component.literal("Failed to save config: " + e.getMessage());
        }
    }

    /**
     * Saves the Forge COMMON config file for the given mod id, if present.
     *
     * @param modId target mod id
     */
    private static void saveCommonConfigForMod(String modId) {
        var set = ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.COMMON);
        if (set == null) return;
        for (ModConfig cfg : set) {
            if (cfg.getModId().equals(modId)) {
                cfg.save();
                return;
            }
        }
    }

    /**
     * Restores focus and returns to the parent screen.
     */
    @Override
    public void onClose() {
        this.setFocused(false);
        Minecraft.getInstance().setScreen(parent);
    }

    /**
     * Forwards click events to the list and its child widgets.
     */
    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (this.list != null && this.list.mouseClicked(x, y, button)) return true;
        return super.mouseClicked(x, y, button);
    }

    /**
     * Forwards key events to the list and focused text boxes.
     */
    @Override
    public boolean keyPressed(int key, int sc, int mods) {
        if (this.list != null && this.list.keyPressed(key, sc, mods)) return true;

        
        if (this.maxReplacerBox != null && this.maxReplacerBox.keyPressed(key, sc, mods)) return true;
        if (this.maxSnowHeightBox   != null && this.maxSnowHeightBox.keyPressed(key, sc, mods)) return true;
        if (this.fullDayNightCycleLengthBox != null && this.fullDayNightCycleLengthBox.keyPressed(key, sc, mods)) return true;
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
        if (this.fullDayNightCycleLengthBox != null && this.fullDayNightCycleLengthBox.charTyped(c, mods)) return true;
        if (this.nightLengthBox != null && this.nightLengthBox.charTyped(c, mods)) return true;
        if (this.dayLengthBox   != null && this.dayLengthBox.charTyped(c, mods)) return true;

        return super.charTyped(c, mods);
    }


}

