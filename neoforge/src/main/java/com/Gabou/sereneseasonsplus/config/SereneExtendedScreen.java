package com.Gabou.sereneseasonsplus.config;

import com.Gabou.sereneseasonsplus.client.config.SereneExtendedList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Simple in-game configuration screen.
 */
public class SereneExtendedScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 440;
    private static final int PANEL_MARGIN = 16;
    private static final int PANEL_TOP = 28;
    private static final int PANEL_BOTTOM_MARGIN = 20;
    private static final int FOOTER_HEIGHT = 40;

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
    private Button seasonalCycleButton;
    private Button customCycleButton;
    private Component errorMessage;
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
        this.betterDaysDynamicTimeCompat = SereneExtendedConfig.ENABLE_BETTER_DAYS_DYNAMIC_TIME_COMPAT.get();
        this.keepFullDayNightCycleAtFixedLength = SereneExtendedConfig.KEEP_FULL_DAY_NIGHT_CYCLE_AT_FIXED_LENGTH.get();
        this.fullDayNightCycleLengthInRealMinutes = SereneExtendedConfig.FULL_DAY_NIGHT_CYCLE_LENGTH_IN_REAL_MINUTES.get();
        this.customDayCycle = SereneExtendedConfig.CUSTOM_CYCLE_LENGTH.get();
        this.customDayLength = SereneExtendedConfig.CUSTOM_DAY_LENGTH.get();
        this.customNightLength = SereneExtendedConfig.CUSTOM_NIGHT_LENGTH.get();
        this.grassFlowerGrowth = SereneExtendedConfig.GRASS_FLOWER_GROWTH_ENABLED.get();
        this.realTimeCanadianSeasons = SereneExtendedConfig.REAL_TIME_CANADIAN_SEASONS.get();


        int panelW = Math.min(MAX_PANEL_WIDTH, Math.max(280, this.width - PANEL_MARGIN * 2));
        int panelX = (this.width - panelW) / 2;
        int top = PANEL_TOP;
        int bottom = this.height - PANEL_BOTTOM_MARGIN;
        int listTop = top + 24;
        int listHeight = Math.max(40, bottom - listTop - FOOTER_HEIGHT);

        this.list = new SereneExtendedList(this.minecraft, panelW - 16, listHeight, listTop, 24);
        this.list.setX(panelX + 8);
        this.addRenderableWidget(this.list);

        var snowFeatureBtn = Button.builder(toggleLabel(snowFeatureEnabled), b -> {
            snowFeatureEnabled = !snowFeatureEnabled;
            b.setMessage(toggleLabel(snowFeatureEnabled));
        }).bounds(0,0,200,20).build();
        snowFeatureBtn.setTooltip(Tooltip.create(Component.literal("Allows the mod to place, replace, and accumulate snow during supported seasonal weather.")));
        this.list.addRow(Component.literal("Seasonal Snow Accumulation"), snowFeatureBtn);

        this.seasonalCycleButton = Button.builder(toggleLabel(seasonalDaylightCycle), b -> {
            seasonalDaylightCycle = !seasonalDaylightCycle;
            if (seasonalDaylightCycle) {
                customDayCycle = false;
                this.customCycleButton.setMessage(toggleLabel(false));
            }
            b.setMessage(toggleLabel(seasonalDaylightCycle));
        }).bounds(0,0,200,20).build();
        this.seasonalCycleButton.setTooltip(Tooltip.create(Component.literal("Changes daylight and nighttime proportions using the current in-game sub-season.")));
        this.list.addRow(Component.literal("Season-Based Day/Night Lengths"), this.seasonalCycleButton);

        var betterDaysCompatBtn = Button.builder(toggleLabel(betterDaysDynamicTimeCompat), b -> {
            betterDaysDynamicTimeCompat = !betterDaysDynamicTimeCompat;
            b.setMessage(toggleLabel(betterDaysDynamicTimeCompat));
        }).bounds(0,0,200,20).build();
        betterDaysCompatBtn.setTooltip(Tooltip.create(Component.literal("Allows Serene Seasons Plus to control Better Days day and night speed values.")));
        this.list.addRow(Component.literal("Seasonal Better Days Time Control"), betterDaysCompatBtn);

        var fixedCycleBtn = Button.builder(toggleLabel(keepFullDayNightCycleAtFixedLength), b -> {
            keepFullDayNightCycleAtFixedLength = !keepFullDayNightCycleAtFixedLength;
            b.setMessage(toggleLabel(keepFullDayNightCycleAtFixedLength));
        }).bounds(0,0,200,20).build();
        fixedCycleBtn.setTooltip(Tooltip.create(Component.literal("Keeps one complete day and night at the configured elapsed duration while seasons change the daylight/night split. This does not sync to the computer clock.")));
        this.list.addRow(Component.literal("Fixed Full Day/Night Cycle Length"), fixedCycleBtn);

        var grassFlowerBtn = Button.builder(toggleLabel(grassFlowerGrowth), b -> {
            grassFlowerGrowth = !grassFlowerGrowth;
            b.setMessage(toggleLabel(grassFlowerGrowth));
        }).bounds(0,0,200,20).build();
        grassFlowerBtn.setTooltip(Tooltip.create(Component.literal("Allows extra grass and flower growth during warm seasons.")));
        this.list.addRow(Component.literal("Seasonal Grass/Flower Growth"), grassFlowerBtn);

        this.customCycleButton = Button.builder(toggleLabel(customDayCycle), b -> {
            customDayCycle = !customDayCycle;
            if (customDayCycle) {
                seasonalDaylightCycle = false;
                this.seasonalCycleButton.setMessage(toggleLabel(false));
            }
            b.setMessage(toggleLabel(customDayCycle));
        }).bounds(0,0,200,20).build();
        this.customCycleButton.setTooltip(Tooltip.create(Component.literal("Uses the custom day and night speed multipliers when seasonal day/night changes are disabled.")));
        this.list.addRow(Component.literal("Custom Day/Night Speed Multipliers"), this.customCycleButton);

        var realTimeBtn = Button.builder(toggleLabel(realTimeCanadianSeasons), b -> {
            realTimeCanadianSeasons = !realTimeCanadianSeasons;
            b.setMessage(toggleLabel(realTimeCanadianSeasons));
        }).bounds(0,0,200,20).build();
        realTimeBtn.setTooltip(Tooltip.create(Component.literal("Synchronizes seasons with the current date in the America/Toronto time zone. Leave off for normal in-game progression.")));
        this.list.addRow(Component.literal("Eastern Canada Calendar Sync"), realTimeBtn);

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
                    if (saveChanges()) {
                        this.minecraft.setScreen(parent);
                    }
                }).bounds(panelX + (panelW - 200) / 2, bottom - 28, 200, 20).build()
        );
    }


    @Override
    /**
     * Draws background, panel chrome, and delegates to list/widgets.
     */
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (this.errorMessage != null) {
            g.drawCenteredString(this.font, this.errorMessage, this.width / 2, this.height - 47, 0xFFFF5555);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int panelW = Math.min(MAX_PANEL_WIDTH, Math.max(280, this.width - PANEL_MARGIN * 2));
        int panelX = (this.width - panelW) / 2;
        int bottom = this.height - PANEL_BOTTOM_MARGIN;
        g.fill(panelX, PANEL_TOP, panelX + panelW, bottom, 0xCC101010);
        g.drawCenteredString(this.font, this.title, this.width / 2, PANEL_TOP + 8, 0xFFFFFF);
    }

    /**
     * Formats a toggle label with ON/OFF state.
     */
    private Component toggleLabel(boolean enabled) {
        return Component.literal(enabled ? "ON" : "OFF");
    }

    /**
     * Validates inputs and writes changes to config, then persists them.
     */
    private boolean saveChanges() {
        int parsed2 = this.tickSnowReplacerThreshold;
        int parsedSnowHeight = this.maxSnowHeight;
        double parsed3 = this.customDayLength;
        double parsed4 = this.customNightLength;
        double parsedFullDayNightCycleMinutes = this.fullDayNightCycleLengthInRealMinutes;


        try {
            parsed2 = Integer.parseInt(this.maxReplacerBox.getValue());
            parsedSnowHeight = Integer.parseInt(this.maxSnowHeightBox.getValue());
        } catch (NumberFormatException ignored) {
            this.errorMessage = Component.literal("Snow settings must be whole numbers.");
            return false;
        }

        try {
            parsed3 = Double.parseDouble(this.dayLengthBox.getValue());
            parsed4 = Double.parseDouble(this.nightLengthBox.getValue());
            parsedFullDayNightCycleMinutes = Double.parseDouble(this.fullDayNightCycleLengthBox.getValue());
        } catch (NumberFormatException ignored) {
            this.errorMessage = Component.literal("Day and night speeds must be numbers.");
            return false;
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
            SereneExtendedConfig.COMMON_SPEC.save();
            this.errorMessage = null;
            return true;
        } catch (RuntimeException exception) {
            this.errorMessage = Component.literal("Failed to save config: " + exception.getMessage());
            return false;
        }
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
